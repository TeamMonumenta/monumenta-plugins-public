package com.playmonumenta.plugins.integrations.luckperms;

import java.util.LinkedHashMap;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument.EntitySelector;
import io.github.jorelali.commandapi.api.arguments.TextArgument;

import me.lucko.luckperms.api.Group;
import me.lucko.luckperms.api.LuckPermsApi;

public class TeleportGuild {
	@SuppressWarnings("unchecked")
	public static void register(LuckPermsApi lp) {
		// teleportguild <guildname> <player>
		CommandPermission perms = CommandPermission.fromString("monumenta.command.teleportguild");

		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("player", new EntitySelectorArgument(EntitySelector.MANY_PLAYERS));

		CommandAPI.getInstance().register("teleportguild", perms, arguments, (sender, args) -> {
			for (Player player : (List<Player>)args[0]) {
				run(lp, player, null);
			}
		});

		arguments.put("guild name", new TextArgument());
		CommandAPI.getInstance().register("teleportguild", perms, arguments, (sender, args) -> {
			for (Player player : (List<Player>)args[0]) {
				run(lp, player, (String)args[1]);
			}
		});
	}

	private static void run(LuckPermsApi lp, Player player, String guildName) throws CommandSyntaxException {

		Group group = null;

		if (guildName == null) {
			// Look up the player's guild
			group = LuckPermsIntegration.getGuild(lp, player);
			if (group == null) {
				String err = ChatColor.RED + "You are not in a guild!";
				player.sendMessage(err);
				CommandAPI.fail(err);
			}
		}

		if (group == null) {
			// Still null, need to look up from name
			// The only way to get here is if guildName != null

			// Guild name sanitization for command usage
			//TODO: Better lookup of guild name?
			String cleanGuildName = LuckPermsIntegration.getCleanGuildName(guildName);

			group = lp.getGroup(cleanGuildName);
			if (group == null) {
				CommandAPI.fail("The luckperms group '" + cleanGuildName + "' does not exist");
			}
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
