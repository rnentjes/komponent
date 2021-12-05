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

fun Int.asSpaces(): String {
  val result = StringBuilder()
  repeat(this) {
    result.append(" ")
  }
  return result.toString()
}

fun FlowOrMetaDataOrPhrasingContent.currentElement(): Element =
  currentElement ?: error("No current element defined!")

fun Element.printTree(indent: Int = 0): String {
  val result = StringBuilder()

  result.append(indent.asSpaces())
  result.append(tagName)
  if (this.namespaceURI != "http://www.w3.org/1999/xhtml") {
    result.append(" [")
    result.append(namespaceURI)
    result.append("]")
  }
  result.append(" (")
  var first = true
  if (hasAttributes()) {
    for (index in 0 until attributes.length) {
      if (!first) {
        result.append(", ")
      } else {
        first = false
      }
      result.append(attributes[index]?.localName)
      result.append("=")
      result.append(attributes[index]?.value)
    }
  }
  result.append(") {")
  result.append("\n")
  for ((name, event) in getKompEvents()) {
    result.append(indent.asSpaces())
    result.append("on")
    result.append(name)
    result.append(" -> ")
    result.append(event)
    result.append("\n")
  }
  for (index in 0 until childNodes.length) {
    childNodes[index]?.let {
      if (it is Element) {
        result.append(it.printTree(indent + 2))
      } else {
        result.append((indent + 2).asSpaces())
        result.append(it.textContent)
        result.append("\n")
      }
    }
  }
  result.append(indent.asSpaces())
  result.append("}\n")

  return result.toString()
}

private fun Element.clearKompAttributes() {
  val attributes = this.asDynamic()["komp-attributes"] as MutableSet<String>?

  if (attributes == null) {
    this.asDynamic()["komp-attributes"] = mutableSetOf<String>()
  } else {
    attributes.clear()
  }

  if (this is HTMLInputElement) {
    this.checked = false
  }
}

private fun Element.getKompAttributes(): MutableSet<String> {
  var result: MutableSet<String>? = this.asDynamic()["komp-attributes"] as MutableSet<String>?

  if (result == null) {
    result = mutableSetOf()

    this.asDynamic()["komp-attributes"] = result
  }

  return result
}

fun Element.setKompAttribute(name: String, value: String) {
  val setAttrs: MutableSet<String> = getKompAttributes()
  setAttrs.add(name)

  if (this is HTMLInputElement) {
    when (name) {
      "checked" -> {
        this.checked = value == "checked"
      }
      "value" -> {
        this.value = value

      }
      else -> {
        setAttribute(name, value)
      }
    }
  } else if (this.getAttribute(name) != value) {
    setAttribute(name, value)
  }
}

private fun Element.clearKompEvents() {
  for ((name, event) in getKompEvents()) {
    currentElement?.removeEventListener(name, event)
  }

  val events = this.asDynamic()["komp-events"] as MutableMap<String, (Event) -> Unit>?

  if (events == null) {
    this.asDynamic()["komp-events"] = mutableMapOf<String, (Event) -> Unit>()
  } else {
    events.clear()
  }
}

private fun Element.setKompEvent(name: String, event: (Event) -> Unit) {
  val eventName: String = if (name.startsWith("on")) {
    name.substring(2)
  } else {
    name
  }

  val events: MutableMap<String, (Event) -> Unit> = getKompEvents()

  events[eventName]?.let {
    println("Warn event already defined!")
    currentElement?.removeEventListener(eventName, it)
  }

  events[eventName] = event

  this.asDynamic()["komp-events"] = events

  this.addEventListener(eventName, event)
}

private fun Element.getKompEvents(): MutableMap<String, (Event) -> Unit> {
  return this.asDynamic()["komp-events"] ?: mutableMapOf()
}

