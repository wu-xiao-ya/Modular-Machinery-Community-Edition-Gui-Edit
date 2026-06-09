package com.fushu.mmceguiext.common.tile;

import com.fushu.mmceguiext.common.registry.CustomAEItemInputBusRegistry;
import com.fushu.mmceguiext.common.util.CustomIdValidator;
import github.kasuminova.mmce.common.tile.MEItemInputBus;
import hellfirepvp.modularmachinery.common.util.IOInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

import javax.annotation.Nullable;
import java.util.List;

public class TileCustomMEItemInputBus extends MEItemInputBus {
    private String definitionId = "";

    public void setDefinitionId(@Nullable String id) {
        String sanitized = CustomIdValidator.sanitizeResourceLocation(id);
        this.definitionId = sanitized == null ? "" : sanitized;
        configureSlotLayout();
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
    public IOInventory buildInventory() {
        int size = resolveInitialSlotCount();
        int[] slotIDs = buildSlotIDs(size);
        IOInventory inv = new IOInventory(this, slotIDs, new int[]{});
        inv.setStackLimit(Integer.MAX_VALUE, slotIDs);
        inv.setListener(slot -> {
            synchronized (this) {
                ensureCustomSlotTrackingCapacity(slot + 1);
                changedSlots[slot] = true;
            }
        });
        return inv;
    }

    @Override
    public IOInventory buildConfigInventory() {
        int size = resolveInitialSlotCount();
        int[] slotIDs = buildSlotIDs(size);
        IOInventory inv = new IOInventory(this, new int[]{}, new int[]{});
        inv.setStackLimit(Integer.MAX_VALUE, slotIDs);
        inv.setMiscSlots(slotIDs);
        inv.setListener(slot -> {
            synchronized (this) {
                ensureCustomSlotTrackingCapacity(slot + 1);
                changedSlots[slot] = true;
            }
        });
        return inv;
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
        String id = CustomIdValidator.readSanitizedString(compound, "definitionId");
        this.definitionId = id == null ? "" : id;
        configureSlotLayout();
    }

    @Override
    public void writeCustomNBT(final NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        compound.setString("definitionId", this.definitionId == null ? "" : this.definitionId);
    }

    private void configureSlotLayout() {
        int required = resolveSlotCount(getDefinition());
        resizeInternalInventory(required);
        resizeConfigInventory(required);
        ensureCustomSlotTrackingCapacity(required);
    }

    private void resizeInternalInventory(int size) {
        IOInventory old = getInternalInventory();
        if (old != null && old.getSlots() >= size) {
            return;
        }
        IOInventory next = createInventory(size, true);
        copyInventory(old, next);
        this.inventory = next;
    }

    private void resizeConfigInventory(int size) {
        IOInventory old = getConfigInventory();
        if (old != null && old.getSlots() >= size) {
            return;
        }
        IOInventory next = createInventory(size, false);
        copyInventory(old, next);
        readConfigInventoryNBT(next.writeNBT());
    }

    private IOInventory createInventory(int size, boolean input) {
        int[] slotIDs = buildSlotIDs(size);
        IOInventory inv = input ? new IOInventory(this, slotIDs, new int[]{}) : new IOInventory(this, new int[]{}, new int[]{});
        inv.setStackLimit(Integer.MAX_VALUE, slotIDs);
        if (!input) {
            inv.setMiscSlots(slotIDs);
        }
        inv.setListener(slot -> {
            synchronized (this) {
                ensureCustomSlotTrackingCapacity(slot + 1);
                changedSlots[slot] = true;
            }
        });
        return inv;
    }

    private static void copyInventory(@Nullable IOInventory oldInv, IOInventory next) {
        if (oldInv == null || next == null) {
            return;
        }
        int copySlots = Math.min(oldInv.getSlots(), next.getSlots());
        for (int i = 0; i < copySlots; i++) {
            next.setStackInSlot(i, oldInv.getStackInSlot(i));
        }
    }

    private static int[] buildSlotIDs(int size) {
        int[] slotIDs = new int[Math.max(1, size)];
        for (int slotID = 0; slotID < slotIDs.length; slotID++) {
            slotIDs[slotID] = slotID;
        }
        return slotIDs;
    }

    private void ensureCustomSlotTrackingCapacity(final int requiredSlots) {
        if (requiredSlots <= 0) {
            return;
        }
        if (changedSlots.length < requiredSlots) {
            boolean[] resized = new boolean[requiredSlots];
            System.arraycopy(changedSlots, 0, resized, 0, changedSlots.length);
            changedSlots = resized;
        }
        if (failureCounter.length < requiredSlots) {
            int[] resized = new int[requiredSlots];
            System.arraycopy(failureCounter, 0, resized, 0, failureCounter.length);
            failureCounter = resized;
        }
    }

    private static int resolveInitialSlotCount() {
        int max = 16;
        List<CustomAEItemInputBusRegistry.Def> defs = CustomAEItemInputBusRegistry.getCached();
        if (defs.isEmpty()) {
            defs = CustomAEItemInputBusRegistry.loadAll();
        }
        for (CustomAEItemInputBusRegistry.Def def : defs) {
            max = Math.max(max, resolveSlotCount(def));
        }
        return max;
    }

    private static int resolveSlotCount(@Nullable CustomAEItemInputBusRegistry.Def def) {
        if (def == null) {
            return 16;
        }
        return Math.max(16, Math.max(def.configSlots.size(), def.storageSlots.size()));
    }
}
