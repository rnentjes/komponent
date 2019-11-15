package nl.astraeus.komp

import kotlinx.html.HtmlBlockTag
import kotlinx.html.TagConsumer
import kotlinx.html.dom.create
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.Node
import org.w3c.dom.css.CSSStyleDeclaration
import kotlin.browser.document

fun HtmlBlockTag.include(component: Komponent) {
  component.render(this.consumer as TagConsumer<HTMLElement>)
}

enum class UpdateStrategy {
  REPLACE,
  DOM_DIFF
}

abstract class Komponent {
  var element: Node? = null
  val declaredStyles: MutableMap<String, CSSStyleDeclaration> = HashMap()

  open fun create(): HTMLElement {
    val result = render(document.create)

    return result
  }

  abstract fun render(consumer: TagConsumer<HTMLElement>): HTMLElement

  open fun declareStyle(className: String, block: CSSStyleDeclaration.() -> Unit) {
    val style = (document.createElement("div") as HTMLDivElement).style
    block(style)
    declaredStyles[className] = style
  }

  open fun refresh() {
    element?.let { element ->
      if (logRenderEvent) {
        console.log("Rendering", this)
      }

      val newElement = create()

      element.parentNode?.replaceChild(newElement, element)

      this.element = newElement
    }
  }

  open fun update() {
    refresh()
  }

  companion object {

    var logRenderEvent = false
    var logReplaceEvent = false
    var logEquals = false
    var updateStrategy = UpdateStrategy.DOM_DIFF

    fun removeElement(element: Node) {
      val parent = element.parentElement ?: throw IllegalArgumentException("Element has no parent!?")

      if (logReplaceEvent) {
        console.log("Remove", element)
      }

      parent.removeChild(element)
    }

    fun create(parent: HTMLElement, component: Komponent, insertAsFirst: Boolean = false) {
      val element = component.create()

      if (insertAsFirst && parent.childElementCount > 0) {
        parent.insertBefore(element, parent.firstChild)
      } else {
        parent.appendChild(element)
      }

      component.element = element
    }
  }
}
