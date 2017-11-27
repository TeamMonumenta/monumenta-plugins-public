package pe.project.protection.stopbeingshitty;

import org.bukkit.ChatColor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

public class StopBeingShitty {
	private static List<Material> contraband = Arrays.asList(
            Material.IRON_INGOT,
            Material.IRON_ORE,
            Material.IRON_BLOCK,
            Material.IRON_HELMET,
            Material.IRON_CHESTPLATE,
            Material.IRON_LEGGINGS,
            Material.IRON_BOOTS,
            Material.IRON_AXE,
            Material.IRON_PICKAXE,
            Material.IRON_SWORD,
            Material.DIAMOND,
            Material.DIAMOND_ORE,
            Material.DIAMOND_BLOCK,
            Material.DIAMOND_AXE,
            Material.DIAMOND_PICKAXE,
            Material.DIAMOND_SWORD,
            Material.DIAMOND_HELMET,
            Material.DIAMOND_CHESTPLATE,
            Material.DIAMOND_LEGGINGS,
            Material.DIAMOND_BOOTS,
            Material.EMERALD,
            Material.EMERALD_ORE,
            Material.EMERALD_BLOCK,
            Material.ENDER_CHEST,
            Material.ANVIL,
            Material.BEACON,
            Material.ELYTRA,
            Material.ENCHANTMENT_TABLE
	);

	static Map<String, Integer> mSketchyEnchants = new HashMap<String, Integer>();
	static Map<String, Integer> mShittyEnchants = new HashMap<String, Integer>();
	static {
		mSketchyEnchants.put("DAMAGE_ALL", 3);
		mSketchyEnchants.put("DAMAGE_UNDEAD", 3);
		mSketchyEnchants.put("ARROW_DAMAGE", 3);
		mSketchyEnchants.put("DURABILITY", 3);

		mShittyEnchants.put("DAMAGE_ALL", 5);
		mShittyEnchants.put("DAMAGE_UNDEAD", 5);
		mShittyEnchants.put("ARROW_DAMAGE", 4);
	}

	public static void chanceOfBeingShitty(Player player, Player commandPlayer) {
    	String shittyMessage = "Chance of %s being shitty - %.2f percent chance %s";
    	List<String> shittyAllInfo = new ArrayList<String>();
    	int slots = 0;
    	int shittySlots = 0;
    	boolean absolutelyShitty = false;
    	String sketchyInfo = "Sketchy Item info - ";
    	String shittyInfo = "Shitty Item info - ";

    	//	Loop through a players inventory...
    	PlayerInventory inventory = commandPlayer.getInventory();
    	Iterator<ItemStack> inventoryIter = inventory.iterator();
    	while (inventoryIter.hasNext()) {
    		int slot = 0;
    		ItemStack item = inventoryIter.next();
    		if (item != null) {
    			String isAbsolutelyShity = isShitty(item, shittyInfo);
    			if (isAbsolutelyShity != null) {
    				shittyAllInfo.add(isAbsolutelyShity);
    				absolutelyShitty = true;
    				slot = 1;
    			} else {
    				String isSketchy = isSketchy(item, sketchyInfo);
    				if (isSketchy != null) {
    					shittyAllInfo.add(isSketchy + " [Inventory]");
    					slot = 1;
    				}
    			}

    			String contraband = isContraband(item);
				if (contraband != null) {
					shittyAllInfo.add(contraband + " [Inventory]");
					absolutelyShitty = true;
					slot = 1;
				}
    		}

    		shittySlots += slot;
    	}

    	slots += inventory.getSize();

    	//	Ender Chest
    	Inventory enderChest = commandPlayer.getEnderChest();
    	Iterator<ItemStack> enderIter = enderChest.iterator();
    	while (enderIter.hasNext()) {
    		int slot = 0;
    		ItemStack item = enderIter.next();
    		if (item != null) {
    			String isAbsolutelyShity = isShitty(item, shittyInfo);
    			if (isAbsolutelyShity != null) {
    				shittyAllInfo.add(isAbsolutelyShity);
    				absolutelyShitty = true;
    				slot = 1;
    			} else {
    				String isSketchy = isSketchy(item, sketchyInfo);
    				if (isSketchy != null) {
    					shittyAllInfo.add(isSketchy + " [Ender Chest]");
    					slot = 1;
    				}
    			}

    			String contraband = isContraband(item);
				if (contraband != null) {
					shittyAllInfo.add(contraband + " [Ender Chest]");
					absolutelyShitty = true;
					slot = 1;
				}
    		}

    		shittySlots += slot;
    	}

    	slots += enderChest.getSize();

    	float chance = (!absolutelyShitty) ? ((float)shittySlots / (float)slots) * 100 : 100;
    	player.sendMessage(ChatColor.LIGHT_PURPLE + String.format(shittyMessage, commandPlayer.getName(), chance, "(" + shittySlots + "/" + slots + ")"));
    	for (String info : shittyAllInfo) {
    		player.sendMessage(info);
    	}
    }

	static String isSketchy(ItemStack stack, String info) {
		String sketchyInfo = ChatColor.YELLOW + info;
		boolean sketchy = false;

		ItemMeta meta = stack.getItemMeta();
		if (meta != null) {
			if (meta.hasDisplayName()) {
				sketchyInfo += meta.getDisplayName() + ChatColor.RESET;
			} else {
				sketchyInfo += stack.getType().toString().replace("_", " ").toLowerCase() + ChatColor.RESET;
			}
		}

		//	Sketchy Enchants.
		Iterator<Entry<String, Integer>> sketchyIter = mSketchyEnchants.entrySet().iterator();
		while (sketchyIter.hasNext()) {
			Entry<String, Integer> entry = sketchyIter.next();
			Enchantment enchant = Enchantment.getByName(entry.getKey());
			if (enchant != null) {
				int level = stack.getEnchantmentLevel(enchant);
				if (level >= entry.getValue()) {
					sketchyInfo += ", " + entry.getKey() + " " + level;
					sketchy = true;
				}
			}
		}

		return sketchy ? sketchyInfo : null;
	}

	static String isShitty(ItemStack stack, String info) {
		String shittyInfo = ChatColor.RED + info;
		boolean shitty = false;

		ItemMeta meta = stack.getItemMeta();
		if (meta != null) {
			String displayName = meta.getDisplayName();
			if (displayName != null && !displayName.isEmpty()) {
				shittyInfo += displayName + ChatColor.RESET;
			} else {
				shittyInfo += stack.getType().toString().replace("_", " ").toLowerCase() + ChatColor.RESET;
			}
		}

		//	Sketchy Enchants.
		Iterator<Entry<String, Integer>> sketchyIter = mShittyEnchants.entrySet().iterator();
		while (sketchyIter.hasNext()) {
			Entry<String, Integer> entry = sketchyIter.next();
			Enchantment enchant = Enchantment.getByName(entry.getKey());
			if (enchant != null) {
				int level = stack.getEnchantmentLevel(enchant);
				if (level >= entry.getValue()) {
					shittyInfo += ", " + entry.getKey() + " " + level;
					shitty = true;
				}
			}
		}

		return shitty ? shittyInfo : null;
	}

	static String isContraband(ItemStack item) {
		Material mat = item.getType();

		for (Material material : contraband) {
			if (material.equals(mat)) {
				return ChatColor.RED + "Contraband Item - " + item.getType().toString().replace("_", " ").toLowerCase() + ChatColor.RESET;
			}
		}

		return null;
	}
}
