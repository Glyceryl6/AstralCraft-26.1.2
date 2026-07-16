package com.astral_craft.common.gameplay.board;

import net.minecraft.server.level.ServerLevel;

/** Context passed from the board coordinator to a platform block's board effect hook. */
public record BoardPanelContext(
        ServerLevel level,
        BoardSession session,
        BoardParticipant participant,
        boolean landing) {

    public void heal(int amount) {
        BoardSessionManager.applyPanelStats(this.level, this.session, this.participant,
                this.participant.stats().heal(Math.max(0, amount)));
    }

    public void damage(int amount) {
        BoardSessionManager.applyPanelStats(this.level, this.session, this.participant,
                this.participant.stats().damage(Math.max(0, amount)));
    }

    public void addCoins(int amount) {
        BoardSessionManager.applyPanelStats(this.level, this.session, this.participant,
                this.participant.stats().addCoins(Math.max(0, amount)));
    }

    public void addRandomCoins(int... values) {
        if (values == null || values.length == 0) return;
        this.addCoins(values[this.level.getRandom().nextInt(values.length)]);
    }

    public void drawCards(int count) {
        BoardSessionManager.drawPanelCards(this.level, this.session, this.participant, Math.max(0, count));
    }

    public void teleportToRandomPortal() {
        BoardSessionManager.teleportFromPanel(this.level, this.session, this.participant);
    }

    public void openShop() {
        BoardSessionManager.openShop(this.level, this.session, this.participant);
    }
}
