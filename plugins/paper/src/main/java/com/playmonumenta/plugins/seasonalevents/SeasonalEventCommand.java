package com.playmonumenta.plugins.seasonalevents;

import com.playmonumenta.plugins.commands.GenericCommand;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class SeasonalEventCommand extends GenericCommand {

	public static void register() {

		CommandPermission perms = CommandPermission.fromString("monumenta.command.battlepass");

		// Add battlepass reload command
		List<Argument> arguments = new ArrayList<>();
		arguments.add(new MultiLiteralArgument("reload"));

		new CommandAPICommand("battlepass")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				SeasonalEventManager.reloadPasses(sender);
			})
			.register();

		// Add battlepass xp command
		arguments.clear();
		arguments.add(new MultiLiteralArgument("addxp"));
		arguments.add(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER));
		arguments.add(new IntegerArgument("amount"));

		new CommandAPICommand("battlepass")
		.withPermission(perms)
		.withArguments(arguments)
		.executes((sender, args) -> {
			Player player = (Player) args[1];

			SeasonalEventManager.addMP(player, (int) args[2]);
		})
		.register();

		// Set battlepass xp command

		arguments.clear();
		arguments.add(new MultiLiteralArgument("setxp"));
		arguments.add(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER));
		arguments.add(new IntegerArgument("amount"));

		new CommandAPICommand("battlepass")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = (Player) args[1];

				SeasonalEventManager.setMP(player, (int) args[2]);
			})
			.register();

		// Get current xp of player

		arguments.clear();
		arguments.add(new MultiLiteralArgument("getxp"));
		arguments.add(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER));

		new CommandAPICommand("battlepass")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = (Player) args[1];
				Player senderPlayer = null;
				if (sender instanceof Player) {
					senderPlayer = (Player) sender;
				}
				if (senderPlayer != null) {
					senderPlayer.sendMessage("" + SeasonalEventManager.getMP(player));
				}
			})
			.register();

		//Battlepass GUI command

		arguments.clear();
		arguments.add(new MultiLiteralArgument("gui"));
		arguments.add(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER));

		new CommandAPICommand("battlepass")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = (Player) args[1];
				@Nullable SeasonalPass seasonalPass = SeasonalEventManager.mActivePass;
				if (seasonalPass == null || !seasonalPass.isActive()) {
					player.sendMessage(Component.text("Could not load the current season pass", NamedTextColor.RED));
					return;
				}
				new SeasonalEventGUI(seasonalPass, player).open();
			}).register();

		//GUI command with specific week
		arguments.clear();
		arguments.add(new MultiLiteralArgument("gui"));
		arguments.add(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER));
		arguments.add(new MultiLiteralArgument("week"));
		arguments.add(new IntegerArgument("week"));

		new CommandAPICommand("battlepass")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = (Player) args[1];
				@Nullable SeasonalPass seasonalPass = SeasonalEventManager.mActivePass;
				if (seasonalPass == null || !seasonalPass.isActive()) {
					player.sendMessage(Component.text("Could not load the current season pass", NamedTextColor.RED));
					return;
				}
				new SeasonalEventGUI(seasonalPass, player, (int) args[3]).open();
			}).register();

		//GUI command with specific date
		arguments.clear();
		arguments.add(new MultiLiteralArgument("gui"));
		arguments.add(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER));
		arguments.add(new MultiLiteralArgument("date"));
		arguments.add(new IntegerArgument("year", 2022));
		arguments.add(new IntegerArgument("month", 1, 12));
		arguments.add(new IntegerArgument("day", 1, 31));

		new CommandAPICommand("battlepass")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = (Player) args[1];
				int year = (Integer) args[3];
				int month = (Integer) args[4];
				int dayOfMonth = (Integer) args[5];
				LocalDateTime dateTime = LocalDateTime.of(year, month, dayOfMonth, 0, 0);

				@Nullable SeasonalPass seasonalPass = SeasonalEventManager.getPass(dateTime);
				if (seasonalPass == null) {
					player.sendMessage(Component.text("Could not load the specified season pass", NamedTextColor.RED));
					return;
				}
				int week = seasonalPass.getWeekOfPass(dateTime);
				new SeasonalEventGUI(seasonalPass, player, week).open();
			}).register();
	}
}
