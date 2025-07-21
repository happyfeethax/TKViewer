package com.gamemode.tkviewer

import com.gamemode.tkviewer.Color
import java.util.ArrayList

class Palette(var animationColorCount: Int, animationColorOffsets: List<Int>, colors: List<Color>, val paletteMetadata: PaletteMetadata) {
    var animationColorOffsets: MutableList<Int>
    var colors: MutableList<Color>

    constructor() : this(0, ArrayList<Int>(), ArrayList<Color>(), PaletteMetadata("DLPalette", java.nio.ByteBuffer.allocate(15), java.nio.ByteBuffer.allocate(7))) {
        for (i in 0..255) {
            this.colors.add(Color(i, i, i, 255))
        }
    }

    val redBytes: ByteArray
        get() {
            val redBytes = ByteArray(colors.size)
            for (i in colors.indices) {
                redBytes[i] = colors[i].red!!.toByte()
            }

            return redBytes
        }

    val greenBytes: ByteArray
        get() {
            val greenBytes = ByteArray(colors.size)
            for (i in colors.indices) {
                greenBytes[i] = colors[i].green!!.toByte()
            }

            return greenBytes
        }

    val blueBytes: ByteArray
        get() {
            val blueBytes = ByteArray(colors.size)
            for (i in colors.indices) {
                blueBytes[i] = colors[i].blue!!.toByte()
            }

            return blueBytes
        }

    init {

        this.animationColorOffsets = ArrayList()
        for (i in animationColorOffsets.indices) {
            this.animationColorOffsets.add(animationColorOffsets[i])
        }

        this.colors = ArrayList()
        for (i in colors.indices) {
            this.colors.add(colors[i])
        }
    }
}