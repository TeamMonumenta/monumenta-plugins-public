package com.playmonumenta.plugins.commands;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.utils.ScoreboardUtils;

import dev.jorel.commandapi.CommandAPICommand;

public class VirtualFirmament {

	private static final String TAG = "VirtualFirmament";

	public static void register() {

		new CommandAPICommand("virtualfirmament")
			.withPermission("monumenta.virtualfirmament")
			.withAliases("vf")
			.executesPlayer((player, args) -> {
				boolean enabled = ScoreboardUtils.toggleTag(player, TAG);
				player.sendMessage(ChatColor.GOLD + "Virtual Firmament " + (enabled ? ChatColor.GREEN + "enabled" : ChatColor.RED + "disabled"));
				player.updateInventory();
			})
			.register();

	}

	public static boolean isEnabled(Player player) {
		return ScoreboardUtils.checkTag(player, TAG);
	}

}
