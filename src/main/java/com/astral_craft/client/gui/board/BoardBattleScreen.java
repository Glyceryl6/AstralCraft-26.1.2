package com.astral_craft.client.gui.board;

import com.astral_craft.client.gui.HandCardRenderHelper;
import com.astral_craft.client.gui.components.AstralFancyButton;
import com.astral_craft.client.gui.components.AstralFancyButton.ButtonStyle;
import com.astral_craft.common.components.CardDefinition;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.network.BoardBattleActionPayload;
import com.astral_craft.common.network.OpenBoardBattlePayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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

/** In-world battle presentation with irrevocable combat-card spending and server-side resolution. */
public class BoardBattleScreen extends Screen {

    private static final int CARD_W = HandCardRenderHelper.FRAMED_CARD_W;
    private static final int CARD_H = HandCardRenderHelper.FRAMED_CARD_H;
    private static final int CARD_GAP = 7;
    private static final int ATTACK_ACCENT = 0xFFD84B61;
    private static final int DEFENSE_ACCENT = 0xFF3F9DCE;
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
    private boolean cardsLocked;
    private boolean resolved;
    private boolean submitted;
    private String resultText;

    public BoardBattleScreen(OpenBoardBattlePayload payload) {
        super(Component.translatable("gui.astral_craft.board.battle"));
        this.boardId = payload.boardId();
        this.attackerEntityId = payload.attackerEntityId();
        this.defenderEntityId = payload.defenderEntityId();
        this.attackerName = payload.attackerName();
        this.defenderName = payload.defenderName();
        this.role = payload.role();
        this.cards = decode(payload.encodedCards());
        this.timeoutTicks = Math.max(0, payload.decisionTicks());
        this.maximumCost = Math.max(0, payload.maximumCost());
        this.resolved = payload.resolved();
        this.resultText = payload.resultText();
    }

