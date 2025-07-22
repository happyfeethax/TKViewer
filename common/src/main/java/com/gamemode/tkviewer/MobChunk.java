package com.gamemode.tkviewer;

import java.util.List;

public class MobChunk {

    private int blockCount;
    private List<MobBlock> blocks;

    public MobChunk(int blockCount, List<MobBlock> blocks) {
        this.blockCount = blockCount;
        this.blocks = blocks;
    }

    public int getBlockCount() {
        return blockCount;
    }

    public void setBlockCount(int blockCount) {
        this.blockCount = blockCount;
    }

    public List<MobBlock> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<MobBlock> blocks) {
        this.blocks = blocks;
    }
}
