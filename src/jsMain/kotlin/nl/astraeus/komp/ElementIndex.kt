package nl.astraeus.komp

import org.w3c.dom.Node
import org.w3c.dom.get

data class ElementIndex(
  val parent: Node,
  var childIndex: Int,
  var setAttr: MutableSet<String> = mutableSetOf()
) {
  override fun toString(): String {
    return "${parent.nodeName}[$childIndex]"
  }
}

fun ArrayList<ElementIndex>.currentParent(): Node {
  this.lastOrNull()?.let {
    return it.parent
  }

  throw IllegalStateException("currentParent should never be null!")
}

fun ArrayList<ElementIndex>.currentElement(): Node? {
  this.lastOrNull()?.let {
    return it.parent.childNodes[it.childIndex]
  }

  return null
}

fun ArrayList<ElementIndex>.currentPosition(): ElementIndex? {
  return if (this.size < 2) {
    null
  } else {
    this[this.size - 2]
  }
}

fun ArrayList<ElementIndex>.nextElement() {
  this.lastOrNull()?.let {
    it.setAttr.clear()
    it.childIndex++
  }
}

fun ArrayList<ElementIndex>.pop() {
  this.removeLast()
}

fun ArrayList<ElementIndex>.push(element: Node) {
  this.add(ElementIndex(element, 0))
}

fun ArrayList<ElementIndex>.replace(new: Node) {
  if (this.currentElement() != null) {
    this.currentElement()?.parentElement?.replaceChild(
      new,
      this.currentElement()!!
    )
  } else {
    this.last().parent.appendChild(new)
  }
}
