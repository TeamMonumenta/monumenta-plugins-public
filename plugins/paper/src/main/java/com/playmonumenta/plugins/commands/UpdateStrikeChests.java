package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.ObjectiveArgument;
import dev.jorel.commandapi.arguments.ScoreHolderArgument;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Objective;
import org.jetbrains.annotations.Nullable;

public class UpdateStrikeChests {
	public static String COMMAND = "monumenta";

	@SuppressWarnings("unchecked")
	public static void register() {
		CommandPermission perms = CommandPermission.fromString("monumenta.updatestrikechests");

		List<Argument<?>> startingArguments = new ArrayList<>();
		startingArguments.add(new LiteralArgument("UpdateStrikeChests"));
		startingArguments.add(new EntitySelectorArgument.ManyPlayers("Targets"));

		Map<String, List<Argument<?>>> limitMap = new HashMap<>();

		List<Argument<?>> limitConstArguments = new ArrayList<>();
		limitConstArguments.add(new LiteralArgument("ConstLimit"));
		limitConstArguments.add(new IntegerArgument("Limit", 0));
		limitMap.put("const", limitConstArguments);

		List<Argument<?>> limitScoreArguments = new ArrayList<>();
		limitScoreArguments.add(new LiteralArgument("ScoreLimit"));
		limitScoreArguments.add(new ScoreHolderArgument.Single("LimitHolder"));
		limitScoreArguments.add(new ObjectiveArgument("LimitObjective"));
		limitMap.put("score", limitScoreArguments);

		Map<String, List<Argument<?>>> countMap = new HashMap<>();
		countMap.put("noChange", new ArrayList<>());

		List<Argument<?>> countResetArguments = new ArrayList<>();
		countResetArguments.add(new LiteralArgument("ResetCount"));
		countMap.put("reset", countResetArguments);

		List<Argument<?>> countConstArguments = new ArrayList<>();
		countConstArguments.add(new LiteralArgument("ConstCount"));
		countConstArguments.add(new IntegerArgument("Count", 0));
		countMap.put("const", countConstArguments);

		List<Argument<?>> countScoreArguments = new ArrayList<>();
		countScoreArguments.add(new LiteralArgument("ScoreCount"));
		countScoreArguments.add(new ScoreHolderArgument.Single("CountHolder"));
		countScoreArguments.add(new ObjectiveArgument("CountObjective"));
		countMap.put("score", countScoreArguments);

		List<Argument<?>> arguments = new ArrayList<>();
		for (Map.Entry<String, List<Argument<?>>> limitArgsEntry : limitMap.entrySet()) {
			String limitArgType = limitArgsEntry.getKey();
			for (Map.Entry<String, List<Argument<?>>> countArgsEntry : countMap.entrySet()) {
				String countArgType = countArgsEntry.getKey();

				arguments.clear();
				arguments.addAll(startingArguments);
				arguments.addAll(limitArgsEntry.getValue());
				arguments.addAll(countArgsEntry.getValue());

				new CommandAPICommand(COMMAND)
					.withPermission(perms)
					.withArguments(arguments)
					.executes((sender, args) -> {
						Collection<Player> targets = args.getUnchecked("Targets");

						int limit;
						switch (limitArgType) {
							case "const" -> limit = args.getUnchecked("Limit");
							case "score" -> {
								String limitHolder = args.getUnchecked("LimitHolder");
								Objective limitObjective = args.getUnchecked("LimitObjective");
								limit = Math.max(0, ScoreboardUtils.getScoreboardValue(limitHolder, limitObjective).orElse(0));
							}
							default -> limit = 0;
						}

						Integer count;
						switch (countArgType) {
							case "const" -> count = args.getUnchecked("Count");
							case "score" -> {
								String countHolder = args.getUnchecked("CountHolder");
								Objective countObjective = args.getUnchecked("CountObjective");
								count = Math.max(0, ScoreboardUtils.getScoreboardValue(countHolder, countObjective).orElse(0));
							}
							case "reset" -> count = 0;
							default -> count = null;
						}

						run(targets, limit, count);
					})
					.register();
			}
		}
	}

	public static void run(Collection<Player> targets,
	                       int limit,
	                       @Nullable Integer count) {
		for (Player target : targets) {
			ClientModHandler.updateStrikeChests(target, limit, count);
		}
	}
}
