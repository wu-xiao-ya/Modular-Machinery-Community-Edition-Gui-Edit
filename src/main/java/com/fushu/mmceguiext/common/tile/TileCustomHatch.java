package com.fushu.mmceguiext.common.tile;

import com.fushu.mmceguiext.common.block.BlockCustomHatch;
import com.fushu.mmceguiext.common.energy.ILongEnergyStorage;
import com.fushu.mmceguiext.common.energy.LongEnergyCapability;
import com.fushu.mmceguiext.common.requirement.LongFluidIOHandler;
import com.fushu.mmceguiext.common.requirement.LongGasIOHandler;
import com.fushu.mmceguiext.common.registry.CustomHatchRegistry;
import com.fushu.mmceguiext.common.util.CustomIdValidator;
import github.kasuminova.mmce.common.util.InfItemFluidHandler;
import github.kasuminova.mmce.common.util.IExtendedGasHandler;
import github.kasuminova.mmce.common.util.concurrent.ReadWriteLockProvider;
import gregtech.api.capability.GregtechCapabilities;
import gregtech.api.capability.IEnergyContainer;
import hellfirepvp.modularmachinery.common.base.Mods;
import hellfirepvp.modularmachinery.common.crafting.ComponentType;
import hellfirepvp.modularmachinery.common.lib.ComponentTypesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import hellfirepvp.modularmachinery.common.tiles.base.MachineComponentTile;
import hellfirepvp.modularmachinery.common.tiles.base.SelectiveUpdateTileEntity;
import hellfirepvp.modularmachinery.common.tiles.base.TileEntityRestrictedTick;
import hellfirepvp.modularmachinery.common.util.IEnergyHandlerAsync;
import hellfirepvp.modularmachinery.common.util.IOInventory;
import mekanism.api.energy.IStrictEnergyAcceptor;
import mekanism.api.energy.IStrictEnergyOutputter;
import mekanism.api.gas.Gas;
import mekanism.api.gas.IGasItem;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.GasTankInfo;
import mekanism.api.gas.IGasHandler;
import mekanism.api.gas.ITubeConnection;
import mcjty.lib.api.power.IBigPower;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fml.common.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Optional.InterfaceList({
    @Optional.Interface(modid = "theoneprobe", iface = "mcjty.lib.api.power.IBigPower"),
    @Optional.Interface(modid = "mekanism", iface = "mekanism.api.energy.IStrictEnergyAcceptor"),
    @Optional.Interface(modid = "mekanism", iface = "mekanism.api.energy.IStrictEnergyOutputter"),
    @Optional.Interface(modid = "mekanism", iface = "mekanism.api.gas.IGasHandler"),
    @Optional.Interface(modid = "mekanism", iface = "mekanism.api.gas.ITubeConnection")
})
public class TileCustomHatch extends TileEntityRestrictedTick implements MachineComponentTile, github.kasuminova.mmce.common.tile.base.MachineCombinationComponent, SelectiveUpdateTileEntity, ReadWriteLockProvider, IBigPower, IStrictEnergyAcceptor, IStrictEnergyOutputter, IGasHandler, ITubeConnection {
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    private static final long GT_ENERGY_MULTIPLIER = 4L;
    private static final int MAX_DYNAMIC_SLOT_INDEX = 4095;
    private static final long MAX_HATCH_CAPACITY = Long.MAX_VALUE;
    private static final long MAX_ENERGY_CAPACITY = Long.MAX_VALUE;
    private static final String NBT_LONG_AMOUNT = "LongAmount";

    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final long componentGroupId = github.kasuminova.mmce.common.tile.base.MachineCombinationComponent.GROUP_ACQUIRER.incrementAndGet();
    private IOInventory inventory = new IOInventory(this, new int[]{INPUT_SLOT}, new int[]{OUTPUT_SLOT});
    private int[] recipeInputSlots = new int[]{INPUT_SLOT};
    private int[] recipeOutputSlots = new int[]{OUTPUT_SLOT};
    private boolean processingInventory = false;
    private boolean pendingInventoryProcess = false;

    private long capacity = 1000L;
    private long fluidCapacity = 1000L;
    private long gasCapacity = 1000L;
    private long energy = 0L;
    private long energyCapacity = 1000L;
    private long energyTransfer = 1000L;
    private boolean outputSlotLock = true;
    private boolean syncingDefinition = false;
    private ItemStack[] outputSlotTemplates = new ItemStack[0];
    private final ProcessorTank tank = new ProcessorTank(this.fluidCapacity);
    private final ProcessorGasTank gasTank = new ProcessorGasTank(this.gasCapacity, 1);
    private final ExternalFluidHandler inputFluidCapability = new ExternalFluidHandler(true, false);
    private final ExternalFluidHandler outputFluidCapability = new ExternalFluidHandler(false, true);
    private final ExternalFluidHandler bidirectionalFluidCapability = new ExternalFluidHandler(true, true);
    private final ExternalGasHandler inputGasCapability = new ExternalGasHandler(true, false);
    private final ExternalGasHandler outputGasCapability = new ExternalGasHandler(false, true);
    private final ExternalGasHandler bidirectionalGasCapability = new ExternalGasHandler(true, true);
    private final EnergyHandler energyHandler = new EnergyHandler();
    private final GTEnergyContainer gtEnergyContainer = new GTEnergyContainer();
    @Nullable
    private final Object opStorageHandler = createOPStorageHandler();
    private String hatchId = "";
    private String inventorySignature = "";

    public TileCustomHatch() {
        this.inventory.setListener(this::onInventoryChanged);
    }

    public void setDefinitionId(@Nullable String id) {
        String sanitized = CustomIdValidator.sanitizeResourceLocation(id);
        this.hatchId = sanitized == null ? "" : sanitized;
        syncDefinition();
    }

    @Nullable
    public String getDefinitionId() {
        return this.hatchId == null || this.hatchId.trim().isEmpty() ? null : this.hatchId.trim();
    }

    @Nullable
    public CustomHatchRegistry.CustomHatchDef getDefinition() {
        CustomHatchRegistry.CustomHatchDef def = CustomHatchRegistry.findById(this.hatchId);
        return def == null ? resolveDefinitionFromBlock() : def;
    }

    @Nullable
    private CustomHatchRegistry.CustomHatchDef resolveDefinitionFromBlock() {
        if (this.world == null || this.pos == null || !this.world.isBlockLoaded(this.pos)) {
            return null;
        }
        if (!(this.world.getBlockState(this.pos).getBlock() instanceof BlockCustomHatch)) {
            return null;
        }
        BlockCustomHatch block = (BlockCustomHatch) this.world.getBlockState(this.pos).getBlock();
        String registryId = block.getRegistryName() == null ? null : block.getRegistryName().toString();
        if (registryId != null && !registryId.equals(this.hatchId)) {
            this.hatchId = registryId;
            syncDefinition();
            if (!this.world.isRemote) {
                markChunkDirty();
            }
        }
        return block.getDefinition();
    }

    public IOInventory getInventory() {
        return this.inventory;
    }

    public void refreshDefinitionLayout() {
        syncDefinition();
    }

