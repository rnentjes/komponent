@file:OptIn(ExperimentalStdlibApi::class)

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
import org.w3c.dom.events.Event
import org.w3c.dom.get

private var currentElement: Element? = null

interface HtmlConsumer : TagConsumer<Element> {
  fun append(node: Element)
  fun include(komponent: Komponent)
  fun debug(block: HtmlConsumer.() -> Unit)
}

fun FlowOrMetaDataOrPhrasingContent.currentElement(): Element =
  currentElement ?: error("No current element defined!")

private data class ElementIndex(
  val parent: Node,
  var childIndex: Int,
  var setAttr: MutableSet<String> = mutableSetOf()
)

private fun ArrayList<ElementIndex>.currentParent(): Node {
  this.lastOrNull()?.let {
    return it.parent
  }

  throw IllegalStateException("currentParent should never be null!")
}

private fun ArrayList<ElementIndex>.currentElement(): Node? {
  this.lastOrNull()?.let {
    return it.parent.childNodes[it.childIndex]
  }

  return null
}

private fun ArrayList<ElementIndex>.nextElement() {
  this.lastOrNull()?.let {
    it.setAttr.clear()
    it.childIndex++
  }
}

private fun ArrayList<ElementIndex>.pop() {
  this.removeLast()
}

private fun ArrayList<ElementIndex>.push(element: Node) {
  this.add(ElementIndex(element, 0))
}

private fun ArrayList<ElementIndex>.replace(new: Node) {
  if (this.currentElement() != null) {
    this.currentElement()?.parentElement?.replaceChild(new, this.currentElement()!!)
  } else {
    this.last().parent.appendChild(new)
  }
}

private fun Node.asElement() = this as? HTMLElement

class HtmlBuilder(
  private val komponent: Komponent?,
  parent: Element,
  childIndex: Int = 0,
) : HtmlConsumer {
  private var currentPosition = arrayListOf<ElementIndex>()
  private var inDebug = false
  private var exceptionThrown = false
  private var currentNode: Node? = null
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
        console.log("Skipped include $komponent, memoize hasn't changed")
      }
    } else {
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
    logReplace { "onTagStart, [${tag.tagName}, ${tag.namespace}], currentPosition: $currentPosition" }
    currentNode = currentPosition.currentElement()

    if (currentNode == null) {
      logReplace { "onTagStart, currentNode1: $currentNode" }
      currentNode = if (tag.namespace != null) {
        document.createElementNS(tag.namespace, tag.tagName)
      } else {
        document.createElement(tag.tagName)
      }

      //logReplace"onTagStart, currentElement1.1: $currentNode")
      currentPosition.currentParent().appendChild(currentNode!!)
    } else if (
      Komponent.updateMode.isReplace ||
      (
          !currentNode?.asElement()?.tagName.equals(tag.tagName, true) ||
              (
                  tag.namespace != null &&
                      !currentNode?.asElement()?.namespaceURI.equals(tag.namespace, true)
                  )
          )
    ) {
      logReplace { "onTagStart, currentElement, namespace: ${currentNode?.asElement()?.namespaceURI} -> ${tag.namespace}" }
      logReplace { "onTagStart, currentElement, replace: ${currentNode?.asElement()?.tagName} -> ${tag.tagName}" }

      currentNode = if (tag.namespace != null) {
        document.createElementNS(tag.namespace, tag.tagName)
      } else {
        document.createElement(tag.tagName)
      }

      currentPosition.replace(currentNode!!)
    }

    check(currentNode == currentPosition.currentElement())

    currentElement = currentNode as? Element ?: currentElement

    if (currentNode is Element) {
      if (root == null) {
        //logReplace"Setting root: $currentNode")
        root = currentNode as Element
      }

      if (Komponent.updateMode.isReplace) {
        currentElement?.clearKompEvents()
      }

      for (entry in tag.attributesEntries) {
        currentElement!!.setKompAttribute(entry.key, entry.value)
        console.log("onTagStart - set attribute", entry.key)
        currentPosition.lastOrNull()?.setAttr?.add(entry.key)
      }

      if (tag.namespace != null) {
        //logReplace"onTagStart, same node type")

        (currentNode as? Element)?.innerHTML = ""
      }
    }

    currentPosition.push(currentNode!!)
  }

  private fun checkTag(tag: Tag) {
    check(currentElement != null) {
      js("debugger")
      "No current tag"
    }
    check(currentElement?.tagName.equals(tag.tagName, ignoreCase = true)) {
      js("debugger")
      "Wrong current tag"
    }
  }

  override fun onTagAttributeChange(tag: Tag, attribute: String, value: String?) {
    logReplace { "onTagAttributeChange, ${tag.tagName} [$attribute, $value]" }

    if (Komponent.enableAssertions) {
      checkTag(tag)
    }

    currentElement?.setKompAttribute(attribute, value)
    if (value == null || value.isEmpty()) {
      currentPosition.lastOrNull()?.setAttr?.remove(attribute)
    } else {
      console.log("onTagAttributeChange - set attribute", attribute)
      currentPosition.lastOrNull()?.setAttr?.add(attribute)
    }
  }

  override fun onTagEvent(tag: Tag, event: String, value: (Event) -> Unit) {
    logReplace { "onTagEvent, ${tag.tagName} [$event, $value]" }

    if (Komponent.enableAssertions) {
      checkTag(tag)
    }

    currentElement?.setKompEvent(event.lowercase(), value)
  }

  override fun onTagEnd(tag: Tag) {
    if (exceptionThrown) {
      return
    }

    while (currentPosition.currentElement() != null) {
      currentPosition.currentElement()?.let {
        it.parentElement?.removeChild(it)
      }
    }

    if (Komponent.enableAssertions) {
      checkTag(tag)
    }

    currentPosition.pop()

    currentNode = currentPosition.currentElement()
    currentElement = currentNode as? Element ?: currentElement

    if (currentElement != null) {
      val setAttrs: Set<String> = currentPosition.lastOrNull()?.setAttr ?: setOf()

      console.log("onTagEnd - set attr:", setAttrs.joinToString(","))

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
              console.log("remove attr", attr)
              if (element is HTMLInputElement) {
                when (attr) {
                  "checked" -> {
                    element.checked = false
                  }
                  "value" -> {
                    element.value = ""
                  }
                  else -> {
                    element.removeAttribute(attr)
                  }
                }
              } else {
                element.removeAttribute(attr)
              }
            }
          }
        }
      }
    }

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
        (Komponent.unsafeMode == UnsafeMode.UNSAFE_SVG_ONLY && namespace == "http://www.w3.org/2000/svg")
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

  override fun onTagError(tag: Tag, exception: Throwable) {
    exceptionThrown = true

    if (exception !is KomponentException) {
      console.log("onTagError", tag, exception)
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
    return root ?: throw IllegalStateException("We can't finalize as there was no tags")
  }

  companion object {
    fun create(content: HtmlBuilder.() -> Unit): Element {
      val container = document.createElement("div") as HTMLElement
      val consumer = HtmlBuilder(null, container, 0)
      content.invoke(consumer)
      return consumer.root ?: error("No root element found after render!")
    }
  }
}
