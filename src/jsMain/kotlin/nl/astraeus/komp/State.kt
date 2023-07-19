package nl.astraeus.komp

import kotlin.reflect.KProperty

interface Delegate<T> {

  operator fun getValue(
    thisRef: Any?,
    property: KProperty<*>
  ): T

  operator fun setValue(
    thisRef: Any?,
    property: KProperty<*>,
    value: T
  )

}

open class StateDelegate<T>(
  val komponent: Komponent,
  initialValue: T
) : Delegate<T> {
  private var value: T = initialValue

  init {
    if (value is MutableCollection<*>) {
      error("Use mutableList to create a collection!")
    }
  }

  override operator fun getValue(
    thisRef: Any?,
    property: KProperty<*>
  ): T {
    return value
  }

  override operator fun setValue(
    thisRef: Any?,
    property: KProperty<*>,
    value: T
  ) {
    if (this.value?.equals(value) != true) {
      this.value = value
      komponent.requestUpdate()
    }
  }
}

inline fun <reified T> Komponent.state(
  initialValue: T
): Delegate<T> = StateDelegate(
  this,
  initialValue
)
