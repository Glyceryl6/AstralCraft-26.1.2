package com.astral_craft.client.gui.cardback;

import com.astral_craft.client.jpgloader.ScopedJpgTextureCache;
import com.astral_craft.common.gameplay.dice.DiceSkinPreferenceManager;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Scans selectable appearance resources by file signature instead of relying on filename extensions. */
public class CardBackResourceCache {

    private static final String CARD_BACK_DIRECTORY = "textures/gui/cards/back";
    private static final String DICE_SKIN_DIRECTORY = "textures/entity/dice/skins";
    private static List<CardBackDefinition> cardBacks;
    private static List<CardBackDefinition> diceSkins;

    public static synchronized List<CardBackDefinition> values() {
        if (cardBacks == null) cardBacks = scan(CARD_BACK_DIRECTORY, CardBackDefinition.builtinDefault());
        return cardBacks;
    }

    public static synchronized List<CardBackDefinition> diceSkins() {
        if (diceSkins == null) {
            CardBackDefinition fallback = new CardBackDefinition(DiceSkinPreferenceManager.DEFAULT_TEXTURE,
                    "dice_skin.astral_craft.default", DiceSkinPreferenceManager.DEFAULT_TEXTURE, true);
            diceSkins = scan(DICE_SKIN_DIRECTORY, fallback);
        }
        return diceSkins;
    }

    public static synchronized void clear() {
        cardBacks = null;
        diceSkins = null;
    }

    private static List<CardBackDefinition> scan(String directory, CardBackDefinition fallback) {
        Map<Identifier, CardBackDefinition> definitions = new LinkedHashMap<>();
        definitions.put(fallback.texture(), fallback);
        Minecraft.getInstance().getResourceManager().listResources(directory, ignored -> true).keySet().stream()
                .filter(ScopedJpgTextureCache::isSupportedTexture)
                .forEach(texture -> definitions.putIfAbsent(texture, CardBackDefinition.scanned(texture)));
        return List.copyOf(definitions.values());
    }

}