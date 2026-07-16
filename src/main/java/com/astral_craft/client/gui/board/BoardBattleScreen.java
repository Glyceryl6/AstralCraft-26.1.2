package com.astral_craft.client.gui.board;

import com.astral_craft.client.gui.HandCardRenderHelper;
import com.astral_craft.client.gui.components.AstralFancyButton;
import com.astral_craft.client.gui.components.AstralFancyButton.ButtonStyle;
import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.network.c2s.BoardBattleActionPayload;
import com.astral_craft.common.network.s2c.OpenBoardBattlePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** In-world battle UI with irrevocable card selection and staged dice/result presentation. */
public class BoardBattleScreen extends Screen {

    private static final int CARD_W = HandCardRenderHelper.FRAMED_CARD_W;
    private static final int CARD_H = HandCardRenderHelper.FRAMED_CARD_H;
    private static final int CARD_GAP = 7;
    private static final int ATTACK_ACCENT = 0xFFD84B61;
    private static final int DEFENSE_ACCENT = 0xFF3F9DCE;
    private static final int DICE_FLASH_END_TICK = 24;
    private static final int BASE_VALUE_STAGE_TICK = 39;
    private static final int CARD_VALUE_STAGE_TICK = 56;
    private final String boardId;
    private final int attackerEntityId;
    private final int defenderEntityId;
    private final String attackerName;
    private final String defenderName;
    private final String role;
    private final List<CombatCard> cards;
    private final Set<Integer> selectedIndexes = new LinkedHashSet<>();
    private int draggingIndex = -1;
    private float cardScroll;
    private int dragOffsetX;
    private int dragOffsetY;
    private int timeoutTicks;
    private int maximumCost;
    private boolean submitted;
    private BattleView view;
    private int phaseAgeTicks;

