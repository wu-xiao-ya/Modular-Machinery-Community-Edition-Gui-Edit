package com.fushu.mmceguiext.common.network;

import hellfirepvp.modularmachinery.common.container.ContainerController;
import hellfirepvp.modularmachinery.common.container.ContainerFactoryController;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import hellfirepvp.modularmachinery.common.util.SmartInterfaceData;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PktControllerButtonAction implements IMessage, IMessageHandler<PktControllerButtonAction, IMessage> {
    private static final byte MODE_SET = 0;
    private static final byte MODE_ADD = 1;

    private BlockPos controllerPos = BlockPos.ORIGIN;
    private String key = "";
    private byte mode = MODE_ADD;
    private float value = 0.0F;
    private boolean hasMin = false;
    private float min = 0.0F;
    private boolean hasMax = false;
    private float max = 0.0F;

    public PktControllerButtonAction() {
    }

    public PktControllerButtonAction(BlockPos controllerPos, String key, boolean additive, float value, Float min, Float max) {
        this.controllerPos = controllerPos == null ? BlockPos.ORIGIN : controllerPos;
        this.key = key == null ? "" : key;
        this.mode = additive ? MODE_ADD : MODE_SET;
        this.value = value;
        this.hasMin = min != null && Float.isFinite(min.floatValue());
        this.min = this.hasMin ? min.floatValue() : 0.0F;
        this.hasMax = max != null && Float.isFinite(max.floatValue());
        this.max = this.hasMax ? max.floatValue() : 0.0F;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.controllerPos = BlockPos.fromLong(buf.readLong());
        this.key = ByteBufUtils.readUTF8String(buf);
        this.mode = buf.readByte();
        this.value = buf.readFloat();
        this.hasMin = buf.readBoolean();
        if (this.hasMin) {
            this.min = buf.readFloat();
        }
        this.hasMax = buf.readBoolean();
        if (this.hasMax) {
            this.max = buf.readFloat();
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(this.controllerPos.toLong());
        ByteBufUtils.writeUTF8String(buf, this.key);
        buf.writeByte(this.mode);
        buf.writeFloat(this.value);
        buf.writeBoolean(this.hasMin);
        if (this.hasMin) {
            buf.writeFloat(this.min);
        }
        buf.writeBoolean(this.hasMax);
        if (this.hasMax) {
            buf.writeFloat(this.max);
        }
    }

    @Override
    public IMessage onMessage(PktControllerButtonAction message, MessageContext ctx) {
        if (message == null || message.key == null || message.key.trim().isEmpty() || !Float.isFinite(message.value)) {
            return null;
        }

        EntityPlayerMP player = ctx.getServerHandler().player;
        if (player == null || player.world == null || !player.world.isBlockLoaded(message.controllerPos)) {
            return null;
        }
        if (!isPlayerEditingThisController(player, message.controllerPos)) {
            return null;
        }

        TileEntity tile = player.world.getTileEntity(message.controllerPos);
        if (!(tile instanceof TileMultiblockMachineController)) {
            return null;
        }
        TileMultiblockMachineController controller = (TileMultiblockMachineController) tile;

        float resolved = message.value;
        if (message.mode == MODE_ADD) {
            SmartInterfaceData current = controller.getSmartInterfaceData(message.key);
            float base = current == null ? 0.0F : current.getValue();
            resolved = base + message.value;
        }
        if (!Float.isFinite(resolved)) {
            return null;
        }
        if (message.hasMin && message.hasMax && message.min > message.max) {
            float swap = message.min;
            message.min = message.max;
            message.max = swap;
        }
        if (message.hasMin) {
            resolved = Math.max(message.min, resolved);
        }
        if (message.hasMax) {
            resolved = Math.min(message.max, resolved);
        }
        if (!Float.isFinite(resolved)) {
            return null;
        }

        PktControllerSmartInterfaceUpdate delegate = new PktControllerSmartInterfaceUpdate(message.controllerPos, message.key, resolved);
        return delegate.onMessage(delegate, ctx);
    }

    private static boolean isPlayerEditingThisController(EntityPlayerMP player, BlockPos controllerPos) {
        if (player.getDistanceSqToCenter(controllerPos) > 64D * 64D) {
            return false;
        }

        if (player.openContainer instanceof ContainerController) {
            return ((ContainerController) player.openContainer).getOwner().getPos().equals(controllerPos);
        }
        if (player.openContainer instanceof ContainerFactoryController) {
            return ((ContainerFactoryController) player.openContainer).getOwner().getPos().equals(controllerPos);
        }
        return false;
    }
}
