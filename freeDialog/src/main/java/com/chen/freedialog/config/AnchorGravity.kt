package com.chen.freedialog.config

/**
 * 针对定位的View，弹框对于他所在的位置
 */
object AnchorGravity {
    private const val AXIS_SPECIFIED = 0x0001 //0000 0001
    private const val AXIS_PULL_BEFORE = 0x0002 //0000 0010
    private const val AXIS_PULL_AFTER = 0x0004 // 0000 0100
    private const val AXIS_PULL_ALIGN = 0x0008 // 0000 1000
    private const val AXIS_X_SHIFT = 0 //0000 0000
    private const val AXIS_Y_SHIFT = 4 // 0000 0100

    const val TOP: Int = (AXIS_PULL_BEFORE or AXIS_SPECIFIED) shl AXIS_Y_SHIFT // 0011 0000  (48)
    const val BOTTOM: Int = (AXIS_PULL_AFTER or AXIS_SPECIFIED) shl AXIS_Y_SHIFT // 0101 0000  (80)
    const val LEFT: Int = (AXIS_PULL_BEFORE or AXIS_SPECIFIED) shl AXIS_X_SHIFT // 0000 0011  (3)
    const val ALIGN_LEFT: Int = (AXIS_PULL_ALIGN or AXIS_SPECIFIED) shl AXIS_X_SHIFT // 0000 1001  (9)
    const val ALIGN_RIGHT: Int = (AXIS_PULL_ALIGN or AXIS_PULL_BEFORE) shl AXIS_X_SHIFT // 0000 1010  (10)

    const val RIGHT: Int = (AXIS_PULL_AFTER or AXIS_SPECIFIED) shl AXIS_X_SHIFT // 0000 0101  (5)
    const val CENTER_VERTICAL: Int = AXIS_SPECIFIED shl AXIS_Y_SHIFT // 0001 0000  (16)
    const val CENTER_HORIZONTAL: Int = AXIS_SPECIFIED shl AXIS_X_SHIFT // 0000 0001  (1)
    const val CENTER: Int = CENTER_VERTICAL or CENTER_HORIZONTAL // 0001 0001  (17)
}
