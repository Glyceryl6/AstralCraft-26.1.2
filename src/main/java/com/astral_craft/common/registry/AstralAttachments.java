package com.astral_craft.common.registry;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.stats.AstralPlayerStats;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class AstralAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, AstralCraft.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<AstralPlayerStats>> PLAYER_STATS = ATTACHMENTS.register("player_stats",
            () -> AttachmentType.builder(() -> AstralPlayerStats.DEFAULT).serialize(AstralPlayerStats.CODEC.fieldOf("stats")).copyOnDeath().build());

}