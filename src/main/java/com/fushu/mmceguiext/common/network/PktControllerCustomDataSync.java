package com.fushu.mmceguiext.common.network;

import com.fushu.mmceguiext.common.util.ControllerCustomDataAccess;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PktControllerCustomDataSync implements IMessage, IMessageHandler<PktControllerCustomDataSync, IMessage> {
    private BlockPos controllerPos = BlockPos.ORIGIN;
    private NBTTagCompound customData = new NBTTagCompound();

    public PktControllerCustomDataSync() {
    }

    public PktControllerCustomDataSync(BlockPos controllerPos, NBTTagCompound customData) {
        this.controllerPos = controllerPos == null ? BlockPos.ORIGIN : controllerPos;
        this.customData = customData == null ? new NBTTagCompound() : customData.copy();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        NetworkBufferUtils.requireReadable(buf, 8);
        this.controllerPos = BlockPos.fromLong(buf.readLong());
        NBTTagCompound tag = ByteBufUtils.readTag(buf);
        this.customData = tag == null ? new NBTTagCompound() : tag;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(this.controllerPos.toLong());
        ByteBufUtils.writeTag(buf, this.customData == null ? new NBTTagCompound() : this.customData);
    }

    @Override
    public IMessage onMessage(PktControllerCustomDataSync message, MessageContext ctx) {
        Minecraft.getMinecraft().addScheduledTask(() -> handle(message));
        return null;
    }

    private static void handle(PktControllerCustomDataSync message) {
        if (message == null || Minecraft.getMinecraft().world == null || !Minecraft.getMinecraft().world.isBlockLoaded(message.controllerPos)) {
            return;
        }
        TileEntity tile = Minecraft.getMinecraft().world.getTileEntity(message.controllerPos);
        ControllerCustomDataAccess.replaceTag(tile, message.customData);
    }
}
