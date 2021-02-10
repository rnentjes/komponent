package nl.astraeus.komp

import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.html.div
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.Node
import org.w3c.dom.css.CSSStyleDeclaration
import kotlin.reflect.KProperty

const val KOMP_KOMPONENT = "komp-komponent"

typealias CssStyle = CSSStyleDeclaration.() -> Unit

class StateDelegate<T>(
  val komponent: Komponent,
  initialValue: T
) {
  var value: T = initialValue

  operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
    return value
  }

  operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
    if (this.value?.equals(value) != true) {
      this.value = value
      komponent.requestUpdate()
    }
  }
}

inline fun <reified T> Komponent.state(initialValue: T): StateDelegate<T> = StateDelegate(this, initialValue)

fun HtmlConsumer.include(component: Komponent) {
  if (Komponent.updateStrategy == UpdateStrategy.REPLACE) {
    if (component.element != null) {
      component.update()
    } else {
      component.refresh()
    }

    component.element?.also {
      append(it)
    }
  } else {
    append(component.create())
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
  private var createIndex = getNextCreateIndex()
  private var dirty: Boolean = true

  var element: Node? = null
  val declaredStyles: MutableMap<String, CSSStyleDeclaration> = HashMap()

  open fun create(): HTMLElement {
    val consumer = HtmlBuilder(this, document)
    consumer.render()
    val result = consumer.finalize()

    if (result.id.isBlank()) {
      result.id = "komp_${createIndex}"
    }

    element = result
    element.asDynamic()[KOMP_KOMPONENT] = this

    dirty = false

    return result
  }

  abstract fun HtmlBuilder.render()

  fun requestUpdate() {
    dirty = true
    scheduleForUpdate(this)
  }

  open fun style(className: String, vararg imports: CssStyle, block: CssStyle = {}) {
    val style = (document.createElement("div") as HTMLDivElement).style
    for (imp in imports) {
      imp(style)
    }
    block(style)
    declaredStyles[className] = style
  }

  open fun update() {
    refresh()
  }

  internal fun refresh() {
    val oldElement = element

    if (logRenderEvent) {
      console.log("Rendering", this)
    }
    val newElement = create()

    if (oldElement != null) {
      element = if (updateStrategy == UpdateStrategy.REPLACE) {
        if (logReplaceEvent) {
          console.log("Replacing", oldElement, newElement)
        }
        oldElement.parentNode?.replaceChild(newElement, oldElement)
        newElement
      } else {
        if (logReplaceEvent) {
          console.log("DomDiffing", oldElement, newElement)
        }
        DiffPatch.updateNode(oldElement, newElement)
      }
    }

    dirty = false
  }

  @JsName("remove")
  fun remove() {
    check(updateStrategy == UpdateStrategy.REPLACE) {
      "remote only works with UpdateStrategy.REPLACE"
    }
    element?.let {
      val parent = it.parentElement ?: throw IllegalArgumentException("Element has no parent!?")

      if (logReplaceEvent) {
        console.log("Remove", it)
      }

      parent.removeChild(it)
    }
  }

  companion object {
    private var nextCreateIndex: Int = 1
    private var updateCallback: Int? = null
    private var scheduledForUpdate = mutableSetOf<Komponent>()

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

    private fun getNextCreateIndex() = nextCreateIndex++

    private fun scheduleForUpdate(komponent: Komponent) {
      scheduledForUpdate.add(komponent)

      if (updateCallback == null) {
        window.setTimeout({
          runUpdate()
        }, 0)
      }
    }

    private fun runUpdate() {
      val todo = scheduledForUpdate.sortedBy { komponent -> komponent.createIndex }

      if (logRenderEvent) {
        console.log("runUpdate")
      }

      todo.forEach { next ->
        val element = next.element
        console.log("update element", element)
        if (element is HTMLElement) {
          console.log("by id", document.getElementById(element.id))
          if (document.getElementById(element.id) != null) {
            if (next.dirty) {
              if (logRenderEvent) {
                console.log("Update dirty ${next.createIndex}")
              }
              next.update()
            } else {
              if (logRenderEvent) {
                console.log("Skip ${next.createIndex}")
              }
            }
          }
        }
      }

      scheduledForUpdate.clear()
      updateCallback = null
    }
  }

}
