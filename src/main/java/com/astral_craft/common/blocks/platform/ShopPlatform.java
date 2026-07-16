package com.astral_craft.common.blocks.platform;

import com.astral_craft.common.blocks.BasePlatform;
import com.astral_craft.common.gameplay.board.BoardPanelContext;
import com.astral_craft.common.gameplay.board.BoardParticipant;
import com.astral_craft.common.gameplay.board.BoardSession;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import com.astral_craft.common.items.BaseHandCard;
import com.astral_craft.common.network.s2c.OpenBoardShopPayload;
import com.astral_craft.common.registry.AstralItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class ShopPlatform extends BasePlatform {

    public static final int TIMEOUT_TICKS = 20 * 25;
    public static final int CARD_PRICE = 3;
    private final Map<UUID, ShopState> states = new HashMap<>();

    public ShopPlatform(Block.Properties properties) {
        super(properties, Trigger.BOTH);
    }

    @Override
    public void applyBoardEffect(BoardPanelContext context) {
        this.open(context.level(), context.session(), context.participant());
    }

    public static void handleAction(ServerPlayer player, String rawBoardId, List<Integer> offerIndexes, boolean leave) {
        try {
            BoardSessionManager.session(player.level(), UUID.fromString(rawBoardId))
                    .ifPresent(session -> handleAction(player, session, offerIndexes, leave));
        } catch (IllegalArgumentException ignored) {}
    }

    private static void handleAction(ServerPlayer player, BoardSession session, List<Integer> offerIndexes, boolean leave) {
        BasePlatform.activeBoardEffect(session.id()).filter(ShopPlatform.class::isInstance).map(ShopPlatform.class::cast)
                .ifPresent(platform -> platform.handleActionInternal(player, session, offerIndexes, leave));
    }

    @Override
    protected void tickPendingBoardEffect(ServerLevel level, BoardSession session) {
        ShopState state = this.states.get(session.id());
        if (state == null) {
            this.deactivateBoardEffect(session.id());
            return;
        }

        if (level.getGameTime() < state.deadlineTick()) return;
        BoardParticipant participant = session.participant(state.slotId()).orElse(null);
        if (participant != null && !BoardSessionManager.isAutomated(level, participant)) {
            BoardSessionManager.updateParticipant(level, session, participant.recordTimedOutDecision());
        }

        this.close(level, session);
    }

    @Override
    protected void pendingParticipantBecameAutomated(ServerLevel level, BoardSession session, UUID slotId) {
        ShopState state = this.states.get(session.id());
        if (state != null && state.slotId().equals(slotId)) this.close(level, session);
    }

    @Override
    protected void discardPendingBoardEffect(UUID boardId) {
        this.states.remove(boardId);
    }

    private void handleActionInternal(ServerPlayer player, BoardSession session, @Nullable List<Integer> offerIndexes, boolean leave) {
        ShopState state = this.states.get(session.id());
        if (state == null) return;
        BoardParticipant participant = session.participant(state.slotId()).orElse(null);
        if (participant == null || !participant.controlledBy(player.getUUID())) return;
        BoardParticipant manual = participant.recordManualDecision();
        if (manual != participant) {
            BoardSessionManager.updateParticipant(player.level(), session, manual);
            participant = manual;
        }

        if (leave) {
            this.close(player.level(), session);
            return;
        }

        Set<Integer> selected = new LinkedHashSet<>(offerIndexes == null ? List.of() : offerIndexes);
        if (selected.isEmpty() || selected.size() > state.offers().size()
                || selected.stream().anyMatch(index -> index < 0 || index >= state.offers().size()
                || state.purchased(index))) {
            this.send(player, participant, state, 2);
            return;
        }

        int cost = selected.size() * CARD_PRICE;
        if (participant.stats().starCoins() < cost) {
            this.send(player, participant, state, 1);
            return;
        }

        BoardParticipant updated = participant.withStats(participant.stats().spendCoins(cost));
        int purchasedMask = state.purchasedMask();
        for (int index : selected) {
            updated = updated.addCard(state.offers().get(index));
            purchasedMask |= 1 << index;
        }

        BoardSessionManager.updateParticipant(player.level(), session, updated);
        ShopState next = state.withPurchasedMask(purchasedMask);
        this.states.put(session.id(), next);
        this.send(player, updated, next, 3);
    }

    private void open(ServerLevel level, BoardSession session, BoardParticipant participant) {
        if (this.states.containsKey(session.id())) return;
        List<Identifier> offers = this.randomOffers(level, OpenBoardShopPayload.MAXIMUM_OFFERS);
        if (offers.isEmpty()) return;
        if (BoardSessionManager.isAutomated(level, participant)) {
            int purchaseCount = Math.min(offers.size(), participant.stats().starCoins() / CARD_PRICE);
            BoardParticipant updated = participant;
            for (int index = 0; index < purchaseCount; index++) {
                updated = updated.addCard(offers.get(index));
            }
            if (purchaseCount > 0) {
                updated = updated.withStats(updated.stats().spendCoins(purchaseCount * CARD_PRICE));
                BoardSessionManager.updateParticipant(level, session, updated);
            }
            return;
        }

        int duration = participant.decisionDurationTicks(TIMEOUT_TICKS);
        ShopState state = new ShopState(session.id(), participant.slotUuid(), offers, 0,
                level.getGameTime() + duration, duration);
        this.states.put(session.id(), state);
        this.activateBoardEffect(session);
        participant.controllerUuid().map(level.getServer().getPlayerList()::getPlayer)
                .ifPresent(player -> this.send(player, participant, state, 0));
    }

    private void send(ServerPlayer player, BoardParticipant participant, ShopState state, int noticeCode) {
        int remaining = (int) Math.max(0L, state.deadlineTick() - player.level().getGameTime());
        PacketDistributor.sendToPlayer(player, new OpenBoardShopPayload(state.boardId().toString(),
                state.offers(), state.purchasedMask(), participant.stats().starCoins(), CARD_PRICE,
                remaining, state.durationTicks(), participant.characterId(), participant.skinId(), noticeCode));
    }

    private List<Identifier> randomOffers(ServerLevel level, int count) {
        List<Identifier> candidates = new ArrayList<>();
        for (AstralItems.ModelledCardItem entry : AstralItems.MODELLED_CARD_ITEMS) {
            Item item = entry.item().get();
            if (!(item instanceof BaseHandCard card) || !BoardSessionManager.validPvpCard(card)) continue;
            Package itemPackage = item.getClass().getPackage();
            if (itemPackage != null && itemPackage.getName().contains(".cards.pve")) continue;
            Identifier id = BuiltInRegistries.ITEM.getKey(item);
            if (!candidates.contains(id)) candidates.add(id);
        }

        Collections.shuffle(candidates, new Random(level.getRandom().nextLong()));
        return List.copyOf(candidates.subList(0, Math.clamp(count, 0, candidates.size())));
    }

    private void close(ServerLevel level, BoardSession session) {
        this.states.remove(session.id());
        this.deactivateBoardEffect(session.id());
        BoardSessionManager.resumeMovementAfterPanel(level, session);
    }

    private record ShopState(UUID boardId, UUID slotId, List<Identifier> offers, int purchasedMask, long deadlineTick, int durationTicks) {

        private ShopState {
            offers = List.copyOf(offers);
            purchasedMask = Math.max(0, purchasedMask);
            durationTicks = Math.max(1, durationTicks);
        }

        private boolean purchased(int index) {
            return (this.purchasedMask & 1 << index) != 0;
        }

        private ShopState withPurchasedMask(int purchasedMask) {
            return new ShopState(this.boardId, this.slotId, this.offers, purchasedMask,
                    this.deadlineTick, this.durationTicks);
        }
    }

}