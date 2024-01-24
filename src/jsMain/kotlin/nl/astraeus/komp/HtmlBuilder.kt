package nl.astraeus.komp

import kotlinx.browser.document
import kotlinx.html.DefaultUnsafe
import kotlinx.html.Entities
import kotlinx.html.FlowOrMetaDataOrPhrasingContent
import kotlinx.html.Tag
import kotlinx.html.TagConsumer
import kotlinx.html.Unsafe
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLSpanElement
import org.w3c.dom.Node
import org.w3c.dom.asList
import org.w3c.dom.get

private var currentElement: Element? = null

interface HtmlConsumer : TagConsumer<Element> {
  fun append(node: Element)
  fun include(komponent: Komponent)
  fun debug(block: HtmlConsumer.() -> Unit)
}

fun FlowOrMetaDataOrPhrasingContent.currentElement(): Element =
  currentElement ?: error("No current element defined!")

private fun Node.asElement() = this as? HTMLElement

class HtmlBuilder(
  private val komponent: Komponent?,
  parent: Element,
  childIndex: Int = 0
) : HtmlConsumer {
  private var currentPosition = arrayListOf<ElementIndex>()
  private var inDebug = false
  private var exceptionThrown = false
  private var currentNode: Node? = null
  private var firstTag: Boolean = true
  var root: Element? = null

  init {
    currentPosition.add(ElementIndex(parent, childIndex))
  }

  override fun include(komponent: Komponent) {
    if (
      komponent.element != null &&
      !komponent.memoizeChanged()
    ) {
      currentPosition.replace(komponent.element!!)
      if (Komponent.logRenderEvent) {
        console.log(
          "Skipped include $komponent, memoize hasn't changed"
        )
      }
    } else {
      // current element should become parent
/*
      val ce = komponent.element
      if (ce != null) {
        append(ce as Element)
      }
*/
      komponent.create(
        currentPosition.last().parent as Element,
        currentPosition.last().childIndex
      )
    }
    currentPosition.nextElement()
  }

  override fun append(node: Element) {
    currentPosition.replace(node)
    currentPosition.nextElement()
  }

  override fun debug(block: HtmlConsumer.() -> Unit) {
    val enableAssertions = Komponent.enableAssertions
    Komponent.enableAssertions = true
    inDebug = true

    try {
      block.invoke(this)
    } finally {
      inDebug = false
      Komponent.enableAssertions = enableAssertions
    }
  }

  private fun logReplace(msg: () -> String) {
    if (Komponent.logReplaceEvent && inDebug) {
      console.log(msg.invoke())
    }
  }

  override fun onTagStart(tag: Tag) {
    logReplace {
      "onTagStart, [${tag.tagName}, ${tag.namespace ?: ""}], currentPosition: $currentPosition"
    }

    currentNode = currentPosition.currentElement()

    if (currentNode == null) {
      logReplace { "onTagStart, currentNode1: $currentNode" }
      currentNode = if (tag.namespace != null) {
        document.createElementNS(tag.namespace, tag.tagName)
      } else {
        document.createElement(tag.tagName)
      }

      logReplace { "onTagStart, currentElement1.1: $currentNode" }
      currentPosition.currentParent().appendChild(currentNode!!)
    } else if (
      !currentNode?.asElement()?.tagName.equals(tag.tagName, true) ||
      (
          tag.namespace != null &&
              !currentNode?.asElement()?.namespaceURI.equals(tag.namespace, true)
          )
    ) {
      logReplace {
        "onTagStart, currentElement, namespace: ${currentNode?.asElement()?.namespaceURI} -> ${tag.namespace}"
      }
      logReplace {
        "onTagStart, currentElement, replace: ${currentNode?.asElement()?.tagName} -> ${tag.tagName}"
      }

      currentNode = if (tag.namespace != null) {
        document.createElementNS(tag.namespace, tag.tagName)
      } else {
        document.createElement(tag.tagName)
      }

      currentPosition.replace(currentNode!!)
    }

    currentElement = currentNode as? Element ?: currentElement

    if (currentNode is Element) {
      if (firstTag) {
        logReplace { "Setting root: $currentNode" }
        root = currentNode as Element
        firstTag = false
      }

      currentElement?.clearKompEvents()

      // if currentElement = checkbox make sure it's cleared
      (currentElement as? HTMLInputElement)?.checked = false

      currentPosition.lastOrNull()?.setAttr?.clear()
      for (entry in tag.attributesEntries) {
        currentElement!!.setKompAttribute(entry.key, entry.value)
        currentPosition.lastOrNull()?.setAttr?.add(entry.key)
      }
    }

    currentPosition.push(currentNode!!)
  }

  private fun checkTag(source: String, tag: Tag) {
    check(currentElement != null) {
      js("debugger;")
      "No current tag ($source)"
    }
    check(currentElement?.tagName.equals(tag.tagName, ignoreCase = true)) {
      js("debugger;")
      "Wrong current tag ($source), got: ${tag.tagName} expected ${currentElement?.tagName}"
    }
  }

  override fun onTagAttributeChange(
    tag: Tag,
    attribute: String,
    value: String?
  ) {
    logReplace { "onTagAttributeChange, ${tag.tagName} [$attribute, $value]" }

    if (Komponent.enableAssertions) {
      checkTag("onTagAttributeChange", tag)
    }

    currentElement?.setKompAttribute(attribute, value)
    if (value == null || value.isEmpty()) {
      currentPosition.currentPosition()?.setAttr?.remove(attribute)
    } else {
      currentPosition.currentPosition()?.setAttr?.add(attribute)
    }
  }

  override fun onTagEvent(
    tag: Tag,
    event: String,
    value: (kotlinx.html.org.w3c.dom.events.Event) -> Unit
  ) {
    logReplace { "onTagEvent, ${tag.tagName} [$event, $value]" }

    if (Komponent.enableAssertions) {
      checkTag("onTagEvent", tag)
    }

    currentElement?.setKompEvent(event.lowercase(), value.asDynamic())
  }

  override fun onTagEnd(tag: Tag) {
    logReplace {
      "onTagEnd, [${tag.tagName}, ${tag.namespace}], currentPosition: $currentPosition"
    }

    if (exceptionThrown) {
      return
    }

    while (currentPosition.currentElement() != null) {
      currentPosition.currentElement()?.let {
        it.parentElement?.removeChild(it)
      }
    }

    if (Komponent.enableAssertions) {
      checkTag("onTagEnd", tag)
    }

    if (currentElement != null) {
      val setAttrs: Set<String> = currentPosition.currentPosition()?.setAttr ?: setOf()

      // remove attributes that where not set
      val element = currentElement
      if (element?.hasAttributes() == true) {
        for (index in 0 until element.attributes.length) {
          val attribute = element.attributes[index]
          if (attribute?.name != null) {
            val attr = attribute.name

            if (
              !setAttrs.contains(attr) &&
              attr != "style"
            ) {
              element.setKompAttribute(attr, null)
            }
          }
        }
      }
    }

    currentPosition.pop()

    currentNode = currentPosition.currentElement()
    currentElement = currentNode as? Element ?: currentElement

    currentPosition.nextElement()

    currentElement = currentElement?.parentElement as? HTMLElement

    //logReplace"onTagEnd, popped: $currentElement")
  }

  override fun onTagContent(content: CharSequence) {
    //logReplace"onTagContent, [$content]")

    check(currentElement != null) {
      "No current DOM node"
    }

    //logReplace"Tag content: $content")
    if (
      currentElement?.nodeType != Node.TEXT_NODE ||
      currentElement?.textContent != content.toString()
    ) {
      currentElement?.textContent = content.toString()
    }

    currentPosition.nextElement()
  }

  override fun onTagContentEntity(entity: Entities) {
    //logReplace"onTagContentEntity, [${entity.text}]")

    check(currentElement != null) {
      "No current DOM node"
    }

    val s = document.createElement("span") as HTMLSpanElement
    s.innerHTML = entity.text
    currentPosition.replace(
      s.childNodes.asList().firstOrNull() ?: document.createTextNode(entity.text)
    )
    currentPosition.nextElement()
  }

  override fun onTagContentUnsafe(block: Unsafe.() -> Unit) {
    with(DefaultUnsafe()) {
      block()

      val textContent = toString()

      //logReplace"onTagContentUnsafe, [$textContent]")

      var namespace: String? = null

      if (currentPosition.currentParent().nodeType == 1.toShort()) {
        val element = currentPosition.currentParent() as Element

        namespace = when (Komponent.unsafeMode) {
          UnsafeMode.UNSAFE_ALLOWED -> {
            element.namespaceURI
          }
          UnsafeMode.UNSAFE_SVG_ONLY -> {
            if (element.namespaceURI == "http://www.w3.org/2000/svg") {
              element.namespaceURI
            } else {
              null
            }
          }
          else -> {
            null
          }
        }
      }

      //logReplace"onTagContentUnsafe, namespace: [$namespace]")

      if (Komponent.unsafeMode == UnsafeMode.UNSAFE_ALLOWED ||
            (
              Komponent.unsafeMode == UnsafeMode.UNSAFE_SVG_ONLY &&
              namespace == "http://www.w3.org/2000/svg"
            )
      ) {
        if (currentElement?.innerHTML != textContent) {
          currentElement?.innerHTML += textContent
        }
      } else if (currentElement?.textContent != textContent) {
        currentElement?.textContent = textContent
      }

      currentPosition.nextElement()
    }
  }

  override fun onTagComment(content: CharSequence) {
    //logReplace"onTagComment, [$content]")

    check(currentElement != null) {
      "No current DOM node"
    }
    currentElement?.appendChild(
      document.createComment(content.toString())
    )

    currentPosition.nextElement()
  }

  fun onTagError(tag: Tag, exception: Throwable) {
    exceptionThrown = true

    if (exception !is KomponentException) {
      val position = mutableListOf<Element>()
      var ce = currentElement
      while (ce != null) {
        position.add(ce)
        ce = ce.parentElement
      }
      val builder = StringBuilder()
      for (element in position.reversed()) {
        builder.append("> ")
        builder.append(element.tagName)
        builder.append("[")
        builder.append(element.findElementIndex())
        builder.append("]")
        if (element.hasAttribute("class")) {
          builder.append("(")
          builder.append(element.getAttribute("class"))
          builder.append(")")
        }
        builder.append(" ")
      }

      throw KomponentException(
        komponent,
        currentElement,
        tag,
        builder.toString(),
        exception.message ?: "error",
        exception
      )
    } else {
      throw exception
    }
  }

  override fun finalize(): Element {
    //logReplace"finalize, currentPosition: $currentPosition")
    return root ?: throw IllegalStateException(
      "We can't finalize as there was no tags"
    )
  }

  companion object {
    fun create(content: HtmlBuilder.() -> Unit): Element {
      val container = document.createElement("div") as HTMLElement
      val consumer = HtmlBuilder(null, container)
      content.invoke(consumer)
      return consumer.root ?: error("No root element found after render!")
    }
  }
}
