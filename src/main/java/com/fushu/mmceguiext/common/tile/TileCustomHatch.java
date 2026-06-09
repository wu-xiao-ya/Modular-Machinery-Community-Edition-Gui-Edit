package com.fushu.mmceguiext.common.tile;

import com.fushu.mmceguiext.common.registry.CustomHatchRegistry;
import com.fushu.mmceguiext.common.util.CustomIdValidator;
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
import mekanism.api.gas.IGasItem;
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
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.Fluid;
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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Optional.InterfaceList({
    @Optional.Interface(modid = "mekanism", iface = "mekanism.api.gas.IGasHandler"),
    @Optional.Interface(modid = "mekanism", iface = "mekanism.api.gas.ITubeConnection")
})
public class TileCustomHatch extends TileEntityRestrictedTick implements MachineComponentTile, github.kasuminova.mmce.common.tile.base.MachineCombinationComponent, SelectiveUpdateTileEntity, ReadWriteLockProvider, IGasHandler, ITubeConnection {
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    private static final int MAX_DYNAMIC_SLOT_INDEX = 4095;

    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final long componentGroupId = github.kasuminova.mmce.common.tile.base.MachineCombinationComponent.GROUP_ACQUIRER.incrementAndGet();
    private IOInventory inventory = new IOInventory(this, new int[]{INPUT_SLOT}, new int[]{OUTPUT_SLOT});
    private int[] recipeInputSlots = new int[]{INPUT_SLOT};
    private int[] recipeOutputSlots = new int[]{OUTPUT_SLOT};
    private boolean processingInventory = false;
    private boolean pendingInventoryProcess = false;

    private int capacity = 1000;
    private int fluidCapacity = 1000;
    private int gasCapacity = 1000;
    private final ProcessorTank tank = new ProcessorTank(this.fluidCapacity);
    private final ProcessorGasTank gasTank = new ProcessorGasTank(this.gasCapacity, 1);
    private final ExternalFluidHandler inputFluidCapability = new ExternalFluidHandler(true, false);
    private final ExternalFluidHandler outputFluidCapability = new ExternalFluidHandler(false, true);
    private final ExternalFluidHandler bidirectionalFluidCapability = new ExternalFluidHandler(true, true);
    private final ExternalGasHandler inputGasCapability = new ExternalGasHandler(true, false);
    private final ExternalGasHandler outputGasCapability = new ExternalGasHandler(false, true);
    private final ExternalGasHandler bidirectionalGasCapability = new ExternalGasHandler(true, true);
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

    private static boolean canStacksMerge(ItemStack existing, ItemStack incoming) {
        return !existing.isEmpty()
            && !incoming.isEmpty()
            && existing.isStackable()
            && incoming.isStackable()
            && existing.isItemEqual(incoming)
            && ItemStack.areItemStackTagsEqual(existing, incoming);
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
        return null;
    }

    @Override
    public void readCustomNBT(NBTTagCompound compound) {
        super.readCustomNBT(compound);
        String id = CustomIdValidator.readSanitizedString(compound, "hatchId");
        this.hatchId = id == null ? "" : id;
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
            return (T) getExternalFluidCapability();
        }
        if (isMekanismGasHandlerCapability(capability)) {
            return (T) getExternalGasCapability();
        }
        if (isMekanismTubeConnectionCapability(capability)) {
            return (T) this;
        }
        return super.getCapability(capability, facing);
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
            scheduleInventoryProcess();
            markForUpdateSync();
        }
    }

    private class ProcessorGasTank extends MultiGasTank {
        public ProcessorGasTank(int capacity, int tankCount) {
            super(capacity, tankCount);
            setOnSlotChanged(slot -> {
                scheduleInventoryProcess();
                markForUpdateSync();
            });
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