private data class ElementIndex(
  val parent: Node,
  var childIndex: Int
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
  val parent: Element,
  var childIndex: Int = 0
) : HtmlConsumer {
  private var currentPosition = arrayListOf<ElementIndex>()
  private var inDebug = false
  var currentNode: Node? = null
  var root: Element? = null
  val currentAttributes: MutableMap<String, String> = mutableMapOf()

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
    inDebug = true

    try {
      block.invoke(this)
    } finally {
      inDebug = false
    }
  }

  fun logReplace(msg: String) {
    if (Komponent.logReplaceEvent && inDebug) {
      console.log(msg)
    }
  }

  override fun onTagStart(tag: Tag) {
    logReplace("onTagStart, [${tag.tagName}, ${tag.namespace}], currentPosition: $currentPosition")
    currentNode = currentPosition.currentElement()

    if (currentNode == null) {
      logReplace("onTagStart, currentNode1: $currentNode")
      currentNode = if (tag.namespace != null) {
        document.createElementNS(tag.namespace, tag.tagName)
      } else {
        document.createElement(tag.tagName)
      }

      //logReplace"onTagStart, currentElement1.1: $currentNode")
      currentPosition.currentParent().appendChild(currentNode!!)
    } else if (
      !currentNode?.asElement()?.tagName.equals(tag.tagName, true) ||
      (
          tag.namespace != null &&
              !currentNode?.asElement()?.namespaceURI.equals(tag.namespace, true)
          )
    ) {
      logReplace("onTagStart, currentElement, namespace: ${currentNode?.asElement()?.namespaceURI} -> ${tag.namespace}")
      logReplace("onTagStart, currentElement, replace: ${currentNode?.asElement()?.tagName} -> ${tag.tagName}")
      currentNode = if (tag.namespace != null) {
        document.createElementNS(tag.namespace, tag.tagName)
      } else {
        document.createElement(tag.tagName)
      }

      currentPosition.replace(currentNode!!)
    } else {
      //logReplace"onTagStart, same node type")

    }

    currentElement = currentNode as? Element ?: currentElement

    if (currentNode is Element) {
      if (root == null) {
        //logReplace"Setting root: $currentNode")
        root = currentNode as Element
      }

      currentElement?.clearKompAttributes()
      currentElement?.clearKompEvents()

      for (entry in tag.attributesEntries) {
        currentElement!!.setKompAttribute(entry.key.lowercase(), entry.value)
      }

      if (tag.namespace != null) {
        //logReplace"onTagStart, same node type")

        (currentNode as? Element)?.innerHTML = ""
      }
    }

    //logReplace"onTagStart, currentElement2: $currentNode")

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
    logReplace("onTagAttributeChange, ${tag.tagName} [$attribute, $value]")

    checkTag(tag)

    if (value == null) {
      currentElement?.removeAttribute(attribute.lowercase())
    } else {
      currentElement?.setKompAttribute(attribute.lowercase(), value)
    }
  }

  override fun onTagEvent(tag: Tag, event: String, value: (Event) -> Unit) {
    //logReplace"onTagEvent, ${tag.tagName} [$event, $value]")

    checkTag(tag)

    currentElement?.setKompEvent(event.lowercase(), value)
  }

  override fun onTagEnd(tag: Tag) {
    while (currentPosition.currentElement() != null) {
      currentPosition.currentElement()?.let {
        it.parentElement?.removeChild(it)
      }
    }

    checkTag(tag)

    currentPosition.pop()

    val setAttrs: List<String> = currentElement.asDynamic()["komp-attributes"] ?: listOf()

    // remove attributes that where not set
    val element = currentElement
    if (element?.hasAttributes() == true) {
      for (index in 0 until element.attributes.length) {
        val attr = element.attributes[index]
        if (attr != null) {

          if (element is HTMLElement && attr.name == "data-has-focus" && "true" == attr.value) {
            element.focus()
          }

          if (attr.name != "style" && !setAttrs.contains(attr.name)) {
            if (element is HTMLInputElement) {
              if (attr.name == "checkbox") {
                element.checked = false
              } else if (attr.name == "value") {
                element.value = ""
              }
            } else {
              if (Komponent.logReplaceEvent) {
                console.log("Clear attribute [${attr.name}]  on $element)")
              }
              element.removeAttribute(attr.name)
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

  override fun finalize(): Element {
    //logReplace"finalize, currentPosition: $currentPosition")
    return root ?: throw IllegalStateException("We can't finalize as there was no tags")
  }

  companion object {
    fun create(content: HtmlBuilder.() -> Unit): Element {
      val container = document.createElement("div") as HTMLElement
      val consumer = HtmlBuilder(container, 0)
      content.invoke(consumer)
      return consumer.root ?: error("No root element found after render!")
    }
  }
}
