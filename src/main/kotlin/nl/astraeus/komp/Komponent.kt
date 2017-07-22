package nl.astraeus.komp

import kotlinx.html.DIV
import kotlinx.html.TagConsumer
import kotlinx.html.dom.create
import org.w3c.dom.HTMLElement
import kotlin.browser.document

fun DIV.include(component: Komponent) {
    val result = component.render(this.consumer as TagConsumer<HTMLElement>)

    component.element = result
    Komponent.define(result, component)
}

abstract class Komponent {
    var element: HTMLElement? = null
    var rendered = false

    fun create(): HTMLElement {
        var elem =element
        if (elem != null) {
            remove(elem)
        }

        elem = render(document.create)
        rendered = true

        define(elem, this)

        this.element = elem

        return elem
    }

    abstract fun render(consumer: TagConsumer<HTMLElement>): HTMLElement

    open fun refresh() {
        if (rendered) {
            refresh(element)
        } else {
            update()
        }
    }

    open fun update() {
        refresh(element)
    }

    companion object {

        private val elements: MutableMap<HTMLElement, Komponent> = HashMap()
        private val elementList: MutableList<Komponent> = ArrayList()

        fun define(element: HTMLElement, component: Komponent) {
            elements[element] = component
            elementList.add(component)
        }

        fun create(parent: HTMLElement, component: Komponent, insertAsFirst: Boolean = false) {
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
        fun remove(component: Komponent) {
            for ((key, value) in elements) {
                if (value == component) {
                    elements.remove(key)
                }
            }
            elementList.remove(component)
        }

        fun refresh(component: Komponent) {
            refresh(component.element)
        }

        fun refresh(element: HTMLElement?) {
            if (element != null) {
                elements[element]?.let {
                    val parent = element.parentElement
                    val newElement = it.create()

                    parent?.replaceChild(newElement, element)
                }
            }
        }
    }
}