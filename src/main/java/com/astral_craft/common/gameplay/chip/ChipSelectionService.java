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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
            player.sendSystemMessage(Component.translatable("message.astral_craft.chip.selected", chip.displayName())
                    .withStyle(ChatFormatting.GOLD), true);
        });
        PENDING.remove(player.getUUID());
    }

    public static List<ChipDefinition> rollBoardChoices(RandomSource random, BoardParticipant participant,
                                                        boolean normalDifficulty, @Nullable Identifier mapId) {
        return rollChoices(random, participant.characterId(), participant.chipProgress(), normalDifficulty,
                participant.stats().stars(), mapId);
    }

    public static BoardParticipant applyBoardChoice(BoardParticipant participant, List<Identifier> offered,
                                                     Identifier chipId, boolean shopPurchase) {
        if (participant == null || chipId == null || offered == null || !offered.contains(chipId)
                || participant.chipProgress().owns(chipId)) return participant;
        ChipDefinition chip = AstralChips.get(chipId).orElse(null);
        if (chip == null) return participant;
        BoardParticipant.ChipProgress progress = participant.chipProgress().withPreviousOffers(offered)
                .acquire(chipId, chip.keywordId().orElse(null));
        if (shopPurchase) progress = progress.completeShopPurchase();
        return chip.applyToBoard(participant).withChipProgress(progress);
    }

    public static List<OpenChipSelectionPayload.Choice> toViews(List<ChipDefinition> choices) {
        return choices.stream().map(chip -> new OpenChipSelectionPayload.Choice(
                chip.registryId(), chip.nameKey(), chip.effectKey(),
                AstralCraft.prefix("textures/gui/chips/" + chip.id().getPath() + ".png"))).toList();
    }

    public static BoardParticipant beforeTurnStart(ServerLevel level, BoardParticipant participant) {
        return applyOwned(level, participant, LifecyclePhase.BEFORE_TURN_START);
    }

    public static BoardParticipant afterEffectCardPlayed(ServerLevel level, BoardParticipant participant) {
        return applyOwned(level, participant, LifecyclePhase.AFTER_EFFECT_CARD_PLAYED);
    }

    public static BoardParticipant afterTurnEnd(ServerLevel level, BoardParticipant participant) {
        return applyOwned(level, participant, LifecyclePhase.AFTER_TURN_END);
    }

    public static int skillCooldownReduction(BoardParticipant participant) {
        if (participant == null) return 0;
        int reduction = 0;
        for (Identifier chipId : participant.chipProgress().owned()) {
            ChipDefinition chip = AstralChips.get(chipId).orElse(null);
            if (chip != null) reduction += chip.skillCooldownReduction();
        }
        return reduction;
    }

    private static BoardParticipant applyOwned(ServerLevel level, BoardParticipant participant, LifecyclePhase phase) {
        if (level == null || participant == null) return participant;
        BoardParticipant updated = participant;
        for (Identifier chipId : participant.chipProgress().owned()) {
            ChipDefinition chip = AstralChips.get(chipId).orElse(null);
            if (chip == null) continue;
            updated = switch (phase) {
                case BEFORE_TURN_START -> chip.beforeTurnStart(level, updated);
                case AFTER_EFFECT_CARD_PLAYED -> chip.afterEffectCardPlayed(level, updated);
                case AFTER_TURN_END -> chip.afterTurnEnd(level, updated);
            };
        }
        return updated;
    }

    private static List<ChipDefinition> rollChoices(RandomSource random, Identifier characterId,
                                                    BoardParticipant.ChipProgress progress, boolean normalDifficulty,
                                                    int level, @Nullable Identifier mapId) {
        List<ChipDefinition> pool = new ArrayList<>();
        for (ChipDefinition chip : AstralChips.values()) {
            if (!progress.owns(chip.registryId()) && chip.availableOn(mapId) && keywordAllowed(progress, chip)) {
                pool.add(chip);
            }
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

    private static @Nullable ChipDefinition weightedPick(RandomSource random, List<ChipDefinition> pool,
                                                          BoardParticipant.ChipProgress progress, Identifier characterId,
                                                          @Nullable ChipRarity rarity, List<ChipDefinition> chosen,
                                                          boolean classWeighted, boolean excludePrevious) {
        Set<Identifier> chosenIds = chosen.stream().map(ChipDefinition::registryId).collect(Collectors.toSet());
        List<ChipDefinition> candidates = pool.stream()
                .filter(chip -> rarity == null || chip.rarity() == rarity)
                .filter(chip -> !chosenIds.contains(chip.registryId()))
                .filter(chip -> !excludePrevious || !progress.previousOffers().contains(chip.registryId())).toList();
        if (candidates.isEmpty()) return null;
        int total = candidates.stream().mapToInt(chip -> weight(progress, characterId, chip, classWeighted)).sum();
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
        ChipPool.Weights weights = CharacterManager.INSTANCE.character(characterId).chipWeights();
        int value = classWeighted ? weights.weight(chip.pool()) : ChipPool.Weights.BASE_WEIGHT;
        if (chip.keywordId().filter(progress.keywordIds()::contains).isPresent()) value += KEYWORD_WEIGHT_BONUS;
        return Math.max(1, value);
    }

    private static boolean keywordAllowed(BoardParticipant.ChipProgress progress, ChipDefinition chip) {
        Optional<Identifier> keyword = chip.keywordId();
        return keyword.isEmpty() || progress.keywordIds().size() < 2 || progress.keywordIds().contains(keyword.get());
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

    private enum LifecyclePhase {
        BEFORE_TURN_START,
        AFTER_EFFECT_CARD_PLAYED,
        AFTER_TURN_END
    }
}
