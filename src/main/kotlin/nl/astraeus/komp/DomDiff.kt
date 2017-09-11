package nl.astraeus.komp

import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.get

/**
 * User: rnentjes
 * Date: 10-9-17
 * Time: 13:33
 */

object DomDiffer {

    fun replaceDiff(newElement: Element, oldElement: Element): Element {
        if (!newElement.isEqualNode(oldElement)) {
            replaceNode(newElement, oldElement)

            return newElement
        } else {
            // think of the children!
            for (index in 0 until newElement.children.length) {
                val newChild = newElement.children[index]
                val oldChild = oldElement.children[index]

                if (newChild is Element && oldChild is Element) {
                    replaceDiff(newChild, oldChild)
                } else {
                    throw IllegalStateException("Children are not nodes! $newChild, $oldChild")
                }
            }

            return oldElement
        }
    }

    private fun replaceNode(newElement: Node, oldElement: Node) {
        val parent = oldElement.parentElement ?: throw IllegalStateException("oldElement has no parent! $oldElement")

        parent.replaceChild(newElement, oldElement)
    }

}
