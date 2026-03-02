package com.fushu.mmceguiext.client;

import com.fushu.mmceguiext.MMCEGuiExtConfig;
import com.fushu.mmceguiext.client.config.MachineGuiStyleManager;
import com.fushu.mmceguiext.client.gui.GuiRenderUtils;
import com.fushu.mmceguiext.client.gui.GuiFactoryControllerResizable;
import com.fushu.mmceguiext.client.gui.GuiMachineControllerResizable;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.client.gui.GuiFactoryController;
import hellfirepvp.modularmachinery.client.gui.GuiMachineController;
import hellfirepvp.modularmachinery.common.container.ContainerController;
import hellfirepvp.modularmachinery.common.container.ContainerFactoryController;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientGuiEventHandler {
    private static final String DEFAULT_MACHINE_BG = "modularmachinery:textures/gui/guicontroller_large.png";
    private static final String DEFAULT_FACTORY_BG = "modularmachinery:textures/gui/guifactory.png";
    private static final int DEFAULT_SPECIAL_THREAD_BG_COLOR = 0xFFB2E5FF;

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (!MMCEGuiExtConfig.enabled || event.getGui() == null) {
            return;
        }

        GuiScreen gui = event.getGui();
        if (gui instanceof GuiMachineControllerResizable || gui instanceof GuiFactoryControllerResizable) {
            return;
        }

        if (MMCEGuiExtConfig.machineController.replaceGui && gui instanceof GuiMachineController) {
            ContainerController container = ((GuiMachineController) gui).getContainer();
            if (shouldReplaceMachineController(container)) {
                event.setGui(new GuiMachineControllerResizable(container));
            }
            return;
        }

        if (MMCEGuiExtConfig.factoryController.replaceGui && gui instanceof GuiFactoryController) {
            ContainerFactoryController container = ((GuiFactoryController) gui).getContainer();
            if (shouldReplaceFactoryController(container)) {
                event.setGui(new GuiFactoryControllerResizable(container));
            }
        }
    }

    private boolean shouldReplaceMachineController(ContainerController container) {
        DynamicMachine machine = resolveMachine(container.getOwner().getFoundMachine(), container.getOwner().getBlueprintMachine());
        MachineGuiStyleManager.ControllerStyle style = MachineGuiStyleManager.resolveMachineController(machine);

        String texture = pickTexture(style.backgroundTexture, MMCEGuiExtConfig.machineController.backgroundTexture);
        boolean hasCustomTexture = hasCustomTexture(texture, DEFAULT_MACHINE_BG);
        boolean hideDefault = pickHideDefault(style.hideDefaultBackground, MMCEGuiExtConfig.machineController.hideDefaultBackground);
        boolean hasMachineStyleOverride = hasMachineStyleOverride(style);
        return hasCustomTexture || hideDefault || hasMachineStyleOverride;
    }

    private boolean shouldReplaceFactoryController(ContainerFactoryController container) {
        DynamicMachine machine = resolveMachine(container.getOwner().getFoundMachine(), container.getOwner().getBlueprintMachine());
        MachineGuiStyleManager.ControllerStyle style = MachineGuiStyleManager.resolveFactoryController(machine);

        String texture = pickTexture(style.backgroundTexture, MMCEGuiExtConfig.factoryController.backgroundTexture);
        boolean hasCustomTexture = hasCustomTexture(texture, DEFAULT_FACTORY_BG);
        boolean hideDefault = pickHideDefault(style.hideDefaultBackground, MMCEGuiExtConfig.factoryController.hideDefaultBackground);
        boolean hasMachineStyleOverride = hasMachineStyleOverride(style);
        boolean customSpecialThreadColor = hasCustomSpecialThreadColor(style);
        return hasCustomTexture || hideDefault || hasMachineStyleOverride || customSpecialThreadColor;
    }

    private DynamicMachine resolveMachine(DynamicMachine found, DynamicMachine blueprint) {
        return found != null ? found : blueprint;
    }

    private String pickTexture(String override, String fallback) {
        if (override != null && !override.trim().isEmpty()) {
            return override.trim();
        }
        return fallback == null ? "" : fallback.trim();
    }

    private boolean pickHideDefault(Boolean override, boolean fallback) {
        return override != null ? override.booleanValue() : fallback;
    }

    private boolean hasCustomTexture(String texture, String defaultTexture) {
        if (texture == null || texture.trim().isEmpty()) {
            return false;
        }
        return !defaultTexture.equalsIgnoreCase(texture.trim());
    }

    private boolean hasCustomSpecialThreadColor(MachineGuiStyleManager.ControllerStyle style) {
        if (style.specialThreadBackgroundColor != null) {
            return style.specialThreadBackgroundColor.intValue() != DEFAULT_SPECIAL_THREAD_BG_COLOR;
        }
        int global = GuiRenderUtils.parseColorARGBOrDefault(
            MMCEGuiExtConfig.factoryController.specialThreadBackgroundColor,
            DEFAULT_SPECIAL_THREAD_BG_COLOR
        );
        return global != DEFAULT_SPECIAL_THREAD_BG_COLOR;
    }

    private boolean hasMachineStyleOverride(MachineGuiStyleManager.ControllerStyle style) {
        return style != MachineGuiStyleManager.ControllerStyle.EMPTY;
    }
}
