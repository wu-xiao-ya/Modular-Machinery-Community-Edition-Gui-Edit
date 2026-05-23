package com.fushu.mmceguiext.common.tile;

import com.fushu.mmceguiext.common.registry.CustomAEItemInputBusRegistry;
import github.kasuminova.mmce.common.tile.MEItemInputBus;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;

public class TileCustomMEItemInputBus extends MEItemInputBus {
    private String definitionId = "";

    public void setDefinitionId(@Nullable String id) {
        this.definitionId = id == null ? "" : id.trim();
    }

    @Nullable
    public String getDefinitionId() {
        return this.definitionId == null || this.definitionId.trim().isEmpty() ? null : this.definitionId.trim();
    }

    @Nullable
    public CustomAEItemInputBusRegistry.Def getDefinition() {
        return CustomAEItemInputBusRegistry.findById(this.definitionId);
    }

    @Override
    public ItemStack getVisualItemStack() {
        if (this.getBlockType() != null) {
            Item item = Item.getItemFromBlock(this.getBlockType());
            if (item != null) {
                return new ItemStack(item);
            }
        }
        return super.getVisualItemStack();
    }

    @Override
    public void readCustomNBT(final NBTTagCompound compound) {
        super.readCustomNBT(compound);
        this.definitionId = compound.hasKey("definitionId") ? compound.getString("definitionId") : "";
    }

    @Override
    public void writeCustomNBT(final NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        compound.setString("definitionId", this.definitionId == null ? "" : this.definitionId);
    }
}
