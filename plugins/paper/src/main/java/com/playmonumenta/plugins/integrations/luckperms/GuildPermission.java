package com.playmonumenta.plugins.integrations.luckperms;

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
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public enum GuildPermission {
	GUILD_SHOP(
		"guild_shop",
		GuildAccessLevel.MANAGER,
		false,
		"Manage Guild Shop",
		"Allows members to manage a guild shop"
	),
	MAIL(
		"mail",
		GuildAccessLevel.MANAGER,
		false,
		"Access Mail",
		"Allows members to access mail to/from a guild"
	),
	CHAT(
		"chat",
		GuildAccessLevel.MEMBER,
		true,
		"Access Chat",
		"Allows players to see and speak in a guild's chat channel (/gc)"
	),
	VISIT(
		"visit",
		GuildAccessLevel.MEMBER,
		true,
		"Visit Plot",
		"Allows guests to visit a guild's plot and guild island"
	),
	/* TODO: Add these once we add move guild plots into worlds, which makes these checks easier
	ITEMS(
		"items",
		GuildAccessLevel.MEMBER,
		true,
		"Access Items",
		"Allows players to interact with items on a guild's plot"
	),
	SURVIVAL(
		"survival",
		GuildAccessLevel.MEMBER,
		true,
		"Survival Mode",
		"Allows guests to place and break blocks on a guild's plot"
	),// */
	;

	public static final String GUILD_PERM_PREFIX = "guild.perm.";
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
	public final GuildAccessLevel mDefaultAccessLevel;
	public final boolean mGuestPerm;
	public final String mLabel;
	public final String mDescription;

	GuildPermission(
		String subPerm,
		GuildAccessLevel defaultAccessLevel,
		boolean guestPerm,
		String label,
		String description
	) {
		mSubPerm = subPerm;
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
		return GUILD_PERM_PREFIX + partialId + "." + mSubPerm;
	}

	public String guildPermissionStringFromPlainTag(String guildPlainTag) {
		String partialId = LuckPermsIntegration.getCleanLpString(guildPlainTag);
		return GUILD_PERM_PREFIX + partialId + "." + mSubPerm;
	}

	/**
	 * Checks if the specified permission holder was given explicit (non-inherited) access
	 * @param guild The guild where the permission applies
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
	 * @param guild The guild where the permission applies
	 * @param permissionHolder The user or group whose access is being modified (usually a player or the owning guild)
	 * @param value Whether the permission is true, false, or falls through to the guild or default of false
	 */
	public void setExplicitPermission(Group guild, PermissionHolder permissionHolder, @Nullable Boolean value) {
		String guildPerm = guildPermissionString(guild);
		if (guildPerm == null) {
			return;
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
			LuckPermsIntegration.GM.saveGroup(group);
			LuckPermsIntegration.pushUpdate();
		} else if (permissionHolder instanceof User user) {
			LuckPermsIntegration.pushUserUpdate(user);
		}
	}

	public boolean hasAccess(Group guild, Player player) {
		return hasAccess(guild, LuckPermsIntegration.getUser(player));
	}

	public boolean hasAccess(Group guild, PermissionHolder permissionHolder) {
		return checkAccess(guild, permissionHolder).mResult;
	}

	public GuildPermissionResult checkAccess(Group guild, PermissionHolder permissionHolder) {
		Boolean value;

		value = getExplicitPermission(guild, permissionHolder);
		if (value != null) {
			return new GuildPermissionResult(permissionHolder, value);
		}

		for (Group userParentGroup : permissionHolder.getInheritedGroups(QueryOptions.nonContextual())) {
			value = getExplicitPermission(guild, userParentGroup);
			if (value != null) {
				return new GuildPermissionResult(userParentGroup, value);
			}
		}

		return new GuildPermissionResult(null, false);
	}

	/**
	 * Gets the guild's root ID of a given GuildPermission node ID string
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
