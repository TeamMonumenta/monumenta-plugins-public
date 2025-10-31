package com.playmonumenta.plugins.integrations.luckperms;

import com.playmonumenta.plugins.Plugin;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import net.luckperms.api.model.data.NodeMap;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

public enum GuildInviteLevel {
	MEMBER_INVITE("invite.member", "member", "Invited as Member"),
	GUEST_INVITE("invite.guest", "guest", "Invited as Guest"),
	NONE("none", "uninvite", "No Invite");

	public final String mId;
	public final String mArgument;
	public final String mDescription;

	GuildInviteLevel(String id, String argument, String description) {
		mId = id;
		mArgument = argument;
		mDescription = description;
	}

	public static GuildInviteLevel byGroup(Group group) {
		return byGroup(group.getName());
	}

	public static GuildInviteLevel byGroup(String group) {
		String[] idParts = group.split("\\.");
		if (idParts.length == 0) {
			return NONE;
		}
		if (idParts.length <= 2 || !"invite".equals(idParts[idParts.length - 2])) {
			return NONE;
		}
		return byId("invite." + idParts[idParts.length - 1]);
	}

	public static GuildInviteLevel byId(String id) {
		for (GuildInviteLevel level : values()) {
			if (level.mId.equals(id)) {
				return level;
			}
		}
		return NONE;
	}

	public String groupNameFromRoot(Group guildRoot) {
		return groupNameFromRoot(guildRoot.getName());
	}

	public String groupNameFromRoot(String guildRoot) {
		return guildRoot + "." + mId;
	}

	public @Nullable Group getGroupFromRoot(Group guildRoot) {
		return getGroupFromRoot(guildRoot.getName());
	}

	public @Nullable Group getGroupFromRoot(String guildRoot) {
		return LuckPermsIntegration.getGroup(groupNameFromRoot(guildRoot));
	}

	public CompletableFuture<Optional<Group>> loadGroupFromRoot(Group guildRoot) {
		return loadGroupFromRoot(guildRoot.getName());
	}

	public CompletableFuture<Optional<Group>> loadGroupFromRoot(String guildRoot) {
		return LuckPermsIntegration.loadGroup(groupNameFromRoot(guildRoot));
	}

	/**
	 * Updates the invite level of a player for a guild
	 *
	 * @param target      The player whose invite level is being updated
	 * @param guild       The guild for which that player's invite level is being updated
	 * @param targetLevel The desired invite level for that player in that guild
	 * @return A future for the User object for the target user
	 */
	public static CompletableFuture<User> setInviteLevel(User target, Group guild, GuildInviteLevel targetLevel) {
		CompletableFuture<User> future = new CompletableFuture<>();

		Group root = LuckPermsIntegration.getGuildRoot(guild);
		if (root == null) {
			future.completeExceptionally(new Exception("Provided group is not a valid guild!"));
			return future;
		}
		String rootIdWithSeparator = root.getName() + ".";

		NodeMap targetData = target.data();
		for (Node node : targetData.toCollection()) {
			if (!(node instanceof InheritanceNode inheritanceNode)) {
				continue;
			}

			if (inheritanceNode.getGroupName().startsWith(rootIdWithSeparator)) {
				GuildInviteLevel foundInviteLevel = byGroup(inheritanceNode.getGroupName());
				if (!NONE.equals(foundInviteLevel)) {
					targetData.remove(inheritanceNode);
				}
			}
		}

		if (NONE.equals(targetLevel)) {
			LuckPermsIntegration.pushUserUpdate(target);
			future.complete(target);
			return future;
		}

		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			try {
				Group targetGroup = targetLevel.loadGroupFromRoot(guild).join().orElse(null);
				if (targetGroup == null) {
					throw new Exception("Could not get " + targetLevel.mId + " for guild " + guild.getName());
				}
				targetData.add(InheritanceNode.builder().group(targetGroup).build());
				LuckPermsIntegration.pushUserUpdate(target);
				future.complete(target);
			} catch (Throwable ex) {
				future.completeExceptionally(ex);
			}
		});

		return future;
	}
}
