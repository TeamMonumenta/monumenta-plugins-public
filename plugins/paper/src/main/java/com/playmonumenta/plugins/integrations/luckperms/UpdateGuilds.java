package com.playmonumenta.plugins.integrations.luckperms;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.model.group.Group;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public class UpdateGuilds {
	public static void register(Plugin plugin) {
		// guild mod update
		CommandPermission perms = CommandPermission.fromString("monumenta.command.guild.mod.update");

		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(new LiteralArgument("mod"));
		arguments.add(new LiteralArgument("update"));

		new CommandAPICommand("guild")
			.withArguments(arguments)
			.executes((sender, args) -> {
				CommandUtils.checkPerm(sender, perms);
				if (ServerProperties.getShardName().contains("build")) {
					throw CommandAPI.failWithString("This command cannot be run on the build shard.");
				}

				run(plugin, sender);
			})
			.register();
	}

	public static void run(Plugin plugin, CommandSender sender) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			List<Group> guilds;
			try {
				guilds = LuckPermsIntegration.getGuilds().join();
			} catch (Exception ex) {
				sender.sendMessage(Component.text("Failed to get list of guilds:", NamedTextColor.RED));
				MessagingUtils.sendStackTrace(sender, ex);
				return;
			}

			for (Group memberGroup : guilds) {
				Group guildRoot = LuckPermsIntegration.getGuildRoot(memberGroup);
				if (guildRoot == null) {
					sender.sendMessage(Component.text("Failed to identify root of " + memberGroup.getFriendlyName(), NamedTextColor.RED));
					continue;
				}
				String guildId = guildRoot.getName();
				try {
					for (GuildAccessLevel accessLevel : List.of(
						GuildAccessLevel.FOUNDER,
						GuildAccessLevel.MANAGER,
						GuildAccessLevel.MEMBER
					)) {
						Group accessGroup = accessLevel.loadGroupFromRoot(guildRoot).join().orElse(null);
						if (accessGroup == null) {
							sender.sendMessage(Component.text("- Could not find " + accessLevel.mId + " group for " + guildId));
							continue;
						}

						for (GuildPermission guildPermission : GuildPermission.values()) {
							if (accessLevel.compareTo(guildPermission.mDefaultAccessLevel) <= 0) {
								guildPermission.setExplicitPermission(guildRoot, accessGroup, true).join();
							}
						}
					}

					sender.sendMessage(Component.text("Updated " + guildId, NamedTextColor.GREEN));
				} catch (Exception ex) {
					sender.sendMessage(Component.text("Failed to update " + guildId + ":", NamedTextColor.RED));
					MessagingUtils.sendStackTrace(sender, ex);
				}
			}
			sender.sendMessage(Component.text("Done!", NamedTextColor.GREEN));
		});
	}
}
