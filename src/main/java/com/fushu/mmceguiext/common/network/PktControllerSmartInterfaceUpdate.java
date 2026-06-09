package com.fushu.mmceguiext.common.network;

import hellfirepvp.modularmachinery.common.container.ContainerController;
import hellfirepvp.modularmachinery.common.container.ContainerFactoryController;
import hellfirepvp.modularmachinery.common.tiles.TileSmartInterface;
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

import java.lang.reflect.Method;
import java.util.Map;

public class PktControllerSmartInterfaceUpdate implements IMessage, IMessageHandler<PktControllerSmartInterfaceUpdate, IMessage> {
    private static final int MAX_INTERFACE_TYPE_LENGTH = 128;
    private BlockPos controllerPos = BlockPos.ORIGIN;
    private String interfaceType = "";
    private float value = 0.0F;

    public PktControllerSmartInterfaceUpdate() {
    }

    public PktControllerSmartInterfaceUpdate(BlockPos controllerPos, String interfaceType, float value) {
        this.controllerPos = controllerPos;
        this.interfaceType = interfaceType == null ? "" : interfaceType;
        this.value = value;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.controllerPos = BlockPos.fromLong(buf.readLong());
        this.interfaceType = ByteBufUtils.readUTF8String(buf);
        this.value = buf.readFloat();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeLong(this.controllerPos.toLong());
        ByteBufUtils.writeUTF8String(buf, this.interfaceType);
        buf.writeFloat(this.value);
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
        if (!Float.isFinite(message.value)) {
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
        applySmartInterfaceUpdate(controller, message.controllerPos, interfaceType, message.value);
    }

    static boolean applySmartInterfaceUpdate(TileMultiblockMachineController controller, BlockPos controllerPos, String interfaceType, float value) {
        interfaceType = normalizeBounded(interfaceType, MAX_INTERFACE_TYPE_LENGTH);
        if (controller == null || controllerPos == null || interfaceType == null || !Float.isFinite(value)) {
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
            updated = true;
            break;
        }
        if (!updated && hasControllerCustomData(controller, interfaceType)) {
            updated = tryWriteControllerCustomData(controller, interfaceType, value);
        }
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

    private static boolean tryInvokeControllerSmartUpdate(TileMultiblockMachineController controller, String interfaceType, float value) {
        try {
            Method method = controller.getClass().getMethod("updateSmartInterfaceValue", String.class, float.class);
            Object result = method.invoke(controller, interfaceType, value);
            return !(result instanceof Boolean) || (Boolean) result;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean tryWriteControllerCustomData(TileMultiblockMachineController controller, String key, float value) {
        try {
            Method getTagMethod = controller.getClass().getMethod("getCustomDataTag");
            Method setTagMethod = controller.getClass().getMethod("setCustomDataTag", NBTTagCompound.class);

            NBTTagCompound tag = new NBTTagCompound();
            Object existing = getTagMethod.invoke(controller);
            if (existing instanceof NBTTagCompound) {
                tag = ((NBTTagCompound) existing).copy();
            }

            tag.setFloat(key, value);
            setTagMethod.invoke(controller, tag);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    static boolean hasControllerCustomData(TileMultiblockMachineController controller, String key) {
        try {
            Method getTagMethod = controller.getClass().getMethod("getCustomDataTag");
            Object existing = getTagMethod.invoke(controller);
            return existing instanceof NBTTagCompound && ((NBTTagCompound) existing).hasKey(key);
        } catch (Exception ignored) {
            return false;
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
