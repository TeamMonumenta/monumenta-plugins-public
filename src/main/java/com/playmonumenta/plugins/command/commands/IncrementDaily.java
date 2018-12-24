package com.playmonumenta.plugins.command.commands;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.command.AbstractCommand;
import com.playmonumenta.plugins.command.CommandContext;
import com.playmonumenta.plugins.server.reset.DailyReset;

import net.sourceforge.argparse4j.inf.ArgumentParser;

public class IncrementDaily extends AbstractCommand {

	public IncrementDaily(Plugin plugin) {
		super(
		    "incrementDaily",
		    "Increments the servers Daily Version so we can message logging in players and online players when the Daily has reset.",
		    plugin
		);
	}

	@Override
	protected void configure(ArgumentParser parser) {
	}

	@Override
	protected boolean run(CommandContext context) {
		com.playmonumenta.plugins.Plugin mPlugin = (com.playmonumenta.plugins.Plugin) this.mPlugin;

		//  Increment the servers Daily version.
		mPlugin.incrementDailyVersion(); // TODO REFACTOR

		//  Loop through all online players, reset their scoreboards and message them about the Daily reset.
		for (Player player : mPlugin.getServer().getOnlinePlayers()) {
			DailyReset.handle(mPlugin, player);
		}

		return true;
	}
}
