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
    private static final int MAX_KEY_LENGTH = 128;
    private static final int MAX_BUTTON_ID_LENGTH = 128;
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
        if (player == null) {
            return null;
        }
        player.getServerWorld().addScheduledTask(() -> handle(message, player));
        return null;
    }

    private static void handle(PktControllerButtonAction message, EntityPlayerMP player) {
        if (player == null || player.world == null || !player.world.isBlockLoaded(message.controllerPos)) {
            return;
        }
        if (!isPlayerEditingThisController(player, message.controllerPos)) {
            return;
        }

        TileEntity tile = player.world.getTileEntity(message.controllerPos);
        if (!(tile instanceof TileMultiblockMachineController)) {
            return;
        }
        TileMultiblockMachineController controller = (TileMultiblockMachineController) tile;

        if (message.kind == KIND_EVENT) {
            String buttonId = normalizeBounded(message.buttonId, MAX_BUTTON_ID_LENGTH);
            if (buttonId == null) {
                return;
            }
            postControllerButtonClickEvent(controller, buttonId);
            return;
        }

        String key = normalizeBounded(message.key, MAX_KEY_LENGTH);
        if (key == null || !Float.isFinite(message.value)) {
            return;
        }

        float resolved = message.value;
        if (message.kind == KIND_SMART_ADD) {
            SmartInterfaceData current = controller.getSmartInterfaceData(key);
            Float customValue = readControllerCustomData(controller, key);
            if (current == null && customValue == null) {
                return;
            }
            float base = current != null
                ? current.getValue()
                : customValue != null && Float.isFinite(customValue.floatValue()) ? customValue.floatValue() : 0.0F;
            resolved = base + message.value;
        } else if (controller.getSmartInterfaceData(key) == null
            && !PktControllerSmartInterfaceUpdate.hasControllerCustomData(controller, key)) {
            return;
        }
        if (!Float.isFinite(resolved)) {
            return;
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
            return;
        }
        PktControllerSmartInterfaceUpdate.applySmartInterfaceUpdate(controller, message.controllerPos, key, resolved);
    }

    private static String normalizeBounded(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() || trimmed.length() > maxLength ? null : trimmed;
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
