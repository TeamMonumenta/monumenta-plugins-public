package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.ObjectiveArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.OptionalInt;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.scoreboard.Objective;

public class CompareScoresCommand {
	private static final List<String> SELECTORS = Arrays.asList("@a", "@e");

	public static void register() {
		new CommandAPICommand("comparescores")
			.withArguments(new EntitySelectorArgument.ManyEntities("targets")
				.replaceSuggestions(ArgumentSuggestions.strings((info) -> {
					List<String> suggestions = new ArrayList<>(SELECTORS);
					return suggestions.toArray(new String[0]);
				}))
			)
			.withArguments(new ObjectiveArgument("scoreboard"))
			.withPermission("monumenta.command.comparescores")
			.executes(CompareScoresCommand::run)
			.register();
	}

	private static int run(CommandSender sender, CommandArguments args) {
		List<?> orderedList = new ArrayList<>((Collection<?>) args.getOrDefault("targets", new ArrayList<>()));
		Objective scoreboard = args.getUnchecked("scoreboard");
		if (orderedList.size() < 2) {
			sender.sendMessage(Component.text("Your selection has less than two entities, are you sure this was correct?", NamedTextColor.GRAY));
		} else {
			OptionalInt first = ScoreboardUtils.getScoreboardValue((Entity) orderedList.get(0), scoreboard);
			OptionalInt second;
			for (Object target : orderedList) {
				if (target instanceof Entity entity) {
					second = ScoreboardUtils.getScoreboardValue(entity, scoreboard);
					if (second.isEmpty() || !first.equals(second)) {
						sender.sendMessage(Component.text("The scores for entities in this selection did not all match.", NamedTextColor.GRAY));
						return 0;
					}
				} else {
					sender.sendMessage(Component.text("Non-entity found? Most likely a glitch.", NamedTextColor.RED));
					MMLog.fine("CompareScoresCommand found a non-entity in its multiple entities selector, it's called " + target);
				}
			}
		}
		sender.sendMessage(Component.text("All scores matched!", NamedTextColor.GREEN));
		return 1;
	}
}
