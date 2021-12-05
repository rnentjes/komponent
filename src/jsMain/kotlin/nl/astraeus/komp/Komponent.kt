package nl.astraeus.komp

import kotlinx.browser.window
import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.Node
import org.w3c.dom.get
import kotlin.reflect.KProperty

class StateDelegate<T>(
  val komponent: Komponent,
  initialValue: T
) {
  var value: T = initialValue

  operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
    return value
  }

  operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
    if (this.value?.equals(value) != true) {
      this.value = value
      komponent.requestUpdate()
    }
  }
}

inline fun <reified T> Komponent.state(initialValue: T): StateDelegate<T> = StateDelegate(this, initialValue)

enum class UnsafeMode {
  UNSAFE_ALLOWED,
  UNSAFE_DISABLED,
  UNSAFE_SVG_ONLY
}

abstract class Komponent {
  val createIndex = getNextCreateIndex()
  private var dirty: Boolean = true
  private var lastMemoizeHash: Int? = null

  var element: Node? = null

  open fun create(parent: Element, childIndex: Int? = null) {
    onBeforeUpdate()
    val builder = HtmlBuilder(
      parent,
      childIndex ?: parent.childNodes.length
    )

    builder.render()
    element = builder.root
    lastMemoizeHash = generateMemoizeHash()
    onAfterUpdate()
  }

  fun memoizeChanged() = lastMemoizeHash == null || lastMemoizeHash != generateMemoizeHash()

  abstract fun HtmlBuilder.render()

  /**
   * This method is called after the Komponent is updated
   *
   * note: it's also called at first render
   */
  open fun onAfterUpdate() {}

  /**
   * This method is called before the Komponent is updated
   * and before memoizeHash is checked
   *
   * note: it's also called at first render
   */
  open fun onBeforeUpdate() {}

  fun requestUpdate() {
    dirty = true
    scheduleForUpdate(this)
  }

  /**
   * Request an immediate update of this Komponent
   *
   * This will run immediately, make sure Komponents are not rendered multiple times
   * Any scheduled updates will be run as well
   */
  fun requestImmediateUpdate() {
    dirty = true
    runUpdateImmediately(this)
  }

  /**
   * This function can be overwritten if you know how to update the Komponent yourself
   *
   * HTMLBuilder.render() is called 1st time the component is rendered, after that this
   * method will be called
   */
  open fun update() {
    refresh()
  }

  /**
   * If this function returns a value it will be stored and on the next render it will be compared.
   *
   * The render will only happen if the hash is not null and has changed
   */
  open fun generateMemoizeHash(): Int? = null

  internal fun refresh() {
    val currentElement = element

    check(currentElement != null) {
      error("element is null")
    }

    val parent = currentElement.parentElement as? HTMLElement ?: error("parent is null!?")
    var childIndex = 0
    for (index in 0 until parent.childNodes.length) {
      if (parent.childNodes[index] == currentElement) {
        childIndex = index
      }
    }
    val consumer = HtmlBuilder(parent, childIndex)
    consumer.root = null
    consumer.render()
    element = consumer.root
    dirty = false
  }

  override fun toString(): String {
    return "${this::class.simpleName}"
  }

  companion object {
    private var nextCreateIndex: Int = 1
    private var updateCallback: Int? = null
    private var scheduledForUpdate = mutableSetOf<Komponent>()
    private var interceptor: (Komponent, () -> Unit) -> Unit = { _, block -> block() }

    var logRenderEvent = false
    var logReplaceEvent = false
    var unsafeMode = UnsafeMode.UNSAFE_DISABLED

    fun create(parent: HTMLElement, component: Komponent) {
      component.create(parent)
    }

    fun setUpdateInterceptor(block: (Komponent, () -> Unit) -> Unit) {
      interceptor = block
    }

    private fun getNextCreateIndex() = nextCreateIndex++

    private fun scheduleForUpdate(komponent: Komponent) {
      scheduledForUpdate.add(komponent)

      if (updateCallback == null) {
        window.setTimeout({
          runUpdate()
        }, 0)
      }
    }

    private fun runUpdateImmediately(komponent: Komponent) {
      scheduledForUpdate.add(komponent)
      runUpdate()
    }

    private fun runUpdate() {
      val todo = scheduledForUpdate.sortedBy { komponent -> komponent.createIndex }

      if (logRenderEvent) {
        console.log("runUpdate")
      }

      todo.forEach { next ->
        interceptor(next) {
          val element = next.element

          if (element is HTMLElement) {
            if (next.dirty) {
              if (logRenderEvent) {
                console.log("Update dirty ${next.createIndex}")
              }
              val memoizeHash = next.generateMemoizeHash()

              if (memoizeHash == null || next.lastMemoizeHash != memoizeHash) {
                next.onBeforeUpdate()
                next.update()
                next.lastMemoizeHash = memoizeHash
                next.onAfterUpdate()
              } else if (logRenderEvent) {
                console.log("Skipped render, memoizeHash is equal $next-[$memoizeHash]")
              }
            } else {
              if (logRenderEvent) {
                console.log("Skip ${next.createIndex}")
              }
            }
          } else {
            console.log("Komponent element is null", next, element)
          }
        }
      }

      scheduledForUpdate.clear()
      updateCallback = null
    }
  }

}
