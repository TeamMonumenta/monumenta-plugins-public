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
	private static final List<GuildPermission> mNewPermissions = List.of(
		GuildPermission.GUILD_OWNED_INFUSION
	);

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
		if (!ServerProperties.getShardName().equals("guildplots")) {
			sender.sendMessage(Component.text("This needs to be run on the guildplots shard", NamedTextColor.RED));
			return;
		}

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
				Group guildRootGroup = LuckPermsIntegration.getGuildRoot(memberGroup);
				if (guildRootGroup == null) {
					sender.sendMessage(Component.text("Failed to identify root of " + memberGroup.getFriendlyName(), NamedTextColor.RED));
					continue;
				}
				String guildRootGroupId = guildRootGroup.getName();
				try {
					for (GuildAccessLevel accessLevel : List.of(
						GuildAccessLevel.FOUNDER,
						GuildAccessLevel.MANAGER,
						GuildAccessLevel.MEMBER
					)) {
						Group accessGroup = accessLevel.loadGroupFromRoot(guildRootGroup).join().orElse(null);
						if (accessGroup == null) {
							sender.sendMessage(Component.text("- Could not find " + accessLevel.mId + " group for " + guildRootGroupId));
							continue;
						}

						for (GuildPermission guildPermission : mNewPermissions) {
							if (accessLevel.compareTo(guildPermission.mDefaultAccessLevel) <= 0) {
								guildPermission.setExplicitPermission(guildRootGroup, accessGroup, true).join();
							}
						}
					}

					Group blockedGroup = GuildAccessLevel.BLOCKED.loadGroupFromRoot(guildRootGroup).join().orElse(null);
					if (blockedGroup == null) {
						sender.sendMessage(Component.text("- Could not find blocked group for " + guildRootGroupId));
					} else {
						for (GuildPermission guildPermission : mNewPermissions) {
							guildPermission.setExplicitPermission(guildRootGroup, blockedGroup, false).join();
						}
					}

					sender.sendMessage(Component.text("Updated " + guildRootGroupId, NamedTextColor.GREEN));
				} catch (Exception ex) {
					sender.sendMessage(Component.text("Failed to update " + guildRootGroupId + ":", NamedTextColor.RED));
					MessagingUtils.sendStackTrace(sender, ex);
				}
			}
			sender.sendMessage(Component.text("Done!", NamedTextColor.GREEN));
		});
	}
}
