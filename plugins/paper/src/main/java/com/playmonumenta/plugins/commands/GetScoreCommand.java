package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.ScoreboardUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;
import dev.jorel.commandapi.arguments.ObjectiveArgument;
import dev.jorel.commandapi.arguments.TextArgument;
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
		.withArguments(new TextArgument("name"))
		.withArguments(new ObjectiveArgument("objective"))
		.executes((sender, args) -> {
			run(sender, (String)args[0], (String)args[1]);
		})
		.register();
		new CommandAPICommand("getscore")
		.withPermission(CommandPermission.fromString("monumenta.command.getscore"))
		.withArguments(new EntitySelectorArgument("targets", EntitySelector.MANY_ENTITIES))
		.withArguments(new ObjectiveArgument("objective"))
		.executes((sender, args) -> {
			for (Entity entity : (Collection<Entity>)args[0]) {
				if (entity instanceof Player) {
					run(sender, entity.getName(), (String)args[1]);
				} else {
					run(sender, entity.getUniqueId().toString(), (String)args[1]);
				}
			}
		})
		.register();
	}

	private static void run(CommandSender sender, String name, String objectiveName) {

		Optional<Integer> scoreValue = Optional.empty();
		Objective objective = Bukkit.getScoreboardManager().getMainScoreboard().getObjective(objectiveName);
		if (objective != null) {
			Score score = objective.getScore(name);
			if (score != null && score.isScoreSet()) {
				scoreValue = ScoreboardUtils.getScoreboardValue(name, objectiveName);
				sender.sendMessage(ChatColor.AQUA + "Score for " + ChatColor.GOLD + name + ChatColor.AQUA + " in " + ChatColor.GOLD + objectiveName + ChatColor.AQUA + ": " + ChatColor.GOLD + scoreValue.get());
			} else {
				sender.sendMessage(ChatColor.AQUA + "Score not set for " + ChatColor.GOLD + name + ChatColor.AQUA + " in " + ChatColor.GOLD + objectiveName);
			}
		}
	}

}
