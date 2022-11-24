package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.gui.PlayerItemStatsGUI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.PlayerArgument;
import org.bukkit.entity.Player;

public class PlayerItemStatsGUICommand {
	public static void register(Plugin plugin) {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.playerstats");

		new CommandAPICommand("playerstats").withPermission(perms).withAliases("ps")
			.executesPlayer((sender, args) -> {
				new PlayerItemStatsGUI(sender).openInventory(sender, plugin);
			}).register();

		new CommandAPICommand("playerstats").withPermission(perms).withAliases("ps")
			.withArguments(new PlayerArgument("other player"))
			.executesPlayer((sender, args) -> {
				Player otherPlayer = (Player) args[0];
				new PlayerItemStatsGUI(sender, otherPlayer).openInventory(sender, plugin);
			}).register();

		new CommandAPICommand("playerstats").withPermission(perms).withAliases("ps")
			.withArguments(new PlayerArgument("player1"), new PlayerArgument("player2"))
			.executesPlayer((sender, args) -> {
				Player player1 = (Player) args[0];
				Player player2 = (Player) args[1];
				new PlayerItemStatsGUI(player1, player2).openInventory(sender, plugin);
			}).register();

	}
}
