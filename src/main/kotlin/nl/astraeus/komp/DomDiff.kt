package nl.astraeus.komp

import org.w3c.dom.Node
import org.w3c.dom.get

/**
 * User: rnentjes
 * Date: 10-9-17
 * Time: 13:33
 */

object DomDiffer {

  fun replaceDiff(oldElement: KompElement, newElement: KompElement, element: Node): Node {
    if (oldElement.isKomponent() && newElement.isKomponent()) {
      if (oldElement.equals(newElement)) {
        newElement.komponent?.update()

        return newElement.komponent?.element ?: newElement.komponent?.create()?.create()
        ?: throw IllegalStateException("Unable to create new element!")
      } else {
        return Komponent.replaceNode(newElement, element)
      }
    } else if (!oldElement.isKomponent() && newElement.isKomponent()) {
      return Komponent.replaceNode(newElement, element)
    } else if (!oldElement.equals(newElement)) {
      return Komponent.replaceNode(newElement, element)
    } else {
      if (oldElement.children == null && newElement.children != null) {
        for (index in 0 until newElement.children.size) {
          Komponent.appendElement(element, newElement.children[index])
        }
      } else if (oldElement.children != null && newElement.children == null) {
        while (element.firstChild != null) {
          if (Komponent.logReplaceEvent) {
            console.log("Remove", element.firstChild)
          }
          element.removeChild(element.firstChild!!)
        }
      } else if (oldElement.children != null && newElement.children != null) {
        if (oldElement.children.size > newElement.children.size) {
          val toRemove = oldElement.children.size - newElement.children.size
          var removed = 0
          var index = 0

          while (index < newElement.children.size) {
            val childNode = element.childNodes[index]

            if (childNode == null) {
              println("Warn childNode is null!")
            } else {
              if ((!oldElement.children[index + removed].equals(newElement.children[index])) && removed < toRemove) {
                if (Komponent.logReplaceEvent) {
                  console.log("Remove", oldElement.children[index + removed], newElement.children[index])
                }
                element.removeChild(childNode)

                removed++
              } else {
                replaceDiff(oldElement.children[index + removed], newElement.children[index], childNode)

                index++
              }
            }
          }

          while (removed < toRemove) {
            element.lastChild?.also {
              Komponent.removeElement(it)
            }

            removed++
          }
        } else {
          for (index in 0 until newElement.children.size) {
            val childNode = element.childNodes[index]

            if (childNode == null) {
              Komponent.appendElement(element, newElement.children[index])
            } else {
              replaceDiff(oldElement.children[index], newElement.children[index], childNode)
            }
          }
        }
      }

      return element
    }
  }

}
