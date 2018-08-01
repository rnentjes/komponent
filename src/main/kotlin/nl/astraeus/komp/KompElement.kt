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
import kotlin.browser.document

/**
 * User: rnentjes
 * Date: 8-5-18
 * Time: 21:58
 */

class KompElement(
    val komponent: Komponent?,
    var text: String,
    var unsafe: Boolean = false,
    val attributes: MutableMap<String, String>? = null,
    val children: MutableList<KompElement>? = null,
    val events: MutableMap<String, (Event) -> Unit>? = null
) {

  constructor(text: String, isText: Boolean = true) : this(
      null,
      text,
      false,
      if (isText) {
        null
      } else {
        HashMap()
      },
      if (isText) {
        null
      } else {
        ArrayList()
      },
      if (isText) {
        null
      } else {
        HashMap()
      }
  )

  constructor(komponent: Komponent) : this(
      komponent,
      "",
      false,
      null,
      null,
      null
  )

  /* shallow equals check */
  fun equals(other: KompElement): Boolean {
    if (komponent != null) {
      return komponent == other.komponent
    } else if (other.isText() || isText()) {
      return other.text == text
    } else {
      if (other.attributes?.size != attributes?.size || other.events?.size != events?.size) {
        return false
      } else {
        (attributes?.entries)?.forEach { entry ->
          if (other.attributes?.get(entry.key) != entry.value) {
            return false
          }
        }
        (events?.entries)?.forEach { entry ->
          if (other.events?.get(entry.key) != entry.value) {
            return false
          }
        }
      }
    }

    return true
  }

  fun isText() = attributes == null && komponent == null

  fun isKomponent() = komponent != null

  fun create(): Node = when {
    komponent != null -> {
      komponent.element?.also {
        Komponent.remove(it)
      }

      val kompElement = komponent.kompElement ?: komponent.create()
      val element = kompElement.create()

      komponent.kompElement = kompElement
      komponent.element = element

      Komponent.define(element, komponent)

      element
    }
    isText() -> document.createTextNode(text)
    else -> {
      val result = document.createElement(text)

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
        result.append(child.create())
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

    currentTag?.children?.add(KompElement(content.toString(), true))
  }

  override fun onTagContentEntity(entity: Entities) {
    console.log("KC.onTagContentEntity", entity)
  }

  override fun onTagContentUnsafe(block: Unsafe.() -> Unit) {
    //console.log("KC.onTagContentUnsafe", block)

    throw IllegalStateException("unsafe blocks are not supported atm.")
  }

  override fun onTagEnd(tag: Tag) {
    //console.log("KC.onTagEnd", tag)

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
    //console.log("KC.onTagStart", tag)

    currentTag?.apply {
      stack.add(this)
    }

    currentTag = KompElement(tag.tagName, false)

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
