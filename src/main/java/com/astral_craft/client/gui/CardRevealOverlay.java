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
import com.astral_craft.client.util.ClientAnimationClock;
import com.astral_craft.common.network.CardRevealPayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class CardRevealOverlay {

    public static final Identifier LAYER = AstralCraft.prefix("card_reveal_overlay");

    public static final CardRevealSettings SETTINGS = new CardRevealSettings();
    public static final CardRevealDebugSettings DEBUG_SETTINGS = new CardRevealDebugSettings();

    private static final Map<Identifier, CardRevealAnimation> ANIMATIONS = new HashMap<>();
    private static final CardRevealRenderer RENDERER = new CardRevealRenderer();
    private static final Deque<CardReveal> PENDING = new ArrayDeque<>();
    private static CardReveal active;

    static {
        registerAnimation(new FlipCardRevealAnimation());
        registerAnimation(new ApproachCardRevealAnimation());
    }

    public static void registerAnimation(CardRevealAnimation animation) {
        ANIMATIONS.put(animation.id(), animation);
    }

    public static void show(CardRevealPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> show(payload));
    }

    public static void show(CardRevealPayload payload) {
        Identifier animationId = normalizeAnimation(payload.animation());
        CardRevealAnimation animation = ANIMATIONS.get(animationId);
        int defaultDuration = animation.defaultDuration(SETTINGS);
        int duration = payload.durationTicks() > 0 ? Math.max(payload.durationTicks(), defaultDuration) : defaultDuration;
        CardReveal reveal = new CardReveal(payload.cardId(), payload.cardType(),
                payload.title(), payload.body(), payload.stack(),
                payload.largeFrontTexture(), payload.largeBackTexture(),
                animationId, ClientAnimationClock.nowTicks(), duration);
        if (active == null) {
            active = reveal;
        } else if (isActive()) {
            PENDING.addLast(reveal);
        } else {
            CardReveal queued = pollNextReveal(ClientAnimationClock.nowTicks());
            if (queued == null) {
                active = reveal;
            } else {
                active = queued;
                PENDING.addLast(reveal);
            }
        }
    }

    public static boolean isActive() {
        return active != null && ClientAnimationClock.elapsedTicks(active.startedAtTick()) < active.durationTicks();
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (active == null) return;
        float ageTicks = ClientAnimationClock.elapsedTicks(active.startedAtTick());
        if (ageTicks >= active.durationTicks()) {
            active = pollNextReveal(ClientAnimationClock.nowTicks());
            return;
        }

        DEBUG_SETTINGS.tick(SETTINGS);

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            active = null;
            PENDING.clear();
            return;
        }

        CardRevealAnimation animation = ANIMATIONS.getOrDefault(active.animation(), ANIMATIONS.get(CardRevealAnimations.FLIP));
        int centerX = graphics.guiWidth() / 2;
        int centerY = graphics.guiHeight() / 2;
        int modelSize = SETTINGS.responsiveModelSize(graphics.guiWidth(), graphics.guiHeight());
        CardRevealRenderContext context = new CardRevealRenderContext(graphics, minecraft, active, SETTINGS, ageTicks, centerX, centerY, modelSize);
        animation.render(context, RENDERER);
    }


    private static CardReveal pollNextReveal(long startedAtTick) {
        CardReveal next = PENDING.pollFirst();
        if (next == null) return null;
        return new CardReveal(next.cardId(), next.cardType(), next.title(), next.body(), next.stack(),
                next.frontTexture(), next.backTexture(), next.animation(), startedAtTick, next.durationTicks());
    }

    private static Identifier normalizeAnimation(Identifier animation) {
        if (animation != null && ANIMATIONS.containsKey(animation)) return animation;
        return CardRevealAnimations.FLIP;
    }

}