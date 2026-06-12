package com.fushu.mmceguiext.common.requirement;

import hellfirepvp.modularmachinery.common.machine.IOType;
import net.minecraftforge.fluids.FluidStack;

public interface LongFluidIOHandler {
    long mmceguiext$simulateFluidIO(FluidStack stack, long maxAmount, IOType actionType);

    long mmceguiext$doFluidIO(FluidStack stack, long maxAmount, IOType actionType);
}
