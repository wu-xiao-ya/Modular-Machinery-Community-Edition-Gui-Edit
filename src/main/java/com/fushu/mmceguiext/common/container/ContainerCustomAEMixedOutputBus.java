package com.fushu.mmceguiext.common.container;

import appeng.container.AEBaseContainer;
import appeng.container.slot.SlotDisabled;
import com.fushu.mmceguiext.common.registry.CustomAEMixedOutputBusRegistry;
import com.fushu.mmceguiext.common.tile.TileCustomAEMixedOutputBus;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nonnull;

public class ContainerCustomAEMixedOutputBus extends AEBaseContainer {
    private final TileCustomAEMixedOutputBus owner;

    public ContainerCustomAEMixedOutputBus(TileCustomAEMixedOutputBus owner, EntityPlayer opening) {
        super(opening.inventory, owner);
        this.owner = owner;
        CustomAEMixedOutputBusRegistry.Def def = this.owner.getDefinition();
        CustomAEMixedOutputBusRegistry.ComponentDef playerInv = findComponent(def, "player_inventory", null);
        int playerInventoryX = playerInv == null ? 8 : playerInv.x;
        int playerInventoryY = playerInv == null ? 123 : playerInv.y;
        this.bindPlayerInventory(getInventoryPlayer(), playerInventoryX, playerInventoryY);

        IItemHandlerModifiable internal = this.owner.getInventory().asGUIAccess();
        for (int i = 0; i < internal.getSlots(); i++) {
            int x = -10000;
            int y = -10000;
            CustomAEMixedOutputBusRegistry.ComponentDef component = findIndexedComponent(def, "slot", "item_storage", i);
            if (component == null) {
                component = findIndexedComponent(def, "slot", "item_output", i);
            }
            if (component != null) {
                x = component.x;
                y = component.y;
            }
            this.addSlotToContainer(new SlotDisabled(internal, i, x, y));
        }
    }

    public TileCustomAEMixedOutputBus getOwner() {
        return owner;
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return this.owner != null
            && !this.owner.isInvalid()
            && this.owner.getWorld() == playerIn.world
            && this.owner.getWorld().getTileEntity(this.owner.getPos()) == this.owner
            && playerIn.getDistanceSqToCenter(this.owner.getPos()) <= 64D;
    }

    @Nonnull
    @Override
    public ItemStack transferStackInSlot(@Nonnull EntityPlayer playerIn, int index) {
        return ItemStack.EMPTY;
    }

    private static CustomAEMixedOutputBusRegistry.ComponentDef findIndexedComponent(CustomAEMixedOutputBusRegistry.Def def, String type, String role, int index) {
        if (def == null || def.gui == null || def.gui.components == null) {
            return null;
        }
        for (CustomAEMixedOutputBusRegistry.ComponentDef component : def.gui.components) {
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

    private static CustomAEMixedOutputBusRegistry.ComponentDef findComponent(CustomAEMixedOutputBusRegistry.Def def, String type, String role) {
        if (def == null || def.gui == null || def.gui.components == null) {
            return null;
        }
        for (CustomAEMixedOutputBusRegistry.ComponentDef component : def.gui.components) {
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
