package com.fushu.mmceguiext.common.item;

import com.fushu.mmceguiext.common.block.BlockCustomMEItemInputBus;
import com.fushu.mmceguiext.common.registry.CustomAEItemInputBusRegistry;
import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public class ItemBlockCustomMEItemInputBus extends ItemBlock {
    private final CustomAEItemInputBusRegistry.Def definition;

    public ItemBlockCustomMEItemInputBus(Block block, CustomAEItemInputBusRegistry.Def definition) {
        super(block);
        this.definition = definition;
        setRegistryName(block.getRegistryName());
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        if (this.definition != null && this.definition.displayName != null && !this.definition.displayName.trim().isEmpty()) {
            return this.definition.displayName;
        }
        return super.getItemStackDisplayName(stack);
    }

    public static ItemStack createStack(BlockCustomMEItemInputBus block) {
        return new ItemStack(block);
    }

    @Nonnull
    public CustomAEItemInputBusRegistry.Def getDefinition() {
        return this.definition;
    }
}
