package com.playmonumenta.plugins.poi;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.seasonalevents.SeasonalPass;
import com.playmonumenta.plugins.utils.ChestUtils;
import com.playmonumenta.plugins.utils.DateUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.redissync.event.PlayerSaveEvent;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

public class POIManager implements Listener {

	public static final String KEY_PLUGIN_DATA = "POI";
	public static final String KEY_POI = "poi";

	public static POIManager mInstance;

	public Map<UUID, List<POICompletion>> mPlayerPOI;

	public POIManager() {
		mPlayerPOI = new HashMap<>();
	}

	public static POIManager getInstance() {
		if (mInstance == null) {
			mInstance = new POIManager();
		}
		return mInstance;
	}

	public boolean completePOI(Player player, String poiName) {
		List<POICompletion> pois = mPlayerPOI.get(player.getUniqueId());
		for (POICompletion poi : pois) {
			if (poi.getPOI().getName().equals(poiName) && !poi.isCompleted()) {
				// Grab POI data
				POI finalPOI = poi.getPOI();

				// Run replacements in POI manager to update on completion
				POICompletion replacePOI = new POICompletion(POI.getPOI(poiName), true);
				pois.remove(poi);
				pois.add(replacePOI);
				mPlayerPOI.put(player.getUniqueId(), pois);

				// Generate loot chest
				List<Component> loreList = new ArrayList<>();
				loreList.add(Component.text("Bonus treasure found within " + finalPOI.getCleanName() + ".", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
				ItemStack chest = ChestUtils.giveChestWithLootTable(finalPOI.getLootPath(), "Weekly Treasure - " + finalPOI.getCleanName(),
					finalPOI.getDisplayColor(), loreList);
				chest.setAmount(1);
				InventoryUtils.giveItem(player, chest);
				return true;
			}
		}
		return false;
	}

	public void resetWeeklyClears(Player player) {
		List<POICompletion> resetPOIs = new ArrayList<>();
		for (POI poi : POI.values()) {
			resetPOIs.add(new POICompletion(poi, false));
		}
		mPlayerPOI.put(player.getUniqueId(), resetPOIs);
	}

	public static void handlePlayerDailyChange(Player p) {
		// Use Season Pass for week data
		SeasonalPass seasonalPass = Plugin.getInstance().mSeasonalEventManager.getPass();
		if (seasonalPass != null && seasonalPass.isActive()) {
			int currentPassWeek = seasonalPass.getWeekOfPass();
			int playerLastDailyVersion = ScoreboardUtils.getScoreboardValue(p, "DailyVersion").orElse(0);
			LocalDateTime lastPlayedDate = DateUtils.localDateTime(playerLastDailyVersion);
			int lastPlayedWeek = seasonalPass.getWeekOfPass(lastPlayedDate);
			if (lastPlayedWeek != currentPassWeek) {
				mInstance.resetWeeklyClears(p);
			}
		}
	}

	//Handlers for player lifecycle events

	//Discard POI data a few ticks after player leaves shard
	//(give time for save event to register)
	@EventHandler(ignoreCancelled = true)
	public void onQuit(PlayerQuitEvent event) {
		Player p = event.getPlayer();
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			if (!p.isOnline()) {
				mPlayerPOI.remove(p.getUniqueId());
			}
		}, 100);
	}

	//Store local POI data into plugin data
	@EventHandler(ignoreCancelled = true)
	public void onSave(PlayerSaveEvent event) {
		Player player = event.getPlayer();
		List<POICompletion> pois = mPlayerPOI.get(player.getUniqueId());
		if (pois != null) {
			JsonObject data = new JsonObject();
			JsonArray poiArray = new JsonArray();
			data.add(KEY_POI, poiArray);
			for (POICompletion poi : pois) {
				JsonObject poiObj = new JsonObject();
				poiObj.addProperty("name", poi.getPOI().getName());
				poiObj.addProperty("complete", poi.isCompleted());
				poiArray.add(poiObj);
			}
			event.setPluginData(KEY_PLUGIN_DATA, data);
		}
	}

	//Load plugin data into local POI data
	public void playerJoinEvent(PlayerJoinEvent event) {
		Player p = event.getPlayer();
		JsonObject poiData = MonumentaRedisSyncAPI.getPlayerPluginData(p.getUniqueId(), KEY_PLUGIN_DATA);
		if (poiData != null) {
			if (poiData.has(KEY_POI)) {
				JsonArray poiArray = poiData.getAsJsonArray(KEY_POI);
				List<POICompletion> playerPOIs = new ArrayList<>();
				for (JsonElement poiElement : poiArray) {
					JsonObject data = poiElement.getAsJsonObject();
					if (data.has("name") && data.has("complete")) {
						POICompletion toAdd = new POICompletion(POI.getPOI(data.getAsJsonPrimitive("name").getAsString()),
							data.getAsJsonPrimitive("complete").getAsBoolean());
						playerPOIs.add(toAdd);
					}
				}
				//Check if we actually loaded any POIs
				if (playerPOIs.size() > 0) {
					mPlayerPOI.put(p.getUniqueId(), playerPOIs);
				}
			}
		} else {
			resetWeeklyClears(p);
		}
	}
}
