package nl.astraeus.komp

import kotlinx.html.*
import kotlinx.html.js.onClickFunction
import org.w3c.dom.HTMLElement
import org.w3c.dom.Node
import org.w3c.dom.get
import kotlin.test.Test
import kotlin.test.assertTrue

fun nodesEqual(node1: Node, node2: Node): Boolean {
  if (node1.childNodes.length != node1.childNodes.length) {
    return false
  }
  if (node1 is HTMLElement && node2 is HTMLElement) {
    if (node1.attributes.length != node2.attributes.length) {
      return false
    }
    for (index in 0 until node1.attributes.length) {
      node1.attributes[index]?.also { attr1 ->
        val attr2 = node2.getAttribute(attr1.name)

        if (attr1.value != attr2) {
          return false
        }
      }
    }
    for (index in 0 until node1.childNodes.length) {
      node1.childNodes[index]?.also { child1 ->
        node2.childNodes[index]?.also { child2 ->
          if (!nodesEqual(child1, child2)) {
            return false
          }
        }
      }
    }
  }
  return true
}

class TestUpdate {

  @Test
  fun testCompare1() {
    val dom1 = HtmlBuilder.create {
      div {
        div(classes = "bla") {
          span {
            +" Some Text "
          }
          table {
            tr {
              td {
                +"Table column"
              }
            }
          }
        }
      }
    }

    val dom2 = HtmlBuilder.create {
      div {
        span {
          id = "123"

          +"New dom!"
        }
        input {
          value = "bla"
        }
      }
    }

    DiffPatch.updateNode(dom1, dom2)

    assertTrue(nodesEqual(dom1, dom2), "Updated dom not equal to original")
  }

  @Test
  fun testCompare2() {
    val dom1 = HtmlBuilder.create {
      div {
        div(classes = "bla") {
          span {
            +" Some Text "
          }
          table {
            tr {
              th {
                + "Header"
              }
            }
            tr {
              td {
                +"Table column"
              }
            }
          }
        }
      }
    }

    val dom2 = HtmlBuilder.create {
      div {
        div {
          span {
            + "Other text"
          }
        }
        span {
          id = "123"

          +"New dom!"
        }
        input {
          value = "bla"

          onClickFunction = {
            println("Clickerdyclick!")
          }
        }
      }
    }

    Komponent.logReplaceEvent = true
    DiffPatch.updateNode(dom1, dom2)

    assertTrue(nodesEqual(dom1, dom2), "Updated dom not equal to original")
  }

}
