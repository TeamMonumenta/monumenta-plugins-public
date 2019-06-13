package com.playmonumenta.plugins.integrations.luckperms;

import java.util.LinkedHashMap;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.PlayerArgument;

import me.lucko.luckperms.api.LuckPermsApi;

public class LeaveGuild {
	public static void register(Plugin plugin, LuckPermsApi lp) {

		// leaveguild <playername>
		CommandPermission perms = CommandPermission.fromString("monumenta.command.leaveguild");

		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("player name", new PlayerArgument());

		CommandAPI.getInstance().register("leaveguild", perms, arguments, (sender, args) -> {
			run(plugin, sender, (Player) args[0]);
		});
	}

	private static void run(Plugin plugin, CommandSender sender, Player player) {

		if (ScoreboardUtils.getScoreboardValue(player, "Guild") != 0) {
			return;
		}

		// Set scores and permissions
		ScoreboardUtils.setScoreboardValue(player, "Guild", 0);
		ScoreboardUtils.setScoreboardValue(player, "Founder", 0);
		/*
		 * TODO:
		 * Turn this pseudocode into something that can actually get a list of all guild perms
		 *
		 * for (String guildName : ALL_THE_GUILD_PERMS_THINGS) {
		 *     Bukkit.dispatchCommand(sender, "lp user " + player.getName() + " parent remove " + guildName);
		 * }
		 */

		// Flair
		player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "You have left your guild.");
	}
}
