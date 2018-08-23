package com.playmonumenta.plugins.command.commands;

import com.playmonumenta.plugins.command.AbstractPlayerCommand;
import com.playmonumenta.plugins.command.CommandContext;
import com.playmonumenta.plugins.managers.potion.PotionManager;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import org.bukkit.entity.Player;

import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;

public class DebugInfo extends AbstractPlayerCommand {
    private final PotionManager mPotionManager;

	public DebugInfo(Plugin plugin, PotionManager potionManager) {
        super(
            "debugInfo",
            "Tells the sender information about the target",
            plugin
        );
        this.mPotionManager = potionManager;
    }

    @Override
    protected void configure(final ArgumentParser parser) {
    }

    @Override
    protected boolean run(final CommandContext context) {
        //noinspection OptionalGetWithoutIsPresent - checked before being called
        final Player player = context.getPlayer().get();

        sendMessage(context, ChatColor.GOLD + "Potion info for player '" + player.getName() + "': " +
		                   mPotionManager.printInfo(player));

		return true;
	}
}
