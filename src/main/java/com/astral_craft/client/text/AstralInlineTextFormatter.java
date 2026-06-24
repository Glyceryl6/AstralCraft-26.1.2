package com.astral_craft.client.text;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AstralInlineTextFormatter {

    public static final int LINE_HEIGHT = 11;
    public static final int PARAGRAPH_GAP = 7;

    public static int draw(GuiGraphicsExtractor graphics, Font font, Component component, int x, int y, int maxWidth, int defaultColor, boolean shadow) {
        List<Line> lines = wrap(font, component == null ? "" : component.getString(), maxWidth, defaultColor);
        for (Line line : lines) {
            if (line.empty()) {
                y += PARAGRAPH_GAP;
                continue;
            }

            int cursorX = x;
            for (Run run : line.runs()) {
                graphics.text(font, Component.literal(run.text()), cursorX, y, run.color(), shadow);
                cursorX += font.width(run.text());
            }

            y += LINE_HEIGHT;
        }

        return y + 4;
    }

    public static int height(Font font, Component component, int maxWidth, int defaultColor) {
        int height = 0;
        for (Line line : wrap(font, component == null ? "" : component.getString(), maxWidth, defaultColor)) {
            height += line.empty() ? PARAGRAPH_GAP : LINE_HEIGHT;
        }

        return height + 4;
    }

    public static List<Line> wrap(Font font, String input, int maxWidth, int defaultColor) {
        String text = input == null ? "" : input.replace("\\n", "\n");
        List<StyledChar> chars = parse(text, defaultColor);
        List<Line> lines = new ArrayList<>();
        List<Run> currentRuns = new ArrayList<>();
        StringBuilder currentText = new StringBuilder();
        int currentColor = defaultColor;
        int currentWidth = 0;
        boolean hasCurrentColor = false;
        for (StyledChar c : chars) {
            if (c.character() == '\n') {
                flushRun(currentRuns, currentText, currentColor);
                lines.add(Line.of(currentRuns));
                currentRuns = new ArrayList<>();
                currentText = new StringBuilder();
                currentWidth = 0;
                hasCurrentColor = false;
                continue;
            }

            String literal = String.valueOf(c.character());
            int charWidth = font.width(literal);
            if (currentWidth > 0 && currentWidth + charWidth > maxWidth) {
                flushRun(currentRuns, currentText, currentColor);
                lines.add(Line.of(currentRuns));
                currentRuns = new ArrayList<>();
                currentText = new StringBuilder();
                currentWidth = 0;
                hasCurrentColor = false;
            }

            if (!hasCurrentColor || currentColor != c.color()) {
                flushRun(currentRuns, currentText, currentColor);
                currentColor = c.color();
                hasCurrentColor = true;
            }

            currentText.append(c.character());
            currentWidth += charWidth;
        }

        flushRun(currentRuns, currentText, currentColor);
        if (!currentRuns.isEmpty() || lines.isEmpty()) {
            lines.add(Line.of(currentRuns));
        }

        while (!lines.isEmpty() && lines.getLast().empty()) {
            lines.removeLast();
        }

        return lines;
    }

    private static List<StyledChar> parse(String text, int defaultColor) {
        List<StyledChar> out = new ArrayList<>();
        int i = 0;
        while (i < text.length()) {
            char ch = text.charAt(i);
            if (ch == '[') {
                int colon = text.indexOf(':', i + 1);
                if (colon > i + 1) {
                    int close = findClosingBracket(text, colon + 1);
                    if (close > colon) {
                        String style = text.substring(i + 1, colon).trim();
                        int color = colorFor(style, defaultColor);
                        String inner = text.substring(colon + 1, close);
                        for (int j = 0; j < inner.length(); j++) {
                            out.add(new StyledChar(inner.charAt(j), color));
                        }
                        i = close + 1;
                        continue;
                    }
                }
            }

            out.add(new StyledChar(ch, defaultColor));
            i++;
        }

        return out;
    }

    private static int findClosingBracket(String text, int start) {
        int depth = 0;
        for (int i = start; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch == '[') depth++;
            if (ch == ']') {
                if (depth == 0) return i;
                depth--;
            }
        }

        return -1;
    }

    private static int colorFor(String style, int fallback) {
        return switch (style.toLowerCase(Locale.ROOT)) {
            case "white" -> 0xFFFFFFFF;
            case "gray", "grey" -> 0xFFB8B8B8;
            case "dark_gray", "dark_grey" -> 0xFF777777;
            case "red", "bad", "damage" -> 0xFFFF6868;
            case "green", "good", "heal" -> 0xFF8CFF80;
            case "blue" -> 0xFF80B8FF;
            case "aqua", "cyan" -> 0xFF71EFFF;
            case "gold", "yellow", "reward" -> 0xFFFFD66B;
            case "purple", "violet", "pvp" -> 0xFFD889FF;
            case "pink", "keyword", "pve" -> 0xFFFF7AC8;
            case "warn", "warning", "orange" -> 0xFFFFA64D;
            default -> fallback;
        };
    }

    private static void flushRun(List<Run> runs, StringBuilder currentText, int currentColor) {
        if (currentText.isEmpty()) return;
        runs.add(new Run(currentText.toString(), currentColor));
        currentText.setLength(0);
    }

    public record Line(List<Run> runs) {

        static Line of(List<Run> runs) {
            return new Line(List.copyOf(runs));
        }

        public boolean empty() {
            return this.runs.isEmpty();
        }

    }

    public record Run(String text, int color) {}

    private record StyledChar(char character, int color) {}

}