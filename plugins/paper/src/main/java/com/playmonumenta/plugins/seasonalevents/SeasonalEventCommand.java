package com.playmonumenta.plugins.seasonalevents;

import com.playmonumenta.plugins.commands.GenericCommand;
import com.playmonumenta.plugins.seasonalevents.gui.PassGui;
import com.playmonumenta.plugins.utils.DateUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
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
		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(new MultiLiteralArgument("reload"));

		new CommandAPICommand("battlepass")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				SeasonalEventManager.reloadPasses(sender);
			})
			.register();

		// Get current xp of player

		arguments.clear();
		arguments.add(new MultiLiteralArgument("getxp"));
		arguments.add(new EntitySelectorArgument.OnePlayer("player"));

		new CommandAPICommand("battlepass")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = (Player) args[1];
				if (sender instanceof Player senderPlayer) {
					senderPlayer.sendMessage(Component.text(SeasonalEventManager.getMP(player)));
				}
			})
			.register();

		//Battlepass GUI command

		arguments.clear();
		arguments.add(new MultiLiteralArgument("gui"));
		arguments.add(new EntitySelectorArgument.OnePlayer("player"));

		new CommandAPICommand("battlepass")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = (Player) args[1];
				LocalDateTime now = DateUtils.localDateTime();
				@Nullable SeasonalPass seasonalPass = SeasonalEventManager.getMostRecentPass();
				if (seasonalPass == null) {
					player.sendMessage(Component.text("Could not load a season pass", NamedTextColor.RED));
					return;
				}
				new PassGui(seasonalPass, player, player, now, false).open();
			}).register();

		arguments.add(new MultiLiteralArgument("view"));
		arguments.add(new EntitySelectorArgument.OnePlayer("other"));

		new CommandAPICommand("battlepass")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = (Player) args[1];
				Player otherPlayer = (Player) args[3];
				LocalDateTime now = DateUtils.localDateTime();
				@Nullable SeasonalPass seasonalPass = SeasonalEventManager.getMostRecentPass();
				if (seasonalPass == null) {
					player.sendMessage(Component.text("Could not load a season pass", NamedTextColor.RED));
					return;
				}
				new PassGui(seasonalPass, player, otherPlayer, now, false).open();
			}).register();

		//GUI command with specific week
		arguments.clear();
		arguments.add(new MultiLiteralArgument("gui"));
		arguments.add(new EntitySelectorArgument.OnePlayer("player"));
		arguments.add(new MultiLiteralArgument("week"));
		arguments.add(new IntegerArgument("week"));

		new CommandAPICommand("battlepass")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = (Player) args[1];
				int week = (int) args[3];
				@Nullable SeasonalPass seasonalPass = SeasonalEventManager.getMostRecentPass();
				if (seasonalPass == null) {
					player.sendMessage(Component.text("Could not load a season pass", NamedTextColor.RED));
					return;
				}
				LocalDateTime dateTime = seasonalPass.mPassStart.plus(week - 1, ChronoUnit.WEEKS);
				new PassGui(seasonalPass, player, player, dateTime, false).open();
			}).register();

		//GUI command with specific date
		arguments.clear();
		arguments.add(new MultiLiteralArgument("gui"));
		arguments.add(new EntitySelectorArgument.OnePlayer("player"));
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
				new PassGui(seasonalPass, player, player, dateTime, false).open();
			}).register();

		arguments.clear();
		arguments.add(new MultiLiteralArgument("modgui"));
		arguments.add(new EntitySelectorArgument.OnePlayer("player"));
		arguments.add(new EntitySelectorArgument.OnePlayer("other"));

		new CommandAPICommand("battlepass")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = (Player) args[1];
				Player otherPlayer = (Player) args[2];
				LocalDateTime now = DateUtils.localDateTime();
				@Nullable SeasonalPass seasonalPass = SeasonalEventManager.getMostRecentPass();
				if (seasonalPass == null) {
					player.sendMessage(Component.text("Could not load a season pass", NamedTextColor.RED));
					return;
				}
				new PassGui(seasonalPass, player, otherPlayer, now, true).open();
			}).register();
	}
}
