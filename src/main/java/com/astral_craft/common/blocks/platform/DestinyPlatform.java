package com.astral_craft.common.blocks.platform;

import com.astral_craft.common.blocks.BasePlatform;
import com.astral_craft.common.components.CardType;
import com.astral_craft.common.entity.character.AstralCharacterEntity;
import com.astral_craft.common.gameplay.board.BoardEntityService;
import com.astral_craft.common.gameplay.board.BoardFortuneService;
import com.astral_craft.common.gameplay.board.BoardPanelContext;
import com.astral_craft.common.gameplay.board.BoardParticipant;
import com.astral_craft.common.gameplay.board.BoardSession;
import com.astral_craft.common.gameplay.board.BoardSessionManager;
import com.astral_craft.common.gameplay.board.BoardSpectatorService;
import com.astral_craft.common.gameplay.cardback.CardBackPreferenceManager;
import com.astral_craft.common.gameplay.fortune.BoardFortuneDefinition;
import com.astral_craft.common.network.s2c.CardRevealPayload;
import com.astral_craft.common.util.AstralServerTickClock;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DestinyPlatform extends BasePlatform {

    private static final int REVEAL_TICKS = 48;
    private static final Map<UUID, PendingDestiny> PENDING = new HashMap<>();

    public DestinyPlatform(Block.Properties properties) {
        super(properties, Trigger.LANDING);
    }

    @Override
    public void applyBoardEffect(BoardPanelContext context) {
        BoardFortuneDefinition definition = BoardFortuneService.randomDefinition(context.level());
        if (definition == null || PENDING.containsKey(context.session().id())) {
            BoardSessionManager.resumeMovementAfterPanel(context.level(), context.session());
            return;
        }
        PENDING.put(context.session().id(), new PendingDestiny(context.participant().slotUuid(), definition,
                AstralServerTickClock.now(context.level()) + REVEAL_TICKS + 2L));
        this.broadcastReveal(context.level(), context.session(), context.participant(), definition);
        this.activateBoardEffect(context.session());
        BoardSessionManager.markChanged(context.level());
    }

    @Override
    protected void tickPendingBoardEffect(ServerLevel level, BoardSession session) {
        PendingDestiny pending = PENDING.get(session.id());
        if (pending != null && AstralServerTickClock.now(level) < pending.applyTick()) return;
        if (pending != null) {
            BoardParticipant source = session.participant(pending.sourceSlot()).orElse(null);
            if (source != null) {
                BoardFortuneService.apply(level, session, source, pending.definition(), List.of(source.slotUuid()));
            }
            PENDING.remove(session.id());
            BoardFortuneService.closePresentation(level, session);
            BoardSessionManager.markChanged(level);
        }
        this.deactivateBoardEffect(session.id());
        BoardSessionManager.resumeMovementAfterPanel(level, session);
    }

    @Override
    protected void discardPendingBoardEffect(UUID boardId) {
        PENDING.remove(boardId);
    }

    private void broadcastReveal(ServerLevel level, BoardSession session, BoardParticipant source,
                                 BoardFortuneDefinition definition) {
        AstralCharacterEntity pawn = BoardEntityService.entity(level, source);
        int sourceEntityId = pawn == null ? -1 : pawn.getId();
        for (ServerPlayer viewer : BoardSpectatorService.presentationViewers(level, session)) {
            PacketDistributor.sendToPlayer(viewer, new CardRevealPayload(definition.id().toString(), ItemStack.EMPTY,
                    CardType.EVENT.getSerializedName(), Component.translatable(definition.nameKey()),
                    Component.translatable(definition.descriptionKey()), definition.texture(),
                    CardBackPreferenceManager.selectedTexture(viewer), CardRevealPayload.ANIMATION_APPROACH,
                    REVEAL_TICKS, sourceEntityId, List.of(sourceEntityId), false));
        }
    }

    private record PendingDestiny(UUID sourceSlot, BoardFortuneDefinition definition, long applyTick) {}
}
