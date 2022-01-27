package com.playmonumenta.plugins.seasonalevents;

import com.playmonumenta.plugins.commands.GenericCommand;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class SeasonalEventCommand extends GenericCommand {

	public static void register(Plugin plugin) {

		CommandPermission perms = CommandPermission.fromString("monumenta.command.battlepass");

		// Add battlepass xp command
		List<Argument> arguments = new ArrayList<>();
		arguments.add(new LiteralArgument("addxp"));
		arguments.add(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER));
		arguments.add(new IntegerArgument("amount"));

		new CommandAPICommand("battlepass")
		.withPermission(perms)
		.withArguments(arguments)
		.executes((sender, args) -> {
			Player player = (Player) args[0];

			SeasonalEventManager.addMP(player, (int) args[1]);
		})
		.register();

		// Set battlepass xp command

		arguments.clear();
		arguments.add(new LiteralArgument("setxp"));
		arguments.add(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER));
		arguments.add(new IntegerArgument("amount"));

		new CommandAPICommand("battlepass")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = (Player) args[0];

				SeasonalEventManager.setMP(player, (int) args[1]);
			})
			.register();

		// Get current xp of player

		arguments.clear();
		arguments.add(new LiteralArgument("getxp"));
		arguments.add(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER));

		new CommandAPICommand("battlepass")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = (Player) args[0];

				player.sendMessage("" + SeasonalEventManager.getMP(player));
			})
			.register();

		//Battlepass GUI command

		arguments.clear();
		arguments.add(new LiteralArgument("gui"));
		arguments.add(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER));

		new CommandAPICommand("battlepass")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				new SeasonalEventGUI(player).openInventory(player, plugin);
			}).register();

		//GUI command with specific week
		arguments.clear();
		arguments.add(new LiteralArgument("gui"));
		arguments.add(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER));
		arguments.add(new IntegerArgument("week"));

		new CommandAPICommand("battlepass")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				new SeasonalEventGUI(player, (int) args[1]).openInventory(player, plugin);
			}).register();
	}
}
