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
    private static final Set<Identifier> CHECKED_STATIC_TEXTURES = new HashSet<>();
    private static final Set<Identifier> UNAVAILABLE_TEXTURES = new HashSet<>();

    public static LoadedJpgTexture getOrLoad(Identifier jpgResource) throws IOException {
        LoadedJpgTexture cached = CACHE.get(jpgResource);
        if (cached != null) return cached;
        byte[] bytes = readResource(jpgResource);
        return load(jpgResource, bytes);
    }

    /** Resolves a resource-backed PNG/JPEG texture. JPEGs are registered dynamically under the original id. */
    public static Identifier resolve(Identifier resourceId) {
        if (resourceId == null) return null;
        ensureAvailable(resourceId);
        return resourceId;
    }

    /**
     * Resolves a client-side appearance texture and falls back when the selected resource is unavailable.
     * This is important for persisted selections from removed resource packs and for other players whose
     * custom card-back resource does not exist on the local client.
     */
    public static Identifier resolveOrFallback(Identifier resourceId, Identifier fallbackId) {
        if (ensureAvailable(resourceId)) return resourceId;
        if (ensureAvailable(fallbackId)) return fallbackId;
        return fallbackId != null ? fallbackId : resourceId;
    }

    public static boolean isSupportedTexture(Identifier resourceId) {
        return ensureAvailable(resourceId);
    }

    public static void clear() {
        Minecraft minecraft = Minecraft.getInstance();
        TextureManager textureManager = minecraft.getTextureManager();
        for (LoadedJpgTexture loaded : CACHE.values()) textureManager.release(loaded.textureId());
        CACHE.clear();
        CHECKED_STATIC_TEXTURES.clear();
        UNAVAILABLE_TEXTURES.clear();
    }

    private static boolean ensureAvailable(Identifier resourceId) {
        if (resourceId == null) return false;
        if (CACHE.containsKey(resourceId) || CHECKED_STATIC_TEXTURES.contains(resourceId)) return true;
        if (UNAVAILABLE_TEXTURES.contains(resourceId)) return false;
        try {
            byte[] bytes = readResource(resourceId);
            if (!JpegNativeImageReader.looksLikeSupportedTexture(bytes)) {
                UNAVAILABLE_TEXTURES.add(resourceId);
                return false;
            }
            if (JpegNativeImageReader.looksLikeJpeg(bytes)) load(resourceId, bytes);
            else CHECKED_STATIC_TEXTURES.add(resourceId);
            return true;
        } catch (IOException ignored) {
            UNAVAILABLE_TEXTURES.add(resourceId);
            return false;
        }
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
        UNAVAILABLE_TEXTURES.remove(resourceId);
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
