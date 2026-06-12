package com.fushu.mmceguiext.common.requirement;

import github.kasuminova.mmce.common.util.IExtendedGasHandler;
import github.kasuminova.mmce.common.util.MultiFluidTank;
import github.kasuminova.mmce.common.util.MultiGasTank;
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.GasTankInfo;
import mekanism.api.gas.IGasHandler;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidTankProperties;
import net.minecraftforge.fml.common.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public final class LongRequirementIO {
    private LongRequirementIO() {
    }

    public static long simulateFluid(FluidStack stack, List<IFluidHandler> handlers, long maxAmount, IOType actionType) {
        if (stack == null || handlers == null || maxAmount <= 0L) {
            return 0L;
        }
        long total = 0L;
        for (IFluidHandler handler : handlers) {
            if (handler == null) {
                continue;
            }
            long remaining = maxAmount - total;
            if (remaining <= 0L) {
                break;
            }
            if (handler instanceof LongFluidIOHandler) {
                total += ((LongFluidIOHandler) handler).mmceguiext$simulateFluidIO(stack, remaining, actionType);
            } else {
                FluidStack copy = stack.copy();
                copy.amount = downcastAmount(remaining);
                if (actionType == IOType.INPUT) {
                    FluidStack drained = handler.drain(copy, false);
                    total += drained == null ? 0L : Math.max(0L, drained.amount);
                } else {
                    total += Math.max(0, handler.fill(copy, false));
                }
            }
        }
        return total;
    }

    public static void doFluid(FluidStack stack, List<IFluidHandler> handlers, long maxAmount, IOType actionType) {
        if (stack == null || handlers == null || maxAmount <= 0L) {
            return;
        }
        long remaining = maxAmount;
        for (IFluidHandler handler : handlers) {
            if (handler == null || remaining <= 0L) {
                continue;
            }
            long moved;
            if (handler instanceof LongFluidIOHandler) {
                moved = ((LongFluidIOHandler) handler).mmceguiext$doFluidIO(stack, remaining, actionType);
            } else {
                FluidStack copy = stack.copy();
                copy.amount = downcastAmount(remaining);
                if (actionType == IOType.INPUT) {
                    FluidStack drained = handler.drain(copy, true);
                    moved = drained == null ? 0L : Math.max(0L, drained.amount);
                } else {
                    moved = Math.max(0, handler.fill(copy, true));
                }
            }
            remaining -= Math.max(0L, moved);
        }
    }

    public static long simulateGas(GasStack stack, List<IExtendedGasHandler> handlers, long maxAmount, IOType actionType) {
        if (stack == null || handlers == null || maxAmount <= 0L) {
            return 0L;
        }
        long total = 0L;
        for (IExtendedGasHandler handler : handlers) {
            if (handler == null) {
                continue;
            }
            long remaining = maxAmount - total;
            if (remaining <= 0L) {
                break;
            }
            if (handler instanceof LongGasIOHandler) {
                total += ((LongGasIOHandler) handler).mmceguiext$simulateGasIO(stack, remaining, actionType);
            } else {
                GasStack copy = stack.copy();
                copy.amount = downcastAmount(remaining);
                if (actionType == IOType.INPUT) {
                    GasStack drawn = handler.drawGas(copy, false);
                    total += drawn == null ? 0L : Math.max(0L, drawn.amount);
                } else if (handler.canReceiveGas(null, copy.getGas())) {
                    total += Math.max(0, handler.receiveGas(null, copy, false));
                }
            }
        }
        return total;
    }

    public static void doGas(GasStack stack, List<IExtendedGasHandler> handlers, long maxAmount, IOType actionType) {
        if (stack == null || handlers == null || maxAmount <= 0L) {
            return;
        }
        long remaining = maxAmount;
        for (IExtendedGasHandler handler : handlers) {
            if (handler == null || remaining <= 0L) {
                continue;
            }
            long moved = 0L;
            if (handler instanceof LongGasIOHandler) {
                moved = ((LongGasIOHandler) handler).mmceguiext$doGasIO(stack, remaining, actionType);
            } else {
                GasStack copy = stack.copy();
                copy.amount = downcastAmount(remaining);
                if (actionType == IOType.INPUT) {
                    GasStack drawn = handler.drawGas(copy, true);
                    moved = drawn == null ? 0L : Math.max(0L, drawn.amount);
                } else if (handler.canReceiveGas(null, copy.getGas())) {
                    moved = Math.max(0, handler.receiveGas(null, copy, true));
                }
            }
            remaining -= Math.max(0L, moved);
        }
    }

    public static boolean hasLongFluidHandler(List<ProcessingComponent<?>> components) {
        if (components == null) {
            return false;
        }
        for (ProcessingComponent<?> component : components) {
            Object provided = component == null ? null : component.getProvidedComponent();
            if (provided instanceof LongFluidIOHandler) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static List<ProcessingComponent<?>> copyFluidComponents(List<ProcessingComponent<?>> components) {
        List<ProcessingComponent<?>> out = new ArrayList<ProcessingComponent<?>>();
        if (components == null) {
            return out;
        }
        for (ProcessingComponent<?> component : components) {
            if (component == null) {
                continue;
            }
            Object provided = component.getProvidedComponent();
            if (provided instanceof LongFluidIOHandler && provided instanceof IFluidHandler) {
                out.add(new ProcessingComponent((MachineComponent) component.component(), new SnapshotFluidHandler((IFluidHandler) provided, (LongFluidIOHandler) provided), component.getTag()));
            } else if (provided instanceof IFluidHandler) {
                out.add(new ProcessingComponent((MachineComponent) component.component(), new MultiFluidTank((IFluidHandler) provided), component.getTag()));
            }
        }
        return out;
    }

    public static boolean hasLongGasHandler(List<ProcessingComponent<?>> components) {
        if (components == null) {
            return false;
        }
        for (ProcessingComponent<?> component : components) {
            Object provided = component == null ? null : component.getProvidedComponent();
            if (provided instanceof LongGasIOHandler) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Optional.Method(modid = "mekanism")
    public static List<ProcessingComponent<?>> copyGasComponents(List<ProcessingComponent<?>> components) {
        List<ProcessingComponent<?>> out = new ArrayList<ProcessingComponent<?>>();
        if (components == null) {
            return out;
        }
        for (ProcessingComponent<?> component : components) {
            if (component == null) {
                continue;
            }
            Object provided = component.getProvidedComponent();
            if (provided instanceof LongGasIOHandler && provided instanceof IExtendedGasHandler) {
                out.add(new ProcessingComponent((MachineComponent) component.component(), new SnapshotGasHandler((IExtendedGasHandler) provided, (LongGasIOHandler) provided), component.getTag()));
            } else if (provided instanceof IGasHandler) {
                out.add(new ProcessingComponent((MachineComponent) component.component(), new MultiGasTank((IGasHandler) provided), component.getTag()));
            }
        }
        return out;
    }

    private static int downcastAmount(long value) {
        return value >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, value);
    }

    private static final class SnapshotFluidHandler implements IFluidHandler, LongFluidIOHandler {
        private final LongFluidIOHandler source;
        @Nullable
        private FluidStack fluid;
        private long amount;
        private long capacity = -1L;

        private SnapshotFluidHandler(IFluidHandler handler, LongFluidIOHandler source) {
            this.source = source;
            IFluidTankProperties[] properties = handler.getTankProperties();
            if (properties != null && properties.length > 0 && properties[0] != null) {
                FluidStack contents = properties[0].getContents();
                if (contents != null && contents.amount > 0) {
                    this.fluid = contents.copy();
                    this.amount = source.mmceguiext$simulateFluidIO(contents, Long.MAX_VALUE, IOType.INPUT);
                    this.capacity = this.amount + source.mmceguiext$simulateFluidIO(contents, Long.MAX_VALUE - this.amount, IOType.OUTPUT);
                }
            }
        }

        @Override
        public IFluidTankProperties[] getTankProperties() {
            return new IFluidTankProperties[]{new SnapshotFluidTankProperties()};
        }

        @Override
        public int fill(FluidStack resource, boolean doFill) {
            return downcastAmount(doFluid(resource, resource == null ? 0L : resource.amount, IOType.OUTPUT, doFill));
        }

        @Nullable
        @Override
        public FluidStack drain(FluidStack resource, boolean doDrain) {
            if (resource == null) {
                return null;
            }
            long moved = doFluid(resource, resource.amount, IOType.INPUT, doDrain);
            if (moved <= 0L) {
                return null;
            }
            FluidStack out = resource.copy();
            out.amount = downcastAmount(moved);
            return out;
        }

        @Nullable
        @Override
        public FluidStack drain(int maxDrain, boolean doDrain) {
            if (this.fluid == null) {
                return null;
            }
            long moved = doFluid(this.fluid, maxDrain, IOType.INPUT, doDrain);
            if (moved <= 0L) {
                return null;
            }
            FluidStack out = this.fluid.copy();
            out.amount = downcastAmount(moved);
            return out;
        }

        @Override
        public long mmceguiext$simulateFluidIO(FluidStack stack, long maxAmount, IOType actionType) {
            return doFluid(stack, maxAmount, actionType, false);
        }

        @Override
        public long mmceguiext$doFluidIO(FluidStack stack, long maxAmount, IOType actionType) {
            return doFluid(stack, maxAmount, actionType, true);
        }

        private long doFluid(FluidStack stack, long maxAmount, IOType actionType, boolean mutate) {
            if (stack == null || maxAmount <= 0L) {
                return 0L;
            }
            if (this.capacity < 0L) {
                this.capacity = this.amount + this.source.mmceguiext$simulateFluidIO(stack, Long.MAX_VALUE - this.amount, IOType.OUTPUT);
            }
            if (actionType == IOType.INPUT) {
                if (this.fluid == null || this.amount <= 0L || !this.fluid.isFluidEqual(stack)) {
                    return 0L;
                }
                long moved = Math.min(maxAmount, this.amount);
                if (mutate) {
                    this.amount -= moved;
                    if (this.amount <= 0L) {
                        this.fluid = null;
                    }
                }
                return moved;
            }
            if (this.fluid != null && !this.fluid.isFluidEqual(stack)) {
                return 0L;
            }
            long moved = Math.min(maxAmount, Math.max(0L, this.capacity - this.amount));
            if (mutate && moved > 0L) {
                if (this.fluid == null) {
                    this.fluid = stack.copy();
                }
                this.amount += moved;
            }
            return moved;
        }

        private final class SnapshotFluidTankProperties implements IFluidTankProperties {
            @Nullable
            @Override
            public FluidStack getContents() {
                if (SnapshotFluidHandler.this.fluid == null || SnapshotFluidHandler.this.amount <= 0L) {
                    return null;
                }
                FluidStack out = SnapshotFluidHandler.this.fluid.copy();
                out.amount = downcastAmount(SnapshotFluidHandler.this.amount);
                return out;
            }

            @Override
            public int getCapacity() {
                return downcastAmount(Math.max(0L, SnapshotFluidHandler.this.capacity));
            }

            @Override
            public boolean canFill() {
                return true;
            }

            @Override
            public boolean canDrain() {
                return true;
            }

            @Override
            public boolean canFillFluidType(FluidStack fluidStack) {
                return true;
            }

            @Override
            public boolean canDrainFluidType(FluidStack fluidStack) {
                return true;
            }
        }
    }

    private static final class SnapshotGasHandler implements IExtendedGasHandler, LongGasIOHandler {
        private final LongGasIOHandler source;
        @Nullable
        private GasStack gas;
        private long amount;
        private long capacity = -1L;

        private SnapshotGasHandler(IExtendedGasHandler handler, LongGasIOHandler source) {
            this.source = source;
            GasTankInfo[] infos = handler.getTankInfo();
            if (infos != null && infos.length > 0 && infos[0] != null) {
                GasStack contents = infos[0].getGas();
                if (contents != null && contents.amount > 0) {
                    this.gas = contents.copy();
                    this.amount = source.mmceguiext$simulateGasIO(contents, Long.MAX_VALUE, IOType.INPUT);
                    this.capacity = this.amount + source.mmceguiext$simulateGasIO(contents, Long.MAX_VALUE - this.amount, IOType.OUTPUT);
                }
            }
        }

        @Nullable
        @Override
        @Optional.Method(modid = "mekanism")
        public GasStack drawGas(GasStack toDraw, boolean doTransfer) {
            if (toDraw == null) {
                return null;
            }
            long moved = doGas(toDraw, toDraw.amount, IOType.INPUT, doTransfer);
            if (moved <= 0L) {
                return null;
            }
            GasStack out = toDraw.copy();
            out.amount = downcastAmount(moved);
            return out;
        }

        @Nullable
        @Override
        @Optional.Method(modid = "mekanism")
        public GasStack drawGas(@Nullable net.minecraft.util.EnumFacing side, int amount, boolean doTransfer) {
            if (this.gas == null) {
                return null;
            }
            GasStack request = this.gas.copy();
            request.amount = amount;
            return drawGas(request, doTransfer);
        }

        @Override
        @Optional.Method(modid = "mekanism")
        public int receiveGas(@Nullable net.minecraft.util.EnumFacing side, GasStack stack, boolean doTransfer) {
            return downcastAmount(doGas(stack, stack == null ? 0L : stack.amount, IOType.OUTPUT, doTransfer));
        }

        @Override
        @Optional.Method(modid = "mekanism")
        public boolean canReceiveGas(@Nullable net.minecraft.util.EnumFacing side, Gas gas) {
            return gas != null && doGas(new GasStack(gas, 1), 1L, IOType.OUTPUT, false) > 0L;
        }

        @Override
        @Optional.Method(modid = "mekanism")
        public boolean canDrawGas(@Nullable net.minecraft.util.EnumFacing side, Gas gas) {
            return this.gas != null && gas != null && this.amount > 0L && this.gas.getGas() == gas;
        }

        @Nonnull
        @Override
        @Optional.Method(modid = "mekanism")
        public GasTankInfo[] getTankInfo() {
            return new GasTankInfo[]{new SnapshotGasTankInfo()};
        }

        @Override
        @Optional.Method(modid = "mekanism")
        public long mmceguiext$simulateGasIO(GasStack stack, long maxAmount, IOType actionType) {
            return doGas(stack, maxAmount, actionType, false);
        }

        @Override
        @Optional.Method(modid = "mekanism")
        public long mmceguiext$doGasIO(GasStack stack, long maxAmount, IOType actionType) {
            return doGas(stack, maxAmount, actionType, true);
        }

        @Optional.Method(modid = "mekanism")
        private long doGas(GasStack stack, long maxAmount, IOType actionType, boolean mutate) {
            if (stack == null || maxAmount <= 0L) {
                return 0L;
            }
            if (this.capacity < 0L) {
                this.capacity = this.amount + this.source.mmceguiext$simulateGasIO(stack, Long.MAX_VALUE - this.amount, IOType.OUTPUT);
            }
            if (actionType == IOType.INPUT) {
                if (this.gas == null || this.amount <= 0L || !this.gas.isGasEqual(stack)) {
                    return 0L;
                }
                long moved = Math.min(maxAmount, this.amount);
                if (mutate) {
                    this.amount -= moved;
                    if (this.amount <= 0L) {
                        this.gas = null;
                    }
                }
                return moved;
            }
            if (this.gas != null && !this.gas.isGasEqual(stack)) {
                return 0L;
            }
            long moved = Math.min(maxAmount, Math.max(0L, this.capacity - this.amount));
            if (mutate && moved > 0L) {
                if (this.gas == null) {
                    this.gas = stack.copy();
                }
                this.amount += moved;
            }
            return moved;
        }

        private final class SnapshotGasTankInfo implements GasTankInfo {
            @Nullable
            @Override
            @Optional.Method(modid = "mekanism")
            public GasStack getGas() {
                if (SnapshotGasHandler.this.gas == null || SnapshotGasHandler.this.amount <= 0L) {
                    return null;
                }
                GasStack out = SnapshotGasHandler.this.gas.copy();
                out.amount = downcastAmount(SnapshotGasHandler.this.amount);
                return out;
            }

            @Override
            @Optional.Method(modid = "mekanism")
            public int getStored() {
                return downcastAmount(SnapshotGasHandler.this.amount);
            }

            @Override
            @Optional.Method(modid = "mekanism")
            public int getMaxGas() {
                return downcastAmount(Math.max(0L, SnapshotGasHandler.this.capacity));
            }
        }
    }
}
