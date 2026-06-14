package com.astral_craft.common.gameplay.board;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.registry.AstralBlocks;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** Maps physical flat platform blocks to logical board panel ids. */
public final class PlatformPanelMapper {

    private static final Map<Block, Identifier> PANEL_BY_BLOCK = new HashMap<>();

    static {
        map(AstralBlocks.PLATFORM_START.get(), "start");
        map(AstralBlocks.PLATFORM_TELEPORT.get(), "portal");
        map(AstralBlocks.PLATFORM_TELEPORT_POINT.get(), "portal");
        map(AstralBlocks.PLATFORM_SHOP.get(), "shop");
        map(AstralBlocks.PLATFORM_EVENT.get(), "event");
        map(AstralBlocks.PLATFORM_DIVINE.get(), "divination");
        map(AstralBlocks.PLATFORM_HEAL.get(), "recover");
        map(AstralBlocks.PLATFORM_HOSPITAL.get(), "hospital");
        map(AstralBlocks.PLATFORM_MOVE_AGAIN.get(), "haste");
        map(AstralBlocks.PLATFORM_MONSTER.get(), "monster");
        map(AstralBlocks.PLATFORM_DAMAGE.get(), "cannon");
        map(AstralBlocks.PLATFORM_CARD.get(), "card_reward");
        map(AstralBlocks.PLATFORM_GOLD.get(), "coin_reward");
        map(AstralBlocks.PLATFORM_LOTTERY.get(), "lottery");
        map(AstralBlocks.PLATFORM_GAMBLE.get(), "guessing");
        map(AstralBlocks.PLATFORM_GIFT.get(), "gift");
        map(AstralBlocks.PLATFORM_JUMP.get(), "jump");
        map(AstralBlocks.PLATFORM_RELIC.get(), "chip_shop");
        map(AstralBlocks.PLATFORM_GIMMICK.get(), "gimmick_console");
        map(AstralBlocks.PLATFORM_CANDY_GHOST.get(), "candy_machine");
        map(AstralBlocks.PLATFORM_FIRE.get(), "damage");
        map(AstralBlocks.PLATFORM_DESTINY.get(), "personal_fortune");
    }

    private static void map(Block block, String panelPath) {
        PANEL_BY_BLOCK.put(block, AstralCraft.prefix(panelPath));
    }

    public static Optional<Identifier> panelId(Block block) {
        return Optional.ofNullable(PANEL_BY_BLOCK.get(block));
    }

    public static boolean isPlatform(Block block) {
        return PANEL_BY_BLOCK.containsKey(block);
    }

}