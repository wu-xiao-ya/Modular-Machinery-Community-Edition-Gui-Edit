package com.fushu.mmceguiext.common.energy;

public interface ILongEnergyStorage {
    long receiveEnergyLong(long maxReceive, boolean simulate);

    long extractEnergyLong(long maxExtract, boolean simulate);

    long getEnergyStoredLong();

    long getMaxEnergyStoredLong();

    boolean canExtractLong();

    boolean canReceiveLong();
}
