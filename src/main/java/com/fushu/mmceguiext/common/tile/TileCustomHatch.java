package com.fushu.mmceguiext.common.tile;

import com.fushu.mmceguiext.common.registry.CustomHatchRegistry;
import github.kasuminova.mmce.common.util.InfItemFluidHandler;
import github.kasuminova.mmce.common.util.IExtendedGasHandler;
import github.kasuminova.mmce.common.util.MultiGasTank;
import github.kasuminova.mmce.common.util.concurrent.ReadWriteLockProvider;
import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.lib.ComponentTypesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.tiles.base.MachineComponentTile;
import hellfirepvp.modularmachinery.common.tiles.base.SelectiveUpdateTileEntity;
import hellfirepvp.modularmachinery.common.tiles.base.TileEntityRestrictedTick;
import hellfirepvp.modularmachinery.common.util.IOInventory;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.GasTankInfo;
import mekanism.api.gas.IGasHandler;
import mekanism.api.gas.ITubeConnection;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.fml.common.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Optional.InterfaceList({
    @Optional.Interface(modid = "mekanism", iface = "mekanism.api.gas.IGasHandler"),
    @Optional.Interface(modid = "mekanism", iface = "mekanism.api.gas.ITubeConnection")
})
public class TileCustomHatch extends TileEntityRestrictedTick implements MachineComponentTile, github.kasuminova.mmce.common.tile.base.MachineCombinationComponent, SelectiveUpdateTileEntity, ReadWriteLockProvider, IGasHandler, ITubeConnection {
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;

    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final long componentGroupId = github.kasuminova.mmce.common.tile.base.MachineCombinationComponent.GROUP_ACQUIRER.incrementAndGet();
    private IOInventory inventory = new IOInventory(this, new int[]{INPUT_SLOT}, new int[]{OUTPUT_SLOT});
    private int[] recipeInputSlots = new int[]{INPUT_SLOT};
    private int[] recipeOutputSlots = new int[]{OUTPUT_SLOT};

    private int capacity = 1000;
    private int fluidCapacity = 1000;
    private int gasCapacity = 1000;
    private final ProcessorTank tank = new ProcessorTank(this.fluidCapacity);
    private final ProcessorGasTank gasTank = new ProcessorGasTank(this.gasCapacity, 1);
    private String hatchId = "";
    private String inventorySignature = "";

    public TileCustomHatch() {
        this.inventory.setListener(this::onInventoryChanged);
    }

    public void setDefinitionId(@Nullable String id) {
        this.hatchId = id == null ? "" : id.trim();
        syncDefinition();
    }

    @Nullable
    public String getDefinitionId() {
        return this.hatchId == null || this.hatchId.trim().isEmpty() ? null : this.hatchId.trim();
    }

    @Nullable
    public CustomHatchRegistry.CustomHatchDef getDefinition() {
        CustomHatchRegistry.CustomHatchDef def = CustomHatchRegistry.findById(this.hatchId);
        return def;
    }

    public IOInventory getInventory() {
        return this.inventory;
    }

    @Override
    public long getUniqueGroupID() {
        return this.componentGroupId;
    }

    @Nullable
    public FluidStack getFluidStack() {
        FluidStack fluid = this.tank.getFluid();
        return fluid == null ? null : fluid.copy();
    }

    public int getCapacity() {
        return this.fluidCapacity;
    }

    public int getFluidCapacity() {
        return this.fluidCapacity;
    }

    public int getGasCapacity() {
        return this.gasCapacity;
    }

    public int[] getRecipeInputSlots() {
        return this.recipeInputSlots.clone();
    }

    public int[] getRecipeOutputSlots() {
        return this.recipeOutputSlots.clone();
    }

    @Nullable
    @Optional.Method(modid = "mekanism")
    public GasStack getGasStack() {
        GasStack gas = this.gasTank.getGasInSlot(0);
        return gas == null ? null : gas.copy();
    }

