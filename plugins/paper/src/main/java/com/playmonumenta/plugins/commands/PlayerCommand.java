package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.custominventories.PEBCustomInventory;
import com.playmonumenta.plugins.custominventories.PlayerDisplayCustomInventory;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.*;
import dev.jorel.commandapi.executors.PlayerCommandExecutor;
import org.bukkit.entity.Player;


public class PlayerCommand {

	public static void register(Plugin plugin) {
		new CommandAPICommand("player")
			.withPermission("monumenta.command.player")
			.withSubcommand(new CommandAPICommand("details")
				.withPermission("monumenta.command.player.details")
				.executesPlayer((PlayerCommandExecutor) (player, args) ->
					new PlayerDisplayCustomInventory(player, player).openInventory(player, plugin)
				)
			)
			.withSubcommand(new CommandAPICommand("details")
				.withPermission("monumenta.command.player.details.other")
				.withArguments(new EntitySelectorArgument.OnePlayer("player"))
				.executes((sender, args) -> {
					Player player = (Player) args[0];
					new PlayerDisplayCustomInventory((Player) sender, player).openInventory((Player) sender, plugin);
				})
			)
			.withSubcommand(new CommandAPICommand("settings")
				.withPermission("monumenta.command.player.settings")
				.executesPlayer((PlayerCommandExecutor) (player, args) ->
					new PEBCustomInventory(player).openInventory(player, plugin)
				)
				.withSubcommand(new CommandAPICommand("sound")
					.withPermission("monumenta.command.player.settings.sound")
					.executesPlayer((PlayerCommandExecutor) (player, args) ->
						new PEBCustomInventory(player, PEBCustomInventory.PebPage.SOUND_CONTROLS).openInventory(player, plugin)
					)
				)
				.withSubcommand(new CommandAPICommand("particles")
					.withPermission("monumenta.command.player.settings.particles")
					.executesPlayer((PlayerCommandExecutor) (player, args) ->
						new PEBCustomInventory(player, PEBCustomInventory.PebPage.PARTIAL_PARTICLES).openInventory(player, plugin)
					)
				)
			)
			.withSubcommand(new CommandAPICommand("status")
				.withPermission("monumenta.command.player.status")
				.withSubcommand(new CommandAPICommand("dailies")
					.withPermission("monumenta.command.player.status.dailies")
					.executesPlayer((PlayerCommandExecutor) (player, args) ->
						player.performCommand("clickable peb_dailies")
					)
				)
				.withSubcommand(new CommandAPICommand("dungeons")
					.withPermission("monumenta.command.player.status.dungeons")
					.executesPlayer((PlayerCommandExecutor) (player, args) ->
						player.performCommand("clickable peb_dungeoninfo")
					)
				)
			)
			.register();
	}

}
