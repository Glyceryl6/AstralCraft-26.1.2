package com.astral_craft.common.blockentity;

import com.astral_craft.common.registry.AstralBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public class PlatformBlockEntity extends BlockEntity {

    private static final Identifier DEFAULT_SKIN = Identifier.withDefaultNamespace("default");
    private @Nullable Identifier characterId;
    private Identifier skinId = DEFAULT_SKIN;

    public PlatformBlockEntity(BlockPos pos, BlockState state) {
        super(AstralBlockEntities.PLATFORM.get(), pos, state);
    }

    public @Nullable Identifier characterId() {
        return this.characterId;
    }

    public Identifier skinId() {
        return this.skinId;
    }

    public void setPortrait(@Nullable Identifier characterId, @Nullable Identifier skinId) {
        Identifier safeSkin = skinId == null ? DEFAULT_SKIN : skinId;
        if (Objects.equals(this.characterId, characterId) && this.skinId.equals(safeSkin)) return;
        this.characterId = characterId;
        this.skinId = safeSkin;
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    public void clearPortrait() {
        this.setPortrait(null, DEFAULT_SKIN);
    }

    @Override
    public void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (this.characterId != null) output.putString("character_id", this.characterId.toString());
        output.putString("skin_id", this.skinId.toString());
    }

    @Override
    public void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.characterId = parseIdentifier(input.getStringOr("character_id", ""));
        Identifier loadedSkin = parseIdentifier(input.getStringOr("skin_id", DEFAULT_SKIN.toString()));
        this.skinId = loadedSkin == null ? DEFAULT_SKIN : loadedSkin;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }


    private static @Nullable Identifier parseIdentifier(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return Identifier.parse(raw);
        } catch (Exception ignored) {
            return null;
        }
    }
}
