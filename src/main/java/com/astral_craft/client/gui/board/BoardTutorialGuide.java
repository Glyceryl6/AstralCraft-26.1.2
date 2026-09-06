package com.astral_craft.client.gui.board;

import com.astral_craft.AstralCraft;
import com.astral_craft.client.gui.components.AstralFancyButton;
import com.astral_craft.common.network.c2s.BoardTutorialHintDismissPayload;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Locale;
import java.util.List;
import java.util.UUID;

/** Client-side, per-match tutorial presentation state. Server rules remain authoritative. */
public class BoardTutorialGuide {

    private static final int LINE_HEIGHT = 11;
    private static final int PADDING = 7;
    private static final int CHECKBOX_SIZE = 9;
    private static UUID boardId;
    private static final EnumSet<Hint> dismissed = EnumSet.noneOf(Hint.class);
    private static final List<CheckboxHitbox> hitboxes = new ArrayList<>();

    public static void start(UUID id) {
        boardId = id;
        dismissed.clear();
        hitboxes.clear();
    }

    public static void clear(UUID id) {
        if (id != null && !id.equals(boardId)) return;
        boardId = null;
        dismissed.clear();
        hitboxes.clear();
    }

    public static boolean active(UUID id) {
        return id != null && id.equals(boardId);
    }

    public static boolean visible(UUID id, Hint hint) {
        return active(id) && !dismissed.contains(hint);
    }

    public static void beginFrame(UUID id) {
        if (active(id)) hitboxes.clear();
    }

    public static boolean mouseClicked(UUID id, double mouseX, double mouseY) {
        if (!active(id)) return false;
        for (CheckboxHitbox hitbox : List.copyOf(hitboxes)) {
            if (mouseX >= hitbox.x() && mouseX <= hitbox.x() + hitbox.width()
                    && mouseY >= hitbox.y() && mouseY <= hitbox.y() + hitbox.height()) {
                dismissed.add(hitbox.hint());
                ClientPacketDistributor.sendToServer(new BoardTutorialHintDismissPayload(id, hitbox.hint().id()));
                return true;
            }
        }
        return false;
    }

    public static int renderBox(GuiGraphicsExtractor graphics, Font font, UUID id, Hint hint,
                                int x, int bottomY, int maxWidth) {
        if (!visible(id, hint)) return 0;
        Component message = Component.translatable(hint.translationKey());
        int width = Math.clamp(maxWidth, 150, 430);
        List<FormattedCharSequence> lines = splitMessage(font, message, width - PADDING * 2);
        Component dismiss = Component.translatable("gui.astral_craft.board.tutorial.dismiss_for_match");
        int checkboxLineWidth = CHECKBOX_SIZE + 5 + font.width(dismiss);
        width = Math.max(Math.min(maxWidth, 430), Math.min(maxWidth, checkboxLineWidth + PADDING * 2));
        lines = splitMessage(font, message, Math.max(1, width - PADDING * 2));
        int textHeight = lines.size() * LINE_HEIGHT;
        int height = PADDING + textHeight + 6 + CHECKBOX_SIZE + PADDING;
        int y = bottomY - height;
        AstralFancyButton.renderOutlinedBox(graphics, x, y, width, height,
                0xE312131C, 0xFFE7D7FF, 0xFF4A3E59, 1, 1);
        int lineY = y + PADDING;
        for (FormattedCharSequence line : lines) {
            graphics.text(font, line, x + PADDING, lineY, 0xFFFFFFFF, false);
            lineY += LINE_HEIGHT;
        }
        int checkboxX = x + PADDING;
        int checkboxY = y + height - PADDING - CHECKBOX_SIZE;
        graphics.fill(checkboxX, checkboxY, checkboxX + CHECKBOX_SIZE, checkboxY + CHECKBOX_SIZE, 0xFFE8E8F2);
        graphics.fill(checkboxX + 1, checkboxY + 1, checkboxX + CHECKBOX_SIZE - 1,
                checkboxY + CHECKBOX_SIZE - 1, 0xFF161620);
        graphics.text(font, dismiss, checkboxX + CHECKBOX_SIZE + 5, checkboxY, 0xFFD9D3E3, false);
        hitboxes.add(new CheckboxHitbox(hint, checkboxX - 2, checkboxY - 2,
                Math.min(width - PADDING + 2, checkboxLineWidth + 4), CHECKBOX_SIZE + 4));
        return height;
    }

    private static List<FormattedCharSequence> splitMessage(Font font, Component message, int maxWidth) {
        List<FormattedCharSequence> result = new ArrayList<>();
        String[] paragraphs = message.getString().split("\n", -1);
        for (String paragraph : paragraphs) {
            List<FormattedCharSequence> wrapped = font.split(Component.literal(paragraph.isEmpty() ? " " : paragraph),
                    Math.max(1, maxWidth));
            result.addAll(wrapped);
        }
        return result;
    }

    public enum Hint {
        DECISION_TIME("gui.astral_craft.board.tutorial.decision_time"),
        PROTECTION("gui.astral_craft.board.tutorial.protection"),
        HAND_DRAG("gui.astral_craft.board.tutorial.hand_drag"),
        BATTLE_START("gui.astral_craft.board.tutorial.battle_start"),
        BATTLE_VALUES("gui.astral_craft.board.tutorial.battle_values"),
        ATTACK_ROLL("gui.astral_craft.board.tutorial.attack_roll"),
        DEFENSE_CHOICE("gui.astral_craft.board.tutorial.defense_choice"),
        DEFENSE_RULES("gui.astral_craft.board.tutorial.defense_rules"),
        TARGET_PLATFORM("gui.astral_craft.board.tutorial.target_platform"),
        TARGET_CHARACTER("gui.astral_craft.board.tutorial.target_character"),
        TARGET_SELF("gui.astral_craft.board.tutorial.target_self"),
        COUNTER("gui.astral_craft.board.tutorial.counter"),
        KNOCKED_DOWN("gui.astral_craft.board.tutorial.knocked_down"),
        KNOCKOUT_OTHER("gui.astral_craft.board.tutorial.knockout_other"),
        BRANCH("gui.astral_craft.board.tutorial.branch");

        private final String translationKey;

        Hint(String translationKey) {
            this.translationKey = translationKey;
        }

        public Identifier id() {
            return AstralCraft.prefix(this.name().toLowerCase(Locale.ROOT));
        }

        public String translationKey() {
            return this.translationKey;
        }
    }

    private record CheckboxHitbox(Hint hint, int x, int y, int width, int height) {}
}
