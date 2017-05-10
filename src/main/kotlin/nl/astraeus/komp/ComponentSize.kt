package nl.astraeus.komp

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
  val xType: SizeType,
  val yType: SizeType,
  val xValue: Float,
  val yValue: Float
)

class NotSized : ComponentSize(SizeType.NONE, SizeType.NONE, 0f, 0f)
