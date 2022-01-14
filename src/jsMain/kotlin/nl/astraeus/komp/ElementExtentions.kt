package nl.astraeus.komp

import org.w3c.dom.Element
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.events.Event
import org.w3c.dom.get

private fun Int.asSpaces(): String {
  val result = StringBuilder()

  repeat(this) {
    result.append(" ")
  }
  return result.toString()
}

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

internal fun Element.clearKompAttributes() {
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

internal fun Element.getKompAttributes(): MutableSet<String> {
  var result: MutableSet<String>? = this.asDynamic()["komp-attributes"] as MutableSet<String>?

  if (result == null) {
    result = mutableSetOf()

    this.asDynamic()["komp-attributes"] = result
  }

  return result
}

internal fun Element.setKompAttribute(name: String, value: String) {
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

internal fun Element.clearKompEvents() {
  for ((name, event) in getKompEvents()) {
    removeEventListener(name, event)
  }

  val events = this.asDynamic()["komp-events"] as MutableMap<String, (Event) -> Unit>?

  if (events == null) {
    this.asDynamic()["komp-events"] = mutableMapOf<String, (Event) -> Unit>()
  } else {
    events.clear()
  }
}

internal fun Element.setKompEvent(name: String, event: (Event) -> Unit) {
  val eventName: String = if (name.startsWith("on")) {
    name.substring(2)
  } else {
    name
  }

  val events: MutableMap<String, (Event) -> Unit> = getKompEvents()

  events[eventName]?.let {
    println("Warn event '$eventName' already defined!")
    removeEventListener(eventName, it)
  }

  events[eventName] = event

  this.asDynamic()["komp-events"] = events

  this.addEventListener(eventName, event)
}

internal fun Element.getKompEvents(): MutableMap<String, (Event) -> Unit> {
  return this.asDynamic()["komp-events"] ?: mutableMapOf()
}

