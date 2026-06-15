package com.fushu.mmceguiext.common.integration.crafttweaker;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.data.IData;
import crafttweaker.api.minecraft.CraftTweakerMC;
import github.kasuminova.mmce.common.event.machine.MachineEvent;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.nbt.NBTTagCompound;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;
import stanhebben.zenscript.annotations.ZenMethod;
import stanhebben.zenscript.annotations.ZenSetter;

@ZenRegister
@ZenClass("mods.mmceguiext.ControllerButtonClickEvent")
public class ControllerButtonClickEvent extends MachineEvent {
    private final String buttonId;

    public ControllerButtonClickEvent(TileMultiblockMachineController controller, String buttonId) {
        super(controller);
        this.buttonId = buttonId == null ? "" : buttonId;
    }

    @ZenGetter("buttonId")
    public String getButtonId() {
        return buttonId;
    }

    @ZenGetter("customData")
    public IData getCustomData() {
        NBTTagCompound tag = controller.getCustomDataTag();
        return CraftTweakerMC.getIDataModifyable(tag == null ? new NBTTagCompound() : tag);
    }

    @ZenSetter("customData")
    public void setCustomData(IData data) {
        if (data == null) {
            return;
        }
        NBTTagCompound tag = CraftTweakerMC.getNBTCompound(data);
        controller.setCustomDataTag(tag == null ? new NBTTagCompound() : tag);
        syncController();
    }

    @ZenMethod
    public boolean hasCustomKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return false;
        }
        NBTTagCompound tag = controller.getCustomDataTag();
        return tag != null && tag.hasKey(key.trim());
    }

    @ZenMethod
    public float getCustomFloat(String key) {
        return getCustomFloat(key, 0.0F);
    }

    @ZenMethod
    public float getCustomFloat(String key, float fallback) {
        if (key == null || key.trim().isEmpty()) {
            return fallback;
        }
        NBTTagCompound tag = controller.getCustomDataTag();
        String normalizedKey = key.trim();
        return tag != null && tag.hasKey(normalizedKey) ? tag.getFloat(normalizedKey) : fallback;
    }

    @ZenMethod
    public void setCustomFloat(String key, float value) {
        if (key == null || key.trim().isEmpty() || !Float.isFinite(value)) {
            return;
        }
        NBTTagCompound existing = controller.getCustomDataTag();
        NBTTagCompound tag = existing == null ? new NBTTagCompound() : existing.copy();
        tag.setFloat(key.trim(), value);
        controller.setCustomDataTag(tag);
        syncController();
    }

    @ZenMethod
    public float addCustomFloat(String key, float delta) {
        if (key == null || key.trim().isEmpty() || !Float.isFinite(delta)) {
            return 0.0F;
        }
        float value = getCustomFloat(key, 0.0F) + delta;
        setCustomFloat(key, value);
        return value;
    }

    @ZenMethod
    public void syncController() {
        controller.markForUpdateSync();
    }
}
