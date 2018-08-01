package nl.astraeus.komp

import kotlinx.html.HtmlBlockTag
import org.w3c.dom.HTMLElement
import org.w3c.dom.Node
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

fun HtmlBlockTag.include(component: Komponent) {
  val consumer = this.consumer
  if (consumer is KompConsumer) {
    consumer.appendKomponent(component)
  }

/*
  val kc = this.consumer
  val result = component.render(kc as KompConsumer)
  val element = result.create()

  component.element = element
  Komponent.define(element, component, result)
*/
}

abstract class Komponent {
  var element: Node? = null
  var kompElement: KompElement? = null
  var rendered = false

  open fun create(): KompElement {
    val result = render(KompConsumer())

    return result
  }

  abstract fun render(consumer: KompConsumer): KompElement

  open fun refresh() {
    if (!rendered) {
      refresh(element)
    } else {
      update()
    }
  }

  open fun update() {
    refresh(element)
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || this::class.js != other::class.js) return false

    other as Komponent

    if (kompElement != other.kompElement) return false
    if (rendered != other.rendered) return false

    return true
  }

  override fun hashCode(): Int {
    var result = kompElement?.hashCode() ?: 0
    result = 31 * result + rendered.hashCode()
    return result
  }

  companion object {

    private val elements: MutableMap<Node, Komponent> = HashMap()

    fun replaceNode(newKomponent: KompElement, oldElement: Node): Node {
      val newElement = newKomponent.create()

      val parent = oldElement.parentElement ?: throw IllegalStateException("oldElement has no parent! $oldElement")

      parent.replaceChild(newElement, oldElement)

      elements[oldElement]?.also {
        elements.remove(oldElement)
      }

      newKomponent.komponent?.also {
        it.element = newElement

        elements[newElement] = it
      }

      return newElement
    }

    fun removeElement(element: Node) {
      val parent = element.parentElement ?: throw IllegalArgumentException("Element has no parent!?")

      parent.removeChild(element)

      elements.remove(element)
    }

    fun appendElement(element: Node, kompElement: KompElement) {
      element.appendChild(kompElement.create())
    }

    fun define(element: Node, component: Komponent) {
      elements[element] = component
    }

    fun create(parent: HTMLElement, component: Komponent, insertAsFirst: Boolean = false) {
      component.kompElement = component.create()
      val element = component.kompElement?.create()

      if (element != null) {
        if (insertAsFirst && parent.childElementCount > 0) {
          parent.insertBefore(element, parent.firstChild)
        } else {
          parent.appendChild(element)
        }

        component.element = element
        elements[element] = component
      }
    }

    fun remove(element: Node) {
      val component = elements[element]

      elements.remove(element)
    }

    @JsName("remove")
    fun remove(component: Komponent) {
      for ((key, value) in elements) {
        if (value == component) {
          elements.remove(key)
        }
      }
    }

    fun refresh(component: Komponent) {
      refresh(component.element)
    }

    fun refresh(element: Node?) {
      if (element != null) {
        elements[element]?.let {
          //val parent = element.parentElement
          val newElement = it.create()
          val kompElement = it.kompElement

          val replacedElement = if (kompElement != null) {
            DomDiffer.replaceDiff(kompElement, newElement, element)
          } else {
            newElement.create()
          }

          it.kompElement = newElement
          it.element = replacedElement

          elements.remove(element)
          elements[replacedElement] = it

          it.rendered = true
        }
      }
    }
  }
}
