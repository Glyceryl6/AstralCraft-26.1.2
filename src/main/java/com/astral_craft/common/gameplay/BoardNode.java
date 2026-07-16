package com.astral_craft.common.gameplay;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;

import java.util.List;

public record BoardNode(String id, Identifier platformId, List<String> next) {

    public static final Codec<BoardNode> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("id").forGetter(BoardNode::id),
            Identifier.CODEC.fieldOf("panel_type").forGetter(BoardNode::platformId),
            Codec.STRING.listOf().optionalFieldOf("next", List.of()).forGetter(BoardNode::next)
    ).apply(instance, BoardNode::new));

    public BoardNode {
        next = List.copyOf(next);
    }

    public String defaultNext() {
        return this.next.isEmpty() ? this.id : this.next.getFirst();
    }

}