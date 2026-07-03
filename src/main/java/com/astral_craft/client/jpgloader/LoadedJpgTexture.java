package com.astral_craft.client.jpgloader;

import net.minecraft.resources.Identifier;

public record LoadedJpgTexture(Identifier textureId, int width, int height) { }