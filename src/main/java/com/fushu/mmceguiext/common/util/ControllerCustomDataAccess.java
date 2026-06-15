package com.fushu.mmceguiext.common.util;

import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTPrimitive;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;

import javax.annotation.Nullable;
import java.lang.reflect.Method;

public final class ControllerCustomDataAccess {
    private ControllerCustomDataAccess() {
    }

    @Nullable
    public static NBTTagCompound readTag(@Nullable TileMultiblockMachineController controller) {
        if (controller == null) {
            return null;
        }
        try {
            Method method = controller.getClass().getMethod("getCustomDataTag");
            Object existing = method.invoke(controller);
            return existing instanceof NBTTagCompound ? ((NBTTagCompound) existing).copy() : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    public static boolean writeNumber(@Nullable TileMultiblockMachineController controller, String key, float value) {
        return write(controller, key, Float.valueOf(value));
    }

    public static boolean writeString(@Nullable TileMultiblockMachineController controller, String key, String value) {
        return write(controller, key, value == null ? "" : value);
    }

    public static boolean replaceTag(@Nullable TileEntity tile, @Nullable NBTTagCompound value) {
        if (!(tile instanceof TileMultiblockMachineController)) {
            return false;
        }
        try {
            Method setTagMethod = tile.getClass().getMethod("setCustomDataTag", NBTTagCompound.class);
            setTagMethod.invoke(tile, value == null ? new NBTTagCompound() : value.copy());
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    public static boolean hasKey(@Nullable TileMultiblockMachineController controller, String key) {
        NBTTagCompound tag = readTag(controller);
        return tag != null && tag.hasKey(key);
    }

    @Nullable
    public static Float readNumber(@Nullable TileMultiblockMachineController controller, String key) {
        NBTTagCompound tag = readTag(controller);
        if (tag == null || !tag.hasKey(key)) {
            return null;
        }
        NBTBase value = tag.getTag(key);
        return value instanceof NBTPrimitive ? Float.valueOf(((NBTPrimitive) value).getFloat()) : null;
    }

    @Nullable
    public static String readString(@Nullable TileMultiblockMachineController controller, String key) {
        NBTTagCompound tag = readTag(controller);
        if (tag == null || !tag.hasKey(key)) {
            return null;
        }
        NBTBase value = tag.getTag(key);
        return value != null && value.getId() == 8 ? tag.getString(key) : null;
    }

    public static boolean write(@Nullable TileMultiblockMachineController controller, String key, Object value) {
        if (controller == null || key == null || key.trim().isEmpty()) {
            return false;
        }
        try {
            Method getTagMethod = controller.getClass().getMethod("getCustomDataTag");
            Method setTagMethod = controller.getClass().getMethod("setCustomDataTag", NBTTagCompound.class);

            NBTTagCompound tag = new NBTTagCompound();
            Object existing = getTagMethod.invoke(controller);
            if (existing instanceof NBTTagCompound) {
                tag = ((NBTTagCompound) existing).copy();
            }

            String normalizedKey = key.trim();
            if (value instanceof Number) {
                tag.setFloat(normalizedKey, ((Number) value).floatValue());
            } else {
                tag.setString(normalizedKey, value == null ? "" : value.toString());
            }
            setTagMethod.invoke(controller, tag);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
