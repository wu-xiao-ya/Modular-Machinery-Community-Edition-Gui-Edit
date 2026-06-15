package com.fushu.mmceguiext.common.container;

import appeng.container.slot.AppEngSlot;
import com.fushu.mmceguiext.common.util.AECapacityCardSupport;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;

public class SlotAEMixedCapacityCard extends AppEngSlot {
    public SlotAEMixedCapacityCard(IItemHandler itemHandler, int index, int xPosition, int yPosition) {
        super(itemHandler, index, xPosition, yPosition);
    }

    @Override
    public boolean isItemValid(@Nonnull ItemStack stack) {
        return AECapacityCardSupport.isCapacityCard(stack) && super.isItemValid(stack);
    }

    @Override
    public int getItemStackLimit(@Nonnull ItemStack stack) {
        return 1;
    }

    @Override
    public int getSlotStackLimit() {
        return 1;
    }
}
