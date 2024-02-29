package com.playmonumenta.plugins.integrations;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.seasonalevents.SeasonalEventManager;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.redissync.event.PlayerSaveEvent;
import com.playmonumenta.redissync.event.PlayerServerTransferEvent;
import java.util.UUID;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.Nullable;

public class MonumentaRedisSyncIntegration implements Listener {
	private static final String IDENTIFIER = "Monumenta";

	private static boolean mEnabled = false;

	private final Plugin mPlugin;
	private final Logger mLogger;

	public MonumentaRedisSyncIntegration(Plugin plugin) {
		mPlugin = plugin;
		mLogger = plugin.getLogger();

		mLogger.info("Enabling MonumentaRedisSync integration");
		mEnabled = true;
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void playerServerTransferEvent(PlayerServerTransferEvent event) {
		Player player = event.getPlayer();
		mLogger.info("PlayerTransferEvent: Player: " + player + "   Target: " + event.getTarget());
		event.getPlayer().clearTitle();

		player.closeInventory();

		NmsUtils.getVersionAdapter().forceDismountVehicle(player);

		if (ServerProperties.getPreventDungeonItemTransfer()) {
			int dropped = InventoryUtils.removeSpecialItems(player, false, true);
			if (dropped == 1) {
				player.sendMessage(Component.text("The dungeon key you were carrying was dropped!", NamedTextColor.RED));
			} else if (dropped > 1) {
				player.sendMessage(Component.text("The dungeon keys you were carrying were dropped!", NamedTextColor.RED));
			}
		} else {
			InventoryUtils.removeSpecialItems(player, true, true);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void playerJoinEvent(PlayerJoinEvent event) {
		Player player = event.getPlayer();

		JsonObject data = MonumentaRedisSyncAPI.getPlayerPluginData(player.getUniqueId(), IDENTIFIER);
		if (data != null) {
			if (data.has("potions")) {
				try {
					mPlugin.mPotionManager.loadFromJsonObject(player, data.get("potions").getAsJsonObject());
					mLogger.fine("Loaded potion data for player " + player.getName());
				} catch (Exception ex) {
					mLogger.severe("Failed to load potion data for player " + player.getName() + ":" + ex.getMessage());
					ex.printStackTrace();
				}
			}

			if (data.has("effects")) {
				try {
					mPlugin.mEffectManager.loadFromJsonObject(player, data.get("effects").getAsJsonObject(), mPlugin);
					mLogger.fine("Loaded effects data for player " + player.getName());
				} catch (Exception ex) {
					mLogger.severe("Failed to load effects data for player " + player.getName() + ":" + ex.getMessage());
					ex.printStackTrace();
				}
			}

			SeasonalEventManager.loadPlayerProgressJson(player, data.get("season_pass_progress"));

			if (data.has("health")) {
				try {
					double health = data.get("health").getAsDouble();
					if (health > 0) {
						// Delay by 5 ticks to allow time for effects, attributes to be processed
						Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
							if (!player.isDead() && health > player.getHealth()) {
								player.setHealth(Math.min(health, EntityUtils.getMaxHealth(player)));
							}
						}, 5);
					}

					mLogger.fine("Loaded health data (" + StringUtils.to2DP(health) + ") for player " + player.getName());
				} catch (Exception ex) {
					mLogger.severe("Failed to load health data for player " + player.getName() + ":" + ex.getMessage());
					ex.printStackTrace();
				}
			}
		} else {
			// data is null
			SeasonalEventManager.loadPlayerProgressJson(player, null);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void playerSaveEvent(PlayerSaveEvent event) {
		Player player = event.getPlayer();

		JsonObject pluginData = new JsonObject();
		pluginData.add("potions", mPlugin.mPotionManager.getAsJsonObject(player, false));
		pluginData.add("effects", mPlugin.mEffectManager.getAsJsonObject(player));
		pluginData.add("season_pass_progress", SeasonalEventManager.getPlayerProgressJson(player));
		pluginData.add("health", new JsonPrimitive(player.getHealth()));
		event.setPluginData(IDENTIFIER, pluginData);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void playerQuitEvent(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			if (!player.isOnline()) {
				mPlugin.mPotionManager.clearAllPotions(player);
				SeasonalEventManager.unloadPlayerData(player);
			}
		}, 5);
	}

	public static @Nullable String cachedUuidToName(UUID uuid) {
		if (mEnabled) {
			return MonumentaRedisSyncAPI.cachedUuidToName(uuid);
		} else {
			return Bukkit.getOfflinePlayer(uuid).getName();
		}
	}

	public static String cachedUuidToNameOrUuid(UUID uuid) {
		String name = cachedUuidToName(uuid);
		if (name != null) {
			return name;
		}
		return uuid.toString();
	}

	public static @Nullable UUID cachedNameToUuid(String name) {
		if (mEnabled) {
			return MonumentaRedisSyncAPI.cachedNameToUuid(name);
		} else {
			return null;
		}
	}

}
