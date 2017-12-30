package pe.project.utils;

import java.util.EnumSet;

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
	public static EnumSet<Material> interactables = EnumSet.of(
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

	public static EnumSet<Material> armors = EnumSet.of(
            Material.LEATHER_BOOTS,
            Material.LEATHER_CHESTPLATE,
            Material.LEATHER_HELMET,
            Material.LEATHER_LEGGINGS,

            Material.CHAINMAIL_BOOTS,
            Material.CHAINMAIL_CHESTPLATE,
            Material.CHAINMAIL_HELMET,
            Material.CHAINMAIL_LEGGINGS,

            Material.GOLD_BOOTS,
            Material.GOLD_CHESTPLATE,
            Material.GOLD_HELMET,
            Material.GOLD_LEGGINGS,

            Material.IRON_BOOTS,
            Material.IRON_CHESTPLATE,
            Material.IRON_HELMET,
            Material.IRON_LEGGINGS,

            Material.DIAMOND_BOOTS,
            Material.DIAMOND_CHESTPLATE,
            Material.DIAMOND_HELMET,
            Material.DIAMOND_LEGGINGS
	);

	public static EnumSet<Material> doors = EnumSet.of(
			Material.ACACIA_DOOR,
			Material.BIRCH_DOOR,
			Material.DARK_OAK_DOOR,
			Material.IRON_DOOR,
			Material.JUNGLE_DOOR,
			Material.SPRUCE_DOOR,
			Material.WOOD_DOOR
	);

	// List of materials that trees can replace when they grow
	public static EnumSet<Material> allowedTreeReplaceMaterials = EnumSet.of(
			Material.AIR,
			Material.LEAVES,
			Material.LEAVES_2,
			Material.SAPLING,
			Material.VINE
	);

	/**
	 * Items drop if they have lore that does not contain $$$
	 */
	public static float getItemDropChance(ItemStack item) {
		if (item != null && (item.hasItemMeta() && item.getItemMeta().hasLore()) && !InventoryUtils.testForItemWithLore(item, "$$$")) {
			return 1.0f;
		} else {
			return -200.0f;
		}
	}

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
		return interactables.contains(mat);
	}

	public static boolean isArmorItem(Material item) {
		return armors.contains(item);
	}

	public static boolean isDoor(Material item) {
		return doors.contains(item);
	}

	public static boolean isAllowedTreeReplace(Material item) {
		return allowedTreeReplaceMaterials.contains(item);
	}
}
