package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.PlayerItemStatsGUI;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.PlayerArgument;
import org.bukkit.ChatColor;
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

				//TODO begin temp
				if (!canLook(sender, otherPlayer)) {
					return;
				}
				//TODO end temp

				new PlayerItemStatsGUI(sender, otherPlayer).openInventory(sender, plugin);
			}).register();

		new CommandAPICommand("playerstats").withPermission(perms).withAliases("ps")
			.withArguments(new PlayerArgument("player1"), new PlayerArgument("player2"))
			.executesPlayer((sender, args) -> {
				Player player1 = (Player) args[0];
				Player player2 = (Player) args[1];

				//TODO begin temp
				if (!canLook(sender, player1) || !canLook(sender, player2)) {
					return;
				}
				//TODO end temp
				new PlayerItemStatsGUI(player1, player2).openInventory(sender, plugin);
			}).register();

	}

	//TODO temp method
	private static boolean canLook(Player looker, Player lookee) {
		if (ScoreboardUtils.getScoreboardValue(lookee, "R3Access").orElse(0) > 0) {
			if (ScoreboardUtils.getScoreboardValue(looker, "R3Access").orElse(0) > 0) {
				return true;
			}
			// non r3 player trying to look at r3 player's items
			looker.sendMessage(ChatColor.RED + "You cannot view this player's stats at this time. Good try though.");
			return false;
		}
		return true;
	}
}
