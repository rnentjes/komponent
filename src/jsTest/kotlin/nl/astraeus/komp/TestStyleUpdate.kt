package nl.astraeus.komp

import kotlinx.browser.document
import kotlinx.html.div
import kotlinx.html.style
import org.w3c.dom.HTMLDivElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Test class for verifying style attribute updates and removals
 */
class StyleKomponent : Komponent() {
    var includeStyle = true
    var styleValue = "color: red;"

    override fun HtmlBuilder.render() {
        div {
            if (includeStyle) {
                style = styleValue
            }
            +"Content"
        }
    }
}

class TestStyleUpdate {

    @Test
    fun testStyleRemoval() {
        // Create a test component
        val styleComponent = StyleKomponent()
        val div = document.createElement("div") as HTMLDivElement

        // Render it
        Komponent.create(div, styleComponent)

        // Verify initial state - should have the style
        val contentDiv = div.querySelector("div")
        println("[DEBUG_LOG] Initial DOM: ${div.printTree()}")
        assertEquals("color: red;", contentDiv?.getAttribute("style"), "Div should have the style initially")

        // Update to remove the style
        styleComponent.includeStyle = false
        styleComponent.requestImmediateUpdate()

        // Verify the style was removed
        println("[DEBUG_LOG] After style removal: ${div.printTree()}")
        assertNull(contentDiv?.getAttribute("style"), "Style should be removed after update")

        // Add the style back
        styleComponent.includeStyle = true
        styleComponent.requestImmediateUpdate()

        // Verify the style was added back
        println("[DEBUG_LOG] After style added back: ${div.printTree()}")
        assertEquals("color: red;", contentDiv?.getAttribute("style"), "Style should be added back")

        // Change the style value
        styleComponent.styleValue = "color: blue;"
        styleComponent.requestImmediateUpdate()

        // Verify the style was changed
        println("[DEBUG_LOG] After style value change: ${div.printTree()}")
        assertEquals("color: blue;", contentDiv?.getAttribute("style"), "Style should be updated to new value")
    }
}