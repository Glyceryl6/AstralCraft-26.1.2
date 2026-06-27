package com.astral_craft.common.gameplay.character;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CharacterSkillEffectState {

    protected static final Codec<Map<String, CharacterSkillEffect>> EFFECT_MAP_CODEC = Codec.unboundedMap(Codec.STRING, CharacterSkillEffect.CODEC);

    public static final Codec<CharacterSkillEffectState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            EFFECT_MAP_CODEC.optionalFieldOf("effects", Map.of()).forGetter(CharacterSkillEffectState::effects)
    ).apply(instance, CharacterSkillEffectState::new));

    @ParametersAreNonnullByDefault
    public static final StreamCodec<ByteBuf, CharacterSkillEffectState> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public CharacterSkillEffectState decode(ByteBuf buffer) {
            int size = ByteBufCodecs.VAR_INT.decode(buffer);
            Map<String, CharacterSkillEffect> effects = new LinkedHashMap<>();
            for (int i = 0; i < size; i++) {
                String key = ByteBufCodecs.STRING_UTF8.decode(buffer);
                CharacterSkillEffect effect = CharacterSkillEffect.STREAM_CODEC.decode(buffer);
                if (effect.durationTicks() > 0 && !key.isBlank()) {
                    effects.put(key, effect);
                }
            }

            return new CharacterSkillEffectState(effects);
        }

        @Override
        public void encode(ByteBuf buffer, @Nullable CharacterSkillEffectState value) {
            Map<String, CharacterSkillEffect> effects = value == null ? Map.of() : value.effects();
            ByteBufCodecs.VAR_INT.encode(buffer, effects.size());
            for (Map.Entry<String, CharacterSkillEffect> entry : effects.entrySet()) {
                ByteBufCodecs.STRING_UTF8.encode(buffer, entry.getKey());
                CharacterSkillEffect.STREAM_CODEC.encode(buffer, entry.getValue());
            }
        }
    };

    protected Map<String, CharacterSkillEffect> effects = new LinkedHashMap<>();

    public CharacterSkillEffectState(Map<String, CharacterSkillEffect> effects) {
        if (effects != null) {
            for (Map.Entry<String, CharacterSkillEffect> entry : effects.entrySet()) {
                if (entry.getKey() != null && !entry.getKey().isBlank() && entry.getValue() != null && entry.getValue().durationTicks() > 0) {
                    this.effects.put(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    public static CharacterSkillEffectState empty() {
        return new CharacterSkillEffectState(Map.of());
    }

    public Map<String, CharacterSkillEffect> effects() {
        return Map.copyOf(this.effects);
    }

    public List<CharacterSkillEffect> activeEffects() {
        return List.copyOf(this.effects.values());
    }

    public boolean isEmpty() {
        return this.effects.isEmpty();
    }

    public boolean contains(String id) {
        return id != null && this.effects.containsKey(id);
    }

    public boolean contains(Identifier id) {
        return id != null && this.contains(id.toString());
    }

    public CharacterSkillEffect add(CharacterSkillEffect effect) {
        if (effect == null || effect.durationTicks() <= 0) return null;
        return this.effects.put(effect.safeId(), effect);
    }

    public CharacterSkillEffect remove(String id) {
        if (id == null || id.isBlank()) return null;
        return this.effects.remove(id);
    }

    public TickResult tickAndCollectExpired() {
        if (this.effects.isEmpty()) return TickResult.empty();
        Map<String, CharacterSkillEffect> next = new LinkedHashMap<>();
        List<CharacterSkillEffect> ticked = new ArrayList<>();
        List<CharacterSkillEffect> expired = new ArrayList<>();
        for (Map.Entry<String, CharacterSkillEffect> entry : this.effects.entrySet()) {
            CharacterSkillEffect current = entry.getValue();
            ticked.add(current);
            CharacterSkillEffect nextEffect = current.ticked();
            if (nextEffect.expired()) {
                expired.add(current);
            } else {
                next.put(entry.getKey(), nextEffect);
            }
        }
        this.effects = next;
        return new TickResult(ticked, expired, true);
    }

    public boolean tick() {
        return this.tickAndCollectExpired().changed();
    }

    public List<CharacterSkillEffect> removeEffectsNotFrom(Identifier characterId) {
        if (characterId == null || this.effects.isEmpty()) return List.of();
        List<CharacterSkillEffect> removed = new ArrayList<>();
        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, CharacterSkillEffect> entry : this.effects.entrySet()) {
            if (!characterId.equals(entry.getValue().safeCharacterId())) {
                removed.add(entry.getValue());
                keys.add(entry.getKey());
            }
        }
        for (String key : keys) {
            this.effects.remove(key);
        }
        return removed;
    }

    public List<CharacterSkillEffect> clearAndCollectRemoved() {
        if (this.effects.isEmpty()) return List.of();
        List<CharacterSkillEffect> removed = new ArrayList<>(this.effects.values());
        this.effects.clear();
        return removed;
    }

    public boolean clear() {
        return !this.clearAndCollectRemoved().isEmpty();
    }

    public record TickResult(List<CharacterSkillEffect> ticked, List<CharacterSkillEffect> expired, boolean changed) {

        public static TickResult empty() {
            return new TickResult(List.of(), List.of(), false);
        }

    }

}
