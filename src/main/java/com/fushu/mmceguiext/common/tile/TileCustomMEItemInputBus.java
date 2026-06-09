package com.fushu.mmceguiext.common.tile;

import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.data.IAEItemStack;
import appeng.me.GridAccessException;
import appeng.util.Platform;
import com.fushu.mmceguiext.common.registry.CustomAEItemInputBusRegistry;
import com.fushu.mmceguiext.common.util.CustomIdValidator;
import github.kasuminova.mmce.common.tile.MEItemInputBus;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.util.IOInventory;
import hellfirepvp.modularmachinery.common.util.ItemUtils;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.stream.IntStream;

public class TileCustomMEItemInputBus extends MEItemInputBus {
    private String definitionId = "";
    private int activeSlots = 16;
    private final IItemHandlerModifiable activeExternalHandler = new ActiveItemHandler(true, false);
    private final IItemHandlerModifiable activeRecipeHandler = new ActiveItemHandler(true, true);

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
        this.activeSlots = required;
        resizeInternalInventory(Math.max(1, required));
        resizeConfigInventory(Math.max(1, required));
        ensureCustomSlotTrackingCapacity(Math.max(1, required));
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

    public int getActiveSlots() {
        return getActiveSlotBound();
    }

    private int getActiveSlotBound() {
        return Math.min(this.activeSlots, Math.min(getInternalInventory().getSlots(), getConfigInventory().getSlots()));
    }

    private boolean isSlotDefined(int slot) {
        CustomAEItemInputBusRegistry.Def def = getDefinition();
        return def == null || slot >= 0
            && slot < def.configSlots.size() && def.configSlots.get(slot) != null
            && slot < def.storageSlots.size() && def.storageSlots.get(slot) != null;
    }

    @Nullable
    @Override
    public MachineComponent.ItemBus provideComponent() {
        return new MachineComponent.ItemBus(IOType.INPUT) {
            @Override
            public IItemHandlerModifiable getContainerProvider() {
                return activeRecipeHandler;
            }

            @Override
            public long getGroupID() {
                return getGroupId();
            }
        };
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        Capability<IItemHandler> cap = CapabilityItemHandler.ITEM_HANDLER_CAPABILITY;
        if (capability == cap) {
            return cap.cast(this.activeExternalHandler);
        }
        return super.getCapability(capability, facing);
    }

    @Nonnull
    @Override
    public TickingRequest getTickingRequest(@Nonnull final IGridNode node) {
        return new TickingRequest(10, 120, !needsActiveUpdate(), true);
    }

    private boolean needsActiveUpdate() {
        int slotCount = getActiveSlotBound();
        ensureCustomSlotTrackingCapacity(slotCount);
        for (int slot = 0; slot < slotCount; slot++) {
            ItemStack cfgStack = getConfigInventory().getStackInSlot(slot);
            ItemStack invStack = getInternalInventory().getStackInSlot(slot);
            if (!isSlotDefined(slot)) {
                if (!invStack.isEmpty()) {
                    return true;
                }
                continue;
            }
            if (cfgStack.isEmpty()) {
                if (!invStack.isEmpty()) {
                    return true;
                }
                continue;
            }
            if (invStack.isEmpty()) {
                return true;
            }
            if (!ItemUtils.matchStacks(cfgStack, invStack) || invStack.getCount() != cfgStack.getCount()) {
                return true;
            }
        }
        return false;
    }

    @Nonnull
    @Override
    public TickRateModulation tickingRequest(@Nonnull final IGridNode node, final int ticksSinceLastCall) {
        if (!proxy.isActive()) {
            return TickRateModulation.IDLE;
        }

        int[] needUpdateSlots = getNeedUpdateSlots();
        if (needUpdateSlots.length == 0) {
            return TickRateModulation.SLOWER;
        }

        java.util.concurrent.locks.ReadWriteLock rwLock = getInternalInventory().getRWLock();
        try {
            rwLock.writeLock().lock();
            boolean successAtLeastOnce = false;
            inTick = true;
            IMEMonitor<IAEItemStack> inv = proxy.getStorage().getInventory(channel);
            int slotBound = getActiveSlotBound();
            ensureCustomSlotTrackingCapacity(slotBound);
            for (final int slot : needUpdateSlots) {
                if (slot < 0 || slot >= slotBound || !isSlotDefined(slot)) {
                    continue;
                }
                changedSlots[slot] = false;
                ItemStack cfgStack = getConfigInventory().getStackInSlot(slot);
                ItemStack invStack = getInternalInventory().getStackInSlot(slot);

                if (cfgStack.isEmpty()) {
                    if (!invStack.isEmpty()) {
                        getInternalInventory().setStackInSlot(slot, insertStackToAE(inv, invStack));
                    }
                    continue;
                }

                if (!ItemUtils.matchStacks(cfgStack, invStack)) {
                    ItemStack left = invStack.isEmpty() ? ItemStack.EMPTY : insertStackToAE(inv, invStack);
                    if (left.isEmpty()) {
                        ItemStack stack = extractStackFromAE(inv, cfgStack);
                        getInternalInventory().setStackInSlot(slot, stack);
                        if (!stack.isEmpty()) {
                            successAtLeastOnce = true;
                        }
                    } else {
                        getInternalInventory().setStackInSlot(slot, left);
                        successAtLeastOnce |= left.getCount() != invStack.getCount();
                    }
                    continue;
                }

                if (cfgStack.getCount() == invStack.getCount()) {
                    continue;
                }

                if (cfgStack.getCount() > invStack.getCount()) {
                    int countToReceive = cfgStack.getCount() - invStack.getCount();
                    ItemStack stack = extractStackFromAE(inv, ItemUtils.copyStackWithSize(invStack, countToReceive));
                    if (!stack.isEmpty()) {
                        int newCount = invStack.getCount() + stack.getCount();
                        getInternalInventory().setStackInSlot(slot, ItemUtils.copyStackWithSize(invStack, newCount));
                        successAtLeastOnce = true;
                        failureCounter[slot] = 0;
                    } else {
                        failureCounter[slot]++;
                    }
                } else {
                    int countToExtract = invStack.getCount() - cfgStack.getCount();
                    ItemStack stack = insertStackToAE(inv, ItemUtils.copyStackWithSize(invStack, countToExtract));
                    if (stack.isEmpty()) {
                        getInternalInventory().setStackInSlot(slot, ItemUtils.copyStackWithSize(invStack, invStack.getCount() - countToExtract));
                    } else {
                        getInternalInventory().setStackInSlot(slot, ItemUtils.copyStackWithSize(invStack, invStack.getCount() - countToExtract + stack.getCount()));
                    }
                    successAtLeastOnce = true;
                }
            }

            return successAtLeastOnce ? TickRateModulation.FASTER : TickRateModulation.SLOWER;
        } catch (GridAccessException e) {
            changedSlots = new boolean[changedSlots.length];
            return TickRateModulation.IDLE;
        } finally {
            inTick = false;
            rwLock.writeLock().unlock();
        }
    }

