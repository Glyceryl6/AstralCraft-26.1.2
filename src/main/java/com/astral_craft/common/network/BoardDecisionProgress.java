package com.astral_craft.common.network;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

/** Shared network representation for a board decision timer and its moving portrait. */
public record BoardDecisionProgress(
        int remainingTicks,
        int durationTicks,
        Identifier characterId,
        Identifier skinId) {

    public static final StreamCodec<ByteBuf, BoardDecisionProgress> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, BoardDecisionProgress::remainingTicks,
            ByteBufCodecs.VAR_INT, BoardDecisionProgress::durationTicks,
            Identifier.STREAM_CODEC, BoardDecisionProgress::characterId,
            Identifier.STREAM_CODEC, BoardDecisionProgress::skinId,
            BoardDecisionProgress::new);

    public BoardDecisionProgress {
        remainingTicks = Math.max(0, remainingTicks);
        durationTicks = Math.max(1, durationTicks);
        characterId = characterId == null ? AstralCraft.prefix("mimi") : characterId;
        skinId = skinId == null ? Identifier.withDefaultNamespace("default") : skinId;
    }

}