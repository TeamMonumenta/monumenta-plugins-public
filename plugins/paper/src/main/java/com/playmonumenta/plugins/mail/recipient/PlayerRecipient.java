package com.playmonumenta.plugins.mail.recipient;

import com.playmonumenta.plugins.integrations.MonumentaRedisSyncIntegration;
import com.playmonumenta.plugins.mail.NoMailAccessException;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.integrations.MonumentaRedisSyncIntegration.ALL_CACHED_PLAYER_NAMES_SUGGESTIONS;

public class PlayerRecipient implements Recipient {
	public static class PlayerRecipientCmdArgs extends RecipientCmdArgs {
		private final String mLabel;
		private final @Nullable Argument<String> mPlayerNameArg;

		public PlayerRecipientCmdArgs(ArgTarget target, String prefix, String suffix) {
			super(target);
			mLabel = prefix + "player" + suffix;
			mRecipientArgs.add(new LiteralArgument("player"));
			if (Objects.requireNonNull(target) == ArgTarget.ARG) {
				mPlayerNameArg = new TextArgument(mLabel)
					.replaceSuggestions(ALL_CACHED_PLAYER_NAMES_SUGGESTIONS);
				mRecipientArgs.add(mPlayerNameArg);
			} else {
				mPlayerNameArg = null;
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
					if (mPlayerNameArg == null) {
						future.completeExceptionally(new Exception(
								mLabel + " argument interpreter was not initialized correctly and cannot continue."));
					} else {
						String targetName = args.getByArgument(mPlayerNameArg);
						try {
							future.complete(new PlayerRecipient(targetName));
						} catch (WrapperCommandSyntaxException e) {
							future.completeExceptionally(new RuntimeException(e));
						}
					}
				}
				case CALLEE -> {
					CommandSender callee = CommandUtils.getCallee(sender);
					if (callee instanceof Player player) {
						future.complete(new PlayerRecipient(player.getUniqueId()));
					} else {
						future.completeExceptionally(
							new Exception(label() + " expects command to be run on a player."));
					}
				}
				case CALLER -> {
					CommandSender caller = CommandUtils.getCallee(sender);
					if (caller instanceof Player player) {
						future.complete(new PlayerRecipient(player.getUniqueId()));
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
	}

	private final UUID mPlayerId;

	public PlayerRecipient(String playerName) throws WrapperCommandSyntaxException {
		UUID playerUuid = MonumentaRedisSyncIntegration.cachedNameToUuid(playerName);
		if (playerUuid == null) {
			throw CommandAPI.failWithString("Could not identify that player - is the spelling/capitalization correct?");
		}
		mPlayerId = playerUuid;
	}

	public PlayerRecipient(UUID playerId) {
		mPlayerId = playerId;
	}

	public static CompletableFuture<@Nullable Recipient> ofRecipientIdString(String recipientId) {
		CompletableFuture<Recipient> future = new CompletableFuture<>();

		try {
			UUID playerId = UUID.fromString(recipientId);
			future.complete(new PlayerRecipient(playerId));
		} catch (IllegalArgumentException ex) {
			MMLog.warning("[Mailbox] failed to load player mailbox with recipient ID string: `" + recipientId + "`");
			MessagingUtils.sendStackTrace(Bukkit.getConsoleSender(), ex);
			future.complete(null);
		}

		return future;
	}

	public UUID getPlayerId() {
		return mPlayerId;
	}

	@Override
	public RecipientType recipientType() {
		return RecipientType.PLAYER;
	}

	@Override
	public String recipientIdStr() {
		return mPlayerId.toString().toLowerCase(Locale.ENGLISH);
	}

	@Override
	public Audience audience() {
		Player player = Bukkit.getPlayer(mPlayerId);
		if (player == null) {
			return Audience.empty();
		}
		return player;
	}

	@Override
	public String friendlyStr(MailDirection mailDirection) {
		String result = MonumentaRedisSyncIntegration.cachedUuidToName(mPlayerId);
		if (result == null) {
			return mailDirection.title() + recipientIdStr();
		}
		return mailDirection.title() + result;
	}

	@Override
	public Component friendlyComponent(MailDirection mailDirection) {
		Component root = Component.text(mailDirection.title())
			.decoration(TextDecoration.ITALIC, false);
		return root
			.append(Component.text(friendlyStr(MailDirection.DEFAULT), NamedTextColor.YELLOW));
	}

	@Override
	public ItemStack icon(MailDirection mailDirection) {
		ItemStack item = ItemUtils.createPlayerHead(mPlayerId, friendlyStr(MailDirection.DEFAULT));
		ItemMeta meta = item.getItemMeta();
		meta.displayName(friendlyComponent(mailDirection));
		item.setItemMeta(meta);
		return item;
	}

	@Override
	public boolean nonMemberCheck(Player viewer) {
		return !viewer.getUniqueId().equals(mPlayerId);
	}

	@Override
	public void lockedCheck(Player viewer) throws NoMailAccessException {
		OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(mPlayerId);
		if (Bukkit.getBannedPlayers().contains(offlinePlayer)) {
			NoMailAccessException ex = new NoMailAccessException(friendlyStr(MailDirection.DEFAULT) + " is banned!");
			ex.closeGui(false);
			throw ex;
		}
	}

	@Override
	public int sentMailboxLimit() {
		return 10;
	}

	@Override
	public int receivedMailboxLimit() {
		return 20;
	}

	@Override
	public int mailboxCompareTo(@NotNull Recipient o) {
		if (!(o instanceof PlayerRecipient other)) {
			return RecipientType.PLAYER.compareTo(o.recipientType());
		}
		return StringUtils.getNaturalSortKey(friendlyStr(MailDirection.DEFAULT))
			.compareTo(StringUtils.getNaturalSortKey(other.friendlyStr(MailDirection.DEFAULT)));
	}

	@Override
	public int compareTo(@NotNull Recipient o) {
		if (!(o instanceof PlayerRecipient other)) {
			return RecipientType.PLAYER.compareTo(o.recipientType());
		}
		return mPlayerId.compareTo(other.mPlayerId);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof PlayerRecipient other)) {
			return false;
		}
		return mPlayerId.equals(other.mPlayerId);
	}

	@Override
	public int hashCode() {
		int result = RecipientType.PLAYER.hashCode();
		result = 31 * result + mPlayerId.hashCode();
		return result;
	}
}
