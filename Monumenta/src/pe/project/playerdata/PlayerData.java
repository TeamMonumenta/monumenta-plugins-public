package pe.project.playerdata;

import java.util.UUID;
import java.io.File;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.attribute.Attribute;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import pe.project.Main;
import pe.project.utils.FileUtils;
import pe.project.utils.InventoryUtils;
import pe.project.utils.ScoreboardUtils;

public class PlayerData {
	/**
	 * Saves player data to a file
	 *
	 * Prints a warning and stacktrace to the log if player data is not saved successfully
	 */
	static public void savePlayerData(Main main, Player player) {
		String playerdata = null;
		try {
			playerdata = convertToString(main, player);
		} catch (Exception e) {
			main.getLogger().severe("Failed to get player data");
			e.printStackTrace();
		}

		savePlayerData(main, player.getUniqueId(), playerdata);
	}

	/**
	 * Saves player data to a file
	 *
	 * Prints a warning and stacktrace to the log if player data is not saved successfully
	 */
	static public void savePlayerData(Main main, UUID playerUUID, String writeContent) {
		if (writeContent == null || writeContent.isEmpty()) {
			main.getLogger().severe("writeContent for player '" + playerUUID + "' is null or empty!");
			return;
		}

		final String fileLocation = main.getDataFolder() + File.separator + "players" + File.separator + playerUUID + ".json";

		try {
			FileUtils.writeFile(fileLocation, writeContent);
		} catch (Exception e) {
			main.getLogger().severe("Failed to write player data to " + fileLocation);
			e.printStackTrace();
		}
	}

	/**
	 * @return Returns a string containing serialized player data
	 *
	 * @throws Exception on error
	 */
	static public String convertToString(Main main, Player player) throws Exception {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		JsonObject root = new JsonObject();

		//	Add basic player information.
		root.addProperty("saturation", player.getSaturation());
		root.addProperty("food_level", player.getFoodLevel());
		root.addProperty("level", player.getLevel());
		root.addProperty("xp", player.getExp());

		//	Save Player Potion Data
		JsonObject potions = main.mPotionManager.getAsJsonObject(player);
		if (potions != null) {
			root.add("potion_info", potions);
		}

		//	Add Armor
		root.addProperty("armor", InventoryUtils.itemStackArrayToBase64(player.getInventory().getArmorContents()));

		//	Add Inventory.
		root.addProperty("inventory", InventoryUtils.itemStackArrayToBase64(player.getInventory().getContents()));

		//	Add Ender Chest.
		root.addProperty("ender_chest", InventoryUtils.itemStackArrayToBase64(player.getEnderChest().getContents()));

		//	Add Scoreboards for this player.
		root.add("scoreboards", ScoreboardUtils.getAsJsonObject(player));

		//	Save the file.
		String content = gson.toJson(root);
		return content;
	}

	/**
	 * Loads player data from file storage and applies it to the player
	 *
	 * @throws Exception on error
	 */
	static public void loadPlayerData(Main main, Player player) throws Exception {
		final String fileLocation = main.getDataFolder() + File.separator + "players" + File.separator + player.getUniqueId() + ".json";

		String content = FileUtils.readFile(fileLocation);
		if (content == null || content.isEmpty()) {
			// This is expected when a player has never logged on before
			main.getLogger().info("No player data file for '" + player.getName() + "'");
			return;
		}

		try {
			_loadPlayerData(main, player, content);
		} catch (Exception e) {
			final String backupFileLocation = main.getDataFolder() + File.separator + "broken_players" + File.separator + player.getUniqueId() + ".json";
			main.getLogger().severe("Failed to apply player data from saved file to player '" + player.getName() + "'");
			main.getLogger().severe("Writing failing player data to '" + backupFileLocation + "'");

			try {
				FileUtils.writeFile(backupFileLocation, content);
			} catch (Exception ex) {
				main.getLogger().severe("Failed to write player data to " + backupFileLocation);
			}

			// Propogate the more-important exception regardless of whether the player data writing failed
			throw new Exception(e);
		}
	}

	/**
	 * Loads player data from provided string and applies it to the player
	 *
	 * @throws Exception on error
	 */
	static private void _loadPlayerData(Main main, Player player, String content) throws Exception {
		if (content == null || content.isEmpty()) {
			throw new Exception("Specified player content is null or empty!");
		}

		Gson gson = new Gson();

		//	Load the file, if it exist than let's start parsing it.
		JsonObject object = gson.fromJson(content, JsonObject.class);

		//	Set health to max
		player.setHealth(player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());

		//	Load Saturation.
		JsonElement saturation = object.get("saturation");
		if (saturation != null) {
			player.setSaturation(saturation.getAsFloat());
		}

		//	Load Exhaustion.
		JsonElement foodLevel = object.get("food_level");
		if (foodLevel != null) {
			player.setFoodLevel(foodLevel.getAsInt());
		}

		//	Load Level.
		JsonElement level = object.get("level");
		if (level != null) {
			player.setLevel(level.getAsInt());
		}

		//	Load Experience.
		JsonElement xp = object.get("xp");
		if (xp != null) {
			player.setExp(xp.getAsFloat());
		}

		//	Load Player Potion Data.
		main.mPotionManager.loadFromJsonObject(player, object);

		//	Load Armor.
		JsonElement armor = object.get("armor");
		if (armor != null) {
			player.getInventory().setArmorContents(InventoryUtils.itemStackArrayFromBase64(armor.getAsString()));
		}

		//	Load Inventory.
		JsonElement inventory = object.get("inventory");
		if (inventory != null) {
			ItemStack[] inv = InventoryUtils.itemStackArrayFromBase64(inventory.getAsString());
			if (inv != null) {
				player.getInventory().setContents(inv);
			}
		}

		//	Load Ender Chest.
		JsonElement ender = object.get("ender_chest");
		if (ender != null) {
			ItemStack[] inv = InventoryUtils.itemStackArrayFromBase64(ender.getAsString());
			if (inv != null) {
				player.getEnderChest().setContents(inv);
			}
		}

		//	Load Scoreboards.
		ScoreboardUtils.loadFromJsonObject(player, object.getAsJsonArray("scoreboards"));
	}
}
