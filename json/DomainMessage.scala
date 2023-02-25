package net.michalp.pass4splayground

import io.circe.Codec
import io.circe.generic.semiauto._

final case class DomainMessage(description: String, value: Long)

object DomainMessage {
  implicit val codec: Codec[DomainMessage] = deriveCodec
}
