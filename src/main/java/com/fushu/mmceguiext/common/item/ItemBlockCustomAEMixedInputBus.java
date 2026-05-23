package com.fushu.mmceguiext.common.item;

import com.fushu.mmceguiext.common.block.BlockCustomAEMixedInputBus;
import com.fushu.mmceguiext.common.registry.CustomAEMixedInputBusRegistry;
import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public class ItemBlockCustomAEMixedInputBus extends ItemBlock {
    private final CustomAEMixedInputBusRegistry.Def definition;

    public ItemBlockCustomAEMixedInputBus(Block block, CustomAEMixedInputBusRegistry.Def definition) {
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

    public static ItemStack createStack(BlockCustomAEMixedInputBus block) {
        return new ItemStack(block);
    }

    @Nonnull
    public CustomAEMixedInputBusRegistry.Def getDefinition() {
        return this.definition;
    }
}
