package com.astral_craft.common.network;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CharacterSkillCutinPayload(
        Identifier characterId,
        String skinId,
        String skillId,
        Identifier animation,
        int durationTicks
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CharacterSkillCutinPayload> TYPE = new CustomPacketPayload.Type<>(AstralCraft.prefix("character_skill_cutin"));

    public static final StreamCodec<ByteBuf, CharacterSkillCutinPayload> STREAM_CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC,
            CharacterSkillCutinPayload::characterId,
            ByteBufCodecs.STRING_UTF8,
            CharacterSkillCutinPayload::skinId,
            ByteBufCodecs.STRING_UTF8,
            CharacterSkillCutinPayload::skillId,
            Identifier.STREAM_CODEC,
            CharacterSkillCutinPayload::animation,
            ByteBufCodecs.VAR_INT,
            CharacterSkillCutinPayload::durationTicks,
            CharacterSkillCutinPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

}
