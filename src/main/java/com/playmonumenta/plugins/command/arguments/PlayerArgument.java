package com.playmonumenta.plugins.command.arguments;

import net.sourceforge.argparse4j.inf.Argument;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.ArgumentType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Player type conversion for the argument parser.
 */
public class PlayerArgument implements ArgumentType<Player> {
	private final Plugin plugin;

	public PlayerArgument(Plugin plugin) {
		this.plugin = plugin;
	}

	@Override
	public Player convert(ArgumentParser argumentParser, Argument argument, String value) throws ArgumentParserException {
		final Player player = plugin.getServer().getPlayer(value);

		if (player == null) {
			throw new ArgumentParserException("Player not found: " + value, argumentParser);
		}

		return player;
	}
}
