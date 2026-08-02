package com.astral_craft.common.gameplay.chip;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.blocks.BasePlatform;
import com.astral_craft.common.gameplay.board.BoardParticipant;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import com.astral_craft.common.gameplay.character.CharacterManager;
import com.astral_craft.common.gameplay.handcard.AstralCardEffects;
import com.astral_craft.common.network.s2c.OpenChipSelectionPayload;
import com.astral_craft.common.registry.AstralChips;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.*;

/** Generates and resolves the three-option chip selection used by standalone rewards and board PVE. */
public class ChipSelectionService {

    private static final int KEYWORD_WEIGHT_BONUS = 30;
    private static final Map<UUID, PlayerChipState> STATES = new HashMap<>();
    private static final Map<UUID, List<Identifier>> PENDING = new HashMap<>();

    public static void open(ServerPlayer player, boolean normalDifficulty, int level) {
        PlayerChipState state = STATES.computeIfAbsent(player.getUUID(), ignored -> new PlayerChipState());
        List<ChipDefinition> choices = rollChoices(player.getRandom(), AstralCraft.prefix("mimi"), state.progress(),
                normalDifficulty, level, null);
        if (choices.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.chip.none").withStyle(ChatFormatting.GRAY), true);
            return;
        }

        List<Identifier> offered = choices.stream().map(ChipDefinition::registryId).toList();
        state.previousOffers = offered;
        PENDING.put(player.getUUID(), offered);
        PacketDistributor.sendToPlayer(player, new OpenChipSelectionPayload(toViews(choices)));
    }

    public static void choose(ServerPlayer player, UUID boardId, Identifier chipId) {
        if (!OpenChipSelectionPayload.NO_BOARD.equals(boardId)) {
            BoardSessionManager.session(player.level(), boardId).ifPresent(session -> BasePlatform.activeBoardEffect(session.id())
                    .ifPresent(platform -> platform.handleBoardChipSelection(player, session, chipId)));
            return;
        }

        List<Identifier> offered = PENDING.get(player.getUUID());
        if (offered == null || !offered.contains(chipId)) return;
        PlayerChipState state = STATES.computeIfAbsent(player.getUUID(), ignored -> new PlayerChipState());
        if (state.owned.contains(chipId)) return;
        AstralChips.get(chipId).ifPresent(chip -> {
            AstralCardEffects.applyChip(player, chip);
            state.owned.add(chipId);
            chip.keywordId().ifPresent(state.keywordIds::add);
            player.sendSystemMessage(Component.translatable("message.astral_craft.chip.selected", chip.displayName()).withStyle(ChatFormatting.GOLD), true);
        });
        PENDING.remove(player.getUUID());
    }

    public static List<ChipDefinition> rollBoardChoices(RandomSource random, BoardParticipant participant,
                                                        boolean normalDifficulty, @Nullable Identifier mapId) {
        return rollChoices(random, participant.characterId(), participant.chipProgress(), normalDifficulty,
                participant.stats().stars(), mapId);
    }

    public static BoardParticipant applyBoardChoice(BoardParticipant participant, List<Identifier> offered, Identifier chipId, boolean shopPurchase) {
        if (participant == null || chipId == null || offered == null || !offered.contains(chipId)
                || participant.chipProgress().owns(chipId)) return participant;
        ChipDefinition chip = AstralChips.get(chipId).orElse(null);
        if (chip == null) return participant;
        BoardParticipant.ChipProgress progress = participant.chipProgress().withPreviousOffers(offered)
                .acquire(chipId, chip.keywordId().orElse(null));
        if (shopPurchase) progress = progress.completeShopPurchase();
        return participant.withStats(participant.stats().applyChip(chip.stats())).withChipProgress(progress);
    }

    public static List<OpenChipSelectionPayload.Choice> toViews(List<ChipDefinition> choices) {
        return choices.stream().map(chip -> new OpenChipSelectionPayload.Choice(
                chip.registryId(), chip.nameKey(), chip.effectKey(),
                AstralCraft.prefix("textures/gui/chips/" + chip.id() + ".png"))).toList();
    }

    public static BoardParticipant beforeTurnStart(ServerLevel level, BoardParticipant participant) {
        return applyOwnedEffects(level, participant, ChipDefinition::beforeTurnStart);
    }

    public static BoardParticipant afterEffectCardPlayed(ServerLevel level, BoardParticipant participant) {
        return applyOwnedEffects(level, participant, ChipDefinition::afterEffectCardPlayed);
    }

    public static BoardParticipant afterTurnEnd(ServerLevel level, BoardParticipant participant) {
        return applyOwnedEffects(level, participant, ChipDefinition::afterTurnEnd);
    }

    public static int skillCooldownReduction(BoardParticipant participant) {
        if (participant == null) return 0;
        int reduction = 0;
        for (Identifier chipId : participant.chipProgress().owned()) {
            ChipDefinition chip = AstralChips.get(chipId).orElse(null);
            if (chip != null) reduction += Math.max(0, chip.stats().skillCooldownReduction());
        }
        return reduction;
    }

