package com.fushu.mmceguiext.common.network;

import com.fushu.mmceguiext.common.tile.TileCustomHatch;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PktCustomHatchEnergySync implements IMessage, IMessageHandler<PktCustomHatchEnergySync, IMessage> {
    private BlockPos pos = BlockPos.ORIGIN;
    private long stored = 0L;
    private long capacity = 1L;
    private long transfer = 1L;

    public PktCustomHatchEnergySync() {
    }

    public PktCustomHatchEnergySync(BlockPos pos, long stored, long capacity, long transfer) {
        this.pos = pos == null ? BlockPos.ORIGIN : pos;
        this.stored = Math.max(0L, stored);
        this.capacity = Math.max(1L, capacity);
        this.transfer = Math.max(1L, transfer);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        NetworkBufferUtils.requireReadable(buf, 32);
        this.pos = BlockPos.fromLong(buf.readLong());
        this.stored = buf.readLong();
        this.capacity = buf.readLong();
        this.transfer = buf.readLong();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(this.pos.toLong());
        buf.writeLong(this.stored);
        buf.writeLong(this.capacity);
        buf.writeLong(this.transfer);
    }

    @Override
    public IMessage onMessage(PktCustomHatchEnergySync message, MessageContext ctx) {
        Minecraft.getMinecraft().addScheduledTask(() -> handle(message));
        return null;
    }

    private static void handle(PktCustomHatchEnergySync message) {
        if (message == null || Minecraft.getMinecraft().world == null || !Minecraft.getMinecraft().world.isBlockLoaded(message.pos)) {
            return;
        }
        TileEntity tile = Minecraft.getMinecraft().world.getTileEntity(message.pos);
        if (!(tile instanceof TileCustomHatch)) {
            return;
        }
        TileCustomHatch hatch = (TileCustomHatch) tile;
        hatch.applyClientEnergyCapacity(message.capacity);
        hatch.applyClientEnergyTransfer(message.transfer);
        hatch.applyClientEnergyStored(message.stored);
    }
}
