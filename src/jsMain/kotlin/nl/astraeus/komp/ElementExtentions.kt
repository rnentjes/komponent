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

internal fun Element.setKompAttribute(name: String, value: String?) {
//  val setAttrs: MutableSet<String> = getKompAttributes()
//  setAttrs.add(name)
  //getNewAttributes().add(name)

  if (value == null || value.isBlank()) {
    if (this is HTMLInputElement) {
      checked = false
    } else {
      removeAttribute(name)
    }
  } else {
    if (this is HTMLInputElement) {
      when (name) {
        "checked" -> {
          checked = "checked" == value
        }
        "class" -> {
          className = value
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
}

internal fun Element.setKompEvent(name: String, event: (Event) -> Unit) {
  val eventName: String = if (name.startsWith("on")) {
    name.substring(2)
  } else {
    name
  }

  this.addEventListener(eventName, event)
}

internal fun Element.findElementIndex(): Int {
  val childNodes = parentElement?.children
  if (childNodes != null) {
    for (index in 0 until childNodes.length) {
      if (childNodes[index] == this) {
        return index
      }
    }
  }

  return 0
}
