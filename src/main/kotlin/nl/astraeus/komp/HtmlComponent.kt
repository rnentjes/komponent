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

enum class Sizing {
    NONE,
    HORIZONTAL,
    VERTICAL
}

abstract class HtmlComponent(val sizing: Sizing = Sizing.NONE) {
    var element: HTMLElement? = null
    var size: ComponentSize? = null

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

    open fun refresh() {
        Komp.refresh(element)

        if (sizing != Sizing.NONE) {
            // resize children
        }
    }

}
