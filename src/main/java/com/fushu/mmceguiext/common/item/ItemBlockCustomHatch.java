package com.fushu.mmceguiext.common.item;

import com.fushu.mmceguiext.common.block.BlockCustomHatch;
import com.fushu.mmceguiext.common.registry.CustomHatchRegistry;
import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ItemBlockCustomHatch extends ItemBlock {
    private final CustomHatchRegistry.CustomHatchDef definition;

    public ItemBlockCustomHatch(Block block, CustomHatchRegistry.CustomHatchDef definition) {
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

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        if (this.definition == null || this.definition.tips == null || this.definition.tips.isEmpty()) {
            return;
        }
        tooltip.addAll(this.definition.tips);
    }

    public static ItemStack createStack(BlockCustomHatch block) {
        return new ItemStack(block);
    }

    @Nonnull
    public CustomHatchRegistry.CustomHatchDef getDefinition() {
        return this.definition;
    }

    private static void setRegistryNameSafe(final Item item, @Nonnull final ResourceLocation name) {
        item.setRegistryName(name);
    }
}
