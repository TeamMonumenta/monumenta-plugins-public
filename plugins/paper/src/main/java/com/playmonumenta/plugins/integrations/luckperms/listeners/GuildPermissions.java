package com.playmonumenta.plugins.integrations.luckperms.listeners;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.guis.Gui;
import com.playmonumenta.plugins.integrations.luckperms.GuildPermission;
import com.playmonumenta.plugins.integrations.luckperms.GuildPlotUtils;
import com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration;
import com.playmonumenta.plugins.integrations.luckperms.TeleportGuildGui;
import com.playmonumenta.plugins.integrations.luckperms.guildgui.GuildGui;
import com.playmonumenta.plugins.mail.MailMan;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.node.NodeAddEvent;
import net.luckperms.api.event.node.NodeRemoveEvent;
import net.luckperms.api.event.sync.PostSyncEvent;
import net.luckperms.api.event.user.UserLoadEvent;
import net.luckperms.api.model.PermissionHolder;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.PermissionNode;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import static com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration.getLoadedUsers;

public class GuildPermissions implements Listener {
	private static final ConcurrentMap<UUID, ConcurrentMap<String, ConcurrentSkipListSet<GuildPermission>>> mUserData
		= new ConcurrentHashMap<>();

	@SuppressWarnings("resource")
	public static void registerLuckPermsEvents(Plugin plugin, EventBus eventBus) {
		eventBus.subscribe(plugin, NodeAddEvent.class, GuildPermissions::nodeAddEvent);
		eventBus.subscribe(plugin, NodeRemoveEvent.class, GuildPermissions::nodeRemoveEvent);

		eventBus.subscribe(plugin, PostSyncEvent.class, GuildPermissions::postSyncEvent);
		eventBus.subscribe(plugin, UserLoadEvent.class, GuildPermissions::userLoadEvent);
	}

	public static void nodeRemoveEvent(NodeRemoveEvent event) {
		PermissionHolder holder = event.getTarget();

		if (holder instanceof Group group) {
			Node node = event.getNode();
			if (node instanceof PermissionNode permissionNode) {
				GuildPermission guildPermission = GuildPermission.getGuildPermission(permissionNode.getPermission());
				if (guildPermission == null) {
					return;
				}
				refreshPermissions(group, guildPermission);
			}
		} else if (holder instanceof User user) {
			Node node = event.getNode();
			if (node instanceof InheritanceNode) {
				refreshPermissions(user);
			} else if (node instanceof PermissionNode permissionNode) {
				String permissionId = permissionNode.getPermission();
				String guildId = GuildPermission.getGuildRootId(permissionId);
				GuildPermission guildPermission = GuildPermission.getGuildPermission(permissionId);
				if (guildId == null || guildPermission == null) {
					return;
				}
				refreshPermissions(user, guildId, guildPermission);
			}
		}
	}

	public static void nodeAddEvent(NodeAddEvent event) {
		PermissionHolder holder = event.getTarget();

		if (holder instanceof Group group) {
			Node node = event.getNode();
			if (node instanceof PermissionNode permissionNode) {
				GuildPermission guildPermission = GuildPermission.getGuildPermission(permissionNode.getPermission());
				if (guildPermission == null) {
					return;
				}
				refreshPermissions(group, guildPermission);
			}
		} else if (holder instanceof User user) {
			Node node = event.getNode();
			if (node instanceof InheritanceNode) {
				refreshPermissions(user);
			} else if (node instanceof PermissionNode permissionNode) {
				String permissionId = permissionNode.getPermission();
				String guildId = GuildPermission.getGuildRootId(permissionId);
				GuildPermission guildPermission = GuildPermission.getGuildPermission(permissionId);
				if (guildId == null || guildPermission == null) {
					return;
				}
				refreshPermissions(user, guildId, guildPermission);
			}
		}
	}

	public static void postSyncEvent(PostSyncEvent event) {
		anyPostSyncEvent("PostSync");
	}

	public static void userLoadEvent(UserLoadEvent event) {
		mUserData.computeIfAbsent(event.getUser().getUniqueId(), k -> new ConcurrentHashMap<>());
		anyPostSyncEvent("UserLoad");
	}

	public static void anyPostSyncEvent(String eventLabel) {
		MMLog.fine("[Guild Permissions Listener/" + eventLabel + "] Checking recent update");
		for (User user : getLoadedUsers()) {
			refreshPermissions(user);
		}
	}

