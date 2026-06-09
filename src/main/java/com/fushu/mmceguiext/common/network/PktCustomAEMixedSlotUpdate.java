package com.fushu.mmceguiext.common.network;

import appeng.fluids.util.AEFluidStack;
import com.fushu.mmceguiext.common.container.ContainerCustomAEMixedInputBus;
import com.fushu.mmceguiext.common.tile.TileCustomAEMixedInputBus;
import com.mekeng.github.common.me.data.impl.AEGasStack;
import com.mekeng.github.util.Utils;
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
    private int slotIndex = 0;

    public PktCustomAEMixedSlotUpdate() {
    }

    public PktCustomAEMixedSlotUpdate(BlockPos pos, int target) {
        this(pos, target, 0);
    }

    public PktCustomAEMixedSlotUpdate(BlockPos pos, int target, int slotIndex) {
        this.pos = pos == null ? BlockPos.ORIGIN : pos;
        this.target = target;
        this.slotIndex = Math.max(0, slotIndex);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.pos = BlockPos.fromLong(buf.readLong());
        this.target = buf.readInt();
        this.slotIndex = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(this.pos.toLong());
        buf.writeInt(this.target);
        buf.writeInt(this.slotIndex);
    }

    @Override
    public IMessage onMessage(PktCustomAEMixedSlotUpdate message, MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().player;
        if (player == null) {
            return null;
        }
        player.getServerWorld().addScheduledTask(() -> handle(message, player));
        return null;
    }

    private static void handle(PktCustomAEMixedSlotUpdate message, EntityPlayerMP player) {
        if (player == null || player.world == null || !player.world.isBlockLoaded(message.pos)) {
            return;
        }
        TileEntity tile = player.world.getTileEntity(message.pos);
        if (!(tile instanceof TileCustomAEMixedInputBus)) {
            return;
        }
        TileCustomAEMixedInputBus bus = (TileCustomAEMixedInputBus) tile;
        if (!(player.openContainer instanceof ContainerCustomAEMixedInputBus)) {
            return;
        }
        ContainerCustomAEMixedInputBus container = (ContainerCustomAEMixedInputBus) player.openContainer;
        if (container.getOwner() != bus || !container.canInteractWith(player)) {
            return;
        }
        ItemStack held = player.inventory.getItemStack();
        if (held.isEmpty()) {
            held = player.getHeldItemMainhand();
        }
        if (held.isEmpty()) {
            held = player.getHeldItemOffhand();
        }

        if (message.target == TARGET_FLUID) {
            if (message.slotIndex < 0 || message.slotIndex >= bus.getActiveFluidSlots()
                || !bus.isFluidConfigSlotDefined(message.slotIndex)) {
                return;
            }
            int slot = message.slotIndex;
            if (held.isEmpty()) {
                bus.getFluidConfig().setFluidInSlot(slot, null);
            } else {
                FluidStack fluid = FluidUtil.getFluidContained(held);
                bus.getFluidConfig().setFluidInSlot(slot, fluid == null ? null : AEFluidStack.fromFluidStack(fluid));
            }
            bus.markForUpdateSync();
            return;
        }

        if (message.target == TARGET_GAS) {
            if (message.slotIndex < 0 || message.slotIndex >= bus.getActiveGasSlots()
                || !bus.isGasConfigSlotDefined(message.slotIndex)) {
                return;
            }
            int slot = message.slotIndex;
            if (held.isEmpty()) {
                bus.getGasConfig().setGas(slot, null);
            } else {
                GasStack gas = Utils.getGasFromItem(held);
                bus.getGasConfig().setGas(slot, gas == null ? null : gas.copy());
            }
            bus.markForUpdateSync();
        }
    }
}
