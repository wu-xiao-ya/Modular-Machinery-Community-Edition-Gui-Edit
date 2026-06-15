package com.fushu.mmceguiext.common.container;

import appeng.container.AEBaseContainer;
import appeng.container.slot.SlotDisabled;
import appeng.container.slot.SlotFake;
import com.fushu.mmceguiext.common.registry.CustomAEMixedInputBusRegistry;
import com.fushu.mmceguiext.common.tile.TileCustomAEMixedInputBus;
import com.fushu.mmceguiext.common.util.AECapacityCardSupport;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.items.IItemHandlerModifiable;

public class ContainerCustomAEMixedInputBus extends AEBaseContainer {
    private final TileCustomAEMixedInputBus owner;
    private final IItemHandlerModifiable capacityCardGuiInventory;

    public ContainerCustomAEMixedInputBus(TileCustomAEMixedInputBus owner, EntityPlayer opening) {
        super(opening.inventory, owner);
        this.owner = owner;
        this.capacityCardGuiInventory = this.owner.getCapacityCardInventory().asGUIAccess();
        CustomAEMixedInputBusRegistry.Def def = this.owner.getDefinition();
        CustomAEMixedInputBusRegistry.ComponentDef playerInv = findComponent(def, "player_inventory", null);
        int playerInventoryX = playerInv == null ? (def == null ? 8 : def.playerInventoryX) : playerInv.x;
        int playerInventoryY = playerInv == null ? (def == null ? 141 : def.playerInventoryY) : playerInv.y;
        this.bindPlayerInventory(getInventoryPlayer(), playerInventoryX, playerInventoryY);

        IItemHandlerModifiable internal = this.owner.getInternalInventory().asGUIAccess();
        int activeItemSlots = Math.min(internal.getSlots(), this.owner.getActiveItemSlots());
        for (int i = 0; i < activeItemSlots; i++) {
            if (def != null && !isItemSlotDefined(def, i)) {
                continue;
            }
            int x = -10000;
            int y = -10000;
            CustomAEMixedInputBusRegistry.ComponentDef component = findIndexedComponent(def, "slot", "item_storage", i);
            if (component == null) {
                component = findIndexedComponent(def, "slot", "item_output", i);
            }
            if (component == null && def != null) {
                CustomAEMixedInputBusRegistry.SlotPoint point = i < def.storageSlots.size() ? def.storageSlots.get(i) : null;
                if (point != null) {
                    x = point.x;
                    y = point.y;
                }
            } else if (component != null) {
                x = component.x;
                y = component.y;
            }
            this.addSlotToContainer(new SlotDisabled(internal, i, x, y));
        }

        IItemHandlerModifiable config = this.owner.getConfigInventory().asGUIAccess();
        int activeConfigSlots = Math.min(config.getSlots(), this.owner.getActiveItemSlots());
        for (int i = 0; i < activeConfigSlots; i++) {
            if (def != null && !isItemSlotDefined(def, i)) {
                continue;
            }
            int x = -10000;
            int y = -10000;
            CustomAEMixedInputBusRegistry.ComponentDef component = findIndexedComponent(def, "slot", "item_config", i);
            if (component == null && def != null && i < def.configSlots.size()) {
                CustomAEMixedInputBusRegistry.SlotPoint point = def.configSlots.get(i);
                if (point != null) {
                    x = point.x;
                    y = point.y;
                }
            } else if (component != null) {
                x = component.x;
                y = component.y;
            }
            this.addSlotToContainer(new SlotFake(config, i, x, y));
        }

        int activeCapacityCards = Math.min(this.capacityCardGuiInventory.getSlots(), this.owner.getActiveCapacityCardSlots());
        for (int i = 0; i < activeCapacityCards; i++) {
            CustomAEMixedInputBusRegistry.ComponentDef component = findIndexedComponent(def, "slot", "capacity_card", i);
            int x = -10000;
            int y = -10000;
            if (component != null) {
                x = component.x;
                y = component.y;
            } else if (def != null && i < def.capacityCardSlots.size()) {
                CustomAEMixedInputBusRegistry.SlotPoint point = def.capacityCardSlots.get(i);
                if (point != null) {
                    x = point.x;
                    y = point.y;
                }
            }
            this.addSlotToContainer(new SlotAEMixedCapacityCard(this.capacityCardGuiInventory, i, x, y));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return this.owner != null
            && !this.owner.isInvalid()
            && this.owner.getWorld() == playerIn.world
            && this.owner.getWorld().getTileEntity(this.owner.getPos()) == this.owner
            && playerIn.getDistanceSqToCenter(this.owner.getPos()) <= 64D;
    }

    public TileCustomAEMixedInputBus getOwner() {
        return this.owner;
    }

    private static CustomAEMixedInputBusRegistry.ComponentDef findIndexedComponent(CustomAEMixedInputBusRegistry.Def def, String type, String role, int index) {
        if (def == null || def.gui == null || def.gui.components == null) {
            return null;
        }
        for (CustomAEMixedInputBusRegistry.ComponentDef component : def.gui.components) {
            if (component == null) {
                continue;
            }
            if (!type.equalsIgnoreCase(component.type == null ? "" : component.type)) {
                continue;
            }
            if (!role.equalsIgnoreCase(component.role == null ? "" : component.role)) {
                continue;
            }
            if (component.index == index) {
                return component;
            }
        }
        return null;
    }

    private static boolean isItemSlotDefined(CustomAEMixedInputBusRegistry.Def def, int index) {
        return index >= 0
            && index < def.configSlots.size() && def.configSlots.get(index) != null
            && index < def.storageSlots.size() && def.storageSlots.get(index) != null;
    }

    private static CustomAEMixedInputBusRegistry.ComponentDef findComponent(CustomAEMixedInputBusRegistry.Def def, String type, String role) {
        if (def == null || def.gui == null || def.gui.components == null) {
            return null;
        }
        for (CustomAEMixedInputBusRegistry.ComponentDef component : def.gui.components) {
            if (component == null) {
                continue;
            }
            if (!type.equalsIgnoreCase(component.type == null ? "" : component.type)) {
                continue;
            }
            if (role != null && !role.equalsIgnoreCase(component.role == null ? "" : component.role)) {
                continue;
            }
            return component;
        }
        return null;
    }
}
