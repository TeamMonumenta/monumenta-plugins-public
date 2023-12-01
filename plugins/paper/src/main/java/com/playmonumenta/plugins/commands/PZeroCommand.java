package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.minigames.pzero.PzeroMap;
import com.playmonumenta.plugins.utils.MessagingUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import java.util.Arrays;
import java.util.Collection;
import org.bukkit.entity.Player;

public class PZeroCommand {
	@SuppressWarnings("unchecked")
	public static void register() {
		new CommandAPICommand("pzero")
			.withPermission("monumenta.command.pzero")
			.withSubcommands(
				new CommandAPICommand("join")
					.withArguments(
						new EntitySelectorArgument.ManyPlayers("player"),
						new TextArgument("map name").includeSuggestions(
							ArgumentSuggestions.stringCollection(info -> Arrays.stream(PzeroMap.values())
								.filter(pzMap -> !pzMap.getName().equals(PzeroMap.MAP_NULL.getName()))
								.map(pzMap -> "\"" + pzMap.getName() + "\"").toList()
							)
						)
					)
					.executes((sender, args) -> {
						for (Player player : (Collection<Player>) args[0]) {
							String result = Plugin.getInstance().mPzeroManager.signUp(player, (String) args[1]);
							if (result != null) {
								player.sendMessage(MessagingUtils.fromMiniMessage("<red>" + result + "</red>"));
							}
						}
					}),
				new CommandAPICommand("leave")
					.withArguments(
						new EntitySelectorArgument.ManyPlayers("player")
					)
					.executes((sender, args) -> {
						for (Player player : (Collection<Player>) args[0]) {
							Plugin.getInstance().mPzeroManager.leave(player);
						}
					}),
				new CommandAPICommand("boost")
					.withArguments(
						new EntitySelectorArgument.ManyPlayers("player")
					)
					.executes((sender, args) -> {
						for (Player player : (Collection<Player>) args[0]) {
							Plugin.getInstance().mPzeroManager.boost(player);
						}
					}),
				new CommandAPICommand("restoreEnergy")
					.withArguments(
						new EntitySelectorArgument.ManyPlayers("players"),
						new IntegerArgument("amount")
					)
					.executes((sender, args) -> {
						for (Player player : (Collection<Player>) args[0]) {
							Plugin.getInstance().mPzeroManager.restoreEnergy(player, (int) args[1], true);
						}
					}),
				new CommandAPICommand("launch")
					.withArguments(
						new EntitySelectorArgument.ManyPlayers("players"),
						new DoubleArgument("x"),
						new DoubleArgument("y"),
						new DoubleArgument("z"),
						new IntegerArgument("duration ticks")
					)
					.executes((sender, args) -> {
						for (Player player : (Collection<Player>) args[0]) {
							Plugin.getInstance().mPzeroManager.launch(player, (double) args[1], (double) args[2], (double) args[3], (int) args[4], 0);
						}
					}),
				new CommandAPICommand("launch")
					.withArguments(
						new EntitySelectorArgument.ManyPlayers("players"),
						new DoubleArgument("x"),
						new DoubleArgument("y"),
						new DoubleArgument("z"),
						new IntegerArgument("duration ticks"),
						new IntegerArgument("grace period on landing")
					)
					.executes((sender, args) -> {
						for (Player player : (Collection<Player>) args[0]) {
							Plugin.getInstance().mPzeroManager.launch(player, (double) args[1], (double) args[2], (double) args[3], (int) args[4], (int) args[5]);
						}
					})
			)
		.register();
	}
}
