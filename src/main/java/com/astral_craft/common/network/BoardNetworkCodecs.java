package com.astral_craft.common.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;

import java.util.UUID;

/** Shared aliases kept for source compatibility with existing board payloads. */
public class BoardNetworkCodecs {

    public static final StreamCodec<ByteBuf, BlockPos> BLOCK_POS_STREAM_CODEC = BlockPos.STREAM_CODEC;
    public static final StreamCodec<ByteBuf, UUID> UUID_STREAM_CODEC = UUIDUtil.STREAM_CODEC;

}
