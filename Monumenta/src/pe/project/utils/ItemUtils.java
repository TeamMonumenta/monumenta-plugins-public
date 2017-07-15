package pe.project.utils;

import java.util.Arrays;
import java.util.List;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import pe.project.utils.PotionUtils.PotionInfo;

public class ItemUtils {
	public static List<Material> interactables = Arrays.asList(
            Material.ACACIA_DOOR,
            Material.ACACIA_FENCE_GATE,
            Material.ANVIL,
            Material.BEACON,
            Material.BED,
            Material.BIRCH_DOOR,
            Material.BIRCH_FENCE_GATE,
            Material.BOAT,
            Material.BOAT_ACACIA,
            Material.BOAT_BIRCH,
            Material.BOAT_DARK_OAK,
            Material.BOAT_JUNGLE,
            Material.BOAT_SPRUCE,
            Material.BREWING_STAND,
            Material.COMMAND,
            Material.CHEST,
            Material.DARK_OAK_DOOR,
            Material.DARK_OAK_FENCE_GATE,
            Material.DAYLIGHT_DETECTOR,
            Material.DAYLIGHT_DETECTOR_INVERTED,
            Material.DISPENSER,
            Material.DROPPER,
            Material.ENCHANTMENT_TABLE,
            Material.ENDER_CHEST,
            Material.FENCE_GATE,
            Material.FURNACE,
            Material.HOPPER,
            Material.HOPPER_MINECART,
            Material.ITEM_FRAME,
            Material.JUNGLE_DOOR,
            Material.JUNGLE_FENCE_GATE,
            Material.LEVER,
            Material.MINECART,
            Material.NOTE_BLOCK,
            Material.POWERED_MINECART,
            Material.REDSTONE_COMPARATOR,
            Material.REDSTONE_COMPARATOR_OFF,
            Material.REDSTONE_COMPARATOR_ON,
            Material.SIGN,
            Material.SIGN_POST,
            Material.STONE_BUTTON,
            Material.STORAGE_MINECART,
            Material.TRAP_DOOR,
            Material.TRAPPED_CHEST,
            Material.WALL_SIGN,
            Material.WOOD_BUTTON,
            Material.WOOD_DOOR,
            Material.WORKBENCH
	);
	
	public static ItemStack createTippedArrows(PotionType type, int amount, PotionData data) {
		ItemStack stack = new ItemStack(Material.TIPPED_ARROW, amount);
		
		PotionMeta meta = (PotionMeta)stack.getItemMeta();
		meta.setBasePotionData(data);
		stack.setItemMeta(meta);
		
		return stack;
	}
	
	public static ItemStack createStackedPotions(PotionEffectType type, int amount, int duration, int amplifier, String name) {
		ItemStack stack = new ItemStack(Material.SPLASH_POTION, amount);
		
		PotionMeta meta = (PotionMeta)stack.getItemMeta();

		meta.setDisplayName("§r" + name);
		meta.addCustomEffect(new PotionEffect(type, duration, amplifier), true);
		meta.setColor(type.getColor());
		stack.setItemMeta(meta);
		
		return stack;
	}
	
	public static void addPotionEffect(ItemStack potion, PotionInfo info) {
		PotionMeta meta = (PotionMeta)potion.getItemMeta();
		meta.addCustomEffect(new PotionEffect(info.type, info.duration, info.amplifier, false, true), false);
		potion.setItemMeta(meta);
	}
	
	public static void setPotionMeta(ItemStack potion, String name, Color color) {
		PotionMeta meta = (PotionMeta)potion.getItemMeta();
		meta.setDisplayName("§r" + name);
		meta.setColor(color);
		potion.setItemMeta(meta);
	}
	
	public static PotionMeta getPotionMeta(ItemStack potion) {
		Material type = potion.getType();
		if (type == Material.POTION || type == Material.SPLASH_POTION || type == Material.LINGERING_POTION) {
			PotionMeta meta = (PotionMeta)potion.getItemMeta();
			return meta;
		}
		
		return null;
	}
	
	public static boolean isInteractable(Material mat) {
		for (Material material : interactables) {
			if (material.equals(mat)) {
				return true;
			}
		}
		
		return false;
	}
	
	public static boolean isBoat(Material item) {
		return item == Material.BOAT || item == Material.BOAT_ACACIA || item == Material.BOAT_BIRCH ||
				item == Material.BOAT_DARK_OAK || item == Material.BOAT_JUNGLE || item == Material.BOAT_SPRUCE;
	}
}
