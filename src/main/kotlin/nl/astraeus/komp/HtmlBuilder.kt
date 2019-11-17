package nl.astraeus.komp

import kotlinx.html.DefaultUnsafe
import kotlinx.html.Entities
import kotlinx.html.Tag
import kotlinx.html.TagConsumer
import kotlinx.html.Unsafe
import org.w3c.dom.Document
import org.w3c.dom.HTMLElement
import org.w3c.dom.Node
import org.w3c.dom.asList
import org.w3c.dom.events.Event

@Suppress("NOTHING_TO_INLINE")
private inline fun HTMLElement.setEvent(name: String, noinline callback : (Event) -> Unit) : Unit {
  asDynamic()[name] = callback
}

interface HtmlConsumer : TagConsumer<HTMLElement> {
  fun append(node: Node)
}

class HtmlBuilder(
    val komponent: Komponent,
    val document : Document
) : HtmlConsumer {
  private val path = arrayListOf<HTMLElement>()
  private var lastLeaved : HTMLElement? = null

  override fun onTagStart(tag: Tag) {
    val element: HTMLElement = when {
      tag.namespace != null -> document.createElementNS(tag.namespace!!, tag.tagName).asDynamic()
      else -> document.createElement(tag.tagName) as HTMLElement
    }

    if (path.isNotEmpty()) {
      path.last().appendChild(element)
    }

    path.add(element)
  }

  override fun onTagAttributeChange(tag: Tag, attribute: String, value: String?) {
    when {
      path.isEmpty() -> throw IllegalStateException("No current tag")
      path.last().tagName.toLowerCase() != tag.tagName.toLowerCase() -> throw IllegalStateException("Wrong current tag")
      else -> path.last().let { node ->
        if (value == null) {
          node.removeAttribute(attribute)
        } else {
          node.setAttribute(attribute, value)
        }
      }
    }
  }

  override fun onTagEvent(tag: Tag, event: String, value: (Event) -> Unit) {
    when {
      path.isEmpty() -> throw IllegalStateException("No current tag")
      path.last().tagName.toLowerCase() != tag.tagName.toLowerCase() -> throw IllegalStateException("Wrong current tag")
      else -> path.last().setEvent(event, value)
    }
  }

  override fun onTagEnd(tag: Tag) {
    if (path.isEmpty() || path.last().tagName.toLowerCase() != tag.tagName.toLowerCase()) {
      throw IllegalStateException("We haven't entered tag ${tag.tagName} but trying to leave")
    }

    val element = path.last()

    tag.attributesEntries.forEach {
      if (it.key == "class") {
        val classes = it.value.split(" ")
        val classNames = StringBuilder()

        for (cls in classes) {
          val cssStyle = komponent.declaredStyles[cls]

          if (cssStyle != null) {
            for (index in 0 until cssStyle.length) {
              val propertyName = cssStyle.item(index)
              element.style.setProperty(propertyName, cssStyle.getPropertyValue(propertyName))
            }
          } else {
            classNames.append(cls)
            classNames.append(" ")
          }
        }

        element.className = classNames.toString()
      } else {
        element.setAttribute(it.key, it.value)
      }
    }

    lastLeaved = path.removeAt(path.lastIndex)
  }

  override fun onTagContent(content: CharSequence) {
    if (path.isEmpty()) {
      throw IllegalStateException("No current DOM node")
    }

    path.last().appendChild(document.createTextNode(content.toString()))
  }

  override fun onTagContentEntity(entity: Entities) {
    if (path.isEmpty()) {
      throw IllegalStateException("No current DOM node")
    }

    // stupid hack as browsers doesn't support createEntityReference
    val s = document.createElement("span") as HTMLElement
    s.innerHTML = entity.text
    path.last().appendChild(s.childNodes.asList().filter { it.nodeType == Node.TEXT_NODE }.first())

    // other solution would be
    //        pathLast().innerHTML += entity.text
  }

  override fun append(node: Node) {
    path.last().appendChild(node)
  }

  override fun onTagContentUnsafe(block: Unsafe.() -> Unit) {
    with(DefaultUnsafe()) {
      block()

      path.last().innerHTML += toString()
    }
  }

  override fun onTagComment(content: CharSequence) {
    if (path.isEmpty()) {
      throw IllegalStateException("No current DOM node")
    }

    path.last().appendChild(document.createComment(content.toString()))
  }

  override fun finalize(): HTMLElement = lastLeaved?.asR() ?: throw IllegalStateException("We can't finalize as there was no tags")

  @Suppress("UNCHECKED_CAST")
  private fun HTMLElement.asR(): HTMLElement = this.asDynamic()

}
