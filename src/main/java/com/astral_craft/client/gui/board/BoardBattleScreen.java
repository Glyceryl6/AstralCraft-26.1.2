package com.astral_craft.client.gui.board;

import com.astral_craft.client.gui.HandCardRenderHelper;
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

/** Transparent battle panel: the world remains visible while combat cards are selected. */
public class BoardBattleScreen extends Screen {

    private static final int CARD_W = HandCardRenderHelper.FRAMED_CARD_W;
    private static final int CARD_H = HandCardRenderHelper.FRAMED_CARD_H;
    private static final int CARD_GAP = 7;
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
    private String defenseMode = "defend";
    private int timeoutTicks;
    private int maximumCost;
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
        this.timeoutTicks = payload.decisionTicks();
        this.maximumCost = Math.max(0, payload.maximumCost());
        this.resolved = payload.resolved();
        this.resultText = payload.resultText();
    }

    public static void open(OpenBoardBattlePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            Screen current = Minecraft.getInstance().screen;
            if (current instanceof BoardBattleScreen battle && battle.boardId.equals(payload.boardId())) {
                battle.timeoutTicks = payload.decisionTicks();
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
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.timeoutTicks > 0) this.timeoutTicks--;
        if (this.resolved && this.timeoutTicks <= 0) this.onClose();
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = this.layout();
        graphics.fill(layout.x(), layout.y(), layout.x() + layout.width(), layout.y() + layout.height(), 0xE813131D);
        graphics.fill(layout.x(), layout.y(), layout.x() + layout.width(), layout.y() + 3, 0xC0FFFFFF);
        graphics.text(this.font, this.attackerName, layout.x() + 30, layout.y() + 18, 0xFFFF7B82, false);
        graphics.text(this.font, this.defenderName,
                layout.x() + layout.width() - 30 - this.font.width(this.defenderName), layout.y() + 18,
                0xFF7BBEFF, false);
        LivingEntity attacker = this.entity(this.attackerEntityId);
        LivingEntity defender = this.entity(this.defenderEntityId);
        int i = layout.x() + layout.width() / 2;
        BoardScreenEntityRenderer.render(graphics, attacker, layout.x() + 16, layout.y() + 36,
                i - 8, layout.cardY() - 8, -90.0F);
        BoardScreenEntityRenderer.render(graphics, defender, layout.x() + layout.width() / 2 + 8,
                layout.y() + 36, layout.x() + layout.width() - 16, layout.cardY() - 8, 90.0F);
        graphics.enableScissor(layout.cardX(), layout.cardY(), layout.cardRight(), layout.cardBottom());
        for (int index = 0; index < this.cards.size(); index++) {
            CombatCard card = this.cards.get(index);
            CardPosition position = layout.cardPosition(index, this.cardScroll);
            if (index != this.draggingIndex && position.y() + CARD_H >= layout.cardY()
                    && position.y() <= layout.cardBottom()) {
                this.renderCard(graphics, card, position.x(), position.y(), mouseX, mouseY, false,
                        this.selectedIndexes.contains(index));
            }
        }

        graphics.disableScissor();
        if (this.draggingIndex >= 0 && this.draggingIndex < this.cards.size()) {
            CombatCard card = this.cards.get(this.draggingIndex);
            this.renderCard(graphics, card, mouseX - this.dragOffsetX, mouseY - this.dragOffsetY,
                    mouseX, mouseY, true, this.selectedIndexes.contains(this.draggingIndex));
        }

        int spent = this.selectedCost();
        Component cost = Component.translatable("gui.astral_craft.board.battle_cost", spent, this.maximumCost);
        graphics.text(this.font, cost, layout.actionX(), layout.actionY() - 15,
                spent <= this.maximumCost ? 0xFFFFD36B : 0xFFFF6666, false);
        if ("defender".equals(this.role) && !this.resolved) {
            this.drawButton(graphics, layout.actionX(), layout.actionY() + 8, 80, 26,
                    "gui.astral_craft.board.defend", "defend".equals(this.defenseMode));
            this.drawButton(graphics, layout.actionX() + 86, layout.actionY() + 8, 80, 26,
                    "gui.astral_craft.board.evade", "evade".equals(this.defenseMode));
        }

        if (!"spectator".equals(this.role) && !this.resolved) {
            this.drawButton(graphics, layout.actionX() + 86, layout.actionY() + 44, 80, 28,
                    this.submitted ? "gui.astral_craft.board.waiting" : "gui.astral_craft.board.ready",
                    !this.submitted && spent <= this.maximumCost);
        }

        Component timer = Component.translatable("gui.astral_craft.board.timeout", (this.timeoutTicks + 19) / 20);
        graphics.text(this.font, timer, i - this.font.width(timer) / 2,
                layout.y() + 17, 0xFFFFFFFF, false);
        if (this.resolved && this.resultText != null) {
            int i1 = layout.y() + layout.height() / 2;
            graphics.fill(layout.x() + 80, i1 - 18,
                    layout.x() + layout.width() - 80, layout.y() + layout.height() / 2 + 18, 0xDD000000);
            graphics.text(this.font, this.resultText,
                    i - this.font.width(this.resultText) / 2,
                    i1 - 4, 0xFFFFFF80, true);
        }
    }

    private void renderCard(GuiGraphicsExtractor graphics, CombatCard card, int x, int y,
                            int mouseX, int mouseY, boolean dragging, boolean selected) {
        HandCardRenderHelper.renderFramedCard(graphics, this.font, card.definition().type(),
                card.definition().largeFrontTexture(card.stack()), card.definition().displayName(card.stack()),
                x, y, mouseX, mouseY, dragging);
        if (selected) graphics.fill(x, y, x + CARD_W, y + CARD_H, 0x4472FF72);
        Component cost = Component.translatable("gui.astral_craft.board.card_cost", card.cost());
        graphics.fill(x + 3, y + 3, x + 25, y + 15, 0xD9000000);
        graphics.text(this.font, cost, x + 5, y + 5, 0xFFFFD36B, true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0 || this.resolved || this.submitted || "spectator".equals(this.role)) {
            return super.mouseClicked(event, doubleClick);
        }

        Layout layout = this.layout();
        if ("defender".equals(this.role)) {
            if (inside(event.x(), event.y(), layout.actionX(), layout.actionY() + 8, 80, 26)) {
                this.defenseMode = "defend";
                return true;
            }
            if (inside(event.x(), event.y(), layout.actionX() + 86, layout.actionY() + 8, 80, 26)) {
                this.defenseMode = "evade";
                return true;
            }
        }

        if (inside(event.x(), event.y(), layout.actionX() + 86, layout.actionY() + 44, 80, 28)) {
            if (this.selectedCost() <= this.maximumCost) {
                List<Integer> serverIndexes = this.selectedIndexes.stream()
                        .map(index -> this.cards.get(index).handIndex()).toList();
                this.submitted = true;
                ClientPacketDistributor.sendToServer(new BoardBattleActionPayload(
                        this.boardId, serverIndexes, this.defenseMode));
            }
            return true;
        }

        for (int index = 0; index < this.cards.size(); index++) {
            CardPosition position = layout.cardPosition(index, this.cardScroll);
            if (event.x() >= layout.cardX() && event.x() <= layout.cardRight()
                    && event.y() >= layout.cardY() && event.y() <= layout.cardBottom()
                    && inside(event.x(), event.y(), position.x(), position.y(), CARD_W, CARD_H)) {
                this.draggingIndex = index;
                this.dragOffsetX = Math.clamp((int) event.x() - position.x(), 0, CARD_W);
                this.dragOffsetY = Math.clamp((int) event.y() - position.y(), 0, CARD_H);
                return true;
            }
        }

        return super.mouseClicked(event, doubleClick);
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

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == 0 && this.draggingIndex >= 0) {
            int index = this.draggingIndex;
            this.draggingIndex = -1;
            if (this.selectedIndexes.contains(index)) {
                this.selectedIndexes.remove(index);
            } else if (this.selectedCost() + this.cards.get(index).cost() <= this.maximumCost) {
                this.selectedIndexes.add(index);
            }
            return true;
        }
        return super.mouseReleased(event);
    }

    private int selectedCost() {
        return this.selectedIndexes.stream().mapToInt(index -> this.cards.get(index).cost()).sum();
    }

    private void drawButton(GuiGraphicsExtractor graphics, int x, int y, int width, int height, String key, boolean active) {
        graphics.fill(x, y, x + width, y + height, active ? 0xFFD64B91 : 0xFF555560);
        Component text = Component.translatable(key);
        graphics.text(this.font, text, x + (width - this.font.width(text)) / 2, y + 8, 0xFFFFFFFF, false);
    }

    private LivingEntity entity(int id) {
        if (Minecraft.getInstance().level == null) return null;
        Entity entity = Minecraft.getInstance().level.getEntity(id);
        return entity instanceof LivingEntity living ? living : null;
    }

    private Layout layout() {
        int width = Math.clamp(this.width - 24, 1, 760);
        int height = Math.clamp(this.height - 24, 1, 420);
        int x = (this.width - width) / 2;
        int y = (this.height - height) / 2;
        int actionWidth = Math.clamp(width / 3, 176, 184);
        int actionX = x + width - actionWidth + 4;
        int cardX = x + 18;
        int cardRight = Math.max(cardX + 1, actionX - 8);
        int cardsWidth = Math.max(1, cardRight - cardX);
        int columns = Math.max(1, (cardsWidth + CARD_GAP) / (CARD_W + CARD_GAP));
        int cardY = y + Math.max(48, height / 2);
        int cardBottom = Math.max(cardY + 1, y + height - 10);
        int actionY = Math.min(cardY, Math.max(y + 42, y + height - 82));
        return new Layout(x, y, width, height, cardX, cardY, cardRight, cardBottom,
                columns, actionX, actionY);
    }

    private float maximumCardScroll(Layout layout) {
        int rows = Math.max(1, (this.cards.size() + layout.columns() - 1) / layout.columns());
        int contentHeight = rows * (CARD_H + CARD_GAP) - CARD_GAP;
        return Math.max(0, contentHeight - (layout.cardBottom() - layout.cardY()));
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

    private record Layout(int x, int y, int width, int height, int cardX, int cardY,
                          int cardRight, int cardBottom, int columns, int actionX, int actionY) {
        CardPosition cardPosition(int index, float scroll) {
            return new CardPosition(this.cardX + index % this.columns * (CARD_W + CARD_GAP),
                    this.cardY + index / this.columns * (CARD_H + CARD_GAP) - Math.round(scroll));
        }
    }
}
