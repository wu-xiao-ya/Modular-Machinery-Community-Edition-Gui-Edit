package com.fushu.mmceguiext.common.container;

import com.fushu.mmceguiext.MMCEGuiExt;
import com.fushu.mmceguiext.common.network.PktCustomHatchEnergySync;
import com.fushu.mmceguiext.common.registry.CustomHatchRegistry;
import com.fushu.mmceguiext.common.tile.TileCustomHatch;
import com.fushu.mmceguiext.common.util.EnergyAccessHelper;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Method;

public class ContainerFluidProcessorHatchCustom extends hellfirepvp.modularmachinery.common.container.ContainerBase<TileEntity> {
    private final int customSlotCount;
    private final List<Integer> customInputSlotIndices = new ArrayList<Integer>();
    private long lastEnergyCapacity = Long.MIN_VALUE;
    private long lastEnergyTransfer = Long.MIN_VALUE;
    private long lastEnergyStored = Long.MIN_VALUE;

    public ContainerFluidProcessorHatchCustom(TileEntity owner, EntityPlayer opening, CustomHatchRegistry.CustomHatchDef def) {
        super(owner, opening);
        if (owner instanceof TileCustomHatch) {
            ((TileCustomHatch) owner).refreshDefinitionLayout(def);
        }
        IItemHandlerModifiable itemHandler = resolveGuiAccess(owner);
        if (itemHandler != null) {
            int added = 0;
            int nextIndex = 0;
            if (def.gui != null && def.gui.components != null) {
                for (CustomHatchRegistry.ComponentDef component : def.gui.components) {
                    if (component == null || !"slot".equalsIgnoreCase(component.type)) {
                        continue;
                    }
                    if (!isRuntimeSlotRole(component.role)) {
                        continue;
                    }
                    int slotIndex;
                    if (component.index >= 0) {
                        slotIndex = component.index;
                        nextIndex = Math.max(nextIndex, slotIndex + 1);
                    } else {
                        slotIndex = nextIndex++;
                    }
                    if (slotIndex < 0 || slotIndex >= itemHandler.getSlots()) {
                        continue;
                    }
                    boolean insertable = "input".equalsIgnoreCase(component.role);
                    addCustomSlot(itemHandler, slotIndex, component.x, component.y, insertable);
                    added++;
                }
            }
            if (added == 0 && !hasGuiSlotComponents(def) && itemHandler.getSlots() >= 2) {
                addCustomSlot(itemHandler, 0, def.inputSlot.x, def.inputSlot.y, true);
                addCustomSlot(itemHandler, 1, def.outputSlot.x, def.outputSlot.y, false);
                added = 2;
            }
            this.customSlotCount = added;
        } else {
            this.customSlotCount = 0;
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return this.owner != null
            && playerIn != null
            && !this.owner.isInvalid()
            && this.owner.getWorld() == playerIn.world
            && this.owner.getWorld().getTileEntity(this.owner.getPos()) == this.owner
            && playerIn.getDistanceSqToCenter(this.owner.getPos()) <= 64D;
    }

    @Override
    public void addListener(@Nonnull IContainerListener listener) {
        super.addListener(listener);
        sendEnergyState(listener);
    }

    @Override
    public void detectAndSendChanges() {
        super.detectAndSendChanges();
        if (!(this.owner instanceof TileCustomHatch)) {
            return;
        }
        TileCustomHatch hatch = (TileCustomHatch) this.owner;
        long capacity = EnergyAccessHelper.getCapacity(hatch);
        long transfer = hatch.getEnergyTransfer();
        long stored = EnergyAccessHelper.getStored(hatch);
        if (capacity == this.lastEnergyCapacity && transfer == this.lastEnergyTransfer && stored == this.lastEnergyStored) {
            return;
        }
        this.lastEnergyCapacity = capacity;
        this.lastEnergyTransfer = transfer;
        this.lastEnergyStored = stored;
        for (IContainerListener listener : this.listeners) {
            sendEnergyState(listener, capacity, transfer, stored);
        }
    }

    @Nonnull
    @Override
    public ItemStack transferStackInSlot(@Nonnull EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        if (index < 0 || index >= this.inventorySlots.size()) {
            return itemstack;
        }
        Slot slot = this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();

            boolean changed = false;
            if (index < 36) {
                if (!this.customInputSlotIndices.isEmpty() && this.mergeItemStackIntoCustomInputs(itemstack1)) {
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

    private void sendEnergyState(IContainerListener listener) {
        if (!(this.owner instanceof TileCustomHatch)) {
            return;
        }
        TileCustomHatch hatch = (TileCustomHatch) this.owner;
        long capacity = EnergyAccessHelper.getCapacity(hatch);
        long transfer = hatch.getEnergyTransfer();
        long stored = EnergyAccessHelper.getStored(hatch);
        sendEnergyState(listener, capacity, transfer, stored);
        this.lastEnergyCapacity = capacity;
        this.lastEnergyTransfer = transfer;
        this.lastEnergyStored = stored;
    }

    private void sendEnergyState(IContainerListener listener, long capacity, long transfer, long stored) {
        if (!(listener instanceof EntityPlayerMP) || this.owner == null || this.owner.getPos() == null) {
            return;
        }
        MMCEGuiExt.NET_CHANNEL.sendTo(
            new PktCustomHatchEnergySync(this.owner.getPos(), stored, capacity, transfer),
            (EntityPlayerMP) listener
        );
    }

    private static boolean hasGuiSlotComponents(CustomHatchRegistry.CustomHatchDef def) {
        if (def == null || def.gui == null || def.gui.components == null) {
            return false;
        }
        for (CustomHatchRegistry.ComponentDef component : def.gui.components) {
            if (component != null && "slot".equalsIgnoreCase(component.type)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRuntimeSlotRole(String role) {
        return "input".equalsIgnoreCase(role) || "output".equalsIgnoreCase(role);
    }

    private void addCustomSlot(IItemHandlerModifiable itemHandler, int slotIndex, int x, int y, boolean insertable) {
        addSlotToContainer(new SlotItemHandler(itemHandler, slotIndex, x, y) {
            @Override
            public boolean isItemValid(@Nonnull ItemStack stack) {
                return insertable && super.isItemValid(stack);
            }
        });
        if (insertable) {
            this.customInputSlotIndices.add(this.inventorySlots.size() - 1);
        }
    }

    private boolean mergeItemStackIntoCustomInputs(ItemStack stack) {
        boolean changed = false;
        for (int slotIndex : this.customInputSlotIndices) {
            if (stack.isEmpty()) {
                break;
            }
            if (this.mergeItemStack(stack, slotIndex, slotIndex + 1, false)) {
                changed = true;
            }
        }
        return changed;
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
