package com.fushu.mmceguiext.common.integration.theoneprobe;

import com.fushu.mmceguiext.common.registry.CustomHatchRegistry;
import com.fushu.mmceguiext.common.tile.TileCustomAEMixedInputBus;
import com.fushu.mmceguiext.common.tile.TileCustomAEMixedOutputBus;
import com.fushu.mmceguiext.common.tile.TileCustomHatch;
import com.fushu.mmceguiext.MMCEGuiExtConfig;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.NumberFormat;
import mcjty.theoneprobe.api.ProbeMode;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;

public class CustomHatchInfoProvider implements IProbeInfoProvider {
    private static final int BAR_FILLED = 0xFF3F8DFF;
    private static final int BAR_GAS_FILLED = 0xFF7BE6FF;
    private static final int BAR_BACKGROUND = 0xFF202020;
    private static final int BAR_BORDER = 0xFF000000;

    @Override
    public String getID() {
        return "mmceguiext:custom_hatch_info_provider";
    }

    @Override
    public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, EntityPlayer player, World world, IBlockState blockState, IProbeHitData data) {
        TileEntity tile = world.getTileEntity(data.getPos());
        if (tile instanceof TileCustomAEMixedInputBus) {
            return;
        }
        if (tile instanceof TileCustomAEMixedOutputBus) {
            return;
        }
        if (!(tile instanceof TileCustomHatch)) {
            return;
        }
        TileCustomHatch hatch = (TileCustomHatch) tile;
        CustomHatchRegistry.CustomHatchDef def = hatch.getDefinition();
        if (def == null) {
            return;
        }

        MMCEGuiExtConfig.CustomHatchTop cfg = MMCEGuiExtConfig.customHatchTop;
        IProbeInfo box = probeInfo.vertical(probeInfo.defaultLayoutStyle().spacing(1));
        String displayName = def.displayName == null || def.displayName.trim().isEmpty() ? "Custom Hatch" : def.displayName;
        if (cfg.showDisplayName) {
            box.text(TextFormatting.AQUA + displayName);
        }
        if (cfg.showDefinitionId && hatch.getDefinitionId() != null) {
            box.text(TextFormatting.DARK_GRAY + hatch.getDefinitionId());
        }
        if (cfg.showItemInfo) {
            addItemInfo(box, hatch);
        }
        if (cfg.showFluidInfo) {
            addFluidInfo(box, hatch);
        }
        if (cfg.showGasInfo) {
            addGasInfo(box, hatch);
        }
    }

    private static String describeComponents(CustomHatchRegistry.CustomHatchDef def) {
        if (def.machineComponents != null && !def.machineComponents.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            for (CustomHatchRegistry.MachineComponentDef component : def.machineComponents) {
                if (component == null || component.type == null || component.type.trim().isEmpty()) {
                    continue;
                }
                if (builder.length() > 0) {
                    builder.append(", ");
                }
                builder.append(component.type.trim());
                if (component.io != null && !component.io.trim().isEmpty()) {
                    builder.append("(").append(component.io.trim()).append(")");
                }
            }
            if (builder.length() > 0) {
                return builder.toString();
            }
        }
        String type = def.componentType == null || def.componentType.trim().isEmpty() ? "fluid" : def.componentType.trim();
        String io = def.ioType == null || def.ioType.trim().isEmpty() ? "input" : def.ioType.trim();
        return type + "(" + io + ")";
    }

    private static void addItemInfo(IProbeInfo box, TileCustomHatch hatch) {
        int[] inputs = hatch.getRecipeInputSlots();
        int[] outputs = hatch.getRecipeOutputSlots();
        int inputStacks = countNonEmptyStacks(hatch, inputs);
        int inputItems = countItems(hatch, inputs);
        int outputStacks = countNonEmptyStacks(hatch, outputs);
        int outputItems = countItems(hatch, outputs);

        if (inputs.length <= 0 && outputs.length <= 0) {
            return;
        }

        IProbeInfo itemBox = box.vertical(box.defaultLayoutStyle().spacing(0));
        if (inputs.length > 0) {
            itemBox.text(
                TextFormatting.GREEN + "Input Items: "
                    + TextFormatting.WHITE + inputItems
                    + TextFormatting.GRAY + " (" + inputStacks + "/" + inputs.length + " slots)"
            );
        }
        if (outputs.length > 0) {
            itemBox.text(
                TextFormatting.GOLD + "Output Items: "
                    + TextFormatting.WHITE + outputItems
                    + TextFormatting.GRAY + " (" + outputStacks + "/" + outputs.length + " slots)"
            );
        }
    }

    private static int countItems(TileCustomHatch hatch, int[] slots) {
        int count = 0;
        for (int slot : slots) {
            if (slot < 0 || slot >= hatch.getInventory().getSlots()) {
                continue;
            }
            ItemStack stack = hatch.getInventory().getStackInSlot(slot);
            if (!stack.isEmpty()) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static int countNonEmptyStacks(TileCustomHatch hatch, int[] slots) {
        int count = 0;
        for (int slot : slots) {
            if (slot < 0 || slot >= hatch.getInventory().getSlots()) {
                continue;
            }
            if (!hatch.getInventory().getStackInSlot(slot).isEmpty()) {
                count++;
            }
        }
        return count;
    }

    private static void addFluidInfo(IProbeInfo box, TileCustomHatch hatch) {
        FluidStack fluid = hatch.getFluidStack();
        int amount = fluid == null ? 0 : fluid.amount;
        int capacity = Math.max(1, hatch.getFluidCapacity());
        String name = fluid == null ? "空" : fluid.getLocalizedName();

        box.text(TextFormatting.WHITE + "流体:" + TextFormatting.AQUA + name);
        box.progress(
            amount,
            capacity,
            box.defaultProgressStyle()
                .width(120)
                .height(14)
                .showText(true)
                .numberFormat(NumberFormat.COMMAS)
                .prefix(name + ": ")
                .suffix(" / " + capacity + " mB")
                .filledColor(BAR_FILLED)
                .alternateFilledColor(BAR_FILLED)
                .backgroundColor(BAR_BACKGROUND)
                .borderColor(BAR_BORDER)
        );
    }

    private static void addGasInfo(IProbeInfo box, TileCustomHatch hatch) {
        try {
            mekanism.api.gas.GasStack gas = hatch.getGasStack();
            int amount = gas == null ? 0 : gas.amount;
            int capacity = Math.max(1, hatch.getGasCapacity());
            String name = gas == null || gas.getGas() == null ? "空" : gas.getGas().getLocalizedName();

            box.text(TextFormatting.WHITE + "气体:" + TextFormatting.AQUA + name);
            box.progress(
                amount,
                capacity,
                box.defaultProgressStyle()
                    .width(120)
                    .height(14)
                    .showText(true)
                    .numberFormat(NumberFormat.COMMAS)
                    .prefix(name + ": ")
                    .suffix(" / " + capacity + " mB")
                    .filledColor(BAR_GAS_FILLED)
                    .alternateFilledColor(BAR_GAS_FILLED)
                    .backgroundColor(BAR_BACKGROUND)
                    .borderColor(BAR_BORDER)
            );
        } catch (Throwable ignored) {
            // Mekanism is optional; keep TOP usable if gas classes are absent or transformed late.
        }
    }
}
