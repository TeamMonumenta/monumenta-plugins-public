package com.playmonumenta.plugins.integrations.luckperms.listeners;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.guis.Gui;
import com.playmonumenta.plugins.integrations.MonumentaNetworkChatIntegration;
import com.playmonumenta.plugins.integrations.luckperms.GuildAccessLevel;
import com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration;
import com.playmonumenta.plugins.integrations.luckperms.TeleportGuildGui;
import com.playmonumenta.plugins.utils.MMLog;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.luckperms.api.event.EventBus;
import net.luckperms.api.event.node.NodeAddEvent;
import net.luckperms.api.event.node.NodeRemoveEvent;
import net.luckperms.api.event.sync.PostSyncEvent;
import net.luckperms.api.event.sync.PreNetworkSyncEvent;
import net.luckperms.api.event.sync.PreSyncEvent;
import net.luckperms.api.event.user.UserLoadEvent;
import net.luckperms.api.model.PermissionHolder;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.MetaNode;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import static com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration.getLoadedUsers;

public class RefreshChat {
	private static final Map<String, Map<String, String>> mGuildDataPreSync = new HashMap<>();
	private static final Map<UUID, Set<String>> mUserDataPreSync = new HashMap<>();

	public static void registerLuckPermsEvents(Plugin plugin, EventBus eventBus) {
		eventBus.subscribe(plugin, NodeAddEvent.class, RefreshChat::nodeAddEvent);
		eventBus.subscribe(plugin, NodeRemoveEvent.class, RefreshChat::nodeRemoveEvent);

		eventBus.subscribe(plugin, PreSyncEvent.class, RefreshChat::preSyncEvent);
		eventBus.subscribe(plugin, PostSyncEvent.class, RefreshChat::postSyncEvent);
		eventBus.subscribe(plugin, PreNetworkSyncEvent.class, RefreshChat::preNetworkSyncEvent);
		eventBus.subscribe(plugin, UserLoadEvent.class, RefreshChat::userLoadEvent);
	}

	public static void nodeRemoveEvent(NodeRemoveEvent event) {
		PermissionHolder holder = event.getTarget();

		if (holder instanceof Group group) {
			Node node = event.getNode();
			if (!(node instanceof MetaNode metaNode)) {
				return;
			}

			refreshChat(group, metaNode);
		} else if (holder instanceof User user) {
			Node node = event.getNode();
			if (node instanceof InheritanceNode) {
				refreshChat(user);
			}
		}
	}

	public static void nodeAddEvent(NodeAddEvent event) {
		PermissionHolder holder = event.getTarget();

		if (holder instanceof Group group) {
			Node node = event.getNode();
			if (!(node instanceof MetaNode metaNode)) {
				return;
			}

			refreshChat(group, metaNode);
		} else if (holder instanceof User user) {
			Node node = event.getNode();
			if (node instanceof InheritanceNode) {
				refreshChat(user);
			}
		}
	}

	public static void preSyncEvent(PreSyncEvent event) {
		// Make sure these are empty.
		mGuildDataPreSync.clear();
		mUserDataPreSync.clear();

		MMLog.fine("[Chat Refresh Listener/PreSync] Loading current guild/user data");
		for (Group guildMemberGroup : LuckPermsIntegration.getLoadedGuildLevelGroup(GuildAccessLevel.MEMBER)) {
			Map<String, String> chatMetaNodes = new HashMap<>();
			for (MetaNode node : guildMemberGroup.getNodes(NodeType.META)) {
				String metaKey = node.getMetaKey();
				if (LuckPermsIntegration.CHAT_META_KEYS.contains(metaKey)) {
					chatMetaNodes.put(metaKey, node.getMetaValue());
				}
			}
			if (!chatMetaNodes.isEmpty()) {
				mGuildDataPreSync.put(guildMemberGroup.getName(), chatMetaNodes);
			}
		}

		for (User user : getLoadedUsers()) {
			Set<String> userGroups = new HashSet<>();
			for (Group group : user.getInheritedGroups(QueryOptions.nonContextual())) {
				userGroups.add(group.getName());
			}
			mUserDataPreSync.put(user.getUniqueId(), userGroups);
		}
	}

	public static void postSyncEvent(PostSyncEvent event) {
		anyPostSyncEvent("PostSync");
	}

	public static void preNetworkSyncEvent(PreNetworkSyncEvent event) {
		// Make sure these are empty.
		mGuildDataPreSync.clear();
		mUserDataPreSync.clear();

		MMLog.fine("[Chat Refresh Listener/PreNetworkSync] Loading current guild/user data");
		for (Group guildMemberGroup : LuckPermsIntegration.getLoadedGuildLevelGroup(GuildAccessLevel.MEMBER)) {
			Map<String, String> chatMetaNodes = new HashMap<>();
			for (MetaNode node : guildMemberGroup.getNodes(NodeType.META)) {
				String metaKey = node.getMetaKey();
				if (LuckPermsIntegration.CHAT_META_KEYS.contains(metaKey)) {
					chatMetaNodes.put(metaKey, node.getMetaValue());
				}
			}
			if (!chatMetaNodes.isEmpty()) {
				mGuildDataPreSync.put(guildMemberGroup.getName(), chatMetaNodes);
			}
		}

		for (User user : getLoadedUsers()) {
			Set<String> userGroups = new HashSet<>();
			for (Group group : user.getInheritedGroups(QueryOptions.nonContextual())) {
				userGroups.add(group.getName());
			}
			mUserDataPreSync.put(user.getUniqueId(), userGroups);
		}
	}

