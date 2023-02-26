package net.michalp.pass4splayground

import ru.tinkoff.phobos.decoding._
import ru.tinkoff.phobos.encoding._
import ru.tinkoff.phobos.syntax._
import ru.tinkoff.phobos.derivation.semiauto._

final case class XmlMessage(description: String, value: Long, rows: List[String])

object XmlMessage {
  implicit val xmlEncoder: XmlEncoder[XmlMessage] = deriveXmlEncoder("xmlMessage")
  implicit val xmlDecoder: XmlDecoder[XmlMessage] = deriveXmlDecoder("xmlMessage")
}
