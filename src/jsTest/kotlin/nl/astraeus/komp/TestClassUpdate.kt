package nl.astraeus.komp

import kotlinx.browser.document
import kotlinx.html.div
import kotlinx.html.classes
import org.w3c.dom.HTMLDivElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test class for verifying class attribute updates and removals
 */
class ClassKomponent : Komponent() {
    var includeClass = true
    var className = "test-class"

    override fun HtmlBuilder.render() {
        div {
            if (includeClass) {
                classes = setOf(className)
            }
            +"Content"
        }
    }
}

class TestClassUpdate {

    @Test
    fun testClassRemoval() {
        // Create a test component
        val classComponent = ClassKomponent()
        val div = document.createElement("div") as HTMLDivElement

        // Render it
        Komponent.create(div, classComponent)

        // Verify initial state - should have the class
        val contentDiv = div.querySelector("div")
        println("[DEBUG_LOG] Initial DOM: ${div.printTree()}")
        assertTrue(contentDiv?.classList?.contains("test-class") ?: false, "Div should have the class initially")

        // Update to remove the class
        classComponent.includeClass = false
        classComponent.requestImmediateUpdate()

        // Verify the class was removed
        println("[DEBUG_LOG] After class removal: ${div.printTree()}")
        assertFalse(contentDiv?.classList?.contains("test-class") ?: true, "Class should be removed after update")

        // Add the class back
        classComponent.includeClass = true
        classComponent.requestImmediateUpdate()

        // Verify the class was added back
        println("[DEBUG_LOG] After class added back: ${div.printTree()}")
        assertTrue(contentDiv?.classList?.contains("test-class") ?: false, "Class should be added back")

        // Change the class name
        classComponent.className = "new-class"
        classComponent.requestImmediateUpdate()

        // Verify the class was changed
        println("[DEBUG_LOG] After class name change: ${div.printTree()}")
        assertFalse(contentDiv?.classList?.contains("test-class") ?: true, "Old class should be removed")
        assertTrue(contentDiv?.classList?.contains("new-class") ?: false, "New class should be added")
    }
}