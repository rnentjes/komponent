package nl.astraeus.komp

import kotlinx.html.Tag
import org.w3c.dom.Element

class KomponentException(
  val komponent: Komponent?,
  val element: Element?,
  val tag: Tag,
  val position: String,
  message: String,
  cause: Throwable
) : RuntimeException(message, cause) {

  override fun toString(): String {
    return "KompException(message='$message', tag='$tag', position='$position')"
  }
}
