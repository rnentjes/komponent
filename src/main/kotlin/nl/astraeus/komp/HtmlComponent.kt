package nl.astraeus.komp

import kotlinx.html.DIV
import kotlinx.html.TagConsumer
import kotlinx.html.dom.create
import org.w3c.dom.HTMLElement
import kotlin.browser.document

fun DIV.include(component: HtmlComponent) {
    val result = component.render(this.consumer as TagConsumer<HTMLElement>)

    component.element = result
    Komp.define(result, component)
}

abstract class HtmlComponent {
    var element: HTMLElement? = null

    fun create(): HTMLElement {
        var elem =element
        if (elem != null) {
            Komp.remove(elem)
        }

        elem = render(document.create)

        Komp.define(elem, this)

        this.element = elem

        return elem
    }

    abstract fun render(consumer: TagConsumer<HTMLElement>): HTMLElement

    fun refresh() {
        Komp.refresh(element)
    }
}
