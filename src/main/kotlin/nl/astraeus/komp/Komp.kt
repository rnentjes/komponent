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

    fun create(parent: HTMLElement, component: HtmlComponent, insertAsFirst: Boolean = false) {
        val element = component.create()

        if (insertAsFirst && parent.childElementCount > 0) {
            parent.insertBefore(element, parent.firstChild)
        } else {
            parent.appendChild(element)
        }

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
                val size = comp.getSize()

                if (size != null) {
                    sizeElement(newElement, size)
                }

                parent?.replaceChild(newElement, element)
            }
        }
    }

    fun sizeElement(element: HTMLElement, size: ComponentSize) {
        var width = ""
        var height = ""
        val parent = element.parentElement as HTMLElement

        when(size.xType) {
            SizeType.ABSOLUTE -> {
                width = "${size.xValue.toInt()}px"
            }
            SizeType.PERCENTAGE -> {
                width = "${(parent.clientWidth *  size.xValue / 100f).toInt()}px"
            }
            SizeType.FILL -> {

            }
            SizeType.FLEX -> {

            }
        }

        if (width.isNotBlank()) {
            element.style.width = width
        }
        if (height.isNotBlank()) {
            element.style.height = height
        }
    }

}