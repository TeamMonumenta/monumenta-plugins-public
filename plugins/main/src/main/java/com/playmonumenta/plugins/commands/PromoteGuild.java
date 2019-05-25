package com.playmonumenta.plugins.commands;

import java.util.LinkedHashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.IntegerArgument;
import io.github.jorelali.commandapi.api.arguments.PlayerArgument;
import io.github.jorelali.commandapi.api.arguments.TextArgument;

public class PromoteGuild extends GenericCommand {

	@SuppressWarnings("unchecked")
	public static void register(Plugin plugin) {

		// promoteguild <guildname> <guildID> <playername>

		CommandPermission perms = CommandPermission.fromString("monumenta.command.promoteguild");

		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("guild name", new TextArgument());
		arguments.put("guild ID number", new IntegerArgument(1, 27));
		arguments.put("player name", new PlayerArgument());

		CommandAPI.getInstance().register("promoteguild", perms, arguments, (sender, args) -> {
			run(plugin, sender, (String) args[0], (Integer) args[1], (Player) args[2]);
		});
	}

	private static void run(Plugin plugin, CommandSender sender, String guildName, int guildID, Player player) {
		if (ScoreboardUtils.getScoreboardValue(player, "Guild") != guildID) {
			return;
		}

		// Check for nearby founder
		for (Player p : PlayerUtils.getNearbyPlayers(player.getLocation(), 1)) {
			if (ScoreboardUtils.getScoreboardValue(p, "Guild") == guildID && ScoreboardUtils.getScoreboardValue(p, "Founder") == 1) {

				// Set scores and permissions
				ScoreboardUtils.setScoreboardValue(player, "Founder", 1);

				// Flair (mostly stolen from CreateGuild)
				player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Congratulations! You are now a founder of " + guildName + "!");
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "execute at @a[x=-770,y=106,z=-128,dx=7,dy=4,dz=13] "
				                       + "run summon minecraft:firework_rocket ~ ~1 ~ "
				                       + "{LifeTime:0,FireworksItem:{id:firework_rocket,Count:1,tag:{Fireworks:{Explosions:[{Type:1,Colors:[I;16528693],FadeColors:[I;16777215]}]}}}}");
				break;
			}
		}
	}
}