    public void refreshDefinitionLayout(CustomHatchRegistry.CustomHatchDef def) {
        if (def == null) {
            syncDefinition();
            return;
        }
        applyDefinition(def);
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

    public long getCapacity() {
        return this.fluidCapacity;
    }

    public long getFluidCapacity() {
        return this.fluidCapacity;
    }

    public long getGasCapacity() {
        return this.gasCapacity;
    }

    public long getFluidAmountLong() {
        return this.tank.getFluidAmountLong();
    }

    @Optional.Method(modid = "mekanism")
    public long getGasAmountLong() {
        return this.gasTank.getGasAmountLong();
    }

    public long getEnergyStoredLong() {
        return this.energy;
    }

    public long getEnergyCapacity() {
        return this.energyCapacity;
    }

    public long getEnergyTransfer() {
        return this.energyTransfer;
    }

    @Override
    @Optional.Method(modid = "theoneprobe")
    public long getStoredPower() {
        return this.energy;
    }

    public double getFluidFillRatio() {
        return fillRatio(getFluidAmountLong(), getFluidCapacity());
    }

    public double getGasFillRatio() {
        return fillRatio(this.gasTank.getGasAmountLong(), getGasCapacity());
    }

    public double getEnergyFillRatio() {
        return fillRatio(getEnergyStoredLong(), getEnergyCapacity());
    }

    public void applyClientEnergyStored(long value) {
        this.energy = clampEnergy(value);
    }

    public void applyClientEnergyCapacity(long value) {
        this.energyCapacity = clampEnergyCapacity(value);
        this.energy = clampEnergy(this.energy);
    }

    public void applyClientEnergyTransfer(long value) {
        this.energyTransfer = clampEnergyCapacity(value);
    }

    public int[] getRecipeInputSlots() {
        return this.recipeInputSlots.clone();
    }

    public int[] getRecipeOutputSlots() {
        return this.recipeOutputSlots.clone();
    }

    public void writeDroppedData(NBTTagCompound compound) {
        if (compound == null) {
            return;
        }
        compound.setTag("inv", this.inventory.writeNBT());
        compound.setTag("fluid", this.tank.writeToNBT(new NBTTagCompound()));
        this.gasTank.writeToNBT(compound, "gas");
        compound.setLong("capacity", this.capacity);
        compound.setLong("fluidCapacity", this.fluidCapacity);
        compound.setLong("gasCapacity", this.gasCapacity);
        compound.setLong("energy", this.energy);
        compound.setLong("energyCapacity", this.energyCapacity);
        compound.setLong("energyTransfer", this.energyTransfer);
        writeOutputSlotTemplates(compound);
    }

    public void readDroppedData(@Nullable NBTTagCompound compound) {
        if (compound == null) {
            return;
        }
        syncDefinition();
        if (compound.hasKey("inv", Constants.NBT.TAG_COMPOUND)) {
            this.inventory.readNBT(compound.getCompoundTag("inv"));
            this.inventory.setListener(this::onInventoryChanged);
            ensureInventoryLayoutMatchesDefinition();
        }
        readOutputSlotTemplates(compound);
        if (compound.hasKey("fluid", Constants.NBT.TAG_COMPOUND)) {
            this.tank.readFromNBT(compound.getCompoundTag("fluid"));
            clampStoredFluid();
        }
        if (compound.hasKey("gas", Constants.NBT.TAG_COMPOUND)) {
            this.gasTank.readFromNBT(compound, "gas");
            clampStoredGas();
        }
        if (compound.hasKey("energy")) {
            this.energy = clampEnergy(compound.getLong("energy"));
        }
        markNoUpdateSync();
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
        boolean combined = addCombinedComponents(def.machineComponents, out, baseGroupId);
        int index = combined ? 1 : 0;
        for (CustomHatchRegistry.MachineComponentDef componentDef : def.machineComponents) {
            if (componentDef == null || componentDef.type == null) {
                continue;
            }
            if (combined && isExplicitCombinedComponentType(componentDef.type)) {
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

    private boolean hasDeclaredAccess(CustomHatchRegistry.CustomHatchDef def, ExternalAccessKind kind, IOType ioType) {
        if (def == null || def.machineComponents == null || def.machineComponents.isEmpty()) {
            return false;
        }
        for (CustomHatchRegistry.MachineComponentDef component : def.machineComponents) {
            if (component == null || !kind.matches(component.type)) {
                continue;
            }
            if (parseIOType(component.io) == ioType) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void doRestrictedTick() {
        if (!world.isRemote) {
            tryProcess();
        }
    }

    public void onPlayerInteract(EntityPlayer player, EnumHand hand) {
        scheduleInventoryProcess();
    }

    public boolean tryHeldItemInteraction(EntityPlayer player, EnumHand hand) {
        ItemStack held = player.getHeldItem(hand);
        if (held.isEmpty()) {
            return false;
        }
        if (tryProcessHeldFluidItem(player, hand, held)) {
            return true;
        }
        return tryProcessHeldGasItem(player, hand, held);
    }

    private void onInventoryChanged(int slot) {
        if (slot < 0) {
            return;
        }
        if (isOutputSlot(slot)) {
            ItemStack stack = this.inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                lockOutputSlot(slot, stack);
            }
        }
        for (int inputSlot : this.recipeInputSlots) {
            if (inputSlot == slot) {
                scheduleInventoryProcess();
                return;
            }
        }
    }

    private void scheduleInventoryProcess() {
        if (this.world != null && !this.world.isRemote) {
            this.pendingInventoryProcess = true;
        }
    }

    private void tryProcess() {
        if (this.processingInventory) {
            return;
        }
        this.processingInventory = true;
        try {
            InventoryLayout layout = buildInventoryLayout(getDefinition());
            for (int inputSlot : layout.inputSlots) {
                if (tryProcessInputSlot(inputSlot, layout.outputSlots)) {
                    return;
                }
            }
        } finally {
            this.processingInventory = false;
        }
    }

    private boolean tryProcessInputSlot(int inputSlot, int[] outputSlots) {
        if (tryProcessFluidInputSlot(inputSlot, outputSlots)) {
            return true;
        }
        if (tryProcessFluidOutputSlot(inputSlot, outputSlots)) {
            return true;
        }
        if (tryProcessGasInputSlot(inputSlot, outputSlots)) {
            return true;
        }
        return tryProcessGasOutputSlot(inputSlot, outputSlots);
    }

    private boolean tryProcessFluidInputSlot(int inputSlot, int[] outputSlots) {
        ItemStack input = this.inventory.getStackInSlot(inputSlot);
        if (input.isEmpty()) {
            return false;
        }

        ItemStack singleInput = input.copy();
        singleInput.setCount(1);
        IFluidHandlerItem handler = FluidUtil.getFluidHandler(singleInput);
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

        this.tank.fill(actuallyDrained.copy(), true);
        return moveOneProcessedContainer(inputSlot, input, outputSlot, emptiedContainer);
    }

    private boolean tryProcessFluidOutputSlot(int inputSlot, int[] outputSlots) {
        ItemStack input = this.inventory.getStackInSlot(inputSlot);
        if (input.isEmpty()) {
            return false;
        }
        ItemStack singleInput = input.copy();
        singleInput.setCount(1);
        FluidActionResult simulated = FluidUtil.tryFillContainer(singleInput.copy(), this.tank, Fluid.BUCKET_VOLUME, null, false);
        if (!simulated.isSuccess()) {
            return false;
        }
        ItemStack filledContainer = simulated.getResult();
        int outputSlot = findOutputSlotFor(filledContainer, outputSlots);
        if (outputSlot < 0) {
            return false;
        }
        FluidActionResult result = FluidUtil.tryFillContainer(singleInput, this.tank, Fluid.BUCKET_VOLUME, null, true);
        if (!result.isSuccess()) {
            return false;
        }
        return moveOneProcessedContainer(inputSlot, input, outputSlot, result.getResult());
    }

    @Optional.Method(modid = "mekanism")
    private boolean tryProcessGasInputSlot(int inputSlot, int[] outputSlots) {
        ItemStack input = this.inventory.getStackInSlot(inputSlot);
        if (input.isEmpty() || !(input.getItem() instanceof IGasItem)) {
            return false;
        }
        ItemStack container = input.copy();
        container.setCount(1);
        GasStack gas = getGasFromItem(container);
        if (gas == null || gas.amount <= 0) {
            return false;
        }
        int accepted = this.gasTank.receiveGas(null, gas.copy(), false);
        if (accepted <= 0) {
            return false;
        }
        GasStack removed = removeGasFromItem(container, accepted, true);
        if (removed == null || removed.amount <= 0) {
            return false;
        }
        int outputSlot = findOutputSlotFor(container, outputSlots);
        if (outputSlot < 0) {
            return false;
        }
        this.gasTank.receiveGas(null, removed.copy(), true);
        return moveOneProcessedContainer(inputSlot, input, outputSlot, container);
    }

    @Optional.Method(modid = "mekanism")
    private boolean tryProcessGasOutputSlot(int inputSlot, int[] outputSlots) {
        ItemStack input = this.inventory.getStackInSlot(inputSlot);
        if (input.isEmpty() || !(input.getItem() instanceof IGasItem)) {
            return false;
        }
        GasStack available = getGasStack();
        if (available == null || available.amount <= 0) {
            return false;
        }
        ItemStack container = input.copy();
        container.setCount(1);
        int accepted = addGasToItem(container, available.copy(), true);
        if (accepted <= 0) {
            return false;
        }
        int outputSlot = findOutputSlotFor(container, outputSlots);
        if (outputSlot < 0) {
            return false;
        }
        GasStack drained = this.gasTank.drawGas(null, accepted, true);
        if (drained == null || drained.amount <= 0) {
            return false;
        }
        return moveOneProcessedContainer(inputSlot, input, outputSlot, container);
    }

    private boolean moveOneProcessedContainer(int inputSlot, ItemStack originalInput, int outputSlot, ItemStack producedContainer) {
        if (originalInput.isEmpty() || producedContainer.isEmpty()) {
            return false;
        }
        ItemStack remainingInput = originalInput.copy();
        remainingInput.shrink(1);
        this.inventory.setStackInSlot(inputSlot, remainingInput.isEmpty() ? ItemStack.EMPTY : remainingInput);
        ItemStack output = this.inventory.getStackInSlot(outputSlot);
        if (output.isEmpty()) {
            this.inventory.setStackInSlot(outputSlot, producedContainer.copy());
        } else {
            ItemStack grown = output.copy();
            grown.grow(producedContainer.getCount());
            this.inventory.setStackInSlot(outputSlot, grown);
        }
        lockOutputSlot(outputSlot, producedContainer);
        markNoUpdateSync();
        return true;
    }

    private boolean tryProcessHeldFluidItem(EntityPlayer player, EnumHand hand, ItemStack held) {
        ItemStack singleHeld = held.copy();
        singleHeld.setCount(1);

        FluidActionResult emptyResult = FluidUtil.tryEmptyContainer(singleHeld, this.tank, Fluid.BUCKET_VOLUME, player, true);
        if (emptyResult.isSuccess()) {
            applyHeldContainerResult(player, hand, held, emptyResult.getResult());
            markNoUpdateSync();
            return true;
        }

        FluidActionResult fillResult = FluidUtil.tryFillContainer(singleHeld, this.tank, Fluid.BUCKET_VOLUME, player, true);
        if (fillResult.isSuccess()) {
            applyHeldContainerResult(player, hand, held, fillResult.getResult());
            markNoUpdateSync();
            return true;
        }
        return false;
    }

    private void applyHeldContainerResult(EntityPlayer player, EnumHand hand, ItemStack originalHeld, ItemStack result) {
        if (originalHeld.getCount() <= 1) {
            player.setHeldItem(hand, result);
            return;
        }

        ItemStack remainingHeld = originalHeld.copy();
        remainingHeld.shrink(1);
        player.setHeldItem(hand, remainingHeld);

        ItemStack remainder = player.inventory.addItemStackToInventory(result.copy()) ? ItemStack.EMPTY : result.copy();
        if (!remainder.isEmpty()) {
            player.dropItem(remainder, false);
        }
    }

    @Optional.Method(modid = "mekanism")
    private boolean tryProcessHeldGasItem(EntityPlayer player, EnumHand hand, ItemStack held) {
        if (!(held.getItem() instanceof IGasItem)) {
            return false;
        }
        GasStack heldGas = getGasFromItem(held);
        if (heldGas != null && heldGas.amount > 0) {
            int accepted = this.gasTank.receiveGas(null, heldGas.copy(), false);
            if (accepted > 0) {
                GasStack removed = removeGasFromItem(held, accepted, true);
                if (removed != null && removed.amount > 0) {
                    this.gasTank.receiveGas(null, removed.copy(), true);
                    player.setHeldItem(hand, held);
                    markNoUpdateSync();
                    return true;
                }
            }
        }

        GasStack storedGas = getGasStack();
        if (storedGas == null || storedGas.amount <= 0) {
            return false;
        }
        int added = addGasToItem(held, storedGas.copy(), true);
        if (added <= 0) {
            return false;
        }
        GasStack drained = this.gasTank.drawGas(null, added, true);
        if (drained == null || drained.amount <= 0) {
            return false;
        }
        player.setHeldItem(hand, held);
        markNoUpdateSync();
        return true;
    }

    private int findOutputSlotFor(ItemStack stack, int[] outputSlots) {
        if (stack.isEmpty()) {
            return outputSlots.length > 0 ? outputSlots[0] : -1;
        }
        if (this.outputSlotLock) {
            return findLockedOutputSlotFor(stack, outputSlots);
        }
        for (int slot : outputSlots) {
            ItemStack output = this.inventory.getStackInSlot(slot);
            if (output.isEmpty()) {
                return slot;
            }
            int slotLimit = Math.min(this.inventory.getSlotLimit(slot), output.getMaxStackSize());
            if (canStacksMerge(output, stack)
                && output.getCount() + stack.getCount() <= slotLimit) {
                return slot;
            }
        }
        return -1;
    }

    private boolean isOutputSlot(int slot) {
        return indexOfOutputSlot(slot) >= 0;
    }

    private int findLockedOutputSlotFor(ItemStack stack, int[] outputSlots) {
        int firstUnboundEmpty = -1;
        for (int i = 0; i < outputSlots.length; i++) {
            int slot = outputSlots[i];
            ItemStack output = this.inventory.getStackInSlot(slot);
            ItemStack template = getOutputSlotTemplate(slot);
            if (!output.isEmpty()) {
                if (!canStacksMerge(output, stack)) {
                    continue;
                }
                int slotLimit = Math.min(this.inventory.getSlotLimit(slot), output.getMaxStackSize());
                if (output.getCount() + stack.getCount() <= slotLimit) {
                    return slot;
                }
                continue;
            }
            if (!template.isEmpty()) {
                if (canStacksRepresentSameItem(template, stack)) {
                    return slot;
                }
                continue;
            }
            if (firstUnboundEmpty < 0) {
                firstUnboundEmpty = slot;
            }
        }
        return firstUnboundEmpty;
    }

    private ItemStack getOutputSlotTemplate(int outputSlot) {
        int index = indexOfOutputSlot(outputSlot);
        return index >= 0 && index < this.outputSlotTemplates.length ? this.outputSlotTemplates[index] : ItemStack.EMPTY;
    }

    private void lockOutputSlot(int outputSlot, ItemStack template) {
        if (!this.outputSlotLock || template.isEmpty()) {
            return;
        }
        int index = indexOfOutputSlot(outputSlot);
        if (index < 0) {
            return;
        }
        resizeOutputSlotTemplates(this.recipeOutputSlots.length);
        if (index >= this.outputSlotTemplates.length || !this.outputSlotTemplates[index].isEmpty()) {
            return;
        }
        ItemStack locked = template.copy();
        locked.setCount(1);
        this.outputSlotTemplates[index] = locked;
    }

    private int indexOfOutputSlot(int outputSlot) {
        for (int i = 0; i < this.recipeOutputSlots.length; i++) {
            if (this.recipeOutputSlots[i] == outputSlot) {
                return i;
            }
        }
        return -1;
    }

    private void resizeOutputSlotTemplates(int size) {
        int length = Math.max(0, size);
        if (this.outputSlotTemplates.length == length) {
            return;
        }
        ItemStack[] next = new ItemStack[length];
        Arrays.fill(next, ItemStack.EMPTY);
        int copy = Math.min(this.outputSlotTemplates.length, next.length);
        for (int i = 0; i < copy; i++) {
            next[i] = this.outputSlotTemplates[i].isEmpty() ? ItemStack.EMPTY : this.outputSlotTemplates[i].copy();
        }
        this.outputSlotTemplates = next;
    }

    private static boolean canStacksMerge(ItemStack existing, ItemStack incoming) {
        return !existing.isEmpty()
            && !incoming.isEmpty()
            && existing.isStackable()
            && incoming.isStackable()
            && existing.isItemEqual(incoming)
            && ItemStack.areItemStackTagsEqual(existing, incoming);
    }

    private static boolean canStacksRepresentSameItem(ItemStack existing, ItemStack incoming) {
        return !existing.isEmpty()
            && !incoming.isEmpty()
            && existing.isItemEqual(incoming)
            && ItemStack.areItemStackTagsEqual(existing, incoming);
    }

    private void writeOutputSlotTemplates(NBTTagCompound compound) {
        resizeOutputSlotTemplates(this.recipeOutputSlots.length);
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < this.outputSlotTemplates.length; i++) {
            ItemStack template = this.outputSlotTemplates[i];
            if (template.isEmpty()) {
                continue;
            }
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("index", i);
            if (i < this.recipeOutputSlots.length) {
                tag.setInteger("slot", this.recipeOutputSlots[i]);
            }
            ItemStack copy = template.copy();
            copy.setCount(1);
            copy.writeToNBT(tag);
            list.appendTag(tag);
        }
        compound.setTag("outputSlotTemplates", list);
    }

    private void readOutputSlotTemplates(NBTTagCompound compound) {
        resizeOutputSlotTemplates(this.recipeOutputSlots.length);
        Arrays.fill(this.outputSlotTemplates, ItemStack.EMPTY);
        if (!compound.hasKey("outputSlotTemplates", Constants.NBT.TAG_LIST)) {
            return;
        }
        NBTTagList list = compound.getTagList("outputSlotTemplates", Constants.NBT.TAG_COMPOUND);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound tag = list.getCompoundTagAt(i);
            int index = tag.hasKey("slot") ? indexOfOutputSlot(tag.getInteger("slot")) : tag.getInteger("index");
            if (index < 0 || index >= this.outputSlotTemplates.length) {
                continue;
            }
            ItemStack template = new ItemStack(tag);
            if (!template.isEmpty()) {
                template.setCount(1);
                this.outputSlotTemplates[index] = template;
            }
        }
    }

    @Nullable
    @Optional.Method(modid = "mekanism")
    private GasStack getGasFromItem(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof IGasItem)) {
            return null;
        }
        GasStack gas = ((IGasItem) stack.getItem()).getGas(stack);
        return gas == null ? null : gas.copy();
    }

    @Nullable
    @Optional.Method(modid = "mekanism")
    private GasStack removeGasFromItem(ItemStack stack, int amount, boolean doTransfer) {
        if (stack.isEmpty() || !(stack.getItem() instanceof IGasItem) || amount <= 0) {
            return null;
        }
        IGasItem item = (IGasItem) stack.getItem();
        GasStack current = item.getGas(stack);
        if (current == null || current.amount <= 0 || !item.canProvideGas(stack, current.getGas())) {
            return null;
        }
        int removed = Math.min(amount, current.amount);
        GasStack result = current.copy();
        result.amount = removed;
        if (doTransfer) {
            int remaining = current.amount - removed;
            item.setGas(stack, remaining <= 0 ? null : current.copy().withAmount(remaining));
        }
        return result;
    }

    @Optional.Method(modid = "mekanism")
    private int addGasToItem(ItemStack stack, GasStack gas, boolean doTransfer) {
        if (stack.isEmpty() || !(stack.getItem() instanceof IGasItem) || gas == null || gas.amount <= 0) {
            return 0;
        }
        IGasItem item = (IGasItem) stack.getItem();
        if (!item.canReceiveGas(stack, gas.getGas())) {
            return 0;
        }
        GasStack current = item.getGas(stack);
        int currentAmount = current == null ? 0 : current.amount;
        if (current != null && current.amount > 0 && current.getGas() != gas.getGas()) {
            return 0;
        }
        int space = Math.max(0, item.getMaxGas(stack) - currentAmount);
        int added = Math.min(space, gas.amount);
        if (added <= 0) {
            return 0;
        }
        if (doTransfer) {
            item.setGas(stack, gas.copy().withAmount(currentAmount + added));
        }
        return added;
    }

    private void syncDefinition() {
        if (this.syncingDefinition) {
            return;
        }
        this.syncingDefinition = true;
        try {
            CustomHatchRegistry.CustomHatchDef def = getDefinition();
            if (def != null) {
                applyDefinition(def);
            }
        } finally {
            this.syncingDefinition = false;
        }
    }

    private void applyDefinition(CustomHatchRegistry.CustomHatchDef def) {
        this.capacity = Math.max(1L, def.capacity);
        this.fluidCapacity = Math.max(1L, def.fluidCapacity > 0L ? def.fluidCapacity : this.capacity);
        this.gasCapacity = Math.max(1L, def.gasCapacity > 0L ? def.gasCapacity : this.capacity);
        this.energyCapacity = Math.max(1L, def.energyCapacity > 0L ? def.energyCapacity : this.capacity);
        this.energyTransfer = Math.max(1L, def.energyTransfer > 0L ? def.energyTransfer : this.energyCapacity);
        this.energy = clampEnergy(this.energy);
        this.outputSlotLock = def.outputSlotLock;
        this.tank.setCustomCapacity(this.fluidCapacity);
        this.gasTank.setCapacity(this.gasCapacity);
        syncInventoryLayout(def);
    }

    private void syncInventoryLayout(CustomHatchRegistry.CustomHatchDef def) {
        InventoryLayout layout = buildInventoryLayout(def);
        String signature = layout.signature();
        if (signature.equals(this.inventorySignature) && this.inventory.getSlots() == layout.slotCount) {
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
        resizeOutputSlotTemplates(layout.outputSlots.length);
        this.inventorySignature = signature;
    }

    private void ensureInventoryLayoutMatchesDefinition() {
        CustomHatchRegistry.CustomHatchDef def = getDefinition();
        if (def == null) {
            return;
        }
        this.inventorySignature = "";
        syncInventoryLayout(def);
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
            if (!isRuntimeSlotRole(component.role)) {
                continue;
            }
            int index;
            if (component.index >= 0) {
                if (component.index > MAX_DYNAMIC_SLOT_INDEX) {
                    continue;
                }
                index = component.index;
                nextIndex = Math.max(nextIndex, index + 1);
            } else {
                if (nextIndex > MAX_DYNAMIC_SLOT_INDEX) {
                    continue;
                }
                index = nextIndex++;
            }
            maxIndex = Math.max(maxIndex, index);
            if ("output".equalsIgnoreCase(component.role)) {
                outputs.add(Integer.valueOf(index));
            } else {
                inputs.add(Integer.valueOf(index));
            }
        }
        if (maxIndex < 0 && !hasGuiSlotComponents(def)) {
            return new InventoryLayout(new int[]{INPUT_SLOT}, new int[]{OUTPUT_SLOT}, 2);
        }
        if (maxIndex < 0) {
            return new InventoryLayout(new int[0], new int[0], 0);
        }
        return new InventoryLayout(toSortedUniqueIntArray(inputs), toSortedUniqueIntArray(outputs), maxIndex + 1);
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

    private static boolean isRuntimeSlotRole(@Nullable String role) {
        return "input".equalsIgnoreCase(role) || "output".equalsIgnoreCase(role);
    }

    private static int[] toSortedUniqueIntArray(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return new int[0];
        }
        Set<Integer> unique = new LinkedHashSet<Integer>(values);
        int[] out = new int[unique.size()];
        int i = 0;
        for (Integer value : unique) {
            out[i++] = value == null ? 0 : value.intValue();
        }
        Arrays.sort(out);
        return out;
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
        return new CombinedMachineComponent(ioType, groupId);
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
            return new ItemMachineComponent(ioType, groupId);
        }
        if ("fluid".equalsIgnoreCase(componentType)) {
            return new FluidMachineComponent(ioType, groupId);
        }
        if ("gas".equalsIgnoreCase(componentType)) {
            return new GasMachineComponent(ioType, groupId);
        }
        if ("energy".equalsIgnoreCase(componentType)
            || "power".equalsIgnoreCase(componentType)
            || "fe".equalsIgnoreCase(componentType)
            || "rf".equalsIgnoreCase(componentType)) {
            return new EnergyMachineComponent(ioType, groupId);
        }
        return null;
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        String id = CustomIdValidator.readSanitizedString(compound, "hatchId");
        this.hatchId = id == null ? "" : id;
        this.capacity = compound.hasKey("capacity") ? clampCapacity(compound.getLong("capacity")) : this.capacity;
        this.fluidCapacity = compound.hasKey("fluidCapacity") ? clampCapacity(compound.getLong("fluidCapacity")) : this.capacity;
        this.gasCapacity = compound.hasKey("gasCapacity") ? clampCapacity(compound.getLong("gasCapacity")) : this.capacity;
        this.energyCapacity = compound.hasKey("energyCapacity") ? clampEnergyCapacity(compound.getLong("energyCapacity")) : this.energyCapacity;
        this.energyTransfer = compound.hasKey("energyTransfer") ? clampEnergyCapacity(compound.getLong("energyTransfer")) : this.energyTransfer;
        syncDefinition();
        if (compound.hasKey("inv", Constants.NBT.TAG_COMPOUND)) {
            this.inventory.readNBT(compound.getCompoundTag("inv"));
            this.inventory.setListener(this::onInventoryChanged);
            ensureInventoryLayoutMatchesDefinition();
        }
        readOutputSlotTemplates(compound);
        if (compound.hasKey("fluid", Constants.NBT.TAG_COMPOUND)) {
            this.tank.readFromNBT(compound.getCompoundTag("fluid"));
            clampStoredFluid();
        }
        if (compound.hasKey("gas", Constants.NBT.TAG_COMPOUND)) {
            this.gasTank.readFromNBT(compound, "gas");
            clampStoredGas();
        }
        this.energy = compound.hasKey("energy") ? clampEnergy(compound.getLong("energy")) : clampEnergy(this.energy);
    }

