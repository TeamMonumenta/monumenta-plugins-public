package pe.project.protection.stopbeingshitty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;

public class StopBeingShitty {
	static Map<String, Integer> mSketchyEnchants = new HashMap<String, Integer>();
	static Map<String, Integer> mShittyEnchants = new HashMap<String, Integer>();
	static {
		mSketchyEnchants.put("sharpness", 3);
		mSketchyEnchants.put("smite", 3);
		mSketchyEnchants.put("power", 3);
		mSketchyEnchants.put("unbreaking", 3);
		
		mShittyEnchants.put("sharpness", 5);
		mShittyEnchants.put("smite", 5);
		mShittyEnchants.put("power", 4);
	}
	
	public static void chanceOfBeingShitty(Player player, Player commandPlayer) {
    	String shittyMessage = "Chance of %s being shitty - %.2f percent chance";
    	List<String> shittyAllInfo = new ArrayList<String>();
    	int slots = 58;
    	int shittySlots = 0;
    	boolean absolutelyShitty = false;
    	
    	//	Loop through a players inventory...
    	PlayerInventory inventory = commandPlayer.getInventory();
    	Iterator<ItemStack> inventoryIter = inventory.iterator();
    	while (inventoryIter.hasNext()) {
    		ItemStack item = inventoryIter.next();
    		if (item != null) {
    			ItemMeta meta = item.getItemMeta();
	    		String shittyInfo = "Shitty Item info - Name: " + item.getType().toString();
	    		boolean isShitty = false;
	    		
	    		if (meta != null) {
	    			String displayName = meta.getDisplayName();
	    			if (displayName != null && !displayName.isEmpty()) {
	    				shittyInfo += " - \"" + displayName + "\"";
	    			}
	    		}
	    		
	    		//	Shitty Enchants.
	    		Iterator<Entry<String, Integer>> shittyIter = mShittyEnchants.entrySet().iterator();
	    		while (shittyIter.hasNext()) {
	    			Entry<String, Integer> entry = shittyIter.next();
	    			Enchantment enchant = Enchantment.getByName(entry.getKey());
	    			if (enchant != null) {
	    				int level = item.getEnchantmentLevel(enchant);
	    				if (level >= entry.getValue()) {
	    					shittyInfo += ", " + entry.getKey() + " " + level;
	    					isShitty = true;
	    					absolutelyShitty = true;
	    				}
	    			}
	    		}
	    		
	    		if (!isShitty) {
		    		//	Sketchy Enchants.
		    		Iterator<Entry<String, Integer>> sketchyIter = mSketchyEnchants.entrySet().iterator();
		    		while (sketchyIter.hasNext()) {
		    			Entry<String, Integer> entry = sketchyIter.next();
		    			Enchantment enchant = Enchantment.getByName(entry.getKey());
		    			if (enchant != null) {
		    				int level = item.getEnchantmentLevel(enchant);
		    				if (level >= entry.getValue()) {
		    					shittyInfo += ", " + entry.getKey() + " " + level;
		    					isShitty = true;
		    				}
		    			}
		    		}
	    		}
	    		
	    		if (meta != null) {
	    			//	Unbreaking
	    			if (meta.isUnbreakable()) {
	    				shittyInfo += ", Unbreakable";
	        			isShitty = true;
	        			absolutelyShitty = true;
	    			}
	    		}
	    		
	    		if (isShitty) {
	    			shittyAllInfo.add(shittyInfo);
	    			shittySlots++;
	    		}
    		}
    	}
    	
    	//	Loop through a players Ender Chest...
    	//	TODO
    	
    	float chance = (!absolutelyShitty) ? ((float)shittySlots / (float)slots) * 100 : 100;
    	player.sendMessage(String.format(shittyMessage, commandPlayer.getName(), chance));
    	for (String info : shittyAllInfo) {
    		player.sendMessage(info);
    	}
    }
}
