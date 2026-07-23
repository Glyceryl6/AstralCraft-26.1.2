package com.astral_craft.common.network.s2c;

import com.astral_craft.AstralCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record BoardAnnouncementPayload(Component title, Component subtitle, int durationTicks,
                                       Identifier sound) implements CustomPacketPayload {

    public static final Type<BoardAnnouncementPayload> TYPE = new Type<>(AstralCraft.prefix("board_announcement"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BoardAnnouncementPayload> STREAM_CODEC = StreamCodec.composite(
            ComponentSerialization.TRUSTED_STREAM_CODEC, BoardAnnouncementPayload::title,
            ComponentSerialization.TRUSTED_STREAM_CODEC, BoardAnnouncementPayload::subtitle,
            ByteBufCodecs.VAR_INT, BoardAnnouncementPayload::durationTicks,
            Identifier.STREAM_CODEC, BoardAnnouncementPayload::sound,
            BoardAnnouncementPayload::new);

    public BoardAnnouncementPayload {
        durationTicks = Math.max(1, durationTicks);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
