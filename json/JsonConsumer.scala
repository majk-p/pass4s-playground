package net.michalp.pass4splayground

import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.implicits._
import com.ocadotechnology.pass4s.circe.syntax._
import com.ocadotechnology.pass4s.connectors.sqs.SqsConnector
import com.ocadotechnology.pass4s.connectors.sqs.SqsEndpoint
import com.ocadotechnology.pass4s.connectors.sqs.SqsSource
import com.ocadotechnology.pass4s.connectors.sqs.SqsUrl
import com.ocadotechnology.pass4s.core.Source
import com.ocadotechnology.pass4s.extra.MessageProcessor
import com.ocadotechnology.pass4s.high.Broker
import net.michalp.pass4splayground.DomainMessage
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region

import java.net.URI

object JsonConsumer extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    implicit val ioLogger: Logger[IO] = NoOpLogger[IO]

    println(DomainMessage("test", 10))
    val awsCredentials = AwsBasicCredentials.create("test", "AWSSECRET");

    val credentialsProvider = StaticCredentialsProvider.create(awsCredentials)
    val sqsConnector =
      SqsConnector.usingLocalAwsWithDefaultAttributesProvider[IO](new URI("http://localhost:4566"), Region.EU_WEST_2, credentialsProvider)
    val sqsSource = SqsEndpoint(SqsUrl("http://localhost:4566/000000000000/local_queue"))
    sqsConnector.use { connector =>
      val broker = Broker.fromConnector(connector)

      val processor = MessageProcessor.init[IO].effectful.bindBroker(broker)

      IO.println(s"Processor listening for messages on $sqsSource") *>
        broker
          .consumer(sqsSource)
          .asJsonConsumer[DomainMessage]
          .consume(IO.println)
          .background
          .void
          .use(_ => IO.never)

    // val processor = MessageProcessor.init[IO].effectful.bindBroker(broker).enrich(_.asJsonConsumer[DomainMessage])

    // IO.println(s"Processor listening for messages on $sqsSource") *>
    //   processor
    //     .handle(sqsSource) { message =>
    //       IO.println(message)
    //     }
    //     .use(_ => IO.never)

    }

  }

}
