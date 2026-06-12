package com.fushu.mmceguiext.common.integration.theoneprobe;

import com.fushu.mmceguiext.common.registry.CustomHatchRegistry;
import com.fushu.mmceguiext.common.tile.TileCustomAEMixedInputBus;
import com.fushu.mmceguiext.common.tile.TileCustomAEMixedOutputBus;
import com.fushu.mmceguiext.common.tile.TileCustomHatch;
import com.fushu.mmceguiext.common.util.UnitFormat;
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
    private static final int BAR_ENERGY_FILLED = 0xFF3DDC84;
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
        DisplayTypes displayTypes = resolveDisplayTypes(def);
        if (cfg.showItemInfo && displayTypes.item) {
            addItemInfo(box, hatch);
        }
        if (cfg.showFluidInfo && displayTypes.fluid) {
            addFluidInfo(box, hatch);
        }
        if (cfg.showGasInfo && displayTypes.gas) {
            addGasInfo(box, hatch);
        }
        if (cfg.showEnergyInfo && displayTypes.energy) {
            addEnergyInfo(box, hatch);
        }
    }

    private static DisplayTypes resolveDisplayTypes(CustomHatchRegistry.CustomHatchDef def) {
        DisplayTypes out = new DisplayTypes();
        boolean hasMachineDeclaration = def.machineComponents != null && !def.machineComponents.isEmpty();
        if (hasMachineDeclaration) {
            for (CustomHatchRegistry.MachineComponentDef component : def.machineComponents) {
                applyContentType(out, component == null ? null : component.type);
            }
        } else {
            applyContentType(out, def.componentType == null || def.componentType.trim().isEmpty() ? "fluid" : def.componentType);
        }
        applyGuiContentTypes(out, def);
        return out;
    }

    private static void applyGuiContentTypes(DisplayTypes out, CustomHatchRegistry.CustomHatchDef def) {
        if (def.gui == null || def.gui.components == null || def.gui.components.isEmpty()) {
            return;
        }
        for (CustomHatchRegistry.ComponentDef component : def.gui.components) {
            if (component == null) {
                continue;
            }
            if ("slot".equalsIgnoreCase(component.type) && isRuntimeItemSlot(component.role)) {
                out.item = true;
            }
            applyContentType(out, component.content);
            applyTextValueContent(out, component.value);
        }
    }

    private static boolean isRuntimeItemSlot(String role) {
        return "input".equalsIgnoreCase(role) || "output".equalsIgnoreCase(role);
    }

    private static void applyTextValueContent(DisplayTypes out, String value) {
        String normalized = normalizeType(value);
        if (normalized == null) {
            return;
        }
        int dot = normalized.indexOf('.');
        if (dot > 0) {
            applyContentType(out, normalized.substring(0, dot));
        }
    }

    private static void applyContentType(DisplayTypes out, String type) {
        String normalized = normalizeType(type);
        if (normalized == null) {
            return;
        }
        if ("mixed".equals(normalized) || "hybrid".equals(normalized) || "item_fluid_gas".equals(normalized)) {
            out.item = true;
            out.fluid = true;
            out.gas = true;
            return;
        }
        if ("item_fluid".equals(normalized)) {
            out.item = true;
            out.fluid = true;
            return;
        }
        if ("item".equals(normalized)) {
            out.item = true;
            return;
        }
        if ("fluid".equals(normalized)) {
            out.fluid = true;
            return;
        }
        if ("gas".equals(normalized)) {
            out.gas = true;
            return;
        }
        if ("energy".equals(normalized) || "power".equals(normalized) || "fe".equals(normalized) || "rf".equals(normalized)) {
            out.energy = true;
        }
    }

    private static String normalizeType(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase().replace('-', '_');
        return normalized.isEmpty() ? null : normalized;
    }

    private static final class DisplayTypes {
        private boolean item;
        private boolean fluid;
        private boolean gas;
        private boolean energy;
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
        long amount = hatch.getFluidAmountLong();
        long capacity = Math.max(1L, hatch.getFluidCapacity());
        String name = fluid == null ? "空" : fluid.getLocalizedName();

        box.text(TextFormatting.WHITE + "流体:" + TextFormatting.AQUA + name);
        box.progress(
            progressPercent(amount, capacity),
            100,
            box.defaultProgressStyle()
                .width(120)
                .height(14)
                .showText(true)
                .numberFormat(NumberFormat.NONE)
                .prefix(name + ": " + UnitFormat.amountWithUnit(amount, "mB") + " / " + UnitFormat.amountWithUnit(capacity, "mB"))
                .filledColor(BAR_FILLED)
                .alternateFilledColor(BAR_FILLED)
                .backgroundColor(BAR_BACKGROUND)
                .borderColor(BAR_BORDER)
        );
    }

    private static void addGasInfo(IProbeInfo box, TileCustomHatch hatch) {
        try {
            mekanism.api.gas.GasStack gas = hatch.getGasStack();
            long amount = hatch.getGasAmountLong();
            long capacity = Math.max(1L, hatch.getGasCapacity());
            String name = gas == null || gas.getGas() == null ? "空" : gas.getGas().getLocalizedName();

            box.text(TextFormatting.WHITE + "气体:" + TextFormatting.AQUA + name);
            box.progress(
                progressPercent(amount, capacity),
                100,
                box.defaultProgressStyle()
                    .width(120)
                    .height(14)
                    .showText(true)
                    .numberFormat(NumberFormat.NONE)
                    .prefix(name + ": " + UnitFormat.amountWithUnit(amount, "mB") + " / " + UnitFormat.amountWithUnit(capacity, "mB"))
                    .filledColor(BAR_GAS_FILLED)
                    .alternateFilledColor(BAR_GAS_FILLED)
                    .backgroundColor(BAR_BACKGROUND)
                    .borderColor(BAR_BORDER)
            );
        } catch (Throwable ignored) {
            // Mekanism is optional; keep TOP usable if gas classes are absent or transformed late.
        }
    }

    private static void addEnergyInfo(IProbeInfo box, TileCustomHatch hatch) {
        long stored = hatch.getEnergyStoredLong();
        long capacity = Math.max(1L, hatch.getEnergyCapacity());

        box.text(TextFormatting.WHITE + "能量:" + TextFormatting.GREEN + UnitFormat.amountWithUnit(stored, "FE"));
        box.progress(
            progressPercent(stored, capacity),
            100,
            box.defaultProgressStyle()
                .width(120)
                .height(14)
                .showText(true)
                .numberFormat(NumberFormat.NONE)
                .prefix("FE: " + UnitFormat.amountWithUnit(stored, "FE") + " / " + UnitFormat.amountWithUnit(capacity, "FE"))
                .filledColor(BAR_ENERGY_FILLED)
                .alternateFilledColor(BAR_ENERGY_FILLED)
                .backgroundColor(BAR_BACKGROUND)
                .borderColor(BAR_BORDER)
        );
    }

    private static int progressPercent(long amount, long capacity) {
        if (capacity <= 0L || amount <= 0L) {
            return 0;
        }
        return (int) Math.max(0L, Math.min(100L, Math.round(amount * 100.0D / capacity)));
    }
}
