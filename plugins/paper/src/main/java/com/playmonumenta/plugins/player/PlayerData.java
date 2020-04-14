package com.playmonumenta.plugins.player;

import org.bukkit.entity.Player;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;

public class PlayerData {

	/**
	 * @return Returns a string containing serialized player data
	 *
	 * @throws Exception on error
	 */
	public static String convertToString(Plugin plugin, Player player) throws Exception {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		JsonObject root = new JsonObject();

		//  Save Player Potion Data
		JsonObject potions = plugin.mPotionManager.getAsJsonObject(player);
		if (potions != null) {
			root.add("potion_info", potions);
		}

		//  Save the file.
		String content = gson.toJson(root);

		if (content == null || content.isEmpty()) {
			throw new Exception("Playerdata content is empty!");
		}

		return content;
	}


	/**
	 * Loads player data from provided string and applies it to the player
	 *
	 * @throws Exception on error
	 */
	private static void applyTransferPlayerData(Plugin plugin, Player player, String content) throws Exception {
		if (content == null || content.isEmpty()) {
			throw new Exception("Specified player content is null or empty!");
		}

		Gson gson = new Gson();

		//  Load the file, if it exist than let's start parsing it.
		JsonObject object = gson.fromJson(content, JsonObject.class);

		//  Load Player Potion Data.
		plugin.mPotionManager.loadFromJsonObject(player, object);

		/*
		 * If there was no transferred data, need to load potion data saved to their character
		 * into the potion manager
		 */
		if (object == null) { // TODO MANGLE
			plugin.mPotionManager.loadFromPlayer(player);
		}

		// Now that the player is all set up, refresh their abilities based on their class
		AbilityManager.getManager().updatePlayerAbilities(player);
	}
}
