package net.michalp.pass4splayground

import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.implicits._
import com.ocadotechnology.pass4s.phobos.syntax._
import com.ocadotechnology.pass4s.connectors.sqs.SqsConnector
import com.ocadotechnology.pass4s.connectors.sqs.SqsEndpoint
import com.ocadotechnology.pass4s.connectors.sqs.SqsSource
import com.ocadotechnology.pass4s.connectors.sqs.SqsUrl
import com.ocadotechnology.pass4s.core.Source
import com.ocadotechnology.pass4s.extra.MessageProcessor
import com.ocadotechnology.pass4s.high.Broker
import net.michalp.pass4splayground.XmlMessage
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region

import java.net.URI

object XmlConsumer extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    implicit val ioLogger: Logger[IO] = NoOpLogger[IO]

    val awsCredentials = AwsBasicCredentials.create("test", "AWSSECRET");

    val credentialsProvider = StaticCredentialsProvider.create(awsCredentials)
    val sqsConnector =
      SqsConnector.usingLocalAwsWithDefaultAttributesProvider[IO](new URI("http://localhost:4566"), Region.EU_WEST_2, credentialsProvider)
    val sqsSource = SqsEndpoint(SqsUrl("http://localhost:4566/000000000000/local_queue"))
    sqsConnector.use { connector =>
      val broker = Broker.fromConnector(connector)

      val processor =
        MessageProcessor
          .init[IO]
          .effectful
          .bindBroker(broker)
          .enrich(_.map(_.text))
          .enrich(
            _.mapM(rawText => IO.println(s"Raw message text: $rawText").as(rawText))
          )
          .enrich(_.asXmlConsumer[XmlMessage])

      IO.println(s"Processor listening for messages on $sqsSource") *>
        processor
          .handle(sqsSource) { message =>
            IO.println(s"Received message: $message")
          }
          .use(_ => IO.never)

    }

  }

}
