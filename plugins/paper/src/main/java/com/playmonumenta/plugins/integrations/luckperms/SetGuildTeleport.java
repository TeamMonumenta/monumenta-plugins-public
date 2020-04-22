package com.playmonumenta.plugins.integrations.luckperms;

import java.util.LinkedHashMap;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.TextArgument;
import io.github.jorelali.commandapi.api.exceptions.WrapperCommandSyntaxException;

import me.lucko.luckperms.api.Group;
import me.lucko.luckperms.api.LuckPermsApi;

public class SetGuildTeleport {
	public static void register(Plugin plugin, LuckPermsApi lp) {
		// setguildteleport <guildname>
		CommandPermission perms = CommandPermission.fromString("monumenta.command.setguildteleport");

		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("guild name", new TextArgument());

		CommandAPI.getInstance().register("setguildteleport", perms, arguments, (sender, args) -> {
			run(plugin, lp, sender, (String)args[0]);
		});
	}

	private static void run(Plugin plugin, LuckPermsApi lp, CommandSender sender,
	                        String guildName) throws WrapperCommandSyntaxException {

		if (!(sender instanceof Player)) {
			CommandAPI.fail("This command can only be run by players");
		}

		Player player = (Player)sender;

		// Guild name sanitization for command usage
		String cleanGuildName = LuckPermsIntegration.getCleanGuildName(guildName);

		//TODO: Better lookup of guild name?
		Group group = lp.getGroup(cleanGuildName);
		if (group == null) {
			CommandAPI.fail("The luckperms group '" + cleanGuildName + "' does not exist");
		}

		Location loc = player.getLocation();

		LuckPermsIntegration.setGuildTp(lp, group, plugin, loc);

		player.sendMessage(ChatColor.GOLD + "Guild teleport set to your location");
	}
}
