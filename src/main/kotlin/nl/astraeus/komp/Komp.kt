package nl.astraeus.komp

import org.w3c.dom.HTMLElement

/**
 * User: rnentjes
 * Date: 29-3-17
 * Time: 15:46
 */

object Komp {

    private val elements: MutableMap<HTMLElement, HtmlComponent> = HashMap()

    fun define(element: HTMLElement, component: HtmlComponent) {
        elements[element] = component
    }

    fun create(parent: HTMLElement, component: HtmlComponent) {
        val element = component.create()

        parent.appendChild(element)

        elements[element] = component
    }

    fun remove(element: HTMLElement) {
        elements.remove(element)
    }

    @JsName("remove")
    fun remove(component: HtmlComponent) {
        for ((key, value) in elements) {
            if (value == component) {
                elements.remove(key)
            }
        }
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