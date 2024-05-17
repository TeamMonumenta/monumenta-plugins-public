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
						for (Player player : (Collection<Player>) args.get("player")) {
							String result = Plugin.getInstance().mPzeroManager.signUp(player, args.getUnchecked("map name"));
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
						for (Player player : (Collection<Player>) args.get("player")) {
							Plugin.getInstance().mPzeroManager.leave(player);
						}
					}),
				new CommandAPICommand("boost")
					.withArguments(
						new EntitySelectorArgument.ManyPlayers("player")
					)
					.executes((sender, args) -> {
						for (Player player : (Collection<Player>) args.get("player")) {
							Plugin.getInstance().mPzeroManager.boost(player);
						}
					}),
				new CommandAPICommand("restoreEnergy")
					.withArguments(
						new EntitySelectorArgument.ManyPlayers("players"),
						new IntegerArgument("amount")
					)
					.executes((sender, args) -> {
						for (Player player : (Collection<Player>) args.get("players")) {
							Plugin.getInstance().mPzeroManager.restoreEnergy(player, args.getUnchecked("amount"), true);
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
					.withOptionalArguments(new IntegerArgument("grace period on landing"))
					.executes((sender, args) -> {
						for (Player player : (Collection<Player>) args.get("players")) {
							Plugin.getInstance().mPzeroManager.launch(player, args.getUnchecked("x"), args.getUnchecked("y"), args.getUnchecked("z"), args.getUnchecked("duration ticks"), args.getOrDefaultUnchecked("grace period on landing", 0));
						}
					})
			)
		.register();
	}
}
