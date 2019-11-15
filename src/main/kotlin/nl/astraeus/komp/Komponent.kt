package nl.astraeus.komp

import kotlinx.html.HtmlBlockTag
import kotlinx.html.TagConsumer
import kotlinx.html.dom.create
import org.w3c.dom.HTMLElement
import org.w3c.dom.Node
import kotlin.browser.document

fun HtmlBlockTag.include(component: Komponent) {
  component.element = component.render(this.consumer as TagConsumer<HTMLElement>)
}

abstract class Komponent {
  var element: Node? = null

  open fun create(): HTMLElement {
    val result = render(document.create)

    element = result

    return result
  }

  abstract fun render(consumer: TagConsumer<HTMLElement>): HTMLElement

  open fun refresh() {
    if (element == null) {
      console.log("Unable to refresh, element == null", this)
    }
    element?.let { element ->
      if (logRenderEvent) {
        console.log("Rendering", this)
      }

      val oldElement = element
      val newElement = create()

      if (logReplaceEvent) {
        console.log("Replacing", oldElement, newElement)
      }
      element.parentNode?.replaceChild(newElement, oldElement)
    }
  }

  open fun update() {
    refresh()
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
