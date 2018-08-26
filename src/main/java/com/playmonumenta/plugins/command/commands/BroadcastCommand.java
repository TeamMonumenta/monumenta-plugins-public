package com.playmonumenta.plugins.command.commands;

import com.google.common.collect.ImmutableList;
import com.playmonumenta.plugins.command.AbstractCommand;
import com.playmonumenta.plugins.command.CommandContext;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import org.bukkit.ChatColor;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.utils.NetworkUtils;

import java.util.List;
import java.util.Optional;

public class BroadcastCommand extends AbstractCommand {
	private static final ImmutableList<String> ALLOWED_COMMANDS = ImmutableList.of(
	            "whitelist",
	            "ban",
	            "ban-ip",
	            "pardon",
	            "pardon-ip",
	            "op",
	            "deop",

	            "say",
	            "msg",
	            "tell",
	            "tellraw",
	            "title",

	            "function",
	            "difficulty",
	            "scoreboard",
	            "setblock"
	        );

	private final boolean enabled;

	public BroadcastCommand(Plugin plugin, boolean enabled) {
		super(
		    "broadcastCommand",
		    "Broadcasts a command to run on all Monumenta servers",
		    plugin
		);
		this.enabled = enabled;
	}

	@Override
	protected void configure(final ArgumentParser parser) {
		parser.addArgument("command")
		.help("command to broadcast");
		parser.addArgument("arguments")
		.help("arguments for the command")
		.nargs("+");
	}

	@Override
	protected boolean run(final CommandContext context) {
		if (!enabled) {
			sendErrorMessage(context, "Use of this command is restricted on this server");
			return false;
		}

		final String command = context.getNamespace().get("command");
		if (!ALLOWED_COMMANDS.contains(command)) {
			sendErrorMessage(context, "The command '" + command + "' is not supported!");
			return false;
		}

		final String commandString = getCommandString(
		                                 command,
		                                 context.getNamespace().get("arguments"),
		                                 context.getPlayer()
		                             );

		sendMessage(context, ChatColor.GOLD + "Broadcasting command '" + commandString + "' to all servers!");

		try {
			NetworkUtils.broadcastCommand((com.playmonumenta.plugins.Plugin) mPlugin, commandString);
		} catch (Exception e) {
			sendErrorMessage(context, "Broadcasting command failed");
		}

		return true;
	}

	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	private String getCommandString(final String command, final List<String> arguments, final Optional<Player> player) {
		final StringBuilder commandStringBuilder = new StringBuilder();
		final String playerName = player.map(HumanEntity::getName).orElse(null);

		commandStringBuilder.append(command);

		arguments.forEach(argument -> {
			// If possible, replace @s with player's name
			argument = checkReplacements(argument, "@s", playerName);

			// Replace the special @A token with @a
			// (so Minecraft isn't allowed to resolve it preemptively)
			argument = checkReplacements(argument, "@A", "@a");

			commandStringBuilder.append(" ").append(argument);
		});

		return commandStringBuilder.toString();
	}

	private String checkReplacements(String argument, String match, String replace) {
		if (!argument.contains(match)) {
			return argument;
		}

		if (replace == null) {
			throw new ArgumentParsingException("Failed to resolve " + match + " argument");
		}

		return argument.replace(match, replace);
	}

	class ArgumentParsingException extends RuntimeException {
		ArgumentParsingException(String message) {
			super(message);
		}
	}

	@Override
	protected void onError(final CommandContext context, final Throwable e) {
		if (e instanceof ArgumentParsingException) {
			sendErrorMessage(context, e.getMessage());
		} else {
			super.onError(context, e);
		}
	}
}
