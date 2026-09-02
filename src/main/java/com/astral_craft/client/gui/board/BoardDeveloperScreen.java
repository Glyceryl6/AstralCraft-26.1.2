package com.astral_craft.client.gui.board;

import com.astral_craft.client.gameplay.character.ClientCharacterDefinitionCache;
import com.astral_craft.client.gui.components.AstralFancyButton;
import com.astral_craft.client.gui.components.AstralFancyButton.ButtonStyle;
import com.astral_craft.common.gameplay.board.BoardParticipant;
import com.astral_craft.common.gameplay.character.CharacterDefinition;
import com.astral_craft.common.gameplay.character.skin.CharacterSkinDefinition;
import com.astral_craft.common.network.c2s.BoardDeveloperConfigPayload;
import com.astral_craft.common.network.s2c.OpenBoardDeveloperPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jspecify.annotations.NonNull;

import java.util.*;

/** Solo development setup: deterministic bot characters/skins followed by unrestricted opening hands. */
public class BoardDeveloperScreen extends Screen {

    private static final int BOT_COUNT = 3;
    private static final int MARGIN = 14;
    private static final int GAP = 5;
    private static final int ROW_H = 24;
    private final UUID boardId;
    private final List<CharacterDefinition> characters;
    private final List<Identifier> cardIds;
    private final BotDraft[] bots = new BotDraft[BOT_COUNT];
    private Page page = Page.CHARACTERS;
    private int selectedBot;
    private float characterScroll;
    private float cardScroll;
    private boolean submitted;

    public BoardDeveloperScreen(OpenBoardDeveloperPayload payload) {
        super(Component.translatable("gui.astral_craft.board.developer.title"));
        this.boardId = payload.boardId();
        this.characters = payload.characters();
        this.cardIds = payload.cardIds();
        ClientCharacterDefinitionCache.INSTANCE.replace(this.characters);
        this.initializeBots();
    }