    @Override
    public void writeCustomNBT(NBTTagCompound compound) {
        super.writeCustomNBT(compound);
        compound.setString("hatchId", this.hatchId == null ? "" : this.hatchId);
        compound.setTag("inv", this.inventory.writeNBT());
        compound.setTag("fluid", this.tank.writeToNBT(new NBTTagCompound()));
        this.gasTank.writeToNBT(compound, "gas");
        compound.setLong("capacity", this.capacity);
        compound.setLong("fluidCapacity", this.fluidCapacity);
        compound.setLong("gasCapacity", this.gasCapacity);
        compound.setLong("energy", this.energy);
        compound.setLong("energyCapacity", this.energyCapacity);
        compound.setLong("energyTransfer", this.energyTransfer);
        writeOutputSlotTemplates(compound);
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY && supportsExternalCapability(ExternalAccessKind.ITEM)
            || capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY && supportsExternalCapability(ExternalAccessKind.FLUID)
            || capability == CapabilityEnergy.ENERGY && supportsExternalCapability(ExternalAccessKind.ENERGY)
            || capability == LongEnergyCapability.LONG_ENERGY && supportsExternalCapability(ExternalAccessKind.ENERGY)
            || isGregTechEnergyCapability(capability)
            || isDraconicOPCapability(capability)
            || isMekanismGasCapability(capability) && supportsExternalCapability(ExternalAccessKind.GAS)
            || super.hasCapability(capability, facing);
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        if (capability == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY) {
            return supportsExternalCapability(ExternalAccessKind.ITEM) ? (T) this.inventory : super.getCapability(capability, facing);
        }
        if (capability == CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY) {
            return supportsExternalCapability(ExternalAccessKind.FLUID) ? (T) getExternalFluidCapability() : super.getCapability(capability, facing);
        }
        if (capability == CapabilityEnergy.ENERGY) {
            return supportsExternalCapability(ExternalAccessKind.ENERGY) ? (T) this.energyHandler : super.getCapability(capability, facing);
        }
        if (capability == LongEnergyCapability.LONG_ENERGY) {
            return supportsExternalCapability(ExternalAccessKind.ENERGY) ? (T) this.energyHandler : super.getCapability(capability, facing);
        }
        if (isGregTechEnergyCapability(capability)) {
            return (T) this.gtEnergyContainer;
        }
        if (isDraconicOPCapability(capability)) {
            return (T) this.opStorageHandler;
        }
        if (isMekanismGasHandlerCapability(capability)) {
            return supportsExternalCapability(ExternalAccessKind.GAS) ? (T) getExternalGasCapability() : super.getCapability(capability, facing);
        }
        if (isMekanismTubeConnectionCapability(capability)) {
            return supportsExternalCapability(ExternalAccessKind.GAS) ? (T) this : super.getCapability(capability, facing);
        }
        return super.getCapability(capability, facing);
    }

