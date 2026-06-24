package com.fushu.mmceguiext.common.util;

import com.fushu.mmceguiext.common.energy.ILongEnergyStorage;
import com.fushu.mmceguiext.common.energy.LongEnergyCapability;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.energy.IEnergyStorage;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

public final class EnergyAccessHelper {
    private EnergyAccessHelper() {
    }

    public static long getStored(@Nullable Object target) {
        ILongEnergyStorage longStorage = resolveLongStorage(target);
        if (longStorage != null) {
            return Math.max(0L, longStorage.getEnergyStoredLong());
        }
        Number reflected = invokeNumberMethod(target, "getEnergyStoredLong");
        if (reflected != null) {
            return Math.max(0L, reflected.longValue());
        }
        IEnergyStorage energyStorage = resolveForgeEnergyStorage(target);
        return energyStorage == null ? 0L : Math.max(0L, (long) energyStorage.getEnergyStored());
    }

    public static long getCapacity(@Nullable Object target) {
        ILongEnergyStorage longStorage = resolveLongStorage(target);
        if (longStorage != null) {
            return Math.max(1L, longStorage.getMaxEnergyStoredLong());
        }
        Number reflected = invokeNumberMethod(target, "getEnergyCapacity");
        if (reflected != null) {
            return Math.max(1L, reflected.longValue());
        }
        reflected = invokeNumberMethod(target, "getMaxEnergyStoredLong");
        if (reflected != null) {
            return Math.max(1L, reflected.longValue());
        }
        IEnergyStorage energyStorage = resolveForgeEnergyStorage(target);
        return energyStorage == null ? 1L : Math.max(1L, (long) energyStorage.getMaxEnergyStored());
    }

    public static long getRemainingCapacity(@Nullable Object target) {
        return Math.max(0L, getCapacity(target) - getStored(target));
    }

    public static long receive(@Nullable Object target, long maxReceive, boolean simulate) {
        if (maxReceive <= 0L) {
            return 0L;
        }
        ILongEnergyStorage longStorage = resolveLongStorage(target);
        if (longStorage != null) {
            return Math.max(0L, longStorage.receiveEnergyLong(maxReceive, simulate));
        }
        IEnergyStorage energyStorage = resolveForgeEnergyStorage(target);
        if (energyStorage == null || !energyStorage.canReceive()) {
            return 0L;
        }
        return Math.max(0L, (long) energyStorage.receiveEnergy(clampToInt(maxReceive), simulate));
    }

    public static long extract(@Nullable Object target, long maxExtract, boolean simulate) {
        if (maxExtract <= 0L) {
            return 0L;
        }
        ILongEnergyStorage longStorage = resolveLongStorage(target);
        if (longStorage != null) {
            return Math.max(0L, longStorage.extractEnergyLong(maxExtract, simulate));
        }
        IEnergyStorage energyStorage = resolveForgeEnergyStorage(target);
        if (energyStorage == null || !energyStorage.canExtract()) {
            return 0L;
        }
        return Math.max(0L, (long) energyStorage.extractEnergy(clampToInt(maxExtract), simulate));
    }

    @Nullable
    public static ILongEnergyStorage resolveLongStorage(@Nullable Object target) {
        if (target instanceof ILongEnergyStorage) {
            return (ILongEnergyStorage) target;
        }
        if (!(target instanceof TileEntity)) {
            return null;
        }
        TileEntity tile = (TileEntity) target;
        if (LongEnergyCapability.LONG_ENERGY == null || tile.isInvalid()) {
            return null;
        }
        for (EnumFacing side : EnumFacing.VALUES) {
            if (tile.hasCapability(LongEnergyCapability.LONG_ENERGY, side)) {
                ILongEnergyStorage storage = tile.getCapability(LongEnergyCapability.LONG_ENERGY, side);
                if (storage != null) {
                    return storage;
                }
            }
        }
        if (tile.hasCapability(LongEnergyCapability.LONG_ENERGY, null)) {
            return tile.getCapability(LongEnergyCapability.LONG_ENERGY, null);
        }
        return null;
    }

    @Nullable
    public static IEnergyStorage resolveForgeEnergyStorage(@Nullable Object target) {
        if (target instanceof IEnergyStorage) {
            return (IEnergyStorage) target;
        }
        if (!(target instanceof TileEntity)) {
            return null;
        }
        TileEntity tile = (TileEntity) target;
        if (tile.isInvalid()) {
            return null;
        }
        for (EnumFacing side : EnumFacing.VALUES) {
            if (tile.hasCapability(CapabilityEnergy.ENERGY, side)) {
                IEnergyStorage storage = tile.getCapability(CapabilityEnergy.ENERGY, side);
                if (storage != null) {
                    return storage;
                }
            }
        }
        if (tile.hasCapability(CapabilityEnergy.ENERGY, null)) {
            return tile.getCapability(CapabilityEnergy.ENERGY, null);
        }
        return null;
    }

    @Nullable
    private static Number invokeNumberMethod(@Nullable Object target, String methodName) {
        if (target == null) {
            return null;
        }
        try {
            Method method = target.getClass().getMethod(methodName);
            Object result = method.invoke(target);
            return result instanceof Number ? (Number) result : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static int clampToInt(long value) {
        return value >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, value);
    }
}
