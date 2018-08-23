package com.playmonumenta.plugins.command.commands;

import com.playmonumenta.plugins.command.AbstractPlayerCommand;
import com.playmonumenta.plugins.command.CommandContext;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import org.bukkit.plugin.Plugin;

public class CheckEmptyInventory extends AbstractPlayerCommand {

	public CheckEmptyInventory(Plugin plugin) {
		super(
		    "checkEmptyInventory",
		    "Return success if the player's inventory is empty, fails otherwise",
		    plugin
		);
	}

	@Override
	protected void configure(final ArgumentParser parser) {
	}

	@Override
	protected boolean run(final CommandContext context) {
		//noinspection OptionalGetWithoutIsPresent - checked before being called
		final Player player = context.getPlayer().get();

		for (ItemStack item : player.getInventory().getContents()) {
			if (item != null) {
				return false;
			}
		}

		return true;
	}
}
