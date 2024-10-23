package nl.astraeus.komp

inline fun <reified T> Komponent.mutableCollectionState(
  initialValue: MutableCollection<T>
): MutableCollection<T> = MutableCollectionStateDelegate(
  this,
  initialValue
)

class MutableCollectionStateDelegate<T>(
  val komponent: Komponent,
  val collection: MutableCollection<T>
): MutableCollection<T> by collection {

  override fun add(element: T): Boolean {
    komponent.requestUpdate()

    return collection.add(element)
  }

  override fun addAll(elements: Collection<T>): Boolean {
    komponent.requestUpdate()

    return collection.addAll(elements)
  }

  override fun clear() {
    komponent.requestUpdate()

    collection.clear()
  }

  // todo: return iterator wrapper to update at changes?
  //override fun iterator(): MutableIterator<T> = collection.iterator()

  override fun remove(element: T): Boolean {
    komponent.requestUpdate()

    return collection.remove(element)
  }

  override fun removeAll(elements: Collection<T>): Boolean {
    komponent.requestUpdate()

    return collection.removeAll(elements)
  }

  override fun retainAll(elements: Collection<T>): Boolean {
    komponent.requestUpdate()

    return collection.retainAll(elements)
  }

}
