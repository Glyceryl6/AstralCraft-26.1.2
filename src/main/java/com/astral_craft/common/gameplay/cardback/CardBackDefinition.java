package com.astral_craft.common.gameplay.cardback;

import com.astral_craft.AstralCraft;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

public record CardBackDefinition(Identifier id, String nameKey, Identifier texture, boolean defaultChoice) {

    public static final Codec<CardBackDefinition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.optionalFieldOf("id", AstralCraft.prefix("default")).forGetter(CardBackDefinition::id),
            Codec.STRING.optionalFieldOf("name_key", "card_back.astral_craft.default").forGetter(CardBackDefinition::nameKey),
            Identifier.CODEC.fieldOf("texture").forGetter(CardBackDefinition::texture),
            Codec.BOOL.optionalFieldOf("default", false).forGetter(CardBackDefinition::defaultChoice)
    ).apply(instance, CardBackDefinition::new));

    public static final StreamCodec<ByteBuf, CardBackDefinition> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC,
            CardBackDefinition::id,
            ByteBufCodecs.STRING_UTF8,
            CardBackDefinition::nameKey,
            Identifier.STREAM_CODEC,
            CardBackDefinition::texture,
            ByteBufCodecs.BOOL,
            CardBackDefinition::defaultChoice,
            CardBackDefinition::new);

    public static CardBackDefinition builtinDefault() {
        return new CardBackDefinition(
                AstralCraft.prefix("default"),
                "card_back.astral_craft.default",
                AstralCraft.prefix("textures/gui/cards/card_back.png"),
                true);
    }

}