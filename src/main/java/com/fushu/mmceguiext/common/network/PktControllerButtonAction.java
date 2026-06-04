package com.fushu.mmceguiext.common.network;

import hellfirepvp.modularmachinery.common.container.ContainerController;
import hellfirepvp.modularmachinery.common.container.ContainerFactoryController;
import hellfirepvp.modularmachinery.ModularMachinery;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import hellfirepvp.modularmachinery.common.util.SmartInterfaceData;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PktControllerButtonAction implements IMessage, IMessageHandler<PktControllerButtonAction, IMessage> {
    private static final byte KIND_SMART_SET = 0;
    private static final byte KIND_SMART_ADD = 1;
    private static final byte KIND_EVENT = 2;

    private BlockPos controllerPos = BlockPos.ORIGIN;
    private String key = "";
    private String buttonId = "";
    private byte kind = KIND_SMART_ADD;
    private float value = 0.0F;
    private boolean hasMin = false;
    private float min = 0.0F;
    private boolean hasMax = false;
    private float max = 0.0F;

    public PktControllerButtonAction() {
    }

    public static PktControllerButtonAction smart(BlockPos controllerPos, String key, boolean additive, float value, Float min, Float max) {
        PktControllerButtonAction pkt = new PktControllerButtonAction();
        pkt.controllerPos = controllerPos == null ? BlockPos.ORIGIN : controllerPos;
        pkt.key = key == null ? "" : key;
        pkt.kind = additive ? KIND_SMART_ADD : KIND_SMART_SET;
        pkt.value = value;
        pkt.hasMin = min != null && Float.isFinite(min.floatValue());
        pkt.min = pkt.hasMin ? min.floatValue() : 0.0F;
        pkt.hasMax = max != null && Float.isFinite(max.floatValue());
        pkt.max = pkt.hasMax ? max.floatValue() : 0.0F;
        return pkt;
    }

    public static PktControllerButtonAction event(BlockPos controllerPos, String buttonId) {
        PktControllerButtonAction pkt = new PktControllerButtonAction();
        pkt.controllerPos = controllerPos == null ? BlockPos.ORIGIN : controllerPos;
        pkt.buttonId = buttonId == null ? "" : buttonId;
        pkt.kind = KIND_EVENT;
        return pkt;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.controllerPos = BlockPos.fromLong(buf.readLong());
        this.kind = buf.readByte();
        this.key = ByteBufUtils.readUTF8String(buf);
        this.buttonId = ByteBufUtils.readUTF8String(buf);
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
        buf.writeByte(this.kind);
        ByteBufUtils.writeUTF8String(buf, this.key);
        ByteBufUtils.writeUTF8String(buf, this.buttonId);
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
        EntityPlayerMP player = ctx.getServerHandler().player;
        if (player == null || player.world == null || !player.world.isBlockLoaded(message.controllerPos)) {
            return null;
        }
        ModularMachinery.log.info(
            "[MMCEGE] Server received button packet pos={} kind={} key={} buttonId={} value={} player={}",
            message.controllerPos,
            message.kind,
            message.key,
            message.buttonId,
            message.value,
            player.getName()
        );
        if (!isPlayerEditingThisController(player, message.controllerPos)) {
            ModularMachinery.log.warn("[MMCEGE] Reject button packet: player is not editing controller at {}", message.controllerPos);
            return null;
        }

        TileEntity tile = player.world.getTileEntity(message.controllerPos);
        if (!(tile instanceof TileMultiblockMachineController)) {
            return null;
        }
        TileMultiblockMachineController controller = (TileMultiblockMachineController) tile;

        if (message.kind == KIND_EVENT) {
            if (message.buttonId == null || message.buttonId.trim().isEmpty()) {
                return null;
            }
            ModularMachinery.log.info("[MMCEGE] Handling event button {}", message.buttonId);
            postControllerButtonClickEvent(controller, message.buttonId);
            return null;
        }

        if (message.key == null || message.key.trim().isEmpty() || !Float.isFinite(message.value)) {
            return null;
        }

        float resolved = message.value;
        if (message.kind == KIND_SMART_ADD) {
            SmartInterfaceData current = controller.getSmartInterfaceData(message.key);
            Float customValue = readControllerCustomData(controller, message.key);
            float base = current != null
                ? current.getValue()
                : customValue != null && Float.isFinite(customValue.floatValue()) ? customValue.floatValue() : 0.0F;
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
        ModularMachinery.log.info(
            "[MMCEGE] Applying smart button key={} resolved={} hasMin={} min={} hasMax={} max={}",
            message.key,
            resolved,
            message.hasMin,
            message.min,
            message.hasMax,
            message.max
        );

        writeControllerCustomData(controller, message.key, resolved);
        PktControllerSmartInterfaceUpdate delegate = new PktControllerSmartInterfaceUpdate(message.controllerPos, message.key, resolved);
        return delegate.onMessage(delegate, ctx);
    }

    private static void writeControllerCustomData(TileMultiblockMachineController controller, String key, float value) {
        try {
            java.lang.reflect.Method getTagMethod = controller.getClass().getMethod("getCustomDataTag");
            java.lang.reflect.Method setTagMethod = controller.getClass().getMethod("setCustomDataTag", NBTTagCompound.class);
            java.lang.reflect.Method markMethod = controller.getClass().getMethod("markForUpdateSync");

            NBTTagCompound tag = new NBTTagCompound();
            Object existing = getTagMethod.invoke(controller);
            if (existing instanceof NBTTagCompound) {
                tag = ((NBTTagCompound) existing).copy();
            }
            tag.setFloat(key, value);
            setTagMethod.invoke(controller, tag);
            markMethod.invoke(controller);
        } catch (Exception ignored) {
        }
    }

    private static Float readControllerCustomData(TileMultiblockMachineController controller, String key) {
        try {
            java.lang.reflect.Method getTagMethod = controller.getClass().getMethod("getCustomDataTag");
            Object existing = getTagMethod.invoke(controller);
            if (!(existing instanceof NBTTagCompound)) {
                return null;
            }
            NBTTagCompound tag = (NBTTagCompound) existing;
            return tag.hasKey(key) ? tag.getFloat(key) : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private static void postControllerButtonClickEvent(TileMultiblockMachineController controller, String buttonId) {
        try {
            Class<?> eventClass = Class.forName("github.kasuminova.mmce.common.event.client.ControllerButtonClickEvent");
            Object event = eventClass
                .getConstructor(TileMultiblockMachineController.class, String.class)
                .newInstance(controller, buttonId);
            eventClass.getMethod("postEvent").invoke(event);
        } catch (ReflectiveOperationException e) {
            ModularMachinery.log.warn("Failed to post controller button click event for button {}", buttonId, e);
        }
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
