package com.fushu.mmceguiext.common.requirement;

import hellfirepvp.modularmachinery.common.machine.IOType;
import mekanism.api.gas.GasStack;

public interface LongGasIOHandler {
    long mmceguiext$simulateGasIO(GasStack stack, long maxAmount, IOType actionType);

    long mmceguiext$doGasIO(GasStack stack, long maxAmount, IOType actionType);
}
