package com.fushu.mmceguiext.client.gui;

import org.lwjgl.input.Keyboard;

final class SubGuiBackKeyPolicy {
    private SubGuiBackKeyPolicy() {
    }

    static boolean isBackKey(int keyCode) {
        return keyCode == Keyboard.KEY_ESCAPE;
    }
}
