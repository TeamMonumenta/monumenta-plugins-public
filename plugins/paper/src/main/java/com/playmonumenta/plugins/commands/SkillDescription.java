package com.playmonumenta.plugins.commands;

import java.util.ArrayList;
import java.util.List;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ObjectiveArgument;

public class SkillDescription extends GenericCommand {
	private static final String COMMAND = "skilldescription";

	public static void register(Plugin plugin) {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.skilldescription");

		List<Argument> arguments = new ArrayList<>();
		arguments.add(new ObjectiveArgument("objective"));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				tell(plugin, sender, (String)args[0]);
			})
			.register();
	}

	private static void tell(Plugin plugin, CommandSender sender, String scoreboardId) {
		CommandSender target = sender;
		if (sender instanceof ProxiedCommandSender) {
			if (((ProxiedCommandSender) sender).getCallee() instanceof Player) {
				target = (CommandSender) ((ProxiedCommandSender) sender).getCallee();
			} else {
				error(sender, "Command must be run as a player.");
				return;
			}
		}

		for (Ability ability : plugin.mAbilityManager.getReferenceAbilities()) {
			if (scoreboardId.equals(ability.getScoreboard())) {
				ability.getInfo().sendDescriptions(target);
				return;
			}
		}

		error(sender, "Could not find the skill " + scoreboardId + ".");
	}
}