    public static void open(OpenBoardBattlePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Screen current = Minecraft.getInstance().screen;
            if (current instanceof BoardBattleScreen battle && battle.boardId.equals(payload.boardId())) {
                battle.timeoutTicks = Math.max(0, payload.decisionTicks());
                battle.maximumCost = Math.max(0, payload.maximumCost());
                battle.resolved = payload.resolved();
                battle.resultText = payload.resultText();
            } else {
                Minecraft.getInstance().setScreen(new BoardBattleScreen(payload));
            }
        });
    }

    @Override
    protected void init() {
        this.cardScroll = Math.clamp(this.cardScroll, 0.0F, this.maximumCardScroll(this.layout()));
    }

    @Override
    public boolean isPauseScreen() { return false; }

    @Override
    public void tick() {
        super.tick();
        if (this.timeoutTicks > 0) this.timeoutTicks--;
        if (!this.resolved && this.timeoutTicks <= 1 && !this.submitted && !"spectator".equals(this.role)) {
            this.submit("defend");
        }
        if (this.resolved && this.timeoutTicks <= 0) this.onClose();
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = this.layout();
        this.renderArena(graphics, layout);

        Component attackLabel = Component.translatable("gui.astral_craft.board.attack");
        Component defenseLabel = Component.translatable("gui.astral_craft.board.defense");
        graphics.text(this.font, attackLabel, layout.x() + 18, layout.y() + 14, 0xFFFF6B74, true);
        graphics.text(this.font, defenseLabel,
                layout.x() + layout.width() - 18 - this.font.width(defenseLabel), layout.y() + 14,
                0xFF67D9FF, true);
        graphics.text(this.font, this.attackerName, layout.x() + 18, layout.y() + 29, 0xFFFFFFFF, false);
        graphics.text(this.font, this.defenderName,
                layout.x() + layout.width() - 18 - this.font.width(this.defenderName), layout.y() + 29,
                0xFFFFFFFF, false);

        LivingEntity attacker = this.entity(this.attackerEntityId);
        LivingEntity defender = this.entity(this.defenderEntityId);
        BoardScreenEntityRenderer.render(graphics, attacker, layout.x() + 22, layout.modelTop(),
                layout.x() + layout.width() / 2 - 24, layout.modelBottom(), -90.0F);
        BoardScreenEntityRenderer.render(graphics, defender, layout.x() + layout.width() / 2 + 24,
                layout.modelTop(), layout.x() + layout.width() - 22, layout.modelBottom(), 90.0F);
        if (attacker != null) this.renderHealth(graphics, attacker, layout.x() + layout.width() / 4, layout.modelBottom() - 11);
        if (defender != null) this.renderHealth(graphics, defender, layout.x() + layout.width() * 3 / 4, layout.modelBottom() - 11);

        this.renderHand(graphics, layout, mouseX, mouseY);
        this.renderCost(graphics, layout);
        this.renderActions(graphics, layout, mouseX, mouseY);

        Component timer = Component.translatable("gui.astral_craft.board.timeout", (this.timeoutTicks + 19) / 20);
        graphics.fill(layout.x() + layout.width() / 2 - 72, layout.y() + 10,
                layout.x() + layout.width() / 2 + 72, layout.y() + 28, 0xCC050509);
        graphics.text(this.font, timer, layout.x() + layout.width() / 2 - this.font.width(timer) / 2,
                layout.y() + 15, 0xFFFFFFFF, false);
        if (this.resolved && this.resultText != null) {
            graphics.fill(layout.x() + 70, layout.y() + layout.height() / 2 - 18,
                    layout.x() + layout.width() - 70, layout.y() + layout.height() / 2 + 18, 0xE8000000);
            graphics.text(this.font, this.resultText,
                    layout.x() + layout.width() / 2 - this.font.width(this.resultText) / 2,
                    layout.y() + layout.height() / 2 - 4, 0xFFFFFF80, true);
        }
    }

    private void renderArena(GuiGraphicsExtractor graphics, Layout layout) {
        graphics.fill(layout.x(), layout.y(), layout.x() + layout.width(), layout.y() + layout.height(), 0xF014141C);
        graphics.fill(layout.x(), layout.y(), layout.x() + layout.width(), layout.y() + 3, 0xD0FFFFFF);
        int arenaTop = layout.modelTop() + 18;
        int arenaBottom = layout.modelBottom();
        graphics.fill(layout.x() + 8, arenaTop, layout.x() + layout.width() - 8, arenaBottom, 0xFFB84E18);
        graphics.fill(layout.x() + 18, arenaTop + 8, layout.x() + layout.width() - 18, arenaBottom - 7, 0xFFFF8B18);
        graphics.fill(layout.x() + 66, arenaTop + 18, layout.x() + layout.width() - 66, arenaBottom - 17, 0xFFFFC431);
        graphics.fill(layout.x() + 8, arenaBottom, layout.x() + layout.width() - 8, arenaBottom + 4, 0xFF301A20);
    }

    private void renderHealth(GuiGraphicsExtractor graphics, LivingEntity entity, int centerX, int y) {
        Component health = Component.literal("♥ " + Math.max(0, Math.round(entity.getHealth())));
        int width = this.font.width(health) + 12;
        graphics.fill(centerX - width / 2, y, centerX + width / 2, y + 15, 0xE8FFFFFF);
        graphics.text(this.font, health, centerX - this.font.width(health) / 2, y + 3, 0xFFC72E4E, true);
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
            boolean available = index < remaining;
            Component diamond = Component.literal("◆");
            graphics.text(this.font, diamond, layout.actionX() + index * 16, layout.actionY() - 16,
                    available ? accent : 0xFF555560, true);
        }
    }

    private void renderActions(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        if ("spectator".equals(this.role) || this.resolved) return;
        int accent = "defender".equals(this.role) ? DEFENSE_ACCENT : ATTACK_ACCENT;
        if (this.submitted) {
            Component waiting = Component.translatable("gui.astral_craft.board.waiting");
            AstralFancyButton.renderButton(graphics, this.font, waiting, layout.actionX(), layout.actionY(),
                    layout.actionW(), 30, false, false, ButtonStyle.button(0xFF555560));
            return;
        }
        if ("defender".equals(this.role) && this.cardsLocked) {
            Component defend = Component.translatable("gui.astral_craft.board.defend");
            Component evade = Component.translatable("gui.astral_craft.board.evade");
            boolean defendHover = inside(mouseX, mouseY, layout.actionX(), layout.actionY(), layout.actionW(), 30);
            boolean evadeHover = inside(mouseX, mouseY, layout.actionX(), layout.actionY() + 38, layout.actionW(), 30);
            AstralFancyButton.renderButton(graphics, this.font, defend, layout.actionX(), layout.actionY(),
                    layout.actionW(), 30, false, defendHover, ButtonStyle.button(DEFENSE_ACCENT));
            AstralFancyButton.renderButton(graphics, this.font, evade, layout.actionX(), layout.actionY() + 38,
                    layout.actionW(), 30, false, evadeHover, ButtonStyle.button(0xFF69A94B));
            return;
        }
        Component ready = Component.translatable("gui.astral_craft.board.ready");
        boolean readyHover = inside(mouseX, mouseY, layout.actionX(), layout.actionY(), layout.actionW(), 34);
        AstralFancyButton.renderButton(graphics, this.font, ready, layout.actionX(), layout.actionY(),
                layout.actionW(), 34, false, readyHover, ButtonStyle.button(accent));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0 || this.resolved || this.submitted || "spectator".equals(this.role)) {
            return super.mouseClicked(event, doubleClick);
        }
        Layout layout = this.layout();
        if ("defender".equals(this.role) && this.cardsLocked) {
            if (inside(event.x(), event.y(), layout.actionX(), layout.actionY(), layout.actionW(), 30)) {
                this.submit("defend");
                return true;
            }
            if (inside(event.x(), event.y(), layout.actionX(), layout.actionY() + 38, layout.actionW(), 30)) {
                this.submit("evade");
                return true;
            }
            return super.mouseClicked(event, doubleClick);
        }
        if (inside(event.x(), event.y(), layout.actionX(), layout.actionY(), layout.actionW(), 34)) {
            if ("defender".equals(this.role)) this.cardsLocked = true;
            else this.submit("defend");
            return true;
        }
        if (this.cardsLocked) return super.mouseClicked(event, doubleClick);
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
        if (mouseX >= layout.cardX() && mouseX <= layout.cardRight()
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
        int width = Math.min(790, Math.max(300, this.width - 20));
        int height = Math.min(430, Math.max(230, this.height - 20));
        int x = (this.width - width) / 2;
        int y = (this.height - height) / 2;
        int modelTop = y + 38;
        int cardY = y + Math.max(160, height - CARD_H - 18);
        int modelBottom = Math.max(modelTop + 70, cardY - 10);
        int actionW = Math.min(148, Math.max(94, width / 5));
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
            String[] parts = raw.split(",", 3);
            if (parts.length != 3) continue;
            try {
                int handIndex = Integer.parseInt(parts[0]);
                Identifier id = Identifier.parse(parts[1]);
                int cost = Math.max(0, Integer.parseInt(parts[2]));
                Item item = BuiltInRegistries.ITEM.getValue(id);
                if (!(item instanceof BaseHandCard card)) continue;
                ItemStack stack = new ItemStack(item);
                result.add(new CombatCard(handIndex, stack, card.definition(stack), cost));
            } catch (IllegalArgumentException ignored) {}
        }
        return List.copyOf(result);
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private record CombatCard(int handIndex, ItemStack stack, CardDefinition definition, int cost) {}

    private record CardPosition(int x, int y) {}

    private record Layout(int x, int y, int width, int height, int modelTop, int modelBottom,
                          int cardX, int cardY, int cardRight, int cardBottom,
                          int actionX, int actionY, int actionW) {
        CardPosition cardPosition(int index, float scroll) {
            return new CardPosition(this.cardX + index * (CARD_W + CARD_GAP) - Math.round(scroll), this.cardY);
        }
    }
}
