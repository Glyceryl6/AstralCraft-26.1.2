package com.astral_craft.client.gui;

import com.astral_craft.common.registry.AstralStatusEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.client.extensions.common.IClientMobEffectExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.jspecify.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;

@ParametersAreNonnullByDefault
public class AstralStatusMobEffectClientExtensions implements IClientMobEffectExtensions {

    protected final Identifier statusId;

    public AstralStatusMobEffectClientExtensions(Identifier statusId) {
        this.statusId = statusId;
    }

    public static void register(RegisterClientExtensionsEvent event) {
        for (AstralStatusEffects.MobEffectIconEntry entry : AstralStatusEffects.mobEffectIconEntries()) {
            event.registerMobEffect(new AstralStatusMobEffectClientExtensions(entry.statusId()), entry.holder());
        }
    }

    @Override
    public boolean renderInventoryIcon(MobEffectInstance instance, AbstractContainerScreen<?> screen, GuiGraphicsExtractor graphics, int x, int y, int blitOffset) {
        AstralStatusIconRenderer.renderMobEffectIcon(graphics, this.statusId, x, y + 7, AstralStatusIconRenderer.MOB_EFFECT_ICON_SIZE, 255);
        return true;
    }

    @Override
    public boolean renderGuiIcon(MobEffectInstance instance, Gui gui, GuiGraphicsExtractor graphics, int x, int y, float z, float alpha) {
        int a = Math.round(Math.clamp(alpha, 0.0F, 1.0F) * 255.0F);
        AstralStatusIconRenderer.renderMobEffectIcon(graphics, this.statusId, x + 3, y + 3, AstralStatusIconRenderer.MOB_EFFECT_ICON_SIZE, a);
        this.renderDuration(graphics, instance, x + 3, y + AstralStatusIconRenderer.MOB_EFFECT_ICON_SIZE + 3, a);
        return true;
    }

    protected void renderDuration(GuiGraphicsExtractor graphics, @Nullable MobEffectInstance instance, int x, int y, int alpha) {
        if (instance == null) return;
        int ticks = Math.max(0, instance.getDuration());
        if (ticks == 0 || alpha <= 10) return;
        Font font = Minecraft.getInstance().font;
        Component text = Component.literal(this.formatDuration(ticks));
        int color = (Math.clamp(alpha, 0, 255) << 24) | 0xFFFFFF;
        graphics.text(font, text, x + AstralStatusIconRenderer.MOB_EFFECT_ICON_SIZE / 2 - font.width(text) / 2, y, color, true);
    }

    protected String formatDuration(int ticks) {
        int seconds = Math.max(1, (int) Math.ceil(ticks / 20.0D));
        if (seconds < 60) {
            return String.valueOf(seconds);
        }

        int minutes = seconds / 60;
        int rest = seconds % 60;
        return minutes + ":" + (rest < 10 ? "0" : "") + rest;
    }

}
