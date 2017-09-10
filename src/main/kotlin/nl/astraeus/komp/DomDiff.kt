package nl.astraeus.komp

import org.w3c.dom.HTMLElement
import org.w3c.dom.get

/**
 * User: rnentjes
 * Date: 10-9-17
 * Time: 13:33
 */

object DomDiffer {

    fun replaceDiff(newElement: HTMLElement, oldElement: HTMLElement) {
        if (!match(newElement, oldElement)) {
            replaceNode(newElement, oldElement)
        } else {
            // think of the children!
            for (index in 0 until newElement.childElementCount) {
                val newChild = newElement.children[index]
                val oldChild = oldElement.children[index]

                if (newChild is HTMLElement && oldChild is HTMLElement) {
                    replaceDiff(newChild, oldChild)
                }
            }
        }
    }

    private fun replaceNode(newElement: HTMLElement, oldElement: HTMLElement) {
        val parent = oldElement.parentElement

        parent?.replaceChild(newElement, oldElement)
    }

    fun match(newElement: HTMLElement, oldElement: HTMLElement): Boolean {
        var result = true

        result = result && newElement.namespaceURI == oldElement.namespaceURI
        result = result && newElement.nodeName == oldElement.nodeName
        result = result && newElement.childElementCount == oldElement.childElementCount

        val newAttr = newElement.attributes
        val oldAttr = oldElement.attributes

        result = result && newAttr.length == oldAttr.length

        if (result) {
            for (index in 0 until newAttr.length) {
                val attr = newAttr[index]

                if (attr != null) {
                    result = result && newAttr.getNamedItem(attr.name)?.name == oldAttr.getNamedItem(attr.name)?.name
                    result = result && newAttr.getNamedItem(attr.name)?.value == oldAttr.getNamedItem(attr.name)?.value
                }
                if (!result) {
                    break
                }
            }
        }

        return result
    }

}
