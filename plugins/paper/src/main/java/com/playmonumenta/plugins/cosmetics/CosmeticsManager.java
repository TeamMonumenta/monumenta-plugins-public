package com.playmonumenta.plugins.cosmetics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.cosmetics.finishers.EliteFinishers;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.redissync.event.PlayerSaveEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.checkerframework.checker.nullness.qual.Nullable;

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
	private boolean listHasCosmetic(List<Cosmetic> cosmetics, CosmeticType type, String name) {
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
	public boolean playerHasCosmetic(Player player, CosmeticType type, @Nullable String name) {
		if (name == null) {
			return false;
		}
		List<Cosmetic> playerCosmetics = mPlayerCosmetics.get(player.getUniqueId());
		if (playerCosmetics != null) {
			return listHasCosmetic(playerCosmetics, type, name);
		}
		return false;
	}

	/**
	 * Unlocks a new cosmetic for the player with given name and type.
	 * Checks to make sure there isn't a duplicate.
	 */
	public boolean addCosmetic(Player player, CosmeticType type, String name) {
		if (player == null) {
			return false;
		}
		List<Cosmetic> playerCosmetics = mPlayerCosmetics.computeIfAbsent(player.getUniqueId(), key -> new ArrayList<>());
		if (!listHasCosmetic(playerCosmetics, type, name)) {
			playerCosmetics.add(new Cosmetic(type, name));
			return true;
		}
		return false;
	}

	/**
	 * Removes the cosmetic of given name from the player's collection
	 */
	public boolean removeCosmetic(Player player, CosmeticType type, String name) {
		if (player == null) {
			return false;
		}
		List<Cosmetic> playerCosmetics = mPlayerCosmetics.get(player.getUniqueId());
		if (playerCosmetics != null) {
			for (Cosmetic c : playerCosmetics) {
				if (c.getType() == type && c.getName().equals(name)) {
					playerCosmetics.remove(c);
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Clears all the player's cosmetics of a certain type (dangerous!)
	 */
	public void clearCosmetics(Player player, CosmeticType type) {
		List<Cosmetic> playerCosmetics = mPlayerCosmetics.get(player.getUniqueId());
		if (playerCosmetics != null) {
			playerCosmetics.removeIf(c -> c.getType() == type);
		}
	}

	/**
	 * Gets a list of all unlocked cosmetics for the given player
	 */
	public List<Cosmetic> getCosmetics(Player player) {
		return mPlayerCosmetics.getOrDefault(player.getUniqueId(), Collections.emptyList());
	}

	/**
	 * Gets a list of unlocked cosmetic of certain type, sorted alphabetically by name
	 */
	public List<Cosmetic> getCosmeticsOfTypeAlphabetical(Player player, CosmeticType type) {
		return getCosmetics(player).stream()
			.filter(c -> c.getType() == type)
			.sorted(Comparator.comparing(Cosmetic::getName))
			.toList();
	}

	/**
	 * Gets the currently equipped cosmetic of given type for the player
	 * NOTE: If we add new types in the future with multiple equippable cosmetics,
	 * we will need to add additional functionality
	 */
	public @Nullable Cosmetic getActiveCosmetic(Player player, CosmeticType type) {
		List<Cosmetic> cosmetics = mPlayerCosmetics.get(player.getUniqueId());
		if (cosmetics != null) {
			for (Cosmetic c : cosmetics) {
				if (c.getType() == type && c.isEquipped()) {
					return c;
				}
			}
		}
		return null;
	}

	public List<Cosmetic> getActiveCosmetics(Player player, CosmeticType type) {
		List<Cosmetic> cosmetics = mPlayerCosmetics.get(player.getUniqueId());
		if (cosmetics != null) {
			return cosmetics.stream().filter(c -> c.getType() == type && c.isEquipped()).toList();
		}
		return Collections.emptyList();
	}

	public @Nullable Cosmetic getRandomActiveCosmetic(Player player, CosmeticType type) {
		List<Cosmetic> activeCosmetics = getActiveCosmetics(player, type);
		if (!activeCosmetics.isEmpty()) {
			return activeCosmetics.get(FastUtils.RANDOM.nextInt(activeCosmetics.size()));
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
			for (Cosmetic cosmetic : cosmetics) {
				JsonObject cosmeticObj = new JsonObject();
				cosmeticObj.addProperty("name", cosmetic.getName());
				cosmeticObj.addProperty("type", cosmetic.getType().getType());
				cosmeticObj.addProperty("enabled", cosmetic.isEquipped());
				cosmeticArray.add(cosmeticObj);
			}
			event.setPluginData(KEY_PLUGIN_DATA, data);
		}
	}

	//Load plugin data into local cosmetic data
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void playerJoinEvent(PlayerJoinEvent event) {
		Player p = event.getPlayer();
		JsonObject cosmeticData = MonumentaRedisSyncAPI.getPlayerPluginData(p.getUniqueId(), KEY_PLUGIN_DATA);
		if (cosmeticData != null) {
			if (cosmeticData.has(KEY_COSMETICS)) {
				JsonArray cosmeticArray = cosmeticData.getAsJsonArray(KEY_COSMETICS);
				List<Cosmetic> playerCosmetics = new ArrayList<>();
				for (JsonElement cosmeticElement : cosmeticArray) {
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
		// call the "event listener" of the vanity manager after the cosmetics manager loaded cosmetics
		Plugin.getInstance().mVanityManager.playerJoinEvent(event);
	}

	// Elite Finisher handler
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void entityDeathEvent(EntityDeathEvent event) {
		LivingEntity mob = event.getEntity();
		Player player = mob.getKiller();

		if (player != null && EntityUtils.isElite(mob)) {
			Cosmetic activeCosmetic = CosmeticsManager.getInstance().getRandomActiveCosmetic(player, CosmeticType.ELITE_FINISHER);
			if (activeCosmetic != null) {
				EliteFinishers.activateFinisher(player, mob, mob.getLocation(), activeCosmetic.getName());
			}
		}
	}
}
