package com.fushu.mmceguiext.common.network;

import com.fushu.mmceguiext.MMCEGuiExt;
import com.fushu.mmceguiext.common.config.ControllerButtonPolicyManager;
import com.fushu.mmceguiext.common.util.ControllerCustomDataAccess;
import hellfirepvp.modularmachinery.common.container.ContainerController;
import hellfirepvp.modularmachinery.common.container.ContainerFactoryController;
import hellfirepvp.modularmachinery.common.tiles.TileSmartInterface;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import hellfirepvp.modularmachinery.common.util.SmartInterfaceData;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

import java.lang.reflect.Method;
import java.util.Map;

public class PktControllerSmartInterfaceUpdate implements IMessage, IMessageHandler<PktControllerSmartInterfaceUpdate, IMessage> {
    private static final int MAX_INTERFACE_TYPE_LENGTH = 128;
    private static final int MAX_VALUE_LENGTH = 1024;
    private static final byte VALUE_NUMBER = 0;
    private static final byte VALUE_STRING = 1;
    private BlockPos controllerPos = BlockPos.ORIGIN;
    private String interfaceType = "";
    private float value = 0.0F;
    private boolean stringValue = false;
    private String textValue = "";

    public PktControllerSmartInterfaceUpdate() {
    }

    public PktControllerSmartInterfaceUpdate(BlockPos controllerPos, String interfaceType, float value) {
        this.controllerPos = controllerPos;
        this.interfaceType = interfaceType == null ? "" : interfaceType;
        this.value = value;
        this.stringValue = false;
    }

