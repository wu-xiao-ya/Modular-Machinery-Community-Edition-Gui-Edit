package com.fushu.mmceguiext.common.network;

import com.fushu.mmceguiext.common.config.ControllerButtonPolicyManager;
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
        NetworkBufferUtils.requireReadable(buf, 9);
        this.controllerPos = BlockPos.fromLong(buf.readLong());
        this.kind = buf.readByte();
        this.key = NetworkBufferUtils.readBoundedUtf8(buf, MAX_KEY_LENGTH);
        this.buttonId = NetworkBufferUtils.readBoundedUtf8(buf, MAX_BUTTON_ID_LENGTH);
        NetworkBufferUtils.requireReadable(buf, 5);
        this.value = buf.readFloat();
        this.hasMin = buf.readBoolean();
        if (this.hasMin) {
            NetworkBufferUtils.requireReadable(buf, 4);
            this.min = buf.readFloat();
        }
        NetworkBufferUtils.requireReadable(buf, 1);
        this.hasMax = buf.readBoolean();
        if (this.hasMax) {
            NetworkBufferUtils.requireReadable(buf, 4);
            this.max = buf.readFloat();
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(this.controllerPos.toLong());
        buf.writeByte(this.kind);
        NetworkBufferUtils.writeBoundedUtf8(buf, this.key, MAX_KEY_LENGTH);
        NetworkBufferUtils.writeBoundedUtf8(buf, this.buttonId, MAX_BUTTON_ID_LENGTH);
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
        if (message == null) {
            return;
        }
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
            if (buttonId == null || ControllerButtonPolicyManager.matchEvent(controller, buttonId) == null) {
                return;
            }
            postControllerButtonClickEvent(controller, buttonId);
            return;
        }

        String key = normalizeBounded(message.key, MAX_KEY_LENGTH);
        if (key == null || !Float.isFinite(message.value)) {
            return;
        }
        ControllerButtonPolicyManager.ButtonPolicy policy =
            ControllerButtonPolicyManager.matchSmart(controller, message.kind, key, message.value);
        if (policy == null) {
            return;
        }

        float resolved = message.value;
        if (message.kind == KIND_SMART_ADD) {
            SmartInterfaceData current = controller.getSmartInterfaceData(key);
            Float customValue = readControllerCustomData(controller, key);
            float base = current != null
                ? current.getValue()
                : customValue != null && Float.isFinite(customValue.floatValue()) ? customValue.floatValue() : 0.0F;
            resolved = base + message.value;
        } else if (controller.getSmartInterfaceData(key) == null
            && !ControllerButtonPolicyManager.isConfiguredSmartKey(controller, key)
            && !PktControllerSmartInterfaceUpdate.hasControllerCustomData(controller, key)) {
            return;
        }
        if (!Float.isFinite(resolved)) {
            return;
        }
        Float min = policy.min;
        Float max = policy.max;
        if (min != null && max != null && min.floatValue() > max.floatValue()) {
            Float swap = min;
            min = max;
            max = swap;
        }
        if (min != null && Float.isFinite(min.floatValue())) {
            resolved = Math.max(min.floatValue(), resolved);
        }
        if (max != null && Float.isFinite(max.floatValue())) {
            resolved = Math.min(max.floatValue(), resolved);
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
        if (player.getDistanceSqToCenter(controllerPos) > 64D) {
            return false;
        }
        if (player.openContainer == null || !player.openContainer.canInteractWith(player)) {
            return false;
        }

        if (player.openContainer instanceof ContainerController) {
            TileMultiblockMachineController owner = ((ContainerController) player.openContainer).getOwner();
            return owner != null
                && owner.getPos().equals(controllerPos)
                && player.world.getTileEntity(controllerPos) == owner;
        }
        if (player.openContainer instanceof ContainerFactoryController) {
            TileMultiblockMachineController owner = ((ContainerFactoryController) player.openContainer).getOwner();
            return owner != null
                && owner.getPos().equals(controllerPos)
                && player.world.getTileEntity(controllerPos) == owner;
        }
        return false;
    }
}
