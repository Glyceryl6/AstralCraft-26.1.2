package com.astral_craft.common.gameplay.character;

import com.astral_craft.AstralCraft;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

import javax.annotation.ParametersAreNonnullByDefault;
import java.util.LinkedHashMap;
import java.util.Map;

public record CharacterSkillEffect(
        String id,
        Identifier characterId,
        Identifier handlerId,
        String nameKey,
        int durationTicks,
        int amplifier,
        Map<String, String> properties) {

    private static final Codec<Map<String, String>> PROPERTY_CODEC = Codec.unboundedMap(Codec.STRING, Codec.STRING);

    public static final Codec<CharacterSkillEffect> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(CharacterSkillEffect::id),
            Identifier.CODEC.optionalFieldOf("character", AstralCraft.prefix("mimi")).forGetter(CharacterSkillEffect::characterId),
            Identifier.CODEC.optionalFieldOf("handler", AstralCraft.prefix("default")).forGetter(CharacterSkillEffect::handlerId),
            Codec.STRING.optionalFieldOf("name_key", "effect.astral_craft.character_skill.generic").forGetter(CharacterSkillEffect::nameKey),
            Codec.INT.optionalFieldOf("duration_ticks", 200).forGetter(CharacterSkillEffect::durationTicks),
            Codec.INT.optionalFieldOf("amplifier", 0).forGetter(CharacterSkillEffect::amplifier),
            PROPERTY_CODEC.optionalFieldOf("properties", Map.of()).forGetter(CharacterSkillEffect::properties)
    ).apply(instance, CharacterSkillEffect::new));

    @ParametersAreNonnullByDefault
    public static final StreamCodec<ByteBuf, CharacterSkillEffect> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public CharacterSkillEffect decode(ByteBuf buffer) {
            String id = ByteBufCodecs.STRING_UTF8.decode(buffer);
            Identifier characterId = Identifier.STREAM_CODEC.decode(buffer);
            Identifier handlerId = Identifier.STREAM_CODEC.decode(buffer);
            String nameKey = ByteBufCodecs.STRING_UTF8.decode(buffer);
            int durationTicks = ByteBufCodecs.VAR_INT.decode(buffer);
            int amplifier = ByteBufCodecs.VAR_INT.decode(buffer);
            int size = ByteBufCodecs.VAR_INT.decode(buffer);
            Map<String, String> properties = new LinkedHashMap<>();
            for (int i = 0; i < size; i++) {
                String key = ByteBufCodecs.STRING_UTF8.decode(buffer);
                String value = ByteBufCodecs.STRING_UTF8.decode(buffer);
                if (!key.isBlank()) {
                    properties.put(key, value);
                }
            }
            return new CharacterSkillEffect(id, characterId, handlerId, nameKey, durationTicks, amplifier, properties);
        }

        @Override
        public void encode(ByteBuf buffer, @Nullable CharacterSkillEffect value) {
            CharacterSkillEffect safe = value == null ? CharacterSkillEffect.generic("skill", AstralCraft.prefix("mimi"), 1) : value;
            ByteBufCodecs.STRING_UTF8.encode(buffer, safe.safeId());
            Identifier.STREAM_CODEC.encode(buffer, safe.safeCharacterId());
            Identifier.STREAM_CODEC.encode(buffer, safe.safeHandlerId());
            ByteBufCodecs.STRING_UTF8.encode(buffer, safe.safeNameKey());
            ByteBufCodecs.VAR_INT.encode(buffer, Math.max(0, safe.durationTicks()));
            ByteBufCodecs.VAR_INT.encode(buffer, Math.max(0, safe.amplifier()));
            Map<String, String> properties = safe.safeProperties();
            ByteBufCodecs.VAR_INT.encode(buffer, properties.size());
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                ByteBufCodecs.STRING_UTF8.encode(buffer, entry.getKey());
                ByteBufCodecs.STRING_UTF8.encode(buffer, entry.getValue());
            }
        }
    };

    public CharacterSkillEffect {
        id = id == null || id.isBlank() ? "skill" : id;
        characterId = characterId == null ? AstralCraft.prefix("mimi") : characterId;
        handlerId = handlerId == null || handlerId.equals(AstralCraft.prefix("default")) ? characterId : handlerId;
        nameKey = nameKey == null || nameKey.isBlank() ? "effect.astral_craft.character_skill.generic" : nameKey;
        durationTicks = Math.max(0, durationTicks);
        amplifier = Math.max(0, amplifier);
        properties = properties == null ? Map.of() : Map.copyOf(properties);
    }

    public static CharacterSkillEffect generic(String id, Identifier characterId, int durationTicks) {
        return new CharacterSkillEffect(id, characterId, characterId, "effect.astral_craft.character_skill.generic", durationTicks, 0, Map.of());
    }

    public CharacterSkillEffect ticked() {
        return new CharacterSkillEffect(this.safeId(), this.safeCharacterId(), this.safeHandlerId(), this.safeNameKey(), Math.max(0, this.durationTicks - 1), this.amplifier, this.safeProperties());
    }

    public boolean expired() {
        return this.durationTicks <= 0;
    }

    public CharacterSkillEffect refresh(int durationTicks) {
        return new CharacterSkillEffect(this.safeId(), this.safeCharacterId(), this.safeHandlerId(), this.safeNameKey(), Math.max(0, durationTicks), this.amplifier, this.safeProperties());
    }

    public CharacterSkillEffect withProperty(String key, String value) {
        Map<String, String> copy = new LinkedHashMap<>(this.safeProperties());
        if (key != null && !key.isBlank()) {
            if (value == null) {
                copy.remove(key);
            } else {
                copy.put(key, value);
            }
        }
        return new CharacterSkillEffect(this.safeId(), this.safeCharacterId(), this.safeHandlerId(), this.safeNameKey(), this.durationTicks, this.amplifier, copy);
    }

    public String property(String key) {
        if (key == null || key.isBlank()) return "";
        return this.safeProperties().getOrDefault(key, "");
    }

    public String property(Identifier key) {
        return key == null ? "" : this.property(key.toString());
    }

    public int propertyInt(String key, int fallback) {
        try {
            String value = this.property(key);
            return value.isBlank() ? fallback : Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    public int propertyInt(Identifier key, int fallback) {
        return key == null ? fallback : this.propertyInt(key.toString(), fallback);
    }

    public boolean propertyBoolean(String key) {
        String value = this.property(key);
        return "true".equalsIgnoreCase(value) || "1".equals(value) || "yes".equalsIgnoreCase(value);
    }

    public boolean propertyBoolean(Identifier key) {
        return key != null && this.propertyBoolean(key.toString());
    }

    public Identifier safeIdAsIdentifier() {
        try {
            String value = this.safeId();
            return value.contains(":") ? Identifier.parse(value) : AstralCraft.prefix(value);
        } catch (Exception exception) {
            return AstralCraft.prefix("skill");
        }
    }

    public String safeId() {
        return this.id == null || this.id.isBlank() ? "skill" : this.id;
    }

    public Identifier safeCharacterId() {
        return this.characterId == null ? AstralCraft.prefix("mimi") : this.characterId;
    }

    public Identifier safeHandlerId() {
        return this.handlerId == null || this.handlerId.equals(AstralCraft.prefix("default")) ? this.safeCharacterId() : this.handlerId;
    }

    public String safeNameKey() {
        return this.nameKey == null || this.nameKey.isBlank() ? "effect.astral_craft.character_skill.generic" : this.nameKey;
    }

    public Map<String, String> safeProperties() {
        return this.properties == null ? Map.of() : this.properties;
    }

}
