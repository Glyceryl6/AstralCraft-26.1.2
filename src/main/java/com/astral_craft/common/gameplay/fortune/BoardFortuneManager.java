package com.astral_craft.common.gameplay.fortune;

import com.astral_craft.AstralCraft;
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
public class BoardFortuneManager extends SimpleJsonResourceReloadListener<BoardFortuneDefinition> {

    public static final String DIRECTORY = "astral_craft/fortunes";
    public static final BoardFortuneManager INSTANCE = new BoardFortuneManager();
    private final Map<Identifier, BoardFortuneDefinition> definitions = new LinkedHashMap<>();

    public BoardFortuneManager() {
        super(BoardFortuneDefinition.CODEC, FileToIdConverter.json(DIRECTORY));
    }

    @Override
    protected void apply(Map<Identifier, BoardFortuneDefinition> elements, ResourceManager resourceManager,
                         ProfilerFiller profiler) {
        this.definitions.clear();
        elements.entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.comparing(Identifier::toString)))
                .forEach(entry -> {
                    BoardFortuneDefinition value = entry.getValue();
                    Identifier id = entry.getKey();
                    this.definitions.put(id, new BoardFortuneDefinition(id,
                            "fortune." + id.getNamespace() + "." + id.getPath() + ".name",
                            "fortune." + id.getNamespace() + "." + id.getPath() + ".description",
                            value.texture(), value.weight(), value.effects()));
                });
    }

    public List<BoardFortuneDefinition> values() {
        return new ArrayList<>(this.definitions.values());
    }
}
