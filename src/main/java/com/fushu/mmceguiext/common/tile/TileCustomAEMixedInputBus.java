package com.fushu.mmceguiext.common.tile;

import appeng.api.AEApi;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.storage.IMEMonitor;
import appeng.api.storage.channels.IItemStorageChannel;
import appeng.api.storage.data.IAEFluidStack;
import appeng.api.storage.data.IAEItemStack;
import appeng.api.util.AECableType;
import appeng.api.util.AEPartLocation;
import appeng.api.util.DimensionalCoord;
import appeng.fluids.util.AEFluidInventory;
import appeng.fluids.util.IAEFluidInventory;
import appeng.fluids.util.IAEFluidTank;
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.IGridProxyable;
import appeng.me.helpers.MachineSource;
import appeng.util.Platform;
import com.fushu.mmceguiext.common.registry.CustomAEMixedInputBusRegistry;
import com.mekeng.github.common.me.data.IAEGasStack;
import com.mekeng.github.common.me.data.impl.AEGasStack;
import com.mekeng.github.common.me.inventory.IGasInventoryHost;
import com.mekeng.github.common.me.inventory.impl.GasInventory;
import com.mekeng.github.common.me.storage.IGasStorageChannel;
import github.kasuminova.mmce.common.tile.SettingsTransfer;
import github.kasuminova.mmce.common.util.AEFluidInventoryUpgradeable;
import github.kasuminova.mmce.common.util.InfItemFluidHandler;
import github.kasuminova.mmce.common.util.GasInventoryHandler;
import github.kasuminova.mmce.common.util.IExtendedGasHandler;
import github.kasuminova.mmce.common.util.Sides;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.lib.ComponentTypesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.tiles.base.MachineComponentTile;
import hellfirepvp.modularmachinery.common.tiles.base.SelectiveUpdateTileEntity;
import hellfirepvp.modularmachinery.common.tiles.base.TileColorableMachineComponent;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import hellfirepvp.modularmachinery.common.util.IOInventory;
import hellfirepvp.modularmachinery.common.util.ItemUtils;
import mekanism.api.gas.GasStack;
import mekanism.common.capabilities.Capabilities;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReadWriteLock;

