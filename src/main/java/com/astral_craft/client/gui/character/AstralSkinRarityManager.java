package com.astral_craft.client.gui.character;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.character.skin.CharacterSkinRarityDefinition;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@ParametersAreNonnullByDefault
public class AstralSkinRarityManager extends SimpleJsonResourceReloadListener<CharacterSkinRarityDefinition> {

    public static final String DIRECTORY = "astral_craft/skin_rarities";
    public static final AstralSkinRarityManager INSTANCE = new AstralSkinRarityManager();

    protected final Map<Identifier, CharacterSkinRarityDefinition> rarities = new LinkedHashMap<>();

    public AstralSkinRarityManager() {
        super(CharacterSkinRarityDefinition.CODEC, FileToIdConverter.json(DIRECTORY));
        this.reloadFallbacks();
    }

    @Override
    protected void apply(Map<Identifier, CharacterSkinRarityDefinition> elements, ResourceManager resourceManager, ProfilerFiller profiler) {
        this.reloadFallbacks();
        this.rarities.putAll(elements);
    }

    public CharacterSkinRarityDefinition getOrDefault(String rawId) {
        return this.rarities.getOrDefault(this.normalize(rawId), CharacterSkinRarityDefinition.none());
    }

    public Identifier normalize(String rawId) {
        String id = rawId.isBlank() ? "none" : rawId.trim().toLowerCase(Locale.ROOT);
        if (id.indexOf(':') >= 0) {
            try {
                return Identifier.parse(id);
            } catch (Exception ignored) {
                return AstralCraft.prefix("none");
            }
        }

        return switch (id) {
            case "common", "bond" -> AstralCraft.prefix("none");
            case "rare" -> AstralCraft.prefix("sapphire");
            case "epic" -> AstralCraft.prefix("amethyst");
            case "legendary" -> AstralCraft.prefix("ultimate");
            default -> AstralCraft.prefix(id);
        };
    }

    public boolean shouldRenderBadge(String rawId) {
        Identifier id = this.normalize(rawId);
        return !id.getPath().equals("none");
    }

    protected void reloadFallbacks() {
        this.rarities.clear();
        this.rarities.put(AstralCraft.prefix("none"), CharacterSkinRarityDefinition.none());
        this.rarities.put(AstralCraft.prefix("sapphire"), new CharacterSkinRarityDefinition("skin_rarity.astral_craft.sapphire", 0xFF25C7FF, 0xFF111111, 0xFF25C7FF));
        this.rarities.put(AstralCraft.prefix("amethyst"), new CharacterSkinRarityDefinition("skin_rarity.astral_craft.amethyst", 0xFFD551FF, 0xFF111111, 0xFFD551FF));
        this.rarities.put(AstralCraft.prefix("emerald"), new CharacterSkinRarityDefinition("skin_rarity.astral_craft.emerald", 0xFF8DFF64, 0xFF111111, 0xFF8DFF64));
        this.rarities.put(AstralCraft.prefix("platinum"), new CharacterSkinRarityDefinition("skin_rarity.astral_craft.platinum", 0xFFFFCFB8, 0xFF111111, 0xFFFFCFB8));
        this.rarities.put(AstralCraft.prefix("ultimate"), new CharacterSkinRarityDefinition("skin_rarity.astral_craft.ultimate", 0xFFE5FF75, 0xFF111111, 0xFFE5FF75));
    }

}
