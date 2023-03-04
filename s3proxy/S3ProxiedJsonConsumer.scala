package net.michalp.pass4splayground

import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.std.UUIDGen
import cats.implicits._
import com.ocadotechnology.pass4s.circe.syntax._
import com.ocadotechnology.pass4s.connectors.sqs.SqsConnector
import com.ocadotechnology.pass4s.connectors.sqs.SqsEndpoint
import com.ocadotechnology.pass4s.connectors.sqs.SqsSource
import com.ocadotechnology.pass4s.connectors.sqs.SqsUrl
import com.ocadotechnology.pass4s.core.Source
import com.ocadotechnology.pass4s.extra.MessageProcessor
import com.ocadotechnology.pass4s.high.Broker
import com.ocadotechnology.pass4s.s3proxy.S3Client
import com.ocadotechnology.pass4s.s3proxy.S3ProxyConfig
import com.ocadotechnology.pass4s.s3proxy.syntax._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region

import java.net.URI

object S3ProxiedJsonConsumer extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    implicit val ioLogger: Logger[IO] = NoOpLogger[IO]

    val awsCredentials = AwsBasicCredentials.create("test", "AWSSECRET");

    val credentialsProvider = StaticCredentialsProvider.create(awsCredentials)
    val endpointOverride = new URI("http://localhost.localstack.cloud:4566")

    val sqsSource = SqsEndpoint(SqsUrl("http://localhost.localstack.cloud:4566/000000000000/local_queue"))

    val consumerConfig =
      S3ProxyConfig
        .Consumer
        .withSnsDefaults()
        .copy(
          shouldDeleteAfterProcessing = true // it doesn't by default, just in case there's more listeners
        )

    val awsResources =
      for {
        sqs <- SqsConnector.usingLocalAwsWithDefaultAttributesProvider[IO](endpointOverride, Region.EU_WEST_2, credentialsProvider)
        s3  <- S3Client.usingLocalAws[IO](endpointOverride, Region.EU_WEST_2, credentialsProvider)
      } yield (sqs, s3)

    awsResources.use { case (connector, s3Client) =>
      implicit val _s3Client: S3Client[IO] = s3Client

      val broker = Broker.fromConnector(connector)

      val processor =
        MessageProcessor
          .init[IO]
          .effectful
          .bindBroker(broker)
          .enrich(_.usingS3Proxy(consumerConfig))
          .enrich(_.asJsonConsumer[PotentiallyLargeMessage])

      IO.println(s"Processor listening for messages on $sqsSource") *>
        processor
          .handle(sqsSource) { message =>
            IO.println(s"Received message: $message")
          }
          .use(_ => IO.never)

    }

  }

}
