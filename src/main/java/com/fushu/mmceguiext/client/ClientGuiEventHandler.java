package com.fushu.mmceguiext.client;

import com.fushu.mmceguiext.MMCEGuiExtConfig;
import com.fushu.mmceguiext.client.config.MachineGuiStyleManager;
import com.fushu.mmceguiext.client.gui.GuiRenderUtils;
import com.fushu.mmceguiext.client.gui.GuiFactoryControllerResizable;
import com.fushu.mmceguiext.client.gui.GuiFluidHatchCustom;
import com.fushu.mmceguiext.client.gui.GuiFluidProcessorHatchCustom;
import com.fushu.mmceguiext.client.gui.GuiItemBusCustom;
import com.fushu.mmceguiext.client.gui.GuiMEItemInputBusCustom;
import com.fushu.mmceguiext.client.gui.GuiMachineControllerResizable;
import com.fushu.mmceguiext.client.gui.GuiUpgradeBusCustom;
import com.fushu.mmceguiext.common.tile.TileCustomMEItemInputBus;
import com.fushu.mmceguiext.common.registry.CustomHatchRegistry;
import github.kasuminova.mmce.client.gui.GuiMEItemInputBus;
import github.kasuminova.mmce.common.tile.MEItemInputBus;
import hellfirepvp.modularmachinery.common.machine.DynamicMachine;
import hellfirepvp.modularmachinery.client.gui.GuiContainerFluidHatch;
import hellfirepvp.modularmachinery.client.gui.GuiContainerItemBus;
import hellfirepvp.modularmachinery.client.gui.GuiContainerUpgradeBus;
import hellfirepvp.modularmachinery.client.gui.GuiFactoryController;
import hellfirepvp.modularmachinery.client.gui.GuiMachineController;
import hellfirepvp.modularmachinery.common.container.ContainerController;
import hellfirepvp.modularmachinery.common.container.ContainerFactoryController;
import hellfirepvp.modularmachinery.common.tiles.base.TileFluidTank;
import hellfirepvp.modularmachinery.common.tiles.TileUpgradeBus;
import hellfirepvp.modularmachinery.common.tiles.base.TileItemBus;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.tileentity.TileEntity;
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
        if (!MMCEGuiExtConfig.novaEngCoreCompatibilityMode) {
            MachineGuiStyleManager.clearPinnedCache();
        }

        GuiScreen gui = event.getGui();
        if (gui instanceof GuiMachineControllerResizable
            || gui instanceof GuiFactoryControllerResizable
            || gui instanceof GuiFluidHatchCustom
            || gui instanceof GuiFluidProcessorHatchCustom
            || gui instanceof GuiItemBusCustom
            || gui instanceof GuiMEItemInputBusCustom
            || gui instanceof GuiUpgradeBusCustom) {
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
            return;
        }

        if (MMCEGuiExtConfig.itemBus.replaceGui && gui instanceof GuiContainerItemBus) {
            TileItemBus itemBus = ((GuiContainerItemBus) gui).getContainer().getOwner();
            if (itemBus != null) {
                event.setGui(new GuiItemBusCustom(itemBus, net.minecraft.client.Minecraft.getMinecraft().player));
            }
            return;
        }

        if (MMCEGuiExtConfig.aeBus.enabled && gui instanceof GuiMEItemInputBus) {
            net.minecraft.inventory.Container container = ((GuiMEItemInputBus) gui).inventorySlots;
            if (container instanceof github.kasuminova.mmce.common.container.ContainerMEItemInputBus) {
                MEItemInputBus bus = ((github.kasuminova.mmce.common.container.ContainerMEItemInputBus) container).getOwner();
                if (bus instanceof TileCustomMEItemInputBus) {
                    event.setGui(new GuiMEItemInputBusCustom(bus, net.minecraft.client.Minecraft.getMinecraft().player));
                }
            }
            return;
        }

        if (MMCEGuiExtConfig.fluidHatch.replaceGui && gui instanceof GuiContainerFluidHatch) {
            TileEntity owner = ((GuiContainerFluidHatch) gui).getContainer().getOwner();
            CustomHatchRegistry.CustomHatchDef customHatch = resolveCustomHatch(owner);
            if (customHatch != null && isFluidProcessorHatch(owner)) {
                event.setGui(new GuiFluidProcessorHatchCustom(owner, net.minecraft.client.Minecraft.getMinecraft().player, customHatch));
                return;
            }
            if (owner instanceof TileFluidTank) {
                TileFluidTank fluidTank = (TileFluidTank) owner;
                event.setGui(new GuiFluidHatchCustom(fluidTank, net.minecraft.client.Minecraft.getMinecraft().player));
            }
            return;
        }

        if (MMCEGuiExtConfig.upgradeBus.replaceGui && gui instanceof GuiContainerUpgradeBus) {
            TileUpgradeBus upgradeBus = ((GuiContainerUpgradeBus) gui).getContainer().getOwner();
            if (upgradeBus != null) {
                event.setGui(new GuiUpgradeBusCustom(upgradeBus, net.minecraft.client.Minecraft.getMinecraft().player));
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

    private boolean isFluidProcessorHatch(TileEntity owner) {
        return owner != null && "hellfirepvp.modularmachinery.common.tiles.TileFluidProcessorHatch".equals(owner.getClass().getName());
    }

    private CustomHatchRegistry.CustomHatchDef resolveCustomHatch(TileEntity owner) {
        if (owner == null || owner.getBlockType() == null || owner.getBlockType().getRegistryName() == null) {
            return null;
        }
        String full = owner.getBlockType().getRegistryName().toString();
        CustomHatchRegistry.CustomHatchDef def = CustomHatchRegistry.findById(full);
        if (def != null) {
            return def;
        }
        return CustomHatchRegistry.findById(owner.getBlockType().getRegistryName().getPath());
    }
}
