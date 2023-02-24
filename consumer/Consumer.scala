//> using scala "2.13"
//> using lib "com.ocadotechnology::pass4s-kernel:0.2.2"
//> using lib "com.ocadotechnology::pass4s-core:0.2.2"
//> using lib "com.ocadotechnology::pass4s-high:0.2.2"
//> using lib "com.ocadotechnology::pass4s-connector-sns:0.2.2"
//> using lib "com.ocadotechnology::pass4s-connector-sqs:0.2.2"
//> using lib "com.ocadotechnology::pass4s-extra:0.2.2"
//> using lib "org.typelevel::log4cats-slf4j:2.5.0"

import com.ocadotechnology.pass4s.high.Broker
import com.ocadotechnology.pass4s.connectors.sqs.SqsConnector
import cats.effect.IOApp
import cats.effect.ExitCode
import cats.effect.IO
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider
import software.amazon.awssdk.regions.Region
import java.net.URI

import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jLogger
import com.ocadotechnology.pass4s.extra.MessageProcessor
import com.ocadotechnology.pass4s.core.Source
import com.ocadotechnology.pass4s.connectors.sqs.SqsSource
import com.ocadotechnology.pass4s.connectors.sqs.SqsUrl
import com.ocadotechnology.pass4s.connectors.sqs.SqsEndpoint
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    implicit val ioLogger: Logger[IO] = Slf4jLogger.getLogger[IO]

    val awsCreds = AwsBasicCredentials.create("test", "AWSSECRET");

    val credentialsProvider = StaticCredentialsProvider.create(awsCreds)
    // val credentialsProvider = AnonymousCredentialsProvider.create()
    val sqsConnector =
      SqsConnector.usingLocalAwsWithDefaultAttributesProvider[IO](new URI("http://localhost:4566"), Region.EU_WEST_2, credentialsProvider)

    sqsConnector.use { connector =>
      println("Creating broker and processor")
      val broker = Broker.fromConnector(connector)
      val processor = MessageProcessor.init[IO].effectful.bindBroker(broker)
      val sqsSource = SqsEndpoint(SqsUrl("http://sqs.localhost.localstack.cloud:4566/000000000000/local_queue"))
      // localhost.localstack.cloud:4566
      processor
        .handle(sqsSource) { message =>
          println("processin message")
          IO.println(message)
        }
        .use(_ => IO.never)

    }

  }

}
