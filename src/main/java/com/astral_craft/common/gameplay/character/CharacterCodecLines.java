package com.astral_craft.common.gameplay.character;

import com.astral_craft.AstralCraft;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public class CharacterCodecLines {

    public static String encode(List<CharacterDefinition> definitions) {
        StringBuilder builder = new StringBuilder();
        for (CharacterDefinition definition : definitions) {
            if (!builder.isEmpty()) builder.append('\n');
            builder.append(escape(definition.id().toString())).append('|')
                    .append(escape(definition.nameKey())).append('|')
                    .append(escape(definition.titleKey())).append('|')
                    .append(escape(definition.modelKey().toString())).append('|')
                    .append(escape(definition.previewTexture().toString())).append('|')
                    .append(escape(definition.entityTypeKey().toString())).append('|')
                    .append(escape(definition.rendererKey().toString())).append('|')
                    .append(escape(definition.animationSetKey().toString())).append('|')
                    .append(escape(definition.previewAction())).append('|')
                    .append(definition.maxPveLevel()).append('|')
                    .append(definition.maxFriendshipLevel()).append('|')
                    .append(definition.baseStats().attack()).append(',')
                    .append(definition.baseStats().defense()).append(',')
                    .append(definition.baseStats().health()).append('|')
                    .append(escapeSkills(definition.skills())).append('|')
                    .append(escapeProfiles(definition.profileSections())).append('|')
                    .append(escapeSkins(definition.skins())).append('|')
                    .append(definition.implicitDefaultSkin()).append(',')
                    .append(definition.implicitBondSkin()).append('|')
                    .append(definition.unlockedByDefault()).append('|')
                    .append(escape(definition.unlockHintKey())).append('|')
                    .append(definition.sortOrder());
        }

        return builder.toString();
    }

    public static List<CharacterDefinition> decode(String encoded) {
        List<CharacterDefinition> result = new ArrayList<>();
        if (encoded == null || encoded.isBlank()) {
            result.add(CharacterDefinition.builtinDefault());
            return result;
        }

        for (String line : encoded.split("\\n")) {
            String[] parts = line.split("\\|", -1);
            if (parts.length < 9) continue;
            try {
                Identifier id = Identifier.parse(unescape(parts[0]));
                String nameKey = unescape(parts[1]);
                String titleKey = unescape(parts[2]);
                Identifier model = Identifier.parse(unescape(parts[3]));
                Identifier texture = Identifier.parse(unescape(parts[4]));
                Identifier entityType = AstralCraft.prefix("astral_character");
                Identifier renderer = AstralCraft.prefix("player");
                Identifier animationSet = AstralCraft.prefix("humanoid");
                String previewAction = "idle";
                int maxPveLevel = 6;
                int maxFriendshipLevel = 5;
                int statsIndex = 5;
                if (parts.length > 16 && !parts[5].contains(",") && !parts[7].matches("-?\\d+")) {
                    entityType = Identifier.parse(unescape(parts[5]));
                    renderer = Identifier.parse(unescape(parts[6]));
                    animationSet = Identifier.parse(unescape(parts[7]));
                    previewAction = unescape(parts[8]);
                    maxPveLevel = decodeInt(parts, 9, 6);
                    maxFriendshipLevel = decodeInt(parts, 10, 5);
                    statsIndex = 11;
                } else if (parts.length > 12 && !parts[5].contains(",")) {
                    entityType = Identifier.parse(unescape(parts[5]));
                    renderer = Identifier.parse(unescape(parts[6]));
                    maxPveLevel = decodeInt(parts, 7, 6);
                    maxFriendshipLevel = decodeInt(parts, 8, 5);
                    statsIndex = 9;
                }

                CharacterStatsDefinition stats = decodeStats(parts[statsIndex]);
                List<CharacterSkillDefinition> skills = decodeSkills(parts[statsIndex + 1]);
                List<CharacterProfileSection> profiles = decodeProfiles(parts[statsIndex + 2]);
                List<CharacterSkinDefinition> skins = decodeSkins(parts[statsIndex + 3]);
                boolean implicitDefaultSkin = true;
                boolean implicitBondSkin = true;
                int metadataOffset = statsIndex + 4;
                if (parts.length > metadataOffset && parts[metadataOffset].contains(",")) {
                    String[] implicit = parts[metadataOffset].split(",", -1);
                    implicitDefaultSkin = implicit.length < 1 || Boolean.parseBoolean(implicit[0]);
                    implicitBondSkin = implicit.length < 2 || Boolean.parseBoolean(implicit[1]);
                    metadataOffset++;
                }

                boolean unlockedByDefault = parts.length > metadataOffset && Boolean.parseBoolean(parts[metadataOffset]);
                String unlockHintKey = parts.length > metadataOffset + 1 && !parts[metadataOffset + 1].isBlank() ? unescape(parts[metadataOffset + 1]) : "character.astral_craft.unlock_hint.placeholder";
                int sortOrder = decodeInt(parts, metadataOffset + 2, 1000);
                result.add(new CharacterDefinition(id, nameKey, titleKey, model, texture, entityType, renderer, animationSet, previewAction, maxPveLevel, maxFriendshipLevel, stats, skills, profiles, skins, implicitDefaultSkin, implicitBondSkin, unlockedByDefault, unlockHintKey, sortOrder));
            } catch (Exception ignored) {}
        }

        if (result.isEmpty()) {
            result.add(CharacterDefinition.builtinDefault());
        }

        return result;
    }

    protected static CharacterStatsDefinition decodeStats(String raw) {
        String[] parts = raw.split(",", -1);
        if (parts.length < 3) {
            return CharacterStatsDefinition.defaultStats();
        }

        try {
            return new CharacterStatsDefinition(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        } catch (NumberFormatException exception) {
            return CharacterStatsDefinition.defaultStats();
        }
    }

    protected static int decodeInt(String[] parts, int index, int fallback) {
        if (index >= parts.length) return fallback;
        try {
            return Integer.parseInt(parts[index]);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    protected static String escapeSkills(List<CharacterSkillDefinition> skills) {
        StringBuilder builder = new StringBuilder();
        for (CharacterSkillDefinition skill : skills) {
            if (!builder.isEmpty()) builder.append('~');
            builder.append(escape(skill.id())).append(',')
                    .append(escape(skill.nameKey())).append(',')
                    .append(escape(skill.descriptionKey())).append(',')
                    .append(skill.cooldown()).append(',')
                    .append(escape(skill.pvpNameKey())).append(',')
                    .append(escape(skill.pvpDescriptionKey())).append(',')
                    .append(skill.pvpCooldown()).append(',')
                    .append(escape(skill.pveNameKey())).append(',')
                    .append(escape(skill.pveDescriptionKey())).append(',')
                    .append(skill.pveCooldown()).append(',')
                    .append(skill.cooldownSeconds()).append(',')
                    .append(escape(skill.handler().toString())).append(',')
                    .append(escape(skill.animationAction())).append(',')
                    .append(skill.pvpCooldownSeconds()).append(',')
                    .append(skill.pveCooldownSeconds());
        }

        return builder.toString();
    }

    protected static List<CharacterSkillDefinition> decodeSkills(String raw) {
        List<CharacterSkillDefinition> result = new ArrayList<>();
        if (raw == null || raw.isBlank()) return result;
        for (String entry : raw.split("~")) {
            String[] parts = entry.split(",", -1);
            if (parts.length >= 4) {
                try {
                    result.add(new CharacterSkillDefinition(
                            unescape(parts[0]), unescape(parts[1]),
                            unescape(parts[2]), Integer.parseInt(parts[3]),
                            parts.length >= 11 ? Integer.parseInt(parts[10]) : 0,
                            parts.length >= 12 ? parseIdentifier(unescape(parts[11]), AstralCraft.prefix("default")) : AstralCraft.prefix("default"),
                            parts.length >= 13 ? unescape(parts[12]) : "skill",
                            parts.length >= 5 ? unescape(parts[4]) : "",
                            parts.length >= 6 ? unescape(parts[5]) : "",
                            parts.length >= 7 ? Integer.parseInt(parts[6]) : -1,
                            parts.length >= 14 ? Integer.parseInt(parts[13]) : -1,
                            parts.length >= 8 ? unescape(parts[7]) : "",
                            parts.length >= 9 ? unescape(parts[8]) : "",
                            parts.length >= 10 ? Integer.parseInt(parts[9]) : -1,
                            parts.length >= 15 ? Integer.parseInt(parts[14]) : -1));
                } catch (NumberFormatException ignored) {}
            }
        }

        return result;
    }

    protected static Identifier parseIdentifier(String raw, Identifier fallback) {
        try {
            return raw == null || raw.isBlank() ? fallback : Identifier.parse(raw);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    protected static String escapeProfiles(List<CharacterProfileSection> sections) {
        StringBuilder builder = new StringBuilder();
        for (CharacterProfileSection section : sections) {
            if (!builder.isEmpty()) builder.append('~');
            builder.append(escape(section.titleKey())).append(',').append(escape(section.bodyKey()));
        }

        return builder.toString();
    }

    protected static List<CharacterProfileSection> decodeProfiles(String raw) {
        List<CharacterProfileSection> result = new ArrayList<>();
        if (raw == null || raw.isBlank()) return result;
        for (String entry : raw.split("~")) {
            String[] parts = entry.split(",", -1);
            if (parts.length >= 2) {
                result.add(new CharacterProfileSection(unescape(parts[0]), unescape(parts[1])));
            }
        }

        return result;
    }

    protected static String escapeSkins(List<CharacterSkinDefinition> skins) {
        StringBuilder builder = new StringBuilder();
        for (CharacterSkinDefinition skin : skins) {
            if (!builder.isEmpty()) builder.append('~');
            builder.append(escape(skin.id())).append(',')
                    .append(escape(skin.nameKey())).append(',')
                    .append(escape(skin.texture().toString())).append(',')
                    .append(skin.unlockedByDefault()).append(',')
                    .append(escape(skin.rarityOrNone()));
        }

        return builder.toString();
    }

    protected static List<CharacterSkinDefinition> decodeSkins(String raw) {
        List<CharacterSkinDefinition> result = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return result;
        }

        for (String entry : raw.split("~")) {
            String[] parts = entry.split(",", -1);
            if (parts.length >= 4) {
                try {
                    String rarity = parts.length >= 5 ? unescape(parts[4]) : "none";
                    result.add(new CharacterSkinDefinition(
                            unescape(parts[0]), unescape(parts[1]),
                            Identifier.parse(unescape(parts[2])),
                            Boolean.parseBoolean(parts[3]), rarity));
                } catch (Exception ignored) {}
            }
        }

        return result;
    }

    protected static String escape(String input) {
        return input.replace("%", "%25").replace("|", "%7C").replace("~", "%7E").replace(",", "%2C").replace("\n", "%0A");
    }

    protected static String unescape(String input) {
        return input.replace("%0A", "\n").replace("%2C", ",").replace("%7E", "~").replace("%7C", "|").replace("%25", "%");
    }

}
