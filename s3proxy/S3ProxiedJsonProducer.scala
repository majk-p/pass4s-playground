package net.michalp.pass4splayground

import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.implicits._
import com.ocadotechnology.pass4s.circe.syntax._
import com.ocadotechnology.pass4s.connectors.sns.SnsArn
import com.ocadotechnology.pass4s.connectors.sns.SnsConnector
import com.ocadotechnology.pass4s.connectors.sns.SnsDestination
import com.ocadotechnology.pass4s.connectors.sqs.SqsConnector
import com.ocadotechnology.pass4s.core.Message
import com.ocadotechnology.pass4s.core.Source
import com.ocadotechnology.pass4s.extra.MessageProcessor
import com.ocadotechnology.pass4s.high.Broker
import com.ocadotechnology.pass4s.s3proxy.S3Client
import com.ocadotechnology.pass4s.s3proxy.S3ProxyConfig
import com.ocadotechnology.pass4s.s3proxy.syntax._
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.noop.NoOpLogger
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region

import java.net.URI

object S3ProxiedJsonProducer extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    implicit val ioLogger: Logger[IO] = NoOpLogger[IO]

    val awsCredentials = AwsBasicCredentials.create("test", "AWSSECRET")
    val snsDestination = SnsDestination(SnsArn("arn:aws:sns:eu-west-2:000000000000:local_sns"))
    val localstackURI = new URI("http://localhost.localstack.cloud:4566")

    val credentialsProvider = StaticCredentialsProvider.create(awsCredentials)

    val bucketName = "large-messages"

    val senderConfig =
      S3ProxyConfig
        .Sender
        .withSnsDefaults(bucketName)
        .copy(minPayloadSize = Some(0))

    val awsResources =
      for {
        sns <- SnsConnector.usingLocalAwsWithDefaultAttributesProvider[IO](localstackURI, Region.EU_WEST_2, credentialsProvider)
        s3  <- S3Client.usingLocalAws[IO](localstackURI, Region.EU_WEST_2, credentialsProvider)
      } yield (sns, s3)

    awsResources.use { case (connector, s3Client) =>
      implicit val _s3Client: S3Client[IO] = s3Client
      val broker = Broker.fromConnector(connector)

      val domainMessageSender =
        broker
          .sender
          .usingS3Proxy(senderConfig)
          .asJsonSender[PotentiallyLargeMessage](snsDestination)

      val domainMessage = PotentiallyLargeMessage("hello world!", 10)

      IO.println(s"Sending message: $domainMessage to $snsDestination") *>
        domainMessageSender.sendOne(domainMessage) *>
        IO.println("Sent, exiting!").as(ExitCode.Success)
    }

  }

}