    public BoardBattleScreen(OpenBoardBattlePayload payload) {
        super(Component.translatable("gui.astral_craft.board.battle"));
        this.boardId = payload.boardId();
        this.attackerEntityId = payload.attackerEntityId();
        this.defenderEntityId = payload.defenderEntityId();
        this.attackerName = payload.attackerName();
        this.defenderName = payload.defenderName();
        this.role = payload.role();
        this.cards = decode(payload.encodedCards());
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

    private void apply(OpenBoardBattlePayload payload) {
        BattleView next = BattleView.parse(payload.resultText());
        boolean phaseChanged = this.view == null || !this.view.phase().equals(next.phase());
        if (phaseChanged) {
            this.phaseAgeTicks = 0;
            if (next.defenseChoice() && "defender".equals(this.role)) this.submitted = false;
        }
        this.view = next;
        this.timeoutTicks = Math.max(0, payload.decisionTicks());
        this.maximumCost = Math.max(0, payload.maximumCost());
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
        if (this.timeoutTicks > 0) this.timeoutTicks--;
        if (this.view.selecting() && this.timeoutTicks <= 1 && !this.submitted && !"spectator".equals(this.role)) {
            this.submit("defend");
        }
        if (this.view.defenseChoice() && this.timeoutTicks <= 1 && !this.submitted && "defender".equals(this.role)) {
            this.submit("defend");
        }
        if (this.view.result() && this.timeoutTicks <= 0) this.onClose();
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = this.layout();
        this.renderArena(graphics, layout);
        this.renderNames(graphics, layout);
        LivingEntity attacker = this.entity(this.attackerEntityId);
        LivingEntity defender = this.entity(this.defenderEntityId);
        BoardScreenEntityRenderer.render(graphics, attacker, layout.x() + 22, layout.modelTop(),
                layout.x() + layout.width() / 2 - 24, layout.modelBottom(), 225.0F);
        BoardScreenEntityRenderer.render(graphics, defender, layout.x() + layout.width() / 2 + 24,
                layout.modelTop(), layout.x() + layout.width() - 22, layout.modelBottom(), -225.0F);
        this.renderHealth(graphics, this.view.attackerHealth(), layout.x() + layout.width() / 4, layout.modelBottom() - 11);
        this.renderHealth(graphics, this.view.defenderHealth(), layout.x() + layout.width() * 3 / 4, layout.modelBottom() - 11);
        this.renderBattleNumbers(graphics, layout);
        this.renderDamagePopup(graphics, layout);
        if (this.view.selecting()) {
            this.renderHand(graphics, layout, mouseX, mouseY);
            this.renderCost(graphics, layout);
        }

        if (this.view.selecting() || this.view.defenseChoice()) {
            this.renderActions(graphics, layout, mouseX, mouseY);
        }

        this.renderStatus(graphics, layout);
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

    private void renderHealth(GuiGraphicsExtractor graphics, int value, int centerX, int y) {
        Component health = Component.literal("♥ " + Math.max(0, value));
        int width = this.font.width(health) + 12;
        graphics.fill(centerX - width / 2, y, centerX + width / 2, y + 15, 0xE8FFFFFF);
        graphics.text(this.font, health, centerX - this.font.width(health) / 2, y + 3, 0xFFC72E4E, true);
    }

    private void renderBattleNumbers(GuiGraphicsExtractor graphics, Layout layout) {
        int center = layout.x() + layout.width() / 2;
        int i = Math.clamp(layout.width() / 9, 58, 92);
        int attackX = center - i;
        int defenseX = center + i;
        int y = this.view.selecting() ? layout.modelTop() + 28 : layout.modelTop() + 18;
        if (this.view.selecting()) {
            Range attackRange = this.displayRange(true);
            Range defenseRange = this.displayRange(false);
            this.renderFraction(graphics, attackRange.minimum(), attackRange.maximum(), attackX, y, ATTACK_ACCENT);
            this.renderFraction(graphics, defenseRange.minimum(), defenseRange.maximum(), defenseX, y, DEFENSE_ACCENT);
            this.renderReadyState(graphics, layout, true);
            this.renderReadyState(graphics, layout, false);
            return;
        }

        if (this.shouldRenderAnimatedValue(true)) {
            this.renderScaledCenteredText(graphics, Component.literal(Integer.toString(this.animatedValue(true))),
                    attackX, y + 8, this.animatedValueColor(true), this.numberScale(true), true);
        }
        if (this.shouldRenderAnimatedValue(false)) {
            this.renderScaledCenteredText(graphics, Component.literal(Integer.toString(this.animatedValue(false))),
                    defenseX, y + 8, this.animatedValueColor(false), this.numberScale(false), true);
        }
        if ((this.view.defenderRolling() && this.phaseAgeTicks >= 54 || this.view.result())
                && "evade".equals(this.view.defenseMode())) {
            Component evade = Component.translatable(this.view.evaded()
                    ? "gui.astral_craft.board.evade_success" : "gui.astral_craft.board.evade_failed");
            this.renderScaledCenteredText(graphics, evade, defenseX, y + 43,
                    this.view.evaded() ? 0xFF79FF8A : 0xFFFF7373, 1.35F, true);
        }
    }

    private void renderReadyState(GuiGraphicsExtractor graphics, Layout layout, boolean attacker) {
        boolean ready = attacker ? this.view.attackerReady() : this.view.defenderReady();
        String name = attacker ? this.attackerName : this.defenderName;
        int color = attacker ? ATTACK_ACCENT : DEFENSE_ACCENT;
        int centerX = attacker ? layout.x() + layout.width() / 4 : layout.x() + layout.width() * 3 / 4;
        int y = layout.modelTop() + 6;
        Component text = ready
                ? Component.translatable("gui.astral_craft.board.battle_ready_overhead")
                : coloredStatus(name, attacker, "gui.astral_craft.board.battle_playing_cards");
        int width = this.font.width(text) + 12;
        graphics.fill(centerX - width / 2, y, centerX + width / 2, y + 16, 0xC9000000);
        graphics.text(this.font, text, centerX - this.font.width(text) / 2, y + 4,
                ready ? 0xFF8CFF9A : color, true);
    }

    private float numberScale(boolean attack) {
        float base = 2.85F;
        int pulseAge = -1;
        boolean rollingSide = this.view.attackerRolling() && attack || this.view.defenderRolling();
        if (rollingSide && this.phaseAgeTicks < DICE_FLASH_END_TICK + 2) {
            pulseAge = Math.floorMod(this.phaseAgeTicks, 4);
        } else if (this.view.defenderRolling()
                && (attack || !"evade".equals(this.view.defenseMode()))) {
            int fromBase = Math.abs(this.phaseAgeTicks - BASE_VALUE_STAGE_TICK);
            int fromBonus = Math.abs(this.phaseAgeTicks - CARD_VALUE_STAGE_TICK);
            pulseAge = Math.min(fromBase, fromBonus);
        }
        if (pulseAge < 0 || pulseAge > 4) return base;
        float pulse = 1.0F - Mth.clamp(pulseAge / 4.0F, 0.0F, 1.0F);
        return base + pulse * 0.85F;
    }

    private void renderScaledCenteredText(
            GuiGraphicsExtractor graphics, Component text,
            int centerX, int y, int color, float scale, boolean shadow) {
        graphics.pose().pushMatrix();
        graphics.pose().translate(centerX, y);
        graphics.pose().scale(scale, scale);
        graphics.text(this.font, text, -this.font.width(text) / 2, -4, color, shadow);
        graphics.pose().popMatrix();
    }

    private Range displayRange(boolean attack) {
        int minimum = attack ? this.view.attackMinimum() : this.view.defenseMinimum();
        int maximum = attack ? this.view.attackMaximum() : this.view.defenseMaximum();
        if (this.view.selecting() && ((attack && "attacker".equals(this.role))
                || (!attack && "defender".equals(this.role)))) {
            int base = attack ? this.view.attackBase() : this.view.defenseBase();
            int minBonus = this.selectedIndexes.stream().mapToInt(index -> this.cards.get(index).minimumBonus()).sum();
            int maxBonus = this.selectedIndexes.stream().mapToInt(index -> this.cards.get(index).maximumBonus()).sum();
            minimum = base + minBonus;
            maximum = base + maxBonus;
        }
        return new Range(minimum, maximum);
    }

    private void renderFraction(GuiGraphicsExtractor graphics, int numerator, int denominator, int centerX, int y, int color) {
        Component top = Component.literal(Integer.toString(numerator));
        Component bottom = Component.literal(Integer.toString(denominator));
        float scale = 1.65F;
        int width = Math.round(Math.max(this.font.width(top), this.font.width(bottom)) * scale) + 14;
        graphics.fill(centerX - width / 2, y, centerX + width / 2, y + 37, 0xC8000000);
        this.renderScaledCenteredText(graphics, top, centerX, y + 8, color, scale, true);
        graphics.fill(centerX - width / 2 + 3, y + 18, centerX + width / 2 - 3, y + 20, 0xFFE8E8E8);
        this.renderScaledCenteredText(graphics, bottom, centerX, y + 29, color, scale, true);
    }

    private boolean shouldRenderAnimatedValue(boolean attack) {
        if (this.view.result() || this.view.defenderRolling()) return true;
        if (this.view.attackerRolling() || this.view.defenseChoice()) return attack;
        return false;
    }

    private int animatedValueColor(boolean attack) {
        boolean rollingSide = this.view.attackerRolling() && attack || this.view.defenderRolling();
        boolean flash = rollingSide && this.phaseAgeTicks < DICE_FLASH_END_TICK;
        if (this.view.defenderRolling() && (attack || !"evade".equals(this.view.defenseMode()))) {
            flash |= Math.abs(this.phaseAgeTicks - BASE_VALUE_STAGE_TICK) <= 4;
            flash |= Math.abs(this.phaseAgeTicks - CARD_VALUE_STAGE_TICK) <= 4;
        }
        if (flash && Math.floorMod(this.phaseAgeTicks, 4) < 2) return 0xFFFFFFFF;
        return attack ? 0xFFFFE3A0 : 0xFFA8E8FF;
    }

    private int animatedValue(boolean attack) {
        int die = attack ? this.view.attackerDie() : this.view.defenderDie();
        int base = attack ? this.view.attackBase() : this.view.defenseBase();
        if (this.view.result()) return attack ? this.view.attackTotal() : this.view.defenseTotal();
        if (this.view.attackerRolling()) {
            if (!attack) return 0;
            return this.phaseAgeTicks < DICE_FLASH_END_TICK
                    ? 1 + Math.floorMod(this.phaseAgeTicks * 5 + 1, 6) : die;
        }
        if (this.view.defenseChoice()) return attack ? die : 0;
        if (this.view.defenderRolling()) {
            int age = this.phaseAgeTicks;
            if (age < DICE_FLASH_END_TICK) {
                return attack ? die : 1 + Math.floorMod(age * 7 + 3, 6);
            }
            if (age < BASE_VALUE_STAGE_TICK) return die;
            if (!attack && "evade".equals(this.view.defenseMode())) return die;
            if (age < CARD_VALUE_STAGE_TICK) return die + base;
            return attack ? this.view.attackTotal() : this.view.defenseTotal();
        }
        return 0;
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

    private void renderStatus(GuiGraphicsExtractor graphics, Layout layout) {
        Component status;
        if (this.view.selecting()) {
            status = Component.translatable("gui.astral_craft.board.battle_selecting");
        } else if (this.view.attackerRolling()) {
            status = coloredStatus(this.attackerName, true, "gui.astral_craft.board.battle_attacker_rolling");
        } else if (this.view.defenseChoice()) {
            status = coloredStatus(this.defenderName, false, "gui.astral_craft.board.battle_choose_defense");
        } else if (this.view.defenderRolling()) {
            status = coloredStatus(this.defenderName, false, "gui.astral_craft.board.battle_defender_rolling");
        } else if (this.view.knockout()) {
            status = coloredStatus(this.defenderName, false, "gui.astral_craft.board.battle_knockout");
        } else {
            status = coloredStatus(this.defenderName, false, "gui.astral_craft.board.battle_not_knockout");
        }

        int statusWidth = Math.max(156, this.font.width(status) + 20);
        int i = layout.x() + layout.width() / 2;
        graphics.fill(i - statusWidth / 2, layout.y() + 9,
                layout.x() + layout.width() / 2 + statusWidth / 2, layout.y() + 29, 0xD0050509);
        graphics.text(this.font, status, i - this.font.width(status) / 2,
                layout.y() + 15, this.view.result() ? 0xFFFFFF80 : 0xFFFFFFFF, true);
        if (this.view.selecting() || this.view.defenseChoice()) {
            Component timer = Component.translatable("gui.astral_craft.board.timeout", (this.timeoutTicks + 19) / 20);
            graphics.text(this.font, timer, i - this.font.width(timer) / 2, layout.y() + 32, 0xFFBFC8FF, false);
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
            if (index != this.draggingIndex && position.x() + CARD_W >= layout.cardX() && position.x() <= layout.cardRight()) {
                this.renderCard(graphics, this.cards.get(index), position.x(), position.y(), mouseX, mouseY, false);
            }
        }

        graphics.disableScissor();
        if (this.draggingIndex >= 0 && this.draggingIndex < this.cards.size()) {
            this.renderCard(graphics, this.cards.get(this.draggingIndex), mouseX - this.dragOffsetX,
                    mouseY - this.dragOffsetY, mouseX, mouseY, true);
        }
    }

    private void renderCard(GuiGraphicsExtractor graphics, CombatCard card, int x, int y,
                            int mouseX, int mouseY, boolean dragging) {
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
        graphics.text(this.font, label, layout.actionX(), layout.actionY() - 30, 0xFFFFD36B, false);
        int accent = "defender".equals(this.role) ? DEFENSE_ACCENT : ATTACK_ACCENT;
        for (int index = 0; index < this.maximumCost; index++) {
            Component diamond = Component.literal("◆");
            graphics.text(this.font, diamond, layout.actionX() + index * 16, layout.actionY() - 16,
                    index < remaining ? accent : 0xFF555560, true);
        }
    }

    private void renderActions(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        if ("spectator".equals(this.role)) return;
        if (this.view.defenseChoice()) {
            if (!"defender".equals(this.role)) {
                AstralFancyButton.renderButton(graphics, this.font,
                        Component.translatable("gui.astral_craft.board.waiting"),
                        layout.actionX(), layout.actionY(), layout.actionW(), 30, false, false,
                        ButtonStyle.button(0xFF555560));
                return;
            }
            Component defend = Component.translatable("gui.astral_craft.board.defend");
            Component evade = Component.translatable("gui.astral_craft.board.evade");
            AstralFancyButton.renderButton(graphics, this.font, defend, layout.actionX(), layout.actionY(),
                    layout.actionW(), 30, false,
                    !this.submitted && inside(mouseX, mouseY, layout.actionX(), layout.actionY(), layout.actionW(), 30),
                    ButtonStyle.button(this.submitted ? 0xFF555560 : DEFENSE_ACCENT));
            AstralFancyButton.renderButton(graphics, this.font, evade, layout.actionX(), layout.actionY() + 38,
                    layout.actionW(), 30, false,
                    !this.submitted && inside(mouseX, mouseY, layout.actionX(), layout.actionY() + 38,
                            layout.actionW(), 30),
                    ButtonStyle.button(this.submitted ? 0xFF555560 : 0xFF69A94B));
            return;
        }
        if (!this.view.selecting()) return;
        int accent = "defender".equals(this.role) ? DEFENSE_ACCENT : ATTACK_ACCENT;
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
        if (event.button() != 0 || "spectator".equals(this.role)) {
            return super.mouseClicked(event, doubleClick);
        }
        Layout layout = this.layout();
        if (this.view.defenseChoice() && "defender".equals(this.role) && !this.submitted) {
            if (inside(event.x(), event.y(), layout.actionX(), layout.actionY(), layout.actionW(), 30)) {
                this.submit("defend");
                return true;
            }
            if (inside(event.x(), event.y(), layout.actionX(), layout.actionY() + 38,
                    layout.actionW(), 30)) {
                this.submit("evade");
                return true;
            }
            return super.mouseClicked(event, doubleClick);
        }
        if (!this.view.selecting() || this.submitted) return super.mouseClicked(event, doubleClick);
        if (inside(event.x(), event.y(), layout.actionX(), layout.actionY(), layout.actionW(), 34)) {
            this.submit("defend");
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
                this.selectedIndexes.add(index);
                this.cardScroll = Math.clamp(this.cardScroll, 0.0F, this.maximumCardScroll(layout));
            }
            return true;
        }
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        Layout layout = this.layout();
        if (this.view.selecting() && mouseX >= layout.cardX() && mouseX <= layout.cardRight()
                && mouseY >= layout.cardY() && mouseY <= layout.cardBottom()) {
            this.cardScroll = Math.clamp(this.cardScroll - (float) (deltaY + deltaX) * 34.0F,
                    0.0F, this.maximumCardScroll(layout));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    private void submit(String defenseMode) {
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
        int modelBottom = Math.max(modelTop + 70, cardY - 10);
        int actionW = Math.clamp(width / 5, 94, 148);
        int actionX = x + width - actionW - 16;
        int cardX = x + 16;
        int cardRight = Math.max(cardX + 1, actionX - 10);
        int cardBottom = y + height - 10;
        int actionY = Math.min(cardY + 18, y + height - 76);
        return new Layout(x, y, width, height, modelTop, modelBottom, cardX, cardY,
                cardRight, cardBottom, actionX, actionY, actionW);
    }

    private float maximumCardScroll(Layout layout) {
        int count = this.visibleCardIndexes().size();
        int contentWidth = Math.max(0, count * (CARD_W + CARD_GAP) - CARD_GAP);
        return Math.max(0, contentWidth - (layout.cardRight() - layout.cardX()));
    }

    private static List<CombatCard> decode(String encoded) {
        List<CombatCard> result = new ArrayList<>();
        if (encoded == null || encoded.isBlank()) return result;
        for (String raw : encoded.split(";")) {
            String[] parts = raw.split(",", 5);
            if (parts.length != 5) continue;
            try {
                int handIndex = Integer.parseInt(parts[0]);
                Identifier id = Identifier.parse(parts[1]);
                int cost = Math.max(0, Integer.parseInt(parts[2]));
                int minimumBonus = Math.max(0, Integer.parseInt(parts[3]));
                int maximumBonus = Math.max(minimumBonus, Integer.parseInt(parts[4]));
                Item item = BuiltInRegistries.ITEM.getValue(id);
                if (!(item instanceof BaseHandCard card)) continue;
                ItemStack stack = new ItemStack(item);
                result.add(new CombatCard(handIndex, stack, card.definition(stack), cost, minimumBonus, maximumBonus));
            } catch (IllegalArgumentException ignored) {}
        }
        return List.copyOf(result);
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private record CombatCard(int handIndex, ItemStack stack, CardDefinition definition, int cost,
                              int minimumBonus, int maximumBonus) {}
    private record CardPosition(int x, int y) {}
    private record Range(int minimum, int maximum) {}

    private record BattleView(String phase, int attackerHealth, int defenderHealth,
                              int attackBase, int defenseBase, int attackMinimum, int attackMaximum,
                              int defenseMinimum, int defenseMaximum, int attackerDie, int defenderDie,
                              int attackBonus, int defenseBonus, int attackTotal, int defenseTotal,
                              int damage, boolean evaded, boolean knockout,
                              boolean attackerReady, boolean defenderReady, String defenseMode) {
        private boolean selecting() { return "select".equals(this.phase); }
        private boolean attackerRolling() { return "attacker_roll".equals(this.phase); }
        private boolean defenseChoice() { return "defense_choice".equals(this.phase); }
        private boolean defenderRolling() { return "defender_roll".equals(this.phase); }
        private boolean result() { return "result".equals(this.phase); }

        private static BattleView parse(String encoded) {
            String[] values = encoded == null ? new String[0] : encoded.split("\\|", -1);
            if (values.length < 21) return empty();
            try {
                return new BattleView(values[0], Integer.parseInt(values[1]), Integer.parseInt(values[2]),
                        Integer.parseInt(values[3]), Integer.parseInt(values[4]),
                        Integer.parseInt(values[5]), Integer.parseInt(values[6]),
                        Integer.parseInt(values[7]), Integer.parseInt(values[8]),
                        Integer.parseInt(values[9]), Integer.parseInt(values[10]),
                        Integer.parseInt(values[11]), Integer.parseInt(values[12]),
                        Integer.parseInt(values[13]), Integer.parseInt(values[14]),
                        Integer.parseInt(values[15]), Boolean.parseBoolean(values[16]),
                        Boolean.parseBoolean(values[17]), Boolean.parseBoolean(values[18]),
                        Boolean.parseBoolean(values[19]), values[20]);
            } catch (NumberFormatException exception) {
                return empty();
            }
        }

        private static BattleView empty() {
            return new BattleView("select", 0, 0, 0, 0, 0, 0, 0, 0,
                    0, 0, 0, 0, 0, 0, 0, false, false, false, false, "defend");
        }
    }

    private record Layout(int x, int y, int width, int height, int modelTop, int modelBottom,
                          int cardX, int cardY, int cardRight, int cardBottom,
                          int actionX, int actionY, int actionW) {
        private CardPosition cardPosition(int index, float scroll) {
            return new CardPosition(this.cardX + index * (CARD_W + CARD_GAP) - Math.round(scroll), this.cardY);
        }
    }
}
