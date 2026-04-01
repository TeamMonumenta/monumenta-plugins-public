package com.playmonumenta.plugins.listeners;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.structures.StructureConquerEvent;
import com.playmonumenta.structures.StructuresPlugin;
import com.playmonumenta.structures.managers.RespawningStructure;
import java.util.HashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.loot.LootTable;
import org.jetbrains.annotations.Nullable;

public class StructureConqueredListener implements Listener {

	private static final HashMap<String, Integer> POI_DIFFICULTY = new HashMap<>();

	private static final int[] R1_SPAWNER_BRACKET = {5, 10, 15, 30, 50};
	private static final int[] R2_SPAWNER_BRACKET = {5, 10, 20, 40, 60};
	private static final String PREFIX = "epic:";
	private static final String TO_CUT = "component='";
	private static final String LOCATION = "/world/poi_conquer_chests/";
	private static final String POI = "poi/";
	private static final String LEVEL = "level_";

	private final Plugin mPlugin;
	@Nullable
	private final String REGION;

	public StructureConqueredListener(Plugin plugin) {
		mPlugin = plugin;
		if (ServerProperties.getShardName().contains("valley") || ServerProperties.getShardName().contains("dev")) {
			REGION = "r1";
			// Amplified PoI's, can't read their difficulty via DailyReward
			POI_DIFFICULTY.put("waterfall_island", 1);
			POI_DIFFICULTY.put("collapsing_tower", 1);
			POI_DIFFICULTY.put("the_litterbox", 1);
			POI_DIFFICULTY.put("mine_jungle", 1);
			POI_DIFFICULTY.put("overgrown_cave", 2);
			POI_DIFFICULTY.put("shrine_water", 1);
			POI_DIFFICULTY.put("shrine_fire", 1);
			POI_DIFFICULTY.put("fire_cave", 2);
			POI_DIFFICULTY.put("corrupted_caves", 3);
			POI_DIFFICULTY.put("shrine_air", 2);
			POI_DIFFICULTY.put("shrine_earth", 2);
			POI_DIFFICULTY.put("tree", 3);

		} else if (ServerProperties.getShardName().contains("isles")) {
			REGION = "r2";
		} else {
			REGION = null;
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
	public void structureConqueredEvent(StructureConquerEvent e) {
		StructuresPlugin structuresPlugin = StructuresPlugin.getInstance();
		if (!structuresPlugin.isEnabled() || REGION == null || structuresPlugin.mRespawnManager == null) {
			return;
		}
		RespawningStructure structure = e.getStructure();
		Location loc = e.getLocation().toCenterLocation();

		LootTable lootTable = retrieveLoot(structure);

		if (lootTable == null) {
			MMLog.severe("[StructureConquerEvent] LootTable " + PREFIX + REGION + LOCATION + LEVEL + retrieveDifficulty(structure) + " does not exist!");
			return;
		}

		Block block = loc.getBlock();
		World world = loc.getWorld();

		world.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.75f, 1.1f);
		world.playSound(loc, Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.75f, 0.9f);

		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			world.playSound(loc, Sound.ENTITY_HORSE_SADDLE, SoundCategory.PLAYERS, 1.5f, 1.0f);
			world.playSound(loc, Sound.BLOCK_CHEST_LOCKED, SoundCategory.PLAYERS, 1.5f, 1.5f);

			new PartialParticle(Particle.CLOUD, loc, 15)
				.delta(0.5)
				.distanceFalloff(15)
				.spawnFull();

			block.setType(Material.CHEST);
			Chest chest = (Chest) block.getState();

			chest.setLootTable(lootTable);
			chest.update();
		}, 1);
	}

	private @Nullable LootTable retrieveLoot(RespawningStructure structure) {
		String name = structure.getConfigLabel();
		NamespacedKey key = NamespacedKey.fromString(PREFIX + REGION + LOCATION + POI + name);
		LootTable loot = key != null ? Bukkit.getLootTable(key) : null;

		if (loot == null) {
			int poiDifficulty = retrieveDifficulty(structure);

			int spawnerReqBias = retrieveSpawnerBias(structure, poiDifficulty);

			int level = Math.clamp(poiDifficulty + spawnerReqBias, 1, 5);

			key = NamespacedKey.fromString(PREFIX + REGION + LOCATION + LEVEL + level);
			loot = key != null ? Bukkit.getLootTable(key) : null;
		}

		return loot;
	}

	private int retrieveDifficulty(RespawningStructure structure) {
		String name = structure.getConfigLabel();
		Integer difficulty = POI_DIFFICULTY.get(name);
		if (difficulty != null) {
			return difficulty;
		}

		String infoString = structure.getSpawnerInfoString();

		String[] strings = infoString.split(" ");

		// Spawner info string should have spawner count, spawner remaining & SQ components
		if (strings.length < 3 || REGION == null) {
			return 1;
		}

		// Parsing & reading actions from SpawnerBreakTrigger
		String toParse = strings[2].substring(TO_CUT.length(), strings[2].length() - 1);

		JsonObject sqObject = JsonParser.parseString(toParse).getAsJsonObject();

		if (!sqObject.has("actions") || !sqObject.get("actions").isJsonArray()) {
			return 1;
		}

		JsonArray array = sqObject.getAsJsonArray("actions");

		for (JsonElement elem : array) {
			if (!elem.isJsonObject()) {
				continue;
			}
			JsonObject action = elem.getAsJsonObject();

			if (action.has("set_scores")) {
				JsonElement scores = action.get("set_scores");

				if (!scores.isJsonObject()) {
					continue;
				}

				JsonObject scoreObject = scores.getAsJsonObject();

				String dailyReward = REGION.equals("r1") ? "DailyReward" : "Daily2Reward";

				if (scoreObject.has(dailyReward)) {
					difficulty = scoreObject.get(dailyReward).getAsInt();
					POI_DIFFICULTY.put(name, difficulty);

					return difficulty;
				}
			}
		}

		return 1;
	}

	// -1/0/+1/+2 tier depending on spawner count
	private int retrieveSpawnerBias(RespawningStructure structure, int difficulty) {
		if (REGION == null) {
			return 0;
		}

		boolean isRegionOne = REGION.equals("r1");

		int spawnerCount = structure.getSpawnerCount();
		int[] bracket = isRegionOne ? R1_SPAWNER_BRACKET : R2_SPAWNER_BRACKET;
		int min = bracket[difficulty - 1];
		int high = bracket[difficulty];

		if (difficulty != 4) {
			int max = bracket[difficulty + 1];

			if (spawnerCount >= max) {
				return 2;
			}
		}

		if (spawnerCount <= min) {
			return -1;
		}
		if (spawnerCount >= high) {
			return 1;
		}

		return 0;
	}

}
