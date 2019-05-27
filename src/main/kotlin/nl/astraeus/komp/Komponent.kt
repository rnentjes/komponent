package nl.astraeus.komp

import kotlinx.html.HtmlBlockTag
import org.w3c.dom.HTMLElement
import org.w3c.dom.Node

fun HtmlBlockTag.include(component: Komponent) {
  val consumer = this.consumer
  if (consumer is KompConsumer) {
    consumer.appendKomponent(component)
  }
}

enum class UpdateStrategy {
  REPLACE,
  DOM_DIFF
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

  open fun refresh(forceRefresh: Boolean = false) {
    if (!rendered || forceRefresh) {
      element?.let { element ->
        if (logRenderEvent) {
          console.log("Rendering", this)
        }

        val newElement = create()

        val replacedElement = if (updateStrategy == UpdateStrategy.REPLACE) {
          //val replacedElement = replaceNode(newElement, element)

          replaceNode(newElement, element)
        } else if (kompElement != null) {
          kompElement?.let {
            DomDiffer.replaceDiff(it, newElement, element)
          }
        } else {
          newElement.create()
        }

        kompElement = newElement
        this.element = replacedElement
        rendered = true
      }
    } else {
      update()
    }
  }

  open fun update() {
    refresh(true)
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

    var logRenderEvent = false
    var logReplaceEvent = false
    var logEquals = false
    var updateStrategy = UpdateStrategy.DOM_DIFF

    fun replaceNode(newKomponent: KompElement, oldElement: Node): Node {
      val newElement = newKomponent.create()

      if (Komponent.logReplaceEvent) {
        console.log("Replace", oldElement, newElement)
      }

      val parent = oldElement.parentElement ?: throw IllegalStateException("oldElement has no parent! $oldElement")

      parent.replaceChild(newElement, oldElement)

      newKomponent.komponent?.also {
        it.kompElement = newKomponent
        it.element = newElement
      }

      return newElement
    }

    fun removeElement(element: Node) {
      val parent = element.parentElement ?: throw IllegalArgumentException("Element has no parent!?")

      if (Komponent.logReplaceEvent) {
        console.log("Remove", element)
      }

      parent.removeChild(element)
    }

    fun appendElement(element: Node, kompElement: KompElement) {
      val newElement = kompElement.create()
      if (logReplaceEvent) {
        console.log("Append", newElement)
      }
      element.appendChild(newElement)
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
      }
    }
  }
}