    @Nullable
    @Override
    public MachineComponent<?> provideComponent() {
        CustomHatchRegistry.CustomHatchDef def = getDefinition();
        if (def != null && def.machineComponents != null && !def.machineComponents.isEmpty()) {
            return null;
        }
        String componentType = def == null || def.componentType == null ? "fluid" : def.componentType;
        IOType ioType = parseIOType(def == null ? null : def.ioType);
        return createMachineComponent(componentType, ioType, getUniqueGroupID());
    }

    @Nonnull
    @Override
    public Collection<MachineComponent<?>> provideComponents() {
        CustomHatchRegistry.CustomHatchDef def = getDefinition();
        if (def == null || def.machineComponents == null || def.machineComponents.isEmpty()) {
            return Collections.emptyList();
        }
        List<MachineComponent<?>> out = new ArrayList<MachineComponent<?>>();
        long baseGroupId = getUniqueGroupID();
        if (addCombinedComponents(def.machineComponents, out, baseGroupId)) {
            return out;
        }
        int index = 0;
        for (CustomHatchRegistry.MachineComponentDef componentDef : def.machineComponents) {
            if (componentDef == null || componentDef.type == null) {
                continue;
            }
            MachineComponent<?> component = createMachineComponent(componentDef.type, parseIOType(componentDef.io), baseGroupId + index++);
            if (component != null) {
                out.add(component);
            }
        }
        return out;
    }

    private boolean addCombinedComponents(List<CustomHatchRegistry.MachineComponentDef> defs, List<MachineComponent<?>> out, long groupId) {
        boolean input = false;
        boolean output = false;
        boolean combinable = false;
        for (CustomHatchRegistry.MachineComponentDef def : defs) {
            if (def == null || def.type == null) {
                continue;
            }
            if (isExplicitCombinedComponentType(def.type)) {
                combinable = true;
                if (parseIOType(def.io) == IOType.OUTPUT) {
                    output = true;
                } else {
                    input = true;
                }
            }
        }
        if (!combinable) {
            return false;
        }
        if (input) {
            out.add(createCombinedMachineComponent(IOType.INPUT, groupId));
        }
        if (output) {
            out.add(createCombinedMachineComponent(IOType.OUTPUT, -1L));
        }
        return true;
    }

    @Override
    public void doRestrictedTick() {
        if (!world.isRemote && ticksExisted % 20 == 0) {
            tryProcess();
        }
    }

    public void onPlayerInteract(EntityPlayer player, EnumHand hand) {
        tryProcess();
    }

    private void onInventoryChanged(int slot) {
        if (world != null && !world.isRemote) {
            tryProcess();
        }
    }

    private void tryProcess() {
        InventoryLayout layout = buildInventoryLayout(getDefinition());
        for (int inputSlot : layout.inputSlots) {
            if (tryProcessInputSlot(inputSlot, layout.outputSlots)) {
                return;
            }
        }
    }

    private boolean tryProcessInputSlot(int inputSlot, int[] outputSlots) {
        ItemStack input = this.inventory.getStackInSlot(inputSlot);
        if (input.isEmpty()) {
            return false;
        }

        IFluidHandlerItem handler = FluidUtil.getFluidHandler(input.copy());
        if (handler == null) {
            return false;
        }

        FluidStack drained = handler.drain(Integer.MAX_VALUE, false);
        if (drained == null || drained.amount <= 0) {
            return false;
        }
        if (this.tank.fill(drained.copy(), false) < drained.amount) {
            return false;
        }

        FluidStack actuallyDrained = handler.drain(drained.amount, true);
        if (actuallyDrained == null || actuallyDrained.amount != drained.amount) {
            return false;
        }
        ItemStack emptiedContainer = handler.getContainer();
        int outputSlot = findOutputSlotFor(emptiedContainer, outputSlots);
        if (outputSlot < 0) {
            return false;
        }

        this.tank.fill(drained.copy(), true);
        this.inventory.setStackInSlot(inputSlot, ItemStack.EMPTY);
        ItemStack output = this.inventory.getStackInSlot(outputSlot);
        if (output.isEmpty()) {
            this.inventory.setStackInSlot(outputSlot, emptiedContainer.copy());
        } else {
            ItemStack grown = output.copy();
            grown.grow(emptiedContainer.getCount());
            this.inventory.setStackInSlot(outputSlot, grown);
        }
        markNoUpdateSync();
        return true;
    }

