package com.fushu.mmceguiext.client.gui;

public class PanelRect {
    private final int x;
    private final int y;
    private final int width;
    private final int height;

    public PanelRect(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getRight() {
        return x + width;
    }

    public int getBottom() {
        return y + height;
    }
}
