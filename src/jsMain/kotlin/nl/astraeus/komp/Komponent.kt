package nl.astraeus.komp

import kotlinx.html.Tag
import kotlinx.html.div
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.Node
import org.w3c.dom.css.CSSStyleDeclaration
import kotlin.browser.document

public typealias CssStyle = CSSStyleDeclaration.() -> Unit

fun Tag.include(component: Komponent) {
  if (component.element != null) {
    component.update()
  } else {
    component.refresh()
  }

  val consumer = this.consumer
  val element = component.element

  if (consumer is HtmlBuilder && element != null) {
    consumer.append(element)
  }
}

class DummyKomponent: Komponent() {
  override fun HtmlBuilder.render() {
    div {
      + "dummy"
    }
  }
}

enum class UpdateStrategy {
  REPLACE,
  DOM_DIFF
}

abstract class Komponent {
  var element: Node? = null
  val declaredStyles: MutableMap<String, CSSStyleDeclaration> = HashMap()

  open fun create(): HTMLElement {
    val consumer = HtmlBuilder(this, document)
    consumer.render()
    val result = consumer.finalize()

    if (logReplaceEvent) {
      console.log("Element hash", result, result.getKompHash())
    }

    element = result

    return result
  }

  abstract fun HtmlBuilder.render()

  open fun style(className: String, vararg imports: CssStyle, block: CssStyle = {}) {
    val style = (document.createElement("div") as HTMLDivElement).style
    for (imp in imports) {
      imp(style)
    }
    block(style)
    declaredStyles[className] = style
  }

  open fun update() = refresh()

  open fun refresh() {
    val oldElement = element
    if (logRenderEvent) {
      console.log("Rendering", this)
    }
    val newElement = create()

    if (oldElement != null) {
      if (updateStrategy == UpdateStrategy.REPLACE) {
        if (logReplaceEvent) {
          console.log("Replacing", oldElement, newElement)
        }
        oldElement.parentNode?.replaceChild(newElement, oldElement)
        element = newElement
      } else {
        if (logReplaceEvent) {
          console.log("DomDiffing", oldElement, newElement)
        }
        element = DiffPatch.updateNode(oldElement, newElement)
      }
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
    var updateStrategy = UpdateStrategy.DOM_DIFF

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
