package com.astral_craft.client.jpgloader;

import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jspecify.annotations.NonNull;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class JpgCacheReloadListener extends SimplePreparableReloadListener<Void> {

    public static final JpgCacheReloadListener INSTANCE = new JpgCacheReloadListener();

    @Override
    protected @NonNull Void prepare(ResourceManager resourceManager, ProfilerFiller profiler) {
        return null;
    }

    @Override
    protected void apply(Void object, ResourceManager resourceManager, ProfilerFiller profiler) {
        ScopedJpgTextureCache.clear();
    }

}