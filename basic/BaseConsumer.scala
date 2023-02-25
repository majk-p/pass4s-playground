//> using scala "2.13"
//> using lib "com.ocadotechnology::pass4s-kernel:0.2.2"
//> using lib "com.ocadotechnology::pass4s-core:0.2.2"
//> using lib "com.ocadotechnology::pass4s-high:0.2.2"
//> using lib "com.ocadotechnology::pass4s-connector-sqs:0.2.2"
//> using lib "org.typelevel::log4cats-noop:2.5.0"

package net.michalp.pass4splayground

import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.implicits._
import com.ocadotechnology.pass4s.connectors.sqs.SqsConnector
import com.ocadotechnology.pass4s.connectors.sqs.SqsEndpoint
import com.ocadotechnology.pass4s.connectors.sqs.SqsSource
import com.ocadotechnology.pass4s.connectors.sqs.SqsUrl
import com.ocadotechnology.pass4s.core.Source
import com.ocadotechnology.pass4s.high.Broker
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.noop.NoOpLogger
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region

import java.net.URI

object BaseConsumer extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    implicit val ioLogger: Logger[IO] = NoOpLogger[IO]

    val awsCredentials = AwsBasicCredentials.create("test", "AWSSECRET")
    val localstackURI = new URI("http://localhost:4566")
    val sqsSource = SqsEndpoint(SqsUrl("http://localhost:4566/000000000000/local_queue"))

    val credentialsProvider = StaticCredentialsProvider.create(awsCredentials)
    val sqsConnector =
      SqsConnector.usingLocalAwsWithDefaultAttributesProvider[IO](localstackURI, Region.EU_WEST_2, credentialsProvider)

    sqsConnector.use { connector =>
      val broker = Broker.fromConnector(connector)

      IO.println(s"Processor listening for messages on $sqsSource") *>
        broker
          .consumer(sqsSource)
          .consume(IO.println)
          .background
          .void
          .use(_ => IO.never)

    }

  }

}
