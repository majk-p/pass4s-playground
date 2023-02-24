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
import cats.implicits._
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
import com.ocadotechnology.pass4s.connectors.sns.SnsArn
import com.ocadotechnology.pass4s.connectors.sns.SnsDestination
import com.ocadotechnology.pass4s.connectors.sns.SnsConnector
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import com.ocadotechnology.pass4s.core.Message

object Producer extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    implicit val ioLogger: Logger[IO] = Slf4jLogger.getLogger[IO]

    val awsCreds = AwsBasicCredentials.create("test", "AWSSECRET");
    val snsDestination = SnsDestination(SnsArn("arn:aws:sns:eu-west-2:000000000000:local_sns"))
    val localstackURI = new URI("http://localhost:4566")

    val credentialsProvider = StaticCredentialsProvider.create(awsCreds)

    val snsConnector =
      SnsConnector.usingLocalAwsWithDefaultAttributesProvider[IO](localstackURI, Region.EU_WEST_2, credentialsProvider)

    snsConnector.use { connector =>
      val broker = Broker.fromConnector(connector)

      val message = Message(Message.Payload("hello world!", Map()), snsDestination)

      IO.println(s"Sending one message to $snsDestination") *>
        broker.sender.sendOne(message) *>
        IO.println("Sent, exiting!").as(ExitCode.Success)
    }

  }

}
