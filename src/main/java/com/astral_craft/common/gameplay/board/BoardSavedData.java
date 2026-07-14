package com.astral_craft.common.gameplay.board;

import com.astral_craft.AstralCraft;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BoardSavedData extends SavedData {

    public static final Codec<BoardSavedData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            BoardSession.Snapshot.CODEC.listOf().optionalFieldOf("boards", List.of())
                    .forGetter(BoardSavedData::snapshots)
    ).apply(instance, BoardSavedData::new));

    public static final SavedDataType<BoardSavedData> TYPE = new SavedDataType<>(
            AstralCraft.prefix("board_sessions"), BoardSavedData::new, CODEC);

    private final Map<UUID, BoardSession> sessions = new LinkedHashMap<>();

    public BoardSavedData() {}

    public BoardSavedData(List<BoardSession.Snapshot> snapshots) {
        for (BoardSession.Snapshot snapshot : snapshots) {
            BoardSession session = BoardSession.fromSnapshot(snapshot);
            this.sessions.put(session.id(), session);
        }
    }

    public List<BoardSession> sessions() {
        return new ArrayList<>(this.sessions.values());
    }

    public BoardSession get(UUID id) {
        return this.sessions.get(id);
    }

    public void put(BoardSession session) {
        this.sessions.put(session.id(), session);
        this.setDirty();
    }

    public boolean remove(UUID id) {
        BoardSession removed = this.sessions.remove(id);
        if (removed != null) {
            this.setDirty();
            return true;
        }
        return false;
    }

    public void markChanged() {
        this.setDirty();
    }

    private List<BoardSession.Snapshot> snapshots() {
        return this.sessions.values().stream().map(BoardSession::snapshot).toList();
    }

}