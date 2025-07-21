package com.gamemode.tkviewer

/**
 * Color represents the RGB (Red, Green, Blue) data of an unsigned integer (Long)
 */
class Color(var rgba: Long) {
    val red: Long? = rgba and 0xFF000000 shr 24
    val green: Long? = rgba and 0x00FF0000 shr 16
    val blue: Long? = rgba and 0x0000FF00 shr 8
    val alpha: Long? = rgba and 0x000000FF

    init { this.rgba = java.lang.Long.rotateRight(rgba, 8) }

    constructor(r: Int, g: Int, b: Int, a: Int) : this(
        ((r.toLong() and 0xFF) shl 24) or
                ((g.toLong() and 0xFF) shl 16) or
                ((b.toLong() and 0xFF) shl 8) or
                (a.toLong() and 0xFF)
    )
}