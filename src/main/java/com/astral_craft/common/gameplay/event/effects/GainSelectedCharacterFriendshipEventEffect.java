package com.astral_craft.common.gameplay.event.effects;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.character.CharacterManager;
import com.astral_craft.common.gameplay.character.CharacterProgressManager;
import com.astral_craft.common.gameplay.event.AstralEventContext;
import com.astral_craft.common.gameplay.event.AstralEventEffect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerPlayer;

public record GainSelectedCharacterFriendshipEventEffect(int amount) implements AstralEventEffect {

    public static final MapCodec<GainSelectedCharacterFriendshipEventEffect> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.optionalFieldOf("amount", 1).forGetter(GainSelectedCharacterFriendshipEventEffect::amount)
    ).apply(instance, GainSelectedCharacterFriendshipEventEffect::new));

    @Override
    public String typeId() {
        return AstralCraft.prefix("gain_selected_character_friendship").toString();
    }

    @Override
    public MapCodec<? extends AstralEventEffect> codec() {
        return CODEC;
    }

    @Override
    public void apply(AstralEventContext context) {
        ServerPlayer target = context.targetPlayer() != null ? context.targetPlayer() : context.triggerPlayer();
        if (target != null) {
            CharacterProgressManager.addFriendship(target, CharacterManager.INSTANCE.defaultCharacter().id(), Math.max(1, this.amount));
        }
    }

}