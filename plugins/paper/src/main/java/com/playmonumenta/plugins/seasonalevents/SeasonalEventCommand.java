package com.playmonumenta.plugins.seasonalevents;

import com.playmonumenta.plugins.commands.GenericCommand;
import com.playmonumenta.plugins.seasonalevents.community.CommunityMissionManager;
import com.playmonumenta.plugins.seasonalevents.community.CommunityMissionsGui;
import com.playmonumenta.plugins.seasonalevents.gui.PassGui;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.DateUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.LongArgument;
import dev.jorel.commandapi.arguments.StringArgument;
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
		CommandPermission permsCommunityMissions = CommandPermission.fromString("monumenta.command.communitymissions");

		// Add battlepass reload command
		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(new LiteralArgument("reload"));

		new CommandAPICommand("battlepass")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				SeasonalEventManager.reloadPasses(sender);
			})
			.register();

		// Get current xp of player

		arguments.clear();
		arguments.add(new LiteralArgument("getxp"));
		arguments.add(new EntitySelectorArgument.OnePlayer("player"));

		new CommandAPICommand("battlepass")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = args.getUnchecked("player");
				sender.sendMessage(Component.text(SeasonalEventManager.getMP(player)));
			})
			.register();

		//Battlepass GUI command

		arguments.clear();
		arguments.add(new LiteralArgument("gui"));
		arguments.add(new EntitySelectorArgument.OnePlayer("player"));

		new CommandAPICommand("battlepass")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = args.getUnchecked("player");
				LocalDateTime now = DateUtils.localDateTime();
				@Nullable SeasonalPass seasonalPass = SeasonalEventManager.getMostRecentPass();
				if (seasonalPass == null) {
					player.sendMessage(Component.text("Could not load a season pass", NamedTextColor.RED));
					return;
				}
				new PassGui(seasonalPass, player, player, now, false).open();
			}).register();

		arguments.add(new LiteralArgument("view"));
		arguments.add(new EntitySelectorArgument.OnePlayer("other"));

		new CommandAPICommand("battlepass")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = args.getUnchecked("player");
				Player otherPlayer = args.getUnchecked("other");
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
		arguments.add(new LiteralArgument("gui"));
		arguments.add(new EntitySelectorArgument.OnePlayer("player"));
		arguments.add(new LiteralArgument("week"));
		arguments.add(new IntegerArgument("week"));

		new CommandAPICommand("battlepass")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = args.getUnchecked("player");
				int week = args.getUnchecked("week");
				@Nullable SeasonalPass seasonalPass = SeasonalEventManager.getMostRecentPass();
				if (seasonalPass == null) {
					player.sendMessage(Component.text("Could not load a season pass", NamedTextColor.RED));
					return;
				}
				LocalDateTime dateTime = seasonalPass.mPassStart.plusWeeks(week - 1);
				new PassGui(seasonalPass, player, player, dateTime, false).open();
			}).register();

		//GUI command with specific date
		arguments.clear();
		arguments.add(new LiteralArgument("gui"));
		arguments.add(new EntitySelectorArgument.OnePlayer("player"));
		arguments.add(new LiteralArgument("date"));
		arguments.add(new IntegerArgument("year", 2022));
		arguments.add(new IntegerArgument("month", 1, 12));
		arguments.add(new IntegerArgument("day", 1, 31));

		new CommandAPICommand("battlepass")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = args.getUnchecked("player");
				int year = args.getUnchecked("year");
				int month = args.getUnchecked("month");
				int dayOfMonth = args.getUnchecked("day");
				LocalDateTime dateTime = LocalDateTime.of(year, month, dayOfMonth, 0, 0);

				@Nullable SeasonalPass seasonalPass = SeasonalEventManager.getMostRecentPass();
				if (seasonalPass == null) {
					player.sendMessage(Component.text("Could not load the specified season pass", NamedTextColor.RED));
					return;
				}
				new PassGui(seasonalPass, player, player, dateTime, false).open();
			}).register();

		arguments.clear();
		arguments.add(new LiteralArgument("modgui"));
		arguments.add(new EntitySelectorArgument.OnePlayer("player"));
		arguments.add(new EntitySelectorArgument.OnePlayer("other"));

		new CommandAPICommand("battlepass")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = args.getUnchecked("player");
				Player otherPlayer = args.getUnchecked("other");
				LocalDateTime now = DateUtils.localDateTime();
				@Nullable SeasonalPass seasonalPass = SeasonalEventManager.getMostRecentPass();
				if (seasonalPass == null) {
					player.sendMessage(Component.text("Could not load a season pass", NamedTextColor.RED));
					return;
				}
				new PassGui(seasonalPass, player, otherPlayer, now, true).open();
			}).register();


		// new community mission commands

		// default command for opening gui for player
		CommandAPICommand communityGui = new CommandAPICommand("gui")
			.executes((sender, args) -> {
				Player player = CommandUtils.getPlayerFromSender(sender);
				if (sender instanceof Player senderPlayer && !senderPlayer.equals(player)) {
					// cant open it on other people as a player
					return;
				}
				new CommunityMissionsGui(player).open();
			});

		// debug command to set total contributions for mission
		CommandAPICommand communityDebugSetTotal = new CommandAPICommand("setTotal")
			.withPermission(permsCommunityMissions)
			.withArguments(new IntegerArgument("index", 1, 3))
			.withArguments(new LongArgument("amount", 0))
			.executes((sender, args) -> {
				int index = (int) args.getUnchecked("index") - 1;
				long amount = args.getUnchecked("amount");

				boolean success = CommunityMissionManager.getInstance().setTotalContribution(index, amount);
				if (success) {
					sender.sendMessage(Component.text("Set Mission " + (index + 1) + " total to " + amount, NamedTextColor.GREEN));
				} else {
					sender.sendMessage(Component.text("Failed: No active community event found.", NamedTextColor.RED));
				}
			});

		// debug command to set personal contributions for mission
		CommandAPICommand communityDebugSetPersonal = new CommandAPICommand("setPersonal")
			.withPermission(permsCommunityMissions)
			.withArguments(new EntitySelectorArgument.OnePlayer("target"))
			.withArguments(new IntegerArgument("index", 1, 3))
			.withArguments(new LongArgument("amount", 0))
			.executes((sender, args) -> {
				Player target = args.getUnchecked("target");
				int index = (int) args.getUnchecked("index") - 1;
				long amount = args.getUnchecked("amount");

				boolean success = CommunityMissionManager.getInstance().setPlayerContribution(index, target.getUniqueId(), amount);
				if (success) {
					sender.sendMessage(Component.text("Set " + target.getName() + "'s contribution for Mission " + (index + 1) + " to " + amount + " (and updated total)", NamedTextColor.GREEN));
				} else {
					sender.sendMessage(Component.text("Failed: No active community event found.", NamedTextColor.RED));
				}
			});

		// internal command (similar to how the hunts warnings work i think)
		CommandAPICommand communityDebugInternalBroadcast = new CommandAPICommand("internalbroadcast")
			.withPermission(permsCommunityMissions)
			.withArguments(new StringArgument("type"))
			.withArguments(new GreedyStringArgument("data"))
			.executes((sender, args) -> {
				String type = args.getUnchecked("type");
				String data = args.getUnchecked("data");
				String[] parts = data.split(" ");

				CommunityMissionManager.getInstance().handleIncomingAlert(type, parts);
			});

		new CommandAPICommand("communitymissions")
			.withSubcommand(communityGui)
			.withSubcommand(communityDebugSetPersonal)
			.withSubcommand(communityDebugSetTotal)
			.withSubcommand(communityDebugInternalBroadcast)
			.register();
	}
}
