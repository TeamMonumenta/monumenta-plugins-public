package com.playmonumenta.plugins.integrations.luckperms;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.luckperms.listeners.LPArguments;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public class LpGroupListCommand {
	public static void register(Plugin plugin) {
		// lpgroup list <group> [recursive]
		CommandPermission perms = CommandPermission.fromString("monumenta.command.lpgroup.list");

		new CommandAPICommand("lpgroup")
			.withArguments(new MultiLiteralArgument("list"))
			.withArguments(new TextArgument("permission group")
				.replaceSuggestions(LPArguments.GROUP_SUGGESTIONS))
			.executes((sender, args) -> {
				CommandUtils.checkPerm(sender, perms);
				if (ServerProperties.getShardName().contains("build")) {
					throw CommandAPI.failWithString("This command cannot be run on the build shard.");
				}

				String permissionGroup = (String) args[1];
				listMembers(plugin, sender, permissionGroup, false);
			})
			.register();

		new CommandAPICommand("lpgroup")
			.withArguments(new MultiLiteralArgument("list"))
			.withArguments(new TextArgument("permission group")
				.replaceSuggestions(LPArguments.GROUP_SUGGESTIONS))
			.withArguments(new MultiLiteralArgument("recursive"))
			.executes((sender, args) -> {
				CommandUtils.checkPerm(sender, perms);
				if (ServerProperties.getShardName().contains("build")) {
					throw CommandAPI.failWithString("This command cannot be run on the build shard.");
				}

				String permissionGroup = (String) args[1];
				listMembers(plugin, sender, permissionGroup, true);
			})
			.register();
	}

	public static void listMembers(Plugin plugin, CommandSender sender, String permissionGroup, boolean recursive) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			try {
				LuckPermsIntegration.listGroupMembers(sender, permissionGroup, recursive).join();
			} catch (Exception ex) {
				Bukkit.getScheduler().runTask(plugin, () -> {
					sender.sendMessage(Component.text("Failed to get list of players in guild:",
						NamedTextColor.RED));
					MessagingUtils.sendStackTrace(sender, ex);
				});
			}
		});
	}
}