    private static BoardParticipant applyOwnedEffects(ServerLevel level, BoardParticipant participant, ChipEffect effect) {
        if (level == null || participant == null) return participant;
        BoardParticipant result = participant;
        for (Identifier chipId : participant.chipProgress().owned()) {
            ChipDefinition chip = AstralChips.get(chipId).orElse(null);
            if (chip != null) result = Objects.requireNonNullElse(effect.apply(chip, level, result), result);
        }
        return result;
    }

    private static List<ChipDefinition> rollChoices(RandomSource random, Identifier characterId,
                                                    BoardParticipant.ChipProgress progress, boolean normalDifficulty,
                                                    int level, @Nullable Identifier mapId) {
        List<ChipDefinition> pool = new ArrayList<>();
        for (ChipDefinition chip : AstralChips.values()) {
            if (!progress.owns(chip.registryId()) && chip.availableOn(mapId) && keywordAllowed(progress, chip)) pool.add(chip);
        }
        if (pool.isEmpty()) return List.of();

        List<ChipDefinition> choices = new ArrayList<>(OpenChipSelectionPayload.MAXIMUM_CHOICES);
        ChipRarity fixedRarity = level >= 3 ? ChipRarity.GOLD : level == 2 ? ChipRarity.PURPLE : ChipRarity.BLUE;
        ChipDefinition first = weightedPick(random, pool, progress, characterId, fixedRarity, choices, true, true);
        if (first == null) first = weightedPick(random, pool, progress, characterId, null, choices, true, true);
        if (first != null) choices.add(first);

        while (choices.size() < OpenChipSelectionPayload.MAXIMUM_CHOICES && choices.size() < pool.size()) {
            ChipRarity rarity = rollRarity(random, normalDifficulty, level);
            ChipDefinition picked = weightedPick(random, pool, progress, characterId, rarity, choices, false, true);
            if (picked == null) picked = weightedPick(random, pool, progress, characterId, rarity, choices, false, false);
            if (picked == null) picked = weightedPick(random, pool, progress, characterId, null, choices, false, true);
            if (picked == null) picked = weightedPick(random, pool, progress, characterId, null, choices, false, false);
            if (picked == null) break;
            choices.add(picked);
        }

        return List.copyOf(choices);
    }

    private static ChipDefinition weightedPick(RandomSource random, List<ChipDefinition> pool,
                                                BoardParticipant.ChipProgress progress, Identifier characterId,
                                                @Nullable ChipRarity rarity, List<ChipDefinition> chosen,
                                                boolean classWeighted, boolean excludePrevious) {
        Set<Identifier> chosenIds = new HashSet<>();
        for (ChipDefinition chip : chosen) chosenIds.add(chip.registryId());
        List<ChipDefinition> candidates = new ArrayList<>();
        for (ChipDefinition chip : pool) {
            if ((rarity == null || chip.rarity() == rarity) && !chosenIds.contains(chip.registryId())
                    && (!excludePrevious || !progress.previousOffers().contains(chip.registryId()))) candidates.add(chip);
        }
        if (candidates.isEmpty()) return null;

        int total = 0;
        for (ChipDefinition chip : candidates) total += weight(progress, characterId, chip, classWeighted);
        int roll = random.nextInt(Math.max(1, total));
        for (ChipDefinition chip : candidates) {
            roll -= weight(progress, characterId, chip, classWeighted);
            if (roll < 0) return chip;
        }
        return candidates.getLast();
    }

    private static ChipRarity rollRarity(RandomSource random, boolean normalDifficulty, int level) {
        int[] weights = AstralChips.rarityWeights(normalDifficulty, level);
        int roll = random.nextInt(Math.max(1, weights[0] + weights[1] + weights[2]));
        if (roll < weights[0]) return ChipRarity.BLUE;
        if (roll < weights[0] + weights[1]) return ChipRarity.PURPLE;
        return ChipRarity.GOLD;
    }

    private static int weight(BoardParticipant.ChipProgress progress, Identifier characterId,
                              ChipDefinition chip, boolean classWeighted) {
        int weight = classWeighted ? CharacterManager.INSTANCE.character(characterId).chipWeight(chip.pool())
                : ChipPool.Weights.DEFAULT_WEIGHT;
        if (chip.keywordId().filter(progress.keywordIds()::contains).isPresent()) weight += KEYWORD_WEIGHT_BONUS;
        return Math.max(1, weight);
    }

    private static boolean keywordAllowed(BoardParticipant.ChipProgress progress, ChipDefinition chip) {
        Identifier keywordId = chip.keywordId().orElse(null);
        return keywordId == null || progress.keywordIds().size() < 2 || progress.keywordIds().contains(keywordId);
    }

    private static class PlayerChipState {
        private final Set<Identifier> owned = new LinkedHashSet<>();
        private final Set<Identifier> keywordIds = new LinkedHashSet<>();
        private List<Identifier> previousOffers = List.of();

        private BoardParticipant.ChipProgress progress() {
            return new BoardParticipant.ChipProgress(List.copyOf(this.owned), this.previousOffers,
                    List.copyOf(this.keywordIds), 0, 0);
        }
    }

    @FunctionalInterface
    private interface ChipEffect {
        BoardParticipant apply(ChipDefinition chip, ServerLevel level, BoardParticipant participant);
    }

}