package com.astral_craft.common.text;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.*;

/**
 * Converts resolved localized text into structured components without leaving
 * formatting state in subsequent text. Unknown or malformed tags are rendered
 * literally instead of throwing.
 */
public class AstralTextFormatter {

    private static final char LEGACY_PREFIX = '§';
    private static final int MAX_TAG_LENGTH = 96;
    private static final Map<String, Integer> NAMED_COLORS = createNamedColors();

    public static Component format(Component component) {
        List<Component> lines = lines(component);
        MutableComponent result = Component.empty();
        for (int index = 0; index < lines.size(); index++) {
            if (index > 0) {
                result.append(Component.literal("\n"));
            }
            result.append(lines.get(index));
        }

        return result;
    }

    public static List<Component> lines(Component component) {
        return lines(component == null ? "" : component.getString());
    }

    public static List<Component> lines(String resolvedText) {
        return new Parser(resolvedText == null ? "" : resolvedText).parse();
    }

    private static Map<String, Integer> createNamedColors() {
        Map<String, Integer> colors = new LinkedHashMap<>();
        Arrays.stream(ChatFormatting.values()).filter(ChatFormatting::isColor)
                .forEach(c -> colors.put(c.name().toLowerCase(), c.getColor()));
        return Map.copyOf(colors);
    }

    private static String canonicalTag(String tag) {
        String normalized = tag.trim().toLowerCase(Locale.ROOT).replace('-', '_');
        return switch (normalized) {
            case "b" -> "bold";
            case "i" -> "italic";
            case "u", "underline" -> "underlined";
            case "s", "strike" -> "strikethrough";
            case "obf" -> "obfuscated";
            default -> normalized;
        };
    }

