package com.fushu.mmceguiext.common.container;

import com.fushu.mmceguiext.common.registry.CustomHatchRegistry;
import com.fushu.mmceguiext.common.tile.TileCustomHatch;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;
import java.lang.reflect.Method;

public class ContainerFluidProcessorHatchCustom extends hellfirepvp.modularmachinery.common.container.ContainerBase<TileEntity> {
    private final int customSlotCount;

    public ContainerFluidProcessorHatchCustom(TileEntity owner, EntityPlayer opening, CustomHatchRegistry.CustomHatchDef def) {
        super(owner, opening);
        IItemHandlerModifiable itemHandler = resolveGuiAccess(owner);
        if (itemHandler != null) {
            int added = 0;
            int nextIndex = 0;
            if (def.gui != null && def.gui.components != null) {
                for (CustomHatchRegistry.ComponentDef component : def.gui.components) {
                    if (component == null || !"slot".equalsIgnoreCase(component.type)) {
                        continue;
                    }
                    int slotIndex = component.index >= 0 ? component.index : nextIndex++;
                    if (slotIndex < 0 || slotIndex >= itemHandler.getSlots()) {
                        continue;
                    }
                    addSlotToContainer(new SlotItemHandler(itemHandler, slotIndex, component.x, component.y));
                    added++;
                }
            }
            if (added == 0 && itemHandler.getSlots() >= 2) {
                addSlotToContainer(new SlotItemHandler(itemHandler, 0, def.inputSlot.x, def.inputSlot.y));
                addSlotToContainer(new SlotItemHandler(itemHandler, 1, def.outputSlot.x, def.outputSlot.y));
                added = 2;
            }
            this.customSlotCount = added;
        } else {
            this.customSlotCount = 0;
        }
    }

    @Nonnull
    @Override
    public ItemStack transferStackInSlot(@Nonnull EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();

            boolean changed = false;
            if (index < 36) {
                if (this.customSlotCount > 0 && this.mergeItemStack(itemstack1, 36, 36 + this.customSlotCount, false)) {
                    changed = true;
                }
            }

            if (!changed) {
                if (index < 27) {
                    if (!this.mergeItemStack(itemstack1, 27, 36, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index < 36) {
                    if (!this.mergeItemStack(itemstack1, 0, 27, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (!this.mergeItemStack(itemstack1, 0, 36, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (itemstack1.getCount() == 0) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(playerIn, itemstack1);
        }

        return itemstack;
    }

    private static IItemHandlerModifiable resolveGuiAccess(TileEntity owner) {
        if (owner instanceof TileCustomHatch) {
            return ((TileCustomHatch) owner).getInventory().asGUIAccess();
        }
        try {
            Method getInventory = owner.getClass().getMethod("getInventory");
            Object inventory = getInventory.invoke(owner);
            if (inventory == null) {
                return null;
            }
            Method asGuiAccess = inventory.getClass().getMethod("asGUIAccess");
            Object guiAccess = asGuiAccess.invoke(inventory);
            return guiAccess instanceof IItemHandlerModifiable ? (IItemHandlerModifiable) guiAccess : null;
        } catch (Exception ignored) {
            return null;
        }
    }
}
