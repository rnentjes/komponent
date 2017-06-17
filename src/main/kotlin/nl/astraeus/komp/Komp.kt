package nl.astraeus.komp

import org.w3c.dom.HTMLElement
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.set

/**
 * User: rnentjes
 * Date: 29-3-17
 * Time: 15:46
 */

object Komp {

    private val elements: MutableMap<HTMLElement, HtmlComponent> = HashMap()
    private val elementList: MutableList<HtmlComponent> = ArrayList()

    fun define(element: HTMLElement, component: HtmlComponent) {
        elements[element] = component
        elementList.add(component)
    }

    fun create(parent: HTMLElement, component: HtmlComponent, insertAsFirst: Boolean = false) {
        val element = component.create()

        if (insertAsFirst && parent.childElementCount > 0) {
            parent.insertBefore(element, parent.firstChild)
        } else {
            parent.appendChild(element)
        }

        elements[element] = component
        elementList.add(component)
    }

    fun remove(element: HTMLElement) {
        val component = elements[element]

        elements.remove(element)
        elementList.remove(component)
    }

    @JsName("remove")
    fun remove(component: HtmlComponent) {
        for ((key, value) in elements) {
            if (value == component) {
                elements.remove(key)
            }
        }
        elementList.remove(component)
    }

    fun refresh(component: HtmlComponent) {
        refresh(component.element)
    }

    fun refresh(element: HTMLElement?) {
        if (element != null) {
            val comp = elements[element]

            if (element is HTMLElement && comp != null) {
                val parent = element.parentElement
                val newElement = comp.create()

                parent?.replaceChild(newElement, element)
            }
        }
    }

}