    @Override
    protected synchronized int[] getNeedUpdateSlots() {
        int slotBound = getActiveSlotBound();
        ensureCustomSlotTrackingCapacity(slotBound);
        long current = world.getTotalWorldTime();
        if (lastFullCheckTick + 100 < current) {
            lastFullCheckTick = current;
            return IntStream.range(0, slotBound).toArray();
        }
        it.unimi.dsi.fastutil.ints.IntList needUpdateSlots = new it.unimi.dsi.fastutil.ints.IntArrayList(slotBound + 1);
        int bound = Math.min(slotBound, Math.min(changedSlots.length, failureCounter.length));
        for (int i = 0; i < bound; i++) {
            if (changedSlots[i] && failureCounter[i] <= 0) {
                needUpdateSlots.add(i);
            }
        }
        return needUpdateSlots.toIntArray();
    }

    private ItemStack extractStackFromAE(final IMEMonitor<IAEItemStack> inv, final ItemStack stack) throws GridAccessException {
        IAEItemStack aeStack = channel.createStack(stack);
        if (aeStack == null) {
            return ItemStack.EMPTY;
        }
        IAEItemStack extracted = Platform.poweredExtraction(proxy.getEnergy(), inv, aeStack, source);
        return extracted == null ? ItemStack.EMPTY : extracted.createItemStack();
    }

    private ItemStack insertStackToAE(final IMEMonitor<IAEItemStack> inv, final ItemStack stack) throws GridAccessException {
        IAEItemStack aeStack = channel.createStack(stack);
        if (aeStack == null) {
            return stack;
        }
        IAEItemStack left = Platform.poweredInsert(proxy.getEnergy(), inv, aeStack, source);
        return left == null ? ItemStack.EMPTY : left.createItemStack();
    }

    @Override
    public boolean hasItem() {
        for (int i = 0; i < getActiveSlotBound(); i++) {
            if (isSlotDefined(i) && !getInternalInventory().getStackInSlot(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean configInvHasItem() {
        for (int i = 0; i < getActiveSlotBound(); i++) {
            if (isSlotDefined(i) && !getConfigInventory().getStackInSlot(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void markNoUpdate() {
        if (hasActiveChangedSlots()) {
            try {
                proxy.getTick().alertDevice(proxy.getNode());
            } catch (GridAccessException e) {
                // NO-OP
            }
        }
        super.markNoUpdate();
    }

    private boolean hasActiveChangedSlots() {
        int bound = Math.min(getActiveSlotBound(), changedSlots.length);
        for (int i = 0; i < bound; i++) {
            if (changedSlots[i]) {
                return true;
            }
        }
        return false;
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
        return Math.max(def.configSlots.size(), def.storageSlots.size());
    }

    private class ActiveItemHandler implements IItemHandlerModifiable {
        private final boolean allowInsert;
        private final boolean allowExtract;

        private ActiveItemHandler(boolean allowInsert, boolean allowExtract) {
            this.allowInsert = allowInsert;
            this.allowExtract = allowExtract;
        }

        @Override
        public void setStackInSlot(int slot, @Nonnull ItemStack stack) {
            if (!this.allowInsert || slot < 0 || slot >= getActiveSlotBound() || !isSlotDefined(slot)) {
                return;
            }
            getInternalInventory().setStackInSlot(slot, stack);
        }

        @Override
        public int getSlots() {
            return getActiveSlotBound();
        }

        @Nonnull
        @Override
        public ItemStack getStackInSlot(int slot) {
            if (slot < 0 || slot >= getActiveSlotBound() || !isSlotDefined(slot)) {
                return ItemStack.EMPTY;
            }
            return getInternalInventory().getStackInSlot(slot);
        }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            if (!this.allowInsert || slot < 0 || slot >= getActiveSlotBound() || !isSlotDefined(slot)) {
                return stack;
            }
            return getInternalInventory().insertItem(slot, stack, simulate);
        }

        @Nonnull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (!this.allowExtract || slot < 0 || slot >= getActiveSlotBound() || !isSlotDefined(slot)) {
                return ItemStack.EMPTY;
            }
            return getInternalInventory().extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            if (slot < 0 || slot >= getActiveSlotBound() || !isSlotDefined(slot)) {
                return 0;
            }
            return getInternalInventory().getSlotLimit(slot);
        }
    }
}
