package com.playmonumenta.plugins.integrations.luckperms;

import com.playmonumenta.plugins.Plugin;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;

public class PlayerGuildInfo {
	private final Group mGuild;
	private final GuildAccessLevel mAccessLevel;
	private final GuildInviteLevel mInviteLevel;
	private final boolean mIsLegacy;

	private PlayerGuildInfo(Group guild, GuildAccessLevel accessLevel, GuildInviteLevel inviteLevel, boolean isLegacy) {
		mGuild = guild;
		mAccessLevel = accessLevel;
		mInviteLevel = inviteLevel;
		mIsLegacy = isLegacy;
	}

	public static CompletableFuture<PlayerGuildInfo> of(User user, Group guild) {
		CompletableFuture<PlayerGuildInfo> future = new CompletableFuture<>();

		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			try {
				boolean isLegacy = !LuckPermsIntegration.isModern(guild);
				Group root;
				GuildAccessLevel accessLevel;
				GuildInviteLevel inviteLevel;
				if (isLegacy) {
					root = guild;
					accessLevel = GuildAccessLevel.NONE;
					inviteLevel = GuildInviteLevel.NONE;
				} else {
					root = LuckPermsIntegration.getGuildRoot(guild);
					if (root == null) {
						root = guild;
						isLegacy = true;
						accessLevel = GuildAccessLevel.NONE;
						inviteLevel = GuildInviteLevel.NONE;
					} else {
						accessLevel = LuckPermsIntegration.getAccessLevel(root, user);
						inviteLevel = LuckPermsIntegration.getInviteLevel(root, user);
					}
				}
				PlayerGuildInfo guildInfo = new PlayerGuildInfo(root, accessLevel, inviteLevel, isLegacy);
				future.complete(guildInfo);
			} catch (Exception ex) {
				future.completeExceptionally(ex);
			}
		});

		return future;
	}

	public static CompletableFuture<List<PlayerGuildInfo>> ofCollection(User user, Collection<Group> guilds) {
		CompletableFuture<List<PlayerGuildInfo>> future = new CompletableFuture<>();

		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			try {
				List<PlayerGuildInfo> result = new ArrayList<>();
				List<CompletableFuture<PlayerGuildInfo>> subFutures = new ArrayList<>();

				for (Group guild : guilds) {
					subFutures.add(PlayerGuildInfo.of(user, guild));
				}

				CompletableFuture.allOf(subFutures.toArray(new CompletableFuture[0])).join();

				for (CompletableFuture<PlayerGuildInfo> infoFuture : subFutures) {
					result.add(infoFuture.join());
				}

				future.complete(result);
			} catch (Exception ex) {
				future.completeExceptionally(ex);
			}
		});

		return future;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof PlayerGuildInfo other)) {
			return false;
		}
		return getGuild().equals(other.getGuild());
	}

	@Override
	public int hashCode() {
		int result = mGuild.hashCode();
		result = 31 * result + mAccessLevel.hashCode();
		result = 31 * result + mInviteLevel.hashCode();
		result = 31 * result + (mIsLegacy ? 1 : 0);
		return result;
	}

	public Group getGuild() {
		return mGuild;
	}

	public GuildAccessLevel getAccessLevel() {
		return mAccessLevel;
	}

	public GuildInviteLevel getInviteLevel() {
		return mInviteLevel;
	}

	public boolean isLegacy() {
		return mIsLegacy;
	}
}
