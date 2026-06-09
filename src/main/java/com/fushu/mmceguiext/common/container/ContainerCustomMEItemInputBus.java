package com.fushu.mmceguiext.common.container;

import appeng.container.AEBaseContainer;
import appeng.container.slot.SlotDisabled;
import appeng.container.slot.SlotFake;
import com.fushu.mmceguiext.common.registry.CustomAEItemInputBusRegistry;
import com.fushu.mmceguiext.common.tile.TileCustomMEItemInputBus;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.util.HashSet;
import java.util.Set;

public class ContainerCustomMEItemInputBus extends AEBaseContainer {
    private final TileCustomMEItemInputBus owner;
    private final Set<Integer> configSlotNumbers = new HashSet<Integer>();

    public ContainerCustomMEItemInputBus(final TileCustomMEItemInputBus owner, final EntityPlayer player) {
        super(player.inventory, owner);
        this.owner = owner;
        CustomAEItemInputBusRegistry.Def def = owner.getDefinition();
        int playerInventoryX = def == null ? 0 : def.playerInventoryX;
        int playerInventoryY = def == null ? 123 : def.playerInventoryY;
        bindPlayerInventory(getInventoryPlayer(), playerInventoryX, playerInventoryY);

        IItemHandlerModifiable config = owner.getConfigInventory().asGUIAccess();
        int activeConfigSlots = Math.min(config.getSlots(), owner.getActiveSlots());
        for (int i = 0; i < activeConfigSlots; i++) {
            CustomAEItemInputBusRegistry.SlotPoint point = def != null && i < def.configSlots.size() ? def.configSlots.get(i) : null;
            int x = point == null ? -10000 : point.x;
            int y = point == null ? -10000 : point.y;
            Slot slot = addSlotToContainer(new SlotFake(config, i, x, y));
            this.configSlotNumbers.add(slot.slotNumber);
        }

        IItemHandlerModifiable internal = owner.getInternalInventory().asGUIAccess();
        int activeInternalSlots = Math.min(internal.getSlots(), owner.getActiveSlots());
        for (int i = 0; i < activeInternalSlots; i++) {
            CustomAEItemInputBusRegistry.SlotPoint point = def != null && i < def.storageSlots.size() ? def.storageSlots.get(i) : null;
            int x = point == null ? -10000 : point.x;
            int y = point == null ? -10000 : point.y;
            addSlotToContainer(new SlotDisabled(internal, i, x, y));
        }
    }

    public TileCustomMEItemInputBus getOwner() {
        return owner;
    }

    public boolean isConfigSlotNumber(int slotNumber) {
        return this.configSlotNumbers.contains(slotNumber);
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return this.owner != null
            && !this.owner.isInvalid()
            && this.owner.getWorld() == playerIn.world
            && this.owner.getWorld().getTileEntity(this.owner.getPos()) == this.owner
            && playerIn.getDistanceSqToCenter(this.owner.getPos()) <= 64D;
    }
}
