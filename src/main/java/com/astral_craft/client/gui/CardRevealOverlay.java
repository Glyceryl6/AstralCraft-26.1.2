package com.astral_craft.client.gui;

import com.astral_craft.AstralCraft;
import com.astral_craft.client.gui.reveal.ApproachCardRevealAnimation;
import com.astral_craft.client.gui.reveal.CardReveal;
import com.astral_craft.client.gui.reveal.CardRevealAnimation;
import com.astral_craft.client.gui.reveal.CardRevealRenderContext;
import com.astral_craft.client.gui.reveal.CardRevealRenderer;
import com.astral_craft.client.gui.reveal.CardRevealSettings;
import com.astral_craft.client.gui.reveal.FlipCardRevealAnimation;
import com.astral_craft.common.network.CardRevealPayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;

/** Totem-like card reveal manager. Individual animation implementations live in client.gui.reveal. */
public class CardRevealOverlay {

    public static final Identifier LAYER = AstralCraft.prefix("card_reveal_overlay");
    public static final long TICK_NANOS = 50_000_000L;

    public static final CardRevealSettings SETTINGS = new CardRevealSettings();

    private static final Map<String, CardRevealAnimation> ANIMATIONS = new HashMap<>();
    private static final CardRevealRenderer RENDERER = new CardRevealRenderer();
    private static CardReveal active;

    static {
        registerAnimation(new FlipCardRevealAnimation());
        registerAnimation(new ApproachCardRevealAnimation());
    }

    public static void registerAnimation(CardRevealAnimation animation) {
        ANIMATIONS.put(animation.id(), animation);
    }

    public static void configureFlipTimings(int introHold, int flip, int outroHold) {
        SETTINGS.flipIntroHoldTicks = Math.max(0, introHold);
        SETTINGS.flipRotateTicks = Math.max(1, flip);
        SETTINGS.flipOutroHoldTicks = Math.max(0, outroHold);
    }

    public static void configureApproachTimings(int inTicks, int holdTicks, int outTicks) {
        SETTINGS.approachInTicks = Math.max(1, inTicks);
        SETTINGS.approachHoldTicks = Math.max(0, holdTicks);
        SETTINGS.approachOutTicks = Math.max(1, outTicks);
    }

    public static int defaultFlipDurationTicks() {
        return SETTINGS.flipDurationTicks();
    }

    public static int defaultApproachDurationTicks() {
        return SETTINGS.approachDurationTicks();
    }

    public static void show(CardRevealPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> show(payload));
    }

    public static void show(CardRevealPayload payload) {
        String animationId = normalizeAnimation(payload.animation());
        CardRevealAnimation animation = ANIMATIONS.get(animationId);
        int defaultDuration = animation.defaultDuration(SETTINGS);
        int duration = payload.durationTicks() > 0 ? payload.durationTicks() : defaultDuration;
        active = new CardReveal(payload.cardId(),
                Component.translatable(payload.titleKey()).getString(),
                Component.translatable(payload.bodyKey()).getString(),
                makeItemStack(payload.itemId()),
                Identifier.parse(payload.largeFrontTexture()),
                Identifier.parse(payload.largeBackTexture()),
                animationId, System.nanoTime(), duration);
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (active == null) {
            return;
        }

        float ageTicks = (System.nanoTime() - active.startedAtNanos()) / (float) TICK_NANOS;
        if (ageTicks >= active.durationTicks()) {
            active = null;
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            active = null;
            return;
        }

        CardRevealAnimation animation = ANIMATIONS.getOrDefault(active.animation(), ANIMATIONS.get(CardRevealPayload.ANIMATION_FLIP));
        int centerX = graphics.guiWidth() / 2;
        int centerY = graphics.guiHeight() / 2;
        int modelSize = Mth.clamp((int) (graphics.guiHeight() * 0.62F), 150, 292);
        CardRevealRenderContext context = new CardRevealRenderContext(graphics, minecraft, active, SETTINGS, ageTicks, centerX, centerY, modelSize);
        animation.render(context, RENDERER);
    }

    private static ItemStack makeItemStack(String itemId) {
        try {
            Identifier id = Identifier.parse(itemId);
            Item item = BuiltInRegistries.ITEM.getValue(id);
            return new ItemStack(item);
        } catch (Exception ignored) {
            return ItemStack.EMPTY;
        }
    }

    private static String normalizeAnimation(String animation) {
        if (ANIMATIONS.containsKey(animation)) {
            return animation;
        }
        return CardRevealPayload.ANIMATION_FLIP;
    }

}