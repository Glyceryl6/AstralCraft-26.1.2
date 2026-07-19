package com.astral_craft.common.gameplay.cardback;

import com.astral_craft.AstralCraft;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;

@ParametersAreNonnullByDefault
public class CardBackManager extends SimpleJsonResourceReloadListener<CardBackDefinition> {

    public static final String DIRECTORY = "astral_craft/card_backs";
    public static final CardBackManager INSTANCE = new CardBackManager();
    protected final Map<Identifier, CardBackDefinition> definitions = new LinkedHashMap<>();

    protected CardBackManager() {
        super(CardBackDefinition.CODEC, FileToIdConverter.json(DIRECTORY));
    }

    @Override
    protected void apply(Map<Identifier, CardBackDefinition> elements, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<Identifier, CardBackDefinition> loaded = new LinkedHashMap<>();
        loaded.put(CardBackDefinition.builtinDefault().id(), CardBackDefinition.builtinDefault());
        for (Map.Entry<Identifier, CardBackDefinition> entry : elements.entrySet()) {
            CardBackDefinition value = entry.getValue();
            Identifier id = value.id().equals(AstralCraft.prefix("default"))
                    && !entry.getKey().equals(AstralCraft.prefix("default")) ? entry.getKey() : value.id();
            loaded.put(id, new CardBackDefinition(id, value.nameKey(), value.texture(), value.defaultChoice()));
        }

        this.definitions.clear();
        loaded.values().stream().sorted(Comparator.comparing(value -> value.id().toString()))
                .forEach(value -> this.definitions.put(value.id(), value));
        if (this.definitions.isEmpty()) {
            this.resetToDefault();
        }
    }

    public void resetToDefault() {
        this.definitions.clear();
        CardBackDefinition definition = CardBackDefinition.builtinDefault();
        this.definitions.put(definition.id(), definition);
    }

    public List<CardBackDefinition> values() {
        return new ArrayList<>(this.definitions.values());
    }

    public boolean contains(Identifier id) {
        return this.definitions.containsKey(id);
    }

    public CardBackDefinition get(Identifier id) {
        CardBackDefinition fallback = this.defaultBack();
        return this.definitions.getOrDefault(id, fallback);
    }

    public CardBackDefinition defaultBack() {
        for (CardBackDefinition definition : this.definitions.values()) {
            if (definition.defaultChoice()) return definition;
        }

        return this.definitions.values().stream().findFirst().orElse(CardBackDefinition.builtinDefault());
    }

}