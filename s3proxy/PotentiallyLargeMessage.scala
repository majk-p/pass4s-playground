package net.michalp.pass4splayground

import io.circe.Codec
import io.circe.generic.semiauto._

final case class PotentiallyLargeMessage(description: String, value: Long)

object PotentiallyLargeMessage {
  implicit val codec: Codec[PotentiallyLargeMessage] = deriveCodec
}
