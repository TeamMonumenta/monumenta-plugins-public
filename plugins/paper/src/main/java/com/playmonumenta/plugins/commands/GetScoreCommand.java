package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.ScoreboardUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;
import dev.jorel.commandapi.arguments.ObjectiveArgument;
import dev.jorel.commandapi.arguments.ScoreHolderArgument;
import dev.jorel.commandapi.arguments.ScoreHolderArgument.ScoreHolderType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;

import java.util.Collection;
import java.util.Optional;

public class GetScoreCommand {

	@SuppressWarnings("unchecked")
	public static void register(Plugin plugin) {
		new CommandAPICommand("getscore")
		.withPermission(CommandPermission.fromString("monumenta.command.getscore"))
		.withArguments(new ScoreHolderArgument("name", ScoreHolderType.MULTIPLE))
		.withArguments(new ObjectiveArgument("objective"))
		.executes((sender, args) -> {
			String objective = (String)args[1];
			for (String scoreHolder : (Collection<String>)args[0]) {
				run(sender, scoreHolder, objective);
			}
		})
		.register();
	}

	private static void run(CommandSender sender, String name, String objectiveName) {
		Optional<Integer> scoreValue = ScoreboardUtils.getScoreboardValue(name, objectiveName);
		if (scoreValue.isPresent()) {
			sender.sendMessage(ChatColor.AQUA + "Score for " + ChatColor.GOLD + name + ChatColor.AQUA + " in " + ChatColor.GOLD + objectiveName + ChatColor.AQUA + ": " + ChatColor.GOLD + scoreValue.get());
		} else {
			sender.sendMessage(ChatColor.AQUA + "Score for " + ChatColor.GOLD + name + ChatColor.AQUA + " in " + ChatColor.GOLD + objectiveName + ChatColor.AQUA + ": " + ChatColor.GOLD + "not set");
		}
	}

}
