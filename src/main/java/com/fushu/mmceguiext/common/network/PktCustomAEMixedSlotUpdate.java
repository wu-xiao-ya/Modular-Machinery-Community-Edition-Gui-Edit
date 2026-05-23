package com.fushu.mmceguiext.common.network;

import appeng.fluids.util.AEFluidStack;
import com.fushu.mmceguiext.common.tile.TileCustomAEMixedInputBus;
import com.mekeng.github.common.me.data.impl.AEGasStack;
import com.mekeng.github.util.Utils;
import hellfirepvp.modularmachinery.ModularMachinery;
import io.netty.buffer.ByteBuf;
import mekanism.api.gas.GasStack;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PktCustomAEMixedSlotUpdate implements IMessage, IMessageHandler<PktCustomAEMixedSlotUpdate, IMessage> {
    public static final int TARGET_FLUID = 0;
    public static final int TARGET_GAS = 1;

    private BlockPos pos = BlockPos.ORIGIN;
    private int target = TARGET_FLUID;

    public PktCustomAEMixedSlotUpdate() {
    }

    public PktCustomAEMixedSlotUpdate(BlockPos pos, int target) {
        this.pos = pos == null ? BlockPos.ORIGIN : pos;
        this.target = target;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.pos = BlockPos.fromLong(buf.readLong());
        this.target = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(this.pos.toLong());
        buf.writeInt(this.target);
    }

    @Override
    public IMessage onMessage(PktCustomAEMixedSlotUpdate message, MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().player;
        if (player == null || player.world == null || !player.world.isBlockLoaded(message.pos)) {
            return null;
        }
        TileEntity tile = player.world.getTileEntity(message.pos);
        if (!(tile instanceof TileCustomAEMixedInputBus)) {
            return null;
        }
        TileCustomAEMixedInputBus bus = (TileCustomAEMixedInputBus) tile;
        ItemStack held = player.inventory.getItemStack();
        if (held.isEmpty()) {
            held = player.getHeldItemMainhand();
        }
        if (held.isEmpty()) {
            held = player.getHeldItemOffhand();
        }

        if (message.target == TARGET_FLUID) {
            if (held.isEmpty()) {
                bus.getFluidConfig().setFluidInSlot(0, null);
                ModularMachinery.log.info("[MMCEGE] Clear mixed fluid config at {}", message.pos);
            } else {
                FluidStack fluid = FluidUtil.getFluidContained(held);
                bus.getFluidConfig().setFluidInSlot(0, fluid == null ? null : AEFluidStack.fromFluidStack(fluid));
                ModularMachinery.log.info("[MMCEGE] Update mixed fluid config at {} using {} -> {}", message.pos, held.getDisplayName(), fluid == null ? "null" : fluid.getLocalizedName());
            }
            bus.markForUpdateSync();
            return null;
        }

        if (message.target == TARGET_GAS) {
            if (held.isEmpty()) {
                bus.getGasConfig().setGas(0, null);
                ModularMachinery.log.info("[MMCEGE] Clear mixed gas config at {}", message.pos);
            } else {
                GasStack gas = Utils.getGasFromItem(held);
                bus.getGasConfig().setGas(0, gas == null ? null : gas.copy());
                ModularMachinery.log.info("[MMCEGE] Update mixed gas config at {} using {} -> {}", message.pos, held.getDisplayName(), gas == null ? "null" : gas.getGas().getLocalizedName());
            }
            bus.markForUpdateSync();
        }

        return null;
    }
}
