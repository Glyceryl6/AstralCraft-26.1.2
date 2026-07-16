package com.astral_craft.common.blocks;

import com.astral_craft.AstralCraft;
import com.astral_craft.common.gameplay.board.BoardPanelContext;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import java.util.Map;

/** Dedicated platform block subclasses. Each block owns its board trigger and effect. */
public class PlatformBlocks {

    private static final Map<String, String> LEGACY_PATHS = Map.ofEntries(
            Map.entry("start", "platform_start"),
            Map.entry("check_point", "platform_start"),
            Map.entry("portal", "platform_teleport"),
            Map.entry("shop", "platform_shop"),
            Map.entry("event", "platform_event"),
            Map.entry("divination", "platform_divine"),
            Map.entry("recover", "platform_heal"),
            Map.entry("hospital", "platform_hospital"),
            Map.entry("haste", "platform_move_again"),
            Map.entry("monster", "platform_monster"),
            Map.entry("calamity", "platform_damage"),
            Map.entry("card_reward", "platform_card"),
            Map.entry("windfall", "platform_gold"),
            Map.entry("lottery", "platform_lottery"),
            Map.entry("guessing", "platform_gamble"),
            Map.entry("gift", "platform_gift"),
            Map.entry("jump", "platform_jump"),
            Map.entry("chip_shop", "platform_relic"),
            Map.entry("gimmick", "platform_gimmick"),
            Map.entry("candy_machine", "platform_candy_ghost"),
            Map.entry("cannon", "platform_fire"),
            Map.entry("fortune", "platform_destiny")
    );

    public static Identifier canonicalPlatformId(Identifier id) {
        if (id == null) return AstralCraft.prefix("platform_heal");
        if (!AstralCraft.MOD_ID.equals(id.getNamespace())) return id;
        String path = LEGACY_PATHS.get(id.getPath());
        return path == null ? id : AstralCraft.prefix(path);
    }

    public static class CandyGhost extends BasePlatform {
        public CandyGhost(Block.Properties properties) { super(properties, Trigger.BOTH); }
    }

    public static class Card extends BasePlatform {
        public Card(Block.Properties properties) { super(properties, Trigger.LANDING); }
        @Override public void applyBoardEffect(BoardPanelContext context) { context.drawCards(2); }
    }

    public static class Damage extends BasePlatform {
        public Damage(Block.Properties properties) { super(properties, Trigger.LANDING); }
        @Override public void applyBoardEffect(BoardPanelContext context) { context.damage(2); }
    }

    public static class Destiny extends BasePlatform {
        public Destiny(Block.Properties properties) { super(properties, Trigger.LANDING); }
    }

    public static class Divine extends BasePlatform {
        public Divine(Block.Properties properties) { super(properties, Trigger.LANDING); }
    }

    public static class Event extends BasePlatform {
        public Event(Block.Properties properties) { super(properties, Trigger.LANDING); }
    }

    public static class Fire extends BasePlatform {
        public Fire(Block.Properties properties) { super(properties, Trigger.LANDING); }
    }

    public static class Gamble extends BasePlatform {
        public Gamble(Block.Properties properties) { super(properties, Trigger.LANDING); }
    }

    public static class Gift extends BasePlatform {
        public Gift(Block.Properties properties) { super(properties, Trigger.BOTH); }
    }

    public static class Gimmick extends BasePlatform {
        public Gimmick(Block.Properties properties) { super(properties, Trigger.LANDING); }
    }

    public static class Gold extends BasePlatform {
        public Gold(Block.Properties properties) { super(properties, Trigger.LANDING); }
        @Override public void applyBoardEffect(BoardPanelContext context) { context.addRandomCoins(2, 3, 4, 5, 10); }
    }

    public static class Heal extends BasePlatform {
        public Heal(Block.Properties properties) { super(properties, Trigger.LANDING); }
        @Override public void applyBoardEffect(BoardPanelContext context) { context.heal(2); }
    }

    public static class Hospital extends BasePlatform {
        public Hospital(Block.Properties properties) { super(properties, Trigger.LANDING); }
    }

    public static class Jump extends BasePlatform {
        public Jump(Block.Properties properties) { super(properties, Trigger.LANDING); }
    }

    public static class Lottery extends BasePlatform {
        public Lottery(Block.Properties properties) { super(properties, Trigger.LANDING); }
    }

    public static class Monster extends BasePlatform {
        public Monster(Block.Properties properties) { super(properties, Trigger.LANDING); }
    }

    public static class MoveAgain extends BasePlatform {
        public MoveAgain(Block.Properties properties) { super(properties, Trigger.LANDING); }
    }

    public static class Relic extends BasePlatform {
        public Relic(Block.Properties properties) { super(properties, Trigger.BOTH); }
    }

    public static class Shop extends BasePlatform {
        public Shop(Block.Properties properties) { super(properties, Trigger.BOTH); }
        @Override public void applyBoardEffect(BoardPanelContext context) { context.openShop(); }
    }

    public static class Start extends BasePlatform {
        public Start(Block.Properties properties) { super(properties, Trigger.BOTH); }
        @Override public boolean isStartPoint() { return true; }
    }

    public static class Teleport extends BasePlatform {
        public Teleport(Block.Properties properties) { super(properties, Trigger.LANDING); }
        @Override public boolean isPortal() { return true; }
        @Override public void applyBoardEffect(BoardPanelContext context) { context.teleportToRandomPortal(); }
    }

    public static class TeleportPoint extends Teleport {
        public TeleportPoint(Block.Properties properties) { super(properties); }
    }
}
