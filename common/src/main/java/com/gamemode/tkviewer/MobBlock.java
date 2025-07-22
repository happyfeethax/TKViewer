package com.gamemode.tkviewer;

public class MobBlock {

    private int frameOffset;
    private int duration;
    private int unknownId1;
    private int transparency;
    private int unknownId2;
    private int unknownId3;

    public MobBlock(int frameOffset, int duration, int unknownId1, int transparency, int unknownId2, int unknownId3) {
        this.frameOffset = frameOffset;
        this.duration = duration;
        this.unknownId1 = unknownId1;
        this.transparency = transparency;
        this.unknownId2 = unknownId2;
        this.unknownId3 = unknownId3;
    }

    public int getFrameOffset() {
        return frameOffset;
    }

    public void setFrameOffset(int frameOffset) {
        this.frameOffset = frameOffset;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getUnknownId1() {
        return unknownId1;
    }

    public void setUnknownId1(int unknownId1) {
        this.unknownId1 = unknownId1;
    }

    public int getTransparency() {
        return transparency;
    }

    public void setTransparency(int transparency) {
        this.transparency = transparency;
    }

    public int getUnknownId2() {
        return unknownId2;
    }

    public void setUnknownId2(int unknownId2) {
        this.unknownId2 = unknownId2;
    }

    public int getUnknownId3() {
        return unknownId3;
    }

    public void setUnknownId3(int unknownId3) {
        this.unknownId3 = unknownId3;
    }
}
