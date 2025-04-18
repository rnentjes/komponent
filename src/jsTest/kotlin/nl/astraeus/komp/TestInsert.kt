package nl.astraeus.komp

import kotlinx.browser.document
import kotlinx.html.div
import kotlinx.html.table
import kotlinx.html.tbody
import kotlinx.html.td
import kotlinx.html.tr
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLTableElement
import org.w3c.dom.HTMLTableRowElement
import org.w3c.dom.HTMLTableCellElement
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * A Komponent that renders a table with rows
 */
class TableKomponent : Komponent() {
    val rows = mutableListOf<RowKomponent>()

    fun addRow(text: String) {
        rows.add(RowKomponent(text))
        requestImmediateUpdate()
    }

    override fun HtmlBuilder.render() {
        div {
            table {
                tbody {
                    for (row in rows) {
                        include(row)
                    }
                }
            }
        }
    }
}

/**
 * A Komponent that represents a single row in a table
 */
class RowKomponent(val text: String) : Komponent() {
    override fun generateMemoizeHash(): Int = text.hashCode()

    override fun HtmlBuilder.render() {
        tr {
            td {
                +text
            }
        }
    }
}

/**
 * Test class for inserting rows in the DOM with a Komponent
 */
class TestInsert {

    @Test
    fun testInsertRow() {
        // Create a test component
        val tableComponent = TableKomponent()
        val div = document.createElement("div") as HTMLDivElement

        // Render it
        Komponent.create(div, tableComponent)

        // Verify initial state - should be an empty table
        val table = div.querySelector("table")
        assertNotNull(table, "Table should be rendered")
        val initialRows = table.querySelectorAll("tr")
        assertEquals(0, initialRows.length, "Table should initially have no rows")

        // Add a row and verify it was inserted
        tableComponent.addRow("First Row")

        // Verify the row was added
        val rowsAfterFirstInsert = table.querySelectorAll("tr")
        assertEquals(1, rowsAfterFirstInsert.length, "Table should have one row after insertion")
        val firstRowCell = table.querySelector("tr td")
        assertNotNull(firstRowCell, "First row cell should exist")
        assertEquals("First Row", firstRowCell.textContent, "Row content should match")

        // Add another row and verify it was inserted
        tableComponent.addRow("Second Row")

        // Verify both rows are present
        val rowsAfterSecondInsert = table.querySelectorAll("tr")
        assertEquals(2, rowsAfterSecondInsert.length, "Table should have two rows after second insertion")
        val allCells = table.querySelectorAll("tr td")
        assertEquals(2, allCells.length, "Table should have two cells")
        assertEquals("First Row", allCells.item(0)?.textContent, "First row content should match")
        assertEquals("Second Row", allCells.item(1)?.textContent, "Second row content should match")

        // Print the DOM tree for debugging
        println("Table DOM: ${div.printTree()}")
    }
}
