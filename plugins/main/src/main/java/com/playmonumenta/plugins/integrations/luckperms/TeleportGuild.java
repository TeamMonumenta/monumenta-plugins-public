package com.playmonumenta.plugins.integrations.luckperms;

import java.util.LinkedHashMap;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.playmonumenta.plugins.Plugin;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument.EntitySelector;
import io.github.jorelali.commandapi.api.arguments.TextArgument;

import me.lucko.luckperms.api.Group;
import me.lucko.luckperms.api.LuckPermsApi;

public class TeleportGuild {
	public static void register(Plugin plugin, LuckPermsApi lp) {
		// teleportguild <guildname> <player>
		CommandPermission perms = CommandPermission.fromString("monumenta.command.teleportguild");

		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("player", new EntitySelectorArgument(EntitySelector.ONE_PLAYER));

		CommandAPI.getInstance().register("teleportguild", perms, arguments, (sender, args) -> {
			run(plugin, lp, sender, (Player)args[0], null);
		});

		arguments.put("guild name", new TextArgument());
		CommandAPI.getInstance().register("teleportguild", perms, arguments, (sender, args) -> {
			run(plugin, lp, sender, (Player)args[0], (String)args[1]);
		});
	}

	private static void run(Plugin plugin, LuckPermsApi lp, CommandSender sender,
	                        Player player, String guildName) throws CommandSyntaxException {

		if (guildName == null) {
			// Look up the player's guild
			guildName = LuckPermsIntegration.getGuildName(lp, player);
			if (guildName == null) {
				String err = ChatColor.RED + "You are not in a guild!";
				player.sendMessage(err);
				CommandAPI.fail(err);
			}
		}

		// Guild name sanitization for command usage
		String cleanGuildName = LuckPermsIntegration.getCleanGuildName(guildName);

		Group group = lp.getGroup(cleanGuildName);
		if (group == null) {
			CommandAPI.fail("The luckperms group '" + cleanGuildName + "' does not exist");
		}

		Location loc = LuckPermsIntegration.getGuildTp(lp, player.getWorld(), group);

		if (loc == null) {
			player.sendMessage(ChatColor.RED + "The teleport for your guild is not set up");
			player.sendMessage(ChatColor.RED + "Please ask a moderator to fix this");
		} else {
			player.teleport(loc);
		}
	}
}
