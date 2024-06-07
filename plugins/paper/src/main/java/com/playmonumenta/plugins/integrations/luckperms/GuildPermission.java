package com.playmonumenta.plugins.integrations.luckperms;

import net.luckperms.api.model.PermissionHolder;
import net.luckperms.api.model.data.NodeMap;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PermissionNode;
import net.luckperms.api.query.QueryOptions;
import org.jetbrains.annotations.Nullable;

public enum GuildPermission {
	CHAT("chat", "Access Chat", "Allows guests to see and speak in a guild's chat channel (/gc)"),
	VISIT("visit", "Visit Plot", "Allows guests to visit a guild's plot and guild island");
	// TODO: Add these once we add move guild plots into worlds, which makes these checks easier
	//ITEMS("items", "Access Items", "Allows guests to interact with items on a guild's plot"),
	//SURVIVAL("survival", "Survival Mode", "Allows guests to place and break blocks on a guild's plot");

	public static final String GUILD_PERM_PREFIX = "guild.perm.";

	public static class GuildPermissionResult {
		public final @Nullable PermissionHolder mCausingHolder;
		public final boolean mResult;

		protected GuildPermissionResult(@Nullable PermissionHolder causingHolder, boolean result) {
			mCausingHolder = causingHolder;
			mResult = result;
		}
	}

	public final String mSubPerm;
	public final String mLabel;
	public final String mDescription;

	GuildPermission(String subPerm, String label, String description) {
		mSubPerm = subPerm;
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
}
