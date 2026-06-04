package com.fushu.mmceguiext.common.item;

import com.fushu.mmceguiext.common.block.BlockCustomAEMixedOutputBus;
import com.fushu.mmceguiext.common.registry.CustomAEMixedOutputBusRegistry;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nonnull;

public class ItemBlockCustomAEMixedOutputBus extends ItemBlock {
    private final CustomAEMixedOutputBusRegistry.Def definition;

    public ItemBlockCustomAEMixedOutputBus(Block block, CustomAEMixedOutputBusRegistry.Def definition) {
        super(block);
        this.definition = definition;
        setRegistryNameSafe(this, block.getRegistryName());
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        if (this.definition != null && this.definition.displayName != null && !this.definition.displayName.trim().isEmpty()) {
            return this.definition.displayName;
        }
        return super.getItemStackDisplayName(stack);
    }

    public static ItemStack createStack(BlockCustomAEMixedOutputBus block) {
        return new ItemStack(block);
    }

    @Nonnull
    public CustomAEMixedOutputBusRegistry.Def getDefinition() {
        return this.definition;
    }

    private static void setRegistryNameSafe(final Item item, @Nonnull final ResourceLocation name) {
        item.setRegistryName(name);
    }
}
