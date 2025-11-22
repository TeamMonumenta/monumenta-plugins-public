package com.playmonumenta.plugins.social;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.TABIntegration;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import static com.playmonumenta.plugins.integrations.MonumentaRedisSyncIntegration.ALL_CACHED_PLAYER_NAMES_SUGGESTIONS;
import static com.playmonumenta.plugins.integrations.MonumentaRedisSyncIntegration.ALL_OTHER_CACHED_PLAYER_NAMES_SUGGESTIONS;

public class FriendCommand {
	private static final String COMMAND = "friend";
	private static final String ALIAS = "f";
	private static final String SUBCOMMAND_ADD = "add";
	private static final String SUBCOMMAND_REMOVE = "remove";
	private static final String SUBCOMMAND_LIST = "list";
	private static final String SUBCOMMAND_LIST_RAW = "list_raw";
	private static final String SUBCOMMAND_ADD_OTHER = "add_other";
	private static final String SUBCOMMAND_REMOVE_OTHER = "remove_other";
	private static final String SUBCOMMAND_LIST_OTHER = "list_other";
	private static final String SUBCOMMAND_LIST_RAW_OTHER = "list_raw_other";
	private static final String SUBCOMMAND_CHECK = "check";
	private static final String PERMISSION_ADD_OTHER = "monumenta.social.friend.addother";
	private static final String PERMISSION_REMOVE_OTHER = "monumenta.social.friend.removeother";
	private static final String PERMISSION_LIST_OTHER = "monumenta.social.friend.listother";
	private static final String PERMISSION_CHECK = "monumenta.social.friend.check";

