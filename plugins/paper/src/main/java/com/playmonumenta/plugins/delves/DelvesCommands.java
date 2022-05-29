package com.playmonumenta.plugins.delves;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import java.util.Collection;
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

		Argument dungeonArg = new StringArgument("dungeon").includeSuggestions(info -> DelvesManager.DUNGEONS.toArray(new String[0]));
		Argument delveModArg = new MultiLiteralArgument(delveModNames);

		//this command is the old used to open Delve GUI
		new CommandAPICommand("opendmsgui")
			.withPermission(perms)
			.withArguments(
				new EntitySelectorArgument("player", EntitySelectorArgument.EntitySelector.ONE_PLAYER),
				dungeonArg)
			.executes((sender, args) -> {
				new DelveCustomInventory((Player) args[0], (String) args[1], true).openInventory((Player) args[0], plugin);
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new MultiLiteralArgument("show"),
				new MultiLiteralArgument("mods"),
				new EntitySelectorArgument("player", EntitySelectorArgument.EntitySelector.ONE_PLAYER),
				dungeonArg)
			.executes((sender, args) -> {
				new DelveCustomInventory((Player) args[2], (String) args[3], false).openInventory((Player) args[2], plugin);
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new MultiLiteralArgument("utils"),
				new MultiLiteralArgument("hasallpoints"),
				new EntitySelectorArgument("player", EntitySelectorArgument.EntitySelector.ONE_PLAYER)
			).executes((commandSender, args) -> {
				int currentPoint = DelvesUtils.getPlayerTotalDelvePoint(null, (Player) args[2], ServerProperties.getShardName());
				return currentPoint == DelvesUtils.MAX_DEPTH_POINTS ? 1 : -1;
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new MultiLiteralArgument("get"),
				new MultiLiteralArgument("mod"),
				new EntitySelectorArgument("player", EntitySelectorArgument.EntitySelector.ONE_PLAYER),
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
				new EntitySelectorArgument("player", EntitySelectorArgument.EntitySelector.ONE_PLAYER),
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
				new EntitySelectorArgument("player", EntitySelectorArgument.EntitySelector.ONE_PLAYER),
				dungeonArg,
				delveModArg,
				new IntegerArgument("rank", 0, 10)
			).executes((commandSender, args) -> {
				return DelvesUtils.setDelvePoint(commandSender, (Player) args[2], (String) args[3], DelvesModifier.fromName((String) args[4]), (Integer) args[5]);
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new MultiLiteralArgument("set"),
				new MultiLiteralArgument("mod"),
				new EntitySelectorArgument("player", EntitySelectorArgument.EntitySelector.ONE_PLAYER),
				dungeonArg,
				delveModArg,
				new IntegerArgument("rank", 0, 10)
			).executes((commandSender, args) -> {
				return DelvesUtils.setDelvePoint(commandSender, (Player) args[2], (String) args[3], DelvesModifier.fromName((String) args[4]), (Integer) args[5]);
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new MultiLiteralArgument("clear"),
				new MultiLiteralArgument("mods"),
				new EntitySelectorArgument("player", EntitySelectorArgument.EntitySelector.ONE_PLAYER)
			).executes((commandSender, args) -> {
				DelvesUtils.clearDelvePlayerByShard(commandSender, (Player) args[2], ServerProperties.getShardName());
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new MultiLiteralArgument("clear"),
				new MultiLiteralArgument("mods"),
				new EntitySelectorArgument("player", EntitySelectorArgument.EntitySelector.ONE_PLAYER),
				dungeonArg
			).executes((commandSender, args) -> {
				DelvesUtils.clearDelvePlayerByShard(commandSender, (Player) args[2], (String) args[3]);
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new MultiLiteralArgument("update"),
				new MultiLiteralArgument("scoreboard"),
				new EntitySelectorArgument("player", EntitySelectorArgument.EntitySelector.ONE_PLAYER)
			).executes((commandSender, args) -> {
				DelvesUtils.updateDelveScoreBoard((Player) args[2]);
			}).register();

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(
				new MultiLiteralArgument("copy"),
				new MultiLiteralArgument("mods"),
				new EntitySelectorArgument("copy player", EntitySelectorArgument.EntitySelector.ONE_PLAYER),
				dungeonArg,
				new EntitySelectorArgument("players to copy", EntitySelectorArgument.EntitySelector.MANY_PLAYERS)
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
				new EntitySelectorArgument("copy player", EntitySelectorArgument.EntitySelector.ONE_PLAYER),
				new EntitySelectorArgument("players to copy", EntitySelectorArgument.EntitySelector.MANY_PLAYERS)
			).executes((commandSender, args) -> {
				for (Player target : ((Collection<Player>) args[3])) {
					DelvesUtils.copyDelvePoint(commandSender, (Player) args[2], target, ServerProperties.getShardName());
				}
			}).register();
	}

}
