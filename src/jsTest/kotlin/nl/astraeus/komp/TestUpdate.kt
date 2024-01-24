package nl.astraeus.komp

import kotlinx.browser.document
import kotlinx.html.DIV
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

class TestKomponent : Komponent() {
  override fun HtmlBuilder.render() {
    div {
      +"Test"
    }
  }
}

class Child1 : Komponent() {
  override fun HtmlBuilder.render() {
    div {
      +"Child 1"
    }
  }
}

class Child2 : Komponent() {
  override fun HtmlBuilder.render() {
    div {
      id ="1234"
      +"Child 2"
    }
  }
}

class SimpleKomponent : Komponent() {
  var hello = true
  var append = HtmlBuilder.create {
    p {
      +"Appended"
    }
  }

  override fun HtmlBuilder.render() {
    div("div_class") {
      input(InputType.checkBox) {
        name = "helloInput"
        checked = hello
      }
      span {
        svg {
          unsafe {
            +"""
              <p bla>
            """.trimIndent()
          }
        }
        if (hello) {
          div {
            +"Hello"
          }
        } else {
          span {
            +"Good bye"
          }
        }
      }
      div {
        if (hello) {
          id = "123"
          +"div text"
        } else {
          +"div text goodbye"
        }

        onClickFunction = if (hello) {
          {
            println("onClick")
          }
        } else {
          {
            println("onClick 2")
          }
        }
      }
      if (hello) {
        span {
          +"2nd span"
        }
      }
      //append(append)
      if (hello) {
        include(Child1())
      } else {
        include(Child2())
      }
      //append(append)
    }
  }

}

class IncludeKomponent(
  var text: String = "My Text"
) : Komponent() {

  override fun generateMemoizeHash(): Int = text.hashCode()

  override fun HtmlBuilder.render() {
    span {
      +text
    }
  }
}

class ReplaceKomponent : Komponent() {
  val includeKomponent = IncludeKomponent("Other text")
  var includeSpan = true

  override fun generateMemoizeHash(): Int = includeSpan.hashCode() * 7 + includeKomponent.generateMemoizeHash()

  override fun HtmlBuilder.render() {
    div {
      +"Child 2"

      div {
        if (includeSpan) {
          for (index in 0 ..< 3) {
            extracted(index)
          }
        }

        include(includeKomponent)
      }
    }
  }

  private fun HtmlBuilder.extracted(index: Int) {
    span {
      i("fas fa-eye") {
        + ("span" + (index+1))
      }
    }
  }
}

class TestUpdate {

  @Test
  fun testUpdateWithEmpty() {
    val div = document.createElement("div") as HTMLDivElement
    val rk = ReplaceKomponent()

    Komponent.logRenderEvent = true

    Komponent.create(div, rk)

    println("ReplaceKomponent: ${div.printTree()}")

    rk.requestImmediateUpdate()

    println("ReplaceKomponent: ${div.printTree()}")

    rk.requestImmediateUpdate()

    println("ReplaceKomponent: ${div.printTree()}")

    rk.includeSpan = false
    rk.requestImmediateUpdate()

    println("ReplaceKomponent: ${div.printTree()}")

    rk.includeSpan = true
    rk.includeKomponent.text = "New Text"
    rk.requestImmediateUpdate()

    println("ReplaceKomponent: ${div.printTree()}")
  }

  @Test
  fun testSimpleKomponent() {
    val sk = SimpleKomponent()
    val div = document.createElement("div") as HTMLDivElement

    Komponent.create(div, sk)

    println("SimpleKomponent: ${div.printTree()}")

    sk.hello = false
    sk.requestImmediateUpdate()

    println("SimpleKomponent updated: ${div.printTree()}")
  }

  @Test
  fun testCreate() {
    var elemTest: Element? = null
    val element = HtmlBuilder.create {
      div(classes = "div_class") {
        classes = classes + "bla'"
        id = "123"
        +"Test"

        span("span_class") {
          +"Span"

          elemTest = currentElement()
        }

        table {
          tr {
            td {
              +"column 1"
            }
            td {
              +"column 2"
            }
          }
        }
      }
    }

    println("Element: ${element.printTree()}")
    println("divTst: ${elemTest?.printTree()}")
    println("span class: ${
      elemTest?.getAttributeNames()?.joinToString
      { "," }
    }"
    )
  }

}
