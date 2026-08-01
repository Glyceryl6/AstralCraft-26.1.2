package com.astral_craft.client.jpgloader;

import com.mojang.blaze3d.platform.NativeImage;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class JpegNativeImageReader {

    private static final long MAX_PIXELS = 8192L * 8192L;
    private static final byte[] PNG_SIGNATURE = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    public static NativeImage readJpeg(byte[] bytes) throws IOException {
        if (!looksLikeJpeg(bytes)) throw new IOException("The supplied bytes do not look like a JPEG file.");

        ByteBuffer encoded = MemoryUtil.memAlloc(bytes.length);
        try {
            encoded.put(bytes).flip();
            try (MemoryStack stack = MemoryStack.stackPush()) {
                IntBuffer width = stack.mallocInt(1);
                IntBuffer height = stack.mallocInt(1);
                IntBuffer channels = stack.mallocInt(1);
                if (!STBImage.stbi_info_from_memory(encoded, width, height, channels)) {
                    throw new IOException("Failed to inspect JPEG: " + STBImage.stbi_failure_reason());
                }

                long pixels = (long) width.get(0) * (long) height.get(0);
                if (width.get(0) <= 0 || height.get(0) <= 0 || pixels > MAX_PIXELS) {
                    throw new IOException("Refusing to load JPEG with invalid or excessive size: " + width.get(0) + "x" + height.get(0));
                }

                encoded.rewind();
                ByteBuffer decoded = STBImage.stbi_load_from_memory(encoded, width, height, channels, 4);
                if (decoded == null) throw new IOException("Failed to decode JPEG: " + STBImage.stbi_failure_reason());
                return new NativeImage(NativeImage.Format.RGBA, width.get(0), height.get(0), true, MemoryUtil.memAddress(decoded));
            }
        } finally {
            MemoryUtil.memFree(encoded);
        }
    }

    public static boolean looksLikeJpeg(byte[] bytes) {
        return bytes != null && bytes.length >= 3
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF;
    }

    public static boolean looksLikePng(byte[] bytes) {
        if (bytes == null || bytes.length < PNG_SIGNATURE.length) return false;
        for (int index = 0; index < PNG_SIGNATURE.length; index++) {
            if (bytes[index] != PNG_SIGNATURE[index]) return false;
        }
        return true;
    }

    public static boolean looksLikeSupportedTexture(byte[] bytes) {
        return looksLikePng(bytes) || looksLikeJpeg(bytes);
    }

}