	public static void userLoadEvent(UserLoadEvent event) {
		anyPostSyncEvent("UserLoad");
	}

	public static void anyPostSyncEvent(String eventLabel) {
		MMLog.fine("[Chat Refresh Listener/" + eventLabel + "] Got event");
		if (mUserDataPreSync.isEmpty()) {
			// No user data to compare against (stealthmods have A permission group and should be included if nothing else)
			MMLog.fine("[Chat Refresh Listener/" + eventLabel + "] No user data to compare against");
			return;
		}

		// Check guilds only for online players, then use these as shortcuts for other guild members
		Set<Group> modifiedGuildGroups = new HashSet<>();
		Set<Group> unmodifiedGuildGroups = new HashSet<>();

		MMLog.fine("[Chat Refresh Listener/" + eventLabel + "] Checking recent update");
		doneWithUser:
		for (User user : getLoadedUsers()) {
			Set<String> oldUserGroups = mUserDataPreSync.get(user.getUniqueId());
			if (oldUserGroups == null) {
				// User was not online and may be ignored; handled by login listener
				continue;
			}

			Set<String> newUserGroups = new HashSet<>();
			for (Group group : user.getInheritedGroups(QueryOptions.nonContextual())) {
				// Shortcut for members of guilds that we know have been modified
				if (modifiedGuildGroups.contains(group)) {
					refreshChat(user);
					continue doneWithUser;
				}
				newUserGroups.add(group.getName());
			}

			if (!oldUserGroups.equals(newUserGroups)) {
				// Player's inherited groups changed, could include patron/mod/guild access changes
				refreshChat(user);
				continue;
			}

			// Get the player's guild, but as a member (if they were only a guest, getGuild would return null)
			Group accessLevelGroup = LuckPermsIntegration.getGuild(user);
			Group guildRoot = LuckPermsIntegration.getGuildRoot(accessLevelGroup);
			if (guildRoot == null) {
				// Player is not in a modern guild
				continue;
			}
			Group memberGroup = GuildAccessLevel.MEMBER.getLoadedGroupFromRoot(guildRoot);
			if (memberGroup == null) {
				// The previous continue statement should have handled this
				continue;
			}

			// Second shortcut check on loaded/unloaded guild groups;
			// otherwise founders/managers might not get caught where members would
			if (unmodifiedGuildGroups.contains(memberGroup)) {
				unmodifiedGuildGroups.add(accessLevelGroup);
				continue;
			} else if (modifiedGuildGroups.contains(memberGroup)) {
				modifiedGuildGroups.add(accessLevelGroup);
				refreshChat(user);
				continue;
			}

			// Get previous data for the guild group
			Map<String, String> oldMetaNodes = mGuildDataPreSync.get(memberGroup.getName());
			if (oldMetaNodes == null) {
				// Previously unloaded, assume modified
				modifiedGuildGroups.add(memberGroup);
				refreshChat(user);
				continue;
			}

			// One-time check per member group if it has been modified
			for (MetaNode node : memberGroup.getNodes(NodeType.META)) {
				String metaKey = node.getMetaKey();
				if (!LuckPermsIntegration.CHAT_META_KEYS.contains(metaKey)) {
					// Not relevant to chat
					continue;
				}

				// Remove old keys as we go - if we don't remove all of them, then something was modified
				String oldMetaValue = oldMetaNodes.remove(metaKey);
				if (!node.getMetaValue().equals(oldMetaValue)) {
					modifiedGuildGroups.add(accessLevelGroup);
					modifiedGuildGroups.add(memberGroup);
					refreshChat(user);
					continue doneWithUser;
				}
			}

			// Verify all keys were present
			if (oldMetaNodes.isEmpty()) {
				unmodifiedGuildGroups.add(accessLevelGroup);
				unmodifiedGuildGroups.add(memberGroup);
			} else {
				modifiedGuildGroups.add(accessLevelGroup);
				modifiedGuildGroups.add(memberGroup);
				refreshChat(user);
			}
		}

		// Clean up variables until the next time they're set up
		mUserDataPreSync.clear();
		mGuildDataPreSync.clear();
	}

	private static void refreshChat(User user) {
		Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
			Player player = Plugin.getInstance().getPlayer(user.getUniqueId());
			if (player == null) {
				return; // Player is not loaded locally - they are most likely in the process of logging in
			}

			MMLog.fine("[Chat Refresh Listener] Refreshing chat state for " + player.getName());
			MonumentaNetworkChatIntegration.refreshPlayer(player);
			LuckPermsIntegration.updatePlayerGuildChat(player);
			if (Gui.getOpenGui(player) instanceof TeleportGuildGui teleportGuildGui) {
				teleportGuildGui.refresh();
			}
		});
	}

	private static void refreshChat(Group guildGroup, MetaNode editedNode) {
		Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
			String metaKey = editedNode.getMetaKey();

			if (!LuckPermsIntegration.CHAT_META_KEYS.contains(metaKey)) {
				return;
			}
			// Edit in either root group or member group so check which one it is.

			Group rootGroup = LuckPermsIntegration.getGuildRoot(guildGroup);
			if (rootGroup == null) {
				MMLog.warning("<!> Missing local guild for group '" + guildGroup.getName() + "'");
				return;
			}

			for (Player player : LuckPermsIntegration.getOnlineGuildMembers(rootGroup, true)) {
				MonumentaNetworkChatIntegration.refreshPlayer(player);
			}
		});
	}
}
