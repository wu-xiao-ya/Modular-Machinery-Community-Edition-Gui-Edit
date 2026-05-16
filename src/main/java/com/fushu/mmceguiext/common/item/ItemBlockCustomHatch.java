package com.fushu.mmceguiext.common.item;

import com.fushu.mmceguiext.common.block.BlockCustomHatch;
import com.fushu.mmceguiext.common.registry.CustomHatchRegistry;
import net.minecraft.block.Block;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;

import javax.annotation.Nonnull;

public class ItemBlockCustomHatch extends ItemBlock {
    private final CustomHatchRegistry.CustomHatchDef definition;

    public ItemBlockCustomHatch(Block block, CustomHatchRegistry.CustomHatchDef definition) {
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

    public static ItemStack createStack(BlockCustomHatch block) {
        return new ItemStack(block);
    }

    @Nonnull
    public CustomHatchRegistry.CustomHatchDef getDefinition() {
        return this.definition;
    }
}
