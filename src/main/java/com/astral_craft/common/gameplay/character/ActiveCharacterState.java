package com.astral_craft.common.gameplay.character;

import com.astral_craft.AstralCraft;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

public record ActiveCharacterState(boolean active, Identifier characterId, String skinId, int level, int friendship, int attack, int defense, int health) {

    public static final ActiveCharacterState NONE = new ActiveCharacterState(false, AstralCraft.prefix("mimi"), "default", 1, 1, 1, 1, 10);

    public static final Codec<ActiveCharacterState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.optionalFieldOf("active", false).forGetter(ActiveCharacterState::active),
            Identifier.CODEC.optionalFieldOf("character", AstralCraft.prefix("mimi")).forGetter(ActiveCharacterState::characterId),
            Codec.STRING.optionalFieldOf("skin", "default").forGetter(ActiveCharacterState::skinId),
            Codec.INT.optionalFieldOf("level", 1).forGetter(ActiveCharacterState::level),
            Codec.INT.optionalFieldOf("friendship", 1).forGetter(ActiveCharacterState::friendship),
            Codec.INT.optionalFieldOf("attack", 1).forGetter(ActiveCharacterState::attack),
            Codec.INT.optionalFieldOf("defense", 1).forGetter(ActiveCharacterState::defense),
            Codec.INT.optionalFieldOf("health", 10).forGetter(ActiveCharacterState::health)
    ).apply(instance, ActiveCharacterState::new));

    public static final StreamCodec<ByteBuf, ActiveCharacterState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            ActiveCharacterState::active,
            Identifier.STREAM_CODEC,
            ActiveCharacterState::characterId,
            ByteBufCodecs.STRING_UTF8,
            ActiveCharacterState::skinId,
            ByteBufCodecs.VAR_INT,
            ActiveCharacterState::level,
            ByteBufCodecs.VAR_INT,
            ActiveCharacterState::friendship,
            ByteBufCodecs.VAR_INT,
            ActiveCharacterState::attack,
            ByteBufCodecs.VAR_INT,
            ActiveCharacterState::defense,
            ByteBufCodecs.VAR_INT,
            ActiveCharacterState::health,
            ActiveCharacterState::new);

    public ActiveCharacterState {
        characterId = characterId == null ? AstralCraft.prefix("mimi") : characterId;
        skinId = skinId == null || skinId.isBlank() ? "default" : skinId;
        level = Math.max(1, level);
        friendship = Math.max(1, friendship);
        attack = Math.max(0, attack);
        defense = Math.max(0, defense);
        health = Math.max(1, health);
    }

    public static ActiveCharacterState of(CharacterDefinition definition, CharacterProgressEntry progress) {
        if (definition == null) {
            return NONE;
        }

        CharacterProgressEntry safeProgress = progress == null ? CharacterProgressEntry.unlockedDefault() : progress;
        CharacterStatsDefinition stats = definition.baseStats();
        return new ActiveCharacterState(true, definition.id(), safeProgress.selectedSkin(), safeProgress.level(), safeProgress.friendship(), stats.attack(), stats.defense(), stats.health());
    }

    public ActiveCharacterState withSkin(String skinId) {
        return new ActiveCharacterState(this.active, this.characterId, skinId, this.level, this.friendship, this.attack, this.defense, this.health);
    }

    public ActiveCharacterState inactive() {
        return new ActiveCharacterState(false, this.characterId, this.skinId, this.level, this.friendship, this.attack, this.defense, this.health);
    }

}