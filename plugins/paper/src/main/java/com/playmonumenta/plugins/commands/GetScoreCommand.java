package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.ScoreboardUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.ObjectiveArgument;
import dev.jorel.commandapi.arguments.ScoreHolderArgument;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.OptionalInt;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class GetScoreCommand {
	private static final List<String> SELECTORS = Arrays.asList("@a", "@e", "@p", "@r", "@s");

	@SuppressWarnings("unchecked")
	public static void register() {
		new CommandAPICommand("getscore")
		.withPermission(CommandPermission.fromString("monumenta.command.getscore"))
		.withArguments(new ScoreHolderArgument.Multiple("targets").replaceSuggestions(ArgumentSuggestions.strings((info) -> {
			// If ScoreHolderArgument's default suggestions get fixed, remove this override.
			List<String> suggestions = new ArrayList<>(SELECTORS);
			for (Player player : Bukkit.getOnlinePlayers()) {
				suggestions.add(player.getName());
			}
			CommandSender sender = info.sender();
			if (sender instanceof LivingEntity senderEntity) {
				@Nullable Entity target = senderEntity.getTargetEntity(5);
				if (target != null) {
					suggestions.add(target.getUniqueId().toString());
				}
			}
			return suggestions.toArray(new String[0]);
		})))
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
		OptionalInt scoreValue = ScoreboardUtils.getScoreboardValue(name, objectiveName);
		if (scoreValue.isPresent()) {
			sender.sendMessage(ChatColor.AQUA + "Score for " + ChatColor.GOLD + name + ChatColor.AQUA + " in " + ChatColor.GOLD + objectiveName + ChatColor.AQUA + ": " + ChatColor.GOLD + scoreValue.getAsInt());
		} else {
			sender.sendMessage(ChatColor.AQUA + "Score for " + ChatColor.GOLD + name + ChatColor.AQUA + " in " + ChatColor.GOLD + objectiveName + ChatColor.AQUA + ": " + ChatColor.GOLD + "not set");
		}
	}

}
