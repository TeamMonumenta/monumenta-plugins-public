package com.playmonumenta.plugins.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;

public class ToggleSwap {

	public static final String SWAP_TAG = "DisableSwapHands";

	public static void register(Plugin plugin) {
		new CommandAPICommand("toggleswap")
			.executes((sender, args) -> {
				run(plugin, sender);
			})
			.register();
	}

	public static void run(Plugin plugin, CommandSender sender) throws WrapperCommandSyntaxException {
		if (!(sender instanceof Player)) {
			CommandAPI.fail("This command can only be run by players");
		}

		Player player = (Player)sender;

		String message;
		if (player.getScoreboardTags().contains(SWAP_TAG)) {
			player.removeScoreboardTag(SWAP_TAG);
			message = "Swapping hands has been enabled";
		} else {
			player.addScoreboardTag(SWAP_TAG);
			message = "Swapping hands has been disabled";
		}

		player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + message);
	}
}
