package com.fushu.mmceguiext.common.requirement;

import github.kasuminova.mmce.common.util.IExtendedGasHandler;
import github.kasuminova.mmce.common.util.MultiGasTank;
import hellfirepvp.modularmachinery.common.crafting.helper.ProcessingComponent;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.machine.MachineComponent;
import mekanism.api.gas.Gas;
import mekanism.api.gas.GasStack;
import mekanism.api.gas.GasTankInfo;
import mekanism.api.gas.IGasHandler;
import net.minecraftforge.fml.common.Optional;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

@Optional.Interface(modid = "mekanism", iface = "mekanism.api.gas.IGasHandler")
public final class LongGasRequirementIO {
    private LongGasRequirementIO() {
    }

    @Optional.Method(modid = "mekanism")
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
                total += clampMoved(((LongGasIOHandler) handler).mmceguiext$simulateGasIO(stack, remaining, actionType), remaining);
            } else {
                total += simulateGasFallback(stack, handler, remaining, actionType);
            }
        }
        return total;
    }

    @Optional.Method(modid = "mekanism")
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
                moved = clampMoved(((LongGasIOHandler) handler).mmceguiext$doGasIO(stack, remaining, actionType), remaining);
            } else {
                moved = doGasFallback(stack, handler, remaining, actionType);
            }
            remaining -= clampMoved(moved, remaining);
        }
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
        return LongRequirementAmounts.downcastAmount(value);
    }

    private static long clampMoved(long moved, long maxAmount) {
        if (moved <= 0L || maxAmount <= 0L) {
            return 0L;
        }
        return Math.min(moved, maxAmount);
    }

    @Optional.Method(modid = "mekanism")
    private static long simulateGasFallback(GasStack stack, IExtendedGasHandler handler, long maxAmount, IOType actionType) {
        return gasFallback(stack, handler, maxAmount, actionType, false);
    }

    @Optional.Method(modid = "mekanism")
    private static long doGasFallback(GasStack stack, IExtendedGasHandler handler, long maxAmount, IOType actionType) {
        return gasFallback(stack, handler, maxAmount, actionType, true);
    }

    @Optional.Method(modid = "mekanism")
    private static long gasFallback(GasStack stack, IExtendedGasHandler handler, long maxAmount, IOType actionType, boolean doTransfer) {
        if (stack == null || handler == null || maxAmount <= 0L) {
            return 0L;
        }
        long moved = 0L;
        while (moved < maxAmount) {
            long remaining = maxAmount - moved;
            GasStack copy = stack.copy();
            copy.amount = downcastAmount(remaining);
            long step = 0L;
            if (actionType == IOType.INPUT) {
                GasStack drawn = handler.drawGas(copy, doTransfer);
                step = drawn == null ? 0L : Math.max(0L, drawn.amount);
            } else if (handler.canReceiveGas(null, copy.getGas())) {
                step = Math.max(0, handler.receiveGas(null, copy, doTransfer));
            }
            step = clampMoved(step, remaining);
            if (step <= 0L) {
                break;
            }
            moved += step;
            if (!doTransfer || step < copy.amount) {
                break;
            }
        }
        return moved;
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
                this.capacity = LongRequirementAmounts.saturatedAdd(this.amount, this.source.mmceguiext$simulateGasIO(stack, Long.MAX_VALUE - this.amount, IOType.OUTPUT));
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
                    this.gas.amount = downcastAmount(this.amount + moved);
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
