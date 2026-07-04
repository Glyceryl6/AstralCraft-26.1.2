package com.astral_craft.common.gameplay.event;

import com.astral_craft.common.gameplay.event.type.AstralEventLocalizationKeys;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ParametersAreNonnullByDefault
public class AstralEventManager extends SimpleJsonResourceReloadListener<AstralEventDefinition> {

    public static final String DIRECTORY = "astral_craft/events";
    public static final AstralEventManager INSTANCE = new AstralEventManager();

    protected final Map<Identifier, AstralEventDefinition> definitions = new LinkedHashMap<>();

    public AstralEventManager() {
        super(AstralEventDefinition.CODEC, FileToIdConverter.json(DIRECTORY));
    }

    @Override
    protected void apply(Map<Identifier, AstralEventDefinition> elements, ResourceManager resourceManager, ProfilerFiller profiler) {
        Map<Identifier, AstralEventDefinition> loaded = new LinkedHashMap<>();
        for (Map.Entry<Identifier, AstralEventDefinition> entry : elements.entrySet()) {
            AstralEventDefinition definition = entry.getValue();
            Identifier id = entry.getKey();
            loaded.put(id, new AstralEventDefinition(id,
                    AstralEventLocalizationKeys.name(id),
                    AstralEventLocalizationKeys.description(id),
                    definition.kind(),
                    definition.texture(),
                    definition.triggers(),
                    definition.conditions(),
                    definition.difficulties(),
                    definition.target(),
                    definition.triggerSettings(),
                    definition.effects(),
                    definition.intervalEffects(),
                    definition.activeConditions(),
                    definition.activeEffects(),
                    definition.endEffects(),
                    definition.cooldownTicks(),
                    definition.chance(),
                    definition.broadcast(),
                    definition.timing(),
                    definition.durationTicks(),
                    definition.intervalTicks()));
        }

        this.definitions.clear();
        loaded.values().stream().sorted(Comparator.comparing(value -> value.id().toString())).forEach(value -> this.definitions.put(value.id(), value));
    }

    public List<AstralEventDefinition> values() {
        return new ArrayList<>(this.definitions.values());
    }

    public List<String> idStrings() {
        List<String> result = new ArrayList<>();
        for (Identifier id : this.definitions.keySet()) {
            result.add(id.toString());
        }

        return result;
    }

    public List<AstralEventDefinition> automaticEvents() {
        List<AstralEventDefinition> result = new ArrayList<>();
        for (AstralEventDefinition definition : this.definitions.values()) {
            if (definition.canAutoTrigger()) {
                result.add(definition);
            }
        }

        return result;
    }

    public boolean contains(Identifier id) {
        return this.definitions.containsKey(id);
    }

    public AstralEventDefinition get(Identifier id) {
        return this.definitions.get(id);
    }

}