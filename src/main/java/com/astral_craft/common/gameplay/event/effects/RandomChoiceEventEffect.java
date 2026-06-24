package com.astral_craft.common.gameplay.event.effects;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.astral_craft.common.gameplay.event.AstralEventEffect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

public record RandomChoiceEventEffect(List<Entry> entries) implements AstralEventEffect {

    public static final MapCodec<RandomChoiceEventEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Entry.CODEC.listOf().optionalFieldOf("entries", List.of()).forGetter(RandomChoiceEventEffect::entries)
    ).apply(instance, RandomChoiceEventEffect::new));

    @Override
    public String typeId() {
        return AstralCraft.prefix("random_choice").toString();
    }

    @Override
    public MapCodec<? extends AstralEventEffect> codec() {
        return CODEC;
    }

    @Override
    public void apply(AstralEventContext context) {
        if (this.entries.isEmpty()) return;
        int total = 0;
        for (Entry entry : this.entries) {
            total += Math.max(0, entry.weight());
        }
        if (total <= 0) return;
        int cursor = context.random().nextInt(total);
        for (Entry entry : this.entries) {
            int weight = Math.max(0, entry.weight());
            if (cursor < weight) {
                for (AstralEventEffect effect : entry.effects()) {
                    if (effect != null) {
                        effect.apply(context);
                    }
                }
                return;
            }
            cursor -= weight;
        }
    }

    public record Entry(int weight, List<AstralEventEffect> effects) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.INT.optionalFieldOf("weight", 1).forGetter(Entry::weight),
                AstralEventEffect.CODEC.listOf().optionalFieldOf("effects", List.of()).forGetter(Entry::effects)
        ).apply(instance, Entry::new));
    }

}