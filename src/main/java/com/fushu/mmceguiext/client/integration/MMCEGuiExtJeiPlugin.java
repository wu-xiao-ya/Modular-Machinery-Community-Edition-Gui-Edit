package com.fushu.mmceguiext.client.integration;

import com.fushu.mmceguiext.client.gui.GuiFactoryControllerResizable;
import com.fushu.mmceguiext.client.gui.GuiMachineControllerResizable;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.gui.IAdvancedGuiHandler;

import java.awt.Rectangle;
import java.util.Collections;
import java.util.List;

@JEIPlugin
public class MMCEGuiExtJeiPlugin implements IModPlugin {
    @Override
    public void register(IModRegistry registry) {
        registry.addAdvancedGuiHandlers(
            new MachineControllerHandler(),
            new FactoryControllerHandler()
        );
    }

    private static class MachineControllerHandler implements IAdvancedGuiHandler<GuiMachineControllerResizable> {
        @Override
        public Class<GuiMachineControllerResizable> getGuiContainerClass() {
            return GuiMachineControllerResizable.class;
        }

        @Override
        public List<Rectangle> getGuiExtraAreas(GuiMachineControllerResizable guiContainer) {
            Rectangle extra = guiContainer.getJeiRightExtensionArea();
            if (extra == null) {
                return Collections.emptyList();
            }
            return Collections.singletonList(extra);
        }
    }

    private static class FactoryControllerHandler implements IAdvancedGuiHandler<GuiFactoryControllerResizable> {
        @Override
        public Class<GuiFactoryControllerResizable> getGuiContainerClass() {
            return GuiFactoryControllerResizable.class;
        }

        @Override
        public List<Rectangle> getGuiExtraAreas(GuiFactoryControllerResizable guiContainer) {
            Rectangle extra = guiContainer.getJeiRightExtensionArea();
            if (extra == null) {
                return Collections.emptyList();
            }
            return Collections.singletonList(extra);
        }
    }
}

