package com.playmonumenta.plugins.command.commands;

import com.playmonumenta.plugins.command.AbstractPlayerCommand;
import com.playmonumenta.plugins.command.CommandContext;
import com.playmonumenta.plugins.managers.potion.PotionManager;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class ClearEffects extends AbstractPlayerCommand {
	private final PotionManager mPotionManager;

	public ClearEffects(Plugin plugin, PotionManager mPotionManager) {
		super(
		    "clearEffects",
		    "Clears all status effects from the player",
		    plugin
		);
		this.mPotionManager = mPotionManager;
	}

	@Override
	protected void configure(final ArgumentParser parser) {
	}

	@Override
	protected boolean run(final CommandContext context) {
		//noinspection OptionalGetWithoutIsPresent - checked before being called
		final Player player = context.getPlayer().get();

		mPotionManager.clearAllPotions(player);

		sendMessage(context, "Cleared potion effects for player '" + player.getName() + "'");

		return true;
	}
}
