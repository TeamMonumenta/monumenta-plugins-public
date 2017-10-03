package pe.project.playerdata;

import java.util.UUID;
import java.io.File;

import org.bukkit.ChatColor;
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
import pe.project.utils.NetworkUtils;

public class PlayerData {
	static public String savePlayerData(Main main, UUID playerUUID, String writeContent) {
		final String fileLocation = main.getDataFolder() + File.separator + "players" + File.separator + playerUUID + ".json";

		try {
			if (FileUtils.getCreateFile(fileLocation) != null) {
				FileUtils.writeFile(fileLocation, writeContent);

				return writeContent;
			}
		} catch (Exception e) {
			main.getLogger().severe("Failed to write player data to " + fileLocation);
		}

		return "";
	}

	static public String serializePlayerData(Main main, Player player) {
		return savePlayerData(main, player.getUniqueId(), convertToString(main, player));
	}

	static public String convertToString(Main main, Player player) {
		try {
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
		} catch (Exception e) {
			main.getLogger().severe("Caught exception: " + e);
			e.printStackTrace();
		}

		return "";
	}

	static public void deserializePlayerData(Main main, Player player) {
		final String fileLocation = main.getDataFolder() + File.separator + "players" + File.separator + player.getUniqueId() + ".json";

		try {
			String content = FileUtils.getCreateFile(fileLocation);
			if (content != null && content != "") {
				loadFromString(main, player, content);
			}
		} catch (Exception e) {
			main.getLogger().severe("Caught exception: " + e);
			e.printStackTrace();
		}
	}

	static public void loadFromString(Main main, Player player, String content) {
		if (content != null && content != "") {
			try {
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

			} catch (Exception e) {
				main.getLogger().severe("Caught exception: " + e);
				e.printStackTrace();

				final String fileLocation = main.getDataFolder() + File.separator + "broken_players" + File.separator + player.getUniqueId() + ".json";

				try {
					if (FileUtils.getCreateFile(fileLocation) != null) {
						FileUtils.writeFile(fileLocation, content);
					}
				} catch (Exception ex) {
					main.getLogger().severe("Failed to write player data to " + fileLocation);
				}

				player.sendMessage(ChatColor.RED + "Something very bad happened while transferring your player data.");
				player.sendMessage(ChatColor.RED + "As a precaution, the server has attempted to move you to Purgatory.");
				player.sendMessage(ChatColor.RED + "If for some reason you aren't on purgatory, take a screenshot and log off.");
				player.sendMessage(ChatColor.RED + "Please post in #moderator-help and tag @admin");
				player.sendMessage(ChatColor.RED + "Include details about what you were doing");
				player.sendMessage(ChatColor.RED + "such as joining or leaving a dungeon (and which one!)");

				NetworkUtils.sendPlayer(main, player, "purgatory");
			}
		}
	}
}
