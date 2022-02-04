package com.playmonumenta.plugins.cosmetics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.cosmetics.finishers.EliteFinishers;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.redissync.event.PlayerSaveEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CosmeticsManager implements Listener {

	public static final String KEY_PLUGIN_DATA = "Cosmetics";
	public static final String KEY_COSMETICS = "cosmetics";

	public static CosmeticsManager mInstance;

	public Map<UUID, List<Cosmetic>> mPlayerCosmetics;

	private CosmeticsManager() {
		mPlayerCosmetics = new HashMap<>();
	}

	public static CosmeticsManager getInstance() {
		if (mInstance == null) {
			mInstance = new CosmeticsManager();
		}
		return mInstance;
	}

	/**
	 * Returns true if the list contains the cosmetic with given name and type
	 */
	private boolean listHasCosmetic(List<Cosmetic> cosmetics, String name, CosmeticType type) {
		for (Cosmetic c : cosmetics) {
			if (c.getName().equals(name) && c.getType() == type) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Returns true if the player has unlocked the cosmetic with given name and type
	 * This is called by external methods such as plot border GUI
	 */
	public boolean playerHasCosmetic(Player p, String name, CosmeticType type) {
		List<Cosmetic> playerCosmetics = mPlayerCosmetics.get(p.getUniqueId());
		if (playerCosmetics != null) {
			return listHasCosmetic(playerCosmetics, name, type);
		}
		return false;
	}

	/**
	 * Unlocks a new cosmetic for the player with given name and type.
	 * Checks to make sure there isn't a duplicate.
	 */
	public boolean addCosmetic(Player p, CosmeticType type, String name) {
		Cosmetic cosmetic = new Cosmetic(type, name);

		if (p != null && mPlayerCosmetics.get(p.getUniqueId()) != null && !listHasCosmetic(mPlayerCosmetics.get(p.getUniqueId()), name, type)) {
			mPlayerCosmetics.get(p.getUniqueId()).add(cosmetic);
			return true;
		} else if (p != null && mPlayerCosmetics.get(p.getUniqueId()) == null) {
			List<Cosmetic> playerCosmetics = new ArrayList<>();
			playerCosmetics.add(cosmetic);
			mPlayerCosmetics.put(p.getUniqueId(), playerCosmetics);
			return true;
		}
		return false;
	}

	/**
	 * Removes the cosmetic of given name from the player's collection
	 */
	public boolean removeCosmetic(Player p, String name) {
		if (p != null && mPlayerCosmetics.get(p.getUniqueId()) != null) {
			for (Cosmetic c : mPlayerCosmetics.get(p.getUniqueId())) {
				if (c.getName().equals(name)) {
					mPlayerCosmetics.get(p.getUniqueId()).remove(c);
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Clears all the player's cosmetics (dangerous!)
	 */
	public boolean clearCosmetics(Player p) {
		List<Cosmetic> playerCosmetics = mPlayerCosmetics.get(p.getUniqueId());
		if (playerCosmetics != null) {
			mPlayerCosmetics.get(p.getUniqueId()).clear();
		}
		return true;
	}

	/**
	 * Gets a list of all unlocked cosmetics for the given player
	 */
	public @Nullable List<Cosmetic> getCosmetics(Player p) {
		return mPlayerCosmetics.get(p.getUniqueId());
	}

	/**
	 * Gets a list of unlocked cosmetic of certain type, sorted alphabetically by name
	 */
	public @Nullable List<Cosmetic> getCosmeticsOfTypeAlphabetical(Player p, CosmeticType type) {
		List<Cosmetic> playerCosmetics = mPlayerCosmetics.get(p.getUniqueId());
		if (playerCosmetics == null || playerCosmetics.size() == 0) {
			return null;
		}
		List<Cosmetic> filteredList = new ArrayList<>();
		for (Cosmetic c : playerCosmetics) {
			if (c.getType() == type) {
				filteredList.add(c);
			}
		}
		Collections.sort(filteredList, Comparator.comparing(Cosmetic::getName));
		return filteredList;
	}

	/**
	 * Gets the currently equipped cosmetic of given type for the player
	 * NOTE: If we add new types in the future with multiple equippable cosmetics,
	 * we will need to add additional functionality
	 */
	public @Nullable Cosmetic getActiveCosmetic(Player p, CosmeticType type) {
		List<Cosmetic> cosmetics = mPlayerCosmetics.get(p.getUniqueId());
		if (cosmetics != null) {
			for (Cosmetic c : cosmetics) {
				if (c.getType() == type && c.isEquipped()) {
					return c;
				}
			}
		}
		return null;
	}

	//Handlers for player lifecycle events

	//Discard cosmetic data a few ticks after player leaves shard
	//(give time for save event to register)
	@EventHandler(ignoreCancelled = true)
	public void onQuit(PlayerQuitEvent event) {
		Player p = event.getPlayer();
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			if (!p.isOnline()) {
				mPlayerCosmetics.remove(p.getUniqueId());
			}
		}, 100);
	}

	//Store local cosmetic data into plugin data
	@EventHandler(ignoreCancelled = true)
	public void onSave(PlayerSaveEvent event) {
		Player player = event.getPlayer();
		List<Cosmetic> cosmetics = mPlayerCosmetics.get(player.getUniqueId());
		if (cosmetics != null) {
			JsonObject data = new JsonObject();
			JsonArray cosmeticArray = new JsonArray();
			data.add(KEY_COSMETICS, cosmeticArray);
			Iterator<Cosmetic> iterCosmetics = cosmetics.iterator();
			while (iterCosmetics.hasNext()) {
				Cosmetic cosmetic = iterCosmetics.next();

				JsonObject cosmeticObj = new JsonObject();
				cosmeticObj.addProperty("name", cosmetic.getName());
				cosmeticObj.addProperty("type", cosmetic.getType().getType());
				cosmeticObj.addProperty("enabled", cosmetic.isEquipped());

				if (cosmeticObj != null) {
					cosmeticArray.add(cosmeticObj);
				}
			}
			event.setPluginData(KEY_PLUGIN_DATA, data);
		}
	}

	//Load plugin data into local cosmetic data
	@EventHandler(ignoreCancelled = true)
	public void onJoin(PlayerJoinEvent event) {
		Player p = event.getPlayer();
		JsonObject cosmeticData = MonumentaRedisSyncAPI.getPlayerPluginData(p.getUniqueId(), KEY_PLUGIN_DATA);
		if (cosmeticData != null) {
			if (cosmeticData.has(KEY_COSMETICS)) {
				JsonArray charmArray = cosmeticData.getAsJsonArray(KEY_COSMETICS);
				List<Cosmetic> playerCosmetics = new ArrayList<>();
				for (JsonElement cosmeticElement : charmArray) {
					JsonObject data = cosmeticElement.getAsJsonObject();
					if (data.has("name") && data.has("type") && data.has("enabled")) {
						playerCosmetics.add(new Cosmetic(CosmeticType.getTypeSelection(data.getAsJsonPrimitive("type").getAsString()), data.getAsJsonPrimitive("name").getAsString(),
							data.getAsJsonPrimitive("enabled").getAsBoolean()));
					}
				}
				//Check if we actually loaded any cosmetics
				if (playerCosmetics.size() > 0) {
					mPlayerCosmetics.put(p.getUniqueId(), playerCosmetics);
				}
			}
		}
	}

	// Elite Finisher handler
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void playerDamagedEntity(EntityDamageByEntityEvent event) {
		Entity damager = event.getDamager();
		Entity damaged = event.getEntity();

		if (damaged instanceof Mob mob
			    && damager instanceof Player player
			    && EntityUtils.isElite(damaged)
			    && event.getFinalDamage() >= mob.getHealth()) {
			Cosmetic activeCosmetic = CosmeticsManager.getInstance().getActiveCosmetic(player, CosmeticType.ELITE_FINISHER);
			if (activeCosmetic != null) {
				EliteFinishers.activateFinisher(player, event.getEntity(), event.getEntity().getLocation(), activeCosmetic.getName());
			}
		}
	}
}
