package com.playmonumenta.plugins.listeners;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.utils.FileUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import io.papermc.paper.event.server.ServerResourcesReloadedEvent;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.Nullable;

public class LootTableManager implements Listener {
	public static final LootTableManager INSTANCE = new LootTableManager();

	private static final Set<NamespacedKey> BUILTIN_LOOT_TABLES;

	static {
		Set<NamespacedKey> builtinLootTables;
		try {
			builtinLootTables = Set.of(
				NamespacedKey.fromString("minecraft:empty")
			);
		} catch (Exception ex) {
			MMLog.severe("Failed to load list of built-in loot tables.");
			MessagingUtils.sendStackTrace(Bukkit.getConsoleSender(), ex);
			builtinLootTables = Set.of();
		}
		BUILTIN_LOOT_TABLES = builtinLootTables;
	}

	public static final class LootTableEntry {
		private final Set<NamespacedKey> mParents = new HashSet<>();
		private final Set<NamespacedKey> mChildren = new HashSet<>();
		private final Set<String> mPaths = new HashSet<>();
		private final NamespacedKey mKey;
		private boolean mHasBonusRolls = false;

		LootTableEntry(NamespacedKey key, String path) {
			mKey = key;
			addPath(path);
		}

		public NamespacedKey getKey() {
			return mKey;
		}

		public boolean hasBonusRolls() {
			return mHasBonusRolls;
		}

		private void addParent(NamespacedKey parent) {
			mParents.add(parent);
		}

		private void addPath(String path) {
			mPaths.add(path);
		}

		private void propagateHasBonusRolls(Map<NamespacedKey, LootTableEntry> map) {
			if (mHasBonusRolls) {
				for (NamespacedKey key : mParents) {
					LootTableEntry parent = map.get(key);
					if (parent != null) {
						parent.mHasBonusRolls = true;
						parent.propagateHasBonusRolls(map);
					} else {
						MMLog.warning("Failed to look up parent loot table, this should be impossible: " + key);
					}
				}
			}
		}

		/* Example loot table:
		{
		  "pools": [
			{
			  "rolls": {
				"min": 1,
				"max": 2
			  },
			  "bonus_rolls": 1,
			  "entries": [
				{
				  "type": "loot_table",
				  "weight": 32,
				  "name": "epic:r2/delves/auxiliary/bonus_tiered"
				}
			  ]
			}
		  ]
		}
		*/

