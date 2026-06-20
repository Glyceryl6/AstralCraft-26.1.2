package com.astral_craft.common.gameplay.event.effects;

import com.astral_craft.common.gameplay.event.AstralEventContext;
import net.minecraft.commands.CommandSourceStack;

final class EventEffectCommands {

    static void run(AstralEventContext context, String command) {
        if (context == null || command == null || command.isBlank()) return;
        String resolved = command
                .replace("@player", context.triggerPlayerName())
                .replace("@target", context.targetSelector());
        CommandSourceStack source = context.commandSource();
        context.triggerPlayer().server.getCommands().performPrefixedCommand(source, resolved);
    }

}