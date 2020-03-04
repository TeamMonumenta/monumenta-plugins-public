package com.playmonumenta.plugins.commands;

import java.util.LinkedHashMap;

import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.StringArgument;

public class SkillDescription extends GenericCommand {
	@SuppressWarnings("unchecked")
	public static void register(Plugin plugin) {
		String command = "skilldescription";
		CommandPermission perms = CommandPermission.fromString("monumenta.command.skilldescription");

		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("objective", new StringArgument());
		CommandAPI.getInstance().register(command,
		                                  perms,
		                                  arguments,
		                                  (sender, args) -> {
		                                      tell(plugin, sender, (String)args[0]);
		                                  }
		);
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

