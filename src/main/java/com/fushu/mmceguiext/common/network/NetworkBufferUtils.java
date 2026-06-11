package com.fushu.mmceguiext.common.network;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraftforge.fml.common.network.ByteBufUtils;

import java.nio.charset.StandardCharsets;

final class NetworkBufferUtils {
    private NetworkBufferUtils() {
    }

    static void requireReadable(ByteBuf buf, int bytes) {
        if (buf == null || bytes < 0 || buf.readableBytes() < bytes) {
            throw new DecoderException("Packet is shorter than expected");
        }
    }

    static String readBoundedUtf8(ByteBuf buf, int maxChars) {
        if (buf == null || maxChars <= 0) {
            return "";
        }
        int maxBytes = maxChars * 4;
        int byteLength = ByteBufUtils.readVarInt(buf, 2);
        if (byteLength < 0) {
            throw new DecoderException("Negative string length");
        }
        if (byteLength > maxBytes || byteLength > buf.readableBytes()) {
            throw new DecoderException("String length exceeds packet bounds");
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
