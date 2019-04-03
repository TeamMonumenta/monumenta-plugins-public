package com.playmonumenta.plugins.commands;

import java.util.Collection;
import java.util.LinkedHashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.NetworkUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument.EntitySelector;
import io.github.jorelali.commandapi.api.arguments.IntegerArgument;
import io.github.jorelali.commandapi.api.arguments.StringArgument;
import io.github.jorelali.commandapi.api.arguments.TextArgument;

public class CreateGuild extends GenericCommand {

	@SuppressWarnings("unchecked")
	public static void register(Plugin plugin) {

		// createguild <guildname> <guildID> <guild tag>
		CommandPermission perms = CommandPermission.fromString("monumenta.command.createguild");

		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("guild name", new TextArgument());
		arguments.put("guild ID number", new IntegerArgument(1, 27));
		arguments.put("guild tag", new StringArgument());
		arguments.put("founders", new EntitySelectorArgument(EntitySelector.MANY_PLAYERS)
		              .overrideSuggestions("@a[x=-770,y=106,z=-128,dx=7,dy=4,dz=13]"));

		CommandAPI.getInstance().register("createguild", perms, arguments, (sender, args) -> {
			run(plugin, sender, (String) args[0], (int) args[1], (String) args[2], (Collection<Player>) args[3]);
		});
	}

	private static void run(Plugin plugin, CommandSender sender, String guildName, int guildID, String guildTag, Collection<Player> founders) {

		// Guild name sanitization for command usage
		String cleanGuildName = guildName.toLowerCase().replace(" ", "");

		int totalPrestige = 0;
		boolean hasEnoughPrestige = true;
		for (Player founder : founders) {
			if (ScoreboardUtils.getScoreboardValue(founder, "Prestige") < 8) {
				sender.sendMessage(ChatColor.DARK_RED + "" + ChatColor.ITALIC + "The minimal prestige count for "
				                   + founder.getName() + " is not reached ("
				                   + ScoreboardUtils.getScoreboardValue(founder, "Prestige") + "/8)");
				hasEnoughPrestige = false;
			} else {
				totalPrestige += ScoreboardUtils.getScoreboardValue(founder, "Prestige");
			}
		}

		// Displays ALL founders without enough prestige
		if (!hasEnoughPrestige) {
			return;
		}

		if (totalPrestige < 50) {
			sender.sendMessage(ChatColor.DARK_RED + "" + ChatColor.ITALIC + "The total prestige count is not enough ("
			                   + totalPrestige + "/50)");
			return;
		}

		// Add tags, display messages and effects
		for (Player founder : founders) {
			ScoreboardUtils.setScoreboardValue(founder, "Founder", 1);
			ScoreboardUtils.setScoreboardValue(founder, "Guild", guildID);
			founder.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD
			                    + "Congratulations! You have founded a new guild. May the gods grant you strength and prosperity!");
			founder.playSound(founder.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1f, 1.5f);

			// fireworks!
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "/execute at @a[x=-770,y=106,z=-128,dx=7,dy=4,dz=13] "
			                       + "run summon minecraft:firework_rocket ~ ~1 ~ "
			                       + "{LifeTime:0,FireworksItem:{id:firework_rocket,Count:1,tag:{Fireworks:{Explosions:[{Type:1,Colors:[I;16528693],FadeColors:[I;16777215]}]}}}}");
		}

		try {
			NetworkUtils.broadcastCommand(plugin, "tellraw @a [\"\",{\"text\":\"A new guild has just been founded. Say hello to " + guildName + "!!\",\"bold\":true}]");
		} catch (Exception e) {
			sender.sendMessage(ChatColor.RED + "Broadcasting command failed");
		}

		// Sort out permissions
		Bukkit.dispatchCommand(sender, "/lp creategroup " + cleanGuildName);
		Bukkit.dispatchCommand(sender, "/lp group " + cleanGuildName + " meta addprefix 1 " + guildTag);
		Bukkit.dispatchCommand(sender, "/lp group " + cleanGuildName + " meta set hoverprefix " + guildName);

	}
}