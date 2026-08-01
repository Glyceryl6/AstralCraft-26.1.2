package com.astral_craft.common.gameplay.chip;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.blocks.BasePlatform;
import com.astral_craft.common.gameplay.board.BoardParticipant;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import com.astral_craft.common.gameplay.handcard.AstralCardEffects;
import com.astral_craft.common.network.s2c.OpenChipSelectionPayload;
import com.astral_craft.common.registry.AstralBoardBuffs;
import com.astral_craft.common.registry.AstralChips;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;
import java.util.stream.Collectors;

/** Generates and resolves the three-option chip selection used by standalone rewards and board PVE. */
public class ChipSelectionService {

    private static final int BASE_WEIGHT = 15;
    private static final int KEYWORD_WEIGHT_BONUS = 30;
    private static final Map<UUID, PlayerChipState> STATES = new HashMap<>();
    private static final Map<UUID, List<Identifier>> PENDING = new HashMap<>();
    private static final Map<String, PoolWeights> CHARACTER_POOL_WEIGHTS = Map.ofEntries(
            character("mimi", 80, 15, 15, 30),
            character("parunan", 60, 15, 15, 60),
            character("fanny", 100, 15, 15, 15),
            character("alana", 15, 15, 100, 15),
            character("komachi", 15, 15, 15, 100),
            character("padman", 15, 15, 100, 15),
            character("papara", 15, 15, 100, 15),
            character("ren", 60, 15, 15, 60),
            character("z3000", 15, 15, 100, 30),
            character("pandaman", 60, 60, 15, 15),
            character("lulu", 15, 100, 15, 15),
            character("fen", 15, 15, 100, 15),
            character("hai_qing", 100, 15, 15, 15),
            character("misaki", 15, 15, 100, 15),
            character("nardis", 15, 15, 60, 60),
            character("jasmine", 15, 15, 100, 15),
            character("luka", 15, 15, 100, 15),
            character("nancy_lu", 15, 15, 100, 15),
            character("megas", 60, 15, 15, 60),
            character("zhao", 15, 100, 15, 15),
            character("al", 80, 15, 15, 30),
            character("rin", 30, 15, 15, 80),
            character("teru", 30, 15, 80, 15),
            character("moses", 15, 15, 100, 15),
            character("mamushi", 15, 15, 100, 15),
            character("bonnie", 15, 15, 100, 15),
            character("k_angel", 100, 15, 15, 15),
            character("ame", 15, 15, 100, 15),
            character("jill", 80, 30, 15, 15),
            character("dorothy", 15, 60, 60, 15));

    public static void open(ServerPlayer player, boolean normalDifficulty, int level) {
        PlayerChipState state = STATES.computeIfAbsent(player.getUUID(), ignored -> new PlayerChipState());
        List<ChipDefinition> choices = rollChoices(player.getRandom(), AstralCraft.prefix("mimi"), state.progress(),
                normalDifficulty, level, Optional.empty());
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
                                                        boolean normalDifficulty, Optional<Identifier> mapId) {
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
                .acquire(chipId, chip.keywordId());
        if (shopPurchase) progress = progress.completeShopPurchase();
        return participant.withStats(participant.stats().applyChip(chip.stats())).withChipProgress(progress);
    }

    public static List<OpenChipSelectionPayload.Choice> toViews(List<ChipDefinition> choices) {
        return choices.stream().map(chip -> new OpenChipSelectionPayload.Choice(
                chip.registryId(), chip.nameKey(), chip.effectKey(),
                AstralCraft.prefix("textures/gui/chips/" + chip.id() + ".png"))).toList();
    }

    public static BoardParticipant beforeTurnStart(BoardParticipant participant) {
        if (participant == null) return null;
        int healStacks = 0;
        if (participant.chipProgress().owns(AstralBuiltinChips.MEDICAL_KIT_EMERGENCY.registryId())) healStacks++;
        if (participant.chipProgress().owns(AstralBuiltinChips.MEDICAL_KIT_FULL.registryId())) healStacks += 3;
        return healStacks <= 0 ? participant : participant.withStats(
                participant.stats().addPermanentBuff(AstralBoardBuffs.HEAL.get(), healStacks));
    }

    public static BoardParticipant afterEffectCardPlayed(BoardParticipant participant) {
        if (participant == null || !participant.chipProgress().owns(AstralBuiltinChips.PIGGY_BANK.registryId())) return participant;
        int played = participant.chipProgress().effectCardsPlayed() + 1;
        boolean reward = played >= 2;
        BoardParticipant.ChipProgress progress = participant.chipProgress().withEffectCardsPlayed(reward ? 0 : played);
        BoardParticipant updated = participant.withChipProgress(progress);
        return reward ? updated.withStats(updated.stats().addCoins(3)) : updated;
    }

    public static BoardParticipant afterTurnEnd(ServerLevel level, BoardParticipant participant) {
        if (level == null || participant == null || participant.hand().size() >= 5
                || !participant.chipProgress().owns(AstralBuiltinChips.SMARTWATCH.registryId())) return participant;
        return BoardSessionManager.randomPvpCardId(level).map(participant::addCard).orElse(participant);
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

    private static List<ChipDefinition> rollChoices(RandomSource random, Identifier characterId,
                                                    BoardParticipant.ChipProgress progress, boolean normalDifficulty,
                                                    int level, Optional<Identifier> mapId) {
        List<ChipDefinition> pool = List.of();
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
                                                ChipRarity rarity, List<ChipDefinition> chosen,
                                                boolean classWeighted, boolean excludePrevious) {
        Set<Identifier> chosenIds = chosen.stream().map(ChipDefinition::registryId)
                .collect(Collectors.toSet());
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
        int weight = classWeighted ? poolWeights(characterId).weight(chip.pool()) : BASE_WEIGHT;
        if (chip.keywordId().filter(progress.keywordIds()::contains).isPresent()) weight += KEYWORD_WEIGHT_BONUS;
        return Math.max(1, weight);
    }

    private static boolean keywordAllowed(BoardParticipant.ChipProgress progress, ChipDefinition chip) {
        Optional<Identifier> keyword = chip.keywordId();
        return keyword.isEmpty() || progress.keywordIds().size() < 2 || progress.keywordIds().contains(keyword.get());
    }

    private static PoolWeights poolWeights(Identifier characterId) {
        String key = characterId == null ? "" : characterId.getPath().replace('-', '_').toLowerCase(Locale.ROOT);
        return CHARACTER_POOL_WEIGHTS.getOrDefault(key, PoolWeights.DEFAULT);
    }

    private static Map.Entry<String, PoolWeights> character(String id, int support, int sustain, int attack, int cards) {
        return Map.entry(id, new PoolWeights(support, sustain, attack, cards));
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

    private record PoolWeights(int support, int sustain, int attack, int cards) {
        private static final PoolWeights DEFAULT = new PoolWeights(BASE_WEIGHT, BASE_WEIGHT, BASE_WEIGHT, BASE_WEIGHT);

        private int weight(ChipPool pool) {
            return switch (pool) {
                case SUPPORT -> this.support;
                case SUSTAIN -> this.sustain;
                case ATTACK -> this.attack;
                case CARDS -> this.cards;
                case GENERAL -> BASE_WEIGHT;
            };
        }
    }

}