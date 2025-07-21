package com.gamemode.tkviewer

class Part(val id: Long, val paletteId: Long, val frameIndex: Long, val frameCount: Long, val chunks: List<PartChunk>,
           val partMetadata: PartMetadata) {
    constructor(id: Long, paletteId: Long, frameIndex: Long, frameCount: Long, unknownByte: Byte, unknownInt: Int, unknownByte2: Byte, unknownInt2: Int, unknownInt3: Int, chunkCount: Int, chunks: List<PartChunk>) : this(id, paletteId, frameIndex, frameCount, chunks, PartMetadata(unknownByte.toInt(), unknownInt, unknownByte2.toInt(), unknownInt2, unknownInt3, chunkCount, 0))
}