package com.gamemode.tkviewer.resources;

import java.awt.image.BufferedImage;
import java.util.Arrays;

public class Sprite {
    private String name;
    private int width;
    private int height;
    // Storing raw pixel data, assuming 8-bit indexed color for now,
    // or it could be ARGB if processed with a palette.
    // For EPF, this will be the raw indexed data.
    private byte[] pixelData;
    private int frameIndex; // Useful if the sprite is part of a multi-frame EPF

    // Fields from EPF frame struct that might be useful
    private int top;
    private int left;
    private int bottom;
    private int right;
    private int pixelDataOffset;  // Offset within the EPF's pixel_data block
    private int stencilDataOffset; // Offset for stencil data within EPF's pixel_data

    // Constructor for a fully formed sprite frame
    public Sprite(String name, int width, int height, byte[] pixelData, int frameIndex) {
        this.name = name;
        this.width = width;
        this.height = height;
        this.pixelData = pixelData;
        this.frameIndex = frameIndex;
    }

    // Constructor more aligned with EPF frame data
    public Sprite(String name, int frameIndex, int top, int left, int bottom, int right, int pixelDataOffset, int stencilDataOffset) {
        this.name = name; // Base name from the EPF file
        this.frameIndex = frameIndex;
        this.top = top;
        this.left = left;
        this.bottom = bottom;
        this.right = right;
        this.width = right - left;
        this.height = bottom - top;
        this.pixelDataOffset = pixelDataOffset;
        this.stencilDataOffset = stencilDataOffset;
        // pixelData will be populated later by EpfFileHandler
        this.pixelData = null;
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public byte[] getPixelData() {
        return pixelData;
    }

    public void setPixelData(byte[] pixelData) {
        this.pixelData = pixelData;
    }

    public int getFrameIndex() {
        return frameIndex;
    }

    public void setFrameIndex(int frameIndex) {
        this.frameIndex = frameIndex;
    }

    public int getTop() {
        return top;
    }

    public void setTop(int top) {
        this.top = top;
    }

    public int getLeft() {
        return left;
    }

    public void setLeft(int left) {
        this.left = left;
    }

    public int getBottom() {
        return bottom;
    }

    public void setBottom(int bottom) {
        this.bottom = bottom;
    }

    public int getRight() {
        return right;
    }

    public void setRight(int right) {
        this.right = right;
    }

    public int getPixelDataOffset() {
        return pixelDataOffset;
    }

    public void setPixelDataOffset(int pixelDataOffset) {
        this.pixelDataOffset = pixelDataOffset;
    }

    public int getStencilDataOffset() {
        return stencilDataOffset;
    }

    public void setStencilDataOffset(int stencilDataOffset) {
        this.stencilDataOffset = stencilDataOffset;
    }

    @Override
    public String toString() {
        return "Sprite{" +
                "name='" + name + '\'' +
                ", frameIndex=" + frameIndex +
                ", width=" + width +
                ", height=" + height +
                ", top=" + top +
                ", left=" + left +
                ", bottom=" + bottom +
                ", right=" + right +
                ", pixelDataOffset=" + pixelDataOffset +
                ", stencilDataOffset=" + stencilDataOffset +
                ", pixelData.length=" + (pixelData != null ? pixelData.length : "null") +
                '}';
    }
}
