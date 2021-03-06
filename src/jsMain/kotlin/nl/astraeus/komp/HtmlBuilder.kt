package nl.astraeus.komp

import kotlinx.browser.document
import kotlinx.html.*
import org.w3c.dom.*
import org.w3c.dom.css.CSSStyleDeclaration
import org.w3c.dom.events.Event

@Suppress("NOTHING_TO_INLINE")
inline fun HTMLElement.setEvent(name: String, noinline callback: (Event) -> Unit) {
  val eventName = if (name.startsWith("on")) {
    name.substring(2)
  } else {
    name
  }
  addEventListener(eventName, callback, null)
  if (Komponent.updateStrategy == UpdateStrategy.DOM_DIFF) {
    //asDynamic()[name] = callback
    val events = getAttribute(EVENT_ATTRIBUTE) ?: ""

    setAttribute(
      EVENT_ATTRIBUTE,
        if (events.isBlank()) {
          eventName
        } else {
          "$events,$eventName"
        }
    )
    asDynamic()["event-$eventName"] = callback
  }
}

interface HtmlConsumer : TagConsumer<HTMLElement> {
  fun append(node: Node)
}

fun HTMLElement.setStyles(cssStyle: CSSStyleDeclaration) {
  for (index in 0 until cssStyle.length) {
    val propertyName = cssStyle.item(index)

    style.setProperty(propertyName, cssStyle.getPropertyValue(propertyName))
  }
}

class HtmlBuilder(
    val komponent: Komponent,
    val document: Document
) : HtmlConsumer {
  private val path = arrayListOf<HTMLElement>()
  private var lastLeaved: HTMLElement? = null

  override fun onTagStart(tag: Tag) {
    val element: HTMLElement = when {
      tag.namespace != null -> document.createElementNS(tag.namespace!!, tag.tagName).asDynamic()
      else                  -> document.createElement(tag.tagName) as HTMLElement
    }

    if (path.isNotEmpty()) {
      path.last().appendChild(element)
    }

    path.add(element)
  }

  override fun onTagAttributeChange(tag: Tag, attribute: String, value: String?) {
    when {
      path.isEmpty()                                                 -> throw IllegalStateException("No current tag")
      path.last().tagName.toLowerCase() != tag.tagName.toLowerCase() -> throw IllegalStateException("Wrong current tag")
      else                                                           -> path.last().let { node ->
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
      path.isEmpty()                                                 -> throw IllegalStateException("No current tag")
      path.last().tagName.toLowerCase() != tag.tagName.toLowerCase() -> throw IllegalStateException("Wrong current tag")
      else                                                           -> path.last().setEvent(event, value)
    }
  }

  override fun onTagEnd(tag: Tag) {
    var hash = 0
    if (path.isEmpty() || path.last().tagName.toLowerCase() != tag.tagName.toLowerCase()) {
      throw IllegalStateException("We haven't entered tag ${tag.tagName} but trying to leave")
    }

    val element = path.last()

    if (Komponent.updateStrategy == UpdateStrategy.DOM_DIFF) {
      for (index in 0 until element.childNodes.length) {
        val child = element.childNodes[index]
        if (child is HTMLElement) {
          hash = hash * 37 + child.getKompHash()
        } else {
          hash = hash * 37 + (child?.textContent?.hashCode() ?: 0)
        }
      }
    }

    for ((key, value) in tag.attributesEntries) {
      if (key == "class") {
        val classes = value.split(Regex("\\s+"))
        val classNames = StringBuilder()

        for (cls in classes) {
          val cssStyle = komponent.declaredStyles[cls]

          if (cssStyle != null) {
            if (Komponent.updateStrategy == UpdateStrategy.DOM_DIFF) {
              hash = hash * 37 + cssStyle.hashCode()
            }

            if (cls.endsWith(":hover")) {
              val oldOnMouseOver = element.onmouseover
              val oldOnMouseOut = element.onmouseout

              element.onmouseover = {
                element.setStyles(cssStyle)

                oldOnMouseOver?.invoke(it)
              }
              element.onmouseout = {
                cls.split(':').firstOrNull()?.let {
                  komponent.declaredStyles[it]?.let { cssStyle ->
                    element.setStyles(cssStyle)
                  }
                }

                oldOnMouseOut?.invoke(it)
              }
            } else {
              element.setStyles(cssStyle)
            }
          } else {
            if (Komponent.updateStrategy == UpdateStrategy.DOM_DIFF) {
              hash = hash * 37 + cls.hashCode()
            }

            classNames.append(cls)
            classNames.append(" ")
          }
        }

        element.className = classNames.toString()

        if (Komponent.updateStrategy == UpdateStrategy.DOM_DIFF) {
          val key_value = "${key}-${classNames}"
          hash = hash * 37 + key_value.hashCode()
        }
      } else {
        element.setAttribute(key, value)

        if (Komponent.updateStrategy == UpdateStrategy.DOM_DIFF) {
          val key_value = "${key}-${value}"
          hash = hash * 37 + key_value.hashCode()
        }
      }


    }

    if (Komponent.updateStrategy == UpdateStrategy.DOM_DIFF) {
      element.setKompHash(hash)
      //element.setAttribute(DiffPatch.HASH_ATTRIBUTE, hash.toString(16))
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

    // stupid hack as browsers don't support createEntityReference
    val s = document.createElement("span") as HTMLElement
    s.innerHTML = entity.text
    path.last().appendChild(s.childNodes.asList().first { it.nodeType == Node.TEXT_NODE })

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

  companion object {
    fun create(content: HtmlBuilder.() -> Unit): HTMLElement {
      val consumer = HtmlBuilder(DummyKomponent(), document)
      content.invoke(consumer)
      return consumer.finalize()
    }
  }
}
