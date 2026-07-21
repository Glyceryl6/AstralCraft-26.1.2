package com.astral_craft.common.gameplay.handcard;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;

public class AstralHandCards {

    protected static final Codec<Map<Identifier, Integer>> CARD_MAP_CODEC = Codec.unboundedMap(Identifier.CODEC, Codec.INT);

    public static final Codec<AstralHandCards> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            CARD_MAP_CODEC.optionalFieldOf("cards", Map.of()).forGetter(AstralHandCards::cards)
    ).apply(instance, AstralHandCards::new));

    private static final StreamCodec<ByteBuf, Map<Identifier, Integer>> MAP_STREAM_CODEC = ByteBufCodecs.map(
            LinkedHashMap::new, Identifier.STREAM_CODEC, ByteBufCodecs.VAR_INT, 256);

    public static final StreamCodec<ByteBuf, AstralHandCards> STREAM_CODEC = MAP_STREAM_CODEC.map(
            AstralHandCards::new, AstralHandCards::cards);

    protected Map<Identifier, Integer> cards = new LinkedHashMap<>();

    public AstralHandCards(Map<Identifier, Integer> cards) {
        if (cards != null) {
            for (Map.Entry<Identifier, Integer> entry : cards.entrySet()) {
                if (entry.getKey() != null && entry.getValue() != null && entry.getValue() > 0) {
                    this.cards.put(entry.getKey(), entry.getValue());
                }
            }
        }
    }

    public static AstralHandCards empty() {
        return new AstralHandCards(Map.of());
    }

    public Map<Identifier, Integer> cards() {
        return Map.copyOf(this.cards);
    }

    public int count(Identifier cardId) {
        return this.cards.getOrDefault(cardId, 0);
    }

    public boolean has(Identifier cardId) {
        return this.count(cardId) > 0;
    }

    public void add(Identifier cardId, int count) {
        if (cardId == null || count <= 0) return;
        this.cards.merge(cardId, count, Integer::sum);
    }

    public boolean remove(Identifier cardId, int count) {
        if (cardId == null || count <= 0) return false;
        int current = this.count(cardId);
        if (current < count) return false;
        if (current == count) {
            this.cards.remove(cardId);
        } else {
            this.cards.put(cardId, current - count);
        }
        return true;
    }

    public boolean emptyHand() {
        return this.cards.isEmpty();
    }

    public int totalCount() {
        int total = 0;
        for (int count : this.cards.values()) {
            total += Math.max(0, count);
        }
        return total;
    }

    public Identifier firstCardId() {
        for (Map.Entry<Identifier, Integer> entry : this.cards.entrySet()) {
            if (entry.getValue() > 0) {
                return entry.getKey();
            }
        }
        return null;
    }

    public void clear() {
        this.cards.clear();
    }

    public String encode() {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<Identifier, Integer> entry : this.cards.entrySet()) {
            if (entry.getValue() <= 0) continue;
            if (!builder.isEmpty()) {
                builder.append(';');
            }
            builder.append(entry.getKey()).append('|').append(entry.getValue());
        }

        return builder.toString();
    }

}