    private boolean isGregTechEnergyCapability(@Nonnull Capability<?> capability) {
        return Mods.GREGTECH.isPresent()
            && supportsExternalCapability(ExternalAccessKind.ENERGY)
            && capability == getGTEnergyCapability();
    }

    @Optional.Method(modid = "gregtech")
    private static Capability<?> getGTEnergyCapability() {
        return GregtechCapabilities.CAPABILITY_ENERGY_CONTAINER;
    }

    private boolean isDraconicOPCapability(@Nonnull Capability<?> capability) {
        Capability<?> opCapability = getOPCapability();
        return opCapability != null
            && this.opStorageHandler != null
            && supportsExternalCapability(ExternalAccessKind.ENERGY)
            && capability == opCapability;
    }

    @Nullable
    private static Capability<?> getOPCapability() {
        return OptionalOPBridge.OP_CAPABILITY;
    }

    private boolean supportsExternalCapability(ExternalAccessKind kind) {
        DirectionAccess access = resolveExternalAccess(kind);
        return access.input || access.output;
    }

    private IFluidHandler getExternalFluidCapability() {
        DirectionAccess access = resolveExternalAccess(ExternalAccessKind.FLUID);
        if (access.input && access.output) {
            return this.bidirectionalFluidCapability;
        }
        return access.output ? this.outputFluidCapability : this.inputFluidCapability;
    }

    private IExtendedGasHandler getExternalGasCapability() {
        DirectionAccess access = resolveExternalAccess(ExternalAccessKind.GAS);
        if (access.input && access.output) {
            return this.bidirectionalGasCapability;
        }
        return access.output ? this.outputGasCapability : this.inputGasCapability;
    }

    private DirectionAccess resolveExternalAccess(ExternalAccessKind kind) {
        CustomHatchRegistry.CustomHatchDef def = getDefinition();
        boolean input = false;
        boolean output = false;
        boolean matchedComponent = false;
        if (def != null && def.machineComponents != null && !def.machineComponents.isEmpty()) {
            for (CustomHatchRegistry.MachineComponentDef component : def.machineComponents) {
                if (component == null) {
                    continue;
                }
                if (!kind.matches(component.type)) {
                    continue;
                }
                matchedComponent = true;
                if (parseIOType(component.io) == IOType.OUTPUT) {
                    output = true;
                } else {
                    input = true;
                }
            }
        }
        if (!matchedComponent) {
            String rootComponentType = def == null || def.componentType == null ? "fluid" : def.componentType;
            if (!kind.matches(rootComponentType)) {
                return new DirectionAccess(false, false);
            }
            if (parseIOType(def == null ? null : def.ioType) == IOType.OUTPUT) {
                output = true;
            } else {
                input = true;
            }
        }
        return new DirectionAccess(input, output);
    }

    private static boolean isMekanismGasCapability(@Nullable Capability<?> capability) {
        return isMekanismGasHandlerCapability(capability) || isMekanismTubeConnectionCapability(capability);
    }

    private static boolean isMekanismGasHandlerCapability(@Nullable Capability<?> capability) {
        if (capability == null) {
            return false;
        }
        return IGasHandler.class.getName().equals(capability.getName());
    }

    private static boolean isMekanismTubeConnectionCapability(@Nullable Capability<?> capability) {
        if (capability == null) {
            return false;
        }
        return ITubeConnection.class.getName().equals(capability.getName());
    }

    @Override
    @Optional.Method(modid = "mekanism")
    public double acceptEnergy(EnumFacing side, double maxReceive, boolean simulate) {
        if (maxReceive <= 0D) {
            return 0D;
        }
        long requested = maxReceive >= (double) Long.MAX_VALUE ? Long.MAX_VALUE : Math.max(0L, (long) Math.floor(maxReceive));
        return (double) com.fushu.mmceguiext.common.util.EnergyAccessHelper.receive(this, requested, simulate);
    }

    @Override
    @Optional.Method(modid = "mekanism")
    public boolean canReceiveEnergy(EnumFacing side) {
        return resolveExternalAccess(ExternalAccessKind.ENERGY).input;
    }

    @Override
    @Optional.Method(modid = "mekanism")
    public double pullEnergy(EnumFacing side, double maxExtract, boolean simulate) {
        if (maxExtract <= 0D) {
            return 0D;
        }
        long requested = maxExtract >= (double) Long.MAX_VALUE ? Long.MAX_VALUE : Math.max(0L, (long) Math.floor(maxExtract));
        return (double) com.fushu.mmceguiext.common.util.EnergyAccessHelper.extract(this, requested, simulate);
    }

    @Override
    @Optional.Method(modid = "mekanism")
    public boolean canOutputEnergy(EnumFacing side) {
        return resolveExternalAccess(ExternalAccessKind.ENERGY).output;
    }

    @Override
    @Optional.Method(modid = "mekanism")
    public boolean canTubeConnect(EnumFacing side) {
        DirectionAccess access = resolveExternalAccess(ExternalAccessKind.GAS);
        return access.input || access.output;
    }

    @Override
    @Optional.Method(modid = "mekanism")
    public int receiveGas(EnumFacing side, GasStack stack, boolean doTransfer) {
        return resolveExternalAccess(ExternalAccessKind.GAS).input ? this.gasTank.receiveGas(side, stack, doTransfer) : 0;
    }

    @Override
    @Optional.Method(modid = "mekanism")
    public GasStack drawGas(EnumFacing side, int amount, boolean doTransfer) {
        return resolveExternalAccess(ExternalAccessKind.GAS).output ? this.gasTank.drawGas(side, amount, doTransfer) : null;
    }

    @Override
    @Optional.Method(modid = "mekanism")
    public boolean canReceiveGas(EnumFacing side, Gas type) {
        return resolveExternalAccess(ExternalAccessKind.GAS).input && this.gasTank.canReceiveGas(side, type);
    }

