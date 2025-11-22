package com.playmonumenta.plugins.mail.recipient;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.luckperms.GuildPermission;
import com.playmonumenta.plugins.integrations.luckperms.LuckPermsIntegration;
import com.playmonumenta.plugins.integrations.luckperms.listeners.GuildArguments;
import com.playmonumenta.plugins.mail.NoMailAccessException;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import dev.jorel.commandapi.executors.CommandArguments;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GuildRecipient implements Recipient {
	public static class GuildRecipientCmdArgs extends RecipientCmdArgs {
		private final String mLabel;
		private final @Nullable Argument<String> mGuildNameArg;

		public GuildRecipientCmdArgs(ArgTarget target, String prefix, String suffix) {
			super(target);
			mLabel = prefix + "guild" + suffix;
			mRecipientArgs.add(new LiteralArgument("guild"));
			if (Objects.requireNonNull(target) == ArgTarget.ARG) {
				mGuildNameArg = new TextArgument(mLabel)
					.replaceSuggestions(GuildArguments.NAME_SUGGESTIONS);
				mRecipientArgs.add(mGuildNameArg);
			} else {
				mGuildNameArg = null;
			}
		}

		@Override
		public String label() {
			return mLabel;
		}

		@Override
		public CompletableFuture<Recipient> getRecipient(CommandSender sender, CommandArguments args) {
			CompletableFuture<Recipient> future = new CompletableFuture<>();

			switch (mTarget) {
				case ARG -> {
					if (mGuildNameArg == null) {
						future.completeExceptionally(new Exception(
							mLabel + " argument interpreter was not initialized correctly and cannot continue."));
					} else {
						String guildName = args.getByArgument(mGuildNameArg);
						getGuildRecipient(future, guildName);
					}
				}
				case CALLEE -> {
					CommandSender callee = CommandUtils.getCallee(sender);
					if (callee instanceof Player player) {
						getGuildRecipient(future, player);
					} else {
						future.completeExceptionally(
							new Exception(label() + " expects command to be run on a player."));
					}
				}
				case CALLER -> {
					CommandSender caller = CommandUtils.getCallee(sender);
					if (caller instanceof Player player) {
						getGuildRecipient(future, player);
					} else {
						future.completeExceptionally(
							new Exception(label() + " expects command to be run by a player."));
					}
				}
				default -> future.completeExceptionally(
					new Exception(label() + " is implemented incorrectly and cannot continue."));
			}

			return future;
		}

		private void getGuildRecipient(CompletableFuture<Recipient> future, Player player) {
			Group playerGuild = LuckPermsIntegration.getGuild(player);
			if (playerGuild == null) {
				future.completeExceptionally(new Exception("You are not in a guild!"));
				return;
			}
			Long playerGuildId = LuckPermsIntegration.getGuildPlotId(playerGuild);
			if (playerGuildId == null) {
				future.completeExceptionally(new Exception(
					"Your guild's plot is missing its numeric ID. Please report this to a moderator."));
				return;
			}
			future.complete(new GuildRecipient(playerGuildId, playerGuild));
		}

		private void getGuildRecipient(CompletableFuture<Recipient> future, String guildName) {
			String guildId = GuildArguments.getIdFromName(guildName);
			if (guildId == null) {
				future.completeExceptionally(new Exception("Could not identify guild by name: " + guildName));
				return;
			}

			Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
				Group guildGroup = LuckPermsIntegration.loadGroup(guildId).join().orElse(null);
				if (guildGroup == null) {
					future.completeExceptionally(new Exception("Unable to load guild " + guildName));
					return;
				}

				Long guildPlotId = LuckPermsIntegration.getGuildPlotId(guildGroup);
				if (guildPlotId == null) {
					future.completeExceptionally(new Exception("Could not get guild's numeric ID: " + guildName));
					return;
				}

				future.complete(new GuildRecipient(guildPlotId, guildGroup));
			});
		}
	}

	public static final Long DUMMY_ID_NOT_LOADED = -1L;
	public static final Long DUMMY_ID_NO_GUILD = -2L;
	public static final Long DUMMY_ID_NO_GUILD_NUMBER = -3L;

	private final long mGuildId;
	private @Nullable Group mGuildRoot;

	/**
	 * Creates a guild recipient; this should only be used by the GuildArguments LuckPerms listener
	 *
	 * @param guildId The numeric guild plot ID for the guild
	 * @param guild   The guild's root group
	 */
	public GuildRecipient(long guildId, @Nullable Group guild) {
		mGuildId = guildId;
		mGuildRoot = LuckPermsIntegration.getGuildRoot(guild);
	}

	public static CompletableFuture<@Nullable Recipient> ofRecipientIdString(String recipientId) {
		CompletableFuture<Recipient> future = new CompletableFuture<>();

		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			long guildId;
			try {
				guildId = Long.parseLong(recipientId);
			} catch (NumberFormatException ex) {
				MMLog.warning("[Mailbox] failed to load guild mailbox with recipient ID string: `" + recipientId + "`");
				MessagingUtils.sendStackTrace(Bukkit.getConsoleSender(), ex);
				future.complete(null);
				return;
			}

			Group guild;
			try {
				// If this is null, assume the guild was deleted, but the mailbox was valid
				// This allows the sender to take back their previously sent mail
				guild = LuckPermsIntegration.getGuildByPlotId(guildId).join();
			} catch (Exception ex) {
				MMLog.warning("[Mailbox] failed to load guild mailbox with recipient ID string: `" + recipientId + "`");
				MessagingUtils.sendStackTrace(Bukkit.getConsoleSender(), ex);
				future.complete(null);
				return;
			}

			future.complete(new GuildRecipient(guildId, guild));
		});

		return future;
	}

	public long getGuildId() {
		return mGuildId;
	}

	/**
	 * This should only be used by the GuildArguments LuckPerms listener to update existing recipients it manages
	 *
	 * @param guildRoot The updated guild group, if it exists
	 */
	public void updateGuildRoot(@Nullable Group guildRoot) {
		mGuildRoot = guildRoot;
	}

	public @Nullable Group getGuildRoot() {
		if (mGuildRoot == null) {
			mGuildRoot = LuckPermsIntegration.getLoadedGuildByPlotId(mGuildId);
		}
		return mGuildRoot;
	}

	@Override
	public RecipientType recipientType() {
		return RecipientType.GUILD;
	}

	@Override
	public String recipientIdStr() {
		return "" + mGuildId;
	}

	@Override
	public Audience audience() {
		Audience audience = Audience.empty();

		Group guildRoot = getGuildRoot();
		if (guildRoot == null) {
			return audience;
		}

		for (Player player : Bukkit.getOnlinePlayers()) {
			if (GuildPermission.MAIL.hasAccess(guildRoot, player)) {
				audience = Audience.audience(audience, player);
			}
		}

		return audience;
	}

	@Override
	public String friendlyStr(MailDirection mailDirection) {
		Group guildRoot = getGuildRoot();

		if (guildRoot == null) {
			if (mGuildId == DUMMY_ID_NOT_LOADED) {
				return mailDirection.title() + "Guild Not Loaded";
			} else if (mGuildId == DUMMY_ID_NO_GUILD) {
				return mailDirection.title() + "Guild Not Found";
			} else if (mGuildId == DUMMY_ID_NO_GUILD_NUMBER) {
				return mailDirection.title() + "Guild Lacking Numeric ID";
			} else if (mGuildId < 0) {
				return mailDirection.title() + "Unknown Guild";
			}

			return mailDirection.title() + "Guild #" + mGuildId;
		}

		return mailDirection.title() + LuckPermsIntegration.getNonNullGuildName(guildRoot);
	}

	@Override
	public Component friendlyComponent(MailDirection mailDirection) {
		Group guildRoot = getGuildRoot();

		Component root = Component.text(mailDirection.title())
			.decoration(TextDecoration.ITALIC, false);

		if (guildRoot == null) {
			return root
				.append(Component.text(friendlyStr(MailDirection.DEFAULT), NamedTextColor.GOLD, TextDecoration.BOLD));
		}

		return root
			.append(LuckPermsIntegration.getGuildFullComponent(guildRoot));
	}

	@Override
	public ItemStack icon(MailDirection mailDirection) {
		ItemStack item = LuckPermsIntegration.getGuildBanner(getGuildRoot());
		ItemMeta meta = item.getItemMeta();
		meta.displayName(friendlyComponent(mailDirection));
		meta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS);
		item.setItemMeta(meta);
		return item;
	}

	@Override
	public boolean nonMemberCheck(Player viewer, GuildPermission guildPermission) {
		Group guildRoot = getGuildRoot();

		if (guildRoot == null) {
			return true;
		}

		User user = LuckPermsIntegration.getUser(viewer);
		return !guildPermission.hasAccess(guildRoot, user);
	}

	@Override
	public void lockedCheck(Player viewer) throws NoMailAccessException {
		Group guildRoot = getGuildRoot();

		if (guildRoot == null) {
			return;
		}

		if (LuckPermsIntegration.isLocked(guildRoot)) {
			NoMailAccessException ex = new NoMailAccessException(friendlyStr(MailDirection.DEFAULT) + " is currently on lockdown");
			ex.closeGui(false);
			throw ex;
		}
	}

	@Override
	public int sentMailboxLimit() {
		return 20;
	}

	@Override
	public int receivedMailboxLimit() {
		return 40;
	}

	@Override
	public int mailboxCompareTo(@NotNull Recipient o) {
		if (!(o instanceof GuildRecipient other)) {
			return RecipientType.GUILD.compareTo(o.recipientType());
		}
		return friendlyStr(MailDirection.DEFAULT)
			.compareTo(other.friendlyStr(MailDirection.DEFAULT));
	}

	@Override
	public int compareTo(@NotNull Recipient o) {
		if (!(o instanceof GuildRecipient other)) {
			return RecipientType.GUILD.compareTo(o.recipientType());
		}
		return Long.compare(mGuildId, other.mGuildId);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof GuildRecipient other)) {
			return false;
		}
		return mGuildId == other.mGuildId;
	}

	@Override
	public int hashCode() {
		int result = RecipientType.GUILD.hashCode();
		result = 31 * result + Long.hashCode(mGuildId);
		return result;
	}
}
