package com.playmonumenta.plugins.integrations.luckperms;

import java.util.Map.Entry;

import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;

import me.lucko.luckperms.LuckPerms;
import me.lucko.luckperms.api.Group;
import me.lucko.luckperms.api.LuckPermsApi;
import me.lucko.luckperms.api.Node;

public class LuckPermsIntegration {
	public LuckPermsIntegration(Plugin plugin) {
		LuckPermsApi lp = LuckPerms.getApi();

		CreateGuild.register(plugin, lp);
		JoinGuild.register(plugin, lp);
		PromoteGuild.register(plugin, lp);
		LeaveGuild.register(plugin, lp);
	}

	public static Group getGuild(LuckPermsApi lp, Player player) {
		for (Node userNode : lp.getUser(player.getUniqueId()).getOwnNodes()) {
			if (userNode.isGroupNode()) {
				Group group = ((Group)userNode);
				for (Node groupChildNode : group.getNodes().values()) {
					if (groupChildNode.isMeta()) {
						Entry<String, String>meta = groupChildNode.getMeta();
						if (meta.getKey().equals("guildname")) {
							return group;
						}
					}
				}
			}
		}

		return null;
	}

	public static String getGuildName(LuckPermsApi lp, Player player) {
		for (Node userNode : lp.getUser(player.getUniqueId()).getOwnNodes()) {
			if (userNode.isGroupNode()) {
				Group group = ((Group)userNode);
				for (Node groupChildNode : group.getNodes().values()) {
					if (groupChildNode.isMeta()) {
						Entry<String, String>meta = groupChildNode.getMeta();
						if (meta.getKey().equals("guildname")) {
							return meta.getValue();
						}
					}
				}
			}
		}

		return null;
	}

	public static String getCleanGuildName(String guildName) {
		// Guild name sanitization for command usage
		return guildName.toLowerCase().replace(" ", "");
	}
}