    @Override
    @Optional.Method(modid = "mekanism")
    public boolean canDrawGas(EnumFacing side, Gas type) {
        return resolveExternalAccess(ExternalAccessKind.GAS).output && this.gasTank.canDrawGas(side, type);
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

    private class ProcessorTank extends FluidTank implements LongFluidIOHandler {
        private long longCapacity = 1000L;
        private long longAmount = 0L;

        public ProcessorTank(long capacity) {
            super(downcastAmount(capacity));
            setCustomCapacity(capacity);
        }

        public long getFluidAmountLong() {
            return this.longAmount;
        }

        public long getCapacityLong() {
            return this.longCapacity;
        }

        public void setCustomCapacity(long capacity) {
            this.longCapacity = clampCapacity(capacity);
            this.longAmount = Math.min(this.longAmount, this.longCapacity);
            syncLegacyView();
        }

        @Override
        @Nullable
        public FluidStack getFluid() {
            return this.fluid == null ? null : this.fluid.copy();
        }

        @Override
        public void setFluid(@Nullable FluidStack fluid) {
            if (fluid == null || fluid.amount <= 0) {
                this.fluid = null;
                this.longAmount = 0L;
                return;
            }
            this.longAmount = Math.min(Math.max(0L, (long) fluid.amount), this.longCapacity);
            this.fluid = fluid.copy();
            syncLegacyView();
        }

        @Override
        public int getFluidAmount() {
            return downcastAmount(this.longAmount);
        }

        @Override
        public int getCapacity() {
            return downcastAmount(this.longCapacity);
        }

        @Override
        public void setCapacity(int capacity) {
            setCustomCapacity(capacity);
        }

        @Override
        public IFluidTankProperties[] getTankProperties() {
            return new IFluidTankProperties[]{new LongFluidTankProperties()};
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            if (!canFillFluidType(resource)) {
                return 0;
            }
            return fillInternal(resource, doFill);
        }

        @Override
        public int fillInternal(FluidStack resource, boolean doFill) {
            if (resource == null || resource.amount <= 0) {
                return 0;
            }
            if (this.fluid != null && !this.fluid.isFluidEqual(resource)) {
                return 0;
            }
            long acceptedLong = Math.min((long) resource.amount, this.longCapacity - this.longAmount);
            int accepted = downcastAmount(acceptedLong);
            if (accepted <= 0) {
                return 0;
            }
            if (doFill) {
                if (this.fluid == null) {
                    this.fluid = resource.copy();
                }
                this.longAmount += accepted;
                syncLegacyView();
                onContentsChanged();
            }
            return accepted;
        }

        @Override
        public long mmceguiext$simulateFluidIO(FluidStack stack, long maxAmount, IOType actionType) {
            return doLongFluidIO(stack, maxAmount, actionType, false);
        }

        @Override
        public long mmceguiext$doFluidIO(FluidStack stack, long maxAmount, IOType actionType) {
            return doLongFluidIO(stack, maxAmount, actionType, true);
        }

        private long doLongFluidIO(FluidStack stack, long maxAmount, IOType actionType, boolean doTransfer) {
            if (stack == null || maxAmount <= 0L) {
                return 0L;
            }
            if (actionType == IOType.INPUT) {
                if (this.fluid == null || this.longAmount <= 0L || !this.fluid.isFluidEqual(stack)) {
                    return 0L;
                }
                long moved = Math.min(maxAmount, this.longAmount);
                if (doTransfer && moved > 0L) {
                    this.longAmount -= moved;
                    syncLegacyView();
                    onContentsChanged();
                }
                return moved;
            }
            if (this.fluid != null && !this.fluid.isFluidEqual(stack)) {
                return 0L;
            }
            long moved = Math.min(maxAmount, this.longCapacity - this.longAmount);
            if (doTransfer && moved > 0L) {
                if (this.fluid == null) {
                    this.fluid = stack.copy();
                }
                this.longAmount += moved;
                syncLegacyView();
                onContentsChanged();
            }
            return moved;
        }

        @Override
        @Nullable
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            if (!canDrainFluidType(getFluid())) {
                return null;
            }
            return drainInternal(resource, doDrain);
        }

        @Override
        @Nullable
        public FluidStack drain(int maxDrain, boolean doDrain) {
            if (!canDrainFluidType(this.fluid)) {
                return null;
            }
            return drainInternal(maxDrain, doDrain);
        }

        @Override
        @Nullable
        public FluidStack drainInternal(FluidStack resource, boolean doDrain) {
            if (resource == null || !resource.isFluidEqual(getFluid())) {
                return null;
            }
            return drainInternal(resource.amount, doDrain);
        }

        @Override
        @Nullable
        public FluidStack drainInternal(int maxDrain, boolean doDrain) {
            if (this.fluid == null || maxDrain <= 0 || this.longAmount <= 0L) {
                return null;
            }
            int drained = downcastAmount(Math.min((long) maxDrain, this.longAmount));
            if (drained <= 0) {
                return null;
            }
            FluidStack stack = this.fluid.copy();
            stack.amount = drained;
            if (doDrain) {
                this.longAmount -= drained;
                syncLegacyView();
                onContentsChanged();
            }
            return stack;
        }

        @Override
        public FluidTank readFromNBT(NBTTagCompound nbt) {
            if (nbt == null || nbt.hasKey("Empty")) {
                this.fluid = null;
                this.longAmount = 0L;
                return this;
            }
            FluidStack fluid = FluidStack.loadFluidStackFromNBT(nbt);
            if (fluid == null || fluid.amount <= 0) {
                this.fluid = null;
                this.longAmount = 0L;
                return this;
            }
            this.longAmount = Math.min(Math.max(0L, nbt.hasKey(NBT_LONG_AMOUNT) ? nbt.getLong(NBT_LONG_AMOUNT) : (long) fluid.amount), this.longCapacity);
            this.fluid = fluid.copy();
            syncLegacyView();
            return this;
        }

        @Override
        public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
            if (nbt == null) {
                nbt = new NBTTagCompound();
            }
            if (this.fluid != null && this.longAmount > 0L) {
                FluidStack copy = this.fluid.copy();
                copy.amount = downcastAmount(this.longAmount);
                copy.writeToNBT(nbt);
                nbt.setLong(NBT_LONG_AMOUNT, this.longAmount);
            } else {
                nbt.setString("Empty", "");
            }
            return nbt;
        }

        private void syncLegacyView() {
            this.capacity = downcastAmount(this.longCapacity);
            if (this.fluid == null || this.longAmount <= 0L) {
                this.fluid = null;
                this.longAmount = 0L;
                return;
            }
            this.fluid.amount = downcastAmount(this.longAmount);
        }

        @Override
        protected void onContentsChanged() {
            scheduleInventoryProcess();
            markForUpdateSync();
        }

        private class LongFluidTankProperties implements IFluidTankProperties {
            @Nullable
            @Override
            public FluidStack getContents() {
                return ProcessorTank.this.getFluid();
            }

            @Override
            public int getCapacity() {
                return ProcessorTank.this.getCapacity();
            }

            @Override
            public boolean canFill() {
                return ProcessorTank.this.canFill();
            }

            @Override
            public boolean canDrain() {
                return ProcessorTank.this.canDrain();
            }

            @Override
            public boolean canFillFluidType(FluidStack fluidStack) {
                return ProcessorTank.this.canFillFluidType(fluidStack);
            }

