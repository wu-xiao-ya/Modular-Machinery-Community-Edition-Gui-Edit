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
import appeng.me.GridAccessException;
import appeng.me.helpers.AENetworkProxy;
import appeng.me.helpers.IGridProxyable;
import appeng.me.helpers.MachineSource;
import appeng.util.Platform;
import com.fushu.mmceguiext.common.registry.CustomAEMixedOutputBusRegistry;
import com.mekeng.github.common.me.data.IAEGasStack;
import com.mekeng.github.common.me.data.impl.AEGasStack;
import com.mekeng.github.common.me.inventory.IGasInventoryHost;
import com.mekeng.github.common.me.inventory.impl.GasInventory;
import com.mekeng.github.common.me.storage.IGasStorageChannel;
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
import hellfirepvp.modularmachinery.common.util.IOInventory;
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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;

public class TileCustomAEMixedOutputBus extends TileColorableMachineComponent implements
    SelectiveUpdateTileEntity,
    MachineComponentTile,
    IActionHost,
    IGridProxyable,
    IGridTickable,
    appeng.fluids.util.IAEFluidInventory,
    IGasInventoryHost {

    private static final int ITEM_SLOT_COUNT = 25;
    private static final int DEFAULT_TANK_SLOT_COUNT = 1;
    private static final int FLUID_TANK_CAPACITY = 8000;
    private static final int GAS_TANK_CAPACITY = 8000;

    protected final AENetworkProxy proxy;
    protected final IActionSource source;

    protected final IItemStorageChannel itemChannel = AEApi.instance().storage().getStorageChannel(IItemStorageChannel.class);
    protected final appeng.api.storage.channels.IFluidStorageChannel fluidChannel = AEApi.instance().storage().getStorageChannel(appeng.api.storage.channels.IFluidStorageChannel.class);
    protected final IGasStorageChannel gasChannel = AEApi.instance().storage().getStorageChannel(IGasStorageChannel.class);

    private IOInventory inventory = buildInventory();
    private AEFluidInventoryUpgradeable fluidTanks = createFluidTanks(DEFAULT_TANK_SLOT_COUNT);
    private GasInventory gasTanks = createGasTanks(DEFAULT_TANK_SLOT_COUNT);
    private GasInventoryHandler gasHandler = new GasInventoryHandler(gasTanks);

    private boolean[] changedItemSlots = new boolean[ITEM_SLOT_COUNT];
    private boolean[] changedFluidSlots = new boolean[DEFAULT_TANK_SLOT_COUNT];
    private boolean[] changedGasSlots = new boolean[DEFAULT_TANK_SLOT_COUNT];
    private long lastFullItemCheckTick = 0;
    private long lastFullFluidCheckTick = 0;
    private long lastFullGasCheckTick = 0;
    private boolean inTick = false;

    private String definitionId = "";
    private final CombinedBusHandler combinedHandler = new CombinedBusHandler();
    private final MachineComponent<InfItemFluidHandler> combinedComponent;

    public TileCustomAEMixedOutputBus() {
        this.source = new MachineSource(this);
        this.proxy = new AENetworkProxy(this, "aeProxy", getVisualItemStack(), true);
        this.proxy.setIdlePowerUsage(1.0D);
        this.proxy.setFlags(GridFlags.REQUIRE_CHANNEL);
        this.inventory.setListener(slot -> {
            synchronized (this) {
                if (slot >= 0 && slot < this.changedItemSlots.length) {
                    this.changedItemSlots[slot] = true;
                }
            }
        });
        this.combinedComponent = new MachineComponent<InfItemFluidHandler>(IOType.OUTPUT) {
            @Override
            public ComponentType getComponentType() {
                return ComponentTypesMM.COMPONENT_ITEM_FLUID_GAS;
            }

            @Override
            public InfItemFluidHandler getContainerProvider() {
                return combinedHandler;
            }
        };
    }

    private IOInventory buildInventory() {
        int[] outputSlots = new int[ITEM_SLOT_COUNT];
        for (int i = 0; i < ITEM_SLOT_COUNT; i++) {
            outputSlots[i] = i;
        }
        return new IOInventory(this, new int[0], outputSlots);
    }

    private AEFluidInventoryUpgradeable createFluidTanks(final int slotCount) {
        return new AEFluidInventoryUpgradeable(this, Math.max(1, slotCount), FLUID_TANK_CAPACITY);
    }

    private GasInventory createGasTanks(final int slotCount) {
        return new GasInventory(Math.max(1, slotCount), GAS_TANK_CAPACITY, this);
    }

    private void configureTankLayout() {
        final int fluidSlots = resolveTankComponentCount("fluid_storage");
        final int gasSlots = resolveTankComponentCount("gas_storage");
        if (this.fluidTanks.getSlots() == fluidSlots && this.gasTanks.size() == gasSlots) {
            return;
        }

        NBTTagCompound fluidData = new NBTTagCompound();
        this.fluidTanks.writeToNBT(fluidData, "tanks");
        NBTTagCompound gasData = this.gasTanks.save();

        this.fluidTanks = createFluidTanks(fluidSlots);
        this.gasTanks = createGasTanks(gasSlots);
        this.gasHandler = new GasInventoryHandler(this.gasTanks);

        this.fluidTanks.readFromNBT(fluidData, "tanks");
        this.gasTanks.load(gasData);
        this.changedFluidSlots = new boolean[this.fluidTanks.getSlots()];
        this.changedGasSlots = new boolean[this.gasTanks.size()];
    }

    private int resolveTankComponentCount(final String role) {
        CustomAEMixedOutputBusRegistry.Def def = getDefinition();
        if (def == null || def.gui == null || def.gui.components == null || def.gui.components.isEmpty()) {
            return DEFAULT_TANK_SLOT_COUNT;
        }
        int count = 0;
        int maxIndexed = 0;
        for (CustomAEMixedOutputBusRegistry.ComponentDef component : def.gui.components) {
            if (component == null) {
                continue;
            }
            if (!"tank".equalsIgnoreCase(component.type == null ? "" : component.type)) {
                continue;
            }
            if (!role.equalsIgnoreCase(component.role == null ? "" : component.role)) {
                continue;
            }
            count++;
            if (component.index >= 0) {
                maxIndexed = Math.max(maxIndexed, component.index + 1);
            }
        }
        return Math.max(DEFAULT_TANK_SLOT_COUNT, Math.max(count, maxIndexed));
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
            this.inventory.setListener(slot -> {
                synchronized (this) {
                    if (slot >= 0 && slot < this.changedItemSlots.length) {
                        this.changedItemSlots[slot] = true;
                    }
                }
            });
        } else {
            this.inventory = buildInventory();
        }
        this.definitionId = compound.hasKey("definitionId") ? compound.getString("definitionId") : "";
        configureTankLayout();
        this.fluidTanks.readFromNBT(compound, "tanks");
        this.gasTanks.load(compound.getCompoundTag("gasTanks"));
        this.changedItemSlots = new boolean[this.inventory.getSlots()];
        this.changedFluidSlots = new boolean[this.fluidTanks.getSlots()];
        this.changedGasSlots = new boolean[this.gasTanks.size()];
    }

    @Override
    public void writeCustomNBT(final NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        this.proxy.writeToNBT(compound);
        compound.setTag("inventory", this.inventory.writeNBT());
        this.fluidTanks.writeToNBT(compound, "tanks");
        compound.setTag("gasTanks", this.gasTanks.save());
        compound.setString("definitionId", this.definitionId == null ? "" : this.definitionId);
    }

    public IOInventory getInventory() {
        return this.inventory;
    }

    public AEFluidInventoryUpgradeable getFluidTanks() {
        return this.fluidTanks;
    }

    public GasInventory getGasTanks() {
        return this.gasTanks;
    }

    public void setDefinitionId(@Nullable String id) {
        this.definitionId = id == null ? "" : id.trim();
        configureTankLayout();
        markDirty();
    }

    @Nullable
    public String getDefinitionId() {
        return this.definitionId == null || this.definitionId.trim().isEmpty() ? null : this.definitionId.trim();
    }

    @Nullable
    public CustomAEMixedOutputBusRegistry.Def getDefinition() {
        return CustomAEMixedOutputBusRegistry.findById(this.definitionId);
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
        return this.combinedComponent;
    }

    @Nonnull
    public Collection<MachineComponent<?>> provideComponents() {
        return Collections.emptyList();
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
    }

    @Nonnull
    @Override
    public TickingRequest getTickingRequest(@Nonnull final IGridNode node) {
        return new TickingRequest(5, 60, !(hasItems() || hasFluid() || hasGas()), true);
    }

    @Nonnull
    @Override
    public TickRateModulation tickingRequest(@Nonnull final IGridNode node, final int ticksSinceLastCall) {
        if (!proxy.isActive()) {
            return TickRateModulation.IDLE;
        }
        boolean success = false;
        this.inTick = true;
        try {
            success |= tickItems();
            success |= tickFluids();
            success |= tickGases();
        } catch (GridAccessException ignored) {
            return TickRateModulation.IDLE;
        } finally {
            this.inTick = false;
        }
        return success ? TickRateModulation.FASTER : TickRateModulation.SLOWER;
    }

    private boolean tickItems() throws GridAccessException {
        int[] slots = getNeedUpdateItemSlots();
        if (slots.length == 0) {
            return false;
        }
        boolean success = false;
        ReadWriteLock rwLock = inventory.getRWLock();
        rwLock.writeLock().lock();
        try {
            IMEMonitor<IAEItemStack> inv = proxy.getStorage().getInventory(itemChannel);
            for (int slot : slots) {
                changedItemSlots[slot] = false;
                ItemStack stack = inventory.getStackInSlot(slot);
                if (stack.isEmpty()) {
                    continue;
                }
                IAEItemStack aeStack = itemChannel.createStack(stack);
                if (aeStack == null) {
                    continue;
                }
                IAEItemStack left = Platform.poweredInsert(proxy.getEnergy(), inv, aeStack, source);
                if (left == null) {
                    inventory.setStackInSlot(slot, ItemStack.EMPTY);
                    success = true;
                } else {
                    inventory.setStackInSlot(slot, left.createItemStack());
                    success |= left.getStackSize() != aeStack.getStackSize();
                }
            }
        } finally {
            rwLock.writeLock().unlock();
        }
        return success;
    }

    private boolean tickFluids() throws GridAccessException {
        int[] slots = getNeedUpdateFluidSlots();
        if (slots.length == 0) {
            return false;
        }
        boolean success = false;
        ReadWriteLock rwLock = fluidTanks.getRWLock();
        rwLock.writeLock().lock();
        try {
            IMEMonitor<IAEFluidStack> inv = proxy.getStorage().getInventory(fluidChannel);
            for (int slot : slots) {
                changedFluidSlots[slot] = false;
                IAEFluidStack fluid = fluidTanks.getFluidInSlot(slot);
                if (fluid == null) {
                    continue;
                }
                IAEFluidStack left = Platform.poweredInsert(proxy.getEnergy(), inv, fluid.copy(), source);
                if (left == null) {
                    fluidTanks.setFluidInSlot(slot, null);
                    success = true;
                } else {
                    fluidTanks.setFluidInSlot(slot, left);
                    success |= left.getStackSize() != fluid.getStackSize();
                }
            }
        } finally {
            rwLock.writeLock().unlock();
        }
        return success;
    }

    private boolean tickGases() throws GridAccessException {
        int[] slots = getNeedUpdateGasSlots();
        if (slots.length == 0) {
            return false;
        }
        boolean success = false;
        IMEMonitor<IAEGasStack> inv = proxy.getStorage().getInventory(gasChannel);
        synchronized (gasTanks) {
            for (int slot : slots) {
                changedGasSlots[slot] = false;
                GasStack gas = gasTanks.getGasStack(slot);
                if (gas == null) {
                    continue;
                }
                IAEGasStack left = Platform.poweredInsert(proxy.getEnergy(), inv, AEGasStack.of(gas), source);
                if (left == null) {
                    gasTanks.setGas(slot, null);
                    success = true;
                } else {
                    gasTanks.setGas(slot, left.getGasStack());
                    success |= left.getStackSize() != gas.amount;
                }
            }
        }
        return success;
    }

    private int[] getNeedUpdateItemSlots() {
        long current = world.getTotalWorldTime();
        if (lastFullItemCheckTick + 100 < current) {
            lastFullItemCheckTick = current;
            int[] slots = new int[inventory.getSlots()];
            for (int i = 0; i < slots.length; i++) {
                slots[i] = i;
            }
            return slots;
        }
        java.util.List<Integer> slots = new java.util.ArrayList<Integer>();
        for (int i = 0; i < changedItemSlots.length; i++) {
            if (changedItemSlots[i]) {
                slots.add(i);
            }
        }
        return slots.stream().mapToInt(Integer::intValue).toArray();
    }

    private int[] getNeedUpdateFluidSlots() {
        long current = world.getTotalWorldTime();
        if (lastFullFluidCheckTick + 100 < current) {
            lastFullFluidCheckTick = current;
            int[] slots = new int[this.fluidTanks.getSlots()];
            for (int i = 0; i < slots.length; i++) {
                slots[i] = i;
            }
            return slots;
        }
        List<Integer> slots = new ArrayList<Integer>();
        for (int i = 0; i < changedFluidSlots.length; i++) {
            if (changedFluidSlots[i]) {
                slots.add(i);
            }
        }
        return slots.stream().mapToInt(Integer::intValue).toArray();
    }

    private int[] getNeedUpdateGasSlots() {
        long current = world.getTotalWorldTime();
        if (lastFullGasCheckTick + 100 < current) {
            lastFullGasCheckTick = current;
            int[] slots = new int[this.gasTanks.size()];
            for (int i = 0; i < slots.length; i++) {
                slots[i] = i;
            }
            return slots;
        }
        List<Integer> slots = new ArrayList<Integer>();
        for (int i = 0; i < changedGasSlots.length; i++) {
            if (changedGasSlots[i]) {
                slots.add(i);
            }
        }
        return slots.stream().mapToInt(Integer::intValue).toArray();
    }

    public boolean hasItems() {
        for (int i = 0; i < inventory.getSlots(); i++) {
            if (!inventory.getStackInSlot(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasFluid() {
        for (int i = 0; i < fluidTanks.getSlots(); i++) {
            if (fluidTanks.getFluidInSlot(i) != null) {
                return true;
            }
        }
        return false;
    }

    public boolean hasGas() {
        for (int i = 0; i < gasTanks.size(); i++) {
            if (gasTanks.getGasStack(i) != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void markNoUpdate() {
        if (hasItems() || hasFluid() || hasGas()) {
            try {
                proxy.getTick().alertDevice(proxy.getNode());
            } catch (GridAccessException ignored) {
            }
        }
        super.markNoUpdate();
    }

    @Override
    public void onFluidInventoryChanged(final appeng.fluids.util.IAEFluidTank inv, final int slot) {
        if (!inTick && slot >= 0 && slot < changedFluidSlots.length) {
            changedFluidSlots[slot] = true;
        }
        markNoUpdateSync();
    }

    @Override
    public void onFluidInventoryChanged(final appeng.fluids.util.IAEFluidTank inv, final int slot, final appeng.util.inv.InvOperation operation, final net.minecraftforge.fluids.FluidStack added, final net.minecraftforge.fluids.FluidStack removed) {
        onFluidInventoryChanged(inv, slot);
    }

    @Override
    public void onGasInventoryChanged(final com.mekeng.github.common.me.inventory.IGasInventory inventory, final int slot) {
        if (!inTick && slot >= 0 && slot < changedGasSlots.length) {
            changedGasSlots[slot] = true;
        }
        markNoUpdateSync();
    }

    @Override
    public void gridChanged() {
    }

    @Nonnull
    @Override
    public IGridNode getActionableNode() {
        return proxy.getNode();
    }

    @Nonnull
    @Override
    public AENetworkProxy getProxy() {
        return proxy;
    }

    @Nonnull
    @Override
    public DimensionalCoord getLocation() {
        return new DimensionalCoord(this);
    }

    @Nullable
    @Override
    public IGridNode getGridNode(@Nonnull AEPartLocation dir) {
        return proxy.getNode();
    }

    @Nonnull
    @Override
    public AECableType getCableConnectionType(@Nonnull AEPartLocation dir) {
        return AECableType.SMART;
    }

    @Override
    public void securityBreak() {
        getWorld().destroyBlock(getPos(), true);
    }

    private void logOutputExposure(final String phase) {
    }
}
