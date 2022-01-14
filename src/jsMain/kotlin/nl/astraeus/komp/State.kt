package nl.astraeus.komp

import kotlin.reflect.KProperty

class StateDelegate<T>(
  val komponent: Komponent,
  initialValue: T
) {
  var value: T = initialValue

  operator fun getValue(
    thisRef: Any?,
    property: KProperty<*>
  ): T {
    return value
  }

  operator fun setValue(
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
): StateDelegate<T> = StateDelegate(
  this,
  initialValue
)
