package nl.astraeus.komp

import kotlinx.html.Entities
import kotlinx.html.Tag
import kotlinx.html.TagConsumer
import kotlinx.html.Unsafe
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.hr
import kotlinx.html.js.onClickFunction
import kotlinx.html.span
import org.w3c.dom.Node
import org.w3c.dom.events.Event
import org.w3c.dom.get
import kotlin.browser.document
import kotlin.dom.isElement

/**
 * User: rnentjes
 * Date: 8-5-18
 * Time: 21:58
 */

enum class ElementType {
  KOMPONENT,
  TAG,
  TEXT,
  UNSAFE,
}

class KompElement(
    val type: ElementType,
    val komponent: Komponent?,
    var text: String,
    val attributes: MutableMap<String, String>? = null,
    val children: MutableList<KompElement>? = null,
    val events: MutableMap<String, (Event) -> Unit>? = null
) {

  constructor(text: String, type: ElementType) : this(
      type,
      if (type == ElementType.KOMPONENT) {
        throw IllegalStateException("Type KOMPONENT not allowed in String constructor")
      } else {
        null
      },
      text,
      if (type == ElementType.TAG) {
        HashMap()
      } else {
        null
      },
      if (type == ElementType.TAG) {
        ArrayList()
      } else {
        null
      },
      if (type == ElementType.TAG) {
        HashMap()
      } else {
        null
      }
  )

  constructor(komponent: Komponent) : this(
      ElementType.KOMPONENT,
      komponent,
      "",
      null,
      null,
      null
  )

  /* shallow equals check */
  fun equals(other: KompElement): Boolean {
    if (komponent != null) {
      val result = komponent == other.komponent
      if (!result && Komponent.logEquals) {
        console.log("!= komponent", this, other)
      }
      return result
    } else if (other.isText() || isText()) {
      if (other.text != text && Komponent.logEquals) {
        console.log("!= text", this, other)
      }
      return other.text == text
    } else {
      if (other.attributes?.size != attributes?.size || other.events?.size != events?.size) {
        if (Komponent.logEquals) {
          console.log("!= attr size or event size", this, other)
        }
        return false
      } else {
        (attributes?.entries)?.forEach { entry ->
          if (!other.attributes?.get(entry.key).equals(entry.value)) {
            if (Komponent.logEquals) {
              console.log("!= attr", this, other)
            }
            return false
          }
        }
        (events?.entries)?.forEach { entry ->
          val thisFunction: dynamic = other.events?.get(entry.key)
          val otherFunction: dynamic = entry.value

          if (thisFunction != null && thisFunction.callableName != undefined) {
            val result = thisFunction.callableName == otherFunction.callableName
            if (!result && Komponent.logEquals) {
              console.log("!= event", thisFunction, otherFunction)
            }
            return result
          }

          if (Komponent.logEquals) {
            console.log("!= event, events have no callableName", thisFunction, otherFunction)
          }
          return false
        }
      }
    }

    return true
  }

  fun isText(): Boolean {
    return type == ElementType.TEXT
  }

  fun isKomponent(): Boolean {
    return type == ElementType.KOMPONENT
  }

  fun create(svg: Boolean = false): Node = when(type) {
    ElementType.KOMPONENT -> {
      val komp = komponent

      if (komp == null) {
        throw IllegalStateException("komponent == null in type Komponent!")
      } else {
        komp.element?.also {
          Komponent.remove(it)
        }

        val kompElement = komp.create()
        val element = kompElement.create()

        komp.kompElement = kompElement
        komp.element = element

        Komponent.define(element, komp)

        element
      }
    }
    ElementType.TEXT -> document.createTextNode(text)
    ElementType.UNSAFE -> {
      val div = if (svg) {
        document.createElementNS("http://www.w3.org/2000/svg","svg")
      } else {
        document.createElement("div")
      }
      var result: Node? = null

      div.innerHTML = text

      console.log("div element with unsafe innerHTML", div)

      for (index in 0 until div.childNodes.length) {
        val node = div.childNodes[index]!!

        console.log("$index -> ", node)

        if (node.isElement) {
          if (result != null) {
            throw IllegalStateException("Only one element allowed in unsafe block!")
          }
          result = node
        }
      }

      result ?: throw IllegalStateException("No element found in unsafe content! [$text]")
    }
    ElementType.TAG -> {
      var svg = text == "svg"
      val result = if (svg) {
        document.createElementNS("http://www.w3.org/2000/svg", text)
      } else {
        document.createElement(text)
      }

      (attributes?.entries)?.forEach { entry ->
        result.setAttribute(entry.key, entry.value)
      }

      (events?.entries)?.forEach { event ->
        val key = if (event.key.startsWith("on")) {
          event.key.substring(2)
        } else {
          event.key
        }
        result.addEventListener(key, event.value)
      }

      children?.forEach { child ->
        result.append(child.create(svg))
      }

      result
    }
  }

  override fun toString(): String {
    return this.toString("")
  }

  fun toString(indent: String = ""): String {
    val result = StringBuilder()

    if (attributes != null) {
      result.append(indent)
      result.append("<")
    }
    result.append(text)
    if (attributes != null) {
      for (entry in attributes.entries) {
        result.append("\n")
        result.append(indent)
        result.append(indent)
        result.append(entry.key)
        result.append("=\"")
        result.append(entry.value)
        result.append("\"")
      }
      events?.apply {
        for (event in this.entries) {
          result.append("\n")
          result.append(indent)
          result.append(indent)
          result.append(event.key)
          result.append("=")
          result.append(event.value)
        }
      }
      result.append("\n")
      result.append(indent)
      result.append(">")

      children?.apply {
        result.append("\n")
        result.append(indent)
        for (child in this) {
          result.append(child.toString("  $indent"))
        }
      }

      result.append("\n")
      result.append(indent)
      result.append("</")
      result.append(text)
      result.append(">")
    }

    return result.toString()
  }
}

