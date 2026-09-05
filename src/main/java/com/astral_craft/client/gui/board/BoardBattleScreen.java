package com.astral_craft.client.gui.board;

import com.astral_craft.client.gui.HandCardRenderHelper;
import com.astral_craft.client.gui.components.AstralFancyButton;
import com.astral_craft.client.gui.components.AstralFancyButton.ButtonStyle;
import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.gameplay.DamagePresentation;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.network.c2s.BoardBattleActionPayload;
import com.astral_craft.common.network.s2c.OpenBoardBattlePayload;
import com.astral_craft.common.network.s2c.OpenBoardBattlePayload.BattleRole;
import com.astral_craft.common.network.s2c.OpenBoardBattlePayload.BattleView;
import com.astral_craft.common.network.s2c.OpenBoardBattlePayload.CombatCardView;
import com.astral_craft.common.network.s2c.OpenBoardBattlePayload.DefenseMode;
import com.astral_craft.common.network.s2c.OpenBoardBattlePayload.PlayedCardView;
import com.astral_craft.common.registry.AstralDataComponents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.NonNull;

import java.util.*;

/** In-world battle UI with irrevocable card selection and staged dice/result presentation. */
public class BoardBattleScreen extends Screen {

    private static final int CARD_W = HandCardRenderHelper.FRAMED_CARD_W;
    private static final int CARD_H = HandCardRenderHelper.FRAMED_CARD_H;
    private static final int CARD_GAP = 7;
    private static final int ATTACK_ACCENT = 0xFFD84B61;
    private static final int DEFENSE_ACCENT = 0xFF3F9DCE;
    private static final int NUMBER_BOX_SIZE = 32;
    private static final int NUMBER_BOX_BORDER = 2;
    private static final int DICE_FLASH_END_TICK = 12;
    private static final int DEFENSE_CHOICE_ANNOUNCE_TICKS = 4;
    private static final int FINAL_VALUE_STAGE_TICK = 22;
    private static final int ATTACK_ROLL_TUTORIAL_HOLD_TICKS = 20 * 8;
    private static final int EVADE_FAILURE_STAGE_TICK = FINAL_VALUE_STAGE_TICK;
    private static final int KNOCKOUT_FLIGHT_START_TICK = 1;
    private static final int KNOCKOUT_FLIGHT_TICKS = 12;
    private static final int KNOCKOUT_EXPLOSION_TICK = 13;
    private static final int KNOCKOUT_EXPLOSION_RENDER_TICKS = 24;
    private static final int VICTORY_MOVE_START_TICK = 14;
    private static final int VICTORY_MOVE_TICKS = 18;
    private static final Identifier HEART_TEXTURE = Identifier.withDefaultNamespace("textures/gui/sprites/hud/heart/full.png");
    private static final Identifier HEART_BLINKING_TEXTURE = Identifier.withDefaultNamespace("textures/gui/sprites/hud/heart/full_blinking.png");
    private static final Identifier CRITICAL_HIT_TEXTURE = Identifier.withDefaultNamespace("textures/particle/critical_hit.png");
    private final UUID boardId;
    private final int attackerEntityId;
    private final int defenderEntityId;
    private final String attackerName;
    private final String defenderName;
    private final BattleRole role;
    private final List<CombatCard> cards;
    private List<PlayedCardView> attackerPlayedCards = List.of();
    private List<PlayedCardView> defenderPlayedCards = List.of();
    private final Set<Integer> selectedIndexes = new LinkedHashSet<>();
    private int draggingIndex = -1;
    private float cardScroll;
    private int dragOffsetX;
    private int dragOffsetY;
    private int timeoutTicks;
    private int timeoutDurationTicks;
    private Identifier characterId;
    private Identifier skinId;
    private int maximumCost;
    private boolean submitted;
    private BattleView view;
    private int phaseAgeTicks;
    private int attackerHealthFlashTicks;
    private int defenderHealthFlashTicks;
    private int attackerScoreFlashTicks;
    private int defenderScoreFlashTicks;
    private int attackRollTutorialTicks;
    private boolean knockoutExplosionTriggered;
    private boolean attackerSixSoundPlayed;
    private boolean defenderSixSoundPlayed;
    private float renderPartialTick;

    public BoardBattleScreen(OpenBoardBattlePayload payload) {
        super(Component.translatable("gui.astral_craft.board.battle"));
        this.boardId = payload.boardId();
        this.attackerEntityId = payload.attackerEntityId();
        this.defenderEntityId = payload.defenderEntityId();
        this.attackerName = payload.attackerName();
        this.defenderName = payload.defenderName();
        this.role = payload.role();
        this.cards = payload.cards().stream().map(CombatCard::from)
                .filter(card -> card.definition() != null).toList();
        this.apply(payload);
    }

