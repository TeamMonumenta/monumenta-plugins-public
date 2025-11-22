package com.playmonumenta.plugins.integrations.luckperms;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.MonumentaRedisSyncIntegration;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.Nullable;

public class PlayerGuildInfo {
	private final User mUser;
	private final @Nullable String mPlayerName;
	private final Group mGuild;
	private final GuildAccessLevel mAccessLevel;
	private final GuildInviteLevel mInviteLevel;
	private final EnumSet<GuildPermission> mGuildPermissions;
	private final EnumSet<GuildFlag> mGuildFlags;

	private PlayerGuildInfo(User user,
	                        Group guild,
	                        GuildAccessLevel accessLevel,
	                        GuildInviteLevel inviteLevel,
	                        EnumSet<GuildPermission> guildPermissions,
	                        EnumSet<GuildFlag> guildFlags) {
		mUser = user;
		mPlayerName = MonumentaRedisSyncIntegration.cachedUuidToName(user.getUniqueId());
		mGuild = guild;
		mAccessLevel = accessLevel;
		mInviteLevel = inviteLevel;
		mGuildPermissions = guildPermissions;
		mGuildFlags = guildFlags;
	}

	public CompletableFuture<PlayerGuildInfo> getUpdated() {
		return of(mUser, mGuild);
	}

	public static CompletableFuture<PlayerGuildInfo> of(User user, Group guild) {
		CompletableFuture<PlayerGuildInfo> future = new CompletableFuture<>();

		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			try {
				Group root;
				GuildAccessLevel accessLevel;
				GuildInviteLevel inviteLevel;
				EnumSet<GuildPermission> guildPermissions;
				EnumSet<GuildFlag> guildFlags;
				root = LuckPermsIntegration.getGuildRoot(guild);
				if (root == null) {
					throw new RuntimeException("Could not find guild root for " + guild.getName());
				} else {
					accessLevel = LuckPermsIntegration.getAccessLevel(root, user);
					inviteLevel = LuckPermsIntegration.getInviteLevel(root, user);
					guildPermissions = EnumSet.noneOf(GuildPermission.class);
					for (GuildPermission guildPermission : GuildPermission.values()) {
						if (guildPermission.hasAccess(root, user)) {
							guildPermissions.add(guildPermission);
						}
					}
					guildFlags = EnumSet.noneOf(GuildFlag.class);
					for (GuildFlag guildFlag : GuildFlag.values()) {
						if (guildFlag.hasFlag(root)) {
							guildFlags.add(guildFlag);
						}
					}
				}
				PlayerGuildInfo guildInfo = new PlayerGuildInfo(
					user,
					root,
					accessLevel,
					inviteLevel,
					guildPermissions,
					guildFlags
				);
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
					subFutures.add(of(user, guild));
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
		return mUser.equals(other.mUser)
			&& Objects.equals(mPlayerName, other.mPlayerName)
			&& mGuild.equals(other.mGuild)
			&& mAccessLevel.equals(other.mAccessLevel)
			&& mInviteLevel.equals(other.mInviteLevel);
	}

	@Override
	public int hashCode() {
		int result = mUser.hashCode();
		result = 31 * result + (mPlayerName == null ? 0 : mPlayerName.hashCode());
		result = 31 * result + mGuild.hashCode();
		result = 31 * result + mAccessLevel.hashCode();
		result = 31 * result + mInviteLevel.hashCode();
		return result;
	}

	public User getUser() {
		return mUser;
	}

	public UUID getUniqueId() {
		return mUser.getUniqueId();
	}

	public @Nullable String getPlayerName() {
		return mPlayerName;
	}

	public String getNonNullName() {
		if (mPlayerName == null) {
			return mUser.getUniqueId().toString().toLowerCase(Locale.ROOT);
		}
		return mPlayerName;
	}

	public String getNameSortKey() {
		String tempString;
		if (mPlayerName == null) {
			tempString = "~Z~Z~Z~" + mUser.getUniqueId().toString().toLowerCase(Locale.ROOT);
		} else {
			tempString = mPlayerName;
		}
		return StringUtils.getNaturalSortKey(tempString);
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

	public EnumSet<GuildPermission> getGuildPermissions() {
		return mGuildPermissions.clone();
	}

	public EnumSet<GuildFlag> getGuildFlags() {
		return mGuildFlags.clone();
	}
}