    private int findOutputSlotFor(ItemStack stack, int[] outputSlots) {
        if (stack.isEmpty()) {
            return outputSlots.length > 0 ? outputSlots[0] : -1;
        }
        for (int slot : outputSlots) {
            ItemStack output = this.inventory.getStackInSlot(slot);
            if (output.isEmpty()) {
                return slot;
            }
            if (ItemStack.areItemStacksEqual(output, stack)
                && ItemStack.areItemStackTagsEqual(output, stack)
                && output.getCount() + stack.getCount() <= output.getMaxStackSize()) {
                return slot;
            }
        }
        return -1;
    }

    private void syncDefinition() {
        CustomHatchRegistry.CustomHatchDef def = getDefinition();
        if (def != null) {
            this.capacity = Math.max(1, def.capacity);
            this.fluidCapacity = Math.max(1, def.fluidCapacity > 0 ? def.fluidCapacity : this.capacity);
            this.gasCapacity = Math.max(1, def.gasCapacity > 0 ? def.gasCapacity : this.capacity);
            this.tank.setCustomCapacity(this.fluidCapacity);
            this.gasTank.setCapacity(this.gasCapacity);
            syncInventoryLayout(def);
        }
    }

    private void syncInventoryLayout(CustomHatchRegistry.CustomHatchDef def) {
        InventoryLayout layout = buildInventoryLayout(def);
        String signature = layout.signature();
        if (signature.equals(this.inventorySignature)) {
            return;
        }
        IOInventory old = this.inventory;
        IOInventory next = new IOInventory(this, layout.inputSlots, layout.outputSlots);
        int copySlots = Math.min(old.getSlots(), next.getSlots());
        for (int i = 0; i < copySlots; i++) {
            next.setStackInSlot(i, old.getStackInSlot(i));
        }
        next.setListener(this::onInventoryChanged);
        this.inventory = next;
        this.recipeInputSlots = layout.inputSlots;
        this.recipeOutputSlots = layout.outputSlots;
        this.inventorySignature = signature;
    }

    public static InventoryLayout buildInventoryLayout(@Nullable CustomHatchRegistry.CustomHatchDef def) {
        if (def == null || def.gui == null || def.gui.components == null || def.gui.components.isEmpty()) {
            return new InventoryLayout(new int[]{INPUT_SLOT}, new int[]{OUTPUT_SLOT}, 2);
        }
        List<Integer> inputs = new ArrayList<Integer>();
        List<Integer> outputs = new ArrayList<Integer>();
        int nextIndex = 0;
        int maxIndex = -1;
        for (CustomHatchRegistry.ComponentDef component : def.gui.components) {
            if (component == null || !"slot".equalsIgnoreCase(component.type)) {
                continue;
            }
            int index = component.index >= 0 ? component.index : nextIndex++;
            maxIndex = Math.max(maxIndex, index);
            if ("output".equalsIgnoreCase(component.role)) {
                outputs.add(Integer.valueOf(index));
            } else {
                inputs.add(Integer.valueOf(index));
            }
        }
        if (maxIndex < 0) {
            return new InventoryLayout(new int[]{INPUT_SLOT}, new int[]{OUTPUT_SLOT}, 2);
        }
        return new InventoryLayout(toIntArray(inputs), toIntArray(outputs), maxIndex + 1);
    }

    private static IOType parseIOType(@Nullable String value) {
        if ("output".equalsIgnoreCase(value) || "out".equalsIgnoreCase(value)) {
            return IOType.OUTPUT;
        }
        return IOType.INPUT;
    }

    private static boolean isExplicitCombinedComponentType(@Nullable String componentType) {
        return "mixed".equalsIgnoreCase(componentType)
            || "hybrid".equalsIgnoreCase(componentType)
            || "item_fluid".equalsIgnoreCase(componentType)
            || "item_fluid_gas".equalsIgnoreCase(componentType);
    }