    public static void open(OpenBoardBattlePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Screen current = Minecraft.getInstance().screen;
            if (current instanceof BoardBattleScreen battle && battle.boardId.equals(payload.boardId())) {
                battle.apply(payload);
            } else {
                Minecraft.getInstance().setScreen(new BoardBattleScreen(payload));
            }
        });
    }

    public static void closePresentation(UUID boardId) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen instanceof BoardBattleScreen screen && screen.boardId.equals(boardId)) screen.onClose();
    }

    private void apply(OpenBoardBattlePayload payload) {
        BattleView next = payload.view();
        if (this.view != null) {
            if (next.attackerHealth() < this.view.attackerHealth()) this.attackerHealthFlashTicks = 20;
            if (next.defenderHealth() < this.view.defenderHealth()) this.defenderHealthFlashTicks = 20;
            if (next.attackMinimum() != this.view.attackMinimum() || next.attackMaximum() != this.view.attackMaximum()) {
                this.attackerScoreFlashTicks = 7;
            }

            if (next.defenseMinimum() != this.view.defenseMinimum() || next.defenseMaximum() != this.view.defenseMaximum()) {
                this.defenderScoreFlashTicks = 7;
            }
        }

        boolean phaseChanged = this.view == null || !this.view.phase().equals(next.phase());
        if (phaseChanged) {
            this.phaseAgeTicks = 0;
            this.knockoutExplosionTriggered = false;
            if (next.attackerRolling() && this.role == BattleRole.ATTACKER) {
                this.attackRollTutorialTicks = ATTACK_ROLL_TUTORIAL_HOLD_TICKS;
            }
            if (next.defenseChoice() && this.role == BattleRole.DEFENDER) this.submitted = false;
        }

        this.view = next;
        this.timeoutTicks = Math.max(0, payload.decisionTicks());
        this.timeoutDurationTicks = Math.max(1, payload.decisionDurationTicks());
        this.characterId = payload.characterId();
        this.skinId = payload.skinId();
        this.maximumCost = Math.max(0, payload.maximumCost());
        this.attackerPlayedCards = payload.attackerPlayedCards().stream()
                .map(card -> new PlayedCardView(card.stack(), card.bonus())).toList();
        this.defenderPlayedCards = payload.defenderPlayedCards().stream()
                .map(card -> new PlayedCardView(card.stack(), card.bonus())).toList();
    }

    @Override
    protected void init() {
        this.cardScroll = Math.clamp(this.cardScroll, 0.0F, this.maximumCardScroll(this.layout()));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        this.phaseAgeTicks++;
        if (this.attackerHealthFlashTicks > 0) this.attackerHealthFlashTicks--;
        if (this.defenderHealthFlashTicks > 0) this.defenderHealthFlashTicks--;
        if (this.attackerScoreFlashTicks > 0) this.attackerScoreFlashTicks--;
        if (this.defenderScoreFlashTicks > 0) this.defenderScoreFlashTicks--;
        if (this.attackRollTutorialTicks > 0) this.attackRollTutorialTicks--;
        if (!this.attackerSixSoundPlayed && this.view.attackerDie() == 6
                && (!this.view.attackerRolling() || this.phaseAgeTicks >= DICE_FLASH_END_TICK)) {
            this.attackerSixSoundPlayed = true;
            this.playMaximumDiceSound();
        }
        if (!this.defenderSixSoundPlayed && this.view.defenderDie() == 6
                && (this.view.result() || this.view.defenderRolling() && this.defenderRollAge() >= DICE_FLASH_END_TICK)) {
            this.defenderSixSoundPlayed = true;
            this.playMaximumDiceSound();
        }

        if (this.view.result() && this.view.knockout()
                && this.phaseAgeTicks >= KNOCKOUT_EXPLOSION_TICK
                && !this.knockoutExplosionTriggered) {
            this.knockoutExplosionTriggered = true;
            this.playKnockoutExplosion();
        }

        if (this.timeoutTicks > 0) this.timeoutTicks--;
        if (this.view.result() && this.timeoutTicks <= 0) this.onClose();
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        this.renderPartialTick = partialTick;
        Layout layout = this.layout();
        BoardTutorialGuide.beginFrame(this.boardId);
        this.renderArena(graphics, layout);
        this.renderNames(graphics, layout);
        LivingEntity attacker = this.entity(this.attackerEntityId);
        LivingEntity defender = this.entity(this.defenderEntityId);
        this.renderCombatants(graphics, layout, attacker, defender);
        this.renderHealth(graphics, this.view.attackerHealth(),
                layout.x() + layout.width() / 4 - 10,
                layout.modelBottom() - 11, this.attackerHealthFlashTicks);
        this.renderHealth(graphics, this.view.defenderHealth(),
                layout.x() + layout.width() * 3 / 4 + 10,
                layout.modelBottom() - 11, this.defenderHealthFlashTicks);
        this.renderBattleNumbers(graphics, layout);
        this.renderFinalBreakdown(graphics, layout);
        this.renderDamagePopup(graphics, layout);
        this.renderVanillaCriticalParticles(graphics, layout);
        this.renderVanillaExplosion(graphics, layout);
        if (this.view.selecting()) {
            this.renderHand(graphics, layout, mouseX, mouseY);
            this.renderCost(graphics, layout);
        }

        if (this.view.selecting() || this.view.defenseChoice()) {
            this.renderActions(graphics, layout, mouseX, mouseY);
        }

        this.renderStatus(graphics, layout);
        this.renderTutorial(graphics, layout);
    }

    private void renderTutorial(GuiGraphicsExtractor graphics, Layout layout) {
        if (!BoardTutorialGuide.active(this.boardId) || this.role == BattleRole.SPECTATOR) return;
        int width = Math.min(410, Math.max(180, layout.width() - 36));
        int x = layout.x() + 18;
        int bottom = Math.max(layout.y() + 118, layout.cardY() - 7);
        boolean attackRollVisible = this.role == BattleRole.ATTACKER && this.attackRollTutorialTicks > 0
                && BoardTutorialGuide.visible(this.boardId, BoardTutorialGuide.Hint.ATTACK_ROLL);
        if (attackRollVisible) {
            int height = BoardTutorialGuide.renderBox(graphics, this.font, this.boardId,
                    BoardTutorialGuide.Hint.ATTACK_ROLL, x, bottom, width);
            bottom -= height > 0 ? height + 5 : 0;
        }
        if (this.view.result() && this.view.knockout()) {
            BoardTutorialGuide.renderBox(graphics, this.font, this.boardId,
                    this.role == BattleRole.DEFENDER ? BoardTutorialGuide.Hint.KNOCKED_DOWN
                            : BoardTutorialGuide.Hint.KNOCKOUT_OTHER, x, bottom, width);
            return;
        }
        if (this.view.defenseChoice()) {
            if (this.role == BattleRole.DEFENDER) {
                int height = BoardTutorialGuide.renderBox(graphics, this.font, this.boardId,
                        BoardTutorialGuide.Hint.DEFENSE_RULES, x, bottom, width);
                bottom -= height > 0 ? height + 5 : 0;
                BoardTutorialGuide.renderBox(graphics, this.font, this.boardId,
                        BoardTutorialGuide.Hint.DEFENSE_CHOICE, x, bottom, width);
            }
            return;
        }
        if (this.view.attackerRolling() && this.role == BattleRole.ATTACKER) {
            if (!attackRollVisible) BoardTutorialGuide.renderBox(graphics, this.font, this.boardId,
                    BoardTutorialGuide.Hint.ATTACK_ROLL, x, bottom, width);
            return;
        }
        if (!this.view.selecting()) return;
        if (!this.selectedIndexes.isEmpty()) {
            int height = BoardTutorialGuide.renderBox(graphics, this.font, this.boardId,
                    BoardTutorialGuide.Hint.BATTLE_VALUES, x, bottom, width);
            bottom -= height > 0 ? height + 5 : 0;
        }
        int battleStartHeight = BoardTutorialGuide.renderBox(graphics, this.font, this.boardId,
                BoardTutorialGuide.Hint.BATTLE_START, x, bottom, width);
        bottom -= battleStartHeight > 0 ? battleStartHeight + 5 : 0;
        BoardTutorialGuide.renderBox(graphics, this.font, this.boardId,
                BoardTutorialGuide.Hint.HAND_DRAG, x, bottom, width);
    }

    private void renderNames(GuiGraphicsExtractor graphics, Layout layout) {
        Component attackLabel = Component.translatable("gui.astral_craft.board.attack");
        Component defenseLabel = Component.translatable("gui.astral_craft.board.defense");
        graphics.text(this.font, attackLabel, layout.x() + 18, layout.y() + 14, 0xFFFF6B74, true);
        graphics.text(this.font, defenseLabel, layout.x() + layout.width() - 18 - this.font.width(defenseLabel),
                layout.y() + 14, 0xFF67D9FF, true);
        graphics.text(this.font, this.attackerName, layout.x() + 18, layout.y() + 29, 0xFFFFFFFF, false);
        graphics.text(this.font, this.defenderName, layout.x() + layout.width() - 18 - this.font.width(this.defenderName),
                layout.y() + 29, 0xFFFFFFFF, false);
    }

    private void renderArena(GuiGraphicsExtractor graphics, Layout layout) {
        graphics.fill(layout.x(), layout.y(), layout.x() + layout.width(), layout.y() + layout.height(), 0xF014141C);
        graphics.fill(layout.x(), layout.y(), layout.x() + layout.width(), layout.y() + 3, 0xD0FFFFFF);
        int arenaTop = layout.modelTop() + 18;
        int arenaBottom = layout.modelBottom();
        int i = layout.x() + layout.width() - 8;
        graphics.fill(layout.x() + 8, arenaTop, i, arenaBottom, 0xFFB84E18);
        graphics.fill(layout.x() + 18, arenaTop + 8, layout.x() + layout.width() - 18, arenaBottom - 7, 0xFFFF8B18);
        graphics.fill(layout.x() + 66, arenaTop + 18, layout.x() + layout.width() - 66, arenaBottom - 17, 0xFFFFC431);
        graphics.fill(layout.x() + 8, arenaBottom, i, arenaBottom + 4, 0xFF301A20);
    }

    private void renderHealth(GuiGraphicsExtractor graphics, int value, int centerX, int y, int flashTicks) {
        Component health = Component.literal(Integer.toString(Math.max(0, value)));
        int iconSize = 16;
        int gap = 5;
        float textScale = 1.45F;
        int textWidth = Math.round(this.font.width(health) * textScale);
        int width = iconSize + gap + textWidth + 14;
        int left = centerX - width / 2;
        graphics.fill(left, y, left + width, y + 22, 0xE8FFFFFF);
        boolean blinking = flashTicks > 0 && Math.floorMod(flashTicks / 3, 2) == 0;
        Identifier texture = blinking ? HEART_BLINKING_TEXTURE : HEART_TEXTURE;
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, left + 6, y + 3, 0.0F, 0.0F,
                iconSize, iconSize, 9, 9, 9, 9, 0xFFFFFFFF);
        int color = blinking ? 0xFFFFF4F4 : 0xFFC72E4E;
        graphics.pose().pushMatrix();
        graphics.pose().translate(left + 6 + iconSize + gap, y + 5);
        graphics.pose().scale(textScale, textScale);
        graphics.text(this.font, health, 0, 0, color, true);
        graphics.pose().popMatrix();
    }

    private void renderCombatants(GuiGraphicsExtractor graphics, Layout layout, LivingEntity attacker, LivingEntity defender) {
        int attackerLeft = layout.x() + 22;
        int attackerRight = layout.x() + layout.width() / 2 - 24;
        int defenderLeft = layout.x() + layout.width() / 2 + 24;
        int defenderRight = layout.x() + layout.width() - 22;
        float age = this.phaseAgeTicks + this.renderPartialTick;
        float approach = this.view.selecting() ? 0.0F
                : this.view.ready() ? smoothStep(Mth.clamp(age / 10.0F, 0.0F, 1.0F)) : 1.0F;
        float approachOffset = Math.clamp(layout.width() * 0.025F, 8.0F, 18.0F) * approach;
        if (!this.view.result() || !this.view.knockout()) {
            BoardScreenEntityRenderer.render(graphics, attacker, attackerLeft, layout.modelTop(), attackerRight,
                    layout.modelBottom(), 225.0F, 1.0F, approachOffset, 0.0F, 0.0F);
            BoardScreenEntityRenderer.render(graphics, defender, defenderLeft, layout.modelTop(), defenderRight,
                    layout.modelBottom(), -225.0F, 1.0F, -approachOffset, 0.0F, 0.0F);
            return;
        }

        float flightProgress = Mth.clamp((age - KNOCKOUT_FLIGHT_START_TICK) / KNOCKOUT_FLIGHT_TICKS, 0.0F, 1.0F);
        float flight = 1.0F - (1.0F - flightProgress) * (1.0F - flightProgress);
        float defenderCenterX = (defenderLeft + defenderRight) * 0.5F;
        float defenderCenterY = (layout.modelTop() + layout.modelBottom()) * 0.5F;
        float modelWidth = defenderRight - defenderLeft;
        float modelHeight = layout.modelBottom() - layout.modelTop();
        float exitCenterX = layout.x() + layout.width() + modelWidth * 0.72F;
        float exitCenterY = layout.modelTop() - modelHeight * 0.72F;
        float defenderOffsetX = Mth.lerp(flight, -approachOffset, exitCenterX - defenderCenterX);
        float linearY = Mth.lerp(flight, defenderCenterY, exitCenterY);
        float arcY = -(float) Math.sin(Math.PI * flightProgress) * modelHeight * 0.18F;
        float defenderOffsetY = linearY - defenderCenterY + arcY;
        float defenderRoll = Mth.lerp(flight, 0.0F, 28.0F);
        if (age < KNOCKOUT_EXPLOSION_TICK) {
            BoardScreenEntityRenderer.render(graphics, defender, defenderLeft, layout.modelTop(), defenderRight,
                    layout.modelBottom(), 225.0F, 1.0F, defenderOffsetX, defenderOffsetY, defenderRoll);
        }

        float victoryProgress = Mth.clamp((age - VICTORY_MOVE_START_TICK) / VICTORY_MOVE_TICKS, 0.0F, 1.0F);
        float victory = smootherStep(victoryProgress);
        float attackerCenterX = (attackerLeft + attackerRight) * 0.5F;
        float attackerCenterY = (layout.modelTop() + layout.modelBottom()) * 0.5F;
        float targetCenterX = layout.x() + layout.width() * 0.5F;
        float targetCenterY = layout.modelTop() + (layout.modelBottom() - layout.modelTop()) * 0.48F;
        float attackerOffsetX = Mth.lerp(victory, approachOffset, targetCenterX - attackerCenterX);
        float attackerOffsetY = Mth.lerp(victory, 0.0F, targetCenterY - attackerCenterY)
                - (float) Math.sin(Math.PI * victoryProgress) * 7.0F;
        float winnerScale = 1.0F + victory * 0.14F;
        BoardScreenEntityRenderer.render(graphics, attacker, attackerLeft, layout.modelTop(), attackerRight,
                layout.modelBottom(), 225.0F, winnerScale, attackerOffsetX, attackerOffsetY, 0.0F);
    }

    private void renderBattleNumbers(GuiGraphicsExtractor graphics, Layout layout) {
        int horizontalOffset = this.numberHorizontalOffset(layout);
        int attackX = layout.x() + layout.width() / 4 - horizontalOffset;
        int defenseX = layout.x() + layout.width() * 3 / 4 + horizontalOffset;
        int numberY = layout.modelTop() + 18;
        if (this.view.scorePhase()) {
            Range attackRange = this.displayRange(true);
            Range defenseRange = this.displayRange(false);
            this.renderFraction(graphics, attackRange.minimum(), attackRange.maximum(), attackX, numberY,
                    true, this.attackerScoreFlashTicks);
            this.renderFraction(graphics, defenseRange.minimum(), defenseRange.maximum(), defenseX, numberY,
                    false, this.defenderScoreFlashTicks);
            this.renderReadyState(graphics, layout, true);
            this.renderReadyState(graphics, layout, false);
            return;
        }

        if (this.view.attackerRolling()) {
            int value = this.phaseAgeTicks < DICE_FLASH_END_TICK
                    ? 1 + Math.floorMod(this.phaseAgeTicks * 5 + 1, 6) : this.view.attackerDie();
            boolean rolling = this.phaseAgeTicks < DICE_FLASH_END_TICK;
            this.renderDiceValue(graphics, value, attackX, numberY, true,
                    rolling ? 1.0F : this.settledDiceScale(true), this.diceTextColor(true, rolling));
            return;
        }

        if (this.view.defenseChoice()) {
            this.renderDiceValue(graphics, this.view.attackerDie(), attackX, numberY, true,
                    this.settledDiceScale(true), this.diceTextColor(true, false));
            return;
        }

        if (this.shouldRenderAnimatedValue(true)) {
            this.renderDiceValue(graphics, this.animatedValue(true), attackX, numberY, true,
                    this.numberScale(true) / 2.85F, this.animatedValueColor(true));
        }
        if (this.shouldRenderAnimatedValue(false)) {
            this.renderDiceValue(graphics, this.animatedValue(false), defenseX, numberY, false,
                    this.numberScale(false) / 2.85F, this.animatedValueColor(false));
        }
        if (this.showEvadeResult()) {
            Component evade = Component.translatable(this.view.evaded()
                    ? "gui.astral_craft.board.evade_success" : "gui.astral_craft.board.evade_failed");
            this.renderScaledCenteredText(graphics, evade, defenseX, numberY + 35,
                    this.view.evaded() ? 0xFF79FF8A : 0xFFFF7373, 1.35F, true);
        }
    }

    private boolean showEvadeResult() {
        if (this.view.defenseMode() != DefenseMode.EVADE) return false;
        return this.view.result() || this.view.defenderRolling() && this.defenderRollAge() >= DICE_FLASH_END_TICK;
    }

    private void renderReadyState(GuiGraphicsExtractor graphics, Layout layout, boolean attacker) {
        boolean ready = attacker ? this.view.attackerReady() : this.view.defenderReady();
        String name = attacker ? this.attackerName : this.defenderName;
        int color = attacker ? ATTACK_ACCENT : DEFENSE_ACCENT;
        int horizontalOffset = this.numberHorizontalOffset(layout);
        int centerX = attacker ? layout.x() + layout.width() / 4 - horizontalOffset
                : layout.x() + layout.width() * 3 / 4 + horizontalOffset;
        int y = layout.modelTop() + 6;
        Component text = ready
                ? Component.translatable("gui.astral_craft.board.battle_ready_overhead").withStyle(ChatFormatting.BLUE)
                : coloredStatus(name, attacker, "gui.astral_craft.board.battle_playing_cards");
        int width = this.font.width(text) + 12;
        graphics.fill(centerX - width / 2, y - 12, centerX + width / 2, y, 0xC9000000);
        graphics.text(this.font, text, centerX - this.font.width(text) / 2, y - 10, ready ? -1 : color, true);
    }

    private float numberScale(boolean attack) {
        float base = 2.85F;
        if (!this.view.defenderRolling()) return base;
        int age = this.phaseAgeTicks;
        int pulseDistance = Integer.MAX_VALUE;
        if (this.view.defenseMode() == DefenseMode.EVADE) {
            if (!this.view.evaded() && this.defenderRollAge() >= DICE_FLASH_END_TICK) {
                pulseDistance = Math.abs(age - EVADE_FAILURE_STAGE_TICK);
            }
        } else if (this.defenderRollAge() >= DICE_FLASH_END_TICK) {
            pulseDistance = Math.abs(age - FINAL_VALUE_STAGE_TICK);
        }

        float stagePulse = pulseDistance > 4 ? 0.0F : 1.0F - Mth.clamp(pulseDistance / 4.0F, 0.0F, 1.0F);
        float sixPulse = this.settledDiceScale(attack) - 1.0F;
        return base + stagePulse * 0.85F + sixPulse * base;
    }

    private void renderScaledCenteredText(GuiGraphicsExtractor graphics, Component text, int centerX, int y, int color, float scale, boolean shadow) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, y);
        graphics.pose().scale(scale, scale);
        graphics.text(this.font, text, -this.font.width(text) / 2, -4, color, shadow);
        graphics.pose().popMatrix();
    }

    private Range displayRange(boolean attack) {
        int minimum = attack ? this.view.attackMinimum() : this.view.defenseMinimum();
        int maximum = attack ? this.view.attackMaximum() : this.view.defenseMaximum();
        if (this.view.selecting() && ((attack && this.role == BattleRole.ATTACKER) || (!attack && this.role == BattleRole.DEFENDER))) {
            int base = attack ? this.view.attackBase() : this.view.defenseBase();
            int minBonus = this.selectedIndexes.stream().mapToInt(index -> this.cards.get(index).minimumBonus()).sum();
            int maxBonus = this.selectedIndexes.stream().mapToInt(index -> this.cards.get(index).maximumBonus()).sum();
            minimum = base + minBonus;
            maximum = base + maxBonus;
        }

        return new Range(minimum, maximum);
    }

    private void renderFraction(GuiGraphicsExtractor graphics, int numerator, int denominator, int centerX,
                                int centerY, boolean attack, int flashTicks) {
        this.renderNumberBox(graphics, centerX, centerY, attack);
        int drawColor = 0xFF080808;
        float scale = 1.0F + (flashTicks > 0 ? 0.08F * flashTicks / 7.0F : 0.0F);
        Component top = Component.literal(Integer.toString(numerator)).withStyle(ChatFormatting.BOLD);
        Component bottom = Component.literal(Integer.toString(denominator)).withStyle(ChatFormatting.BOLD);
        this.renderScaledCenteredText(graphics, top, centerX, centerY - 8, drawColor, scale, false);
        graphics.fill(centerX - 8, centerY - 1, centerX + 9, centerY + 1, 0xFF080808);
        this.renderScaledCenteredText(graphics, bottom, centerX, centerY + 8, drawColor, scale, false);
    }

    private void renderDiceValue(GuiGraphicsExtractor graphics, int value, int centerX, int centerY,
                                 boolean attack, float scale, int textColor) {
        this.renderNumberBox(graphics, centerX, centerY, attack);
        Component text = Component.literal(Integer.toString(value)).withStyle(ChatFormatting.BOLD);
        this.renderScaledCenteredText(graphics, text, centerX, centerY + 1, textColor, 2.25F * scale, false);
    }

    private void renderNumberBox(GuiGraphicsExtractor graphics, int centerX, int centerY, boolean attack) {
        int half = NUMBER_BOX_SIZE / 2;
        graphics.fill(centerX - half, centerY - half, centerX + half, centerY + half, 0xFF050505);
        graphics.fill(centerX - half + NUMBER_BOX_BORDER, centerY - half + NUMBER_BOX_BORDER,
                centerX + half - NUMBER_BOX_BORDER, centerY + half - NUMBER_BOX_BORDER,
                attack ? ATTACK_ACCENT : DEFENSE_ACCENT);
    }

    private float settledDiceScale(boolean attack) {
        int die = attack ? this.view.attackerDie() : this.view.defenderDie();
        int age = this.maximumDiceEffectAge(attack);
        if (die != 6 || age < 0 || age > 12) return 1.0F;
        return 1.0F + (float) Math.sin(age / 12.0F * Math.PI) * 0.22F;
    }

    private int diceTextColor(boolean attack, boolean rolling) {
        return this.maximumRollFlashesWhite(attack, rolling) ? 0xFFFFFFFF : 0xFF080808;
    }

    private int maximumDiceEffectAge(boolean attack) {
        if (attack) return this.view.attackerRolling() ? this.phaseAgeTicks - DICE_FLASH_END_TICK : Integer.MAX_VALUE;
        return this.view.defenderRolling() ? this.defenderRollAge() - DICE_FLASH_END_TICK : Integer.MAX_VALUE;
    }

    private boolean shouldRenderAnimatedValue(boolean attack) {
        if (this.view.result() || !this.view.defenderRolling()) return false;
        if (this.phaseAgeTicks > FINAL_VALUE_STAGE_TICK + 8) return false;
        return attack || this.phaseAgeTicks >= DEFENSE_CHOICE_ANNOUNCE_TICKS;
    }

    private int animatedValueColor(boolean attack) {
        return this.maximumRollFlashesWhite(attack, false) ? 0xFFFFFFFF : 0xFF080808;
    }

    private boolean maximumRollFlashesWhite(boolean attack, boolean rolling) {
        if (rolling || (attack ? this.view.attackerDie() : this.view.defenderDie()) != 6) return false;
        int age = this.maximumDiceEffectAge(attack);
        return age >= 0 && age <= 12 && Math.floorMod(age, 4) < 2;
    }

    private int animatedValue(boolean attack) {
        int die = attack ? this.view.attackerDie() : this.view.defenderDie();
        int base = attack ? this.view.attackBase() : this.view.defenseBase();
        if (this.view.result()) {
            if (this.view.defenseMode() == DefenseMode.EVADE) {
                if (this.view.evaded()) return die;
                return attack ? this.view.attackTotal() : 0;
            }

            return attack ? this.view.attackTotal() : this.view.defenseTotal();
        }

        if (!this.view.defenderRolling()) return 0;
        int rollAge = this.defenderRollAge();
        if (rollAge < 0) return attack ? this.view.attackerDie() : 0;
        if (rollAge < DICE_FLASH_END_TICK) {
            return attack ? this.view.attackerDie() : 1 + Math.floorMod(rollAge * 7 + 3, 6);
        }

        if (this.view.defenseMode() == DefenseMode.EVADE) {
            if (this.view.evaded() || this.phaseAgeTicks < EVADE_FAILURE_STAGE_TICK) return die;
            return attack ? this.view.attackTotal() : 0;
        }

        if (this.phaseAgeTicks < FINAL_VALUE_STAGE_TICK) return die;
        return attack ? this.view.attackTotal() : this.view.defenseTotal();
    }

    private int defenderRollAge() {
        return this.phaseAgeTicks - DEFENSE_CHOICE_ANNOUNCE_TICKS;
    }

    private static float smoothStep(float value) {
        return value * value * (3.0F - 2.0F * value);
    }

    private static float smootherStep(float value) {
        return value * value * value * (value * (value * 6.0F - 15.0F) + 10.0F);
    }

    private void renderFinalBreakdown(GuiGraphicsExtractor graphics, Layout layout) {
        if (!this.showFinalBreakdown()) return;
        int attackCenter = layout.x() + layout.width() / 4;
        int defenseCenter = layout.x() + layout.width() * 3 / 4;
        float scale = 0.62F;
        int cardHeight = Math.round(CARD_H * scale);
        int cardsY = Math.min(layout.cardY() + 8, layout.y() + layout.height() - cardHeight - 8);
        int horizontalOffset = this.numberHorizontalOffset(layout);
        int valueY = layout.modelTop() + 42;
        this.renderPanelValue(graphics, attackCenter - horizontalOffset, valueY, true, this.view.attackBase());
        this.renderPanelValue(graphics, defenseCenter + horizontalOffset, valueY, false, this.view.defenseBase());
        this.renderPlayedCards(graphics, this.attackerPlayedCards, attackCenter, cardsY, true, scale);
        this.renderPlayedCards(graphics, this.defenderPlayedCards, defenseCenter, cardsY, false, scale);
    }

    private boolean showFinalBreakdown() {
        return this.view.result() || this.view.defenderRolling() && this.phaseAgeTicks >= FINAL_VALUE_STAGE_TICK;
    }

    private void renderPanelValue(GuiGraphicsExtractor graphics, int centerX, int y, boolean attack, int value) {
        Component label = Component.translatable(attack ? "gui.astral_craft.board.attack" : "gui.astral_craft.board.defense")
                .copy().append(Component.literal(": " + value));
        int width = this.font.width(label) + 10;
        graphics.fill(centerX - width / 2, y - 2, centerX + width / 2, y + 11, 0xC9000000);
        graphics.text(this.font, label, centerX - this.font.width(label) / 2, y,
                attack ? 0xFFFF7C85 : 0xFF75DEFF, true);
    }

    private void renderPlayedCards(GuiGraphicsExtractor graphics, List<PlayedCardView> cards,
                                   int centerX, int y, boolean attack, float scale) {
        if (cards.isEmpty()) return;
        int cardWidth = Math.round(CARD_W * scale);
        int step = Math.max(18, Math.round(cardWidth * 0.58F));
        int totalWidth = cardWidth + step * (cards.size() - 1);
        int startX = centerX - totalWidth / 2;
        for (int index = 0; index < cards.size(); index++) {
            PlayedCardView card = cards.get(index);
            CardDefinition definition = card.stack().get(AstralDataComponents.CARD_DEFINITION);
            if (definition == null && card.stack().getItem() instanceof BaseHandCard handCard) {
                definition = handCard.definition(card.stack());
            }
            if (definition == null) continue;
            int x = startX + index * step;
            graphics.pose().pushMatrix();
            graphics.pose().translate(x, y);
            graphics.pose().scale(scale, scale);
            HandCardRenderHelper.renderFramedCard(graphics, this.font, definition.type(),
                    definition.largeFrontTexture(card.stack()), definition.displayName(card.stack()),
                    0, 0, -1000, -1000, false);
            graphics.pose().popMatrix();
            Component bonus = Component.literal((card.bonus() >= 0 ? "+" : "") + card.bonus())
                    .withStyle(ChatFormatting.BOLD);
            int bonusX = x + cardWidth / 2 - this.font.width(bonus) / 2;
            graphics.fill(bonusX - 3, y - 12, bonusX + this.font.width(bonus) + 3, y - 1, 0xD9000000);
            graphics.text(this.font, bonus, bonusX, y - 11, attack ? 0xFFFF8C95 : 0xFF80E4FF, true);
        }
    }

    private void renderDamagePopup(GuiGraphicsExtractor graphics, Layout layout) {
        if (!this.view.result() || this.view.damage() <= 0) return;
        int age = Math.min(this.phaseAgeTicks, 28);
        int centerX = layout.x() + layout.width() * 3 / 4 + Math.clamp(layout.width() / 10, 42, 76);
        int baseY = layout.modelTop() + (layout.modelBottom() - layout.modelTop()) / 2;
        int y = baseY - Math.round(Mth.clamp(age / 28.0F, 0.0F, 1.0F) * 18.0F);
        float pulse = age < 6 ? 1.85F - age * 0.10F : 1.25F;
        Component damage = Component.literal("-" + this.view.damage()).withStyle(ChatFormatting.RED, ChatFormatting.BOLD);
        this.renderScaledCenteredText(graphics, damage, centerX, y, 0xFFFF4545, pulse, true);
    }

    private void renderVanillaCriticalParticles(GuiGraphicsExtractor graphics, Layout layout) {
        if (!this.view.result() || this.view.damage() < DamagePresentation.CRITICAL_DAMAGE_THRESHOLD) return;
        float age = this.phaseAgeTicks + this.renderPartialTick;
        if (age > 18.0F) return;
        int centerX = layout.x() + layout.width() * 3 / 4;
        int centerY = layout.modelTop() + (layout.modelBottom() - layout.modelTop()) / 2;
        for (int index = 0; index < 12; index++) {
            float progress = Mth.clamp((age - index * 0.45F) / 12.0F, 0.0F, 1.0F);
            if (progress <= 0.0F || progress >= 1.0F) continue;
            double angle = index * Math.PI * 2.0D / 12.0D + index * 0.31D;
            float radius = 10.0F + 44.0F * smoothStep(progress);
            int x = Math.round(centerX + (float) Math.cos(angle) * radius);
            int y = Math.round(centerY + (float) Math.sin(angle) * radius * 0.62F);
            int size = Math.max(4, Math.round(13.0F * (1.0F - progress * 0.55F)));
            graphics.blit(RenderPipelines.GUI_TEXTURED, CRITICAL_HIT_TEXTURE, x - size / 2, y - size / 2,
                    0.0F, 0.0F, size, size, 8, 8, 8, 8, 0xFFFFFFFF);
        }
    }

    private void renderVanillaExplosion(GuiGraphicsExtractor graphics, Layout layout) {
        if (!this.view.result() || !this.view.knockout()) return;
        float age = this.phaseAgeTicks + this.renderPartialTick - KNOCKOUT_EXPLOSION_TICK;
        if (age < 0.0F || age >= KNOCKOUT_EXPLOSION_RENDER_TICKS) return;
        int centerX = Math.round(this.explosionCenterX(layout));
        int centerY = Math.round(this.explosionCenterY(layout));
        float flash = 1.0F - Mth.clamp(age / 3.5F, 0.0F, 1.0F);
        if (flash > 0.0F) {
            int radius = Math.round(42.0F * flash + 14.0F);
            int alpha = Math.round(155.0F * flash);
            graphics.fill(Math.max(layout.x(), centerX - radius), layout.y(),
                    layout.x() + layout.width(),
                    Math.min(layout.y() + layout.height(), centerY + radius),
                    alpha << 24 | 0x00FFF1BD);
        }

        this.renderExplosionParticle(graphics, centerX, centerY, age, 0, -12.0F, 11.0F, 1.4F);
        for (int index = 0; index < 22; index++) {
            float delay = (index % 7) * 0.48F;
            float particleAge = age - delay;
            if (particleAge < 0.0F || particleAge >= 15.0F) continue;
            float spread = index / 21.0F;
            double angle = Math.toRadians(102.0D + spread * 88.0D) + Math.sin(index * 1.71D) * 0.09D;
            float distance = 10.0F + smoothStep(Mth.clamp(particleAge / 13.0F, 0.0F, 1.0F))
                    * (30.0F + index % 5 * 8.0F);
            float x = (float) Math.cos(angle) * distance;
            float y = (float) Math.sin(angle) * distance - particleAge * 0.16F;
            float scale = 0.54F + index % 4 * 0.12F;
            this.renderExplosionParticle(graphics, centerX, centerY, particleAge, index + 1, x, y, scale);
        }
        this.renderExplosionSparks(graphics, centerX, centerY, age);
    }

    private void renderExplosionSparks(GuiGraphicsExtractor graphics, int centerX, int centerY, float age) {
        for (int index = 0; index < 20; index++) {
            float particleAge = age - (index % 5) * 0.32F;
            if (particleAge < 0.0F || particleAge >= 13.0F) continue;
            float progress = Mth.clamp(particleAge / 13.0F, 0.0F, 1.0F);
            float spread = index / 19.0F;
            double angle = Math.toRadians(98.0D + spread * 94.0D) + Math.cos(index * 1.37D) * 0.08D;
            float distance = 14.0F + smoothStep(progress) * (38.0F + index % 6 * 7.0F);
            int x = Math.round(centerX + (float) Math.cos(angle) * distance);
            int y = Math.round(centerY + (float) Math.sin(angle) * distance + progress * 3.0F);
            int alpha = Math.round(255.0F * (1.0F - progress));
            if ((index & 1) == 0) {
                int size = Math.max(3, Math.round(8.0F * (1.0F - progress * 0.55F)));
                graphics.blit(RenderPipelines.GUI_TEXTURED, CRITICAL_HIT_TEXTURE, x - size / 2, y - size / 2,
                        0.0F, 0.0F, size, size, 8, 8, 8, 8, alpha << 24 | 0x00FFFFFF);
            } else {
                int length = Math.max(2, Math.round(7.0F * (1.0F - progress)));
                int color = alpha << 24 | (index % 3 == 0 ? 0x00FFF3A5 : 0x00FF9D4D);
                if (Math.abs(Math.cos(angle)) > Math.abs(Math.sin(angle))) {
                    graphics.fill(x - length, y - 1, x + length + 1, y + 2, color);
                } else {
                    graphics.fill(x - 1, y - length, x + 2, y + length + 1, color);
                }
            }
        }
    }

    private void renderExplosionParticle(GuiGraphicsExtractor graphics, int centerX, int centerY, float age, int seed, float offsetX, float offsetY, float scale) {
        int frame = Math.clamp((int) (age * 1.05F + seed % 3), 0, 15);
        Identifier texture = Identifier.withDefaultNamespace("textures/particle/explosion_" + frame + ".png");
        float fade = 1.0F - Mth.clamp(age / 16.0F, 0.0F, 1.0F);
        int size = Math.max(7, Math.round((28.0F + seed % 5 * 3.0F) * scale * (0.68F + fade * 0.52F)));
        int alpha = Math.round(255.0F * Mth.clamp(fade * 1.35F, 0.0F, 1.0F));
        int x = Math.round(centerX + offsetX) - size / 2;
        int y = Math.round(centerY + offsetY) - size / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 0.0F, 0.0F,
                size, size, 16, 16, 16, 16, alpha << 24 | 0x00FFFFFF);
    }

    private void playMaximumDiceSound() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) minecraft.player.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 0.85F, 1.55F);
    }

    private void playKnockoutExplosion() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null) {
            minecraft.player.playSound(SoundEvents.GENERIC_EXPLODE.value(), 1.15F, 0.92F);
        }
    }

    private float explosionCenterX(Layout layout) {
        return layout.x() + layout.width() - Math.clamp(layout.width() / 12.0F, 28.0F, 46.0F);
    }

    private float explosionCenterY(Layout layout) {
        return layout.y() + Math.clamp(layout.height() / 10.0F, 24.0F, 38.0F);
    }

    private void renderStatus(GuiGraphicsExtractor graphics, Layout layout) {
        Component status;
        if (this.view.selecting()) {
            status = Component.translatable("gui.astral_craft.board.battle_selecting");
        } else if (this.view.ready()) {
            status = Component.translatable("gui.astral_craft.board.battle_ready_phase");
        } else if (this.view.defenseChoice()) {
            status = coloredStatus(this.defenderName, false, "gui.astral_craft.board.battle_choose_defense");
        } else if (this.view.attackerRolling()) {
            status = coloredStatus(this.attackerName, true, "gui.astral_craft.board.battle_attacker_rolling");
        } else if (this.view.defenderRolling()) {
            status = coloredStatus(this.defenderName, false, this.view.defenseMode() == DefenseMode.EVADE
                    ? "gui.astral_craft.board.battle_chose_evade" : "gui.astral_craft.board.battle_chose_defend");
        } else if (this.view.result() && this.view.knockout()) {
            status = coloredStatus(this.defenderName, false, "gui.astral_craft.board.battle_knockout");
        } else if (this.view.result()) {
            status = coloredStatus(this.defenderName, false, "gui.astral_craft.board.battle_not_knockout");
        } else {
            status = Component.translatable("gui.astral_craft.board.battle_rolling");
        }

        int i = layout.x() + layout.width() / 2;
        graphics.text(this.font, status, i - this.font.width(status) / 2,
                layout.y() + 15, this.view.result() ? 0xFFFFFF80 : 0xFFFFFFFF, true);
        if (this.view.selecting() || this.view.defenseChoice()) {
            BoardDecisionProgressBar.render(graphics, this.font, this.characterId, this.skinId,
                    this.timeoutTicks, this.timeoutDurationTicks, i,
                    layout.y() + layout.height() - 16, Math.min(270, layout.width() - 50));
        }
    }

    private static Component coloredStatus(String name, boolean attacker, String translationKey) {
        MutableComponent coloredName = Component.literal(name).withStyle(attacker
                ? ChatFormatting.RED : ChatFormatting.BLUE, ChatFormatting.BOLD);
        return Component.translatable(translationKey, coloredName);
    }

    private void renderHand(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        List<Integer> visible = this.visibleCardIndexes();
        graphics.enableScissor(layout.cardX(), layout.cardY(), layout.cardRight(), layout.cardBottom());
        for (int visibleIndex = 0; visibleIndex < visible.size(); visibleIndex++) {
            int index = visible.get(visibleIndex);
            CardPosition position = layout.cardPosition(visibleIndex, this.cardScroll);
            if (index != this.draggingIndex && position.x() + CARD_W >= layout.cardX()
                    && position.x() <= layout.cardRight()) {
                this.renderCard(graphics, this.cards.get(index), position.x(), position.y(), mouseX, mouseY, false);
            }
        }

        graphics.disableScissor();
        if (this.draggingIndex >= 0 && this.draggingIndex < this.cards.size()) {
            this.renderCard(graphics, this.cards.get(this.draggingIndex), mouseX - this.dragOffsetX,
                    mouseY - this.dragOffsetY, mouseX, mouseY, true);
        }
    }

    private void renderCard(GuiGraphicsExtractor graphics, CombatCard card, int x, int y, int mouseX, int mouseY, boolean dragging) {
        HandCardRenderHelper.renderFramedCard(graphics, this.font, card.definition().type(),
                card.definition().largeFrontTexture(card.stack()), card.definition().displayName(card.stack()),
                x, y, mouseX, mouseY, dragging);
        Component cost = Component.translatable("gui.astral_craft.board.card_cost", card.cost());
        graphics.fill(x + 3, y + 3, x + 27, y + 16, 0xE0000000);
        graphics.text(this.font, cost, x + 5, y + 6, 0xFFFFD36B, true);
    }

    private void renderCost(GuiGraphicsExtractor graphics, Layout layout) {
        int remaining = Math.max(0, this.maximumCost - this.selectedCost());
        Component label = Component.translatable("gui.astral_craft.board.battle_cost_remaining", remaining);
        int labelY = layout.actionY() + 40;
        graphics.text(this.font, label, layout.actionX(), labelY, 0xFFFFD36B, false);
        int accent = this.role == BattleRole.DEFENDER ? DEFENSE_ACCENT : ATTACK_ACCENT;
        int diamondY = labelY + 13;
        for (int index = 0; index < this.maximumCost; index++) {
            this.renderCostDiamond(graphics, layout.actionX() + index * 15 + 5, diamondY,
                    index < remaining ? accent : 0xFF555560);
        }
    }

    private void renderCostDiamond(GuiGraphicsExtractor graphics, int centerX, int centerY, int color) {
        for (int row = -4; row <= 4; row++) {
            int halfWidth = 4 - Math.abs(row);
            graphics.fill(centerX - halfWidth, centerY + row,
                    centerX + halfWidth + 1, centerY + row + 1, color);
        }
        graphics.fill(centerX - 2, centerY - 2, centerX + 1, centerY + 1, 0x66FFFFFF);
    }

    private void renderActions(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        if (this.role == BattleRole.SPECTATOR) return;
        if (this.view.defenseChoice()) {
            if (this.role != BattleRole.DEFENDER) {
                AstralFancyButton.renderButton(graphics, this.font, Component.translatable("gui.astral_craft.board.waiting"),
                        layout.actionX(), layout.actionY(), layout.actionW(), 30, false, false,
                        ButtonStyle.button(0xFF555560));
                return;
            }

            int choiceWidth = Math.min(380, layout.width() - 32);
            int choiceX = layout.x() + (layout.width() - choiceWidth) / 2;
            int buttonWidth = (choiceWidth - 8) / 2;
            int evadeX = choiceX + buttonWidth + 8;
            Component defend = Component.translatable("gui.astral_craft.board.defend");
            int attackerDie = this.view.attackerDie();
            Component evade = attackerDie < 6
                    ? Component.translatable("gui.astral_craft.board.evade_greater_attack_roll", attackerDie)
                    : Component.translatable("gui.astral_craft.board.evade_equal_six");
            if (!this.view.evadeAllowed()) {
                graphics.centeredText(this.font,
                        Component.translatable("gui.astral_craft.board.all_or_nothing_no_evade"),
                        layout.x() + layout.width() / 2, layout.actionY() + 16, 0xFFFF7777);
            }

            AstralFancyButton.renderButton(graphics, this.font, defend, choiceX, layout.actionY(), buttonWidth, 30, false,
                    !this.submitted && inside(mouseX, mouseY, choiceX, layout.actionY(), buttonWidth, 30),
                    ButtonStyle.button(this.submitted ? 0xFF555560 : DEFENSE_ACCENT));
            boolean evadeEnabled = !this.submitted && this.view.evadeAllowed();
            AstralFancyButton.renderButton(graphics, this.font, evade, evadeX, layout.actionY(), buttonWidth, 30, false,
                    evadeEnabled && inside(mouseX, mouseY, evadeX, layout.actionY(), buttonWidth, 30),
                    ButtonStyle.button(evadeEnabled ? 0xFF69A94B : 0xFF555560));
            return;
        }

        if (!this.view.selecting()) return;
        int accent = this.role == BattleRole.DEFENDER ? DEFENSE_ACCENT : ATTACK_ACCENT;
        if (this.submitted) {
            AstralFancyButton.renderButton(graphics, this.font,
                    Component.translatable("gui.astral_craft.board.waiting"),
                    layout.actionX(), layout.actionY(), layout.actionW(), 30, false, false,
                    ButtonStyle.button(0xFF555560));
            return;
        }

        AstralFancyButton.renderButton(graphics, this.font, Component.translatable("gui.astral_craft.board.ready"),
                layout.actionX(), layout.actionY(), layout.actionW(), 34, false,
                inside(mouseX, mouseY, layout.actionX(), layout.actionY(), layout.actionW(), 34),
                ButtonStyle.button(accent));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0 && BoardTutorialGuide.mouseClicked(this.boardId, event.x(), event.y())) return true;
        if (event.button() != 0 || this.role == BattleRole.SPECTATOR) {
            return super.mouseClicked(event, doubleClick);
        }

        Layout layout = this.layout();
        if (this.view.defenseChoice() && this.role == BattleRole.DEFENDER && !this.submitted) {
            int choiceWidth = Math.min(380, layout.width() - 32);
            int choiceX = layout.x() + (layout.width() - choiceWidth) / 2;
            int buttonWidth = (choiceWidth - 8) / 2;
            int evadeX = choiceX + buttonWidth + 8;
            if (inside(event.x(), event.y(), choiceX, layout.actionY(), buttonWidth, 30)) {
                this.submit(DefenseMode.DEFEND);
                return true;
            }

            if (this.view.evadeAllowed() && inside(event.x(), event.y(), evadeX, layout.actionY(), buttonWidth, 30)) {
                this.submit(DefenseMode.EVADE);
                return true;
            }

            return super.mouseClicked(event, doubleClick);
        }

        if (!this.view.selecting() || this.submitted) return super.mouseClicked(event, doubleClick);
        if (inside(event.x(), event.y(), layout.actionX(), layout.actionY(), layout.actionW(), 34)) {
            this.submit(DefenseMode.DEFEND);
            return true;
        }

        List<Integer> visible = this.visibleCardIndexes();
        for (int visibleIndex = 0; visibleIndex < visible.size(); visibleIndex++) {
            int index = visible.get(visibleIndex);
            CardPosition position = layout.cardPosition(visibleIndex, this.cardScroll);
            if (event.x() >= layout.cardX() && event.x() <= layout.cardRight()
                    && event.y() >= layout.cardY() && event.y() <= layout.cardBottom()
                    && inside(event.x(), event.y(), position.x(), position.y(), CARD_W, CARD_H)) {
                if (this.selectedCost() + this.cards.get(index).cost() <= this.maximumCost) {
                    this.draggingIndex = index;
                    this.dragOffsetX = Math.clamp((int) event.x() - position.x(), 0, CARD_W);
                    this.dragOffsetY = Math.clamp((int) event.y() - position.y(), 0, CARD_H);
                }
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && this.draggingIndex >= 0) {
            int index = this.draggingIndex;
            this.draggingIndex = -1;
            Layout layout = this.layout();
            if (event.y() < layout.cardY() - 8
                    && this.selectedCost() + this.cards.get(index).cost() <= this.maximumCost) {
                if (this.selectedIndexes.add(index)) {
                    if (this.role == BattleRole.ATTACKER) this.attackerScoreFlashTicks = 7;
                    if (this.role == BattleRole.DEFENDER) this.defenderScoreFlashTicks = 7;
                }

                this.cardScroll = Math.clamp(this.cardScroll, 0.0F, this.maximumCardScroll(layout));
            }

            return true;
        }

        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        Layout layout = this.layout();
        if (this.view.selecting() && mouseX >= layout.cardX() && mouseX <= layout.cardRight() && mouseY >= layout.cardY() && mouseY <= layout.cardBottom()) {
            this.cardScroll = Math.clamp(this.cardScroll - (float) (deltaY + deltaX) * 34.0F, 0.0F, this.maximumCardScroll(layout));
            return true;
        }

        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    private void submit(DefenseMode defenseMode) {
        List<Integer> serverIndexes = this.selectedIndexes.stream()
                .map(index -> this.cards.get(index).handIndex()).toList();
        this.submitted = true;
        ClientPacketDistributor.sendToServer(new BoardBattleActionPayload(this.boardId, serverIndexes, defenseMode));
    }

    private int selectedCost() {
        return this.selectedIndexes.stream().mapToInt(index -> this.cards.get(index).cost()).sum();
    }

    private List<Integer> visibleCardIndexes() {
        List<Integer> indexes = new ArrayList<>();
        for (int index = 0; index < this.cards.size(); index++) {
            if (!this.selectedIndexes.contains(index)) indexes.add(index);
        }

        return indexes;
    }

    private LivingEntity entity(int id) {
        if (Minecraft.getInstance().level == null) return null;
        Entity entity = Minecraft.getInstance().level.getEntity(id);
        return entity instanceof LivingEntity living ? living : null;
    }

    private Layout layout() {
        int width = Math.clamp(this.width - 20, 300, 790);
        int height = Math.clamp(this.height - 20, 230, 430);
        int x = (this.width - width) / 2;
        int y = (this.height - height) / 2;
        int modelTop = y + 48;
        int cardY = y + Math.max(160, height - CARD_H - 18);
        int modelBottom = Math.max(modelTop + 100, cardY - 10);
        int actionW = Math.clamp(width / 5, 94, 148);
        int actionX = x + width - actionW - 16;
        int cardX = x + 16;
        int cardRight = Math.max(cardX + 1, actionX - 10);
        int cardBottom = y + height - 10;
        int actionY = Math.min(cardY + 18, y + height - 76);
        return new Layout(x, y, width, height, modelTop, modelBottom, cardX, cardY,
                cardRight, cardBottom, actionX, actionY, actionW);
    }

    private int numberHorizontalOffset(Layout layout) {
        return Math.clamp(layout.width() / 14, 22, 56);
    }

    private float maximumCardScroll(Layout layout) {
        int count = this.visibleCardIndexes().size();
        int contentWidth = Math.max(0, count * (CARD_W + CARD_GAP) - CARD_GAP);
        return Math.max(0, contentWidth - (layout.cardRight() - layout.cardX()));
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private record CombatCard(int handIndex, ItemStack stack, CardDefinition definition, int cost, int minimumBonus, int maximumBonus) {
        private static CombatCard from(CombatCardView view) {
            ItemStack stack = view.stack().copy();
            CardDefinition definition = stack.get(AstralDataComponents.CARD_DEFINITION);
            if (definition == null && stack.getItem() instanceof BaseHandCard card) definition = card.definition(stack);
            return new CombatCard(view.handIndex(), stack, definition, view.cost(),
                    view.minimumBonus(), view.maximumBonus());
        }
    }
    private record CardPosition(int x, int y) {}
    private record Range(int minimum, int maximum) {}

    private record Layout(int x, int y, int width, int height, int modelTop, int modelBottom,
                          int cardX, int cardY, int cardRight, int cardBottom,
                          int actionX, int actionY, int actionW) {
        private CardPosition cardPosition(int index, float scroll) {
            return new CardPosition(this.cardX + index * (CARD_W + CARD_GAP) - Math.round(scroll), this.cardY);
        }
    }

}
