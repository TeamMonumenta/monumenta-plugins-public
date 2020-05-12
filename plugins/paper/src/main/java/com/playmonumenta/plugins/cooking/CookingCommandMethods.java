package com.playmonumenta.plugins.cooking;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

class CookingCommandMethods {

	static void runOpenTable(CommandSender sender, Plugin plugin) {
		if (!(sender instanceof Player)) {
			return;
		}
		plugin.mCookingTableInventoryManager.openTable((Player)sender);
	}

	static void runCreateNewItem(CommandSender sender) {
		Player p = CommandUtils.getPlayerFromSender(sender);
		if (p == null) {
			return;
		}

		if (p.getInventory().getItemInMainHand().getType() != Material.AIR) {
			p.sendMessage("You must be empty-handed to use this command");
			return;
		}
		new CookingItemObject().setAsMainhandItem(p);
	}

	static void runShowJson(CommandSender sender) {
		Player p = CommandUtils.getPlayerFromSender(sender);
		if (p == null) {
			return;
		}
		String json = CookingUtils.extractItemDataFromFirstLoreLine(p.getInventory().getItemInMainHand());
		if (json == null) {
			p.sendMessage("held item is not detected as a cooking item");
			return;
		}
		p.sendMessage(json);
	}

	static void runUpdate(CommandSender sender) {
		Player p = CommandUtils.getPlayerFromSender(sender);
		if (p == null) {
			return;
		}
		String json = CookingUtils.extractItemDataFromFirstLoreLine(p.getInventory().getItemInMainHand());
		if (json == null) {
			p.sendMessage("held item is not detected as a cooking item");
			return;
		}
		CookingItemObject item = CookingUtils.cookingItemObjectFromJson(json);
		item.setAsMainhandItem(p);
	}

	static void runVarMaterial(CommandSender sender, String materialStr) {
		Player p = CommandUtils.getPlayerFromSender(sender);
		if (p == null) {
			return;
		}
		Material mat = Material.getMaterial(materialStr);
		if (mat == null) {
			p.sendMessage("Unknown Material_Name given.\nPlease refer to https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html for a complete material list");
		}
		String json = CookingUtils.extractItemDataFromFirstLoreLine(p.getInventory().getItemInMainHand());
		if (json == null) {
			p.sendMessage("held item is not detected as a cooking item");
			return;
		}
		CookingItemObject item = CookingUtils.cookingItemObjectFromJson(json);
		item.setMaterial(mat);
		item.setAsMainhandItem(p);
	}

	static void runVarName(CommandSender sender, String newName) {
		Player p = CommandUtils.getPlayerFromSender(sender);
		if (p == null) {
			return;
		}
		String json = CookingUtils.extractItemDataFromFirstLoreLine(p.getInventory().getItemInMainHand());
		if (json == null) {
			p.sendMessage("held item is not detected as a cooking item");
			return;
		}
		CookingItemObject item = CookingUtils.cookingItemObjectFromJson(json);
		item.setName(newName);
		item.setAsMainhandItem(p);
	}

	static void runVarType(CommandSender sender, String str) {
		Player p = CommandUtils.getPlayerFromSender(sender);
		if (p == null) {
			return;
		}
		String json = CookingUtils.extractItemDataFromFirstLoreLine(p.getInventory().getItemInMainHand());
		if (json == null) {
			p.sendMessage("held item is not detected as a cooking item");
			return;
		}
		CookingItemType type;
		try {
			type = CookingItemType.valueOf(str);
		} catch (IllegalArgumentException e) {
			p.sendMessage("Wrong type parameter given. Must be one of [base, tool, ingredient, meal]");
			return;
		}
		CookingItemObject item = CookingUtils.cookingItemObjectFromJson(json);
		item.setType(type);
		item.setAsMainhandItem(p);
	}