    private MachineComponent<?> createCombinedMachineComponent(IOType ioType, long groupId) {
        return new MachineComponent<InfItemFluidHandler>(ioType) {
            @Override
            public ComponentType getComponentType() {
                return ComponentTypesMM.COMPONENT_ITEM_FLUID_GAS;
            }

            @Override
            public InfItemFluidHandler getContainerProvider() {
                int[] slots = ioType == IOType.OUTPUT ? TileCustomHatch.this.recipeOutputSlots : TileCustomHatch.this.recipeInputSlots;
                return new CombinedHatchHandler(new FilteredItemHandlerView(TileCustomHatch.this.inventory, slots), TileCustomHatch.this.tank, TileCustomHatch.this.gasTank);
            }

            @Override
            public long getGroupID() {
                return groupId;
            }
        };
    }

    @Nullable
    private MachineComponent<?> createMachineComponent(String componentType, IOType ioType, long groupId) {
        if ("mixed".equalsIgnoreCase(componentType)
            || "hybrid".equalsIgnoreCase(componentType)
            || "item_fluid".equalsIgnoreCase(componentType)
            || "item_fluid_gas".equalsIgnoreCase(componentType)) {
            return createCombinedMachineComponent(ioType, groupId);
        }
        if ("item".equalsIgnoreCase(componentType)) {
            return new MachineComponent.ItemBus(ioType) {
                @Override
                public net.minecraftforge.items.IItemHandlerModifiable getContainerProvider() {
                    return new FilteredItemHandlerView(TileCustomHatch.this.inventory, ioType == IOType.OUTPUT ? TileCustomHatch.this.recipeOutputSlots : TileCustomHatch.this.recipeInputSlots);
                }

                @Override
                public long getGroupID() {
                    return groupId;
                }
            };
        }
        if ("fluid".equalsIgnoreCase(componentType)) {
            return new MachineComponent.FluidHatch(ioType) {
                @Override
                public FluidTank getContainerProvider() {
                    return TileCustomHatch.this.tank;
                }

                @Override
                public long getGroupID() {
                    return groupId;
                }
            };
        }
        if ("gas".equalsIgnoreCase(componentType)) {
            return new MachineComponent<IExtendedGasHandler>(ioType) {
                @Override
                public ComponentType getComponentType() {
                    return ComponentTypesMM.COMPONENT_GAS;
                }

                @Override
                public IExtendedGasHandler getContainerProvider() {
                    return TileCustomHatch.this.gasTank;
                }

                @Override
                public long getGroupID() {
                    return groupId;
                }
            };
        }
        return null;
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        this.hatchId = compound.hasKey("hatchId") ? compound.getString("hatchId") : "";
        if (compound.hasKey("inv", Constants.NBT.TAG_COMPOUND)) {
            this.inventory.readNBT(compound.getCompoundTag("inv"));
            this.inventory.setListener(this::onInventoryChanged);
        }
        if (compound.hasKey("fluid", Constants.NBT.TAG_COMPOUND)) {
            this.tank.readFromNBT(compound.getCompoundTag("fluid"));
        }
        if (compound.hasKey("gas", Constants.NBT.TAG_COMPOUND)) {
            this.gasTank.readFromNBT(compound, "gas");
        }
        this.capacity = compound.hasKey("capacity") ? compound.getInteger("capacity") : this.capacity;
        this.fluidCapacity = compound.hasKey("fluidCapacity") ? compound.getInteger("fluidCapacity") : this.capacity;
        this.gasCapacity = compound.hasKey("gasCapacity") ? compound.getInteger("gasCapacity") : this.capacity;
        syncDefinition();
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        compound.setString("hatchId", this.hatchId == null ? "" : this.hatchId);
        compound.setTag("inv", this.inventory.writeNBT());
        compound.setTag("fluid", this.tank.writeToNBT(new NBTTagCompound()));
        this.gasTank.writeToNBT(compound, "gas");
        compound.setInteger("capacity", this.capacity);
        compound.setInteger("fluidCapacity", this.fluidCapacity);
        compound.setInteger("gasCapacity", this.gasCapacity);
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY
            || capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY
            || isMekanismGasCapability(capability)
            || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return (T) this.inventory;
        }
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return (T) this.tank;
        }
        if (isMekanismGasCapability(capability)) {
            return (T) this;
        }
        return super.getCapability(capability, facing);
    }

    private static boolean isMekanismGasCapability(@Nullable Capability<?> capability) {
        if (capability == null) {
            return false;
        }
        String gasType = IGasHandler.class.getName();
        String tubeConnectionName = ITubeConnection.class.getName();
        return gasType.equals(capability.getName()) || tubeConnectionName.equals(capability.getName());
    }

    @Override
    @Optional.Method(modid = "mekanism")
    public boolean canTubeConnect(EnumFacing side) {
        return true;
    }

    @Override
    @Optional.Method(modid = "mekanism")
    public int receiveGas(EnumFacing side, GasStack stack, boolean doTransfer) {
        return this.gasTank.receiveGas(side, stack, doTransfer);
    }

    @Override
    @Optional.Method(modid = "mekanism")
    public GasStack drawGas(EnumFacing side, int amount, boolean doTransfer) {
        return this.gasTank.drawGas(side, amount, doTransfer);
    }

    @Override
    @Optional.Method(modid = "mekanism")
    public boolean canReceiveGas(EnumFacing side, Gas type) {
        return this.gasTank.canReceiveGas(side, type);
    }

    @Override
    @Optional.Method(modid = "mekanism")
    public boolean canDrawGas(EnumFacing side, Gas type) {
        return this.gasTank.canDrawGas(side, type);
    }

    @Nonnull
    @Override
    @Optional.Method(modid = "mekanism")
    public GasTankInfo[] getTankInfo() {
        return this.gasTank.getTankInfo();
    }

    @Nonnull
    @Override
    public ReadWriteLock getRWLock() {
        return this.rwLock;
    }

    private class ProcessorTank extends FluidTank {
        public ProcessorTank(int capacity) {
            super(capacity);
        }

        public void setCustomCapacity(int capacity) {
            this.capacity = Math.max(1, capacity);
            if (this.fluid != null && this.fluid.amount > this.capacity) {
                this.fluid.amount = this.capacity;
            }
        }

        @Override
        protected void onContentsChanged() {
            markForUpdateSync();
        }
    }

    private class ProcessorGasTank extends MultiGasTank {
        public ProcessorGasTank(int capacity, int tankCount) {
            super(capacity, tankCount);
            setOnSlotChanged(slot -> markForUpdateSync());
        }
    }

    private static class CombinedHatchHandler extends InfItemFluidHandler {
        private final IItemHandlerModifiable itemHandler;
        private final IFluidHandler fluidHandler;
        private final IExtendedGasHandler gasHandler;

        private CombinedHatchHandler(IItemHandlerModifiable itemHandler, IFluidHandler fluidHandler, IExtendedGasHandler gasHandler) {
            super(itemHandler, fluidHandler);
            this.itemHandler = itemHandler;
            this.fluidHandler = fluidHandler;
            this.gasHandler = gasHandler;
        }

        @Override
        public synchronized void setStackInSlot(int slot, @Nonnull ItemStack stack) {
            this.itemHandler.setStackInSlot(slot, stack);
        }

        @Override
        public int getSlots() {
            return this.itemHandler.getSlots();
        }

        @Nonnull
        @Override
        public ItemStack getStackInSlot(int slot) {
            return this.itemHandler.getStackInSlot(slot);
        }

        @Nonnull
        @Override
        public synchronized ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            return this.itemHandler.insertItem(slot, stack, simulate);
        }

        @Nonnull
        @Override
        public synchronized ItemStack extractItem(int slot, int amount, boolean simulate) {
            return this.itemHandler.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return this.itemHandler.getSlotLimit(slot);
        }

        @Override
        public synchronized IFluidTankProperties[] getTankProperties() {
            return this.fluidHandler.getTankProperties();
        }

        @Override
        public synchronized int fill(FluidStack resource, boolean doFill) {
            return this.fluidHandler.fill(resource, doFill);
        }

        @Nullable
        @Override
        public synchronized FluidStack drain(FluidStack resource, boolean doDrain) {
            return this.fluidHandler.drain(resource, doDrain);
        }

        @Nullable
        @Override
        public synchronized FluidStack drain(int maxDrain, boolean doDrain) {
            return this.fluidHandler.drain(maxDrain, doDrain);
        }

        @Override
        @Optional.Method(modid = "mekanism")
        public GasStack drawGas(GasStack toDraw, boolean doTransfer) {
            return this.gasHandler.drawGas(toDraw, doTransfer);
        }

        @Override
        @Optional.Method(modid = "mekanism")
        public int receiveGas(@Nullable EnumFacing side, GasStack toReceive, boolean doTransfer) {
            return this.gasHandler.receiveGas(side, toReceive, doTransfer);
        }

        @Override
        @Optional.Method(modid = "mekanism")
        public GasStack drawGas(@Nullable EnumFacing side, int drawAmount, boolean doTransfer) {
            return this.gasHandler.drawGas(side, drawAmount, doTransfer);
        }

        @Override
        @Optional.Method(modid = "mekanism")
        public boolean canReceiveGas(@Nullable EnumFacing side, Gas gas) {
            return this.gasHandler.canReceiveGas(side, gas);
        }

        @Override
        @Optional.Method(modid = "mekanism")
        public boolean canDrawGas(@Nullable EnumFacing side, Gas gas) {
            return this.gasHandler.canDrawGas(side, gas);
        }

        @Nonnull
        @Override
        @Optional.Method(modid = "mekanism")
        public GasTankInfo[] getTankInfo() {
            return this.gasHandler.getTankInfo();
        }
    }

    private class FilteredItemHandlerView implements IItemHandlerModifiable {
        private final IOInventory delegate;
        private final int[] slots;

        private FilteredItemHandlerView(IOInventory delegate, int[] slots) {
            this.delegate = delegate;
            this.slots = slots == null ? new int[0] : slots.clone();
        }

        private int translateSlot(int slot) {
            return slot >= 0 && slot < this.slots.length ? this.slots[slot] : -1;
        }

        @Override
        public void setStackInSlot(int slot, @Nonnull ItemStack stack) {
            int realSlot = translateSlot(slot);
            if (realSlot >= 0) {
                this.delegate.setStackInSlot(realSlot, stack);
            }
        }

        @Override
        public int getSlots() {
            return this.slots.length;
        }

        @Nonnull
        @Override
        public ItemStack getStackInSlot(int slot) {
            int realSlot = translateSlot(slot);
            return realSlot >= 0 ? this.delegate.getStackInSlot(realSlot) : ItemStack.EMPTY;
        }

        @Nonnull
        @Override
        public ItemStack insertItem(int slot, @Nonnull ItemStack stack, boolean simulate) {
            int realSlot = translateSlot(slot);
            return realSlot >= 0 ? this.delegate.insertItem(realSlot, stack, simulate) : stack;
        }

        @Nonnull
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            int realSlot = translateSlot(slot);
            return realSlot >= 0 ? this.delegate.extractItem(realSlot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            int realSlot = translateSlot(slot);
            return realSlot >= 0 ? this.delegate.getSlotLimit(realSlot) : 0;
        }
    }

    private static int[] toIntArray(List<Integer> values) {
        int[] out = new int[values.size()];
        for (int i = 0; i < values.size(); i++) {
            out[i] = values.get(i).intValue();
        }
        return out;
    }

    public static class InventoryLayout {
        public final int[] inputSlots;
        public final int[] outputSlots;
        public final int slotCount;

        public InventoryLayout(int[] inputSlots, int[] outputSlots, int slotCount) {
            this.inputSlots = inputSlots;
            this.outputSlots = outputSlots;
            this.slotCount = Math.max(1, slotCount);
        }

        public String signature() {
            return java.util.Arrays.toString(this.inputSlots) + "|" + java.util.Arrays.toString(this.outputSlots) + "|" + this.slotCount;
        }
    }
}
