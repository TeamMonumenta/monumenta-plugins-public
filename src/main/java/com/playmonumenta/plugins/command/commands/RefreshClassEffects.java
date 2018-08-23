package com.playmonumenta.plugins.command.commands;

import com.playmonumenta.plugins.command.AbstractPlayerCommand;
import com.playmonumenta.plugins.command.CommandContext;
import com.playmonumenta.plugins.managers.potion.PotionManager;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class RefreshClassEffects extends AbstractPlayerCommand {
	private final PotionManager mPotionManager;

	public RefreshClassEffects(Plugin plugin, PotionManager potionManager) {
		super(
		    "refreshClassEffects",
		    "Refreshes class effects for affected player (either sender or target of /execute)",
		    plugin
		);
		this.mPotionManager = potionManager;
	}

	/**
	 * /execute @p[stuff] ~ ~ ~ refreshClassEffects
	 *
	 * @param parser the {@link ArgumentParser} specific to the command
	 */
	@Override
	protected void configure(ArgumentParser parser) {
	}

	@Override
	protected boolean run(CommandContext context) {
		//noinspection OptionalGetWithoutIsPresent - checked before being called
		final Player player = context.getPlayer().get();

		mPotionManager.refreshClassEffects(player);
		sendMessage(context, ChatColor.GOLD + "Refreshed class effects for player '" + player.getName() + "'");

		return true;
	}
}
