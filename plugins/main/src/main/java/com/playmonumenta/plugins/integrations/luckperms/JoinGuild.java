package com.playmonumenta.plugins.integrations.luckperms;

import java.util.LinkedHashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument.EntitySelector;
import io.github.jorelali.commandapi.api.arguments.TextArgument;

import me.lucko.luckperms.api.Group;
import me.lucko.luckperms.api.LuckPermsApi;
import me.lucko.luckperms.api.User;

public class JoinGuild {
	public static void register(Plugin plugin, LuckPermsApi lp) {
		// joinguild <guildname> <playername>
		CommandPermission perms = CommandPermission.fromString("monumenta.command.joinguild");

		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("player", new EntitySelectorArgument(EntitySelector.ONE_PLAYER));
		arguments.put("founder", new EntitySelectorArgument(EntitySelector.ONE_PLAYER));

		CommandAPI.getInstance().register("joinguild", perms, arguments, (sender, args) -> {
			run(plugin, lp, sender, (Player) args[0], (Player) args[1]);
		});
	}

	private static void run(Plugin plugin, LuckPermsApi lp, CommandSender sender,
	                        Player player, Player founder) throws CommandSyntaxException {
		if (ScoreboardUtils.getScoreboardValue(founder, "Founder") != 1) {
			CommandAPI.fail("Player '" + founder.getName() + "' is not a founder");
		}

		String founderGuildName = LuckPermsIntegration.getGuildName(lp, founder);
		if (founderGuildName == null) {
			CommandAPI.fail("Founder '" + founder.getName() + "' is not in a guild");
		}

		String currentGuildName = LuckPermsIntegration.getGuildName(lp, player);
		if (currentGuildName != null) {
			String err = ChatColor.RED + "You are already in the guild '" + currentGuildName + "' !";
			player.sendMessage(err);
			CommandAPI.fail(err);
		}

		// Guild name sanitization for command usage
		String cleanGuildName = LuckPermsIntegration.getCleanGuildName(founderGuildName);

		Group group = lp.getGroup(cleanGuildName);
		if (group == null) {
			String err = ChatColor.RED + "The luckperms group '" + cleanGuildName + "' does not exist!";
			player.sendMessage(err);
			CommandAPI.fail(err);
		}

		// Add user to guild
		User user = lp.getUser(player.getUniqueId());
		user.setPermission(lp.getNodeFactory().makeGroupNode(group).build());

		// Success indicators
		player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Congratulations! You have joined " + founderGuildName + "!");
		Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
							   "execute at " + player.getName()
							   + "run summon minecraft:firework_rocket ~ ~1 ~ "
							   + "{LifeTime:0,FireworksItem:{id:firework_rocket,Count:1,tag:{Fireworks:{Explosions:[{Type:1,Colors:[I;16528693],FadeColors:[I;16777215]}]}}}}");
	}
}
