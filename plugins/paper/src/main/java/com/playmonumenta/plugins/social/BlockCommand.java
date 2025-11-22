package com.playmonumenta.plugins.social;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import static com.playmonumenta.plugins.integrations.MonumentaRedisSyncIntegration.ALL_CACHED_PLAYER_NAMES_SUGGESTIONS;
import static com.playmonumenta.plugins.integrations.MonumentaRedisSyncIntegration.ALL_OTHER_CACHED_PLAYER_NAMES_SUGGESTIONS;

public class BlockCommand {
	private static final String COMMAND = "block";
	private static final String SUBCOMMAND_ADD = "add";
	private static final String SUBCOMMAND_REMOVE = "remove";
	private static final String SUBCOMMAND_LIST = "list";
	private static final String SUBCOMMAND_LIST_RAW = "list_raw";
	private static final String SUBCOMMAND_LIST_OTHER = "list_other";
	private static final String SUBCOMMAND_LIST_RAW_OTHER = "list_raw_other";
	private static final String SUBCOMMAND_CHECK = "check";
	private static final String PERMISSION_LIST_OTHER = "monumenta.social.block.listother";
	private static final String PERMISSION_CHECK = "monumenta.social.block.check";

	public static void register() {
		new CommandAPICommand(COMMAND)

			//region <PUBLIC COMMANDS>
			/* Public commands:
			- /block add <player>
			- /block remove <player>
			- /block list
			- /block list_raw [page]
			 */

			/* /block add <player> */
			.withSubcommand(new CommandAPICommand(SUBCOMMAND_ADD)
				// Suggest all online players except for the command sender and those who are vanished
				.withArguments(new StringArgument("player").replaceSuggestions(ALL_OTHER_CACHED_PLAYER_NAMES_SUGGESTIONS))
				.executesPlayer((sender, args) -> {
					SocialManager.blockPlayer(sender.getUniqueId(), StringUtils.getUuidFromInput((String) args.get("player")));
				})
			)

			/* /block remove <player> */
			.withSubcommand(new CommandAPICommand(SUBCOMMAND_REMOVE)
				// Suggest all blocked players
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

							return senderCache.getBlockedPlayers().stream()
								.map(SocialManager::resolveName)
								.filter(Objects::nonNull)
								.toArray(String[]::new);
						});
					}))
				)
				.executesPlayer((sender, args) -> {
					SocialManager.unblockPlayer(sender.getUniqueId(), StringUtils.getUuidFromInput((String) args.get("player")));
				})
			)

			/* /block list */
			.withSubcommand(new CommandAPICommand(SUBCOMMAND_LIST)
				.executesPlayer((sender, args) -> {
					String senderName = sender.getName();
					UUID senderUuid = sender.getUniqueId();

					SocialManager.getSocialDisplayInfo(senderUuid).whenCompleteAsync((senderSocialDisplayInfo, throwable) -> {
						if (throwable != null) {
							MMLog.severe("Caught exception trying to list blocked players for player " + senderName + " : " + throwable.getMessage());
							sender.sendMessage(Component.text("An error occurred while trying to list blocked players. Please report this: " + throwable.getMessage(), NamedTextColor.RED));
							MessagingUtils.sendStackTrace(sender, throwable);
							return;
						}

						new BlockListGui(sender, senderName, senderUuid, senderSocialDisplayInfo).open();
					}, runnable -> Bukkit.getScheduler().runTask(Plugin.getInstance(), runnable));
				})
			)

			/* /block list_raw [page] */
			.withSubcommand(new CommandAPICommand(SUBCOMMAND_LIST_RAW)
				.withOptionalArguments(new IntegerArgument("page", 1))
				.executesPlayer((sender, args) -> {
					listRaw(sender, sender.getName(), sender.getUniqueId(), (int) args.getOrDefault("page", 1));
				})
			)
			//endregion

			//region <MODERATOR COMMANDS>
			/* Moderator commands:
			- /block list_other <player>
			- /block list_raw_other <player> [page]
			- /block check <player one> <player two>
			 */

			/* /block list_other <player> */
			.withSubcommand(new CommandAPICommand(SUBCOMMAND_LIST_OTHER)
				.withPermission(PERMISSION_LIST_OTHER)
				// Suggest names of every player to have joined the server
				.withArguments(new StringArgument("player").replaceSuggestions(ALL_CACHED_PLAYER_NAMES_SUGGESTIONS))
				.executesPlayer((moderator, args) -> {
					String playerName = (String) args.get("player");
					UUID playerUuid = StringUtils.getUuidFromInput(playerName);

					SocialManager.getSocialDisplayInfo(playerUuid).whenCompleteAsync((playerSocialDisplayInfo, throwable) -> {
						if (throwable != null) {
							MMLog.severe("Caught exception trying to list blocked players for player " + playerName + " : " + throwable.getMessage());
							moderator.sendMessage(Component.text("An error occurred while trying to list blocked players. Please report this: " + throwable.getMessage(), NamedTextColor.RED));
							MessagingUtils.sendStackTrace(moderator, throwable);
							return;
						}

						new BlockListGui(moderator, playerName, playerUuid, playerSocialDisplayInfo).open();
					}, runnable -> Bukkit.getScheduler().runTask(Plugin.getInstance(), runnable));
				})
			)

			/* /block list_raw_other <player> [page] */
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

			/* /block check <player one> <player two> */
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
						moderator.sendMessage(SocialManager.appendPrefix(Component.text("A player cannot block themselves!", NamedTextColor.RED)));
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

						if (playerOneCache.hasBlocked(playerTwoUuid)) {
							moderator.sendMessage(SocialManager.appendPrefix(Component.text(playerOneName + " has " + playerTwoName + " blocked.", NamedTextColor.GREEN)));
						} else {
							moderator.sendMessage(SocialManager.appendPrefix(Component.text(playerOneName + " does not have " + playerTwoName + " blocked.", NamedTextColor.RED)));
						}
					});
				})
			)
			//endregion

			.register();
	}

	private static void listRaw(Player commandSender, String playerName, UUID playerUuid, int requestedPage) {
		final boolean isSelf = commandSender.getUniqueId().equals(playerUuid);

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

			Set<UUID> blockedSet = playerCache.getBlockedPlayers();
			if (blockedSet.isEmpty()) {
				commandSender.sendMessage(SocialManager.appendPrefix(Component.text((isSelf ? "You have " : SocialManager.resolveName(playerUuid) + " has ") + "no one blocked.", NamedTextColor.RED)));
				return;
			}

			// Sort blocked players by alphabetical order
			List<String> blockedNames = blockedSet.stream()
				.map(SocialManager::resolveName)
				.sorted(String.CASE_INSENSITIVE_ORDER)
				.toList();

			final int pageSize = 8; // Amount of entries per page
			final int totalPages = (int) Math.ceil((double) blockedNames.size() / pageSize);
			final int currentPage = Math.min(requestedPage, totalPages);
			final int startIndex = (currentPage - 1) * pageSize;
			final int endIndex = Math.min(startIndex + pageSize, blockedNames.size());

			Component backButton = Component.text("<<", NamedTextColor.YELLOW, TextDecoration.BOLD)
				.hoverEvent(HoverEvent.showText(Component.text("Click to go page " + (currentPage - 1) + ".", NamedTextColor.YELLOW)))
				.clickEvent(ClickEvent.runCommand("/block " + (isSelf ? "list_raw " + (currentPage - 1) : "list_raw_other " + playerName + " " + (currentPage - 1))));
			Component forwardButton = Component.text(">>", NamedTextColor.YELLOW, TextDecoration.BOLD)
				.hoverEvent(HoverEvent.showText(Component.text("Click to go to page " + (currentPage + 1) + ".", NamedTextColor.YELLOW)))
				.clickEvent(ClickEvent.runCommand("/block " + (isSelf ? "list_raw " + (currentPage + 1) : "list_raw_other " + playerName + " " + (currentPage + 1))));
			Component title = Component.text("                       ")
				.append((currentPage > 1 ? backButton : Component.text("  ")))
				.append(Component.text(" Blocked (Page " + currentPage + " of " + totalPages + ") ", NamedTextColor.GOLD))
				.append((currentPage < totalPages ? forwardButton : Component.empty()));

			List<Component> blockedComponents = new ArrayList<>();
			for (int i = startIndex; i < endIndex; i++) {
				Component blockedComponent = Component.empty()
					.append(Component.text("> ", NamedTextColor.GOLD, TextDecoration.BOLD))
					.append(Component.text(blockedNames.get(i), NamedTextColor.WHITE));

				blockedComponents.add(blockedComponent);
			}

			Component blockedList = Component.join(JoinConfiguration.separator(Component.newline()), blockedComponents);
			Component finalComponent = title.append(Component.newline()).append(blockedList);
			commandSender.sendMessage(SocialManager.appendHeaderAndFooter(finalComponent));
		});
	}
}