    private static Integer parseColor(String value) {
        String normalized = stripQuotes(value.trim()).toLowerCase(Locale.ROOT).replace('-', '_');
        Integer named = NAMED_COLORS.get(normalized);
        if (named != null) return named;
        if (normalized.startsWith("#")) normalized = normalized.substring(1);
        if (normalized.startsWith("0x")) normalized = normalized.substring(2);
        if (normalized.length() != 6) return null;
        try {
            return Integer.parseUnsignedInt(normalized, 16) & 0xFFFFFF;
        } catch (NumberFormatException _) {
            return null;
        }
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2) {
            char first = value.charAt(0);
            char last = value.charAt(value.length() - 1);
            if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
                return value.substring(1, value.length() - 1);
            }
        }

        return value;
    }

    private static class Parser {

        private final String text;
        private final List<Component> lines = new ArrayList<>();
        private final Deque<FormatFrame> frames = new ArrayDeque<>();
        private final StringBuilder buffer = new StringBuilder();
        private MutableComponent line = Component.empty();
        private FormatState state = FormatState.DEFAULT;

        private Parser(String text) {
            this.text = text;
        }

        private List<Component> parse() {
            int index = 0;
            while (index < this.text.length()) {
                char current = this.text.charAt(index);
                if (current == '\\' && index + 1 < this.text.length() && isEscapable(this.text.charAt(index + 1))) {
                    this.buffer.append(this.text.charAt(index + 1));
                    index += 2;
                    continue;
                }

                if (current == '\n' || current == '|') {
                    this.flush();
                    this.lines.add(this.line);
                    this.line = Component.empty();
                    index++;
                    continue;
                }

                if (current == LEGACY_PREFIX && index + 1 < this.text.length()) {
                    int nextIndex = this.applyLegacy(index);
                    if (nextIndex > index) {
                        index = nextIndex;
                        continue;
                    }
                }

                if (current == '<') {
                    int closing = this.text.indexOf('>', index + 1);
                    if (closing > index && closing - index <= MAX_TAG_LENGTH) {
                        String tag = this.text.substring(index + 1, closing);
                        this.flush();
                        if (this.applyTag(tag)) {
                            index = closing + 1;
                            continue;
                        }
                    }
                }

                this.buffer.append(current);
                index++;
            }

            this.flush();
            this.lines.add(this.line);
            return List.copyOf(this.lines);
        }

        private int applyLegacy(int index) {
            char code = Character.toLowerCase(this.text.charAt(index + 1));
            if (code == 'x') {
                Integer rgb = this.readLegacyHex(index);
                if (rgb == null) return index;
                this.flush();
                this.frames.clear();
                this.state = FormatState.DEFAULT.withColor(rgb);
                return index + 14;
            }

            ChatFormatting formatting = ChatFormatting.getByCode(code);
            if (formatting != null) {
                Integer color = formatting.getColor();
                if (color != null) {
                    this.flush();
                    this.frames.clear();
                    this.state = FormatState.DEFAULT.withColor(color);
                    return index + 2;
                }
            }

            FormatState updated = switch (code) {
                case 'k' -> this.state.withObfuscated(true);
                case 'l' -> this.state.withBold(true);
                case 'm' -> this.state.withStrikethrough(true);
                case 'n' -> this.state.withUnderlined(true);
                case 'o' -> this.state.withItalic(true);
                case 'r' -> FormatState.DEFAULT;
                default -> null;
            };
            if (updated == null) return index;
            this.flush();
            if (code == 'r') {
                this.frames.clear();
            }
            this.state = updated;
            return index + 2;
        }

        private Integer readLegacyHex(int index) {
            if (index + 13 >= this.text.length()) return null;
            int rgb = 0;
            for (int nibble = 0; nibble < 6; nibble++) {
                int prefixIndex = index + 2 + nibble * 2;
                if (this.text.charAt(prefixIndex) != LEGACY_PREFIX) return null;
                int digit = Character.digit(this.text.charAt(prefixIndex + 1), 16);
                if (digit < 0) return null;
                rgb = rgb << 4 | digit;
            }

            return rgb;
        }

        private boolean applyTag(String rawTag) {
            String tag = rawTag.trim();
            if (tag.isEmpty()) return false;
            boolean closing = tag.startsWith("/");
            if (closing) {
                String name = canonicalTag(tag.substring(1));
                if (this.frames.isEmpty() || !this.frames.peek().tag().equals(name)) {
                    this.frames.clear();
                    this.state = FormatState.DEFAULT;
                    return false;
                }
                this.state = this.frames.pop().previous();
                return true;
            }

            boolean selfClosing = tag.endsWith("/");
            if (selfClosing) {
                tag = tag.substring(0, tag.length() - 1).trim();
            }

            int equals = tag.indexOf('=');
            String name = canonicalTag(equals < 0 ? tag : tag.substring(0, equals));
            String value = equals < 0 ? "" : tag.substring(equals + 1).trim();
            if (name.equals("reset")) {
                this.frames.clear();
                this.state = FormatState.DEFAULT;
                return true;
            }

            if (name.equals("color")) {
                Integer color = parseColor(value);
                if (color == null) return false;
                return selfClosing || this.push("color", this.state.withColor(color));
            }

            Integer namedColor = NAMED_COLORS.get(name);
            if (namedColor != null && value.isEmpty()) {
                return selfClosing || this.push(name, this.state.withColor(namedColor));
            }

            if (!value.isEmpty()) return false;
            return switch (name) {
                case "bold" -> selfClosing || this.push(name, this.state.withBold(true));
                case "italic" -> selfClosing || this.push(name, this.state.withItalic(true));
                case "underlined" -> selfClosing || this.push(name, this.state.withUnderlined(true));
                case "strikethrough" -> selfClosing || this.push(name, this.state.withStrikethrough(true));
                case "obfuscated" -> selfClosing || this.push(name, this.state.withObfuscated(true));
                default -> false;
            };
        }

        private boolean push(String tag, FormatState next) {
            this.frames.push(new FormatFrame(tag, this.state));
            this.state = next;
            return true;
        }

        private void flush() {
            if (this.buffer.isEmpty()) return;
            MutableComponent segment = Component.literal(this.buffer.toString());
            if (this.state.color() != null) segment.withColor(this.state.color());
            if (this.state.bold()) segment.withStyle(ChatFormatting.BOLD);
            if (this.state.italic()) segment.withStyle(ChatFormatting.ITALIC);
            if (this.state.underlined()) segment.withStyle(ChatFormatting.UNDERLINE);
            if (this.state.strikethrough()) segment.withStyle(ChatFormatting.STRIKETHROUGH);
            if (this.state.obfuscated()) segment.withStyle(ChatFormatting.OBFUSCATED);
            this.line.append(segment);
            this.buffer.setLength(0);
        }

        private static boolean isEscapable(char character) {
            return character == '\\' || character == '<' || character == '>'
                    || character == '|' || character == LEGACY_PREFIX;
        }

    }

    private record FormatFrame(String tag, FormatState previous) {}

    private record FormatState(
            Integer color, boolean bold, boolean italic, boolean underlined,
            boolean strikethrough, boolean obfuscated) {

        private static final FormatState DEFAULT = new FormatState(null, false, false, false, false, false);

        private FormatState withColor(Integer color) {
            return new FormatState(color, this.bold, this.italic, this.underlined, this.strikethrough, this.obfuscated);
        }

        private FormatState withBold(boolean bold) {
            return new FormatState(this.color, bold, this.italic, this.underlined, this.strikethrough, this.obfuscated);
        }

        private FormatState withItalic(boolean italic) {
            return new FormatState(this.color, this.bold, italic, this.underlined, this.strikethrough, this.obfuscated);
        }

        private FormatState withUnderlined(boolean underlined) {
            return new FormatState(this.color, this.bold, this.italic, underlined, this.strikethrough, this.obfuscated);
        }

        private FormatState withStrikethrough(boolean strikethrough) {
            return new FormatState(this.color, this.bold, this.italic, this.underlined, strikethrough, this.obfuscated);
        }

        private FormatState withObfuscated(boolean obfuscated) {
            return new FormatState(this.color, this.bold, this.italic, this.underlined, this.strikethrough, obfuscated);
        }

    }

}