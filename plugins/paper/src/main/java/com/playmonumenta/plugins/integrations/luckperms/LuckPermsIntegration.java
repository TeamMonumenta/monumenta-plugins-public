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
	public LuckPermsIntegration(Plugin plugin) {
		LuckPermsApi lp = LuckPerms.getApi();

		CreateGuild.register(plugin, lp);
		JoinGuild.register(plugin, lp);
		PromoteGuild.register(plugin, lp);
		LeaveGuild.register(plugin, lp);
		TestGuild.register(plugin, lp);
		TeleportGuild.register(plugin, lp);
		SetGuildTeleport.register(plugin, lp);
	}

	public static Group getGuild(LuckPermsApi lp, Player player) {
		for (Node userNode : lp.getUser(player.getUniqueId()).getOwnNodes()) {
			if (userNode.isGroupNode()) {
				Group group = lp.getGroup(userNode.getGroupName());
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

	public static void setGuildTp(LuckPermsApi lp, Group group, Plugin plugin, Location loc) {
		// Remove all the other guildtp meta nodes
		for (Node groupChildNode : group.getNodes().values()) {
			if (groupChildNode.isMeta()) {
				Entry<String, String> meta = groupChildNode.getMeta();
				if (meta.getKey().equals("guildtp")) {
					group.unsetPermission(groupChildNode);
				}
			}
		}

		group.setPermission(lp.getNodeFactory().makeMetaNode("guildtp", LocationUtils.locationToString(loc)).build());

		new BukkitRunnable() {
			@Override
			public void run() {
				lp.getGroupManager().saveGroup(group);
				lp.runUpdateTask();
				lp.getMessagingService().ifPresent(MessagingService::pushUpdate);
			}
		}.runTaskAsynchronously(plugin);
	}

	public static Location getGuildTp(LuckPermsApi lp, World world, Group group) {
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
			// Pass
		}

		return null;
	}

	public static String getCleanGuildName(String guildName) {
		// Guild name sanitization for command usage
		return guildName.toLowerCase().replace(" ", "_");
	}
}
