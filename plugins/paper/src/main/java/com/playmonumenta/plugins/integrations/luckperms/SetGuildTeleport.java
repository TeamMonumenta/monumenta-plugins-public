package com.playmonumenta.plugins.integrations.luckperms;

import java.util.ArrayList;
import java.util.List;

import com.playmonumenta.plugins.Plugin;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.TextArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import net.luckperms.api.model.group.Group;

public class SetGuildTeleport {
	public static void register(Plugin plugin) {
		// setguildteleport <guildname>
		CommandPermission perms = CommandPermission.fromString("monumenta.command.setguildteleport");

		List<Argument> arguments = new ArrayList<>();
		arguments.add(new TextArgument("guild name"));

		new CommandAPICommand("setguildteleport")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				run(plugin, sender, (String)args[0]);
			})
			.register();
	}

	private static void run(Plugin plugin, CommandSender sender,
	                        String guildName) throws WrapperCommandSyntaxException {

		if (!(sender instanceof Player)) {
			CommandAPI.fail("This command can only be run by players");
		}

		Player player = (Player)sender;

		// Guild name sanitization for command usage
		String cleanGuildName = LuckPermsIntegration.getCleanGuildName(guildName);

		//TODO: Better lookup of guild name?
		Group group = LuckPermsIntegration.GM.getGroup(cleanGuildName);
		if (group == null) {
			CommandAPI.fail("The luckperms group '" + cleanGuildName + "' does not exist");
		}

		Location loc = player.getLocation();

		LuckPermsIntegration.setGuildTp(group, plugin, loc);

		player.sendMessage(ChatColor.GOLD + "Guild teleport set to your location");
	}
}