	public static void register() {
		new CommandAPICommand(COMMAND)
			.withAliases(ALIAS)

			//region <PUBLIC COMMANDS>
			/* Public commands:
			- /friend add <player>
			- /friend remove <player>
			- /friend list
			- /friend list_raw [page]
			 */

			/* /friend add <player> */
			.withSubcommand(new CommandAPICommand(SUBCOMMAND_ADD)
				// Suggest all online players except for the command sender and those who are vanished
				.withArguments(new StringArgument("player").replaceSuggestions(ALL_OTHER_CACHED_PLAYER_NAMES_SUGGESTIONS))
				.executesPlayer((sender, args) -> {
					SocialManager.sendFriendRequest(sender.getUniqueId(), StringUtils.getUuidFromInput((String) args.get("player")));
				})
			)

			/* /friend remove <player> */
			.withSubcommand(new CommandAPICommand(SUBCOMMAND_REMOVE)
				// Suggest all added friends
				.withArguments(new StringArgument("player")
					.replaceSuggestions(ArgumentSuggestions.stringsAsync((info) -> {
						if (!(info.sender() instanceof Player sender)) {
							// Return no suggestions if the command sender isn't a player
							return CompletableFuture.completedFuture(new String[0]);
						}

						UUID senderUuid = sender.getUniqueId();

						// Load the sender's social cache from memory or from Redis
						return SocialManager.loadSocialCaches(senderUuid).thenApply(socialCaches -> {
							if (socialCaches == null) {
								return new String[0];
							}

							PlayerSocialCache senderCache = socialCaches.get(senderUuid);

							// This shouldn't occur if loadSocialCaches didn't return null
							if (senderCache == null) {
								return new String[0];
							}

							return senderCache.getFriends().stream()
								.map(SocialManager::resolveName)
								.filter(Objects::nonNull)
								.toArray(String[]::new);
						});
					}))
				)
				.executesPlayer((sender, args) -> {
					SocialManager.removeFriend(null, sender.getUniqueId(), StringUtils.getUuidFromInput((String) args.get("player")));
				})
			)

			/* /friend list */
			.withSubcommand(new CommandAPICommand(SUBCOMMAND_LIST)
				.executesPlayer((sender, args) -> {
					String senderName = sender.getName();
					UUID senderUuid = sender.getUniqueId();

					SocialManager.getSocialDisplayInfo(senderUuid).whenCompleteAsync((senderSocialDisplayInfo, throwable) -> {
						if (throwable != null) {
							MMLog.severe("Caught exception trying to list friends for player " + senderName + " : " + throwable.getMessage());
							sender.sendMessage(Component.text("An error occurred while trying to list friends. Please report this: " + throwable.getMessage(), NamedTextColor.RED));
							MessagingUtils.sendStackTrace(sender, throwable);
							return;
						}

						new FriendListGui(sender, senderName, senderUuid, senderSocialDisplayInfo).open();
					}, runnable -> Bukkit.getScheduler().runTask(Plugin.getInstance(), runnable));
				})
			)

			/* /friend list_raw [page] */
			.withSubcommand(new CommandAPICommand(SUBCOMMAND_LIST_RAW)
				.withOptionalArguments(new IntegerArgument("page", 1))
				.executesPlayer((sender, args) -> {
					listRaw(sender, sender.getName(), sender.getUniqueId(), (int) args.getOrDefault("page", 1));
				})
			)
			//endregion

			//region <MODERATOR COMMANDS>
			/* Moderator commands:
			- /friend add_other <player one> <player two>
			- /friend remove_other <player one> <player two>
			- /friend list_other <player>
			- /friend list_raw_other <player> [page]
			- /friend check <player one> <player two>
			 */

			/* /friend add_other <player one> <player two> */
			.withSubcommand(new CommandAPICommand(SUBCOMMAND_ADD_OTHER)
				.withPermission(PERMISSION_ADD_OTHER)
				// Suggest names of every player to have joined the server for both arguments
				.withArguments(
					new StringArgument("player one").replaceSuggestions(ALL_CACHED_PLAYER_NAMES_SUGGESTIONS),
					new StringArgument("player two").replaceSuggestions(ALL_CACHED_PLAYER_NAMES_SUGGESTIONS)
				)
				.executesPlayer((moderator, args) -> {
					SocialManager.addFriend(moderator, StringUtils.getUuidFromInput((String) args.get("player one")), StringUtils.getUuidFromInput((String) args.get("player two")));
				})
			)

			/* /friend remove_other <player one> <player two> */
			.withSubcommand(new CommandAPICommand(SUBCOMMAND_REMOVE_OTHER)
				.withPermission(PERMISSION_REMOVE_OTHER)
				.withArguments(
					// Suggest names of every player to have joined the server for the first argument
					new StringArgument("player one")
						.replaceSuggestions(ALL_CACHED_PLAYER_NAMES_SUGGESTIONS),
					// Suggest added friends of previous name argument for the second argument
					new StringArgument("player two")
						.replaceSuggestions(ArgumentSuggestions.stringsAsync((info) -> {
							String playerOneName = (String) info.previousArgs().get("player one");
							UUID playerOneUuid;

							try {
								playerOneUuid = StringUtils.getUuidFromInput(playerOneName);
							} catch (WrapperCommandSyntaxException exception) {
								// Return no suggestions if previous name argument is invalid
								return CompletableFuture.completedFuture(new String[0]);
							}

							// Load player one's social cache from memory or from Redis
							return SocialManager.loadSocialCaches(playerOneUuid).thenApply(socialCaches -> {
								if (socialCaches == null) {
									return new String[0];
								}

								PlayerSocialCache playerOneCache = socialCaches.get(playerOneUuid);

								// This shouldn't occur if loadSocialCaches didn't return null
								if (playerOneCache == null) {
									return new String[0];
								}

								return playerOneCache.getFriends().stream()
									.map(SocialManager::resolveName)
									.filter(Objects::nonNull)
									.toArray(String[]::new);
							});
						}))
				)
				.executesPlayer((moderator, args) -> {
					UUID playerOneUuid = StringUtils.getUuidFromInput((String) args.get("player one"));
					UUID playerTwoUuid = StringUtils.getUuidFromInput((String) args.get("player two"));

					// Swap UUIDs if a moderator runs `/friend remove_other <friend> <moderator>`
					if (moderator.getUniqueId().equals(playerTwoUuid)) {
						UUID temp = playerOneUuid;
						playerOneUuid = playerTwoUuid;
						playerTwoUuid = temp;
					}

					SocialManager.removeFriend(moderator, playerOneUuid, playerTwoUuid);
				})
			)

			/* /friend list_other <player> */
			.withSubcommand(new CommandAPICommand(SUBCOMMAND_LIST_OTHER)
				.withPermission(PERMISSION_LIST_OTHER)
				// Suggest names of every player to have joined the server
				.withArguments(new StringArgument("player").replaceSuggestions(ALL_CACHED_PLAYER_NAMES_SUGGESTIONS))
				.executesPlayer((moderator, args) -> {
					String playerName = (String) args.get("player");
					UUID playerUuid = StringUtils.getUuidFromInput(playerName);

					SocialManager.getSocialDisplayInfo(playerUuid).whenCompleteAsync((playerSocialDisplayInfo, throwable) -> {
						if (throwable != null) {
							MMLog.severe("Caught exception trying to list friends for player " + playerName + " : " + throwable.getMessage());
							moderator.sendMessage(Component.text("An error occurred while trying to list friends. Please report this: " + throwable.getMessage(), NamedTextColor.RED));
							MessagingUtils.sendStackTrace(moderator, throwable);
							return;
						}

						new FriendListGui(moderator, playerName, playerUuid, playerSocialDisplayInfo).open();
					}, runnable -> Bukkit.getScheduler().runTask(Plugin.getInstance(), runnable));
				})
			)

			/* /friend list_raw_other <player> [page] */
			.withSubcommand(new CommandAPICommand(SUBCOMMAND_LIST_RAW_OTHER)
				.withPermission(PERMISSION_LIST_OTHER)
				// Suggest names of every player to have joined the server
				.withArguments(new StringArgument("player").replaceSuggestions(ALL_CACHED_PLAYER_NAMES_SUGGESTIONS))
				.withOptionalArguments(new IntegerArgument("page", 1))
				.executesPlayer((moderator, args) -> {
					String playerName = (String) args.get("player");
					UUID playerUuid = StringUtils.getUuidFromInput(playerName);

					listRaw(moderator, playerName, playerUuid, (int) args.getOrDefault("page", 1));
				})
			)

			/* /friend check <player one> <player two> */
			.withSubcommand(new CommandAPICommand(SUBCOMMAND_CHECK)
				.withPermission(PERMISSION_CHECK)
				// Suggest names of every player to have joined the server for both arguments
				.withArguments(
					new StringArgument("player one").replaceSuggestions(ALL_CACHED_PLAYER_NAMES_SUGGESTIONS),
					new StringArgument("player two").replaceSuggestions(ALL_CACHED_PLAYER_NAMES_SUGGESTIONS)
				)
				.executesPlayer((moderator, args) -> {
					String playerOneName = (String) args.get("player one");
					String playerTwoName = (String) args.get("player two");
					UUID playerOneUuid = StringUtils.getUuidFromInput(playerOneName);
					UUID playerTwoUuid = StringUtils.getUuidFromInput(playerTwoName);

					if (playerOneUuid.equals(playerTwoUuid)) {
						moderator.sendMessage(SocialManager.appendPrefix(Component.text("A player cannot be friends with themselves!", NamedTextColor.RED)));
						return;
					}

					// Load player one's social cache from memory or from Redis
					SocialManager.loadSocialCaches(playerOneUuid).thenAccept(socialCaches -> {
						if (socialCaches == null) {
							moderator.sendMessage(SocialManager.appendPrefix(Component.text("An error occurred while trying to load player data. Please report this to server operators if this keeps happening.", NamedTextColor.RED)));
							MMLog.severe(SocialManager.LOG_PREFIX + "Failed to load social cache for " + playerOneUuid + ". Is Redis down?");
							return;
						}

						PlayerSocialCache playerOneCache = socialCaches.get(playerOneUuid);

						// This shouldn't occur if loadSocialCaches didn't return null
						if (playerOneCache == null) {
							moderator.sendMessage(SocialManager.appendPrefix(Component.text("An error occurred while trying to load player data. Please report this to server operators if this keeps happening.", NamedTextColor.RED)));
							MMLog.severe(SocialManager.LOG_PREFIX + "Failed to load social cache for " + playerOneUuid + ". Is Redis down?");
							return;
						}

						if (playerOneCache.isFriendsWith(playerTwoUuid)) {
							moderator.sendMessage(SocialManager.appendPrefix(Component.text(playerOneName + " and " + playerTwoName + " are friends.", NamedTextColor.GREEN)));
						} else {
							moderator.sendMessage(SocialManager.appendPrefix(Component.text(playerOneName + " and " + playerTwoName + " are not friends.", NamedTextColor.RED)));
						}
					});
				})
			)
			//endregion

			.register();
	}

