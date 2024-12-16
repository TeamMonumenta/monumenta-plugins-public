package com.playmonumenta.plugins.managers;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.playmonumenta.networkchat.commands.ChatCommand;
import com.playmonumenta.networkrelay.NetworkRelayAPI;
import com.playmonumenta.networkrelay.NetworkRelayMessageEvent;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.MonumentaNetworkRelayIntegration;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.redissync.RedisAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
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

public class UsernameManager implements Listener {
	private final Plugin mPlugin;
	private static final TreeSet<String> mBlacklistedUsernames = new TreeSet<>();

	private static final String REDIS_KEY = "badnames";
	private static final String ADD_BADNAME_CHANNEL = "addBadnameChannel";
	private static final String REMOVE_BADNAME_CHANNEL = "removeBadnameChannel";

	private static final String COMMAND = "badname";
	private static final String SUBCOMMAND_ADD = "add";
	private static final String SUBCOMMAND_REMOVE = "remove";
	private static final String SUBCOMMAND_CHECK = "check";
	private static final String SUBCOMMAND_LIST = "list";
	private static final String PERMISSION = "monumenta.commands.badname";
	private static final Argument<?> USERNAME_ARGUMENT = new StringArgument("username").replaceSuggestions(ChatCommand.ALL_CACHED_PLAYER_NAMES_SUGGESTIONS);
	private static final Argument<?> BLACKLISTED_USERNAME_ARGUMENT = new StringArgument("username").replaceSuggestions(ArgumentSuggestions.strings((info) -> mBlacklistedUsernames.toArray(new String[0])));

	public UsernameManager(Plugin plugin) {
		mPlugin = plugin;
		loadBadNamesFromRedis();
	}

