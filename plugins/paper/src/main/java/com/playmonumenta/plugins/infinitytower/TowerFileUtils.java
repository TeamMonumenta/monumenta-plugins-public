package com.playmonumenta.plugins.infinitytower;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.infinitytower.guis.TowerGuiItem;
import com.playmonumenta.plugins.infinitytower.guis.TowerGuiShowMobs;
import com.playmonumenta.plugins.infinitytower.mobs.TowerMobInfo;
import com.playmonumenta.plugins.infinitytower.mobs.TowerMobRarity;
import com.playmonumenta.plugins.utils.FileUtils;
import com.playmonumenta.redissync.ConfigAPI;
import com.playmonumenta.redissync.RedisAPI;

import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTItem;
import de.tr7zw.nbtapi.NBTListCompound;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class TowerFileUtils {


	public static final List<TowerMobInfo> TOWER_MOBS_INFO = new ArrayList<>();

	public static final Map<TowerMobRarity, List<TowerMobInfo>> TOWER_MOBS_RARITY_MAP = new LinkedHashMap<>();

	public static final List<TowerTeam> DEFAULT_TEAMS = new ArrayList<>();

	public static final Map<Integer, TowerTeam> PLAYERS_DEFENDERS_TEAM = new LinkedHashMap<>();

	public static final Map<Integer, List<TowerFloor>> TOWER_FLOORS_LOCATION_MAP = new LinkedHashMap<>();


	static {
		TOWER_MOBS_RARITY_MAP.put(TowerMobRarity.COMMON, new ArrayList<>());
		TOWER_MOBS_RARITY_MAP.put(TowerMobRarity.RARE, new ArrayList<>());
		TOWER_MOBS_RARITY_MAP.put(TowerMobRarity.EPIC, new ArrayList<>());
		TOWER_MOBS_RARITY_MAP.put(TowerMobRarity.LEGENDARY, new ArrayList<>());
	}

	public static void loadTowerMobsInfo() {
		TOWER_MOBS_INFO.clear();
		TOWER_MOBS_RARITY_MAP.forEach((rarity, list) -> list.clear());

		try {
			JsonObject obj = readFile("TowerMobs.json");
			int size = obj.getAsJsonPrimitive("size").getAsInt();

			JsonArray arr = obj.getAsJsonArray("mobsinfo");
			for (int i = 0; i < size; i++) {
				TowerMobInfo info = TowerMobInfo.fromJson(arr.get(i).getAsJsonObject());
				TOWER_MOBS_INFO.add(info);

				if (info.mBuyable) {
					TOWER_MOBS_RARITY_MAP.get(info.mMobRarity).add(info);
				}
			}
		} catch (Exception e) {
			warning(e.getMessage());
			e.printStackTrace();
			TowerConstants.SHOULD_GAME_START = false;
		}

		if (TowerConstants.SHOULD_GAME_START && TOWER_MOBS_INFO.isEmpty()) {
			TowerConstants.SHOULD_GAME_START = false;
		}

	}

	public static void loadDefaultTeams() {
		JsonObject tower = readFile("InfinityTowerDefault.json");
		DEFAULT_TEAMS.clear();
		try {
			JsonArray teams = tower.getAsJsonArray("DefaultTeams");
			for (int i = 0; i < teams.size(); i++) {
				DEFAULT_TEAMS.add(TowerTeam.fromJson((JsonObject) teams.get(i)));
			}
			TowerConstants.DESIGNED_FLOORS = teams.size();

		} catch (Exception e) {
			warning(e.getMessage());
			TowerConstants.SHOULD_GAME_START = false;
		}

		if (TowerConstants.SHOULD_GAME_START && DEFAULT_TEAMS.isEmpty()) {
			TowerConstants.SHOULD_GAME_START = false;
		}
	}

	public static void loadPlayerTeams() {
		JsonObject tower = readFileRedis("InfinityTowerPlayer.json");
		PLAYERS_DEFENDERS_TEAM.clear();
		try {
			JsonArray teams = tower.getAsJsonArray("playersteams");
			for (int i = 0; i < teams.size(); i++) {
				int pos = teams.get(i).getAsJsonObject().getAsJsonPrimitive("floor").getAsInt();
				TowerTeam team = TowerTeam.fromJson(teams.get(i).getAsJsonObject().get("team").getAsJsonObject());
				PLAYERS_DEFENDERS_TEAM.put(pos, team);
			}

		} catch (Exception e) {
			warning(e.getMessage());
			//TowerConstants.SHOULD_GAME_START = false; no problem ?
		}

	}

	public static void loadFloors() {
		TOWER_FLOORS_LOCATION_MAP.clear();
		try {
			JsonObject obj = readFile("InfinityFloors.json");
			JsonArray arr = obj.getAsJsonArray("floors");
			for (int i = 0; i < arr.size(); i++) {
				int id = ((JsonObject) arr.get(i)).getAsJsonPrimitive("ID").getAsInt();
				TOWER_FLOORS_LOCATION_MAP.computeIfAbsent(id, k -> new ArrayList<>());
				TOWER_FLOORS_LOCATION_MAP.get(id).add(TowerFloor.fromJson(((JsonObject) arr.get(i)).getAsJsonObject("floor")));
			}
		} catch (Exception e) {
			warning(e.getMessage());
			TowerConstants.SHOULD_GAME_START = false;
		}
	}

	public static TowerTeam getFloorTeam(int floorNum) {
		TowerTeam team = PLAYERS_DEFENDERS_TEAM.get(floorNum);
		return team != null ? team : (DEFAULT_TEAMS.size() > floorNum ? DEFAULT_TEAMS.get(floorNum) : null);
	}

	public static TowerTeam getDefaultFloorTeam(int floor) {
		if (DEFAULT_TEAMS.size() > floor) {
			return DEFAULT_TEAMS.get(floor);
		} else {
			return null;
		}
	}

	public static void saveDefaultTeam(TowerTeam team, int floor) {
		if (!DEFAULT_TEAMS.contains(team)) {
			if (floor < DEFAULT_TEAMS.size()) {
				DEFAULT_TEAMS.remove(floor);
			}
			DEFAULT_TEAMS.add(floor, team);
		}

	}


	public static void convertPlayerTeamLocation(TowerGame game) {
		TowerFloor floor = game.mFloor;

		for (TowerMob mob : game.mPlayer.mTeam.mMobs) {
			mob.setLocation(floor.mXSize - mob.getX(), mob.getY(), floor.mZSize - mob.getZ());
		}

	}

	public static void savePlayerTeam(TowerTeam team, int floor) {
		JsonObject obj = new JsonObject();
		obj.addProperty("floor", floor);
		obj.add("team", team.toJson());

		TowerManager.broadcastUpdateTower(obj);
	}

	public static void updatePlayersTower(JsonObject obj) {
		int floor = obj.getAsJsonPrimitive("floor").getAsInt();
		TowerTeam team = TowerTeam.fromJson(obj.getAsJsonObject("team"));

		PLAYERS_DEFENDERS_TEAM.put(floor, team);

		savePlayerTower();
	}

	public static void savePlayerTower() {
		JsonObject tower = new JsonObject();

		if (PLAYERS_DEFENDERS_TEAM.isEmpty()) {
			return;
		}

		JsonArray arr = tower.getAsJsonArray("playersteams");
		if (arr == null) {
			arr = new JsonArray();
			tower.add("playersteams", arr);
		}

		for (Map.Entry<Integer, TowerTeam> teamFloor : new HashSet<>(PLAYERS_DEFENDERS_TEAM.entrySet())) {
			int floor = teamFloor.getKey();
			TowerTeam team = teamFloor.getValue();

			JsonObject obj = new JsonObject();

			obj.addProperty("floor", floor);
			obj.add("team", team.toJson());
			arr.add(obj);
		}

		saveFileRedis(tower, "InfinityTowerPlayer.json");
	}

	public static void saveDefaultTower() {
		if (DEFAULT_TEAMS.isEmpty()) {
			return;
		}

		JsonObject tower = new JsonObject();

		JsonArray arr = tower.getAsJsonArray("DefaultTeams");
		if (arr == null) {
			arr = new JsonArray();
			tower.add("DefaultTeams", arr);
		}

		for (TowerTeam team : DEFAULT_TEAMS) {
			arr.add(team.toJson());
		}


		saveFile(tower, "InfinityTowerDefault.json");
	}

	public static void saveTowerMobs() {
		if (TOWER_MOBS_INFO.isEmpty()) {
			return;
		}

		JsonObject obj = new JsonObject();
		int size = TOWER_MOBS_INFO.size();
		obj.addProperty("size", size);

		JsonArray arr = new JsonArray();
		obj.add("mobsinfo", arr);

		for (TowerMobInfo info : TOWER_MOBS_INFO) {
			arr.add(info.toJson());
		}

		saveFile(obj, "TowerMobs.json");
	}

	public static TowerFloor getTowerFloor(TowerGame game, int nextFloor) {
		if (TOWER_FLOORS_LOCATION_MAP.get(game.ID) == null) {
			return null;
		}

		for (TowerFloor floor : TOWER_FLOORS_LOCATION_MAP.get(game.ID)) {
			if (floor.mMin <= nextFloor && floor.mMax >= nextFloor) {
				return floor;
			}
		}
		return null;
	}

	public static Integer getNextID() {
		Set<Integer> keys = new HashSet<>(TOWER_FLOORS_LOCATION_MAP.keySet());

		for (TowerGame game : new ArrayList<>(TowerManager.GAMES.values())) {
			keys.remove(game.ID);
		}

		if (keys.isEmpty()) {
			return null;
		}

		return keys.toArray(new Integer[0])[0];

	}

	public static String getHeadTexture(ItemStack item) {
		if (item == null || item.getType() != Material.PLAYER_HEAD) {
			return null;
		}

		try {
			return new NBTItem(item).getCompound("SkullOwner").getCompound("Properties").getCompoundList("textures").get(0).getString("Value");
		} catch (Exception e) {
			return null;
		}
	}

	public static ItemStack getHeadFromTexture(String texture) {
		if (texture == null || texture.isEmpty()) {
			return null;
		}

		ItemStack head = new ItemStack(Material.PLAYER_HEAD);

		NBTItem item = new NBTItem(head);
		NBTCompound skull = item.addCompound("SkullOwner");

		skull.setString("Name", null);
		skull.setString("Id", UUID.randomUUID().toString());

		NBTListCompound tex = skull.addCompound("Properties").getCompoundList("textures").addCompound();
		tex.setString("Value", texture);


		return item.getItem();
	}

	private static String getRedisPath(String fileName) {
		return ConfigAPI.getServerDomain() + ":infinitytower:" + fileName;

	}

	public static JsonObject readFile(String fileName) {
		try {
			return FileUtils.readJson(TowerManager.mPlugin.getDataFolder() + File.separator + "InfinityTower" + File.separator + fileName);
		} catch (Exception e) {
			warning("exception while reading file: " + fileName + " Reason: " + e.getMessage());
		}
		return null;
	}

	public static JsonObject readFileRedis(String fileName) {
		try {
			String data = RedisAPI.getInstance().async().get(getRedisPath(fileName)).get(15, TimeUnit.SECONDS);
			if (data == null) {
				warning("Tried to read file '" + fileName + "' from redis but it was empty");
			}
			return new Gson().fromJson(data, JsonObject.class);
		} catch (Exception e) {
			warning("exception while reading redis file: " + fileName + " Reason: " + e.getMessage());
		}
		return null;
	}

	public static void saveFile(JsonObject obj, String fileName) {
		try {
			FileUtils.writeJson(TowerManager.mPlugin.getDataFolder() + File.separator + "InfinityTower" + File.separator + fileName, obj);
		} catch (Exception e) {
			warning("exception while save file : " + e.getMessage());
		}
	}

	public static void saveFileRedis(JsonObject obj, String fileName) {
		RedisAPI.getInstance().async().set(getRedisPath(fileName), new Gson().toJson(obj)).whenComplete((unused, ex) -> {
			if (ex != null) {
				warning("exception while save redis file : " + ex.getMessage());
			}
		});
	}

	public static TowerMobInfo getMobInfo(String losName) {
		for (TowerMobInfo item : TOWER_MOBS_INFO) {
			if (item.mLosName.equals(losName)) {
				return item;
			}
		}
		return null;
	}

	public static void removeMobInfo(TowerMobInfo info) {
		TOWER_MOBS_INFO.remove(info);
		for (TowerMobRarity rarity : TOWER_MOBS_RARITY_MAP.keySet()) {
			TOWER_MOBS_RARITY_MAP.get(rarity).remove(info);
		}
		for (TowerTeam team : DEFAULT_TEAMS) {
			team.removeAll(info);
		}

		for (Map.Entry<Integer, TowerTeam> entry : PLAYERS_DEFENDERS_TEAM.entrySet()) {
			entry.getValue().removeAll(info);
		}



	}

	public static void updateItem(TowerMobInfo item) {
		JsonObject obj = readFile("TowerMobs.json");

		if (obj != null) {
			int size = obj.getAsJsonPrimitive("size").getAsInt();
			JsonArray arr = obj.getAsJsonArray("mobsinfo");

			boolean removed = false;

			if (arr != null) {
				for (int i = 0; i < size; i++) {
					TowerMobInfo oldItem = TowerMobInfo.fromJson((JsonObject) arr.get(i));
					if (oldItem.mLosName.equals(item.mLosName)) {
						removed = true;
						arr.remove(arr.get(i));
						break;
					}
				}
			} else {
				arr = new JsonArray();
				obj.add("mobsinfo", arr);
			}
			arr.add(item.toJson());

			if (!removed) {
				size++;
				obj.addProperty("size", size);

				TOWER_MOBS_INFO.add(item);
				TOWER_MOBS_RARITY_MAP.get(item.mMobRarity).add(item);
				TowerGuiShowMobs.MOBS_ITEMS.add(new TowerGuiItem(item.getBuyableItem()));
			} else {
				TowerGuiShowMobs.MOBS_ITEMS.clear();

				for (TowerMobInfo item2 : TOWER_MOBS_INFO) {
					TowerGuiShowMobs.MOBS_ITEMS.add(new TowerGuiItem(item2.getBuyableItem()));
				}
			}

			saveFile(obj, "TowerMobs.json");

		}
	}

	public static void warning(String msg) {
		TowerManager.mPlugin.getLogger().warning("[InfinityTower] " + msg);
	}


}
