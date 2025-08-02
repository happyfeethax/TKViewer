package com.gamemode.tkviewer

import com.gamemode.tkviewer.resources.Stencil
import java.awt.image.BufferedImage
import java.nio.ByteBuffer
import kotlin.math.pow
import kotlin.math.sqrt

class Frame(
    var top: Int, var left: Int, var bottom: Int, var right: Int, val width: Int, val height: Int,
    var pixelDataOffset: Long, var stencilDataOffset: Long, var rawPixelData: ByteBuffer,
    var rawStencilData: ByteBuffer, var stencil: Stencil
) {

    constructor(image: BufferedImage, palette: Palette) : this(
        0, 0, image.height, image.width, image.width, image.height,
        0L, 0L, ByteBuffer.allocate(0), ByteBuffer.allocate(0), Stencil(mutableListOf<BooleanArray>())
    ) {
        val pixelData = ByteArray(image.width * image.height)
        val stencilRows = mutableListOf<BooleanArray>()

        for (y in 0 until image.height) {
            val stencilRow = BooleanArray(image.width)
            for (x in 0 until image.width) {
                val color = java.awt.Color(image.getRGB(x, y), true)
                if (color.alpha == 0) {
                    pixelData[y * image.width + x] = 0
                    stencilRow[x] = false
                } else {
                    pixelData[y * image.width + x] = findClosestPaletteIndex(color, palette).toByte()
                    stencilRow[x] = true
                }
            }
            stencilRows.add(stencilRow)
        }

        this.rawPixelData = ByteBuffer.wrap(pixelData)
        this.stencil = Stencil(stencilRows)
        this.rawStencilData = this.stencil.toByteBuffer()
    }

    private fun findClosestPaletteIndex(color: java.awt.Color, palette: Palette): Int {
        var closestIndex = 0
        var minDistance = Double.MAX_VALUE

        for (i in 0 until palette.colors.size) {
            val paletteColor = palette.colors[i]
            val distance = colorDistance(color, paletteColor)
            if (distance < minDistance) {
                minDistance = distance
                closestIndex = i
            }
        }

        return closestIndex
    }

    private fun colorDistance(c1: java.awt.Color, c2: com.gamemode.tkviewer.Color): Double {
        val redDiff = (c1.red - c2.red!!).toDouble()
        val greenDiff = (c1.green - c2.green!!).toDouble()
        val blueDiff = (c1.blue - c2.blue!!).toDouble()
        return sqrt(redDiff.pow(2) + greenDiff.pow(2) + blueDiff.pow(2))
    }
}