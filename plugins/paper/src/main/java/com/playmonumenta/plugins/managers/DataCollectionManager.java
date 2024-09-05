package com.playmonumenta.plugins.managers;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.FileUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadableNBT;
import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.Nullable;

public class DataCollectionManager {

	public static final List<String> EXCLUDED_SHARDS = List.of("build", "plots", "playerplots", "tutorial", "purgatory");
	public static final List<InfusionType> INTERESTING_INFUSIONS = Arrays.stream(InfusionType.values()).filter(InfusionType::isDataCollected).toList();
	private static final int RUN_INTERVAL = Constants.ONE_HOUR;
	private static final String EXPORT_PATH = ServerProperties.getGameplayDataExportPath();

	private final Server mServer = Plugin.getInstance().getServer();
	private @Nullable BukkitTask mDataCollectionTask;

	public DataCollectionManager() {
		if (Plugin.IS_PLAY_SERVER) {
			resumeDataCollection();
		}
	}

	private void addDataPoint(List<World> worlds) {
		DataPoint dataPoint = new DataPoint();
		worlds.forEach(world -> dataPoint.addAllPlayerInformation(processWorld(world)));

		long timestamp = dataPoint.getTimestamp();
		String finalPath = EXPORT_PATH + "%s - %s.json".formatted(timestamp, dataPoint.getShardName());
		if (dataPoint.getPlayerInformation().isEmpty()) {
			MMLog.info("Skipping writing gameplay data to %s - 0 players found".formatted(finalPath));
			return;
		}

		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			try {
				MMLog.info("Writing gameplay data of %s players to %s".formatted(dataPoint.getPlayerInformation().size(), finalPath));
				FileUtils.writeJson(finalPath, dataPoint.toJson(), false);
			} catch (IOException e) {
				MMLog.warning("Failed to export player gameplay data of %s players to %s".formatted(dataPoint.getPlayerInformation().size(), finalPath));
			}
		});
	}

	private List<PlayerInformation> processWorld(World world) {
		ArrayList<PlayerInformation> playerInformation = new ArrayList<>();
		world.getPlayers().forEach(player -> playerInformation.add(processPlayer(world, player)));
		return playerInformation;
	}

	private PlayerInformation processPlayer(World world, Player player) {
		return new PlayerInformation(world.getName(), player);
	}

	private Map<String, Integer> getPlayerSkills(Player player) {
		HashMap<String, Integer> skills = new HashMap<>();
		AbilityManager.getManager().getPlayerAbilities(player).getAbilitiesIgnoringSilence().forEach(ability -> {
			String scoreboardId = ability.getInfo().getScoreboard();
			if (scoreboardId != null) {
				int scoreboardValue = ScoreboardUtils.getScoreboardValue(player, scoreboardId).orElse(0);
				skills.put(scoreboardId, scoreboardValue);
			}
		});
		return skills;
	}

	private List<String> getPlayerCharms(Player player, CharmManager.CharmType charmType) {
		return CharmManager.getInstance().getCharms(player, charmType).stream().map(itemStack -> MessagingUtils.plainText(itemStack.getItemMeta().displayName())).toList();
	}

	private void addOrSumInfusionLevel(InfusionType infusionType, ReadableNBT infusionsNbt, JsonObject infusionsObject) {
		int level = ItemStatUtils.getInfusionLevel(infusionsNbt, infusionType);
		if (level == 0) {
			return;
		}
		if (infusionsObject.has(infusionType.getName())) {
			infusionsObject.addProperty(infusionType.getName(), infusionsObject.get(infusionType.getName()).getAsInt() + level);
		} else {
			infusionsObject.addProperty(infusionType.getName(), level);
		}
	}

	private void examineEquipmentItem(ItemStack item, String slotName, JsonObject gearObject, JsonObject infusionObject) {
		if (item == null || item.getType() == Material.AIR) {
			return;
		}

		gearObject.addProperty(slotName, MessagingUtils.plainText(ItemUtils.getDisplayName(item)));
		NBT.get(item, nbt -> {
			ReadableNBT infusionNbt = ItemStatUtils.getInfusions(nbt);
			if (infusionNbt != null) {
				INTERESTING_INFUSIONS.forEach(infusionType -> {
					addOrSumInfusionLevel(infusionType, infusionNbt, infusionObject);
				});
			}
		});
	}

	private JsonObject getPlayerGear(Player player) {
		PlayerInventory inventory = player.getInventory();
		ItemStack mainhandItem = inventory.getItemInMainHand();

		ArrayList<ItemStack> hotbarToExamine = new ArrayList<>();
		for (int i = 0; i < 9; i++) {
			hotbarToExamine.add(inventory.getItem(i));
		}

		JsonObject gearNames = new JsonObject();
		JsonArray hotbarNames = new JsonArray();
		JsonObject activeInfusions = new JsonObject();

		examineEquipmentItem(inventory.getItemInOffHand(), "offhand", gearNames, activeInfusions);
		examineEquipmentItem(inventory.getHelmet(), "helmet", gearNames, activeInfusions);
		examineEquipmentItem(inventory.getChestplate(), "chestplate", gearNames, activeInfusions);
		examineEquipmentItem(inventory.getLeggings(), "leggings", gearNames, activeInfusions);
		examineEquipmentItem(inventory.getBoots(), "boots", gearNames, activeInfusions);

		String selectedItemName = "none";
		if (mainhandItem.getType() != Material.AIR) {
			selectedItemName = MessagingUtils.plainText(ItemUtils.getDisplayName(mainhandItem));
			NBT.get(mainhandItem, nbt -> {
				INTERESTING_INFUSIONS.forEach(infusionType -> {
					ReadableNBT infusionNbt = ItemStatUtils.getInfusions(nbt);
					if (infusionNbt != null) {
						addOrSumInfusionLevel(infusionType, infusionNbt, activeInfusions);
					}
				});
			});
		}

		hotbarToExamine.forEach(item -> {
			if (item != null && item.getType() != Material.AIR) {
				hotbarNames.add(MessagingUtils.plainText(ItemUtils.getDisplayName(item)));
			}
		});

		JsonObject equipment = new JsonObject();
		equipment.add("gear_names", gearNames);
		equipment.add("hotbar_names", hotbarNames);
		equipment.add("active_infusions", activeInfusions);
		equipment.addProperty("selected_item", selectedItemName);
		return equipment;
	}

	private static class DataPoint {
		private final ArrayList<PlayerInformation> mPlayerInformation = new ArrayList<>();
		private final long mTimestamp;
		private final String mShardName;

		public DataPoint() {
			mTimestamp = Instant.now().toEpochMilli();
			mShardName = ServerProperties.getShardName();
		}

		public JsonObject toJson() {
			JsonArray playerInformation = new JsonArray();
			mPlayerInformation.forEach(info -> playerInformation.add(info.toJson()));

			JsonObject obj = new JsonObject();
			obj.addProperty("timestamp", mTimestamp);
			obj.addProperty("shard_name", mShardName);
			obj.add("player_info", playerInformation);
			return obj;
		}

		public List<PlayerInformation> getPlayerInformation() {
			return mPlayerInformation;
		}

		public void addPlayerInformation(PlayerInformation playerInformation) {
			mPlayerInformation.add(playerInformation);
		}

		public void addAllPlayerInformation(List<PlayerInformation> playerInformation) {
			playerInformation.forEach(this::addPlayerInformation);
		}

		public long getTimestamp() {
			return mTimestamp;
		}

		public String getShardName() {
			return mShardName;
		}
	}

	private class PlayerInformation {
		private final JsonObject mGearData;
		private final CharmManager.CharmType mCharmType;
		private final List<String> mCharmNames;
		private final Map<String, Double> mCharmSummary;
		private final Map<String, Integer> mSkills;
		private final String mWorldName;
		private final String mPlayerName;
		private final String mClass;
		private final String mSpec;
		private final String mRegionName;

		public PlayerInformation(String worldName, Player player) {
			mGearData = getPlayerGear(player);
			mCharmType = Plugin.getInstance().mCharmManager.mEnabledCharmType;
			mCharmNames = getPlayerCharms(player, mCharmType);
			mCharmSummary = Plugin.getInstance().mCharmManager.getSummaryOfAllAttributes(player, mCharmType);
			mSkills = getPlayerSkills(player);
			mWorldName = worldName;
			mPlayerName = player.getName();
			mClass = AbilityUtils.getClass(player);
			mSpec = AbilityUtils.getSpec(player);
			mRegionName = ServerProperties.getRegion(player).getName();
		}

		public JsonObject toJson() {
			JsonObject skills = new JsonObject();
			mSkills.forEach(skills::addProperty);
			JsonArray charmNames = new JsonArray();
			mCharmNames.forEach(charmNames::add);
			JsonObject charmSummary = new JsonObject();
			mCharmSummary.forEach(charmSummary::addProperty);

			JsonObject obj = new JsonObject();
			obj.addProperty("player_name", mPlayerName);
			obj.addProperty("world_name", mWorldName);
			obj.addProperty("class_name", mClass);
			obj.addProperty("spec_name", mSpec);
			obj.addProperty("region_name", mRegionName);
			obj.addProperty("selected_item", mGearData.get("selected_item").getAsString());
			obj.addProperty("charm_type", mCharmType.name());
			obj.add("skills", skills);
			obj.add("gear_names", mGearData.get("gear_names"));
			obj.add("hotbar_names", mGearData.get("hotbar_names"));
			obj.add("active_infusions", mGearData.get("active_infusions"));
			obj.add("charm_names", charmNames);
			obj.add("charm_summary", charmSummary);

			return obj;
		}
	}

	public boolean pauseDataCollection() {
		if (EXCLUDED_SHARDS.contains(ServerProperties.getShardName())) {
			return false;
		}

		if (mDataCollectionTask != null) {
			mDataCollectionTask.cancel();
		}
		return true;
	}

	public boolean resumeDataCollection() {
		if (EXCLUDED_SHARDS.contains(ServerProperties.getShardName())) {
			return false;
		}

		mDataCollectionTask = new BukkitRunnable() {
			@Override
			public void run() {
				addDataPoint(mServer.getWorlds());
			}
		}.runTaskTimer(Plugin.getInstance(), RUN_INTERVAL, RUN_INTERVAL);
		return true;
	}
}
