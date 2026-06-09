package com.fushu.mmceguiext.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import java.nio.charset.StandardCharsets;

final class NetworkBufferUtils {
    private NetworkBufferUtils() {
    }

    static String readBoundedUtf8(ByteBuf buf, int maxChars) {
        if (buf == null || maxChars <= 0) {
            return "";
        }
        int maxBytes = maxChars * 4;
        int byteLength = ByteBufUtils.readVarInt(buf, 2);
        if (byteLength < 0) {
            return "";
        }
        if (byteLength > maxBytes || byteLength > buf.readableBytes()) {
            buf.skipBytes(Math.min(Math.max(byteLength, 0), buf.readableBytes()));
            return "";
        }
        String value = buf.toString(buf.readerIndex(), byteLength, StandardCharsets.UTF_8);
        buf.skipBytes(byteLength);
        return value.length() <= maxChars ? value : "";
    }

    static void writeBoundedUtf8(ByteBuf buf, String value, int maxChars) {
        String out = value == null ? "" : value;
        if (maxChars > 0 && out.length() > maxChars) {
            out = out.substring(0, maxChars);
        }
        ByteBufUtils.writeUTF8String(buf, out);
    }
}
