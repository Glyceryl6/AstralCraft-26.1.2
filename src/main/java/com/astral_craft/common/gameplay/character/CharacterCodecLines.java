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
                    .append(definition.baseStats().attack()).append(',')
                    .append(definition.baseStats().defense()).append(',')
                    .append(definition.baseStats().health()).append(',')
                    .append(definition.baseStats().speed()).append('|')
                    .append(escapeSkills(definition.skills())).append('|')
                    .append(escapeProfiles(definition.profileSections())).append('|')
                    .append(escapeSkins(definition.skins()));
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
                CharacterStatsDefinition stats = decodeStats(parts[5]);
                List<CharacterSkillDefinition> skills = decodeSkills(parts[6]);
                List<CharacterProfileSection> profiles = decodeProfiles(parts[7]);
                List<CharacterSkinDefinition> skins = decodeSkins(parts[8]);
                result.add(new CharacterDefinition(id, nameKey, titleKey, model, texture, stats, skills, profiles, skins));
            } catch (Exception ignored) {}
        }

        if (result.isEmpty()) {
            result.add(CharacterDefinition.builtinDefault());
        }

        return result;
    }

    protected static CharacterStatsDefinition decodeStats(String raw) {
        String[] parts = raw.split(",", -1);
        if (parts.length < 4) {
            return CharacterStatsDefinition.defaultStats();
        }

        try {
            return new CharacterStatsDefinition(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
        } catch (NumberFormatException exception) {
            return CharacterStatsDefinition.defaultStats();
        }
    }

    protected static String escapeSkills(List<CharacterSkillDefinition> skills) {
        StringBuilder builder = new StringBuilder();
        for (CharacterSkillDefinition skill : skills) {
            if (!builder.isEmpty()) builder.append('~');
            builder.append(escape(skill.id())).append(',')
                    .append(escape(skill.nameKey())).append(',')
                    .append(escape(skill.descriptionKey())).append(',')
                    .append(skill.cooldown());
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
                            unescape(parts[2]), Integer.parseInt(parts[3])));
                } catch (NumberFormatException ignored) {}
            }
        }

        return result;
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
                    .append(skin.unlockedByDefault());
        }

        return builder.toString();
    }

    protected static List<CharacterSkinDefinition> decodeSkins(String raw) {
        List<CharacterSkinDefinition> result = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            result.add(new CharacterSkinDefinition("default", "character.astral_craft.skin.default",
                    AstralCraft.prefix("textures/entity/character/default.png"), true));
            return result;
        }

        for (String entry : raw.split("~")) {
            String[] parts = entry.split(",", -1);
            if (parts.length >= 4) {
                try {
                    result.add(new CharacterSkinDefinition(
                            unescape(parts[0]), unescape(parts[1]),
                            Identifier.parse(unescape(parts[2])),
                            Boolean.parseBoolean(parts[3])));
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