public class TileCustomAEMixedInputBus extends TileColorableMachineComponent implements
    SelectiveUpdateTileEntity,
    MachineComponentTile,
    IActionHost,
    IGridProxyable,
    IGridTickable,
    SettingsTransfer,
    IAEFluidInventory,
    IGasInventoryHost {

    private static final String ITEM_CONFIG_TAG = "configInventory";
    private static final String FLUID_CONFIG_TAG = "fluidConfig";
    private static final String GAS_CONFIG_TAG = "gasConfig";
    private static final int ITEM_SLOT_COUNT = 15;
    private static final int TANK_SLOT_COUNT = 1;
    private static final int FLUID_TANK_CAPACITY = 8000;
    private static final int GAS_TANK_CAPACITY = 8000;

    private static final Map<ItemStack, IAEItemStack> AE_STACK_CACHE = new WeakHashMap<ItemStack, IAEItemStack>();

    protected final AENetworkProxy proxy;
    protected final IActionSource source;

    protected final IItemStorageChannel itemChannel = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class);
    protected final appeng.api.storage.channels.IFluidStorageChannel fluidChannel = AEApi.instance().storage().getStorageChannel(appeng.api.storage.channels.IFluidStorageChannel.class);
    protected final IGasStorageChannel gasChannel = AEApi.instance().storage().getStorageChannel(IGasStorageChannel.class);

    private IOInventory inventory = buildInventory();
    private IOInventory configInventory = buildConfigInventory();
    private final AEFluidInventoryUpgradeable fluidTanks = new AEFluidInventoryUpgradeable(this, TANK_SLOT_COUNT, FLUID_TANK_CAPACITY);
    private final AEFluidInventory fluidConfig = new AEFluidInventory(this, TANK_SLOT_COUNT);
    private final GasInventory gasTanks = new GasInventory(TANK_SLOT_COUNT, GAS_TANK_CAPACITY, this);
    private final GasInventory gasConfig = new GasInventory(TANK_SLOT_COUNT, this);
    private final GasInventoryHandler gasHandler = new GasInventoryHandler(gasTanks);

    private boolean[] changedItemSlots = new boolean[ITEM_SLOT_COUNT];
    private int[] itemFailureCounter = new int[ITEM_SLOT_COUNT];
    private boolean[] changedFluidSlots = new boolean[TANK_SLOT_COUNT];
    private boolean[] changedGasSlots = new boolean[TANK_SLOT_COUNT];
    private long lastFullItemCheckTick = 0;
    private long lastFullFluidCheckTick = 0;
    private long lastFullGasCheckTick = 0;
    private boolean inTick = false;
    private boolean componentExposed = false;

    private String definitionId = "";
    private final CombinedBusHandler combinedHandler = new CombinedBusHandler();
    private final MachineComponent<InfItemFluidHandler> combinedComponent;

    public TileCustomAEMixedInputBus() {
        this.source = new MachineSource(this);
        this.proxy = new AENetworkProxy(this, "aeProxy", getVisualItemStack(), true);
        this.proxy.setIdlePowerUsage(1.0D);
        this.proxy.setFlags(GridFlags.REQUIRE_CHANNEL);
        bindInventoryListeners();
        this.componentExposed = hasBufferedRecipeInput();
        final long combinedGroupId = github.kasuminova.mmce.common.tile.base.MachineCombinationComponent.GROUP_ACQUIRER.incrementAndGet();
        this.combinedComponent = new MachineComponent<InfItemFluidHandler>(IOType.INPUT) {
            @Override
            public ComponentType getComponentType() {
                return ComponentTypesMM.COMPONENT_ITEM_FLUID_GAS;
            }

            @Override
            public InfItemFluidHandler getContainerProvider() {
                return combinedHandler;
            }

            @Override
            public long getGroupID() {
                return combinedGroupId;
            }
        };
    }

    private void bindInventoryListeners() {
        this.inventory.setListener(slot -> {
            synchronized (this) {
                if (slot >= 0 && slot < this.changedItemSlots.length) {
                    this.changedItemSlots[slot] = true;
                }
            }
            onBufferedInputStateChanged();
        });
        this.configInventory.setListener(slot -> {
            synchronized (this) {
                if (slot >= 0 && slot < this.changedItemSlots.length) {
                    this.changedItemSlots[slot] = true;
                }
            }
        });
    }

    private IOInventory buildInventory() {
        int[] slotIDs = new int[ITEM_SLOT_COUNT];
        for (int slotID = 0; slotID < ITEM_SLOT_COUNT; slotID++) {
            slotIDs[slotID] = slotID;
        }
        IOInventory inv = new IOInventory(this, slotIDs, new int[]{});
        inv.setStackLimit(Integer.MAX_VALUE, slotIDs);
        return inv;
    }

    private IOInventory buildConfigInventory() {
        int[] slotIDs = new int[ITEM_SLOT_COUNT];
        for (int slotID = 0; slotID < ITEM_SLOT_COUNT; slotID++) {
            slotIDs[slotID] = slotID;
        }
        IOInventory inv = new IOInventory(this, new int[]{}, new int[]{});
        inv.setStackLimit(Integer.MAX_VALUE, slotIDs);
        inv.setMiscSlots(slotIDs);
        return inv;
    }

    @Override
    public void validate() {
        super.validate();
        Sides.SERVER.runIfPresent(() -> ModularMachinery.EXECUTE_MANAGER.addSyncTask(this.proxy::onReady));
    }

    @Override
    public void invalidate() {
        super.invalidate();
        this.proxy.invalidate();
    }

    @Override
    public void onChunkUnload() {
        super.onChunkUnload();
        this.proxy.onChunkUnload();
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
            || capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY
            || capability == Capabilities.GAS_HANDLER_CAPABILITY
            || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return CapabilityItemHandler.ITEM_HANDLER_CAPABILITY.cast(this.inventory);
        }
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY.cast(this.fluidTanks);
        }
        if (capability == Capabilities.GAS_HANDLER_CAPABILITY) {
            return Capabilities.GAS_HANDLER_CAPABILITY.cast(this.gasHandler);
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void readCustomNBT(final NBTTagCompound compound) {
        super.readCustomNBT(compound);
        if (world != null && !world.isRemote) {
            try {
                this.proxy.readFromNBT(compound);
            } catch (IllegalStateException e) {
                ModularMachinery.log.warn(e);
            }
        }

        if (compound.hasKey("inventory")) {
            this.inventory = IOInventory.deserialize(this, compound.getCompoundTag("inventory"));
        } else {
            this.inventory = buildInventory();
        }
        if (compound.hasKey(ITEM_CONFIG_TAG)) {
            this.configInventory = IOInventory.deserialize(this, compound.getCompoundTag(ITEM_CONFIG_TAG));
        } else {
            this.configInventory = buildConfigInventory();
        }
        bindInventoryListeners();

        this.fluidTanks.readFromNBT(compound, "tanks");
        this.fluidConfig.readFromNBT(compound, FLUID_CONFIG_TAG);
        this.gasTanks.load(compound.getCompoundTag("gasTanks"));
        this.gasConfig.load(compound.getCompoundTag(GAS_CONFIG_TAG));

        this.definitionId = compound.hasKey("definitionId") ? compound.getString("definitionId") : "";
        resetDirtyState();
        this.componentExposed = hasBufferedRecipeInput();
    }

    @Override
    public void writeCustomNBT(final NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        this.proxy.writeToNBT(compound);
        compound.setTag("inventory", this.inventory.writeNBT());
        compound.setTag(ITEM_CONFIG_TAG, this.configInventory.writeNBT());
        this.fluidTanks.writeToNBT(compound, "tanks");
        this.fluidConfig.writeToNBT(compound, FLUID_CONFIG_TAG);
        compound.setTag("gasTanks", this.gasTanks.save());
        compound.setTag(GAS_CONFIG_TAG, this.gasConfig.save());
        compound.setString("definitionId", this.definitionId == null ? "" : this.definitionId);
    }

    private void resetDirtyState() {
        this.changedItemSlots = new boolean[this.inventory.getSlots()];
        this.itemFailureCounter = new int[this.inventory.getSlots()];
        this.changedFluidSlots = new boolean[this.fluidTanks.getSlots()];
        this.changedGasSlots = new boolean[this.gasTanks.size()];
    }

    public IOInventory getInternalInventory() {
        return this.inventory;
    }

    public IOInventory getConfigInventory() {
        return this.configInventory;
    }

    public IAEFluidTank getFluidTanks() {
        return this.fluidTanks;
    }

    public IAEFluidTank getFluidConfig() {
        return this.fluidConfig;
    }

    public GasInventory getGasTanks() {
        return this.gasTanks;
    }

    public GasInventory getGasConfig() {
        return this.gasConfig;
    }

    public void setDefinitionId(@Nullable String id) {
        this.definitionId = id == null ? "" : id.trim();
        markDirty();
    }

    @Nullable
    public String getDefinitionId() {
        return this.definitionId == null || this.definitionId.trim().isEmpty() ? null : this.definitionId.trim();
    }

    @Nullable
    public CustomAEMixedInputBusRegistry.Def getDefinition() {
        return CustomAEMixedInputBusRegistry.findById(this.definitionId);
    }

    public boolean configInvHasItem() {
        for (int i = 0; i < this.configInventory.getSlots(); i++) {
            if (!this.configInventory.getStackInSlot(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public ItemStack getVisualItemStack() {
        if (this.getBlockType() != null) {
            Item item = Item.getItemFromBlock(this.getBlockType());
            if (item != null) {
                return new ItemStack(item);
            }
        }
        return ItemStack.EMPTY;
    }

    @Nullable
    @Override
    public MachineComponent<?> provideComponent() {
        return hasBufferedRecipeInput() ? this.combinedComponent : null;
    }

    @Nonnull
    public Collection<MachineComponent<?>> provideComponents() {
        return Collections.emptyList();
    }

    @Override
    public boolean canGroupInput() {
        return true;
    }

    public boolean hasItems() {
        for (int i = 0; i < this.inventory.getSlots(); i++) {
            if (!this.inventory.getStackInSlot(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasFluid() {
        return this.fluidTanks.getFluidInSlot(0) != null;
    }

    public boolean hasGas() {
        return this.gasTanks.getGasStack(0) != null;
    }

    private boolean hasBufferedRecipeInput() {
        return hasItems() || hasFluid() || hasGas();
    }

    private void onBufferedInputStateChanged() {
        boolean exposed = hasBufferedRecipeInput();
        if (this.componentExposed == exposed) {
            return;
        }
        this.componentExposed = exposed;
        markForUpdateSync();
        if (world != null && !world.isRemote) {
            ModularMachinery.EXECUTE_MANAGER.addSyncTask(this::refreshOwningControllers);
        }
    }

    @SuppressWarnings("unchecked")
    private void refreshOwningControllers() {
        if (world == null || world.isRemote) {
            return;
        }
        try {
            Class<?> managerClass = Class.forName("github.kasuminova.mmce.common.world.MachineComponentManager");
            Object manager = managerClass.getField("INSTANCE").get(null);
            java.lang.reflect.Field componentMapField = managerClass.getDeclaredField("componentMap");
            componentMapField.setAccessible(true);
            Map<Object, Object> componentMap = (Map<Object, Object>) componentMapField.get(manager);
            Object worldEntries = componentMap.get(world);
            if (!(worldEntries instanceof Map)) {
                return;
            }

            Object componentInfo = ((Map<?, ?>) worldEntries).get(getPos());
            if (componentInfo == null) {
                return;
            }

            Set<TileMultiblockMachineController> owners =
                (Set<TileMultiblockMachineController>) componentInfo.getClass().getMethod("owners").invoke(componentInfo);
            if (owners == null || owners.isEmpty()) {
                return;
            }

            java.lang.reflect.Field lastStructureCheckTick = TileMultiblockMachineController.class.getDeclaredField("lastStructureCheckTick");
            lastStructureCheckTick.setAccessible(true);
            java.lang.reflect.Method updateComponents = TileMultiblockMachineController.class.getDeclaredMethod("updateComponents");
            updateComponents.setAccessible(true);
            for (TileMultiblockMachineController owner : owners) {
                if (owner == null || owner.isInvalid()) {
                    continue;
                }
                lastStructureCheckTick.setInt(owner, -1);
                updateComponents.invoke(owner);
                owner.markForUpdateSync();
                owner.setSearchRecipeImmediately(true);
            }
        } catch (ReflectiveOperationException e) {
            ModularMachinery.log.warn("Failed to refresh owning controllers for custom AE mixed input bus", e);
        }
    }

    private class CombinedBusHandler extends InfItemFluidHandler {
        private CombinedBusHandler() {
            super(inventory, fluidTanks);
        }

        @Override
        public synchronized void setStackInSlot(int slot, @Nonnull ItemStack stack) {
            inventory.setStackInSlot(slot, stack);
        }

        @Override
        public int getSlots() {
            return inventory.getSlots();
        }

        @Nonnull
        @Override
        public ItemStack getStackInSlot(int slot) {
            return inventory.getStackInSlot(slot);
        }

        @Nonnull
        @Override
        public synchronized ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            return inventory.insertItem(slot, stack, simulate);
        }

        @Nonnull
        @Override
        public synchronized ItemStack extractItem(int slot, int amount, boolean simulate) {
            return inventory.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return inventory.getSlotLimit(slot);
        }

        @Override
        public synchronized net.minecraftforge.fluids.capability.IFluidTankProperties[] getTankProperties() {
            return fluidTanks.getTankProperties();
        }

        @Override
        public synchronized int fill(net.minecraftforge.fluids.FluidStack resource, boolean doFill) {
            return fluidTanks.fill(resource, doFill);
        }

        @Nullable
        @Override
        public synchronized net.minecraftforge.fluids.FluidStack drain(net.minecraftforge.fluids.FluidStack resource, boolean doDrain) {
            return fluidTanks.drain(resource, doDrain);
        }

        @Nullable
        @Override
        public synchronized net.minecraftforge.fluids.FluidStack drain(int maxDrain, boolean doDrain) {
            return fluidTanks.drain(maxDrain, doDrain);
        }

        @Override
        public synchronized int receiveGas(@Nullable EnumFacing side, GasStack toReceive, boolean doTransfer) {
            return gasHandler.receiveGas(side, toReceive, doTransfer);
        }

        @Override
        public synchronized GasStack drawGas(@Nullable EnumFacing side, int drawAmount, boolean doTransfer) {
            return gasHandler.drawGas(side, drawAmount, doTransfer);
        }

        @Override
        public synchronized GasStack drawGas(GasStack toDraw, boolean doTransfer) {
            return gasHandler.drawGas(toDraw, doTransfer);
        }

        @Override
        public boolean canReceiveGas(@Nullable EnumFacing side, mekanism.api.gas.Gas gas) {
            return gasHandler.canReceiveGas(side, gas);
        }

        @Override
        public boolean canDrawGas(@Nullable EnumFacing side, mekanism.api.gas.Gas gas) {
            return gasHandler.canDrawGas(side, gas);
        }

        @Nonnull
        @Override
        public mekanism.api.gas.GasTankInfo[] getTankInfo() {
            return gasHandler.getTankInfo();
        }

        @Override
        public boolean isEmpty() {
            return !hasBufferedRecipeInput();
        }
    }

    @Nonnull
    @Override
    public TickingRequest getTickingRequest(@Nonnull final IGridNode node) {
        return new TickingRequest(10, 120, !needsAnyUpdate(), true);
    }

    private boolean needsAnyUpdate() {
        return needsItemUpdate() || needsFluidUpdate() || needsGasUpdate();
    }

    private boolean needsItemUpdate() {
        int slotCount = Math.min(this.inventory.getSlots(), this.configInventory.getSlots());
        for (int slot = 0; slot < slotCount; slot++) {
            ItemStack cfgStack = this.configInventory.getStackInSlot(slot);
            ItemStack invStack = this.inventory.getStackInSlot(slot);

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

    private boolean needsFluidUpdate() {
        int capacity = this.fluidTanks.getCapacity();
        for (int slot = 0; slot < this.fluidConfig.getSlots(); slot++) {
            IAEFluidStack cfgStack = this.fluidConfig.getFluidInSlot(slot);
            IAEFluidStack invStack = this.fluidTanks.getFluidInSlot(slot);
            if (cfgStack == null) {
                if (invStack != null) {
                    return true;
                }
                continue;
            }
            if (invStack == null) {
                return true;
            }
            if (!cfgStack.equals(invStack) || invStack.getStackSize() != capacity) {
                return true;
            }
        }
        return false;
    }

    private boolean needsGasUpdate() {
        int capacity = this.gasTanks.getTanks()[0].getMaxGas();
        for (int slot = 0; slot < this.gasConfig.size(); slot++) {
            GasStack cfgStack = this.gasConfig.getGasStack(slot);
            GasStack invStack = this.gasTanks.getGasStack(slot);
            if (cfgStack == null) {
                if (invStack != null) {
                    return true;
                }
                continue;
            }
            if (invStack == null) {
                return true;
            }
            if (!cfgStack.isGasEqual(invStack) || invStack.amount != capacity) {
                return true;
            }
        }
        return false;
    }

    @Nonnull
    @Override
    public TickRateModulation tickingRequest(@Nonnull final IGridNode node, final int ticksSinceLastCall) {
        if (!this.proxy.isActive()) {
            return TickRateModulation.IDLE;
        }

        boolean success = false;
        this.inTick = true;
        try {
            success |= tickItems();
            success |= tickFluids();
            success |= tickGases();
            return success ? TickRateModulation.FASTER : TickRateModulation.SLOWER;
        } catch (GridAccessException e) {
            return TickRateModulation.IDLE;
        } finally {
            this.inTick = false;
        }
    }

    private boolean tickItems() throws GridAccessException {
        int[] slots = getNeedUpdateItemSlots();
        if (slots.length == 0) {
            return false;
        }

        boolean success = false;
        ReadWriteLock rwLock = this.inventory.getRWLock();
        rwLock.writeLock().lock();
        try {
            IMEMonitor<IAEItemStack> inv = this.proxy.getStorage().getInventory(this.itemChannel);
            int slotBound = Math.min(this.inventory.getSlots(), this.configInventory.getSlots());
            for (int slot : slots) {
                if (slot < 0 || slot >= slotBound) {
                    continue;
                }
                this.changedItemSlots[slot] = false;
                ItemStack cfgStack = this.configInventory.getStackInSlot(slot);
                ItemStack invStack = this.inventory.getStackInSlot(slot);

                if (cfgStack.isEmpty()) {
                    if (!invStack.isEmpty()) {
                        this.inventory.setStackInSlot(slot, insertItemToAE(inv, invStack));
                    }
                    continue;
                }

                if (!ItemUtils.matchStacks(cfgStack, invStack)) {
                    if (invStack.isEmpty() || insertItemToAE(inv, invStack).isEmpty()) {
                        ItemStack stack = extractItemFromAE(inv, cfgStack);
                        this.inventory.setStackInSlot(slot, stack);
                        if (!stack.isEmpty()) {
                            success = true;
                        }
                    }
                    continue;
                }

                if (cfgStack.getCount() == invStack.getCount()) {
                    continue;
                }

                if (cfgStack.getCount() > invStack.getCount()) {
                    int countToReceive = cfgStack.getCount() - invStack.getCount();
                    ItemStack stack = extractItemFromAE(inv, ItemUtils.copyStackWithSize(invStack, countToReceive));
                    if (!stack.isEmpty()) {
                        this.inventory.setStackInSlot(slot, ItemUtils.copyStackWithSize(invStack, invStack.getCount() + stack.getCount()));
                        success = true;
                        this.itemFailureCounter[slot] = 0;
                    } else {
                        this.itemFailureCounter[slot]++;
                    }
                } else {
                    int countToExtract = invStack.getCount() - cfgStack.getCount();
                    ItemStack left = insertItemToAE(inv, ItemUtils.copyStackWithSize(invStack, countToExtract));
                    if (left.isEmpty()) {
                        this.inventory.setStackInSlot(slot, ItemUtils.copyStackWithSize(invStack, invStack.getCount() - countToExtract));
                    } else {
                        this.inventory.setStackInSlot(slot, ItemUtils.copyStackWithSize(invStack, invStack.getCount() - countToExtract + left.getCount()));
                    }
                    success = true;
                }
            }
            return success;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    private boolean tickFluids() throws GridAccessException {
        int[] slots = getNeedUpdateFluidSlots();
        if (slots.length == 0) {
            return false;
        }

        boolean success = false;
        ReadWriteLock rwLock = this.fluidTanks.getRWLock();
        rwLock.writeLock().lock();
        try {
            IMEMonitor<IAEFluidStack> inv = this.proxy.getStorage().getInventory(this.fluidChannel);
            int capacity = this.fluidTanks.getCapacity();
            for (int slot : slots) {
                this.changedFluidSlots[slot] = false;
                IAEFluidStack cfgStack = this.fluidConfig.getFluidInSlot(slot);
                IAEFluidStack invStack = this.fluidTanks.getFluidInSlot(slot);

                if (cfgStack == null) {
                    if (invStack != null) {
                        this.fluidTanks.setFluidInSlot(slot, insertFluidToAE(inv, invStack));
                    }
                    continue;
                }

                if (!cfgStack.equals(invStack)) {
                    if (invStack != null) {
                        IAEFluidStack left = insertFluidToAE(inv, invStack);
                        if (left != null) {
                            this.fluidTanks.setFluidInSlot(slot, left);
                            continue;
                        }
                    }
                    IAEFluidStack stack = extractFluidFromAE(inv, cfgStack.copy().setStackSize(capacity));
                    this.fluidTanks.setFluidInSlot(slot, stack);
                    if (stack != null) {
                        success = true;
                    }
                    continue;
                }

                if (invStack == null || invStack.getStackSize() == capacity) {
                    continue;
                }

                if (capacity > invStack.getStackSize()) {
                    IAEFluidStack stack = extractFluidFromAE(inv, invStack.copy().setStackSize(capacity - invStack.getStackSize()));
                    if (stack != null) {
                        this.fluidTanks.setFluidInSlot(slot, invStack.copy().setStackSize(invStack.getStackSize() + stack.getStackSize()));
                        success = true;
                    }
                } else {
                    int countToExtract = (int) (invStack.getStackSize() - capacity);
                    IAEFluidStack left = insertFluidToAE(inv, invStack.copy().setStackSize(countToExtract));
                    if (left == null) {
                        this.fluidTanks.setFluidInSlot(slot, invStack.copy().setStackSize(invStack.getStackSize() - countToExtract));
                    } else {
                        this.fluidTanks.setFluidInSlot(slot, invStack.copy().setStackSize(invStack.getStackSize() - countToExtract + left.getStackSize()));
                    }
                    success = true;
                }
            }
            return success;
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    private boolean tickGases() throws GridAccessException {
        int[] slots = getNeedUpdateGasSlots();
        if (slots.length == 0) {
            return false;
        }

        boolean success = false;
        synchronized (this.gasTanks) {
            IMEMonitor<IAEGasStack> inv = this.proxy.getStorage().getInventory(this.gasChannel);
            int capacity = this.gasTanks.getTanks()[0].getMaxGas();
            for (int slot : slots) {
                this.changedGasSlots[slot] = false;
                GasStack cfgStack = this.gasConfig.getGasStack(slot);
                GasStack invStack = this.gasTanks.getGasStack(slot);

                if (cfgStack == null) {
                    if (invStack != null) {
                        IAEGasStack left = insertGasToAE(inv, invStack);
                        this.gasTanks.setGas(slot, left == null ? null : left.getGasStack());
                    }
                    continue;
                }

                if (!cfgStack.isGasEqual(invStack)) {
                    if (invStack != null) {
                        IAEGasStack left = insertGasToAE(inv, invStack);
                        if (left != null) {
                            this.gasTanks.setGas(slot, left.getGasStack());
                            continue;
                        }
                    }
                    GasStack copied = cfgStack.copy();
                    copied.amount = capacity;
                    IAEGasStack stack = extractGasFromAE(inv, copied);
                    if (stack != null) {
                        this.gasTanks.setGas(slot, stack.getGasStack());
                        success = true;
                    }
                    continue;
                }

                if (invStack == null || invStack.amount == capacity) {
                    continue;
                }

                if (capacity > invStack.amount) {
                    GasStack copied = invStack.copy();
                    copied.amount = capacity - invStack.amount;
                    IAEGasStack stack = extractGasFromAE(inv, copied);
                    if (stack != null) {
                        copied = invStack.copy();
                        copied.amount = (int) (invStack.amount + stack.getStackSize());
                        this.gasTanks.setGas(slot, copied);
                        success = true;
                    }
                } else {
                    GasStack copied = invStack.copy();
                    copied.amount = invStack.amount - capacity;
                    IAEGasStack left = insertGasToAE(inv, copied);
                    if (left == null) {
                        copied = invStack.copy();
                        copied.amount = capacity;
                        this.gasTanks.setGas(slot, copied);
                    } else {
                        copied = invStack.copy();
                        copied.amount = (int) (capacity + left.getStackSize());
                        this.gasTanks.setGas(slot, copied);
                    }
                    success = true;
                }
            }
        }
        return success;
    }

    private int[] getNeedUpdateItemSlots() {
        long current = world.getTotalWorldTime();
        if (this.lastFullItemCheckTick + 100 < current) {
            this.lastFullItemCheckTick = current;
            int[] slots = new int[this.inventory.getSlots()];
            for (int i = 0; i < slots.length; i++) {
                slots[i] = i;
            }
            return slots;
        }
        java.util.List<Integer> slots = new java.util.ArrayList<Integer>();
        for (int i = 0; i < this.changedItemSlots.length; i++) {
            if (this.changedItemSlots[i] && this.itemFailureCounter[i] <= 0) {
                slots.add(i);
            }
        }
        return slots.stream().mapToInt(Integer::intValue).toArray();
    }

    private int[] getNeedUpdateFluidSlots() {
        long current = world.getTotalWorldTime();
        if (this.lastFullFluidCheckTick + 100 < current) {
            this.lastFullFluidCheckTick = current;
            return new int[]{0};
        }
        java.util.List<Integer> slots = new java.util.ArrayList<Integer>();
        for (int i = 0; i < this.changedFluidSlots.length; i++) {
            if (this.changedFluidSlots[i]) {
                slots.add(i);
            }
        }
        return slots.stream().mapToInt(Integer::intValue).toArray();
    }

    private int[] getNeedUpdateGasSlots() {
        long current = world.getTotalWorldTime();
        if (this.lastFullGasCheckTick + 100 < current) {
            this.lastFullGasCheckTick = current;
            return new int[]{0};
        }
        java.util.List<Integer> slots = new java.util.ArrayList<Integer>();
        for (int i = 0; i < this.changedGasSlots.length; i++) {
            if (this.changedGasSlots[i]) {
                slots.add(i);
            }
        }
        return slots.stream().mapToInt(Integer::intValue).toArray();
    }

    private ItemStack extractItemFromAE(final IMEMonitor<IAEItemStack> inv, final ItemStack stack) throws GridAccessException {
        IAEItemStack aeStack = createItemStack(stack);
        if (aeStack == null) {
            return ItemStack.EMPTY;
        }
        IAEItemStack extracted = Platform.poweredExtraction(this.proxy.getEnergy(), inv, aeStack, this.source);
        return extracted == null ? ItemStack.EMPTY : extracted.createItemStack();
    }

    private ItemStack insertItemToAE(final IMEMonitor<IAEItemStack> inv, final ItemStack stack) throws GridAccessException {
        IAEItemStack aeStack = createItemStack(stack);
        if (aeStack == null) {
            return stack;
        }
        IAEItemStack left = Platform.poweredInsert(this.proxy.getEnergy(), inv, aeStack, this.source);
        return left == null ? ItemStack.EMPTY : left.createItemStack();
    }

    private IAEItemStack createItemStack(final ItemStack stack) {
        return AE_STACK_CACHE.computeIfAbsent(stack, v -> this.itemChannel.createStack(stack));
    }

    private IAEFluidStack extractFluidFromAE(final IMEMonitor<IAEFluidStack> inv, final IAEFluidStack stack) throws GridAccessException {
        return Platform.poweredExtraction(this.proxy.getEnergy(), inv, stack.copy(), this.source);
    }

    private IAEFluidStack insertFluidToAE(final IMEMonitor<IAEFluidStack> inv, final IAEFluidStack stack) throws GridAccessException {
        return Platform.poweredInsert(this.proxy.getEnergy(), inv, stack.copy(), this.source);
    }

    private IAEGasStack extractGasFromAE(final IMEMonitor<IAEGasStack> inv, final GasStack stack) throws GridAccessException {
        return Platform.poweredExtraction(this.proxy.getEnergy(), inv, AEGasStack.of(stack), this.source);
    }

    private IAEGasStack insertGasToAE(final IMEMonitor<IAEGasStack> inv, final GasStack stack) throws GridAccessException {
        return Platform.poweredInsert(this.proxy.getEnergy(), inv, AEGasStack.of(stack), this.source);
    }

    @Override
    public void markNoUpdate() {
        if (needsAnyUpdate()) {
            try {
                this.proxy.getTick().alertDevice(this.proxy.getNode());
            } catch (GridAccessException e) {
                // NO-OP
            }
        }
        super.markNoUpdate();
    }

    @Override
    public NBTTagCompound downloadSettings() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setTag(ITEM_CONFIG_TAG, this.configInventory.writeNBT());
        this.fluidConfig.writeToNBT(tag, FLUID_CONFIG_TAG);
        tag.setTag(GAS_CONFIG_TAG, this.gasConfig.save());
        return tag;
    }

    @Override
    public void uploadSettings(NBTTagCompound settings) {
        if (settings.hasKey(ITEM_CONFIG_TAG)) {
            this.configInventory = IOInventory.deserialize(this, settings.getCompoundTag(ITEM_CONFIG_TAG));
        }
        this.fluidConfig.readFromNBT(settings, FLUID_CONFIG_TAG);
        this.gasConfig.load(settings.getCompoundTag(GAS_CONFIG_TAG));
        bindInventoryListeners();
        markForUpdateSync();
        try {
            this.proxy.getTick().alertDevice(this.proxy.getNode());
        } catch (GridAccessException e) {
            ModularMachinery.log.warn("Error while uploading settings", e);
        }
    }

    @Override
    public void onFluidInventoryChanged(final IAEFluidTank inv, final int slot) {
        if (!this.inTick && slot >= 0 && slot < this.changedFluidSlots.length) {
            this.changedFluidSlots[slot] = true;
        }
        if (inv == this.fluidTanks) {
            onBufferedInputStateChanged();
        }
        markNoUpdateSync();
    }

    @Override
    public void onFluidInventoryChanged(final IAEFluidTank inv, final int slot, final appeng.util.inv.InvOperation operation, final net.minecraftforge.fluids.FluidStack added, final net.minecraftforge.fluids.FluidStack removed) {
        onFluidInventoryChanged(inv, slot);
    }

    @Override
    public void onGasInventoryChanged(final com.mekeng.github.common.me.inventory.IGasInventory iGasInventory, final int slot) {
        if (!this.inTick && slot >= 0 && slot < this.changedGasSlots.length) {
            this.changedGasSlots[slot] = true;
        }
        if (iGasInventory == this.gasTanks) {
            onBufferedInputStateChanged();
        }
        markNoUpdateSync();
    }

    @Override
    public void gridChanged() {
    }

    @Nonnull
    @Override
    public IGridNode getActionableNode() {
        return this.proxy.getNode();
    }

    @Nonnull
    @Override
    public AENetworkProxy getProxy() {
        return this.proxy;
    }

    @Nonnull
    @Override
    public DimensionalCoord getLocation() {
        return new DimensionalCoord(this);
    }

    @Nullable
    @Override
    public IGridNode getGridNode(@Nonnull final AEPartLocation dir) {
        return this.proxy.getNode();
    }

    @Nonnull
    @Override
    public AECableType getCableConnectionType(@Nonnull final AEPartLocation dir) {
        return AECableType.SMART;
    }

    @Override
    public void securityBreak() {
        getWorld().destroyBlock(getPos(), true);
    }
}
