package nl.astraeus.komp

import org.w3c.dom.Element
import org.w3c.dom.HTMLElement
import org.w3c.dom.get
import kotlin.browser.document
import kotlin.browser.window

/**
 * User: rnentjes
 * Date: 29-3-17
 * Time: 15:46
 */

object Komp {

    private val elements: MutableMap<HTMLElement, HtmlComponent> = HashMap()
    private val elementList: MutableList<HtmlComponent> = ArrayList()
    private var resizing = false

    init {
        window.onresize = {
            //Komp.resize()
        }
    }

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

        //resize()
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

        //resize()
    }

    private fun resize() {
        if (!resizing) {
            resizing = true

            window.setTimeout({
                resizing = false

                resizeComponents()
            })
        }
    }

    private fun resizeComponents() {
        for (component in elementList) {
            component.element?.setAttribute("data-resized", "false")
        }

        for (component in elementList) {
            if (component.sizing != Sizing.NONE && component.element?.getAttribute("data-resize") != "true") {
                console.log("resize", component)

                resize(component)
            }
        }
    }

    private fun resize(comp: HtmlComponent) {
        val parent = comp.element?.parentElement

        if (parent != null) {
            val sizes = getSiblingSizes(parent)
            val parentSize = elements[parent]?.size
            val container: SizeContainer

            if (parentSize != null) {
                container = SizeContainer(
                  parentSize.calculatedSize,
                  sizes
                )
            } else {
                val leftString = (parent as HTMLElement).style.left
                val topString = parent.style.top
                val widthString = parent.style.width
                val heightString = parent.style.height

                if (parent == document.body) {
                    container = SizeContainer(
                      Rect(parent.clientLeft, parent.clientTop, parent.clientWidth, parent.clientHeight),
                      sizes
                    )
                } else if (leftString.endsWith("px") && topString.endsWith("px") && widthString.endsWith("px") && heightString.endsWith("px")) {
                    container = SizeContainer(
                      Rect(
                        leftString.slice(0..leftString.length - 3).toInt(),
                        topString.slice(0..topString.length - 3).toInt(),
                        widthString.slice(0..widthString.length - 3).toInt(),
                        heightString.slice(0..heightString.length - 3).toInt()
                      ),
                      sizes
                    )
                } else {
                    container = SizeContainer(
                      Rect(parent.clientLeft, parent.clientTop, parent.clientWidth, parent.clientHeight),
                      sizes
                    )
                }
            }

            container.calculate()
        }
    }

    private fun getSiblingSizes(parent: Element): List<ComponentSize> {
        val result: MutableList<ComponentSize> = ArrayList()

        for (index in 0..parent.children.length-1) {
            val child = parent.children[index]

            if (child is HTMLElement) {
                val comp = elements[child]
                val size = getSize(child)
                comp?.size = size

                result.add(ComponentSize(child, size.type, size.value))
            }
        }

        return result
    }

    fun getSize(element: HTMLElement): ComponentSize {
        val sizeText = element.attributes?.get("size")?.value
        var result: ComponentSize? = null

        if (sizeText != null) {
            val (type, size) = getSizeFromAttribute(sizeText)

            result = ComponentSize(element, type, size)
        }

        return result ?: throw IllegalStateException("Unable to calculate size for $this")
    }

    private fun getSizeFromAttribute(sizeString: String): Pair<SizeType, Float> {
        if (sizeString == "fill") {
            return SizeType.FILL to 0f
        } else if (sizeString.endsWith("px")) {
            return SizeType.ABSOLUTE to sizeString.slice(0..sizeString.length-3).toFloat()
        } else if (sizeString.endsWith("%")) {
            return SizeType.PERCENTAGE to sizeString.slice(0..sizeString.length-2).toFloat()
        } else {
            return SizeType.FLEX to sizeString.toFloat()
        }
    }


}