	// Local events
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void playerJoinEvent(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		mUserData.computeIfAbsent(player.getUniqueId(), k -> new ConcurrentHashMap<>());
		refreshPermissions(player);
		GuildPlotUtils.guildPlotAccessCheckAndKick(player);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void playerQuitEvent(PlayerQuitEvent event) {
		mUserData.remove(event.getPlayer().getUniqueId());
	}

	public static void refreshPermissions(Group group, GuildPermission guildPermission) {
		Group guildRoot = LuckPermsIntegration.getGuildRoot(group);
		if (guildRoot == null) {
			return;
		}
		Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
			for (Player player : Bukkit.getOnlinePlayers()) {
				User user = LuckPermsIntegration.getUser(player);
				if (user.getInheritedGroups(QueryOptions.defaultContextualOptions()).contains(guildRoot)) {
					refreshPermissions(user, guildRoot.getName(), guildPermission);
				} else if (GuildPermission.VISIT.equals(guildPermission)) {
					// Check anyway (for public visit access)
					GuildPlotUtils.guildPlotAccessCheckAndKick(player);
				}
			}
		});
	}

	public static void refreshPermissions(User user, String guildId, GuildPermission guildPermission) {
		Group guild = LuckPermsIntegration.loadGroup(guildId).join().orElse(null);
		Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
			UUID playerId = user.getUniqueId();
			Player player = Bukkit.getPlayer(playerId);
			if (player == null) {
				return;
			}
			ConcurrentSkipListSet<GuildPermission> guildPermissions = mUserData
				.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>())
				.computeIfAbsent(guildId, k -> new ConcurrentSkipListSet<>());
			if (guildPermission.hasAccess(guild, user)) {
				if (guildPermissions.add(guildPermission)) {
					applyPermissionChange(player, guildId, guildPermission, true);
				}
			} else {
				if (guildPermissions.remove(guildPermission)) {
					applyPermissionChange(player, guildId, guildPermission, false);
				}
			}
		});
	}

	public static void refreshPermissions(Player player) {
		if (player == null) {
			return; // Player is not loaded locally - they are most likely in the process of logging in
		}

		MMLog.fine("[Guild Permissions Listener] Refreshing guild permissions for " + player.getName());
		refreshPermissions(player, LuckPermsIntegration.getUser(player));
	}

	private static void refreshPermissions(User user) {
		Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
			Player player = Bukkit.getPlayer(user.getUniqueId());
			if (player == null) {
				return;
			}
			refreshPermissions(player, user);
		});
	}

	private static void refreshPermissions(Player player, User user) {
		final ConcurrentMap<String, ConcurrentSkipListSet<GuildPermission>> userData = mUserData.get(user.getUniqueId());
		if (userData == null) {
			// Not online
			return;
		}
		Set<Group> relevantGuilds = LuckPermsIntegration.getRelevantGuilds(user, true, false);

		Set<String> kickedFromGuilds = new HashSet<>(userData.keySet());
		for (Group guild : relevantGuilds) {
			Group guildRoot = LuckPermsIntegration.getGuildRoot(guild);
			if (guildRoot == null) {
				continue;
			}
			String guildName = guildRoot.getName();
			kickedFromGuilds.remove(guildName);

			ConcurrentSkipListSet<GuildPermission> recordedGuildPerms
				= userData.computeIfAbsent(guildName, k -> new ConcurrentSkipListSet<>());
			for (GuildPermission guildPermission : GuildPermission.values()) {
				boolean hasPerm = guildPermission.hasAccess(guildRoot, user);
				if (!hasPerm && recordedGuildPerms.remove(guildPermission)) {
					applyPermissionChange(player, guildName, guildPermission, false);
				}
				if (hasPerm && recordedGuildPerms.add(guildPermission)) {
					applyPermissionChange(player, guildName, guildPermission, true);
				}
			}
		}

		for (String guildId : kickedFromGuilds) {
			ConcurrentSkipListSet<GuildPermission> guildPerms = userData.remove(guildId);
			for (GuildPermission guildPermission : guildPerms) {
				applyPermissionChange(player, guildId, guildPermission, false);
			}
		}
	}

	private static void applyPermissionChange(
		Player player,
		String guildId,
		GuildPermission guildPermission,
		boolean isEnabled
	) {
		MMLog.fine(
			"[Guild Permissions Listener/Permission Change] " + player.getName()
				+ " had their " + guildPermission.mLabel
				+ " permission " + (isEnabled ? "granted" : "revoked")
				+ " in guild ID " + guildId
				+ "; updating relevant code"
		);
		Gui openGui = Gui.getOpenGui(player);
		if (openGui instanceof GuildGui guildGui) {
			guildGui.refresh();
		}
		if (GuildPermission.VISIT.equals(guildPermission)) {
			if (openGui instanceof TeleportGuildGui teleportGuildGui) {
				teleportGuildGui.refresh();
			}
			GuildPlotUtils.guildPlotAccessCheckAndKick(player);
		}
		if (GuildPermission.MAIL.equals(guildPermission)) {
			MailMan.playerGuildChange(player);
		}
		if (GuildPermission.SURVIVAL.equals(guildPermission)) {
			ZoneUtils.setExpectedGameMode(player, true);
		}
	}

}
