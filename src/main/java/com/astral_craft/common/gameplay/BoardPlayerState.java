package com.astral_craft.common.gameplay;

import java.util.UUID;

public record BoardPlayerState(
        UUID playerId,
        String currentNodeId,
        int hp,
        int maxHp,
        int attack,
        int defense,
        int coins,
        int stars,
        int movementBonus,
        int cardPlaysRemaining
) {

    public BoardPlayerState moveTo(String nodeId) {
        return new BoardPlayerState(playerId, nodeId, hp, maxHp, attack, defense, coins, stars, movementBonus, cardPlaysRemaining);
    }

    public BoardPlayerState heal(int amount) {
        int nextHp = Math.min(maxHp, hp + Math.max(0, amount));
        return new BoardPlayerState(playerId, currentNodeId, nextHp, maxHp, attack, defense, coins, stars, movementBonus, cardPlaysRemaining);
    }

    public BoardPlayerState damage(int amount) {
        int nextHp = Math.max(0, hp - Math.max(0, amount));
        return new BoardPlayerState(playerId, currentNodeId, nextHp, maxHp, attack, defense, coins, stars, movementBonus, cardPlaysRemaining);
    }

    public BoardPlayerState spendCoins(int amount) {
        int nextCoins = Math.max(0, coins - Math.max(0, amount));
        return new BoardPlayerState(playerId, currentNodeId, hp, maxHp, attack, defense, nextCoins, stars, movementBonus, cardPlaysRemaining);
    }

    public BoardPlayerState addCoins(int amount) {
        return new BoardPlayerState(playerId, currentNodeId, hp, maxHp, attack, defense, coins + Math.max(0, amount), stars, movementBonus, cardPlaysRemaining);
    }
}
