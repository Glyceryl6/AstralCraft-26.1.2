package com.astral_craft.common.gameplay.board;

import com.astral_craft.common.gameplay.PanelTypes;
import com.astral_craft.common.registry.AstralBlocks;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Maps physical platform blocks to their registered logical board panel types. */
public class PlatformPanelMapper {

    private static final Map<Block, Identifier> PANEL_BY_BLOCK = new HashMap<>();

    static {
        map(AstralBlocks.PLATFORM_START.get(), PanelTypes.START.getId());
        map(AstralBlocks.PLATFORM_TELEPORT.get(), PanelTypes.PORTAL.getId());
        map(AstralBlocks.PLATFORM_TELEPORT_POINT.get(), PanelTypes.PORTAL.getId());
        map(AstralBlocks.PLATFORM_SHOP.get(), PanelTypes.SHOP.getId());
        map(AstralBlocks.PLATFORM_EVENT.get(), PanelTypes.EVENT.getId());
        map(AstralBlocks.PLATFORM_DIVINE.get(), PanelTypes.DIVINATION.getId());
        map(AstralBlocks.PLATFORM_HEAL.get(), PanelTypes.RECOVER.getId());
        map(AstralBlocks.PLATFORM_HOSPITAL.get(), PanelTypes.HOSPITAL.getId());
        map(AstralBlocks.PLATFORM_MOVE_AGAIN.get(), PanelTypes.HASTE.getId());
        map(AstralBlocks.PLATFORM_MONSTER.get(), PanelTypes.MONSTER.getId());
        map(AstralBlocks.PLATFORM_DAMAGE.get(), PanelTypes.CALAMITY.getId());
        map(AstralBlocks.PLATFORM_CARD.get(), PanelTypes.CARD_REWARD.getId());
        map(AstralBlocks.PLATFORM_GOLD.get(), PanelTypes.WINDFALL.getId());
        map(AstralBlocks.PLATFORM_LOTTERY.get(), PanelTypes.LOTTERY.getId());
        map(AstralBlocks.PLATFORM_GAMBLE.get(), PanelTypes.GUESSING.getId());
        map(AstralBlocks.PLATFORM_GIFT.get(), PanelTypes.GIFT.getId());
        map(AstralBlocks.PLATFORM_JUMP.get(), PanelTypes.JUMP.getId());
        map(AstralBlocks.PLATFORM_RELIC.get(), PanelTypes.CHIP_SHOP.getId());
        map(AstralBlocks.PLATFORM_GIMMICK.get(), PanelTypes.GIMMICK.getId());
        map(AstralBlocks.PLATFORM_CANDY_GHOST.get(), PanelTypes.CANDY_MACHINE.getId());
        map(AstralBlocks.PLATFORM_FIRE.get(), PanelTypes.CANNON.getId());
        map(AstralBlocks.PLATFORM_DESTINY.get(), PanelTypes.FORTUNE.getId());
    }

    private static void map(Block block, Identifier panelId) {
        PANEL_BY_BLOCK.put(block, panelId);
    }

    public static Optional<Identifier> panelId(Block block) {
        return Optional.ofNullable(PANEL_BY_BLOCK.get(block));
    }

    public static boolean isPlatform(Block block) {
        return PANEL_BY_BLOCK.containsKey(block);
    }

}
