package com.astral_craft.client.gui;

import com.astral_craft.AstralCraft;
import com.astral_craft.client.gui.board.BoardTurnScreen;
import com.astral_craft.client.gui.reveal.*;
import com.astral_craft.client.util.ClientAnimationClock;
import com.astral_craft.common.network.s2c.CardRevealControlPayload;
import com.astral_craft.common.network.s2c.CardRevealPayload;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.*;

public class CardRevealOverlay {

    public static final Identifier LAYER = AstralCraft.prefix("card_reveal_overlay");

    public static final CardRevealSettings SETTINGS = new CardRevealSettings();
    public static final CardRevealDebugSettings DEBUG_SETTINGS = new CardRevealDebugSettings();

    private static final Map<Identifier, CardRevealAnimation> ANIMATIONS = new HashMap<>();
    private static final CardRevealRenderer RENDERER = new CardRevealRenderer();
    private static final Deque<CardReveal> PENDING = new ArrayDeque<>();
    private static final Map<UUID, CardRevealControlPayload.Action> PENDING_CONTROLS = new HashMap<>();
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
        CardRevealControlPayload.Action pendingControl = PENDING_CONTROLS.remove(payload.revealId());
        if (pendingControl == CardRevealControlPayload.Action.RELEASE) return;
        CardReveal reveal = new CardReveal(payload.cardId(), payload.cardType(),
                payload.title(), payload.body(), payload.stack(),
                payload.largeFrontTexture(), payload.largeBackTexture(),
                animationId, ClientAnimationClock.nowTicks(), duration,
                payload.sourceEntityId(), payload.targetEntityIds(), payload.revealId(),
                pendingControl == CardRevealControlPayload.Action.HOLD);
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

        BoardTurnScreen.restorePendingCounterScreen();
    }

    public static void control(CardRevealControlPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> control(payload));
    }

    public static void control(CardRevealControlPayload payload) {
        if (payload == null) return;
        boolean hold = payload.action() == CardRevealControlPayload.Action.HOLD;
        if (active != null && active.revealId().equals(payload.revealId())) {
            if (hold) {
                active = active.withHeld(true);
                BoardTurnScreen.restorePendingCounterScreen();
            } else {
                active = pollNextReveal(ClientAnimationClock.nowTicks());
            }
            return;
        }

        if (PENDING.isEmpty()) {
            PENDING_CONTROLS.put(payload.revealId(), payload.action());
            return;
        }

        boolean matched = false;
        Deque<CardReveal> replaced = new ArrayDeque<>();
        while (!PENDING.isEmpty()) {
            CardReveal reveal = PENDING.removeFirst();
            if (reveal.revealId().equals(payload.revealId())) {
                matched = true;
                if (hold) replaced.addLast(reveal.withHeld(true));
            } else {
                replaced.addLast(reveal);
            }
        }

        PENDING.addAll(replaced);
        if (!matched) PENDING_CONTROLS.put(payload.revealId(), payload.action());
        if (hold) BoardTurnScreen.restorePendingCounterScreen();
    }

    public static boolean isActive() {
        return active != null && (active.held()
                || ClientAnimationClock.elapsedTicks(active.startedAtTick()) < active.durationTicks());
    }

    public static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        if (active == null) return;
        float ageTicks = ClientAnimationClock.elapsedTicks(active.startedAtTick());
        if (!active.held() && ageTicks >= active.durationTicks()) {
            active = pollNextReveal(ClientAnimationClock.nowTicks());
            return;
        }
        if (active.held()) {
            ageTicks = Math.min(ageTicks, SETTINGS.flipIntroHoldTicks + SETTINGS.flipRotateTicks + 5.0F);
        }

        DEBUG_SETTINGS.tick(SETTINGS);

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            active = null;
            PENDING.clear();
            PENDING_CONTROLS.clear();
            return;
        }

        CardRevealAnimation animation = ANIMATIONS.getOrDefault(active.animation(), ANIMATIONS.get(CardRevealAnimations.FLIP));
        int centerX = graphics.guiWidth() / 2;
        int centerY = graphics.guiHeight() / 2;
        int modelSize = SETTINGS.responsiveModelSize(graphics.guiWidth(), graphics.guiHeight());
        CardRevealRenderContext context = new CardRevealRenderContext(graphics, minecraft, active, SETTINGS, ageTicks, centerX, centerY, modelSize);
        animation.render(context, RENDERER);
    }


    private static CardReveal pollNextReveal(double startedAtTick) {
        CardReveal next = PENDING.pollFirst();
        if (next == null) return null;
        return next.withStartedAt(startedAtTick);
    }

    private static Identifier normalizeAnimation(Identifier animation) {
        if (animation != null && ANIMATIONS.containsKey(animation)) return animation;
        return CardRevealAnimations.FLIP;
    }

}