package com.playmonumenta.plugins.integrations.luckperms;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.luckperms.listeners.LPArguments;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public class LpGroupDeleteCommand {
	public static void register(Plugin plugin) {
		// lpgroup delete <group> [recursive]
		CommandPermission perms = CommandPermission.fromString("monumenta.command.lpgroup.delete");

		new CommandAPICommand("lpgroup")
			.withArguments(new LiteralArgument("delete"))
			.withArguments(new TextArgument("permission group")
				.replaceSuggestions(LPArguments.GROUP_SUGGESTIONS))
			.executes((sender, args) -> {
				CommandUtils.checkPerm(sender, perms);
				if (ServerProperties.getShardName().contains("build")) {
					throw CommandAPI.failWithString("This command cannot be run on the build shard.");
				}

				String permissionGroup = args.getUnchecked("permission group");
				deleteGroup(plugin, sender, permissionGroup, false);
			})
			.register();

		new CommandAPICommand("lpgroup")
			.withArguments(new LiteralArgument("delete"))
			.withArguments(new TextArgument("permission group")
				.replaceSuggestions(LPArguments.GROUP_SUGGESTIONS))
			.withArguments(new LiteralArgument("recursive"))
			.executes((sender, args) -> {
				CommandUtils.checkPerm(sender, perms);
				if (ServerProperties.getShardName().contains("build")) {
					throw CommandAPI.failWithString("This command cannot be run on the build shard.");
				}

				String permissionGroup = args.getUnchecked("permission group");
				deleteGroup(plugin, sender, permissionGroup, true);
			})
			.register();
	}

	public static void deleteGroup(Plugin plugin, CommandSender sender, String permissionGroup, boolean recursive) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			try {
				LuckPermsIntegration.deleteGroup(sender, permissionGroup, recursive).join();
			} catch (Exception ex) {
				Bukkit.getScheduler().runTask(plugin, () -> {
					sender.sendMessage(Component.text("Failed to delete " + permissionGroup + ":", NamedTextColor.RED, TextDecoration.BOLD));
					MessagingUtils.sendStackTrace(sender, ex);
				});
			}
		});
	}
}

