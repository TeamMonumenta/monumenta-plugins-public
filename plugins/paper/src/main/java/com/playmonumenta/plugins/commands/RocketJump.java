package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.abilities.alchemist.UnstableAmalgam;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.LiteralArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class RocketJump {

	public static void register() {

		new CommandAPICommand("RocketJump")
			.withPermission("monumenta.rocketjump")
			.withAliases("rj")
			.executesPlayer((player, args) -> {
				if (ScoreboardUtils.getScoreboardValue(player, UnstableAmalgam.ROCKET_JUMP_OBJECTIVE).orElse(0) == 0) {
					ScoreboardUtils.setScoreboardValue(player, UnstableAmalgam.ROCKET_JUMP_OBJECTIVE, 1);
					player.sendMessage(Component.text("Rocket jump: ", NamedTextColor.GOLD).append(Component.text("Enable yours", NamedTextColor.AQUA)));
				} else {
					ScoreboardUtils.setScoreboardValue(player, UnstableAmalgam.ROCKET_JUMP_OBJECTIVE, 0);
					player.sendMessage(Component.text("Rocket jump: ", NamedTextColor.GOLD).append(Component.text("Disabled", NamedTextColor.AQUA)));
				}
			})
			.register();

		new CommandAPICommand("RocketJump")
			.withPermission("monumenta.rocketjump")
			.withAliases("rj")
			.withArguments(new LiteralArgument("none"))
			.executesPlayer((player, args) -> {
				ScoreboardUtils.setScoreboardValue(player, UnstableAmalgam.ROCKET_JUMP_OBJECTIVE, 0);
				player.sendMessage(Component.text("Rocket jump: ", NamedTextColor.GOLD).append(Component.text("Disabled", NamedTextColor.AQUA)));
			})
			.register();

		new CommandAPICommand("RocketJump")
			.withPermission("monumenta.rocketjump")
			.withAliases("rj")
			.withArguments(new LiteralArgument("self"))
			.executesPlayer((player, args) -> {
				ScoreboardUtils.setScoreboardValue(player, UnstableAmalgam.ROCKET_JUMP_OBJECTIVE, 1);
				player.sendMessage(Component.text("Rocket jump: ", NamedTextColor.GOLD).append(Component.text("Enable yours", NamedTextColor.AQUA)));
			})
			.register();

		new CommandAPICommand("RocketJump")
			.withPermission("monumenta.rocketjump")
			.withAliases("rj")
			.withArguments(new LiteralArgument("all"))
			.executesPlayer((player, args) -> {
				ScoreboardUtils.setScoreboardValue(player, UnstableAmalgam.ROCKET_JUMP_OBJECTIVE, 100);
				player.sendMessage(Component.text("Rocket jump: ", NamedTextColor.GOLD).append(Component.text("Enable all", NamedTextColor.AQUA)));
			})
			.register();

	}
}
