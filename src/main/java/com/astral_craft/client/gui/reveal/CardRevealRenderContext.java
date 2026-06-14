package com.astral_craft.client.gui.reveal;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public record CardRevealRenderContext(
        GuiGraphicsExtractor graphics, Minecraft minecraft,
        CardReveal reveal, CardRevealSettings settings,
        float ageTicks, int centerX, int centerY, int modelSize) { }