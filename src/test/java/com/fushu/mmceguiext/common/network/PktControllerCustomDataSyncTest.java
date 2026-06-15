package com.fushu.mmceguiext.common.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.BlockPos;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PktControllerCustomDataSyncTest {
    @Test
    public void packetRoundTripsControllerPositionAndCustomData() throws Exception {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setFloat("count", 3.0F);
        tag.setString("status", "ready");
        BlockPos pos = new BlockPos(12, 64, -5);

        PktControllerCustomDataSync out = new PktControllerCustomDataSync(pos, tag);
        ByteBuf buffer = Unpooled.buffer();
        out.toBytes(buffer);

        PktControllerCustomDataSync in = new PktControllerCustomDataSync();
        in.fromBytes(buffer);

        assertEquals(pos, getField(in, "controllerPos"));
        NBTTagCompound decoded = (NBTTagCompound) getField(in, "customData");
        assertTrue(decoded.hasKey("count"));
        assertEquals(3.0F, decoded.getFloat("count"), 0.0001F);
        assertEquals("ready", decoded.getString("status"));
    }

    private static Object getField(Object target, String name) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }
}
