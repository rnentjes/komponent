package nl.astraeus.komp

import org.w3c.dom.HTMLElement

/**
 * User: rnentjes
 * Date: 10-5-17
 * Time: 16:48
 */

enum class SizeType {
    NONE,
    ABSOLUTE,
    PERCENTAGE,
    FLEX,
    FILL
}

open class ComponentSize(
  val element: HTMLElement,
  val type: SizeType,
  val value: Float
) {
    var calculatedSize: Rect = Rect(0,0,0,0)
}

class Rect(
  val left: Int,
  val top: Int,
  val width: Int,
  val height: Int
) {
    override fun toString(): String {
        return "Rect(left=$left, top=$top, width=$width, height=$height)"
    }
}

class SizeContainer(
  val parentSize: Rect,
  val componentList: List<ComponentSize>
) {
    var totalSize = 0
    var totalPixels = 0f
    var totalPercentage = 0f
    var totalFlex = 0f
    var fillCount = 0f

    var afterPixels = 0f
    var afterPercentage = 0f
    var calculatedSize = 0
    var calculatedStart = 0

    fun calculate() {
        for (size in componentList) {
            when(size.type) {
                SizeType.ABSOLUTE -> {
                    totalPixels += size.value
                }
                SizeType.PERCENTAGE -> {
                    totalPercentage += size.value
                }
                SizeType.FLEX -> {
                    totalFlex += size.value
                }
                SizeType.FILL -> {
                    fillCount++
                }
            }
        }

/*        if (layout == LayoutType.HORIZONTAL) {
            totalSize = parentSize.width
        } else {
            totalSize = parentSize.height
        }*/

        afterPixels = totalSize - totalPixels
        afterPercentage = afterPixels * totalPercentage / 100f

        for (size in componentList) {
            when(size.type) {
                SizeType.ABSOLUTE -> {
                    calculatedSize = size.value.toInt()
                }
                SizeType.PERCENTAGE -> {
                    calculatedSize = (afterPixels * size.value / 100f).toInt()
                }
                SizeType.FLEX -> {
                    calculatedSize = (afterPercentage * size.value / totalFlex).toInt()
                }
                SizeType.FILL -> {
                    calculatedSize = (afterPercentage * size.value / fillCount).toInt()
                }
            }

/*            if (layout == LayoutType.HORIZONTAL) {
                size.calculatedSize = Rect(calculatedStart, parentSize.top, calculatedSize, parentSize.height)
            } else {
                size.calculatedSize = Rect(parentSize.left, calculatedStart, parentSize.width, calculatedSize)
            }*/

            calculatedStart += calculatedSize
            console.log("Set component to ${size.calculatedSize}", size.element)

            size.element.style.position = "absolute"
            size.element.style.left = "${size.calculatedSize.left}px"
            size.element.style.top = "${size.calculatedSize.top}px"
            size.element.style.width = "${size.calculatedSize.width}px"
            size.element.style.height = "${size.calculatedSize.height}px"

            size.element.setAttribute("data-resized", "true")
        }
    }
}

