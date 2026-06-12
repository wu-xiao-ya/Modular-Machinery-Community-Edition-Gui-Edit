package com.fushu.mmceguiext.common.item;

import com.fushu.mmceguiext.common.block.BlockCustomHatch;
import com.fushu.mmceguiext.common.registry.CustomHatchRegistry;
import net.minecraft.block.Block;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.NonNullList;
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
        CustomHatchRegistry.CustomHatchDef def = resolveStackDefinition(stack);
        if (def != null && def.displayName != null && !def.displayName.trim().isEmpty()) {
            return def.displayName;
        }
        return super.getItemStackDisplayName(stack);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<String> tooltip, ITooltipFlag flagIn) {
        super.addInformation(stack, worldIn, tooltip, flagIn);
        CustomHatchRegistry.CustomHatchDef def = resolveStackDefinition(stack);
        if (def == null || def.tips == null || def.tips.isEmpty()) {
            return;
        }
        tooltip.addAll(def.tips);
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> items) {
        if (!isInCreativeTab(tab)) {
            return;
        }
        if (this.definition != null) {
            items.add(new ItemStack(this));
            return;
        }
        for (CustomHatchRegistry.CustomHatchDef def : CustomHatchRegistry.getRegistered()) {
            if (def == null || def.id == null || def.id.trim().isEmpty()) {
                continue;
            }
            ItemStack stack = new ItemStack(this);
            NBTTagCompound tag = new NBTTagCompound();
            tag.setString("hatchId", def.id);
            stack.setTagCompound(tag);
            items.add(stack);
        }
    }

    public static ItemStack createStack(BlockCustomHatch block) {
        return new ItemStack(block);
    }

    @Nullable
    public CustomHatchRegistry.CustomHatchDef getDefinition() {
        return this.definition;
    }

    @Nullable
    private CustomHatchRegistry.CustomHatchDef resolveStackDefinition(ItemStack stack) {
        if (this.definition != null) {
            return this.definition;
        }
        if (stack == null || !stack.hasTagCompound()) {
            return null;
        }
        String id = com.fushu.mmceguiext.common.util.CustomIdValidator.readSanitizedString(stack.getTagCompound(), "hatchId");
        return CustomHatchRegistry.findById(id);
    }

    private static void setRegistryNameSafe(final Item item, @Nonnull final ResourceLocation name) {
        item.setRegistryName(name);
    }
}
