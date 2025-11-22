package com.playmonumenta.plugins.integrations.luckperms;

import com.playmonumenta.plugins.Plugin;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.luckperms.api.model.PermissionHolder;
import net.luckperms.api.model.data.NodeMap;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PermissionNode;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public enum GuildPermission {
	// It's safe to re-order these; only affects the order in the GUI
	CHAT(
		"chat",
		"chat",
		GuildAccessLevel.MEMBER,
		true,
		"Access Chat",
		"Allows players to see and speak in a guild's chat channel (/gc)"
	),
	VISIT(
		"visit",
		"visit",
		GuildAccessLevel.MEMBER,
		true,
		"Visit Plot",
		"Allows players to visit a guild's plot and guild island"
	),
	VIEW_ITEMS(
		"view_items",
		"view_items",
		GuildAccessLevel.MEMBER,
		true,
		"View Items",
		"Allows players to view items in containers on a guild's plot"
	),
	MOVE_ITEMS(
		"move_items",
		"move_items",
		GuildAccessLevel.MEMBER,
		true,
		"Move Items",
		"Allows players to move items in containers on a guild's plot"
	),
	SURVIVAL(
		"survival",
		"survival",
		GuildAccessLevel.MEMBER,
		true,
		"Survival Mode",
		"Allows players to place and break blocks on a guild's plot"
	),
	USE_TRAVEL_ANCHOR(
		"use_travel_anchor",
		"use_travel_anchor",
		GuildAccessLevel.MEMBER,
		true,
		"Use Travel Anchors",
		"Allows players to teleport using travel anchors"
	),
	EDIT_TRAVEL_ANCHOR(
		"edit_travel_anchor",
		"edit_travel_anchor",
		GuildAccessLevel.MEMBER,
		true,
		"Edit Travel Anchors",
		"Allows players to make changes to travel anchors"
	),
	USE_VAULT(
		"use_vault",
		"use_vault",
		GuildAccessLevel.MEMBER,
		true,
		"Use Vault",
		"Allows players to use vaults owned by the guild"
	),
	EDIT_VAULT_OWNERSHIP(
		"edit_vault_ownership",
		"edit_vault_ownership",
		GuildAccessLevel.MANAGER,
		false,
		"Edit Vault Ownership",
		"Allows members to mark/unmark a vault as owned by the guild"
	),
	EGGS(
		"eggs",
		"eggs",
		GuildAccessLevel.MEMBER,
		false,
		"Use Eggs and Egginator",
		"Allows members to use spawn eggs, or an Egginator to pick up purchased entities"
	),
	GUILD_OWNED_INFUSION(
		"guild_owned_infusion",
		"guild_owned_infusion",
		GuildAccessLevel.MANAGER,
		false,
		"Manage Owned Infusion",
		"Allows members to interact with the guild's Owned infusion (via adding/removing/editing items)."
	),
	GUILD_SHOP(
		"guild_shop",
		"guild_shop",
		GuildAccessLevel.MANAGER,
		false,
		"Manage Guild Shop",
		"Allows members to manage a guild shop"
	),
	MAIL(
		"mail",
		"mail",
		GuildAccessLevel.MANAGER,
		false,
		"Access Mail",
		"Allows members to access mail to/from a guild"
	),
	MOVE_SPAWN(
		"move_spawn",
		"move_spawn",
		GuildAccessLevel.MANAGER,
		false,
		"Move the Guild Plot Spawn",
		"Allows members to move your guild plot's spawn location"
	),
	CHANGE_TIME(
		"change_time",
		"change_time",
		GuildAccessLevel.MANAGER,
		false,
		"Change the Guild Plot's Time",
		"Allows members to change your guild plot's time of day"
	),
	MANAGE_MEMBERSHIP(
		"manage_membership",
		"manage_membership",
		GuildAccessLevel.MANAGER,
		false,
		"Manage Invites and Access",
		"Allows members send and revoke invites, as well as promote/demote/kick/block players"
	),
	LOCKDOWN(
		"lockdown",
		"lockdown",
		GuildAccessLevel.MANAGER,
		false,
		"Lock Down the Guild",
		"Allows members to lock everyone out of the guild until a moderator intervenes"
	),
	;

	public static final String GUILD_PERM_PREFIX = "guild.perm.";
	public static final String DISABLE_GUILD_PERM_PREFIX = "guild.disable_perm.";
	public static final Pattern RE_GUILD_PERM_ID = Pattern.compile("^guild\\.perm\\.([^.]+)\\.([^.]+)$");

	public static class GuildPermissionResult {
		public final @Nullable PermissionHolder mCausingHolder;
		public final boolean mResult;

		protected GuildPermissionResult(@Nullable PermissionHolder causingHolder, boolean result) {
			mCausingHolder = causingHolder;
			mResult = result;
		}
	}

	public final String mSubPerm;
	public final String mArgument;
	public final GuildAccessLevel mDefaultAccessLevel;
	public final boolean mGuestPerm;
	public final String mLabel;
	public final String mDescription;

	GuildPermission(
		String subPerm,
		String argument,
		GuildAccessLevel defaultAccessLevel,
		boolean guestPerm,
		String label,
		String description
	) {
		mSubPerm = subPerm;
		mArgument = argument;
		mDefaultAccessLevel = defaultAccessLevel;
		mGuestPerm = guestPerm;
		mLabel = label;
		mDescription = description;
	}

	public @Nullable String guildPermissionString(Group guild) {
		String partialId = LuckPermsIntegration.getGuildPartialId(guild);
		if (partialId == null) {
			// Guild not found
			return null;
		}
		return guildPermissionStringPrefix(partialId) + mSubPerm;
	}

	// Used to disable a permission due to bugs
	public String globalDisablePermissionString() {
		return DISABLE_GUILD_PERM_PREFIX + mSubPerm;
	}

	public static String guildPermissionStringPrefix(String guildPlainTag) {
		String partialId = LuckPermsIntegration.getCleanLpString(guildPlainTag);
		return GUILD_PERM_PREFIX + partialId + ".";
	}

	public String guildPermissionStringFromPlainTag(String guildPlainTag) {
		String partialId = LuckPermsIntegration.getCleanLpString(guildPlainTag);
		return guildPermissionStringPrefix(partialId) + mSubPerm;
	}

	/**
	 * Checks if the specified permission holder was given explicit (non-inherited) access
	 *
	 * @param guild            The guild where the permission applies
	 * @param permissionHolder The user or group whose access is being checked (usually a player or the owning guild)
	 * @return True or false if explicitly set, otherwise null
	 */
	public @Nullable Boolean getExplicitPermission(Group guild, PermissionHolder permissionHolder) {
		String guildPerm = guildPermissionString(guild);
		if (guildPerm == null) {
			return null;
		}

		for (PermissionNode permissionNode : permissionHolder.getNodes(NodeType.PERMISSION)) {
			if (permissionNode.getPermission().equals(guildPerm)) {
				return permissionNode.getValue();
			}
		}
		return null;
	}

	/**
	 * Sets or clears a user's or guild's permission within a guild
	 *
	 * @param guild            The guild where the permission applies
	 * @param permissionHolder The user or group whose access is being modified (usually a player or the owning guild)
	 * @param value            Whether the permission is true, false, or falls through to the guild or default of false
	 */
	public CompletableFuture<Void> setExplicitPermission(Group guild, PermissionHolder permissionHolder, @Nullable Boolean value) {
		CompletableFuture<Void> future = new CompletableFuture<>();

		String guildPerm = guildPermissionString(guild);
		if (guildPerm == null) {
			future.complete(null);
			return future;
		}

		NodeMap data = permissionHolder.data();
		for (Node node : data.toCollection()) {
			if (!(node instanceof PermissionNode permissionNode)) {
				continue;
			}
			if (permissionNode.getPermission().equals(guildPerm)) {
				data.remove(permissionNode);
				break;
			}
		}

		if (value != null) {
			PermissionNode permissionNode = PermissionNode.builder().permission(guildPerm).value(value).build();
			data.add(permissionNode);
		}

		if (permissionHolder instanceof Group group) {
			LuckPermsIntegration.GM.saveGroup(group).join();
			LuckPermsIntegration.pushUpdate();
		} else if (permissionHolder instanceof User user) {
			LuckPermsIntegration.pushUserUpdate(user);
		}

		if (permissionHolder instanceof Group group) {
			Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
				LuckPermsIntegration.GM.saveGroup(group).join();
				LuckPermsIntegration.pushUpdate();
				future.complete(null);
			});
		} else if (permissionHolder instanceof User user) {
			LuckPermsIntegration.pushUserUpdate(user);
			future.complete(null);
		}

		return future;
	}

	public static CompletableFuture<Void> clearExplicitPermissions(PermissionHolder permissionHolder, String oldGuildTag) {
		CompletableFuture<Void> future = new CompletableFuture<>();

		String guildPermPrefix = guildPermissionStringPrefix(oldGuildTag);

		NodeMap data = permissionHolder.data();
		for (Node node : new ArrayList<>(data.toCollection())) {
			if (!(node instanceof PermissionNode permissionNode)) {
				continue;
			}

			String permId = permissionNode.getPermission();
			if (!permId.startsWith(guildPermPrefix)) {
				continue;
			}

			data.remove(permissionNode);
		}

		if (permissionHolder instanceof Group group) {
			Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
				LuckPermsIntegration.GM.saveGroup(group).join();
				LuckPermsIntegration.pushUpdate();
				future.complete(null);
			});
		} else if (permissionHolder instanceof User user) {
			LuckPermsIntegration.pushUserUpdate(user);
			future.complete(null);
		}

		return future;
	}

	public static CompletableFuture<Void> renameExplicitPermissions(Group newGuild, PermissionHolder permissionHolder, String oldGuildTag) {
		CompletableFuture<Void> future = new CompletableFuture<>();

		Map<String, String> renameMap = new HashMap<>();
		Map<String, @Nullable Boolean> permMap = new HashMap<>();
		for (GuildPermission guildPermission : values()) {
			String newGuildPerm = guildPermission.guildPermissionString(newGuild);
			if (newGuildPerm == null) {
				future.complete(null);
				return future;
			}

			String oldGuildPerm = guildPermission.guildPermissionStringFromPlainTag(oldGuildTag);

			renameMap.put(oldGuildPerm, newGuildPerm);
			permMap.put(oldGuildPerm, null);
			permMap.put(newGuildPerm, null);
		}

		NodeMap data = permissionHolder.data();
		for (Node node : new ArrayList<>(data.toCollection())) {
			if (!(node instanceof PermissionNode permissionNode)) {
				continue;
			}

			String permId = permissionNode.getPermission();
			if (!permMap.containsKey(permId)) {
				continue;
			}

			permMap.put(permId, permissionNode.getValue());
			data.remove(permissionNode);
		}

		for (Map.Entry<String, String> renameEntry : renameMap.entrySet()) {
			String oldPerm = renameEntry.getKey();
			String newPerm = renameEntry.getValue();

			@Nullable Boolean oldValue = permMap.get(oldPerm);
			@Nullable Boolean newValue = permMap.get(newPerm);

			if (newValue == null) {
				newValue = oldValue;
			}

			if (newValue != null) {
				PermissionNode permissionNode = PermissionNode.builder().permission(newPerm).value(newValue).build();
				data.add(permissionNode);
			}
		}

		if (permissionHolder instanceof Group group) {
			Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
				LuckPermsIntegration.GM.saveGroup(group).join();
				LuckPermsIntegration.pushUpdate();
				future.complete(null);
			});
		} else if (permissionHolder instanceof User user) {
			LuckPermsIntegration.pushUserUpdate(user);
			future.complete(null);
		}

		return future;
	}

	public boolean hasAccess(Group guild, Player player) {
		return hasAccess(guild, LuckPermsIntegration.getUser(player));
	}

	public boolean hasAccess(@Nullable Group guild, PermissionHolder permissionHolder) {
		return checkAccess(guild, permissionHolder).mResult;
	}

	public GuildPermissionResult checkAccess(@Nullable Group guild, PermissionHolder permissionHolder) {
		// Check if disabled due to bug
		if (LuckPermsIntegration.hasPermission(permissionHolder, globalDisablePermissionString()).orElse(false)) {
			return new GuildPermissionResult(null, false);
		}

		// Guild not found
		if (guild == null) {
			return new GuildPermissionResult(null, false);
		}
		Boolean value;

		// Permission set for this permissionHolder in particular
		value = getExplicitPermission(guild, permissionHolder);
		if (value != null) {
			return new GuildPermissionResult(permissionHolder, value);
		}

		// Permission set for the permissionHolder's group (or its parents)
		for (Group userParentGroup : permissionHolder.getInheritedGroups(QueryOptions.nonContextual())) {
			value = getExplicitPermission(guild, userParentGroup);
			if (value != null) {
				return new GuildPermissionResult(userParentGroup, value);
			}
		}

		// Public access check
		Group guildRoot = LuckPermsIntegration.getGuildRoot(guild);
		if (permissionHolder instanceof User user && guildRoot != null) {
			GuildAccessLevel accessLevel = LuckPermsIntegration.getAccessLevel(guildRoot, user);
			if (GuildAccessLevel.NONE.equals(accessLevel)) {
				Group noneGroup = GuildAccessLevel.NONE.getLoadedGroupFromRoot(guildRoot);
				if (noneGroup != null) {
					value = getExplicitPermission(guildRoot, noneGroup);
					if (value != null) {
						return new GuildPermissionResult(noneGroup, value);
					}
				}
			}
		}

		// No matches, denied
		return new GuildPermissionResult(null, false);
	}

	/**
	 * Gets the guild's root ID of a given GuildPermission node ID string
	 *
	 * @param permissionId The permission ID you wish to check
	 * @return The guild's root ID of the GuildPermission node, or null if not a GuildPermission node
	 */
	public static @Nullable String getGuildRootId(String permissionId) {
		Matcher matcher = RE_GUILD_PERM_ID.matcher(permissionId);
		if (!matcher.matches()) {
			return null;
		}
		return LuckPermsIntegration.GUILD_MK + "." + matcher.group(1);
	}

	/**
	 * Gets the GuildPermission from a given permission node
	 *
	 * @param permissionId The permission ID you wish to check
	 * @return The GuildPermission from a given permission ID, or null if not a GuildPermission
	 */
	public static @Nullable GuildPermission getGuildPermission(String permissionId) {
		Matcher matcher = RE_GUILD_PERM_ID.matcher(permissionId);
		if (!matcher.matches()) {
			return null;
		}
		String guildPermissionId = matcher.group(2);
		for (GuildPermission guildPermission : values()) {
			if (guildPermission.mSubPerm.equals(guildPermissionId)) {
				return guildPermission;
			}
		}
		return null;
	}
}
