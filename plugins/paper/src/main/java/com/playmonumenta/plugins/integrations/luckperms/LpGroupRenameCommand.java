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
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public class LpGroupRenameCommand {
	public static void register(Plugin plugin) {
		// lpgroup rename <old group id> <new group id>
		CommandPermission perms = CommandPermission.fromString("monumenta.command.lpgroup.rename");

		new CommandAPICommand("lpgroup")
			.withArguments(new LiteralArgument("rename"))
			.withArguments(new TextArgument("old group ID")
				.replaceSuggestions(LPArguments.GROUP_SUGGESTIONS))
			.withArguments(new TextArgument("new group ID")
				.replaceSuggestions(LPArguments.GROUP_SUGGESTIONS))
			.executes((sender, args) -> {
				CommandUtils.checkPerm(sender, perms);
				if (ServerProperties.getShardName().contains("build")) {
					throw CommandAPI.failWithString("This command cannot be run on the build shard.");
				}

				String oldId = args.getUnchecked("old group ID");
				String newId = args.getUnchecked("new group ID");
				renameGroup(plugin, sender, oldId, newId);
			})
			.register();
	}

	public static void renameGroup(Plugin plugin, CommandSender sender, String oldId, String newId) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			try {
				LuckPermsIntegration.renameGroup(sender, oldId, newId).join();
			} catch (Exception ex) {
				Bukkit.getScheduler().runTask(plugin, () -> {
					sender.sendMessage(Component.text("Failed to get rename group in guild:",
						NamedTextColor.RED));
					MessagingUtils.sendStackTrace(sender, ex);
				});
			}
		});
	}
}