            @Override
            public boolean canDrainFluidType(FluidStack fluidStack) {
                return ProcessorTank.this.canDrainFluidType(fluidStack);
            }
        }
    }

    private class ProcessorGasTank implements IExtendedGasHandler, LongGasIOHandler {
        private final GasStack[] contents;
        private final long[] amounts;
        private final GasTankInfo[] props;
        private long capacity;

        public ProcessorGasTank(long capacity, int tankCount) {
            int tanks = Math.max(1, tankCount);
            this.contents = new GasStack[tanks];
            this.amounts = new long[tanks];
            this.props = new GasTankInfo[tanks];
            for (int i = 0; i < this.props.length; i++) {
                this.props[i] = new GasTankInfoImpl(i);
            }
            setCapacity(capacity);
        }

        public long getCapacityLong() {
            return this.capacity;
        }

        public long getGasAmountLong() {
            return this.amounts.length == 0 ? 0L : this.amounts[0];
        }

        public ProcessorGasTank setCapacity(final long capacity) {
            this.capacity = clampCapacity(capacity);
            for (int i = 0; i < this.contents.length; i++) {
                if (this.amounts[i] > this.capacity) {
                    this.amounts[i] = this.capacity;
                    syncSlot(i);
                }
            }
            return this;
        }

        @Nonnull
        @Override
        public GasTankInfo[] getTankInfo() {
            return this.props;
        }

        @Override
        @Optional.Method(modid = "mekanism")
        public synchronized int receiveGas(EnumFacing side, GasStack gasStack, boolean doFill) {
            if (gasStack == null || gasStack.amount <= 0) {
                return 0;
            }
            GasStack insert = gasStack.copy();
            int totalFillAmount = 0;
            for (int i = 0; i < this.contents.length; i++) {
                int filled = fill(i, insert, doFill);
                totalFillAmount += filled;
                if (insert.amount <= filled) {
                    break;
                }
                insert.amount -= filled;
            }
            return totalFillAmount;
        }

        @Optional.Method(modid = "mekanism")
        public int fill(final int slot, final GasStack insert, final boolean doFill) {
            if (slot < 0 || slot >= this.contents.length || insert == null || insert.amount <= 0) {
                return 0;
            }
            GasStack content = this.contents[slot];
            if (content != null && !content.isGasEqual(insert)) {
                return 0;
            }
            long acceptedLong = Math.min((long) insert.amount, this.capacity - this.amounts[slot]);
            int accepted = downcastAmount(acceptedLong);
            if (accepted <= 0) {
                return 0;
            }
            if (doFill) {
                if (content == null) {
                    this.contents[slot] = insert.copy();
                }
                this.amounts[slot] += accepted;
                syncSlot(iClamp(slot));
                onSlotChanged(slot);
            }
            return accepted;
        }

        @Override
        @Optional.Method(modid = "mekanism")
        public long mmceguiext$simulateGasIO(GasStack stack, long maxAmount, IOType actionType) {
            return doLongGasIO(stack, maxAmount, actionType, false);
        }

        @Override
        @Optional.Method(modid = "mekanism")
        public long mmceguiext$doGasIO(GasStack stack, long maxAmount, IOType actionType) {
            return doLongGasIO(stack, maxAmount, actionType, true);
        }

        @Optional.Method(modid = "mekanism")
        private long doLongGasIO(GasStack stack, long maxAmount, IOType actionType, boolean doTransfer) {
            if (stack == null || stack.amount <= 0 || maxAmount <= 0L) {
                return 0L;
            }
            long moved = 0L;
            if (actionType == IOType.INPUT) {
                for (int i = 0; i < this.contents.length && moved < maxAmount; i++) {
                    GasStack content = this.contents[i];
                    if (content == null || this.amounts[i] <= 0L || !content.isGasEqual(stack)) {
                        continue;
                    }
                    long slotMoved = Math.min(maxAmount - moved, this.amounts[i]);
                    if (doTransfer && slotMoved > 0L) {
                        this.amounts[i] -= slotMoved;
                        syncSlot(i);
                        onSlotChanged(i);
                    }
                    moved += slotMoved;
                }
                return moved;
            }
            for (int i = 0; i < this.contents.length && moved < maxAmount; i++) {
                GasStack content = this.contents[i];
                if (content != null && !content.isGasEqual(stack)) {
                    continue;
                }
                long slotMoved = Math.min(maxAmount - moved, this.capacity - this.amounts[i]);
                if (slotMoved <= 0L) {
                    continue;
                }
                if (doTransfer) {
                    if (content == null) {
                        this.contents[i] = stack.copy();
                    }
                    this.amounts[i] += slotMoved;
                    syncSlot(i);
                    onSlotChanged(i);
                }
                moved += slotMoved;
            }
            return moved;
        }

        @Nullable
        @Override
        @Optional.Method(modid = "mekanism")
        public synchronized GasStack drawGas(final GasStack resource, final boolean doDrain) {
            if (resource == null || resource.amount <= 0) {
                return null;
            }
            GasStack res = resource.copy();
            int totalDrainAmount = 0;
            for (int i = 0; i < this.contents.length; i++) {
                GasStack content = this.contents[i];
                if (content == null || !content.isGasEqual(res)) {
                    continue;
                }
                GasStack drainedStack = drain(i, res.amount, doDrain);
                if (drainedStack == null || drainedStack.amount <= 0) {
                    continue;
                }
                int drained = drainedStack.amount;
                totalDrainAmount += drained;
                if (drained >= res.amount) {
                    break;
                }
                res.amount -= drained;
            }
            if (totalDrainAmount <= 0) {
                return null;
            }
            GasStack drained = resource.copy();
            drained.amount = totalDrainAmount;
            return drained;
        }

        @Nullable
        @Optional.Method(modid = "mekanism")
        public GasStack drain(final int slot, final int maxDrain, final boolean doDrain) {
            if (slot < 0 || slot >= this.contents.length || maxDrain <= 0) {
                return null;
            }
            GasStack content = this.contents[slot];
            if (content == null || this.amounts[slot] <= 0L) {
                return null;
            }
            int drained = downcastAmount(Math.min((long) maxDrain, this.amounts[slot]));
            if (drained <= 0) {
                return null;
            }
            GasStack copied = content.copy();
            copied.amount = drained;
            if (doDrain) {
                this.amounts[slot] -= drained;
                syncSlot(slot);
                onSlotChanged(slot);
            }
            return copied;
        }

        @Nullable
        @Override
        @Optional.Method(modid = "mekanism")
        public synchronized GasStack drawGas(final EnumFacing side, final int maxDrain, final boolean doDrain) {
            if (maxDrain <= 0) {
                return null;
            }
            for (int i = 0; i < this.contents.length; i++) {
                GasStack content = this.contents[i];
                if (content == null || this.amounts[i] <= 0L) {
                    continue;
                }
                GasStack toDrain = content.copy();
                toDrain.amount = maxDrain;
                return drawGas(toDrain, doDrain);
            }
            return null;
        }

        @Nullable
        @Optional.Method(modid = "mekanism")
        public GasStack getGasInSlot(final int slot) {
            return slot >= 0 && slot < this.contents.length && this.contents[slot] != null ? this.contents[slot].copy() : null;
        }

        @Optional.Method(modid = "mekanism")
        public void setGasInSlot(final int slot, final GasStack stack) {
            if (slot < 0 || slot >= this.contents.length) {
                return;
            }
            if (stack == null || stack.amount <= 0) {
                this.contents[slot] = null;
                this.amounts[slot] = 0L;
            } else {
                this.contents[slot] = stack.copy();
                this.amounts[slot] = Math.min(Math.max(0L, (long) stack.amount), this.capacity);
                syncSlot(slot);
            }
            onSlotChanged(slot);
        }

        @Override
        @Optional.Method(modid = "mekanism")
        public boolean canReceiveGas(final EnumFacing side, final Gas type) {
            return type != null && receiveGas(null, new GasStack(type, 1), false) > 0;
        }

        @Override
        @Optional.Method(modid = "mekanism")
        public boolean canDrawGas(final EnumFacing side, final Gas type) {
            if (type == null) {
                return false;
            }
            GasStack drawn = drawGas(new GasStack(type, 1), false);
            return drawn != null && drawn.amount > 0;
        }

        @Optional.Method(modid = "mekanism")
        public void readFromNBT(final NBTTagCompound compound, final String name) {
            Arrays.fill(this.contents, null);
            Arrays.fill(this.amounts, 0L);
            NBTTagCompound tag = compound.getCompoundTag(name);
            if (tag.isEmpty()) {
                return;
            }
            for (int i = 0; i < this.contents.length; i++) {
                NBTTagCompound t = tag.getCompoundTag("#" + i);
                if (t.isEmpty()) {
                    continue;
                }
                GasStack gas = GasStack.readFromNBT(t);
                if (gas == null || gas.amount <= 0) {
                    continue;
                }
                this.contents[i] = gas.copy();
                this.amounts[i] = Math.min(Math.max(0L, t.hasKey(NBT_LONG_AMOUNT) ? t.getLong(NBT_LONG_AMOUNT) : (long) gas.amount), this.capacity);
                syncSlot(i);
            }
        }

        @Optional.Method(modid = "mekanism")
        public void writeToNBT(final NBTTagCompound compound, final String name) {
            NBTTagCompound tag = new NBTTagCompound();
            for (int i = 0; i < this.contents.length; i++) {
                GasStack stack = this.contents[i];
                if (stack == null || this.amounts[i] <= 0L) {
                    continue;
                }
                NBTTagCompound t = new NBTTagCompound();
                GasStack copy = stack.copy();
                copy.amount = downcastAmount(this.amounts[i]);
                copy.write(t);
                t.setLong(NBT_LONG_AMOUNT, this.amounts[i]);
                tag.setTag("#" + i, t);
            }
            compound.setTag(name, tag);
        }

        private int iClamp(int slot) {
            return Math.max(0, Math.min(slot, this.contents.length - 1));
        }

        private void syncSlot(final int slot) {
            if (slot < 0 || slot >= this.contents.length) {
                return;
            }
            GasStack content = this.contents[slot];
            if (content == null || this.amounts[slot] <= 0L) {
                this.contents[slot] = null;
                this.amounts[slot] = 0L;
                return;
            }
            content.amount = downcastAmount(this.amounts[slot]);
        }

        private void onSlotChanged(final int slot) {
            scheduleInventoryProcess();
            markForUpdateSync();
        }

        private class GasTankInfoImpl implements GasTankInfo {
            private final int index;

            private GasTankInfoImpl(final int index) {
                this.index = index;
            }

            @Nullable
            @Override
            public GasStack getGas() {
                return ProcessorGasTank.this.getGasInSlot(this.index);
            }

            @Override
            public int getStored() {
                return this.index >= 0 && this.index < ProcessorGasTank.this.amounts.length
                    ? downcastAmount(ProcessorGasTank.this.amounts[this.index])
                    : 0;
            }

            @Override
            public int getMaxGas() {
                return downcastAmount(ProcessorGasTank.this.capacity);
            }
        }
    }

    private static final class DirectionAccess {
        private final boolean input;
        private final boolean output;

        private DirectionAccess(boolean input, boolean output) {
            this.input = input;
            this.output = output;
        }
    }

    private enum ExternalAccessKind {
        ITEM {
            @Override
            boolean matches(@Nullable String componentType) {
                return "item".equalsIgnoreCase(componentType)
                    || "item_fluid".equalsIgnoreCase(componentType)
                    || "item_fluid_gas".equalsIgnoreCase(componentType)
                    || "mixed".equalsIgnoreCase(componentType)
                    || "hybrid".equalsIgnoreCase(componentType);
            }
        },
        FLUID {
            @Override
            boolean matches(@Nullable String componentType) {
                return "fluid".equalsIgnoreCase(componentType) || isSharedFluidComponent(componentType);
            }
        },
        GAS {
            @Override
            boolean matches(@Nullable String componentType) {
                return "gas".equalsIgnoreCase(componentType)
                    || "item_fluid_gas".equalsIgnoreCase(componentType)
                    || "mixed".equalsIgnoreCase(componentType)
                    || "hybrid".equalsIgnoreCase(componentType);
            }
        },
        ENERGY {
            @Override
            boolean matches(@Nullable String componentType) {
                return "energy".equalsIgnoreCase(componentType)
                    || "power".equalsIgnoreCase(componentType)
                    || "fe".equalsIgnoreCase(componentType)
                    || "rf".equalsIgnoreCase(componentType);
            }
        };

        abstract boolean matches(@Nullable String componentType);

        static boolean isSharedFluidComponent(@Nullable String componentType) {
            return "item_fluid".equalsIgnoreCase(componentType)
                || "item_fluid_gas".equalsIgnoreCase(componentType)
                || "mixed".equalsIgnoreCase(componentType)
                || "hybrid".equalsIgnoreCase(componentType);
        }
    }

    private class ExternalFluidHandler implements IFluidHandler {
        private final boolean allowFill;
        private final boolean allowDrain;

        private ExternalFluidHandler(boolean allowFill, boolean allowDrain) {
            this.allowFill = allowFill;
            this.allowDrain = allowDrain;
        }

        @Override
        public IFluidTankProperties[] getTankProperties() {
            return tank.getTankProperties();
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            return this.allowFill ? tank.fill(resource, doFill) : 0;
        }

        @Nullable
        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            return this.allowDrain ? tank.drain(resource, doDrain) : null;
        }

        @Nullable
        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) {
            return this.allowDrain ? tank.drain(maxDrain, doDrain) : null;
        }
    }

    private class ExternalGasHandler implements IExtendedGasHandler {
        private final boolean allowReceive;
        private final boolean allowDraw;

        private ExternalGasHandler(boolean allowReceive, boolean allowDraw) {
            this.allowReceive = allowReceive;
            this.allowDraw = allowDraw;
        }

        @Override
        @Optional.Method(modid = "mekanism")
        public int receiveGas(@Nullable EnumFacing side, GasStack stack, boolean doTransfer) {
            return this.allowReceive ? gasTank.receiveGas(side, stack, doTransfer) : 0;
        }

        @Override
        @Optional.Method(modid = "mekanism")
        public GasStack drawGas(GasStack toDraw, boolean doTransfer) {
            return this.allowDraw ? gasTank.drawGas(toDraw, doTransfer) : null;
        }

        @Override
        @Optional.Method(modid = "mekanism")
        public GasStack drawGas(@Nullable EnumFacing side, int amount, boolean doTransfer) {
            return this.allowDraw ? gasTank.drawGas(side, amount, doTransfer) : null;
        }

        @Override
        @Optional.Method(modid = "mekanism")
        public boolean canReceiveGas(@Nullable EnumFacing side, Gas type) {
            return this.allowReceive && gasTank.canReceiveGas(side, type);
        }

        @Override
        @Optional.Method(modid = "mekanism")
        public boolean canDrawGas(@Nullable EnumFacing side, Gas type) {
            return this.allowDraw && gasTank.canDrawGas(side, type);
        }

        @Nonnull
        @Override
        @Optional.Method(modid = "mekanism")
        public GasTankInfo[] getTankInfo() {
            return gasTank.getTankInfo();
        }
    }

    private long clampEnergy(long value) {
        return Math.max(0L, Math.min(value, this.energyCapacity));
    }

    private static long clampCapacity(long value) {
        return Math.max(1L, Math.min(value, MAX_HATCH_CAPACITY));
    }

    private static long clampEnergyCapacity(long value) {
        return Math.max(1L, Math.min(value, MAX_ENERGY_CAPACITY));
    }

    private void clampStoredFluid() {
        this.tank.setCustomCapacity(this.fluidCapacity);
    }

    @Optional.Method(modid = "mekanism")
    private void clampStoredGas() {
        this.gasTank.setCapacity(this.gasCapacity);
    }

    private int downcastEnergy(long value) {
        return value >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, value);
    }

    private static long firstLongArg(@Nullable Object[] args) {
        if (args == null || args.length == 0 || !(args[0] instanceof Number)) {
            return 0L;
        }
        return ((Number) args[0]).longValue();
    }

    private static boolean secondBooleanArg(@Nullable Object[] args) {
        return args != null && args.length > 1 && args[1] instanceof Boolean && (Boolean) args[1];
    }

    @Nullable
    private Object coerceEnergyReturn(long value, Class<?> returnType) {
        if (returnType == Long.TYPE || returnType == Long.class) {
            return value;
        }
        if (returnType == Integer.TYPE || returnType == Integer.class) {
            return downcastEnergy(value);
        }
        if (returnType == Boolean.TYPE || returnType == Boolean.class) {
            return value != 0L;
        }
        return null;
    }

    @Nullable
    private static Object defaultReturnValue(Class<?> returnType) {
        if (returnType == Void.TYPE) {
            return null;
        }
        if (returnType == Boolean.TYPE) {
            return false;
        }
        if (returnType == Byte.TYPE) {
            return (byte) 0;
        }
        if (returnType == Short.TYPE) {
            return (short) 0;
        }
        if (returnType == Integer.TYPE) {
            return 0;
        }
        if (returnType == Long.TYPE) {
            return 0L;
        }
        if (returnType == Float.TYPE) {
            return 0.0F;
        }
        if (returnType == Double.TYPE) {
            return 0.0D;
        }
        if (returnType == Character.TYPE) {
            return (char) 0;
        }
        return null;
    }

    private static double fillRatio(long amount, long capacity) {
        if (capacity <= 0L || amount <= 0L) {
            return 0.0D;
        }
        if (amount >= capacity) {
            return 1.0D;
        }
        return (double) amount / (double) capacity;
    }

    private static int downcastAmount(long value) {
        return value >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, value);
    }

    private static long gtEuToFe(long value) {
        if (value <= 0L) {
            return 0L;
        }
        if (value > Long.MAX_VALUE / GT_ENERGY_MULTIPLIER) {
            return Long.MAX_VALUE;
        }
        return value * GT_ENERGY_MULTIPLIER;
    }

    private static long gtFeToEu(long value) {
        return value <= 0L ? 0L : value / GT_ENERGY_MULTIPLIER;
    }

    private static long saturatedMultiply(long left, long right) {
        if (left <= 0L || right <= 0L) {
            return 0L;
        }
        if (left > Long.MAX_VALUE / right) {
            return Long.MAX_VALUE;
        }
        return left * right;
    }

    private static long saturatedAdd(long left, long right) {
        if (right > 0L && left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        if (right < 0L && left < Long.MIN_VALUE - right) {
            return Long.MIN_VALUE;
        }
        return left + right;
    }

    @Optional.Interface(iface = "gregtech.api.capability.IEnergyContainer", modid = "gregtech")
    private class GTEnergyContainer implements IEnergyContainer {
        @Override
        @Optional.Method(modid = "gregtech")
        public synchronized long acceptEnergyFromNetwork(EnumFacing side, long voltage, long amperage) {
            if (!inputsEnergy(side) || voltage <= 0L || amperage <= 0L) {
                return 0L;
            }
            long spaceEu = gtFeToEu(Math.max(0L, energyCapacity - energy));
            long transferEu = getTransferEu();
            long maxReceiveEu = Math.min(spaceEu, transferEu);
            if (maxReceiveEu < voltage) {
                return 0L;
            }
            long acceptedAmperage = Math.min(amperage, maxReceiveEu / voltage);
            if (acceptedAmperage <= 0L) {
                return 0L;
            }
            long acceptedEu = saturatedMultiply(acceptedAmperage, voltage);
            long acceptedFe = Math.min(gtEuToFe(acceptedEu), energyCapacity - energy);
            if (acceptedFe <= 0L) {
                return 0L;
            }
            energy += acceptedFe;
            markNoUpdateSync();
            return acceptedAmperage;
        }

        @Override
        @Optional.Method(modid = "gregtech")
        public boolean inputsEnergy(EnumFacing side) {
            return resolveExternalAccess(ExternalAccessKind.ENERGY).input;
        }

        @Override
        @Optional.Method(modid = "gregtech")
        public boolean outputsEnergy(EnumFacing side) {
            return resolveExternalAccess(ExternalAccessKind.ENERGY).output;
        }

        @Override
        @Optional.Method(modid = "gregtech")
        public synchronized long changeEnergy(long differenceAmount) {
            if (differenceAmount == 0L) {
                return 0L;
            }
            if (differenceAmount > 0L) {
                return addEnergy(differenceAmount);
            }
            long requested = differenceAmount == Long.MIN_VALUE ? Long.MAX_VALUE : -differenceAmount;
            return -removeEnergy(requested);
        }

        @Override
        @Optional.Method(modid = "gregtech")
        public synchronized long addEnergy(long energyToAdd) {
            if (!inputsEnergy(null) || energyToAdd <= 0L) {
                return 0L;
            }
            long acceptedEu = Math.min(energyToAdd, gtFeToEu(Math.max(0L, energyCapacity - energy)));
            long acceptedFe = Math.min(gtEuToFe(acceptedEu), energyCapacity - energy);
            if (acceptedFe <= 0L) {
                return 0L;
            }
            energy += acceptedFe;
            markNoUpdateSync();
            return acceptedEu;
        }

        @Override
        @Optional.Method(modid = "gregtech")
        public synchronized long removeEnergy(long energyToRemove) {
            if (!outputsEnergy(null) || energyToRemove <= 0L) {
                return 0L;
            }
            long removedEu = Math.min(energyToRemove, gtFeToEu(energy));
            long removedFe = Math.min(gtEuToFe(removedEu), energy);
            if (removedFe <= 0L) {
                return 0L;
            }
            energy -= removedFe;
            markNoUpdateSync();
            return removedEu;
        }

        @Override
        @Optional.Method(modid = "gregtech")
        public synchronized long getEnergyStored() {
            return gtFeToEu(energy);
        }

        @Override
        @Optional.Method(modid = "gregtech")
        public synchronized long getEnergyCapacity() {
            return gtFeToEu(energyCapacity);
        }

        @Override
        @Optional.Method(modid = "gregtech")
        public long getOutputAmperage() {
            return outputsEnergy(null) ? 1L : 0L;
        }

        @Override
        @Optional.Method(modid = "gregtech")
        public long getOutputVoltage() {
            return outputsEnergy(null) ? getTransferEu() : 0L;
        }

        @Override
        @Optional.Method(modid = "gregtech")
        public long getInputAmperage() {
            return inputsEnergy(null) ? 1L : 0L;
        }

        @Override
        @Optional.Method(modid = "gregtech")
        public long getInputVoltage() {
            return inputsEnergy(null) ? getTransferEu() : 0L;
        }

        @Optional.Method(modid = "gregtech")
        private long getTransferEu() {
            return Math.max(1L, gtFeToEu(energyTransfer));
        }
    }

    @Nullable
    private Object createOPStorageHandler() {
        Class<?> storageClass = OptionalOPBridge.OP_STORAGE_CLASS;
        if (storageClass == null || OptionalOPBridge.OP_CAPABILITY == null) {
            return null;
        }
        return Proxy.newProxyInstance(
            storageClass.getClassLoader(),
            new Class<?>[]{storageClass},
            new OPStorageInvocationHandler()
        );
    }

    private synchronized long receiveOP(long maxReceive, boolean simulate) {
        if (maxReceive <= 0L || !resolveExternalAccess(ExternalAccessKind.ENERGY).input) {
            return 0L;
        }
        long accepted = Math.min(Math.min(maxReceive, energyTransfer), energyCapacity - energy);
        if (accepted <= 0L) {
            return 0L;
        }
        if (!simulate) {
            energy += accepted;
            markNoUpdateSync();
        }
        return accepted;
    }

    private synchronized long extractOP(long maxExtract, boolean simulate) {
        if (maxExtract <= 0L || !resolveExternalAccess(ExternalAccessKind.ENERGY).output) {
            return 0L;
        }
        long extracted = Math.min(Math.min(maxExtract, energyTransfer), energy);
        if (extracted <= 0L) {
            return 0L;
        }
        if (!simulate) {
            energy -= extracted;
            markNoUpdateSync();
        }
        return extracted;
    }

    private synchronized long modifyEnergyStored(long amount) {
        long before = energy;
        energy = clampEnergy(saturatedAdd(energy, amount));
        long changed = energy - before;
        if (changed != 0L) {
            markNoUpdateSync();
        }
        return changed;
    }

    /**
     * Runtime bridge for newer BrandonsCore / Draconic Evolution OP APIs. The 1.12.2 jars used by
     * this project do not ship IOPStorage or CapabilityOP, so this must stay reflection-only.
     */
    private class OPStorageInvocationHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, @Nullable Object[] args) {
            String name = method.getName();
            if (method.getDeclaringClass() == Object.class) {
                return invokeObjectMethod(proxy, method, args);
            }
            if ("getOPStored".equals(name) || "getEnergyStored".equals(name)) {
                return coerceEnergyReturn(energy, method.getReturnType());
            }
            if ("getMaxOPStored".equals(name) || "getMaxEnergyStored".equals(name)) {
                return coerceEnergyReturn(energyCapacity, method.getReturnType());
            }
            if ("receiveOP".equals(name) || "receiveEnergy".equals(name)) {
                long received = receiveOP(firstLongArg(args), secondBooleanArg(args));
                return coerceEnergyReturn(received, method.getReturnType());
            }
            if ("extractOP".equals(name) || "extractEnergy".equals(name)) {
                long extracted = extractOP(firstLongArg(args), secondBooleanArg(args));
                return coerceEnergyReturn(extracted, method.getReturnType());
            }
            if ("modifyEnergyStored".equals(name)) {
                long changed = modifyEnergyStored(firstLongArg(args));
                return coerceEnergyReturn(changed, method.getReturnType());
            }
            if ("canExtract".equals(name)) {
                return resolveExternalAccess(ExternalAccessKind.ENERGY).output;
            }
            if ("canReceive".equals(name)) {
                return resolveExternalAccess(ExternalAccessKind.ENERGY).input;
            }
            return defaultReturnValue(method.getReturnType());
        }

        private Object invokeObjectMethod(Object proxy, Method method, @Nullable Object[] args) {
            if ("toString".equals(method.getName())) {
                return "TileCustomHatch.OPStorage";
            }
            if ("hashCode".equals(method.getName())) {
                return System.identityHashCode(proxy);
            }
            if ("equals".equals(method.getName())) {
                return args != null && args.length == 1 && proxy == args[0];
            }
            return null;
        }
    }

    private class EnergyHandler implements IEnergyStorage, IEnergyHandlerAsync, ILongEnergyStorage {
        @Override
        public synchronized int receiveEnergy(int maxReceive, boolean simulate) {
            if (maxReceive <= 0 || !canReceive()) {
                return 0;
            }
            long accepted = Math.min(Math.min((long) maxReceive, energyTransfer), energyCapacity - energy);
            if (accepted <= 0L) {
                return 0;
            }
            if (!simulate) {
                energy += accepted;
                markNoUpdateSync();
            }
            return downcastEnergy(accepted);
        }

        @Override
        public synchronized int extractEnergy(int maxExtract, boolean simulate) {
            if (maxExtract <= 0 || !canExtract()) {
                return 0;
            }
            long extracted = Math.min(Math.min((long) maxExtract, energyTransfer), energy);
            if (extracted <= 0L) {
                return 0;
            }
            if (!simulate) {
                energy -= extracted;
                markNoUpdateSync();
            }
            return downcastEnergy(extracted);
        }

        @Override
        public synchronized int getEnergyStored() {
            return downcastEnergy(energy);
        }

        @Override
        public synchronized int getMaxEnergyStored() {
            return downcastEnergy(energyCapacity);
        }

        @Override
        public boolean canExtract() {
            return resolveExternalAccess(ExternalAccessKind.ENERGY).output;
        }

        @Override
        public boolean canReceive() {
            return resolveExternalAccess(ExternalAccessKind.ENERGY).input;
        }

        @Override
        public synchronized long getCurrentEnergy() {
            return energy;
        }

        @Override
        public synchronized void setCurrentEnergy(long value) {
            energy = clampEnergy(value);
            markNoUpdateSync();
        }

        @Override
        public synchronized long getMaxEnergy() {
            return energyCapacity;
        }

        @Override
        public synchronized long receiveEnergyLong(long maxReceive, boolean simulate) {
            if (maxReceive <= 0L || !canReceiveLong()) {
                return 0L;
            }
            long accepted = Math.min(Math.min(maxReceive, energyTransfer), energyCapacity - energy);
            if (accepted <= 0L) {
                return 0L;
            }
            if (!simulate) {
                energy += accepted;
                markNoUpdateSync();
            }
            return accepted;
        }

        @Override
        public synchronized long extractEnergyLong(long maxExtract, boolean simulate) {
            if (maxExtract <= 0L || !canExtractLong()) {
                return 0L;
            }
            long extracted = Math.min(Math.min(maxExtract, energyTransfer), energy);
            if (extracted <= 0L) {
                return 0L;
            }
            if (!simulate) {
                energy -= extracted;
                markNoUpdateSync();
            }
            return extracted;
        }

        @Override
        public synchronized long getEnergyStoredLong() {
            return energy;
        }

        @Override
        public synchronized long getMaxEnergyStoredLong() {
            return energyCapacity;
        }

        @Override
        public boolean canExtractLong() {
            return canExtract();
        }

        @Override
        public boolean canReceiveLong() {
            return canReceive();
        }

        @Override
        public synchronized boolean extractEnergy(long value) {
            if (value < 0L || energy < value) {
                return false;
            }
            energy -= value;
            markNoUpdateSync();
            return true;
        }

        @Override
        public synchronized boolean receiveEnergy(long value) {
            if (value < 0L || energyCapacity - energy < value) {
                return false;
            }
            energy += value;
            markNoUpdateSync();
            return true;
        }
    }

    private class CombinedMachineComponent extends MachineComponent<InfItemFluidHandler> {
        private final long groupId;

        private CombinedMachineComponent(IOType ioType, long groupId) {
            super(ioType);
            this.groupId = groupId;
        }

        @Override
        public ComponentType getComponentType() {
            return ComponentTypesMM.COMPONENT_ITEM_FLUID_GAS;
        }

        @Override
        public InfItemFluidHandler getContainerProvider() {
            int[] slots = this.ioType == IOType.OUTPUT ? TileCustomHatch.this.recipeOutputSlots : TileCustomHatch.this.recipeInputSlots;
            return new CombinedHatchHandler(new FilteredItemHandlerView(TileCustomHatch.this.inventory, slots), TileCustomHatch.this.tank, TileCustomHatch.this.gasTank);
        }

        @Override
        public long getGroupID() {
            return this.groupId;
        }
    }

    private class ItemMachineComponent extends MachineComponent.ItemBus {
        private final long groupId;

        private ItemMachineComponent(IOType ioType, long groupId) {
            super(ioType);
            this.groupId = groupId;
        }

        @Override
        public net.minecraftforge.items.IItemHandlerModifiable getContainerProvider() {
            return new FilteredItemHandlerView(TileCustomHatch.this.inventory, this.ioType == IOType.OUTPUT ? TileCustomHatch.this.recipeOutputSlots : TileCustomHatch.this.recipeInputSlots);
        }

        @Override
        public long getGroupID() {
            return this.groupId;
        }
    }

    private class FluidMachineComponent extends MachineComponent.FluidHatch {
        private final long groupId;

        private FluidMachineComponent(IOType ioType, long groupId) {
            super(ioType);
            this.groupId = groupId;
        }

        @Override
        public FluidTank getContainerProvider() {
            return TileCustomHatch.this.tank;
        }

        @Override
        public long getGroupID() {
            return this.groupId;
        }
    }

    private class GasMachineComponent extends MachineComponent<IExtendedGasHandler> {
        private final long groupId;

        private GasMachineComponent(IOType ioType, long groupId) {
            super(ioType);
            this.groupId = groupId;
        }

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
            return this.groupId;
        }
    }

    private class EnergyMachineComponent extends MachineComponent.EnergyHatch {
        private final long groupId;

        private EnergyMachineComponent(IOType ioType, long groupId) {
            super(ioType);
            this.groupId = groupId;
        }

        @Override
        public IEnergyHandlerAsync getContainerProvider() {
            return TileCustomHatch.this.energyHandler;
        }

        @Override
        public long getGroupID() {
            return this.groupId;
        }
    }

    private static class CombinedHatchHandler extends InfItemFluidHandler implements LongFluidIOHandler, LongGasIOHandler {
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

        @Override
        public long mmceguiext$simulateFluidIO(FluidStack stack, long maxAmount, IOType actionType) {
            if (this.fluidHandler instanceof LongFluidIOHandler) {
                return ((LongFluidIOHandler) this.fluidHandler).mmceguiext$simulateFluidIO(stack, maxAmount, actionType);
            }
            return fallbackFluidIO(stack, maxAmount, actionType, false);
        }

        @Override
        public long mmceguiext$doFluidIO(FluidStack stack, long maxAmount, IOType actionType) {
            if (this.fluidHandler instanceof LongFluidIOHandler) {
                return ((LongFluidIOHandler) this.fluidHandler).mmceguiext$doFluidIO(stack, maxAmount, actionType);
            }
            return fallbackFluidIO(stack, maxAmount, actionType, true);
        }

        private long fallbackFluidIO(FluidStack stack, long maxAmount, IOType actionType, boolean doTransfer) {
            if (stack == null || maxAmount <= 0L) {
                return 0L;
            }
            FluidStack copy = stack.copy();
            copy.amount = downcastAmount(maxAmount);
            if (actionType == IOType.INPUT) {
                FluidStack drained = this.fluidHandler.drain(copy, doTransfer);
                return drained == null ? 0L : Math.max(0L, drained.amount);
            }
            return Math.max(0, this.fluidHandler.fill(copy, doTransfer));
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
        public long mmceguiext$simulateGasIO(GasStack stack, long maxAmount, IOType actionType) {
            if (this.gasHandler instanceof LongGasIOHandler) {
                return ((LongGasIOHandler) this.gasHandler).mmceguiext$simulateGasIO(stack, maxAmount, actionType);
            }
            return fallbackGasIO(stack, maxAmount, actionType, false);
        }

        @Override
        @Optional.Method(modid = "mekanism")
        public long mmceguiext$doGasIO(GasStack stack, long maxAmount, IOType actionType) {
            if (this.gasHandler instanceof LongGasIOHandler) {
                return ((LongGasIOHandler) this.gasHandler).mmceguiext$doGasIO(stack, maxAmount, actionType);
            }
            return fallbackGasIO(stack, maxAmount, actionType, true);
        }

        @Optional.Method(modid = "mekanism")
        private long fallbackGasIO(GasStack stack, long maxAmount, IOType actionType, boolean doTransfer) {
            if (stack == null || maxAmount <= 0L) {
                return 0L;
            }
            GasStack copy = stack.copy();
            copy.amount = downcastAmount(maxAmount);
            if (actionType == IOType.INPUT) {
                GasStack drawn = this.gasHandler.drawGas(copy, doTransfer);
                return drawn == null ? 0L : Math.max(0L, drawn.amount);
            }
            return Math.max(0, this.gasHandler.receiveGas(null, copy, doTransfer));
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

    private static class OptionalOPBridge {
        private static final String[] OP_STORAGE_CLASS_NAMES = new String[]{
            "com.brandon3055.brandonscore.api.power.IOPStorage",
            "com.brandon3055.draconicevolution.api.power.IOPStorage",
            "com.brandon3055.draconicevolution.api.energy.IOPStorage"
        };
        private static final String[] OP_CAPABILITY_CLASS_NAMES = new String[]{
            "com.brandon3055.brandonscore.api.power.CapabilityOP",
            "com.brandon3055.draconicevolution.api.power.CapabilityOP",
            "com.brandon3055.draconicevolution.api.energy.CapabilityOP"
        };
        private static final String[] OP_CAPABILITY_FIELD_NAMES = new String[]{"OP", "OP_STORAGE", "ENERGY"};

        @Nullable
        private static final Class<?> OP_STORAGE_CLASS = findOPStorageClass();
        @Nullable
        private static final Capability<?> OP_CAPABILITY = findOPCapability();

        @Nullable
        private static Class<?> findOPStorageClass() {
            for (String className : OP_STORAGE_CLASS_NAMES) {
                Class<?> storageClass = findClass(className);
                if (storageClass != null && storageClass.isInterface()) {
                    return storageClass;
                }
            }
            return null;
        }

        @Nullable
        private static Capability<?> findOPCapability() {
            for (String className : OP_CAPABILITY_CLASS_NAMES) {
                Class<?> capabilityClass = findClass(className);
                Capability<?> capability = findNamedCapability(capabilityClass);
                if (capability != null) {
                    return capability;
                }
                capability = findFirstCapability(capabilityClass);
                if (capability != null) {
                    return capability;
                }
            }
            return null;
        }

        @Nullable
        private static Capability<?> findNamedCapability(@Nullable Class<?> capabilityClass) {
            if (capabilityClass == null) {
                return null;
            }
            for (String fieldName : OP_CAPABILITY_FIELD_NAMES) {
                try {
                    Field field = capabilityClass.getField(fieldName);
                    Object value = field.get(null);
                    if (value instanceof Capability) {
                        return (Capability<?>) value;
                    }
                } catch (ReflectiveOperationException | LinkageError ignored) {
                    // Continue probing known optional API shapes.
                }
            }
            return null;
        }

        @Nullable
        private static Capability<?> findFirstCapability(@Nullable Class<?> capabilityClass) {
            if (capabilityClass == null) {
                return null;
            }
            try {
                for (Field field : capabilityClass.getFields()) {
                    Object value = field.get(null);
                    if (value instanceof Capability) {
                        return (Capability<?>) value;
                    }
                }
            } catch (ReflectiveOperationException | LinkageError ignored) {
                return null;
            }
            return null;
        }

        @Nullable
        private static Class<?> findClass(String className) {
            try {
                return Class.forName(className);
            } catch (ClassNotFoundException | LinkageError ignored) {
                return null;
            }
        }
    }
}
