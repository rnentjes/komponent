package nl.astraeus.komp

import kotlinx.html.dom.create
import kotlinx.html.js.div
import kotlinx.html.style
import org.w3c.dom.HTMLElement
import kotlin.browser.document

/**
 * User: rnentjes
 * Date: 31-1-18
 * Time: 15:58
 */

enum class SizeType {
  HBAR,
  VBAR
}

abstract class SizedKomponent(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int
): Komponent() {
  var parent: SizedKomponent? = null
  var type: SizeType = SizeType.HBAR
  var size: Int = 0

  constructor(
      parent: SizedKomponent,
      type: SizeType,
      size: Int
  ) :this(0,0,0,0) {
    this.parent = parent
    this.type = type
    this.size = size
  }

  override fun create(): HTMLElement {
    val innerResult = super.create()

    val result = document.create.div {
      style = "left: ${left}px; top: ${top}px; width: ${width}px; height: ${height}px;" // sizing here
    }

    result.appendChild(innerResult)
    this.element = result
    return result
  }

}
