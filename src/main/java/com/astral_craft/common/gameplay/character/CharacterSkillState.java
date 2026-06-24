package com.astral_craft.common.gameplay.character;

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

    public static final StreamCodec<ByteBuf, CharacterSkillState> STREAM_CODEC = new StreamCodec<>() {

        @Override
        public CharacterSkillState decode(ByteBuf buffer) {
            int size = ByteBufCodecs.VAR_INT.decode(buffer);
            Map<String, Integer> cooldowns = new LinkedHashMap<>();
            for (int i = 0; i < size; i++) {
                String key = ByteBufCodecs.STRING_UTF8.decode(buffer);
                int ticks = ByteBufCodecs.VAR_INT.decode(buffer);
                if (ticks > 0) {
                    cooldowns.put(key, ticks);
                }
            }

            return new CharacterSkillState(cooldowns);

        }

        @Override
        public void encode(ByteBuf buffer, CharacterSkillState value) {
            Map<String, Integer> cooldowns = value == null ? Map.of() : value.cooldowns();
            ByteBufCodecs.VAR_INT.encode(buffer, cooldowns.size());
            for (Map.Entry<String, Integer> entry : cooldowns.entrySet()) {
                ByteBufCodecs.STRING_UTF8.encode(buffer, entry.getKey());
                ByteBufCodecs.VAR_INT.encode(buffer, entry.getValue());
            }
        }

    };

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
