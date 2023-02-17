package com.playmonumenta.plugins.delves;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.FloatArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.ObjectiveArgument;
import dev.jorel.commandapi.arguments.ScoreHolderArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Player;

public class DelvesCommands {

	private static final String COMMAND = "delves";

	public static void register(Plugin plugin) {
		String perms = "monumenta.command.delves";

		String[] delveModNames = new String[DelvesModifier.values().length];
		int i = 0;
		for (DelvesModifier mod : DelvesModifier.values()) {
			delveModNames[i++] = mod.name();
		}

		Argument<?> dungeonArg = new StringArgument("dungeon").includeSuggestions(
			ArgumentSuggestions.strings(info -> DelvesManager.DUNGEONS.toArray(new String[0])));
		Argument<?> delveModArg = new MultiLiteralArgument(delveModNames);

		//this command is the old used to open Delve GUI
		new CommandAPICommand("opendmsgui")
			.withPermission(perms)
			.withArguments(
				new EntitySelectorArgument.OnePlayer("player"),
				dungeonArg)
			.executes((sender, args) -> {
				new DelveCustomInventory((Player) args[0], (String) args[1], true).openInventory((Player) args[0], plugin);
			}).register();

		new CommandAPICommand("openmoderatordmsgui")
			.withPermission(perms)
			.withArguments(
				new EntitySelectorArgument.OnePlayer("moderator"),
				new EntitySelectorArgument.OnePlayer("playerToDebug"),
				dungeonArg)
			.executes((sender, args) -> {
				new DelveCustomInventory((Player) args[1], (String) args[2], true).openInventory((Player) args[0], plugin);
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new MultiLiteralArgument("show"),
				new MultiLiteralArgument("mods"),
				new EntitySelectorArgument.OnePlayer("player"),
				dungeonArg)
			.executes((sender, args) -> {
				new DelveCustomInventory((Player) args[2], (String) args[3], false).openInventory((Player) args[2], plugin);
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new MultiLiteralArgument("utils"),
				new MultiLiteralArgument("hasallpoints"),
				new EntitySelectorArgument.OnePlayer("player")
			).executes((commandSender, args) -> {
				int currentPoint = DelvesUtils.getPlayerTotalDelvePoint(null, (Player) args[2], ServerProperties.getShardName());
				return currentPoint >= DelvesUtils.MAX_DEPTH_POINTS ? 1 : -1;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new MultiLiteralArgument("get"),
				new MultiLiteralArgument("mod"),
				new EntitySelectorArgument.OnePlayer("player"),
				dungeonArg,
				delveModArg
			).executes((commandSender, args) -> {
				return DelvesUtils.stampDelveInfo(commandSender, (Player) args[2], (String) args[3], DelvesModifier.fromName((String) args[4]));
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new MultiLiteralArgument("get"),
				new MultiLiteralArgument("total"),
				new MultiLiteralArgument("points"),
				new EntitySelectorArgument.OnePlayer("player"),
				dungeonArg
			).executes((commandSender, args) -> {
				return DelvesUtils.getPlayerTotalDelvePoint(commandSender, (Player) args[3], (String) args[4]);
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new MultiLiteralArgument("get"),
				new MultiLiteralArgument("total"),
				new MultiLiteralArgument("points"),
				new MultiLiteralArgument("range")
			).executesPlayer((player, args) -> {
				return DelvesUtils.getTotalDelvePointInRange(player, player.getLocation());
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new MultiLiteralArgument("set"),
				new MultiLiteralArgument("mod"),
				new EntitySelectorArgument.ManyPlayers("entity"),
				dungeonArg,
				delveModArg,
				new IntegerArgument("rank", 0, 10)
			).executes((commandSender, args) -> {
				int rank = (Integer) args[5];
				for (Player target : ((Collection<Player>) args[2])) {
					DelvesUtils.setDelvePoint(commandSender, target, (String) args[3], DelvesModifier.fromName((String) args[4]), rank);
				}
				return rank;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new MultiLiteralArgument("set"),
				new MultiLiteralArgument("mod"),
				new EntitySelectorArgument.ManyPlayers("player"),
				dungeonArg,
				delveModArg,
				new MultiLiteralArgument("score"),
				new ScoreHolderArgument.Single("score holder"),
				new ObjectiveArgument("objective")
			).executes((commandSender, args) -> {
				DelvesModifier mod = DelvesModifier.fromName((String) args[4]);
				String scoreHolder = (String) args[6];
				String objective = (String) args[7];
				int rank = ScoreboardUtils.getScoreboardValue(scoreHolder, objective).orElse(0);
				rank = DelvesUtils.getMaxPointAssignable(mod, rank);
				for (Player target : ((Collection<Player>) args[2])) {
					DelvesUtils.setDelvePoint(commandSender, target, (String) args[3], mod, rank);
				}
				return rank;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new MultiLiteralArgument("random"),
				new MultiLiteralArgument("mods"),
				new EntitySelectorArgument.ManyPlayers("player"),
				dungeonArg,
				new IntegerArgument("pointsToAssign", 0)
			).executes((commandSender, args) -> {
				List<Player> otherPlayers = new ArrayList<>((Collection<Player>) args[2]);
				Player firstPlayer = otherPlayers.remove(0);
				DelvesUtils.assignRandomDelvePoints(firstPlayer, (String) args[3], (Integer) args[4]);
				for (Player target : otherPlayers) {
					DelvesUtils.copyDelvePoint(commandSender, firstPlayer, target, (String) args[3]);
				}
				return DelvesUtils.getPlayerTotalDelvePoint(commandSender, firstPlayer, (String) args[3]);
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new MultiLiteralArgument("clear"),
				new MultiLiteralArgument("mods"),
				new EntitySelectorArgument.ManyPlayers("entity")
			).executes((commandSender, args) -> {
				int count = 0;
				for (Player target : ((Collection<Player>) args[2])) {
					count++;
					DelvesUtils.clearDelvePlayerByShard(commandSender, target, ServerProperties.getShardName());
				}
				return count;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new MultiLiteralArgument("clear"),
				new MultiLiteralArgument("mods"),
				new EntitySelectorArgument.ManyPlayers("player"),
				dungeonArg
			).executes((commandSender, args) -> {
				int count = 0;
				for (Player target : ((Collection<Player>) args[2])) {
					count++;
					DelvesUtils.clearDelvePlayerByShard(commandSender, target, (String) args[3]);
				}
				return count;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new MultiLiteralArgument("update"),
				new MultiLiteralArgument("scoreboard"),
				new EntitySelectorArgument.ManyEntities("player")
			).executes((commandSender, args) -> {
				for (Player target : ((Collection<Player>) args[2])) {
					DelvesUtils.updateDelveScoreBoard(target);
				}
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new MultiLiteralArgument("copy"),
				new MultiLiteralArgument("mods"),
				new EntitySelectorArgument.OnePlayer("copy player"),
				dungeonArg,
				new EntitySelectorArgument.ManyPlayers("players to copy")
			).executes((commandSender, args) -> {
				for (Player target : ((Collection<Player>) args[4])) {
					DelvesUtils.copyDelvePoint(commandSender, (Player) args[2], target, (String) args[3]);
				}
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new MultiLiteralArgument("copy"),
				new MultiLiteralArgument("mods"),
				new EntitySelectorArgument.OnePlayer("copy player"),
				new EntitySelectorArgument.ManyPlayers("players to copy")
			).executes((commandSender, args) -> {
				for (Player target : ((Collection<Player>) args[3])) {
					DelvesUtils.copyDelvePoint(commandSender, (Player) args[2], target, ServerProperties.getShardName());
				}
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new MultiLiteralArgument("validate"),
				new EntitySelectorArgument.OnePlayer("player"),
				dungeonArg
			).executes((commandSender, args) -> {
				Player player = (Player) args[1];
				String dungeon = (String) args[2];
				return DelvesManager.validateDelvePreset(player, dungeon) ? 1 : 0;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new LiteralArgument("set"),
				new LiteralArgument("preset"),
				new EntitySelectorArgument.OnePlayer("player"),
				dungeonArg,
				new GreedyStringArgument("preset").replaceSuggestions(ArgumentSuggestions.strings(Arrays.stream(DelvePreset.values()).map(p -> p.mName).toArray(String[]::new)))
			).executes((commandSender, args) -> {
				Player player = (Player) args[0];
				String dungeon = (String) args[1];
				DelvePreset preset = DelvePreset.getDelvePreset((String) args[2]);
				if (preset == null) {
					throw CommandAPI.failWithString("Unknown preset '" + args[2] + "'");
				}
				DelvesManager.savePlayerData(player, dungeon, preset.mModifiers, preset.mId);
				if (commandSender != null && !(commandSender instanceof ProxiedCommandSender)) {
					commandSender.sendMessage(Component.text("Applied preset '" + preset.mName + "' to " + player.getName() + " for shard " + dungeon, NamedTextColor.GOLD));
				}
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new MultiLiteralArgument("spawnercheck"),
				new EntitySelectorArgument.OnePlayer("player"),
				new FloatArgument("percentage", 0, 100)
			).executes((commandSender, args) -> {
				Player player = (Player) args[1];
				float percentage = (Float) args[2];
				int broken = DelvesManager.getSpawnersBroken(player.getWorld());
				int total = DelvesManager.getSpawnersTotal(player.getWorld());
				if (broken < 0 || total < 0) {
					return 0;
				} else if (broken >= (percentage / 100.0) * total) {
					return 1;
				} else {
					return 0;
				}
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new MultiLiteralArgument("setbrokenspawners"),
				new EntitySelectorArgument.OnePlayer("player"),
				new IntegerArgument("broken", 0)
			).executes((commandSender, args) -> {
				Player player = (Player) args[1];
				int broken = (int) args[2];
				DelvesManager.setSpawnersBroken(player.getWorld(), broken);
				commandSender.sendMessage(Component.text("Set broken spawner count to " + broken, NamedTextColor.GOLD));
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new MultiLiteralArgument("debugchallenge"),
				new EntitySelectorArgument.OnePlayer("player"),
				dungeonArg
			).executes((commandSender, args) -> {
				Player player = (Player) args[1];
				String dungeon = (String) args[2];
				int broken = DelvesManager.getSpawnersBroken(player.getWorld());
				int total = DelvesManager.getSpawnersTotal(player.getWorld());
				boolean isChallenge = DelvesManager.validateDelvePreset(player, dungeon);
				if (!isChallenge) {
					commandSender.sendMessage(Component.text("Instance is not a challenge delve", NamedTextColor.RED));
					return;
				}
				commandSender.sendMessage(Component.text("Instance is a challenge delve", NamedTextColor.GREEN));
				commandSender.sendMessage(Component.text(String.format("Spawners broken: %d / %d (%d %%)", broken, total, 100 * broken / total), NamedTextColor.GOLD));
			}).register();
	}

}
