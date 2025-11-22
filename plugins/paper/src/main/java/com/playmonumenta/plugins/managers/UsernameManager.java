package com.playmonumenta.plugins.managers;

import com.google.gson.JsonObject;
import com.playmonumenta.networkrelay.NetworkRelayAPI;
import com.playmonumenta.networkrelay.NetworkRelayMessageEvent;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.MonumentaNetworkRelayIntegration;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.redissync.RedisAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.StringArgument;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.jetbrains.annotations.NotNull;

import static com.playmonumenta.plugins.integrations.MonumentaRedisSyncIntegration.ALL_CACHED_PLAYER_NAMES_SUGGESTIONS;

public class UsernameManager implements Listener {
	//region <DECLARATIONS>
	private static final String AUDIT_LOG_PREFIX = "[Username Manager] ";

	private static final String BADNAMES_REDIS_KEY = "badnames";
	private static final String ADD_BADNAME_CHANNEL = "addBadnameChannel";
	private static final String REMOVE_BADNAME_CHANNEL = "removeBadnameChannel";

	private static final String COMMAND = "badname";
	private static final String SUBCOMMAND_ADD = "add";
	private static final String SUBCOMMAND_REMOVE = "remove";
	private static final String SUBCOMMAND_CHECK = "check";
	private static final String SUBCOMMAND_LIST = "list";
	private static final String PERMISSION = "monumenta.commands.badname";

	private static final TreeSet<String> mBlacklistedUsernames = new TreeSet<>();
	//endregion

	//region <HELPER FUNCTIONS>
	private static Component appendPrefix(Component message) {
		Component prefix = Component.empty().append(Component.text("Usernames > ", NamedTextColor.GOLD));

		return prefix.append(message);
	}

	private static Component insertKickMessage(String username) {
		Component title = Component.empty().append(Component.text("You are currently blocked from joining this server!", NamedTextColor.RED));
		Component reason = Component.empty().append(Component.text("Reason: Your username, " + username + ", is breaking our rules and is not allowed on the server.", NamedTextColor.WHITE));
		Component infoOne = Component.empty().append(Component.text("Please change your Minecraft username before trying to join again.", NamedTextColor.RED));
		Component infoTwo = Component.empty().append(Component.text("You may contact the moderation team via ModMail at ", NamedTextColor.RED));
		Component discord = Component.empty().append(Component.text("https://discord.gg/monumenta", NamedTextColor.AQUA));
		Component infoThree = Component.empty().append(Component.text(" if you believe your username has been incorrectly blocked.", NamedTextColor.RED));

		return
			title.append(Component.newline()).append(Component.newline())
				.append(reason).append(Component.newline()).append(Component.newline())
				.append(infoOne).append(Component.newline())
				.append(infoTwo).append(discord).append(infoThree);
	}

	// The following method to check for valid usernames is adapted from
	// net.minecraft.world.entity.player.Player.isValidUsername
	private static boolean isValidUsername(String name) {
		if (name == null || name.isEmpty() || name.length() > 16) {
			return false;
		}

		for (int i = 0, len = name.length(); i < len; ++i) {
			char c = name.charAt(i);

			if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || (c == '_' || c == '.')) {
				continue;
			}

			return false;
		}

