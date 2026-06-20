package com.astral_craft.client.gui;

import com.astral_craft.AstralCraft;
import com.astral_craft.client.gui.reveal.ApproachCardRevealAnimation;
import com.astral_craft.client.gui.reveal.CardReveal;
import com.astral_craft.client.gui.reveal.CardRevealAnimation;
import com.astral_craft.client.gui.reveal.CardRevealAnimations;
import com.astral_craft.client.gui.reveal.CardRevealDebugSettings;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;

public class CardRevealOverlay {

    public static final Identifier LAYER = AstralCraft.prefix("card_reveal_overlay");

    public static final CardRevealSettings SETTINGS = new CardRevealSettings();
    public static final CardRevealDebugSettings DEBUG_SETTINGS = new CardRevealDebugSettings();

    private static final Map<Identifier, CardRevealAnimation> ANIMATIONS = new HashMap<>();
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

    public static void configureModelAndTextScale(float modelScale, float textScale) {
        SETTINGS.cardModelScale = Math.max(0.05F, modelScale);
        SETTINGS.cardTextScale = Math.max(0.05F, textScale);
    }

    public static void configureTextWidths(float titleRatio, float bodyRatio) {
        SETTINGS.titleTextMaxWidthRatio = Math.max(0.05F, titleRatio);
        SETTINGS.bodyTextMaxWidthRatio = Math.max(0.05F, bodyRatio);
        SETTINGS.textMaxWidthRatio = SETTINGS.bodyTextMaxWidthRatio;
    }

    public static void reloadDebugSettings() {
        DEBUG_SETTINGS.reloadNow(SETTINGS);
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
        Identifier animationId = normalizeAnimation(payload.animation());
        CardRevealAnimation animation = ANIMATIONS.get(animationId);
        int defaultDuration = animation.defaultDuration(SETTINGS);
        int duration = payload.durationTicks() > 0 ? Math.max(payload.durationTicks(), defaultDuration) : defaultDuration;
        active = new CardReveal(payload.cardId(), payload.cardType(),
                Component.translatable(payload.titleKey()).getString(),
                Component.translatable(payload.bodyKey()).getString(),
                makeItemStack(payload.itemId()),
                Identifier.parse(payload.largeFrontTexture()),
                Identifier.parse(payload.largeBackTexture()),
                animationId, currentClientGameTicks(null), duration);
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (active == null) {
            return;
        }

        float ageTicks = currentClientGameTicks(deltaTracker) - active.startedAtTicks();
        if (ageTicks >= active.durationTicks()) {
            active = null;
            return;
        }

        DEBUG_SETTINGS.tick(SETTINGS);

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            active = null;
            return;
        }

        CardRevealAnimation animation = ANIMATIONS.getOrDefault(active.animation(), ANIMATIONS.get(CardRevealAnimations.FLIP));
        int centerX = graphics.guiWidth() / 2;
        int centerY = graphics.guiHeight() / 2;
        int modelSize = SETTINGS.responsiveModelSize(graphics.guiWidth(), graphics.guiHeight());
        CardRevealRenderContext context = new CardRevealRenderContext(graphics, minecraft, active, SETTINGS, ageTicks, centerX, centerY, modelSize);
        animation.render(context, RENDERER);
    }

    private static float currentClientGameTicks(DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return 0.0F;
        }
        float partialTick = deltaTracker == null ? 0.0F : deltaTracker.getGameTimeDeltaPartialTick(false);
        return minecraft.level.getGameTime() + partialTick;
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

    private static Identifier normalizeAnimation(String animation) {
        Identifier id = parseAnimationId(animation);
        if (ANIMATIONS.containsKey(id)) {
            return id;
        }
        return CardRevealAnimations.FLIP;
    }

    private static Identifier parseAnimationId(String animation) {
        if (animation == null || animation.isBlank()) {
            return CardRevealAnimations.FLIP;
        }
        try {
            if (animation.indexOf(':') >= 0) {
                return Identifier.parse(animation);
            }
            return AstralCraft.prefix(animation);
        } catch (Exception ignored) {
            return CardRevealAnimations.FLIP;
        }
    }

}