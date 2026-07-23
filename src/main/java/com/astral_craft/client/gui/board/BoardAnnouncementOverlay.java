package com.astral_craft.client.gui.board;

import com.astral_craft.AstralCraft;
import com.astral_craft.client.util.ClientAnimationClock;
import com.astral_craft.common.network.s2c.BoardAnnouncementPayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class BoardAnnouncementOverlay {

    public static final Identifier LAYER = AstralCraft.prefix("board_announcement");
    protected static Announcement active;

    public static void show(BoardAnnouncementPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> show(payload));
    }

    public static void show(BoardAnnouncementPayload payload) {
        active = new Announcement(payload, ClientAnimationClock.nowTicks());
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) return;
        SoundEvent sound = BuiltInRegistries.SOUND_EVENT.getValue(payload.sound());
        if (sound != null) minecraft.player.playSound(sound, 1.0F, 1.0F);
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (active == null) return;
        Minecraft minecraft = Minecraft.getInstance();
        float age = ClientAnimationClock.elapsedTicks(active.startedAtTick());
        if (age >= active.payload().durationTicks() || minecraft.player == null) {
            active = null;
            return;
        }

        float intro = Mth.clamp(age / 8.0F, 0.0F, 1.0F);
        float outro = Mth.clamp((active.payload().durationTicks() - age) / 10.0F, 0.0F, 1.0F);
        float shown = Math.min(intro, outro);
        float scale = 1.35F + (1.0F - shown) * 0.25F;
        int centerX = graphics.guiWidth() / 2;
        int centerY = graphics.guiHeight() / 2 - 20;
        int alpha = Math.clamp(Math.round(shown * 255.0F), 0, 255);
        int titleColor = alpha << 24 | 0x00FFFFFF;
        int subtitleColor = alpha << 24 | 0x00FFD27D;
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, centerY);
        graphics.pose().scale(scale, scale);
        graphics.centeredText(minecraft.font, active.payload().title(), 0, -5, titleColor);
        graphics.pose().popMatrix();
        if (!active.payload().subtitle().getString().isBlank()) {
            graphics.centeredText(minecraft.font, active.payload().subtitle(), centerX, centerY + 18, subtitleColor);
        }
    }

    protected record Announcement(BoardAnnouncementPayload payload, double startedAtTick) {}

}