    public static void open(OpenBoardDeveloperPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(new BoardDeveloperScreen(payload)));
    }

    @Override
    protected void init() {
        this.characterScroll = Mth.clamp(this.characterScroll, 0.0F, this.maximumCharacterScroll());
        this.cardScroll = Mth.clamp(this.cardScroll, 0.0F, this.maximumCardScroll());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractBackground(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {}

    @Override
    public void extractRenderState(@NonNull GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        Layout layout = this.layout();
        graphics.fill(0, 0, this.width, this.height, 0xE6090912);
        graphics.fill(layout.panelX(), layout.panelY(), layout.panelRight(), layout.panelBottom(), 0xE011111C);
        graphics.text(this.font, this.title, layout.panelX() + 9, layout.panelY() + 7, 0xFFFFFFFF, false);
        graphics.text(this.font, Component.translatable(this.page == Page.CHARACTERS
                        ? "gui.astral_craft.board.developer.characters_hint" : "gui.astral_craft.board.developer.cards_hint"),
                layout.panelX() + 9, layout.panelY() + 21, 0xFFAEB7DC, false);
        this.renderBotTabs(graphics, layout, mouseX, mouseY);
        if (this.page == Page.CHARACTERS) this.renderCharacterPage(graphics, layout, mouseX, mouseY);
        else this.renderCardPage(graphics, layout, mouseX, mouseY);
        this.renderFooter(graphics, layout, mouseX, mouseY);
    }

    private void renderBotTabs(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        int tabWidth = (layout.contentW() - GAP * (BOT_COUNT - 1)) / BOT_COUNT;
        for (int index = 0; index < BOT_COUNT; index++) {
            int x = layout.contentX() + index * (tabWidth + GAP);
            boolean hovered = inside(mouseX, mouseY, x, layout.tabsY(), tabWidth, ROW_H);
            Component label = Component.translatable("gui.astral_craft.board.developer.bot", index + 1);
            AstralFancyButton.renderTab(graphics, this.font, label, x, layout.tabsY(), tabWidth, ROW_H,
                    index == this.selectedBot, hovered, 0xFFD64B91);
        }
    }

    private void renderCharacterPage(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        BotDraft draft = this.bots[this.selectedBot];
        graphics.text(this.font, Component.translatable("gui.astral_craft.board.developer.characters"),
                layout.contentX(), layout.bodyY(), 0xFFFFD1E8, false);
        int listTop = layout.bodyY() + 13;
        int listBottom = layout.skinY() - 18;
        graphics.enableScissor(layout.contentX(), listTop, layout.contentRight(), listBottom);
        for (int index = 0; index < this.characters.size(); index++) {
            CharacterDefinition definition = this.characters.get(index);
            int y = listTop + index * (ROW_H + 2) - Math.round(this.characterScroll);
            if (y + ROW_H < listTop || y > listBottom) continue;
            boolean used = this.characterUsedByOtherBot(definition.id());
            boolean selected = draft != null && definition.id().equals(draft.characterId);
            boolean hovered = !used && inside(mouseX, mouseY, layout.contentX(), y, layout.contentW(), ROW_H);
            Component name = Component.translatable(definition.getDescriptionId());
            if (used && !selected) name = Component.translatable("gui.astral_craft.board.developer.character_used", name);
            AstralFancyButton.renderButton(graphics, this.font, name, layout.contentX(), y,
                    layout.contentW(), ROW_H, selected, hovered,
                    ButtonStyle.button(used && !selected ? 0xFF555562 : 0xFF7659C9));
        }
        graphics.disableScissor();

        graphics.text(this.font, Component.translatable("gui.astral_craft.board.developer.skins"),
                layout.contentX(), layout.skinTitleY(), 0xFFFFD1E8, false);
        if (draft == null) return;
        CharacterDefinition definition = this.definition(draft.characterId);
        if (definition == null) return;
        int skinColumns = Math.min(3, Math.max(1, definition.skins().size()));
        int skinWidth = (layout.contentW() - GAP * (skinColumns - 1)) / skinColumns;
        for (int index = 0; index < definition.skins().size(); index++) {
            CharacterSkinDefinition skin = definition.skins().get(index);
            int x = layout.contentX() + index % skinColumns * (skinWidth + GAP);
            int y = layout.skinY() + index / skinColumns * (ROW_H + GAP);
            boolean selected = draft.skinId.getPath().equals(skin.id());
            boolean hovered = inside(mouseX, mouseY, x, y, skinWidth, ROW_H);
            AstralFancyButton.renderButton(graphics, this.font, Component.translatable(skin.nameKey()), x,
                    y, skinWidth, ROW_H, selected, hovered, 0xFF4C8EC9);
        }
    }

    private void renderCardPage(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        BotDraft draft = this.bots[this.selectedBot];
        int total = draft == null ? 0 : draft.totalCards();
        graphics.text(this.font, Component.translatable("gui.astral_craft.board.developer.card_total", total),
                layout.contentX(), layout.bodyY(), 0xFFFFD1E8, false);
        int listTop = layout.bodyY() + 14;
        int listBottom = layout.footerY() - 7;
        graphics.enableScissor(layout.contentX(), listTop, layout.contentRight(), listBottom);
        for (int index = 0; index < this.cardIds.size(); index++) {
            Identifier cardId = this.cardIds.get(index);
            int y = listTop + index * (ROW_H + 2) - Math.round(this.cardScroll);
            if (y + ROW_H < listTop || y > listBottom) continue;
            int count = draft == null ? 0 : draft.cards.getOrDefault(cardId, 0);
            boolean hovered = inside(mouseX, mouseY, layout.contentX(), y, layout.contentW(), ROW_H);
            Component label = Component.translatable("gui.astral_craft.board.developer.card_entry",
                    this.cardName(cardId), count);
            AstralFancyButton.renderButton(graphics, this.font, label, layout.contentX(), y,
                    layout.contentW(), ROW_H, count > 0, hovered, 0xFF4C8EC9);
        }
        graphics.disableScissor();
    }

    private void renderFooter(GuiGraphicsExtractor graphics, Layout layout, int mouseX, int mouseY) {
        int buttonW = Math.max(86, (layout.contentW() - GAP) / 2);
        int leftX = layout.contentX();
        int rightX = layout.contentRight() - buttonW;
        if (this.page == Page.CARDS) {
            boolean hoverBack = inside(mouseX, mouseY, leftX, layout.footerY(), buttonW, ROW_H);
            AstralFancyButton.renderButton(graphics, this.font,
                    Component.translatable("gui.astral_craft.board.developer.back"), leftX, layout.footerY(),
                    buttonW, ROW_H, false, hoverBack, 0xFF646477);
        }
        boolean hoverNext = inside(mouseX, mouseY, rightX, layout.footerY(), buttonW, ROW_H);
        Component next = Component.translatable(this.page == Page.CHARACTERS
                ? "gui.astral_craft.board.developer.configure_cards" : "gui.astral_craft.board.developer.start");
        AstralFancyButton.renderButton(graphics, this.font, next, rightX, layout.footerY(), buttonW, ROW_H,
                false, hoverNext, 0xFFD64B91);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        Layout layout = this.layout();
        int tabWidth = (layout.contentW() - GAP * (BOT_COUNT - 1)) / BOT_COUNT;
        if (event.button() == 0) {
            for (int index = 0; index < BOT_COUNT; index++) {
                int x = layout.contentX() + index * (tabWidth + GAP);
                if (inside(event.x(), event.y(), x, layout.tabsY(), tabWidth, ROW_H)) {
                    this.selectedBot = index;
                    this.characterScroll = 0.0F;
                    this.cardScroll = 0.0F;
                    return true;
                }
            }
        }

        if (this.page == Page.CHARACTERS && event.button() == 0) {
            int listTop = layout.bodyY() + 13;
            int listBottom = layout.skinY() - 18;
            for (int index = 0; index < this.characters.size(); index++) {
                int y = listTop + index * (ROW_H + 2) - Math.round(this.characterScroll);
                if (event.y() < listTop || event.y() > listBottom
                        || !inside(event.x(), event.y(), layout.contentX(), y, layout.contentW(), ROW_H)) continue;
                CharacterDefinition definition = this.characters.get(index);
                if (this.characterUsedByOtherBot(definition.id())) return true;
                this.bots[this.selectedBot] = new BotDraft(definition.id(), this.firstSkin(definition), new LinkedHashMap<>());
                return true;
            }

            BotDraft draft = this.bots[this.selectedBot];
            CharacterDefinition definition = draft == null ? null : this.definition(draft.characterId);
            if (definition != null) {
                int skinColumns = Math.min(3, Math.max(1, definition.skins().size()));
                int skinWidth = (layout.contentW() - GAP * (skinColumns - 1)) / skinColumns;
                for (int index = 0; index < definition.skins().size(); index++) {
                    int x = layout.contentX() + index % skinColumns * (skinWidth + GAP);
                    int y = layout.skinY() + index / skinColumns * (ROW_H + GAP);
                    if (inside(event.x(), event.y(), x, y, skinWidth, ROW_H)) {
                        CharacterSkinDefinition skin = definition.skins().get(index);
                        draft.skinId = BoardParticipant.skinIdentifier(definition.id(), skin.id());
                        return true;
                    }
                }
            }
        }

        if (this.page == Page.CARDS && (event.button() == 0 || event.button() == 1)) {
            int listTop = layout.bodyY() + 14;
            int listBottom = layout.footerY() - 7;
            for (int index = 0; index < this.cardIds.size(); index++) {
                int y = listTop + index * (ROW_H + 2) - Math.round(this.cardScroll);
                if (event.y() < listTop || event.y() > listBottom
                        || !inside(event.x(), event.y(), layout.contentX(), y, layout.contentW(), ROW_H)) continue;
                BotDraft draft = this.bots[this.selectedBot];
                Identifier cardId = this.cardIds.get(index);
                int count = draft.cards.getOrDefault(cardId, 0);
                if (event.button() == 0) draft.cards.put(cardId, count + 1);
                else if (count <= 1) draft.cards.remove(cardId);
                else draft.cards.put(cardId, count - 1);
                return true;
            }
        }

        int buttonW = Math.max(86, (layout.contentW() - GAP) / 2);
        int leftX = layout.contentX();
        int rightX = layout.contentRight() - buttonW;
        if (event.button() == 0 && this.page == Page.CARDS
                && inside(event.x(), event.y(), leftX, layout.footerY(), buttonW, ROW_H)) {
            this.page = Page.CHARACTERS;
            return true;
        }
        if (event.button() == 0 && inside(event.x(), event.y(), rightX, layout.footerY(), buttonW, ROW_H)) {
            if (this.page == Page.CHARACTERS) {
                this.page = Page.CARDS;
                this.cardScroll = 0.0F;
            } else {
                this.submit();
            }
            return true;
        }

        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        float delta = (float) (deltaY + deltaX) * 32.0F;
        if (this.page == Page.CHARACTERS) {
            this.characterScroll = Mth.clamp(this.characterScroll - delta, 0.0F, this.maximumCharacterScroll());
        } else {
            this.cardScroll = Mth.clamp(this.cardScroll - delta, 0.0F, this.maximumCardScroll());
        }
        return true;
    }

    @Override
    public void onClose() {
        if (!this.submitted) ClientPacketDistributor.sendToServer(new BoardDeveloperConfigPayload(this.boardId, List.of()));
        super.onClose();
    }

    private void submit() {
        List<BoardDeveloperConfigPayload.BotSetup> setups = new ArrayList<>();
        for (BotDraft draft : this.bots) {
            if (draft == null) return;
            List<BoardDeveloperConfigPayload.CardCount> cards = draft.cards.entrySet().stream()
                    .filter(entry -> entry.getValue() > 0)
                    .map(entry -> new BoardDeveloperConfigPayload.CardCount(entry.getKey(), entry.getValue())).toList();
            setups.add(new BoardDeveloperConfigPayload.BotSetup(draft.characterId, draft.skinId, cards));
        }
        this.submitted = true;
        ClientPacketDistributor.sendToServer(new BoardDeveloperConfigPayload(this.boardId, setups));
        super.onClose();
    }

    private void initializeBots() {
        for (int index = 0; index < BOT_COUNT; index++) {
            int finalIndex = index;
            CharacterDefinition definition = this.characters.stream()
                    .filter(value -> !this.characterUsed(value.id(), finalIndex)).findFirst().orElse(null);
            if (definition != null) this.bots[index] = new BotDraft(definition.id(), this.firstSkin(definition), new LinkedHashMap<>());
        }
    }

    private boolean characterUsed(Identifier characterId, int except) {
        for (int index = 0; index < this.bots.length; index++) {
            if (index == except || this.bots[index] == null) continue;
            if (this.bots[index].characterId.equals(characterId)) return true;
        }
        return false;
    }

    private boolean characterUsedByOtherBot(Identifier characterId) {
        return this.characterUsed(characterId, this.selectedBot);
    }

    private CharacterDefinition definition(Identifier characterId) {
        return this.characters.stream().filter(value -> value.id().equals(characterId)).findFirst().orElse(null);
    }

    private Identifier firstSkin(CharacterDefinition definition) {
        String skin = definition.skins().isEmpty() ? "default" : definition.skins().getFirst().id();
        return BoardParticipant.skinIdentifier(definition.id(), skin);
    }

    private Component cardName(Identifier cardId) {
        Item item = BuiltInRegistries.ITEM.getValue(cardId);
        return item == null ? Component.literal(cardId.toString()) : new ItemStack(item).getHoverName();
    }

    private float maximumCharacterScroll() {
        Layout layout = this.layout();
        int listTop = layout.bodyY() + 13;
        int listBottom = layout.skinY() - 18;
        return Math.max(0, this.characters.size() * (ROW_H + 2) - 2 - (listBottom - listTop));
    }

    private float maximumCardScroll() {
        Layout layout = this.layout();
        int listTop = layout.bodyY() + 14;
        int listBottom = layout.footerY() - 7;
        return Math.max(0, this.cardIds.size() * (ROW_H + 2) - 2 - (listBottom - listTop));
    }

    private Layout layout() {
        int panelX = MARGIN;
        int panelY = MARGIN;
        int panelRight = Math.max(panelX + 180, this.width - MARGIN);
        int panelBottom = Math.max(panelY + 150, this.height - MARGIN);
        int contentX = panelX + 10;
        int contentRight = panelRight - 10;
        int tabsY = panelY + 38;
        int bodyY = tabsY + ROW_H + 9;
        int footerY = panelBottom - ROW_H - 9;
        int skinY = footerY - ROW_H * 2 - GAP - 8;
        int skinTitleY = skinY - 13;
        return new Layout(panelX, panelY, panelRight, panelBottom, contentX, contentRight,
                tabsY, bodyY, skinTitleY, skinY, footerY);
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private enum Page { CHARACTERS, CARDS }

    private static class BotDraft {
        private Identifier characterId;
        private Identifier skinId;
        private final Map<Identifier, Integer> cards;

        private BotDraft(Identifier characterId, Identifier skinId, Map<Identifier, Integer> cards) {
            this.characterId = characterId;
            this.skinId = skinId;
            this.cards = new LinkedHashMap<>(cards);
        }

        private int totalCards() {
            return this.cards.values().stream().mapToInt(Integer::intValue).sum();
        }
    }

    private record Layout(int panelX, int panelY, int panelRight, int panelBottom, int contentX,
                          int contentRight, int tabsY, int bodyY, int skinTitleY, int skinY, int footerY) {
        int contentW() {
            return this.contentRight - this.contentX;
        }
    }

}