package com.playmonumenta.plugins.player;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileNotFoundException;

import java.util.UUID;

import org.bukkit.attribute.Attribute;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.FileUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.NetworkUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

public class PlayerData {
	/**
	 * Saves player data to a file
	 *
	 * Prints a warning and stacktrace to the log if player data is not saved successfully
	 */
	static public void savePlayerData(Plugin plugin, Player player) {
		String playerdata = null;
		try {
			playerdata = convertToString(plugin, player);
		} catch (Exception e) {
			plugin.getLogger().severe("Failed to get player data");
			e.printStackTrace();
		}

		savePlayerData(plugin, player.getUniqueId(), playerdata);
	}

	/**
	 * Saves player data to a file
	 *
	 * Prints a warning and stacktrace to the log if player data is not saved successfully
	 */
	static public void savePlayerData(Plugin plugin, UUID playerUUID, String writeContent) {
		if (writeContent == null || writeContent.isEmpty()) {
			plugin.getLogger().severe("writeContent for player '" + playerUUID + "' is null or empty!");
			return;
		}

		final String fileLocation = plugin.getDataFolder() + File.separator + "players" + File.separator + playerUUID + ".json";

		try {
			FileUtils.writeFile(fileLocation, writeContent);
		} catch (Exception e) {
			plugin.getLogger().severe("Failed to write player data to " + fileLocation);
			e.printStackTrace();
		}
	}

	/**
	 * Makes a backup of the playerdata file and then removes it from the primary location
	 */
	static public void removePlayerDataFile(Plugin plugin, Player player) {
		final String fileLocation = plugin.getDataFolder() + File.separator + "players" + File.separator + player.getUniqueId() + ".json";
		final String backupFileLocation = plugin.getDataFolder() + File.separator + "backup_players" + File.separator + player.getUniqueId() + ".json";

		try {
			FileUtils.moveFile(fileLocation, backupFileLocation);
		} catch (FileNotFoundException e) {
			// Player file didn't exist, no problem
			return;
		} catch (Exception e) {
			plugin.getLogger().severe("Generic failure backing up player data file");
			e.printStackTrace();
		}
	}

	/**
	 * @return Returns a string containing serialized player data
	 *
	 * @throws Exception on error
	 */
	static public String convertToString(Plugin plugin, Player player) throws Exception {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		JsonObject root = new JsonObject();

		//	Add basic player information.
		root.addProperty("saturation", player.getSaturation());
		root.addProperty("food_level", player.getFoodLevel());
		root.addProperty("level", player.getLevel());
		root.addProperty("xp", player.getExp());

		//	Save Player Potion Data
		JsonObject potions = plugin.mPotionManager.getAsJsonObject(player);
		if (potions != null) {
			root.add("potion_info", potions);
		}

		//	Add Armor
		root.addProperty("armor", InventoryUtils.itemStackArrayToBase64(player.getInventory().getArmorContents()));

		//	Add Inventory.
		root.addProperty("inventory", InventoryUtils.itemStackArrayToBase64(player.getInventory().getContents()));

		//	Add Ender Chest.
		root.addProperty("ender_chest", InventoryUtils.itemStackArrayToBase64(player.getEnderChest().getContents()));

		//	Add Scoreboards and Tags for this player.
		root.add("scoreboards", ScoreboardUtils.getAsJsonObject(player));

		//	Save the file.
		String content = gson.toJson(root);

		if (content == null || content.isEmpty()) {
			throw new Exception("Playerdata content is empty!");
		}

		return content;
	}

	/**
	 * Main function for initializing player data when they join the server
	 * Either loads data from previously saved file or from the player themselves
	 */
	static public void initializePlayer(Plugin plugin, Player player) {
		String transferContent = null;

		/*
		 * First attempt to load the playerdata from the transfer file location
		 *
		 * If that succeeds, apply it to the player.
		 * If all goes well, remove the transfer file
		 */
		try {
			transferContent = getTransferPlayerData(plugin, player);

			/* This player just transferred from another shard - apply their info */
			if (transferContent != null) {
				applyTransferPlayerData(plugin, player, transferContent);

				removePlayerDataFile(plugin, player);
			}
		} catch (Exception e) {
			plugin.getLogger().severe("Failed to load playerdata for player '" + player.getName() + "'");
			e.printStackTrace();

			final String backupFileLocation = plugin.getDataFolder() + File.separator + "broken_players" + File.separator + player.getUniqueId() + ".json";
			plugin.getLogger().severe("Failed to apply player data from saved file to player '" + player.getName() + "'");
			plugin.getLogger().severe("Writing failing player data to '" + backupFileLocation + "'");

			try {
				FileUtils.writeFile(backupFileLocation, transferContent);
			} catch (Exception ex) {
				plugin.getLogger().severe("Failed to write player data to " + backupFileLocation);
			}

			player.sendMessage(ChatColor.RED + "Something very bad happened while transferring your player data.");
			player.sendMessage(ChatColor.RED + "  As a precaution, the server has attempted to move you to Purgatory.");
			player.sendMessage(ChatColor.RED + "  If for some reason you aren't on purgatory, take a screenshot and log off.");
			player.sendMessage(ChatColor.RED + "  Please post in #moderator-help and tag @admin");
			player.sendMessage(ChatColor.RED + "  Include details about what you were doing");
			player.sendMessage(ChatColor.RED + "  such as joining or leaving a dungeon (and which one!)");

			try {
				NetworkUtils.sendPlayer(plugin, player, "purgatory");
			} catch (Exception ex) {
				plugin.getLogger().severe("CRITICAL: Failed to send failed player '" + player.getName() + "' to purgatory");
				ex.printStackTrace();
			}
		}

		/*
		 * If there was no transferred data, need to load potion data saved to their character
		 * into the potion manager
		 */
		if (transferContent == null) {
			plugin.mPotionManager.loadFromPlayer(player);
		}

		// Now that the player is all set up, refresh their abilities based on their class
		plugin.mPotionManager.refreshClassEffects(player);
	}

	/**
	 * Loads player data from file storage if it exists and returns it
	 *
	 * @throws Exception on error
	 */
	static private String getTransferPlayerData(Plugin plugin, Player player) throws Exception {
		final String fileLocation = plugin.getDataFolder() + File.separator + "players" + File.separator + player.getUniqueId() + ".json";

		String content = null;
		try {
			content = FileUtils.readFile(fileLocation);
		} catch (FileNotFoundException e) {
			// This is the usual case when a player logs out and back in on the same server
			return null;
		}
		if (content == null || content.isEmpty()) {
			// This is bad - if the file didn't exist, a FileNotFound exception should have been raised, not bad/empty data returned
			throw new Exception("No player data returned for player '" + player.getName() + "'");
		}

		return content;
	}

	/**
	 * Loads player data from provided string and applies it to the player
	 *
	 * @throws Exception on error
	 */
	static private void applyTransferPlayerData(Plugin plugin, Player player, String content) throws Exception {
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
		plugin.mPotionManager.loadFromJsonObject(player, object);

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

		//	Load Scoreboards and Tags
		ScoreboardUtils.loadFromJsonObject(player, object.getAsJsonObject("scoreboards"));
	}
}
