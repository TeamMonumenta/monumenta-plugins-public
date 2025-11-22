package com.playmonumenta.plugins.plots;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.google.gson.JsonObject;
import com.playmonumenta.networkrelay.NetworkRelayAPI;
import com.playmonumenta.networkrelay.NetworkRelayMessageEvent;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.MonumentaRedisSyncIntegration;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.redissync.RemoteDataAPI;
import com.playmonumenta.worlds.paper.MonumentaWorldManagementAPI;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlotManager implements Listener {
	private static final String MYPLOTACCESS_KEY = "myplotaccess|";
	private static final String OTHERPLOTACCESS_KEY = "otherplotaccess|";

	private static final String PLOT_ACCESS_GRANTED_NOTIFICATION_CHANNEL = "plotAccessGrantedNotificationChannel";
	private static final String MODERATOR_FORCE_ADDED_PLOT_ACCESS_NOTIFICATION_CHANNEL = "moderatorForceAddedPlotAccessNotificationChannel";

	static Component appendPrefix(Component message) {
		Component prefix = Component.empty().append(Component.text("Plots > ", NamedTextColor.GOLD));

		return prefix.append(message);
	}

	private static void broadcastNotification(String channel, Map<String, String> jsonProperties, String logMessage) {
		JsonObject jsonObject = new JsonObject();

		for (Map.Entry<String, String> entry : jsonProperties.entrySet()) {
			jsonObject.addProperty(entry.getKey(), entry.getValue());
		}

		try {
			NetworkRelayAPI.sendBroadcastMessage(channel, jsonObject);
		} catch (Exception ex) {
			MMLog.warning(logMessage);
		}
	}

	private static boolean plotAccessIsExpired(long time) {
		return time >= 0 && Instant.now().getEpochSecond() > time;
	}

	static void plotAccessAdd(CommandSender sender, UUID ownerUUID, UUID otherUUID, @Nullable String duration) throws WrapperCommandSyntaxException {
		long expiration = -1;
		if (duration != null) {
			duration = duration.toUpperCase(Locale.getDefault());
			if (duration.contains("D")) {
				duration = "P" + (duration.endsWith("D") ? duration : duration.replaceAll("D", "DT"));
			} else {
				duration = "PT" + duration;
			}
			try {
				expiration = Duration.parse(duration).getSeconds();
				if (expiration > 365 * 24 * 60 * 60) {
					sender.sendMessage(appendPrefix(Component.text("The duration cannot exceed 1 year!", NamedTextColor.RED)));
					return;
				} else if (expiration < 60) {
					sender.sendMessage(appendPrefix(Component.text("The duration must be at least 1 minute!", NamedTextColor.RED)));
					return;
				}

				// Add the current time, so we store the time when it expires, not the delta
				expiration += Instant.now().getEpochSecond();
			} catch (Exception ex) {
				throw CommandAPI.failWithString("Caught exception: " + ex.getMessage());
			}
		}

		if (ownerUUID.equals(otherUUID)) {
			if (sender instanceof Player player && player.getUniqueId().equals(ownerUUID)) {
				sender.sendMessage(appendPrefix(Component.text("You cannot add yourself to your own plot!", NamedTextColor.RED)));
			} else {
				sender.sendMessage(appendPrefix(Component.text("You cannot grant a player access to their own plot!", NamedTextColor.RED)));
			}
			return;
		}

		Player ownerPlayer = Bukkit.getPlayer(ownerUUID);
		if (ownerPlayer != null) {
			int plot = ScoreboardUtils.getScoreboardValue(ownerPlayer, Constants.Objectives.OWN_PLOT).orElse(0);
			plotAccessAdd(sender, ownerUUID, otherUUID, plot, expiration);
		} else {
			long finalExpiration = expiration;
			MonumentaRedisSyncAPI.getPlayerScores(ownerUUID).thenAccept(scores -> {
				Integer plot = scores.get(Constants.Objectives.OWN_PLOT);
				plotAccessAdd(sender, ownerUUID, otherUUID, plot == null ? 0 : plot, finalExpiration);
			});
		}
	}

	private static void plotAccessAdd(CommandSender sender, UUID ownerUUID, UUID otherUUID, int plot, long expiration) {
		if (plot <= 0) {
			if (sender instanceof Player player && player.getUniqueId().equals(ownerUUID)) {
				sender.sendMessage(appendPrefix(Component.text("You do not have a plot to add someone to!", NamedTextColor.RED)));
			} else {
				sender.sendMessage(appendPrefix(Component.text(MonumentaRedisSyncIntegration.cachedUuidToName(ownerUUID) + " does not have a plot!", NamedTextColor.RED)));
			}
			return;
		}

		CompletableFuture<Boolean> futureOwn = RemoteDataAPI.set(ownerUUID, MYPLOTACCESS_KEY + otherUUID, Long.toString(expiration));
		CompletableFuture<Boolean> futureOther = RemoteDataAPI.set(otherUUID, OTHERPLOTACCESS_KEY + ownerUUID, plot + "," + expiration);
		futureOwn.thenAccept(successOwn -> futureOther.thenAccept(successOther -> {

			String ownerName = MonumentaRedisSyncIntegration.cachedUuidToName(ownerUUID);
			String otherName = MonumentaRedisSyncIntegration.cachedUuidToName(otherUUID);
			String plotName = sender instanceof Player player && player.getUniqueId().equals(ownerUUID) ? "your plot" : ownerName + "'s plot";

			if (otherName == null) {
				return;
			}

			if (successOwn) {
				sender.sendMessage(appendPrefix(Component.text("You have granted ", NamedTextColor.GREEN).append(Component.text(otherName, NamedTextColor.AQUA)).append(Component.text(" access to " + plotName + ".", NamedTextColor.GREEN))));
			} else {
				sender.sendMessage(appendPrefix(Component.text("You have updated the duration of ", NamedTextColor.GREEN).append(Component.text(otherName, NamedTextColor.AQUA)).append(Component.text("'s access to " + plotName + ".", NamedTextColor.GREEN))));
			}

			Player ownerPlayer = Bukkit.getPlayer(ownerUUID);
			if (ownerPlayer != sender) {
				Map<String, String> jsonProperties = Map.of(
					"ownerUUID", String.valueOf(ownerUUID),
					"otherName", otherName
				);
				broadcastNotification(MODERATOR_FORCE_ADDED_PLOT_ACCESS_NOTIFICATION_CHANNEL, jsonProperties, "Failed to notify other shards of a plot access granted by a moderator via RabbitMQ.");
			}
			if (successOther && ownerName != null) {
				Map<String, String> jsonProperties = Map.of(
					"otherUUID", String.valueOf(otherUUID),
					"ownerName", ownerName
				);
				broadcastNotification(PLOT_ACCESS_GRANTED_NOTIFICATION_CHANNEL, jsonProperties, "Failed to notify other shards of a granted plot access via RabbitMQ.");
			}
		}));
	}

	/* TODO: There needs to be some security mechanism that verifies players still have access to a plot if they last visited it but it expired */
	/* Maybe when player joins, fetch their access and see if it's currently expired? And boot them to their own plot if so?  */

	static void plotAccessRemove(Player owner, String removedName) throws WrapperCommandSyntaxException {
		UUID removedUUID = StringUtils.getUuidFromInput(removedName);

		plotAccessRemove(owner, owner.getUniqueId(), removedUUID);
	}

	static void plotAccessRemove(@Nullable CommandSender sender, UUID ownerUUID, UUID otherUUID) {
		CompletableFuture<Boolean> future1 = RemoteDataAPI.del(ownerUUID, MYPLOTACCESS_KEY + otherUUID);
		CompletableFuture<Boolean> future2 = RemoteDataAPI.del(otherUUID, OTHERPLOTACCESS_KEY + ownerUUID);
		if (sender != null) {
			future1.thenAccept(success1 -> future2.thenAccept(success2 -> {
				String otherName = MonumentaRedisSyncIntegration.cachedUuidToName(otherUUID);
				if (otherName == null) {
					otherName = otherUUID.toString();
				}
				String ownerName = MonumentaRedisSyncIntegration.cachedUuidToName(ownerUUID);
				if (ownerName == null) {
					ownerName = ownerUUID.toString();
				}
				String plotName = sender instanceof Player player && player.getUniqueId().equals(ownerUUID) ? "your plot" : ownerName + "'s plot";
				if (success1 && success2) {
					sender.sendMessage(appendPrefix(Component.text(otherName + " can no longer access " + plotName + ".", NamedTextColor.GREEN)));
				} else {
					sender.sendMessage(appendPrefix(Component.text(otherName + " does not have access to " + plotName + "!", NamedTextColor.RED)));
				}
			}));
		}
	}

	public static void sendPlayerToPlot(Player player) {
		try {
			if (ServerProperties.getShardName().equals("playerplots")) {
				MonumentaWorldManagementAPI.sortWorld(player);
			} else {
				MonumentaRedisSyncAPI.sendPlayer(player, "playerplots");
			}
		} catch (Exception ex) {
			player.sendMessage(Component.text("An error occurred while sending you to playerplots. Please report this: " + ex.getMessage(), NamedTextColor.RED));
			MessagingUtils.sendStackTrace(player, ex);
		}
	}

	public static class PlotInfo {
		public static class OtherAccessToOwnerPlotRecord {
			final UUID mUUID;
			final long mExpiration;
			@Nullable PlayerProfile mProfile = null;
			@Nullable ItemStack mHead = null;
			@Nullable String mName = null;

			protected OtherAccessToOwnerPlotRecord(UUID uuid, long expiration) {
				mUUID = uuid;
				mExpiration = expiration;
			}
		}

		public static class OwnerAccessToOtherPlotsRecord {
			final int mPlotId;
			final long mExpiration;
			@Nullable PlayerProfile mProfile = null;
			@Nullable ItemStack mHead = null;
			@Nullable String mName = null;

			protected OwnerAccessToOtherPlotsRecord(int plotId, long expiration) {
				mPlotId = plotId;
				mExpiration = expiration;
			}
		}

		final UUID mUUID;

		// Plot ID this player owns
		final int mOwnedPlotId;
		// Plot ID this player currently has selected
		final int mCurrentPlotId;

		// Access other players have to owner plot, uuid | expiration
		final Map<UUID, OtherAccessToOwnerPlotRecord> mOtherAccessToOwnerPlot;
		// Access owner has to other plots, uuid | expiration
		final Map<UUID, OwnerAccessToOtherPlotsRecord> mOwnerAccessToOtherPlots;

		protected PlotInfo(UUID uuid, int ownedPlotId, int currentPlotId, Map<UUID, OtherAccessToOwnerPlotRecord> otherAccessToOwnerPlot, Map<UUID, OwnerAccessToOtherPlotsRecord> ownerAccessToOtherPlots) {
			mUUID = uuid;
			mOwnedPlotId = ownedPlotId;
			mCurrentPlotId = currentPlotId;
			mOtherAccessToOwnerPlot = otherAccessToOwnerPlot;
			mOwnerAccessToOtherPlots = ownerAccessToOtherPlots;
		}

		/* Call this to fetch the player's name and head */
		public CompletableFuture<PlotInfo> populateNamesAndHeads() {
			CompletableFuture<PlotInfo> future = new CompletableFuture<>();

			/* Populate all the names and (empty) profiles on the main thread */
			for (Map.Entry<UUID, OtherAccessToOwnerPlotRecord> entry : mOtherAccessToOwnerPlot.entrySet()) {
				UUID uuid = entry.getKey();
				OtherAccessToOwnerPlotRecord rec = entry.getValue();
				rec.mName = MonumentaRedisSyncIntegration.cachedUuidToName(uuid);
				rec.mProfile = Bukkit.getServer().createProfile(uuid, rec.mName);
			}

			for (Map.Entry<UUID, OwnerAccessToOtherPlotsRecord> entry : mOwnerAccessToOtherPlots.entrySet()) {
				UUID uuid = entry.getKey();
				OwnerAccessToOtherPlotsRecord rec = entry.getValue();
				rec.mName = MonumentaRedisSyncIntegration.cachedUuidToName(uuid);
				rec.mProfile = Bukkit.getServer().createProfile(uuid, rec.mName);
			}

			Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
				/* Complete all the profiles async */
				for (Map.Entry<UUID, OtherAccessToOwnerPlotRecord> entry : mOtherAccessToOwnerPlot.entrySet()) {
					PlayerProfile profile = entry.getValue().mProfile;

					if (profile != null) {
						profile.complete();
					}
				}

				for (Map.Entry<UUID, OwnerAccessToOtherPlotsRecord> entry : mOwnerAccessToOtherPlots.entrySet()) {
					PlayerProfile profile = entry.getValue().mProfile;

					if (profile != null) {
						profile.complete();
					}
				}

				Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
					/* Switch back to the main thread to finish assembling all the heads */
					for (Map.Entry<UUID, OtherAccessToOwnerPlotRecord> entry : mOtherAccessToOwnerPlot.entrySet()) {
						OtherAccessToOwnerPlotRecord rec = entry.getValue();

						ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
						SkullMeta meta = (SkullMeta) head.getItemMeta();
						meta.setPlayerProfile(rec.mProfile);
						meta.displayName(Component.text(rec.mName, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
						List<Component> lore = new ArrayList<>();
						lore.add(Component.text("Access expires in:", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
						String timeLeft = (rec.mExpiration == -1) ? "Unlimited" : MessagingUtils.getTimeDifferencePretty(rec.mExpiration);
						lore.add(Component.text(timeLeft, NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
						meta.lore(lore);
						meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
						head.setItemMeta(meta);

						rec.mHead = head;
					}

					for (Map.Entry<UUID, OwnerAccessToOtherPlotsRecord> entry : mOwnerAccessToOtherPlots.entrySet()) {
						OwnerAccessToOtherPlotsRecord rec = entry.getValue();

						ItemStack head = new ItemStack(Material.PLAYER_HEAD, 1);
						SkullMeta meta = (SkullMeta) head.getItemMeta();
						meta.setPlayerProfile(rec.mProfile);
						meta.displayName(Component.text(rec.mName + " (#" + rec.mPlotId + ")", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
						List<Component> lore = new ArrayList<>();
						lore.add(Component.text("Access expires in:", NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
						String timeLeft = (rec.mExpiration == -1) ? "Unlimited" : MessagingUtils.getTimeDifferencePretty(rec.mExpiration);
						lore.add(Component.text(timeLeft, NamedTextColor.LIGHT_PURPLE).decoration(TextDecoration.ITALIC, false));
						meta.lore(lore);
						meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
						head.setItemMeta(meta);

						rec.mHead = head;
					}

					/* Complete the future on the main thread */
					future.complete(this);
				});
			});

			return future;
		}
	}

	/* Should be run on main thread, does work async and completes on main thread */
	static CompletableFuture<PlotInfo> getPlotInfo(UUID ownerUUID) {
		CompletableFuture<PlotInfo> future = new CompletableFuture<>();

		final int plot;
		final int currentPlot;
		final CompletableFuture<Map<String, Integer>> scoresFuture;

		Player owner = Bukkit.getPlayer(ownerUUID);
		if (owner != null) {
			plot = ScoreboardUtils.getScoreboardValue(owner, Constants.Objectives.OWN_PLOT).orElse(0);
			currentPlot = ScoreboardUtils.getScoreboardValue(owner, Constants.Objectives.CURRENT_PLOT).orElse(0);
			scoresFuture = null;
		} else {
			// These two values will be overwritten by the scores future
			plot = 0;
			currentPlot = 0;
			scoresFuture = MonumentaRedisSyncAPI.getPlayerScores(ownerUUID);
		}

		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			try {
				Map<String, String> allRemoteData = RemoteDataAPI.getAll(ownerUUID).get();

				/* If the player wasn't online, fetching scores already started, need to complete that and get the specific interesting values */
				final int finalPlot;
				final int finalCurrentPlot;
				if (scoresFuture != null) {
					Map<String, Integer> scores = scoresFuture.get();
					finalPlot = scores.getOrDefault(Constants.Objectives.OWN_PLOT, 0);
					finalCurrentPlot = scores.getOrDefault(Constants.Objectives.CURRENT_PLOT, 0);
				} else {
					finalPlot = plot;
					finalCurrentPlot = currentPlot;
				}

				Map<UUID, PlotInfo.OtherAccessToOwnerPlotRecord> otherAccessToOwnerPlot = new HashMap<>();
				Map<UUID, PlotInfo.OwnerAccessToOtherPlotsRecord> ownerAccessToOtherPlots = new HashMap<>();

				/* Filter the entries into other access to plot, self access to other plots, and remove expired entries */
				allRemoteData.forEach((key, value) -> {
					if (key.startsWith(MYPLOTACCESS_KEY) || key.startsWith(OTHERPLOTACCESS_KEY)) {
						UUID otherUUID;
						long expiration;
						if (key.startsWith(MYPLOTACCESS_KEY)) {
							otherUUID = UUID.fromString(key.substring(MYPLOTACCESS_KEY.length()));
							expiration = Long.parseLong(value);
							if (plotAccessIsExpired(expiration)) {
								plotAccessRemove(null, ownerUUID, otherUUID);
							} else {
								otherAccessToOwnerPlot.put(otherUUID, new PlotInfo.OtherAccessToOwnerPlotRecord(otherUUID, expiration));
							}
						} else {
							otherUUID = UUID.fromString(key.substring(OTHERPLOTACCESS_KEY.length()));
							String[] split = value.split(",");
							int plotId = Integer.parseInt(split[0]);
							expiration = Long.parseLong(split[1]);
							if (plotAccessIsExpired(expiration)) {
								plotAccessRemove(null, otherUUID, ownerUUID);
							} else {
								ownerAccessToOtherPlots.put(otherUUID, new PlotInfo.OwnerAccessToOtherPlotsRecord(plotId, expiration));
							}
						}
					}
				});

				PlotInfo info = new PlotInfo(ownerUUID, finalPlot, finalCurrentPlot, otherAccessToOwnerPlot, ownerAccessToOtherPlots);

				/* Complete future on main thread for easy use of .whenCompleted() */
				Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> future.complete(info));
			} catch (Exception ex) {
				Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> future.completeExceptionally(ex));
			}
		});

		return future;
	}

	public static int getPlotRegion(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, "PlotRegion").orElse(0);
	}

	public static void setPlotRegion(Player player, int region) {
		String[] regionNames = {"all", "King's Valley", "Celsian Isles", "Architect's Ring"};
		region = Math.max(0, Math.min(region, regionNames.length - 1));
		String regionName = regionNames[region];
		int oldRegion = getPlotRegion(player);
		if (oldRegion == region) {
			player.sendMessage(appendPrefix(Component.text("Your plot region is already set as " + regionName + "!", NamedTextColor.RED)));
			return;
		}
		if (region == 3 && !PlayerUtils.hasUnlockedRing(player)) {
			player.sendMessage(appendPrefix(Component.text("You cannot set your plot region to Architect's Ring before discovering it!", NamedTextColor.RED)));
			return;
		}
		if (region == 2 && !PlayerUtils.hasUnlockedIsles(player)) {
			player.sendMessage(appendPrefix(Component.text("You cannot set your plot region to Celsian Isles before discovering it!", NamedTextColor.RED)));
			return;
		}
		ScoreboardUtils.setScoreboardValue(player, "PlotRegion", region);
		AbilityUtils.refreshClass(player);
		Plugin.getInstance().mItemStatManager.updateStats(player);
		player.sendMessage(appendPrefix(Component.text("Your plot region has been set to " + regionName + ".", NamedTextColor.GREEN)));
	}

	public static CompletableFuture<List<UUID>> getOtherAccessesToOwnerPlot(UUID plotOwnerUUID) {
		return RemoteDataAPI.getAll(plotOwnerUUID).thenApply(data -> {
			if (data.isEmpty()) {
				return Collections.emptyList();
			}

			return data.keySet().stream()
				.filter(key -> key.startsWith(MYPLOTACCESS_KEY))
				.map(key -> {
					// Split the "myplotaccess|otherUUID" key
					String[] parts = key.split("\\|");

					// Extract the UUID
					return UUID.fromString(parts[1]);
				})
				.toList();
		});
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void networkRelayMessageEvent(@NotNull NetworkRelayMessageEvent event) {
		JsonObject data = event.getData();
		switch (event.getChannel()) {
			case PLOT_ACCESS_GRANTED_NOTIFICATION_CHANNEL -> {
				if (data.has("otherUUID") && data.has("ownerName")) {
					UUID otherUUID = UUID.fromString(data.get("otherUUID").getAsString());
					String ownerName = data.get("ownerName").getAsString();

					Player addedPlayer = Bukkit.getPlayer(otherUUID);

					if (addedPlayer != null) {
						addedPlayer.sendMessage(appendPrefix(Component.text("You now have access to ", NamedTextColor.GREEN).append(Component.text(ownerName, NamedTextColor.AQUA)).append(Component.text("'s plot.", NamedTextColor.GREEN))));
					}
				}
			}

			case MODERATOR_FORCE_ADDED_PLOT_ACCESS_NOTIFICATION_CHANNEL -> {
				if (data.has("ownerUUID") && data.has("otherName")) {
					UUID ownerUUID = UUID.fromString(data.get("ownerUUID").getAsString());
					String otherName = data.get("otherName").getAsString();

					Player ownerPlayer = Bukkit.getPlayer(ownerUUID);

					if (ownerPlayer != null) {
						ownerPlayer.sendMessage(appendPrefix(Component.text(otherName, NamedTextColor.AQUA).append(Component.text(" was granted access to your plot by a moderator.", NamedTextColor.GREEN))));
					}
				}
			}

			default -> {
				// Do nothing
			}
		}
	}
}
