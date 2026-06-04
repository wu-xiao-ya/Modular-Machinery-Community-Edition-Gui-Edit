package com.fushu.mmceguiext.common.container;

import appeng.container.AEBaseContainer;
import appeng.container.slot.SlotDisabled;
import appeng.container.slot.SlotFake;
import com.fushu.mmceguiext.common.registry.CustomAEItemInputBusRegistry;
import com.fushu.mmceguiext.common.tile.TileCustomMEItemInputBus;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.items.IItemHandlerModifiable;

public class ContainerCustomMEItemInputBus extends AEBaseContainer {
    private final TileCustomMEItemInputBus owner;

    public ContainerCustomMEItemInputBus(final TileCustomMEItemInputBus owner, final EntityPlayer player) {
        super(player.inventory, owner);
        this.owner = owner;
        CustomAEItemInputBusRegistry.Def def = owner.getDefinition();
        int playerInventoryX = def == null ? 0 : def.playerInventoryX;
        int playerInventoryY = def == null ? 123 : def.playerInventoryY;
        bindPlayerInventory(getInventoryPlayer(), playerInventoryX, playerInventoryY);

        IItemHandlerModifiable config = owner.getConfigInventory().asGUIAccess();
        for (int i = 0; i < config.getSlots(); i++) {
            CustomAEItemInputBusRegistry.SlotPoint point = def != null && i < def.configSlots.size() ? def.configSlots.get(i) : null;
            int x = point == null ? -10000 : point.x;
            int y = point == null ? -10000 : point.y;
            addSlotToContainer(new SlotFake(config, i, x, y));
        }

        IItemHandlerModifiable internal = owner.getInternalInventory().asGUIAccess();
        for (int i = 0; i < internal.getSlots(); i++) {
            CustomAEItemInputBusRegistry.SlotPoint point = def != null && i < def.storageSlots.size() ? def.storageSlots.get(i) : null;
            int x = point == null ? -10000 : point.x;
            int y = point == null ? -10000 : point.y;
            addSlotToContainer(new SlotDisabled(internal, i, x, y));
        }
    }

    public TileCustomMEItemInputBus getOwner() {
        return owner;
    }
}
