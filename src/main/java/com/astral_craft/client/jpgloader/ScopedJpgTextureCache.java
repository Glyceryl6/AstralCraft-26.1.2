package com.astral_craft.client.jpgloader;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class ScopedJpgTextureCache {

    private static final Map<Identifier, LoadedJpgTexture> CACHE = new HashMap<>();
    private static final Set<Identifier> CHECKED_NON_JPEG = new HashSet<>();

    public static LoadedJpgTexture getOrLoad(Identifier jpgResource) throws IOException {
        LoadedJpgTexture cached = CACHE.get(jpgResource);
        if (cached != null) return cached;
        byte[] bytes = readResource(jpgResource);
        return load(jpgResource, bytes);
    }

    /** Resolves a resource-backed PNG/JPEG texture. JPEGs are registered dynamically under the original id. */
    public static Identifier resolve(Identifier resourceId) {
        if (resourceId == null || CACHE.containsKey(resourceId) || CHECKED_NON_JPEG.contains(resourceId)) return resourceId;
        try {
            byte[] bytes = readResource(resourceId);
            if (JpegNativeImageReader.looksLikeJpeg(bytes)) load(resourceId, bytes);
            else CHECKED_NON_JPEG.add(resourceId);
        } catch (IOException ignored) {
            CHECKED_NON_JPEG.add(resourceId);
        }
        return resourceId;
    }

    public static boolean isSupportedTexture(Identifier resourceId) {
        try {
            byte[] bytes = readResource(resourceId);
            if (JpegNativeImageReader.looksLikeJpeg(bytes)) load(resourceId, bytes);
            return JpegNativeImageReader.looksLikeSupportedTexture(bytes);
        } catch (IOException ignored) {
            return false;
        }
    }

    public static void clear() {
        Minecraft minecraft = Minecraft.getInstance();
        TextureManager textureManager = minecraft.getTextureManager();
        for (LoadedJpgTexture loaded : CACHE.values()) textureManager.release(loaded.textureId());
        CACHE.clear();
        CHECKED_NON_JPEG.clear();
    }

    private static LoadedJpgTexture load(Identifier resourceId, byte[] bytes) throws IOException {
        LoadedJpgTexture cached = CACHE.get(resourceId);
        if (cached != null) return cached;
        NativeImage image = JpegNativeImageReader.readJpeg(bytes);
        int width = image.getWidth();
        int height = image.getHeight();
        DynamicTexture texture = new DynamicTexture(() -> "Scoped JPG " + resourceId, image);
        Minecraft.getInstance().getTextureManager().register(resourceId, texture);
        LoadedJpgTexture loaded = new LoadedJpgTexture(resourceId, width, height);
        CACHE.put(resourceId, loaded);
        return loaded;
    }

    private static byte[] readResource(Identifier resourceId) throws IOException {
        Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(resourceId);
        if (resource.isEmpty()) throw new IOException("Missing scoped texture resource: " + resourceId);
        try (InputStream input = resource.get().open()) {
            return input.readAllBytes();
        }
    }
}
