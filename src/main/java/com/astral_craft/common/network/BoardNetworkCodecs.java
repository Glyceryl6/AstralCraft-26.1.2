package com.astral_craft.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

public class BoardNetworkCodecs {

    public static final StreamCodec<ByteBuf, BlockPos> BLOCK_POS_STREAM_CODEC = new StreamCodec<>() {
        @Override
        public BlockPos decode(ByteBuf buffer) {
            return new BlockPos(buffer.readInt(), buffer.readInt(), buffer.readInt());
        }

        @Override
        public void encode(ByteBuf buffer, BlockPos value) {
            buffer.writeInt(value.getX());
            buffer.writeInt(value.getY());
            buffer.writeInt(value.getZ());
        }
    };

    public static final StreamCodec<ByteBuf, UUID> UUID_STREAM_CODEC = new StreamCodec<>() {
        @Override
        public UUID decode(ByteBuf buffer) {
            return new UUID(buffer.readLong(), buffer.readLong());
        }

        @Override
        public void encode(ByteBuf buffer, UUID value) {
            buffer.writeLong(value.getMostSignificantBits());
            buffer.writeLong(value.getLeastSignificantBits());
        }
    };

}