	private void loadBadNamesFromRedis() {
		Bukkit.getScheduler().runTaskAsynchronously(mPlugin, () -> {
			TreeSet<String> tempSet = new TreeSet<>(RedisAPI.getInstance().async().smembers(REDIS_KEY).toCompletableFuture().join());
			Bukkit.getScheduler().runTask(mPlugin, () -> {
				mBlacklistedUsernames.addAll(tempSet);
			});
		});
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

	private static void addUsernameToBlacklist(String username) {
		RedisAPI.getInstance().async().sadd(REDIS_KEY, username.toLowerCase(Locale.ROOT));

		JsonObject changeJson = new JsonObject();
		changeJson.addProperty("username", username);
		try {
			NetworkRelayAPI.sendBroadcastMessage(ADD_BADNAME_CHANNEL, changeJson);
		} catch (Exception e) {
			MMLog.warning("Failed to notify other shards of an added bad name through RabbitMQ; this is non-critical, but RabbitMQ needs fixing");
		}
	}

	private void rabbitmqAddUsernameToBlacklist(String username) {
		mBlacklistedUsernames.add(username.toLowerCase(Locale.ROOT));

		for (Player player : Bukkit.getOnlinePlayers()) {
			if (player.getName().toLowerCase(Locale.ROOT).equals(username.toLowerCase(Locale.ROOT))) {
				player.kick(insertKickMessage(player.getName()));
			}
		}
	}

	private static void removeUsernameFromBlacklist(String username) {
		RedisAPI.getInstance().async().srem(REDIS_KEY, username.toLowerCase(Locale.ROOT));

		JsonObject changeJson = new JsonObject();
		changeJson.addProperty("username", username);
		try {
			NetworkRelayAPI.sendBroadcastMessage(REMOVE_BADNAME_CHANNEL, changeJson);
		} catch (Exception e) {
			MMLog.warning("Failed to notify other shards of a removed bad name through RabbitMQ; this is non-critical, but RabbitMQ needs fixing");
		}
	}

	private void rabbitmqRemoveUsernameFromBlacklist(String username) {
		mBlacklistedUsernames.remove(username.toLowerCase(Locale.ROOT));
	}

	private static boolean isBlacklistedUsername(String username) {
		return mBlacklistedUsernames.contains(username.toLowerCase(Locale.ROOT));
	}

	public void handleAddBadName(JsonObject data) {
		if (data.has("username") && data.get("username") instanceof JsonPrimitive usernamePrimitive && usernamePrimitive.isString()) {
			String username = usernamePrimitive.getAsString();
			rabbitmqAddUsernameToBlacklist(username);
		}
	}

	public void handleRemoveBadName(JsonObject data) {
		if (data.has("username") && data.get("username") instanceof JsonPrimitive usernamePrimitive && usernamePrimitive.isString()) {
			String username = usernamePrimitive.getAsString();
			rabbitmqRemoveUsernameFromBlacklist(username);
		}
	}

	private Component insertKickMessage(String username) {
		return
			Component.text("You are currently blocked from joining this server!\n\n", NamedTextColor.RED)
			.append(Component.text("Reason: ", NamedTextColor.WHITE))
			.append(Component.text("Your username, " + username + ", is breaking our rules and is not allowed on the server.\n\n", NamedTextColor.WHITE))
			.append(Component.text("Please change your Minecraft username before trying to join again.\n", NamedTextColor.RED))
			.append(Component.text("If you believe your username has been incorrectly blocked, please contact the moderation team via ModMail at ", NamedTextColor.RED))
			.append(Component.text("https://discord.gg/monumenta", NamedTextColor.AQUA))
			.append(Component.text(".", NamedTextColor.RED));
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	public void onPlayerLogin(PlayerLoginEvent event) {
		if (isBlacklistedUsername(event.getPlayer().getName())) {
			event.disallow(PlayerLoginEvent.Result.KICK_OTHER, insertKickMessage(event.getPlayer().getName()));
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void networkRelayMessageEvent(@NotNull NetworkRelayMessageEvent event) {
		JsonObject data = event.getData();
		switch (event.getChannel()) {
			case ADD_BADNAME_CHANNEL -> handleAddBadName(data);
			case REMOVE_BADNAME_CHANNEL -> handleRemoveBadName(data);
			default -> {
			}
		}
	}

	public static void register() {
		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)

			.withSubcommand(new CommandAPICommand(SUBCOMMAND_ADD)
				.withArguments(USERNAME_ARGUMENT)
				.executesPlayer((player, args) -> {
					String username = (String) args.get("username");

					if (isValidUsername(username)) {
						if (!isBlacklistedUsername(username)) {
							addUsernameToBlacklist(username);
							player.sendMessage(Component.text(username + " has been blacklisted.\n", NamedTextColor.GREEN)
								.append(Component.text("Please be aware that blacklisted usernames are NOT case-sensitive!\n", NamedTextColor.GREEN, TextDecoration.ITALIC))
								.append(Component.text("This means " + username + " and " + username.toLowerCase(Locale.ROOT) + " will be unable to join the server!", NamedTextColor.GREEN, TextDecoration.ITALIC))
							);
							MonumentaNetworkRelayIntegration.sendAuditLogSevereMessage(player.getName() + " marked " + username + " as a bad name!");
						} else {
							player.sendMessage(Component.text(username + " is already blacklisted.", NamedTextColor.RED));
						}
					} else {
						player.sendMessage(Component.text(username + " is not a valid Minecraft username. Please check your spelling and try again.", NamedTextColor.RED));
					}
				})
			)

			.withSubcommand(new CommandAPICommand(SUBCOMMAND_REMOVE)
				.withArguments(BLACKLISTED_USERNAME_ARGUMENT)
				.executesPlayer((player, args) -> {
					String username = (String) args.get("username");

					if (isBlacklistedUsername(username)) {
						removeUsernameFromBlacklist(username);
						player.sendMessage(Component.text(username + " has been removed from the blacklist.", NamedTextColor.GREEN));
						MonumentaNetworkRelayIntegration.sendAuditLogSevereMessage(player.getName() + " unmarked " + username + " as a bad name!");
					} else {
						player.sendMessage(Component.text(username + " is currently not blacklisted.", NamedTextColor.RED));
					}
				})
			)

			.withSubcommand(new CommandAPICommand(SUBCOMMAND_CHECK)
				.withArguments(USERNAME_ARGUMENT)
				.executesPlayer((player, args) -> {
					String username = (String) args.get("username");

					if (isBlacklistedUsername(username)) {
						player.sendMessage(Component.text(username + " is currently blacklisted.", NamedTextColor.GREEN));
					} else {
						player.sendMessage(Component.text(username + " is currently not blacklisted.", NamedTextColor.RED));
					}
				})
			)

			.withSubcommand(new CommandAPICommand(SUBCOMMAND_LIST)
				.executesPlayer((player, args) -> {
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

					player.openBook(bookOfBadnames);
				})
			)

			.register();
	}
}
