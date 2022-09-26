package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.ScoreboardUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.LiteralArgument;
import org.bukkit.ChatColor;

public class RocketJump {
	private static final String SCB_NAME = "RocketJumper";

	public static void register() {

		new CommandAPICommand("RocketJump")
			.withPermission("monumenta.rocketjump")
			.withAliases("rj")
			.executesPlayer((player, args) -> {
				if (ScoreboardUtils.getScoreboardValue(player, SCB_NAME).get() == 0) {
					ScoreboardUtils.setScoreboardValue(player, SCB_NAME, 1);
					player.sendMessage(ChatColor.GOLD + "Rocket jump: " + ChatColor.AQUA + "Enable yours");
				} else {
					ScoreboardUtils.setScoreboardValue(player, SCB_NAME, 0);
					player.sendMessage(ChatColor.GOLD + "Rocket jump: " + ChatColor.RED + "Disabled");
				}
			})
			.register();

		new CommandAPICommand("RocketJump")
			.withPermission("monumenta.rocketjump")
			.withAliases("rj")
			.withArguments(new LiteralArgument("none"))
			.executesPlayer((player, args) -> {
				ScoreboardUtils.setScoreboardValue(player, SCB_NAME, 0);
				player.sendMessage(ChatColor.GOLD + "Rocket jump: " + ChatColor.RED + "Disabled");
			})
			.register();

		new CommandAPICommand("RocketJump")
			.withPermission("monumenta.rocketjump")
			.withAliases("rj")
			.withArguments(new LiteralArgument("self"))
			.executesPlayer((player, args) -> {
				ScoreboardUtils.setScoreboardValue(player, SCB_NAME, 1);
				player.sendMessage(ChatColor.GOLD + "Rocket jump: " + ChatColor.AQUA + "Enable yours");
			})
			.register();

		new CommandAPICommand("RocketJump")
			.withPermission("monumenta.rocketjump")
			.withAliases("rj")
			.withArguments(new LiteralArgument("all"))
			.executesPlayer((player, args) -> {
				ScoreboardUtils.setScoreboardValue(player, SCB_NAME, 100);
				player.sendMessage(ChatColor.GOLD + "Rocket jump: " + ChatColor.GREEN + "Enable all");
			})
			.register();

	}
}
