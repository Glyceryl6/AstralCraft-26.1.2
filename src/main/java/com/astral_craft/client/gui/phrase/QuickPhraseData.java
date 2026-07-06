package com.astral_craft.client.gui.phrase;

import com.astral_craft.AstralCraft;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class QuickPhraseData {

    public static final Map<String, Integer> MOD_PHRASE_COUNTS = new LinkedHashMap<>();

    static {
        registerModPhraseCount(AstralCraft.MOD_ID, 24);
    }

    public static void registerModPhraseCount(String modId, int phraseCount) {
        if (modId == null || modId.isBlank()) return;
        MOD_PHRASE_COUNTS.put(modId.trim(), Math.max(0, phraseCount));
    }

    public static List<ModPhraseGroup> modGroups() {
        List<ModPhraseGroup> groups = new ArrayList<>();
        groups.add(groupFor(AstralCraft.MOD_ID, MOD_PHRASE_COUNTS.getOrDefault(AstralCraft.MOD_ID, 0)));
        for (Map.Entry<String, Integer> entry : MOD_PHRASE_COUNTS.entrySet()) {
            String modId = entry.getKey();
            if (AstralCraft.MOD_ID.equals(modId)) continue;
            int count = Math.max(0, entry.getValue() == null ? 0 : entry.getValue());
            if (count > 0) groups.add(groupFor(modId, count));
        }

        return List.copyOf(groups);
    }

    public static Component phraseComponent(Phrase phrase) {
        return Component.translatable(phrase.key());
    }

    public static Component modDisplayName(ModPhraseGroup group) {
        String namespace = group.namespace();
        Language language = Language.getInstance();
        for (String key : defaultModDisplayKeys(namespace)) {
            if (language.has(key)) {
                return Component.translatable(key);
            }
        }

        return Component.literal(namespace);
    }

    public static String phraseKey(String modId, int index) {
        return "quick_phrase." + modId + "." + index;
    }

    private static ModPhraseGroup groupFor(String modId, int count) {
        List<Phrase> phrases = new ArrayList<>(Math.max(0, count));
        for (int i = 0; i < count; i++) {
            phrases.add(new Phrase(phraseKey(modId, i)));
        }

        return new ModPhraseGroup(modId, List.copyOf(phrases));
    }

    private static List<String> defaultModDisplayKeys(String namespace) {
        return List.of(
                "itemGroup." + namespace,
                "itemGroup." + namespace + ".main",
                "itemGroup." + namespace + ".tab",
                "itemGroup." + namespace + ".creative_tab",
                "itemGroup." + namespace + ".items",
                "itemGroup." + namespace + "." + namespace);
    }

    public record ModPhraseGroup(String namespace, List<Phrase> phrases) {}

    public record Phrase(String key) {}

}