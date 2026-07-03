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

    public static NativeImage readJpeg(byte[] bytes) throws IOException {
        if (!looksLikeJpeg(bytes)) {
            throw new IOException("The supplied bytes do not look like a JPEG file.");
        }

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
                if (decoded == null) {
                    throw new IOException("Failed to decode JPEG: " + STBImage.stbi_failure_reason());
                }

                // useStb=true lets NativeImage.close() free this pointer through stb_image_free.
                return new NativeImage(NativeImage.Format.RGBA, width.get(0), height.get(0), true, MemoryUtil.memAddress(decoded));
            }
        } finally {
            MemoryUtil.memFree(encoded);
        }
    }

    private static boolean looksLikeJpeg(byte[] bytes) {
        return bytes.length >= 4
                && (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[bytes.length - 2] & 0xFF) == 0xFF
                && (bytes[bytes.length - 1] & 0xFF) == 0xD9;
    }

}