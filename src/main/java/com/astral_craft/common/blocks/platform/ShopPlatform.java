package com.astral_craft.common.blocks.platform;

import com.astral_craft.common.util.AstralServerTickClock;
import com.astral_craft.common.blocks.BasePlatform;
import com.astral_craft.common.gameplay.board.BoardPanelContext;
import com.astral_craft.common.gameplay.board.BoardEventService;
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

import java.util.*;

public class ShopPlatform extends BasePlatform {

    public static final int TIMEOUT_TICKS = 20 * 25;
    public static final int CARD_PRICE = 3;
    private static final int DEFAULT_OFFER_COUNT = 3;
    private final Map<UUID, ShopState> states = new HashMap<>();

    public ShopPlatform(Block.Properties properties) {
        super(properties, Trigger.BOTH);
    }

    @Override
    public void applyBoardEffect(BoardPanelContext context) {
        this.open(context);
    }

    protected int offerCount(BoardPanelContext context) {
        return DEFAULT_OFFER_COUNT + BoardEventService.shopOfferBonus(context.session());
    }

    public static void handleAction(ServerPlayer player, UUID boardId, List<Integer> offerIndexes, boolean leave) {
        BoardSessionManager.session(player.level(), boardId).ifPresent(session -> handleAction(player, session, offerIndexes, leave));
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

        if (AstralServerTickClock.now(level) < state.deadlineTick()) return;
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

    private void handleActionInternal(ServerPlayer player, BoardSession session, List<Integer> offerIndexes, boolean leave) {
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

    private void open(BoardPanelContext context) {
        ServerLevel level = context.level();
        BoardSession session = context.session();
        BoardParticipant participant = context.participant();
        if (this.states.containsKey(session.id())) return;
        int offerCount = Math.clamp(this.offerCount(context), 0, OpenBoardShopPayload.MAXIMUM_ENCODED_OFFERS);
        List<Identifier> offers = this.randomOffers(level, offerCount);
        if (offers.isEmpty()) return;
        if (BoardSessionManager.isAutomated(level, participant)) {
            if (participant.hand().size() > participant.maxHandSize()) return;
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
                AstralServerTickClock.now(level) + duration, duration);
        this.states.put(session.id(), state);
        this.activateBoardEffect(session);
        participant.controllerUuid().map(level.getServer().getPlayerList()::getPlayer)
                .ifPresent(player -> this.send(player, participant, state, 0));
    }

    private void send(ServerPlayer player, BoardParticipant participant, ShopState state, int noticeCode) {
        int remaining = (int) Math.max(0L, state.deadlineTick() - AstralServerTickClock.now(player.level()));
        PacketDistributor.sendToPlayer(player, new OpenBoardShopPayload(state.boardId(),
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
        return List.copyOf(candidates.subList(0, Math.min(Math.max(0, count), candidates.size())));
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