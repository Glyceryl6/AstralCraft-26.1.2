package com.astral_craft.common.gameplay.chip;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.BuffKind;
import com.astral_craft.common.gameplay.handcard.AstralCardEffects;
import com.astral_craft.common.network.s2c.OpenChipSelectionPayload;
import com.astral_craft.common.registry.AstralChips;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.*;

/** Opens and resolves the three-choice chip selection UI. */
public class ChipSelectionService {

    private static final Map<UUID, PlayerChipState> STATES = new HashMap<>();
    private static final Map<UUID, List<Identifier>> PENDING = new HashMap<>();

    public static void open(ServerPlayer player, boolean normalDifficulty, int level) {
        PlayerChipState state = STATES.computeIfAbsent(player.getUUID(), ignored -> new PlayerChipState());
        List<ChipDefinition> choices = rollChoices(player, state, normalDifficulty, level);
        if (choices.isEmpty()) {
            player.sendSystemMessage(Component.translatable("message.astral_craft.chip.none").withStyle(ChatFormatting.GRAY), true);
            return;
        }

        PENDING.put(player.getUUID(), choices.stream().map(chip -> AstralCraft.prefix(chip.id())).toList());
        List<OpenChipSelectionPayload.Choice> views = choices.stream()
                .map(chip -> new OpenChipSelectionPayload.Choice(
                        AstralCraft.prefix(chip.id()),
                        chip.nameKey(),
                        chip.effectKey(),
                        AstralCraft.prefix("textures/gui/chips/" + chip.id() + ".png")))
                .toList();
        PacketDistributor.sendToPlayer(player, new OpenChipSelectionPayload(views));
    }

    public static void choose(ServerPlayer player, Identifier chipId) {
        List<Identifier> offered = PENDING.get(player.getUUID());
        if (offered == null || !offered.contains(chipId)) return;
        PlayerChipState state = STATES.computeIfAbsent(player.getUUID(), ignored -> new PlayerChipState());
        if (state.owned.contains(chipId.getPath())) return;
        AstralChips.get(chipId.getPath()).ifPresent(chip -> {
            AstralCardEffects.applyChip(player, chip);
            state.owned.add(chip.id());
            chip.keyword().ifPresent(keyword -> state.keywordBias.merge(keyword, 1, Integer::sum));
            player.sendSystemMessage(Component.translatable("message.astral_craft.chip.selected", chip.displayName()).withStyle(ChatFormatting.GOLD), true);
        });
        PENDING.remove(player.getUUID());
    }

    private static List<ChipDefinition> rollChoices(ServerPlayer player, PlayerChipState state, boolean normalDifficulty, int level) {
        List<ChipDefinition> list = new ArrayList<>();
        AstralChips.allHolders().forEach(holder -> list.add(holder.get()));
        List<ChipDefinition> pool = list.stream().filter(chip -> !state.owned.contains(chip.id())).toList();
        List<ChipDefinition> choices = new ArrayList<>();
        for (int i = 0; i < 3 && !pool.isEmpty(); i++) {
            ChipRarity rarity = rollRarity(player, normalDifficulty, level);
            ChipDefinition picked = weightedPick(player, pool, state, rarity, choices);
            if (picked == null) picked = weightedPick(player, pool, state, null, choices);
            if (picked == null) break;
            choices.add(picked);
        }

        return choices;
    }

    private static ChipRarity rollRarity(ServerPlayer player, boolean normalDifficulty, int level) {
        int[] weights = AstralChips.rarityWeights(normalDifficulty, level);
        int total = weights[0] + weights[1] + weights[2];
        int roll = player.getRandom().nextInt(Math.max(1, total));
        if (roll < weights[0]) return ChipRarity.BLUE;
        if (roll < weights[0] + weights[1]) return ChipRarity.PURPLE;
        return ChipRarity.GOLD;
    }

    private static ChipDefinition weightedPick(ServerPlayer player, List<ChipDefinition> pool, PlayerChipState state, ChipRarity rarity, List<ChipDefinition> chosen) {
        Set<String> chosenIds = new HashSet<>();
        for (ChipDefinition chip : chosen) chosenIds.add(chip.id());
        List<ChipDefinition> candidates = pool.stream()
                .filter(chip -> rarity == null || chip.rarity() == rarity)
                .filter(chip -> !chosenIds.contains(chip.id())).toList();
        if (candidates.isEmpty()) return null;
        int total = 0;
        for (ChipDefinition chip : candidates) total += weight(state, chip);
        int roll = player.getRandom().nextInt(Math.max(1, total));
        for (ChipDefinition chip : candidates) {
            roll -= weight(state, chip);
            if (roll < 0) return chip;
        }

        return candidates.getLast();
    }

    private static int weight(PlayerChipState state, ChipDefinition chip) {
        return 10 + chip.keyword().map(keyword -> state.keywordBias.getOrDefault(keyword, 0) * 18).orElse(0);
    }

    private static class PlayerChipState {
        private final Set<String> owned = new HashSet<>();
        private final Map<BuffKind, Integer> keywordBias = new HashMap<>();
    }

}