package com.fushu.mmceguiext.client.gui;

import org.junit.Test;
import org.lwjgl.input.Keyboard;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SubGuiBackKeyPolicyTest {
    @Test
    public void escapeIsTreatedAsBackKey() {
        assertTrue(SubGuiBackKeyPolicy.isBackKey(Keyboard.KEY_ESCAPE));
    }

    @Test
    public void nonEscapeKeysDoNotTriggerBackNavigation() {
        assertFalse(SubGuiBackKeyPolicy.isBackKey(Keyboard.KEY_RETURN));
    }
}