	private static void listRaw(Player commandSender, String playerName, UUID playerUuid, int requestedPage) {
		final boolean isSelf = commandSender.getUniqueId().equals(playerUuid);
		final boolean commandSenderCanSeeVanished = commandSender.hasPermission("group.devops");

		// Load the sender's social cache from memory or from Redis
		SocialManager.loadSocialCaches(playerUuid).thenAccept(socialCaches -> {
			if (socialCaches == null) {
				commandSender.sendMessage(SocialManager.appendPrefix(Component.text("An error occurred while trying to load player data. Please report this to server operators if this keep happening.", NamedTextColor.RED)));
				MMLog.severe(SocialManager.LOG_PREFIX + "Failed to load social cache for " + playerUuid + ". Is Redis down?");
				return;
			}

			// Fetch the sender's social cache
			PlayerSocialCache playerCache = socialCaches.get(playerUuid);

			// This shouldn't occur if loadSocialCaches didn't return null
			if (playerCache == null) {
				commandSender.sendMessage(SocialManager.appendPrefix(Component.text("An error occurred while trying to load player data. Please report this to server operators if this keep happening.", NamedTextColor.RED)));
				MMLog.severe(SocialManager.LOG_PREFIX + "Failed to load social cache for " + playerUuid + ". Is Redis down?");
				return;
			}

			Set<UUID> friendSet = playerCache.getFriends();
			if (friendSet.isEmpty()) {
				commandSender.sendMessage(SocialManager.appendPrefix(Component.text((isSelf ? "You have " : SocialManager.resolveName(playerUuid) + " has ") + "no friends added.", NamedTextColor.RED)));
				return;
			}

			List<UUID> friends = new ArrayList<>(friendSet);
			List<UUID> onlineFriends = new ArrayList<>();
			List<UUID> offlineFriends = new ArrayList<>();
			Map<UUID, String> friendNames = new HashMap<>();

			// Parse each friend to be sorted to display as either online or offline
			for (UUID friendUuid : friends) {
				String friendName = SocialManager.resolveName(friendUuid);
				friendNames.put(friendUuid, friendName);

				TABIntegration.MonumentaPlayer monuPlayer = TABIntegration.mPlayers.get(friendUuid);
				boolean isOnline = monuPlayer != null && !monuPlayer.getShardId().isEmpty();
				boolean isVanished = monuPlayer != null && monuPlayer.mIsHidden;

				// Show a vanished friend as online only if the command sender has permission to see vanished players
				if (isOnline && (!isVanished || commandSenderCanSeeVanished)) {
					onlineFriends.add(friendUuid);
				} else {
					offlineFriends.add(friendUuid);
				}
			}

			// Sort online and offline friends by alphabetical order
			onlineFriends.sort(Comparator.comparing(friendNames::get, String.CASE_INSENSITIVE_ORDER));
			offlineFriends.sort(Comparator.comparing(friendNames::get, String.CASE_INSENSITIVE_ORDER));

			// Display online friends first before offline friends
			List<UUID> sortedFriends = new ArrayList<>();
			sortedFriends.addAll(onlineFriends);
			sortedFriends.addAll(offlineFriends);

			final int pageSize = 8; // Amount of entries per page
			final int totalPages = (int) Math.ceil((double) sortedFriends.size() / pageSize);
			final int currentPage = Math.min(requestedPage, totalPages);
			final int startIndex = (currentPage - 1) * pageSize;
			final int endIndex = Math.min(startIndex + pageSize, sortedFriends.size());

			Component backButton = Component.text("<<", NamedTextColor.YELLOW, TextDecoration.BOLD)
				.hoverEvent(HoverEvent.showText(Component.text("Click to go to page " + (currentPage - 1) + ".", NamedTextColor.YELLOW)))
				.clickEvent(ClickEvent.runCommand("/friend " + (isSelf ? "list_raw " + (currentPage - 1) : "list_raw_other " + playerName + " " + (currentPage - 1))));
			Component forwardButton = Component.text(">>", NamedTextColor.YELLOW, TextDecoration.BOLD)
				.hoverEvent(HoverEvent.showText(Component.text("Click to go to page " + (currentPage + 1) + ".", NamedTextColor.YELLOW)))
				.clickEvent(ClickEvent.runCommand("/friend " + (isSelf ? "list_raw " + (currentPage + 1) : "list_raw_other " + playerName + " " + (currentPage + 1))));
			Component title = Component.text("                       ")
				.append((currentPage > 1 ? backButton : Component.text("  ")))
				.append(Component.text(" Friends (Page " + currentPage + " of " + totalPages + ") ", NamedTextColor.GOLD))
				.append((currentPage < totalPages ? forwardButton : Component.empty()));

			List<Component> friendComponents = new ArrayList<>();
			for (int i = startIndex; i < endIndex; i++) {
				UUID friendUuid = sortedFriends.get(i);
				String friendName = friendNames.get(friendUuid);
				String shardInfo = " (offline)";
				TextColor shardColor = NamedTextColor.RED;

				TABIntegration.MonumentaPlayer monuPlayer = TABIntegration.mPlayers.get(friendUuid);
				if (monuPlayer != null && !monuPlayer.getShardId().isEmpty()) {
					// Show a vanished friend's shard only if the command sender can see vanished players
					if (monuPlayer.mIsHidden && commandSenderCanSeeVanished) {
						shardInfo = " (vanished: " + monuPlayer.getShardId() + ")";
						shardColor = NamedTextColor.GRAY;
					} else if (!monuPlayer.mIsHidden) {
						shardInfo = " (" + monuPlayer.getShardId() + ")";
						shardColor = NamedTextColor.GREEN;
					}
				}

				Component friendComponent = Component.empty()
					.append(Component.text("> ", NamedTextColor.GOLD, TextDecoration.BOLD))
					.append(Component.text(friendName, NamedTextColor.WHITE))
					.append(Component.text(shardInfo, shardColor));

				friendComponents.add(friendComponent);
			}

			Component friendList = Component.join(JoinConfiguration.separator(Component.newline()), friendComponents);
			Component finalComponent = title.append(Component.newline()).append(friendList);
			commandSender.sendMessage(SocialManager.appendHeaderAndFooter(finalComponent));
		});
	}
}
