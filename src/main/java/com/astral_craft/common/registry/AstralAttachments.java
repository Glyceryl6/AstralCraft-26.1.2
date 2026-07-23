package com.astral_craft.common.registry;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.character.ActiveCharacterState;
import com.astral_craft.common.gameplay.character.CharacterProgress;
import com.astral_craft.common.gameplay.character.skill.CharacterSkillState;
import com.astral_craft.common.gameplay.event.AstralEventPreferences;
import com.astral_craft.common.gameplay.event.AstralEventState;
import com.astral_craft.common.gameplay.handcard.AstralHandCards;
import com.astral_craft.common.stats.AstralPlayerStats;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class AstralAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, AstralCraft.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<AstralPlayerStats>> PLAYER_STATS = ATTACHMENTS.register("player_stats",
            () -> AttachmentType.builder(() -> AstralPlayerStats.DEFAULT).serialize(AstralPlayerStats.CODEC.fieldOf("stats")).copyOnDeath().build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<CharacterProgress>> CHARACTER_PROGRESS = ATTACHMENTS.register("character_progress",
            () -> AttachmentType.builder(() -> new CharacterProgress(AstralCraft.prefix("mimi"))).serialize(CharacterProgress.CODEC.fieldOf("progress")).copyOnDeath().build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ActiveCharacterState>> ACTIVE_CHARACTER = ATTACHMENTS.register("active_character",
            () -> AttachmentType.builder(() -> ActiveCharacterState.NONE).serialize(ActiveCharacterState.CODEC.fieldOf("active_character")).sync(ActiveCharacterState.STREAM_CODEC).copyOnDeath().build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<AstralHandCards>> HAND_CARDS = ATTACHMENTS.register("hand_cards",
            () -> AttachmentType.builder(AstralHandCards::empty).serialize(AstralHandCards.CODEC.fieldOf("hand_cards")).sync(AstralHandCards.STREAM_CODEC).copyOnDeath().build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<CharacterSkillState>> CHARACTER_SKILLS = ATTACHMENTS.register("character_skills",
            () -> AttachmentType.builder(CharacterSkillState::empty).serialize(CharacterSkillState.CODEC.fieldOf("character_skills")).sync(CharacterSkillState.STREAM_CODEC).copyOnDeath().build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<AstralEventState>> EVENT_STATE = ATTACHMENTS.register("event_state",
            () -> AttachmentType.builder(AstralEventState::empty).serialize(AstralEventState.CODEC.fieldOf("event_state")).copyOnDeath().build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<AstralEventPreferences>> EVENT_PREFERENCES = ATTACHMENTS.register("event_preferences",
            () -> AttachmentType.builder(() -> AstralEventPreferences.DEFAULT).serialize(AstralEventPreferences.CODEC.fieldOf("event_preferences")).copyOnDeath().build());

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<Identifier>> CARD_BACK = ATTACHMENTS.register("card_back",
            () -> AttachmentType.builder(() -> AstralCraft.prefix("default")).serialize(Identifier.CODEC.fieldOf("card_back")).copyOnDeath().build());

}