		/*
		 * This will load the loot table and parse out:
		 * - Whether it has any entries that specify bonus rolls
		 * - What child loot tables are called by this table
		 *
		 * This uses the passed-in map to find the child entries and link them together
		 * Note that this isn't allowed to add or remove items from the map or it will cause an exception
		 */
		private void loadAndUpdateChildren(Map<NamespacedKey, LootTableEntry> map) {
			for (String path : mPaths) {
				try {
					JsonObject json = FileUtils.readJson(path);
					JsonElement pools = json.get("pools");
					if (pools != null && pools.isJsonArray()) {
						for (JsonElement poolElement : pools.getAsJsonArray()) {
							if (poolElement.isJsonObject()) {
								JsonObject pool = poolElement.getAsJsonObject();

								JsonElement bonusRolls = pool.get("bonus_rolls");
								if (bonusRolls != null) {
									// Bonus rolls not null, could be a range or a value
									if (bonusRolls.isJsonPrimitive() && bonusRolls.getAsJsonPrimitive().isNumber()) {
										if (Math.abs(bonusRolls.getAsJsonPrimitive().getAsNumber().doubleValue()) >= 0.001) {
											// Nonzero, definitely has bonus rolls
											mHasBonusRolls = true;
										}
									} else if (bonusRolls.isJsonObject()) {
										JsonObject numberProvider = bonusRolls.getAsJsonObject();
										JsonElement min = numberProvider.get("min");
										JsonElement max = numberProvider.get("max");
										// If any of these is true, then assume bonus_rolls are nonzero
										if (min == null || max == null ||
												!min.isJsonPrimitive() || !max.isJsonPrimitive() ||
												!min.getAsJsonPrimitive().isNumber() || !max.getAsJsonPrimitive().isNumber() ||
												Math.abs(min.getAsJsonPrimitive().getAsNumber().doubleValue()) >= 0.001 || Math.abs(max.getAsJsonPrimitive().getAsNumber().doubleValue()) >= 0.001) {
											mHasBonusRolls = true;
										} // Otherwise has min/max but they are both zero, don't count that as bonus rolls
									} else {
										MMLog.warning("Found strange loot table with 'bonus_rolls' entry that is neither a number or an object: " + path);
									}
								}

								JsonElement entries = pool.get("entries");
								if (entries != null && entries.isJsonArray()) {
									for (JsonElement entryElement : entries.getAsJsonArray()) {
										if (entryElement.isJsonObject()) {
											JsonObject entry = entryElement.getAsJsonObject();
											JsonElement typeElement = entry.get("type");
											if (typeElement != null && typeElement.isJsonPrimitive() && typeElement.getAsJsonPrimitive().isString()) {
												if (typeElement.getAsJsonPrimitive().getAsString().equals("loot_table")) {
													JsonElement nameElement = entry.get("name");
													if (nameElement != null && nameElement.isJsonPrimitive() && nameElement.getAsJsonPrimitive().isString()) {
														String name = nameElement.getAsJsonPrimitive().getAsString();
														NamespacedKey childKey = NamespacedKey.fromString(name);
														if (childKey != null) {
															LootTableEntry child = map.get(childKey);
															if (child != null) {
																child.addParent(mKey);
																mChildren.add(childKey);
															} else if (!BUILTIN_LOOT_TABLES.contains(childKey)) {
																MMLog.warning("Found strange loot table that specifies nonexistent child '" + childKey + "': " + path);
															}
														} else {
															MMLog.warning("Found strange loot table with 'entries' entry with invalid 'name': " + path);
														}
													} else {
														MMLog.warning("Found strange loot table with 'entries' entry that is missing 'name': " + path);
													}
												}
											} else {
												MMLog.warning("Found strange loot table with 'entries' entry that is missing 'type': " + path);
											}
										} else {
											MMLog.warning("Found strange loot table with 'entries' entry that is not an object: " + path);
										}
									}
								} else {
									MMLog.warning("Found strange loot table with pool missing 'entries': " + path);
								}
							} else {
								MMLog.warning("Found strange loot table with 'pools' entry that is not an object: " + path);
							}
						}
					} else if (!path.contains("/datapacks/vanilla/")) {
						MMLog.warning("Found strange loot table missing 'pools': " + path);
					}
				} catch (Exception ex) {
					MMLog.severe("Failed to load loot table '" + path + "' : " + ex.getMessage());
					ex.printStackTrace();
				}
			}
		}
	}

	private final Map<NamespacedKey, LootTableEntry> mTables = new HashMap<>();

	private LootTableManager() {
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void serverResourcesReloadedEvent(ServerResourcesReloadedEvent event) {
		reload();
	}

	public void reload() {
		mTables.clear();

		Map<NamespacedKey, Path> datapackFiles = FileUtils.getEnabledDatapackFiles("loot_tables", ".json");
		if (datapackFiles.size() == 0) {
			MMLog.severe("Failed to find any matching loot tables from datapacks - this may be a critical problem");
		}

		// Load all the loot table namespaces into the map
		for (Map.Entry<NamespacedKey, Path> file : datapackFiles.entrySet()) {
			String path = file.getValue().toString();
			// Insert new or update the existing record
			mTables.compute(file.getKey(), (k, v) -> {
				if (v == null) {
					return new LootTableEntry(k, path);
				} else {
					v.addPath(path);
					return v;
				}
			});
		}

		// Actually load the loot tables & update parents and children
		for (LootTableEntry entry : mTables.values()) {
			entry.loadAndUpdateChildren(mTables);
		}

		// Loop over each entry as a child, propagating hasBonusRolls up to all of their parents if it is set
		for (LootTableEntry entry : mTables.values()) {
			entry.propagateHasBonusRolls(mTables);
		}

		// Print out all the loot tables with and without bonus rolls:
		MMLog.finer("Loot tables with bonus rolls:");
		for (LootTableEntry entry : mTables.values()) {
			if (entry.hasBonusRolls()) {
				MMLog.finer("  " + entry.getKey());
			}
		}
		MMLog.finer("Loot tables without bonus rolls:");
		for (LootTableEntry entry : mTables.values()) {
			if (!entry.hasBonusRolls()) {
				MMLog.finer("  " + entry.getKey());
			}
		}
	}

	public static @Nullable LootTableEntry getLootTableEntry(NamespacedKey key) {
		return INSTANCE.mTables.get(key);
	}
}
