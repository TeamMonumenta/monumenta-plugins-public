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
import java.util.Set;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.luckperms.api.model.data.NodeMap;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public class UpdateGuilds {
	private static final List<GuildPermission> newPermissions = List.of(
		GuildPermission.MANAGE_MEMBERSHIP,
		GuildPermission.LOCKDOWN
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

						for (GuildPermission guildPermission : newPermissions) {
							if (accessLevel.compareTo(guildPermission.mDefaultAccessLevel) <= 0) {
								guildPermission.setExplicitPermission(guildRootGroup, accessGroup, true).join();
							}
						}
					}

					Group guestGroup = GuildAccessLevel.GUEST.loadGroupFromRoot(guildRootGroup).join().orElse(null);
					if (guestGroup == null) {
						sender.sendMessage(Component.text("- Could not find guest group for " + guildRootGroupId));
					}

					String guildBlockedGroupId = GuildAccessLevel.BLOCKED.groupNameFromRoot(guildRootGroupId);
					String guildNoneGroupId = GuildAccessLevel.NONE.groupNameFromRoot(guildRootGroupId);

					Group guildBlockedGroup = LuckPermsIntegration.GM.createAndLoadGroup(guildBlockedGroupId).join();
					NodeMap guildBlockedGroupData = guildBlockedGroup.data();
					guildBlockedGroupData.add(InheritanceNode.builder(guildRootGroup).build());

					Group guildNoneGroup = LuckPermsIntegration.GM.createAndLoadGroup(guildNoneGroupId).join();
					NodeMap guildNoneGroupData = guildNoneGroup.data();
					guildNoneGroupData.add(InheritanceNode.builder(guildRootGroup).build());

					for (GuildPermission guildPermission : GuildPermission.values()) {
						guildPermission.setExplicitPermission(guildRootGroup, guildBlockedGroup, false).join();
					}

					LuckPermsIntegration.GM.saveGroup(guildBlockedGroup).join();
					LuckPermsIntegration.GM.saveGroup(guildNoneGroup).join();

					sender.sendMessage(Component.text("Updated " + guildRootGroupId, NamedTextColor.GREEN));
				} catch (Exception ex) {
					sender.sendMessage(Component.text("Failed to update " + guildRootGroupId + ":", NamedTextColor.RED));
					MessagingUtils.sendStackTrace(sender, ex);
				}
			}

			sender.sendMessage(Component.text("Done with guilds, updating players:", NamedTextColor.GREEN));
			Set<UUID> allUsers = LuckPermsIntegration.UM.getUniqueUsers().join();
			int userIndex = 0;
			int numUsers = allUsers.size();
			for (UUID userId : allUsers) {
				userIndex++;
				if (userIndex % 1000 == 0) {
					sender.sendMessage(Component.text("Updating user " + userIndex + "/" + numUsers + "...", NamedTextColor.YELLOW));
				}

				User user = LuckPermsIntegration.UM.getUser(userId);
				if (user == null) {
					continue;
				}

				for (Group guild : guilds) {
					GuildAccessLevel accessLevel = LuckPermsIntegration.getAccessLevel(guild, user);
					if (!GuildAccessLevel.NONE.equals(accessLevel)) {
						continue;
					}

					String guildTag = LuckPermsIntegration.getGuildPlainTag(guild);
					if (guildTag != null) {
						GuildPermission.clearExplicitPermissions(user, guildTag);
					}
				}
			}

			sender.sendMessage(Component.text("Done!", NamedTextColor.GREEN));
		});
	}
}
