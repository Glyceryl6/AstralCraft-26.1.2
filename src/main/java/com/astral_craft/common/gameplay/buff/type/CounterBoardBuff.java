package com.astral_craft.common.gameplay.buff.type;

import com.astral_craft.common.gameplay.board.BoardParticipant;
import com.astral_craft.common.gameplay.board.BoardSession;
import com.astral_craft.common.gameplay.buff.BoardBuff;
import com.astral_craft.common.gameplay.buff.BoardBuffInstance;
import com.astral_craft.common.stats.AstralPlayerStats;
import net.minecraft.server.level.ServerLevel;

public class CounterBoardBuff extends BoardBuff {

    public CounterBoardBuff(int color) {
        super(Properties.of(color).stacking().permanent());
    }

    @Override
    public BattleFollowUp onDefendedBattle(ServerLevel level, BoardSession session, BoardParticipant defender,
                                           BoardParticipant attacker, BoardBuffInstance instance) {
        if (defender.knockedDown() || attacker.knockedDown()) return BattleFollowUp.none(defender);
        AstralPlayerStats stats = defender.stats();
        if (instance.acquiredLevels() > 0) {
            BoardBuffInstance next = instance.withAcquiredLevels(instance.acquiredLevels() - 1);
            stats = next == null ? stats.removeBuff(instance.id()) : stats.setBuff(next);
        }

        return new BattleFollowUp(defender.withStats(stats), true);
    }

}