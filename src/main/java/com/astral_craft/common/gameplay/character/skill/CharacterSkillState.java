package com.astral_craft.common.gameplay.character.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.LinkedHashMap;
import java.util.Map;

public class CharacterSkillState {

    protected static final Codec<Map<String, Integer>> COOLDOWN_CODEC = Codec.unboundedMap(Codec.STRING, Codec.INT);

    public static final Codec<CharacterSkillState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            COOLDOWN_CODEC.optionalFieldOf("cooldowns", Map.of()).forGetter(CharacterSkillState::cooldowns)
    ).apply(instance, CharacterSkillState::new));

    private static final StreamCodec<ByteBuf, Map<String, Integer>> MAP_STREAM_CODEC = ByteBufCodecs.map(
            LinkedHashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_INT, 256);

    public static final StreamCodec<ByteBuf, CharacterSkillState> STREAM_CODEC = MAP_STREAM_CODEC.map(
            CharacterSkillState::new, CharacterSkillState::cooldowns);

    protected Map<String, Integer> cooldowns = new LinkedHashMap<>();

    public CharacterSkillState(Map<String, Integer> cooldowns) {
        if (cooldowns != null) {
            for (Map.Entry<String, Integer> entry : cooldowns.entrySet()) {
                if (entry.getKey() != null && !entry.getKey().isBlank() && entry.getValue() != null && entry.getValue() > 0) {
                    this.cooldowns.put(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    public static CharacterSkillState empty() {
        return new CharacterSkillState(Map.of());
    }

    public Map<String, Integer> cooldowns() {
        return Map.copyOf(this.cooldowns);
    }

    public int cooldown(String key) {
        return this.cooldowns.getOrDefault(key, 0);
    }

    public boolean onCooldown(String key) {
        return this.cooldown(key) > 0;
    }

    public void setCooldown(String key, int ticks) {
        if (key == null || key.isBlank()) return;
        if (ticks <= 0) {
            this.cooldowns.remove(key);
        } else {
            this.cooldowns.put(key, ticks);
        }
    }

    public boolean tick() {
        if (this.cooldowns.isEmpty()) return false;
        Map<String, Integer> next = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : this.cooldowns.entrySet()) {
            int value = entry.getValue() - 1;
            if (value > 0) {
                next.put(entry.getKey(), value);
            }
        }

        if (next.equals(this.cooldowns)) {
            return false;
        }
        this.cooldowns = next;
        return true;
    }

}
