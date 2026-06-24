package com.fushu.mmceguiext.common.energy;

import net.minecraft.nbt.NBTBase;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.CapabilityManager;

import javax.annotation.Nullable;

public final class LongEnergyCapability {
    @CapabilityInject(ILongEnergyStorage.class)
    public static final Capability<ILongEnergyStorage> LONG_ENERGY = null;

    private LongEnergyCapability() {
    }

    public static void register() {
        CapabilityManager.INSTANCE.register(ILongEnergyStorage.class, new Capability.IStorage<ILongEnergyStorage>() {
            @Nullable
            @Override
            public NBTBase writeNBT(Capability<ILongEnergyStorage> capability, ILongEnergyStorage instance, EnumFacing side) {
                return null;
            }

            @Override
            public void readNBT(Capability<ILongEnergyStorage> capability, ILongEnergyStorage instance, EnumFacing side, NBTBase nbt) {
            }
        }, EmptyLongEnergyStorage::new);
    }

    private static final class EmptyLongEnergyStorage implements ILongEnergyStorage {
        @Override
        public long receiveEnergyLong(long maxReceive, boolean simulate) {
            return 0L;
        }

        @Override
        public long extractEnergyLong(long maxExtract, boolean simulate) {
            return 0L;
        }

        @Override
        public long getEnergyStoredLong() {
            return 0L;
        }

        @Override
        public long getMaxEnergyStoredLong() {
            return 0L;
        }

        @Override
        public boolean canExtractLong() {
            return false;
        }

        @Override
        public boolean canReceiveLong() {
            return false;
        }
    }
}
