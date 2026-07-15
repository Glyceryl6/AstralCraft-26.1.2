package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Opens the client-side battle staging screen. The battle session itself should remain server-authoritative. */
public record OpenBattleScenePayload(
        int attackerId,
        int defenderId,
        boolean localPlayerIsAttacker,
        int availableCost,
        int decisionTicks,
        String availableCards
) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenBattleScenePayload> TYPE = new CustomPacketPayload.Type<>(AstralCraft.prefix("open_battle_scene"));

    public static final StreamCodec<ByteBuf, OpenBattleScenePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            OpenBattleScenePayload::attackerId,
            ByteBufCodecs.VAR_INT,
            OpenBattleScenePayload::defenderId,
            ByteBufCodecs.BOOL,
            OpenBattleScenePayload::localPlayerIsAttacker,
            ByteBufCodecs.VAR_INT,
            OpenBattleScenePayload::availableCost,
            ByteBufCodecs.VAR_INT,
            OpenBattleScenePayload::decisionTicks,
            ByteBufCodecs.STRING_UTF8,
            OpenBattleScenePayload::availableCards,
            OpenBattleScenePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