	static void runVarLore(CommandSender sender, String s) {
		Player p = CommandUtils.getPlayerFromSender(sender);
		if (p == null) {
			return;
		}
		String json = CookingUtils.extractItemDataFromFirstLoreLine(p.getInventory().getItemInMainHand());
		if (json == null) {
			p.sendMessage("held item is not detected as a cooking item");
			return;
		}
		CookingItemObject item = CookingUtils.cookingItemObjectFromJson(json);
		item.setLore(s.replace("\\n","\n"));
		item.setAsMainhandItem(p);
	}

	static void runVarTier(CommandSender sender, String region, String tier) {
		Player p = CommandUtils.getPlayerFromSender(sender);
		if (p == null) {
			return;
		}
		String json = CookingUtils.extractItemDataFromFirstLoreLine(p.getInventory().getItemInMainHand());
		if (json == null) {
			p.sendMessage("held item is not detected as a cooking item");
			return;
		}
		CookingItemObject item = CookingUtils.cookingItemObjectFromJson(json);
		try {
			item.setRegion(ItemUtils.ItemRegion.valueOf(region));
		} catch (IllegalArgumentException e) {
			p.sendMessage("Unknown Region " + region);
		}
		try {
			item.setTier(ItemUtils.ItemTier.valueOf(tier));
		} catch (IllegalArgumentException e) {
			p.sendMessage("Unknown Item Tier " + tier);
		}
		item.setAsMainhandItem(p);
	}

	static void runVarConsumeEffectsAdd(CommandSender sender, String key, int val) {
		Player p = CommandUtils.getPlayerFromSender(sender);
		if (p == null) {
			return;
		}
		String json = CookingUtils.extractItemDataFromFirstLoreLine(p.getInventory().getItemInMainHand());
		if (json == null) {
			p.sendMessage("held item is not detected as a cooking item");
			return;
		}
		CookingItemObject item = CookingUtils.cookingItemObjectFromJson(json);
		CookingEffectsEnum effect;
		try {
			effect = CookingEffectsEnum.valueOf(key);
		} catch (IllegalArgumentException e) {
			p.sendMessage("Unknown Effect " + key);
			return;
		}
		item.setConsumeEffect(effect, val);
		item.setAsMainhandItem(p);
	}

	static void runVarCookingEffectsAdd(CommandSender sender, String key, int val) {
		Player p = CommandUtils.getPlayerFromSender(sender);
		if (p == null) {
			return;
		}
		String json = CookingUtils.extractItemDataFromFirstLoreLine(p.getInventory().getItemInMainHand());
		if (json == null) {
			p.sendMessage("held item is not detected as a cooking item");
			return;
		}
		CookingItemObject item = CookingUtils.cookingItemObjectFromJson(json);
		CookingEffectsEnum effect;
		try {
			effect = CookingEffectsEnum.valueOf(key);
		} catch (IllegalArgumentException e) {
			p.sendMessage("Unknown Effect " + key);
			return;
		}
		item.setCookingEffect(effect, val);
		item.setAsMainhandItem(p);
	}

	static void runVarNameModifiersAdd(CommandSender sender, String val) {
		Player p = CommandUtils.getPlayerFromSender(sender);
		if (p == null) {
			return;
		}
		String json = CookingUtils.extractItemDataFromFirstLoreLine(p.getInventory().getItemInMainHand());
		if (json == null) {
			p.sendMessage("held item is not detected as a cooking item");
			return;
		}
		CookingItemObject item = CookingUtils.cookingItemObjectFromJson(json);
		item.addNameModifier(val);
		item.setAsMainhandItem(p);
	}

	static void runVarNameModifiersRemove(CommandSender sender, String val) {
		Player p = CommandUtils.getPlayerFromSender(sender);
		if (p == null) {
			return;
		}
		String json = CookingUtils.extractItemDataFromFirstLoreLine(p.getInventory().getItemInMainHand());
		if (json == null) {
			p.sendMessage("held item is not detected as a cooking item");
			return;
		}
		CookingItemObject item = CookingUtils.cookingItemObjectFromJson(json);
		item.removeNameModifier(val);
		item.setAsMainhandItem(p);
	}
}