class UnsafeWrapper: Unsafe {
  var text = ""

  override fun String.unaryPlus() {
    text += this@unaryPlus
  }
}

class KompConsumer : TagConsumer<KompElement> {
  val stack = ArrayList<KompElement>()
  var currentTag: KompElement? = null

  override fun finalize(): KompElement {
    return currentTag!!
  }

  override fun onTagAttributeChange(tag: Tag, attribute: String, value: String?) {
    //console.log("KC.onTagAttributeChange", tag, attribute, value)
    if (value != null) {
      currentTag?.attributes?.put(attribute, value)
    }
  }

  override fun onTagContent(content: CharSequence) {
    //console.log("KC.onTagContent", content)

    currentTag?.children?.add(KompElement(content.toString(), ElementType.TEXT))
  }

  override fun onTagContentEntity(entity: Entities) {
    console.log("KC.onTagContentEntity", entity)
  }

  override fun onTagContentUnsafe(block: Unsafe.() -> Unit) {
    //console.log("KC.onTagContentUnsafe", block)
    val txt = UnsafeWrapper()

    block.invoke(txt)

    console.log("KC.onTagContentUnsafe", txt)
    currentTag?.children?.add(KompElement(txt.text, ElementType.UNSAFE))
  }

  override fun onTagEnd(tag: Tag) {
    console.log("KC.onTagEnd", tag)

    check(currentTag != null)
    check(currentTag?.children != null)

    val ke = currentTag
    if (stack.isNotEmpty()) {
      currentTag = stack.removeAt(stack.lastIndex)

      if (ke != null) {
        currentTag?.children?.add(ke)
      }
    }
  }

  override fun onTagEvent(tag: Tag, event: String, value: (Event) -> Unit) {
    //console.log("KC.onTagEvent", tag, event, value)

    currentTag?.events?.put(event, value)
  }

  override fun onTagStart(tag: Tag) {
    console.log("KC.onTagStart", tag)

    currentTag?.apply {
      stack.add(this)
    }

    currentTag = KompElement(tag.tagName, ElementType.TAG)

    for (attr in tag.attributes.entries) {
      currentTag?.attributes?.set(attr.key, attr.value)
    }
  }

  fun appendKomponent(komponent: Komponent) {
    currentTag?.children?.add(KompElement(komponent))
  }

}

class KompTest : Komponent() {
  var counter = 0
  var show = false
  var child: KompTest? = null

  override fun render(consumer: KompConsumer) = consumer.div {
    h1 {
      +"Test"
    }
    hr { }
    span {
      +"Clicks $counter"

      onClickFunction = {
        println("click")
        counter++

        update()
      }
    }
    if (show) {
      hr {}

      span {
        +"Hide element"

        onClickFunction = {
          show = false

          update()
        }
      }

      if (child == null) {
        child = KompTest()
      }

      include(child!!)
    } else {
      hr {}

      span {
        +"Show element"

        onClickFunction = {
          show = true

          console.log("show", this)

          update()
        }
      }
    }
  }
}

/*
fun main(args: Array<String>) {
  val test = KompTest()

  println(test.create())

  Komponent.create(document.body!!, KompTest())
}
*/
