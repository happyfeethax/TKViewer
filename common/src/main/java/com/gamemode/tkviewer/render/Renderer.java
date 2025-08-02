package com.gamemode.tkviewer.render;

import java.awt.*;

public interface Renderer {
    int getCount();
    int getCount(boolean useEpfCount);
    Image[] getFrames(int index);
    String getInfo(int index);
    int getFrameIndex(int index, int offset);
    void dispose();
    void replaceFrame(int partIndex, int frameIndex, com.gamemode.tkviewer.Frame newFrame);
}
