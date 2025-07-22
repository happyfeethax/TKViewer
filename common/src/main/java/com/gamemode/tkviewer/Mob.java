package com.gamemode.tkviewer;

import java.util.List;

public class Mob {

    private long frameIndex;
    private int chunkCount;
    private byte unknown1;
    private int paletteId;
    private List<MobChunk> chunks;

    public Mob(long frameIndex, int chunkCount, byte unknown1, int paletteId, List<MobChunk> chunks) {
        this.frameIndex = frameIndex;
        this.chunkCount = chunkCount;
        this.unknown1 = unknown1;
        this.paletteId = paletteId;
        this.chunks = chunks;
    }

    public long getFrameIndex() {
        return frameIndex;
    }

    public void setFrameIndex(long frameIndex) {
        this.frameIndex = frameIndex;
    }

    public int getChunkCount() {
        return chunkCount;
    }

    public void setChunkCount(int chunkCount) {
        this.chunkCount = chunkCount;
    }

    public byte getUnknown1() {
        return unknown1;
    }

    public void setUnknown1(byte unknown1) {
        this.unknown1 = unknown1;
    }

    public int getPaletteId() {
        return paletteId;
    }

    public void setPaletteId(int paletteId) {
        this.paletteId = paletteId;
    }

    public List<MobChunk> getChunks() {
        return chunks;
    }

    public void setChunks(List<MobChunk> chunks) {
        this.chunks = chunks;
    }
}
