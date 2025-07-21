package com.gamemode.tkviewer

class PartChunk(val blockCount: Int, val unknownId1: Int, val blocks: List<PartBlock>) {
    constructor(blockCount: Int, unknownId1: Int, unknownId2: Int, blocks: List<PartBlock>) : this(blockCount, unknownId1, blocks)
}