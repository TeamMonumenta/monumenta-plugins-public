package com.playmonumenta.plugins.integrations.luckperms;

import java.util.Map.Entry;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.LocationUtils;

import me.lucko.luckperms.LuckPerms;
import me.lucko.luckperms.api.Group;
import me.lucko.luckperms.api.LuckPermsApi;
import me.lucko.luckperms.api.MessagingService;
import me.lucko.luckperms.api.Node;

public class LuckPermsIntegration {
	protected static LuckPermsApi LP = null;

	public LuckPermsIntegration(Plugin plugin) {
		plugin.getLogger().info("Enabling LuckPerms integration");
		LP = LuckPerms.getApi();

		CreateGuild.register(plugin);
		JoinGuild.register(plugin);
		PromoteGuild.register();
		LeaveGuild.register(plugin);
		TestGuild.register(plugin);
		TeleportGuild.register();
		SetGuildTeleport.register(plugin);
	}

	public static Group getGuild(Player player) {
		if (LuckPermsIntegration.LP == null) {
			return null;
		}

		for (Node userNode : LP.getUser(player.getUniqueId()).getOwnNodes()) {
			if (userNode.isGroupNode()) {
				Group group = LP.getGroup(userNode.getGroupName());
				for (Node groupChildNode : group.getNodes().values()) {
					if (groupChildNode.isMeta()) {
						Entry<String, String> meta = groupChildNode.getMeta();
						if (meta.getKey().equals("guildname")) {
							return group;
						}
					}
				}
			}
		}

		return null;
	}

	public static String getGuildName(Group group) {
		if (group == null) {
			return null;
		}

		for (Node groupChildNode : group.getNodes().values()) {
			if (groupChildNode.isMeta()) {
				Entry<String, String> meta = groupChildNode.getMeta();
				if (meta.getKey().equals("guildname")) {
					return meta.getValue();
				}
			}
		}

		return null;
	}

	public static void setGuildTp(Group group, Plugin plugin, Location loc) {
		// Remove all the other guildtp meta nodes
		for (Node groupChildNode : group.getNodes().values()) {
			if (groupChildNode.isMeta()) {
				Entry<String, String> meta = groupChildNode.getMeta();
				if (meta.getKey().equals("guildtp")) {
					group.unsetPermission(groupChildNode);
				}
			}
		}

		group.setPermission(LP.getNodeFactory().makeMetaNode("guildtp", LocationUtils.locationToString(loc)).build());

		new BukkitRunnable() {
			@Override
			public void run() {
				LP.getGroupManager().saveGroup(group);
				LP.runUpdateTask();
				LP.getMessagingService().ifPresent(MessagingService::pushUpdate);
			}
		}.runTaskAsynchronously(plugin);
	}

	public static Location getGuildTp(World world, Group group) {
		try {
			for (Node groupChildNode : group.getNodes().values()) {
				if (groupChildNode.isMeta()) {
					Entry<String, String> meta = groupChildNode.getMeta();
					if (meta.getKey().equals("guildtp")) {
						return LocationUtils.locationFromString(world, meta.getValue());
					}
				}
			}
		} catch (Exception e) {
			return null;
		}

		return null;
	}

	public static String getCleanGuildName(String guildName) {
		// Guild name sanitization for command usage
		return guildName.toLowerCase().replace(" ", "_");
	}
}