    public PktControllerSmartInterfaceUpdate(BlockPos controllerPos, String interfaceType, String value) {
        this.controllerPos = controllerPos;
        this.interfaceType = interfaceType == null ? "" : interfaceType;
        this.stringValue = true;
        this.textValue = value == null ? "" : value;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        NetworkBufferUtils.requireReadable(buf, 8);
        this.controllerPos = BlockPos.fromLong(buf.readLong());
        this.interfaceType = NetworkBufferUtils.readBoundedUtf8(buf, MAX_INTERFACE_TYPE_LENGTH);
        NetworkBufferUtils.requireReadable(buf, 1);
        byte kind = buf.readByte();
        this.stringValue = kind == VALUE_STRING;
        if (this.stringValue) {
            this.textValue = NetworkBufferUtils.readBoundedUtf8(buf, MAX_VALUE_LENGTH);
        } else {
            NetworkBufferUtils.requireReadable(buf, 4);
            this.value = buf.readFloat();
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(this.controllerPos.toLong());
        NetworkBufferUtils.writeBoundedUtf8(buf, this.interfaceType, MAX_INTERFACE_TYPE_LENGTH);
        buf.writeByte(this.stringValue ? VALUE_STRING : VALUE_NUMBER);
        if (this.stringValue) {
            NetworkBufferUtils.writeBoundedUtf8(buf, this.textValue, MAX_VALUE_LENGTH);
        } else {
            buf.writeFloat(this.value);
        }
    }

    @Override
    public IMessage onMessage(PktControllerSmartInterfaceUpdate message, MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().player;
        if (player == null) {
            return null;
        }
        player.getServerWorld().addScheduledTask(() -> handle(message, player));
        return null;
    }

    private static void handle(PktControllerSmartInterfaceUpdate message, EntityPlayerMP player) {
        if (message == null) {
            return;
        }
        String interfaceType = normalizeBounded(message.interfaceType, MAX_INTERFACE_TYPE_LENGTH);
        if (interfaceType == null) {
            return;
        }
        if (!message.stringValue && !Float.isFinite(message.value)) {
            return;
        }
        if (message.stringValue && message.textValue != null && message.textValue.length() > MAX_VALUE_LENGTH) {
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
        boolean updated;
        if (message.stringValue) {
            updated = applySmartInterfaceUpdate(controller, message.controllerPos, interfaceType, message.textValue == null ? "" : message.textValue);
        } else {
            updated = applySmartInterfaceUpdate(controller, message.controllerPos, interfaceType, message.value);
        }
        if (updated) {
            syncCustomDataToPlayer(controller, player);
        }
    }

    static boolean applySmartInterfaceUpdate(TileMultiblockMachineController controller, BlockPos controllerPos, String interfaceType, float value) {
        interfaceType = normalizeBounded(interfaceType, MAX_INTERFACE_TYPE_LENGTH);
        if (controller == null || controllerPos == null || interfaceType == null || !Float.isFinite(value)) {
            return false;
        }
        boolean configuredKey = ControllerButtonPolicyManager.isConfiguredSmartKey(controller, interfaceType);
        if (!configuredKey && !hasFoundSmartInterface(controller, interfaceType)) {
            return false;
        }
        if (tryInvokeControllerSmartUpdate(controller, interfaceType, value)) {
            return true;
        }

        boolean updated = false;
        for (Map.Entry<TileSmartInterface.SmartInterfaceProvider, String> entry : controller.getFoundSmartInterfaces().entrySet()) {
            if (!interfaceType.equals(entry.getValue())) {
                continue;
            }
            TileSmartInterface.SmartInterfaceProvider provider = entry.getKey();
            if (provider == null) {
                continue;
            }

            SmartInterfaceData current = provider.getMachineData(controllerPos);
            if (current == null) {
                continue;
            }

            provider.addMachineData(controllerPos, current.getParent(), current.getType(), value, true);
            ControllerCustomDataAccess.writeNumber(controller, interfaceType, value);
            updated = true;
            break;
        }
        if (!updated && configuredKey) {
            updated = ControllerCustomDataAccess.writeNumber(controller, interfaceType, value);
        }
        if (updated) {
            controller.markForUpdateSync();
        }
        return updated;
    }

    static boolean applySmartInterfaceUpdate(TileMultiblockMachineController controller, BlockPos controllerPos, String interfaceType, String value) {
        interfaceType = normalizeBounded(interfaceType, MAX_INTERFACE_TYPE_LENGTH);
        if (controller == null || controllerPos == null || interfaceType == null || value == null || value.length() > MAX_VALUE_LENGTH) {
            return false;
        }

        boolean configuredKey = ControllerButtonPolicyManager.isConfiguredSmartKey(controller, interfaceType)
            || hasControllerCustomData(controller, interfaceType);
        if (!configuredKey) {
            return false;
        }

        boolean updated = ControllerCustomDataAccess.writeString(controller, interfaceType, value);
        if (updated) {
            controller.markForUpdateSync();
        }
        return updated;
    }

    private static String normalizeBounded(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() || trimmed.length() > maxLength ? null : trimmed;
    }

    private static boolean hasFoundSmartInterface(TileMultiblockMachineController controller, String interfaceType) {
        for (Map.Entry<TileSmartInterface.SmartInterfaceProvider, String> entry : controller.getFoundSmartInterfaces().entrySet()) {
            if (interfaceType.equals(entry.getValue()) && entry.getKey() != null) {
                return true;
            }
        }
        return false;
    }

    private static boolean tryInvokeControllerSmartUpdate(TileMultiblockMachineController controller, String interfaceType, float value) {
        try {
            Method method = controller.getClass().getMethod("updateSmartInterfaceValue", String.class, float.class);
            Object result = method.invoke(controller, interfaceType, value);
            return !(result instanceof Boolean) || (Boolean) result;
        } catch (Exception ignored) {
            return false;
        }
    }

    static boolean hasControllerCustomData(TileMultiblockMachineController controller, String key) {
        return ControllerCustomDataAccess.hasKey(controller, key);
    }

    static Float readNumericControllerCustomData(TileMultiblockMachineController controller, String key) {
        return ControllerCustomDataAccess.readNumber(controller, key);
    }

    static void syncCustomDataToPlayer(TileMultiblockMachineController controller, EntityPlayerMP player) {
        if (controller == null || player == null) {
            return;
        }
        MMCEGuiExt.NET_CHANNEL.sendTo(
            new PktControllerCustomDataSync(controller.getPos(), ControllerCustomDataAccess.readTag(controller)),
            player
        );
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
