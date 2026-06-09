package com.fushu.mmceguiext.common.network;

import appeng.container.slot.SlotFake;
import com.fushu.mmceguiext.common.container.ContainerCustomMEItemInputBus;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class PktCustomMEItemInputBusInvAction implements IMessage, IMessageHandler<PktCustomMEItemInputBusInvAction, IMessage> {
    private static final int MAX_REQUEST_AMOUNT = 1_000_000_000;

    private int newAmount;
    private int slotID;

    public PktCustomMEItemInputBusInvAction() {
    }

    public PktCustomMEItemInputBusInvAction(final int newAmount, final int slotID) {
        this.newAmount = newAmount;
        this.slotID = slotID;
    }

    @Override
    public void fromBytes(final ByteBuf buf) {
        this.newAmount = buf.readInt();
        this.slotID = buf.readInt();
    }

    @Override
    public void toBytes(final ByteBuf buf) {
        buf.writeInt(this.newAmount);
        buf.writeInt(this.slotID);
    }

    @Override
    public IMessage onMessage(final PktCustomMEItemInputBusInvAction message, final MessageContext ctx) {
        EntityPlayerMP player = ctx.getServerHandler().player;
        if (player == null) {
            return null;
        }
        player.getServerWorld().addScheduledTask(() -> handle(message, player));
        return null;
    }

    private static void handle(final PktCustomMEItemInputBusInvAction message, final EntityPlayerMP player) {
        if (!(player.openContainer instanceof ContainerCustomMEItemInputBus)) {
            return;
        }
        ContainerCustomMEItemInputBus container = (ContainerCustomMEItemInputBus) player.openContainer;
        if (!container.canInteractWith(player)) {
            return;
        }
        if (message.slotID < 0 || message.slotID >= container.inventorySlots.size()) {
            return;
        }
        if (!container.isConfigSlotNumber(message.slotID)) {
            return;
        }

        Slot slot = container.getSlot(message.slotID);
        if (!(slot instanceof SlotFake)) {
            return;
        }

        ItemStack stack = slot.getStack();
        if (stack.isEmpty() || message.newAmount <= 0) {
            return;
        }
        if (message.newAmount > MAX_REQUEST_AMOUNT) {
            return;
        }

        ItemStack newStack = stack.copy();
        newStack.setCount(message.newAmount);
        slot.putStack(newStack);
    }
}
