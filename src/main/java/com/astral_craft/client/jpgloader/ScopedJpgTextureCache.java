package com.astral_craft.client.jpgloader;

import com.astral_craft.AstralCraft;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ScopedJpgTextureCache {

    private static final Map<Identifier, LoadedJpgTexture> CACHE = new HashMap<>();

    public static LoadedJpgTexture getOrLoad(Identifier jpgResource) throws IOException {
        LoadedJpgTexture cached = CACHE.get(jpgResource);
        if (cached != null) return cached;
        Minecraft minecraft = Minecraft.getInstance();
        Optional<Resource> resource = minecraft.getResourceManager().getResource(jpgResource);
        if (resource.isEmpty()) {
            throw new IOException("Missing scoped JPG resource: " + jpgResource);
        }

        NativeImage image;
        try (InputStream input = resource.get().open()) {
            image = JpegNativeImageReader.readJpeg(input.readAllBytes());
        }

        int width = image.getWidth();
        int height = image.getHeight();
        Identifier dynamicId = dynamicTextureId(jpgResource);
        DynamicTexture texture = new DynamicTexture(() -> "Scoped JPG " + jpgResource, image);
        TextureManager textureManager = minecraft.getTextureManager();
        textureManager.register(dynamicId, texture);
        LoadedJpgTexture loaded = new LoadedJpgTexture(dynamicId, width, height);
        CACHE.put(jpgResource, loaded);
        return loaded;
    }

    public static void clear() {
        Minecraft minecraft = Minecraft.getInstance();
        TextureManager textureManager = minecraft.getTextureManager();
        for (LoadedJpgTexture loaded : CACHE.values()) {
            textureManager.release(loaded.textureId());
        }

        CACHE.clear();
    }

    private static Identifier dynamicTextureId(Identifier source) {
        String sanitized = source.getNamespace() + "/" + source.getPath();
        sanitized = sanitized.replace('.', '_').replace('/', '_');
        return Identifier.fromNamespaceAndPath(AstralCraft.MOD_ID, "dynamic/jpg/" + sanitized);
    }

}