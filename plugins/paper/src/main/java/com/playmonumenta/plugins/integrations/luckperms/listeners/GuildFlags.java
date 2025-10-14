package com.playmonumenta.plugins.integrations.luckperms.listeners;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.guis.Gui;
import com.playmonumenta.plugins.integrations.luckperms.GuildFlag;
import com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration;
import com.playmonumenta.plugins.integrations.luckperms.guildgui.GuildGui;
import com.playmonumenta.plugins.utils.MMLog;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.node.NodeAddEvent;
import net.luckperms.api.event.node.NodeRemoveEvent;
import net.luckperms.api.event.sync.PostSyncEvent;
import net.luckperms.api.model.PermissionHolder;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.PermissionNode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

public class GuildFlags implements Listener {
	private static final ConcurrentSkipListMap<Long, ConcurrentSkipListSet<GuildFlag>> mGuildFlags = new ConcurrentSkipListMap<>();

	@SuppressWarnings("resource")
	public static void registerLuckPermsEvents(Plugin plugin, EventBus eventBus) {
		eventBus.subscribe(plugin, NodeAddEvent.class, GuildFlags::nodeAddEvent);
		eventBus.subscribe(plugin, NodeRemoveEvent.class, GuildFlags::nodeRemoveEvent);

		eventBus.subscribe(plugin, PostSyncEvent.class, GuildFlags::postSyncEvent);

		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), GuildFlags::refreshAllGuildFlags);
	}

	public static void nodeAddEvent(NodeAddEvent event) {
		PermissionHolder holder = event.getTarget();

		if (holder instanceof Group group) {
			Node node = event.getNode();
			if (node instanceof PermissionNode permissionNode) {
				GuildFlag guildFlag = GuildFlag.getGuildFlag(permissionNode.getPermission());
				if (guildFlag == null) {
					return;
				}
				refreshFlags(group, guildFlag);
			}
		}
	}

	public static void nodeRemoveEvent(NodeRemoveEvent event) {
		PermissionHolder holder = event.getTarget();

		if (holder instanceof Group group) {
			Node node = event.getNode();
			if (node instanceof PermissionNode permissionNode) {
				GuildFlag guildFlag = GuildFlag.getGuildFlag(permissionNode.getPermission());
				if (guildFlag == null) {
					return;
				}
				refreshFlags(group, guildFlag);
			}
		}
	}

	public static void postSyncEvent(PostSyncEvent event) {
		refreshAllGuildFlags();
	}

	public static void refreshAllGuildFlags() {
		for (Group guild : LuckPermsIntegration.getLoadedGuilds()) {
			for (GuildFlag guildFlag : GuildFlag.values()) {
				refreshFlags(guild, guildFlag);
			}
		}
	}

	public static void refreshFlags(Group group, GuildFlag guildFlag) {
		Group guildRoot = LuckPermsIntegration.getGuildRoot(group);
		if (guildRoot == null) {
			return;
		}
		Long guildPlotId = LuckPermsIntegration.getGuildPlotId(guildRoot);
		if (guildPlotId == null) {
			return;
		}

		ConcurrentSkipListSet<GuildFlag> cachedFlagSet = mGuildFlags
			.computeIfAbsent(guildPlotId, k -> new ConcurrentSkipListSet<>());

		Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
			if (guildFlag.hasFlag(guildRoot)) {
				if (cachedFlagSet.add(guildFlag)) {
					applyFlagChange(guildRoot, guildFlag, true);
				}
			} else {
				if (cachedFlagSet.remove(guildFlag)) {
					applyFlagChange(guildRoot, guildFlag, false);
				}
			}
		});
	}

	private static void applyFlagChange(
		Group guildRoot,
		GuildFlag guildFlag,
		boolean isEnabled
	) {
		String guildName = LuckPermsIntegration.getNonNullGuildName(guildRoot);
		MMLog.fine(
			"[Guild Flags Listener/Flag Change] " + guildName
				+ " had their " + guildFlag.mFlagLpId
				+ " flag " + (isEnabled ? "enabled" : "disabled")
				+ "; updating relevant code"
		);
		for (Player player : Bukkit.getOnlinePlayers()) {
			Gui openGui = Gui.getOpenGui(player);
			if (openGui instanceof GuildGui guildGui) {
				guildGui.refreshIfGuild(guildRoot);
			}
		}
	}
}
