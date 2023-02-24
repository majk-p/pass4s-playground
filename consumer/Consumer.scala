//> using scala "2.13"
//> using lib "com.ocadotechnology::pass4s-kernel:0.2.2"
//> using lib "com.ocadotechnology::pass4s-core:0.2.2"
//> using lib "com.ocadotechnology::pass4s-high:0.2.2"
//> using lib "com.ocadotechnology::pass4s-connector-sns:0.2.2"
//> using lib "com.ocadotechnology::pass4s-connector-sqs:0.2.2"
//> using lib "com.ocadotechnology::pass4s-extra:0.2.2"
//> using lib "org.typelevel::log4cats-noop:2.5.0"

import com.ocadotechnology.pass4s.high.Broker
import com.ocadotechnology.pass4s.connectors.sqs.SqsConnector
import cats.effect.IOApp
import cats.implicits._
import cats.effect.ExitCode
import cats.effect.IO
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider
import software.amazon.awssdk.regions.Region
import java.net.URI

import org.typelevel.log4cats.Logger
import com.ocadotechnology.pass4s.extra.MessageProcessor
import com.ocadotechnology.pass4s.core.Source
import com.ocadotechnology.pass4s.connectors.sqs.SqsSource
import com.ocadotechnology.pass4s.connectors.sqs.SqsUrl
import com.ocadotechnology.pass4s.connectors.sqs.SqsEndpoint
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import org.typelevel.log4cats.noop.NoOpLogger

object Consumer extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    implicit val ioLogger: Logger[IO] = NoOpLogger[IO]

    val awsCreds = AwsBasicCredentials.create("test", "AWSSECRET");

    val credentialsProvider = StaticCredentialsProvider.create(awsCreds)
    val sqsConnector =
      SqsConnector.usingLocalAwsWithDefaultAttributesProvider[IO](new URI("http://localhost:4566"), Region.EU_WEST_2, credentialsProvider)
    val sqsSource = SqsEndpoint(SqsUrl("http://localhost:4566/000000000000/local_queue"))
    sqsConnector.use { connector =>
      val broker = Broker.fromConnector(connector)
      val processor = MessageProcessor.init[IO].effectful.bindBroker(broker)

      IO.println(s"Processor listening for messages on $sqsSource") *>
        processor
          .handle(sqsSource) { message =>
            IO.println(message)
          }
          .use(_ => IO.never)

    }

  }

}
