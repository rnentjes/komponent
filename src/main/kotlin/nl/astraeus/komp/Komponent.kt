package nl.astraeus.komp

import kotlinx.html.Tag
import org.w3c.dom.HTMLElement
import org.w3c.dom.Node
import kotlin.browser.document

fun Tag.include(component: Komponent) {
  component.update()

  val consumer = this.consumer
  val element = component.element

  if (consumer is HtmlBuilder && element != null) {
    consumer.append(element)
  }
}

abstract class Komponent {
  var element: Node? = null

  open fun create(): HTMLElement {
    val consumer = HtmlBuilder(document)
    val result = render(consumer)

    element = result

    return result
  }

  abstract fun render(consumer: HtmlBuilder): HTMLElement

  open fun update() = refresh()

  open fun refresh() {
    val oldElement = element
    if (logRenderEvent) {
      console.log("Rendering", this)
    }
    val newElement = create()

    if (oldElement != null) {
        if (logReplaceEvent) {
          console.log("Replacing", oldElement, newElement)
        }
        oldElement.parentNode?.replaceChild(newElement, oldElement)
    }
  }

  @JsName("remove")
  fun remove() {
    element?.let {
      val parent = it.parentElement ?: throw IllegalArgumentException("Element has no parent!?")

      if (logReplaceEvent) {
        console.log("Remove", it)
      }

      parent.removeChild(it)
    }
  }

  companion object {
    var logRenderEvent = false
    var logReplaceEvent = false

    fun create(parent: HTMLElement, component: Komponent, insertAsFirst: Boolean = false) {
      val element = component.create()

      if (insertAsFirst && parent.childElementCount > 0) {
        parent.insertBefore(element, parent.firstChild)
      } else {
        parent.appendChild(element)
      }
    }
  }
}