		return true;
	}
	//endregion

	//region <USERNAME MANAGEMENT>
	public UsernameManager() {
		// Load blacklisted usernames from Redis into the shard's local TreeSet
		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			TreeSet<String> tempSet = new TreeSet<>(RedisAPI.getInstance().async().smembers(BADNAMES_REDIS_KEY).toCompletableFuture().join());
			Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> mBlacklistedUsernames.addAll(tempSet));
		});
	}

	private static void addUsernameToBlacklist(String username) {
		// Add the blacklisted username to Redis
		RedisAPI.getInstance().async().sadd(BADNAMES_REDIS_KEY, username.toLowerCase(Locale.ROOT));

		// Notify other shards about the added blacklisted username via RabbitMQ
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("username", username);
		try {
			NetworkRelayAPI.sendBroadcastMessage(ADD_BADNAME_CHANNEL, jsonObject);
		} catch (Exception exception) {
			MMLog.warning("Failed to notify other shards of an added bad name through RabbitMQ; this is non-critical, but RabbitMQ needs fixing.");
		}
	}

	private static void removeUsernameFromBlacklist(String username) {
		// Remove the blacklisted username from Redis
		RedisAPI.getInstance().async().srem(BADNAMES_REDIS_KEY, username.toLowerCase(Locale.ROOT));

		// Notify other shards about the removed blacklisted username via RabbitMQ
		JsonObject jsonObject = new JsonObject();
		jsonObject.addProperty("username", username);
		try {
			NetworkRelayAPI.sendBroadcastMessage(REMOVE_BADNAME_CHANNEL, jsonObject);
		} catch (Exception exception) {
			MMLog.warning("Failed to notify other shards of a removed bad name through RabbitMQ; this is non-critical, but RabbitMQ needs fixing.");
		}
	}

	private static boolean isBlacklistedUsername(String username) {
		return mBlacklistedUsernames.contains(username.toLowerCase(Locale.ROOT));
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	public void onPlayerLogin(PlayerLoginEvent event) {
		// Check the connecting player's username to see if it's blacklisted
		String username = event.getPlayer().getName();

		if (isBlacklistedUsername(username)) {
			event.disallow(PlayerLoginEvent.Result.KICK_OTHER, insertKickMessage(username));
		}
	}
	//endregion

	//region <RABBITMQ>
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void networkRelayMessageEvent(@NotNull NetworkRelayMessageEvent event) {
		JsonObject data = event.getData();
		switch (event.getChannel()) {
			case ADD_BADNAME_CHANNEL -> {
				if (data.has("username")) {
					String username = data.get("username").getAsString();

					// Add the blacklisted username to the shard's local TreeSet
					mBlacklistedUsernames.add(username.toLowerCase(Locale.ROOT));

					// Kick the player with the corresponding username if they're online
					for (Player player : Bukkit.getOnlinePlayers()) {
						if (player.getName().toLowerCase(Locale.ROOT).equals(username.toLowerCase(Locale.ROOT))) {
							player.kick(insertKickMessage(player.getName()));
						}
					}
				}
			}

			case REMOVE_BADNAME_CHANNEL -> {
				if (data.has("username")) {
					String username = data.get("username").getAsString();

					// Remove the blacklisted username from the shard's local TreeSet
					mBlacklistedUsernames.remove(username.toLowerCase(Locale.ROOT));
				}
			}

			default -> {
				// Do nothing
			}
		}
	}
	//endregion

	//region <COMMANDS>
	public static void register() {
		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)

			/* /badname add <username> */
			.withSubcommand(new CommandAPICommand(SUBCOMMAND_ADD)
				// Suggest names of every player to have joined the server
				.withArguments(new StringArgument("username").replaceSuggestions(ALL_CACHED_PLAYER_NAMES_SUGGESTIONS))
				.executesPlayer((moderator, args) -> {
					String username = (String) args.get("username");

					if (!isValidUsername(username)) {
						moderator.sendMessage(appendPrefix(Component.text(username + " is not a valid Minecraft username. Please check your spelling and try again.", NamedTextColor.RED)));
						return;
					}

					if (isBlacklistedUsername(username)) {
						moderator.sendMessage(appendPrefix(Component.text(username + " is already blacklisted.", NamedTextColor.RED)));
						return;
					}

					addUsernameToBlacklist(username);
					moderator.sendMessage(appendPrefix(Component.text(username + " has been blacklisted.", NamedTextColor.GREEN).append(Component.newline())
						.append(Component.text("Please be aware that blacklisted usernames are NOT case-sensitive!", NamedTextColor.GREEN, TextDecoration.ITALIC)).append(Component.newline())
						.append(Component.text("This means " + username + " and " + username.toLowerCase(Locale.ROOT) + " will be unable to join the server!", NamedTextColor.GREEN, TextDecoration.ITALIC))
					));
					MonumentaNetworkRelayIntegration.sendAuditLogSevereMessage(AUDIT_LOG_PREFIX + moderator.getName() + " marked " + username + " as a bad name!");
				})
			)

			/* /badname remove <username> */
			.withSubcommand(new CommandAPICommand(SUBCOMMAND_REMOVE)
				// Suggest all blacklisted usernames
				.withArguments(new StringArgument("username")
					.replaceSuggestions(ArgumentSuggestions.strings((info) ->
						mBlacklistedUsernames.toArray(new String[0])))
				)
				.executesPlayer((moderator, args) -> {
					String username = (String) args.get("username");

					if (!isBlacklistedUsername(username)) {
						moderator.sendMessage(appendPrefix(Component.text(username + " is currently not blacklisted.", NamedTextColor.RED)));
						return;
					}

					removeUsernameFromBlacklist(username);
					moderator.sendMessage(appendPrefix(Component.text(username + " has been removed from the blacklist.", NamedTextColor.GREEN)));
					MonumentaNetworkRelayIntegration.sendAuditLogSevereMessage(AUDIT_LOG_PREFIX + moderator.getName() + " unmarked " + username + " as a bad name!");
				})
			)

			/* /badname check <username> */
			.withSubcommand(new CommandAPICommand(SUBCOMMAND_CHECK)
				// Suggest names of every player to have joined the server
				.withArguments(new StringArgument("username").replaceSuggestions(ALL_CACHED_PLAYER_NAMES_SUGGESTIONS))
				.executesPlayer((moderator, args) -> {
					String username = (String) args.get("username");

					if (isBlacklistedUsername(username)) {
						moderator.sendMessage(appendPrefix(Component.text(username + " is currently blacklisted.", NamedTextColor.GREEN)));
					} else {
						moderator.sendMessage(appendPrefix(Component.text(username + " is currently not blacklisted.", NamedTextColor.RED)));
					}
				})
			)

			/* /badname list */
			.withSubcommand(new CommandAPICommand(SUBCOMMAND_LIST)
				.executesPlayer((moderator, args) -> {
					ItemStack bookOfBadnames = new ItemStack(Material.WRITTEN_BOOK);
					BookMeta bookMeta = (BookMeta) bookOfBadnames.getItemMeta();
					List<Component> pages = new ArrayList<>();
					StringBuilder pageBuilder = new StringBuilder();
					int lineCounter = 0;

					for (String username : mBlacklistedUsernames) {
						if (lineCounter == 14) {
							pages.add(Component.text(pageBuilder.toString()));
							pageBuilder = new StringBuilder();
							lineCounter = 0;
						}

						pageBuilder.append(username).append("\n");
						lineCounter++;
					}

					if (!pageBuilder.isEmpty()) {
						pages.add(Component.text(pageBuilder.toString()));
					}

					bookMeta.setTitle("Bad Names");
					bookMeta.setAuthor("Moderators");
					bookMeta.addPages(pages.toArray(new Component[0]));
					bookOfBadnames.setItemMeta(bookMeta);

					moderator.openBook(bookOfBadnames);
				})
			)

			.register();
	}
	//endregion
}
