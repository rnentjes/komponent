package nl.astraeus.komp

import kotlinx.browser.document
import kotlinx.html.InputType
import kotlinx.html.classes
import kotlinx.html.div
import kotlinx.html.i
import kotlinx.html.id
import kotlinx.html.input
import kotlinx.html.js.onClickFunction
import kotlinx.html.p
import kotlinx.html.span
import kotlinx.html.svg
import kotlinx.html.table
import kotlinx.html.td
import kotlinx.html.tr
import kotlinx.html.unsafe
import org.w3c.dom.Element
import org.w3c.dom.HTMLDivElement
import kotlin.test.Test

class TestSvgKomponent : Komponent() {
  override fun HtmlBuilder.render() {
    div {
      +"Test"

      svg("my-class") {
        classes += "added-class"
        unsafe {
          +"""arc(1,2)"""
        }
      }
    }
  }
}

class TestSvg {

  @Test
  fun testUpdateWithEmpty() {
    val div = document.createElement("div") as HTMLDivElement
    val rk = TestSvgKomponent()

    Komponent.logRenderEvent = true

    Komponent.create(div, rk)

    println("SvgKomponent: ${div.printTree()}")
  }
}
