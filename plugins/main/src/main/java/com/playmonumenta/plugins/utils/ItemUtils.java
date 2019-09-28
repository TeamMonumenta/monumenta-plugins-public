package com.playmonumenta.plugins.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionData;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.PotionUtils.PotionInfo;

import net.md_5.bungee.api.ChatColor;

public class ItemUtils {
	public static final Set<Material> armors = EnumSet.of(
		Material.LEATHER_BOOTS,
		Material.LEATHER_CHESTPLATE,
		Material.LEATHER_HELMET,
		Material.LEATHER_LEGGINGS,

		Material.CHAINMAIL_BOOTS,
		Material.CHAINMAIL_CHESTPLATE,
		Material.CHAINMAIL_HELMET,
		Material.CHAINMAIL_LEGGINGS,

		Material.GOLDEN_BOOTS,
		Material.GOLDEN_CHESTPLATE,
		Material.GOLDEN_HELMET,
		Material.GOLDEN_LEGGINGS,

		Material.IRON_BOOTS,
		Material.IRON_CHESTPLATE,
		Material.IRON_HELMET,
		Material.IRON_LEGGINGS,

		Material.DIAMOND_BOOTS,
		Material.DIAMOND_CHESTPLATE,
		Material.DIAMOND_HELMET,
		Material.DIAMOND_LEGGINGS
	);

	public static final Set<Material> wearable = EnumSet.of(
		Material.LEATHER_BOOTS,
		Material.LEATHER_CHESTPLATE,
		Material.LEATHER_HELMET,
		Material.LEATHER_LEGGINGS,

		Material.CHAINMAIL_BOOTS,
		Material.CHAINMAIL_CHESTPLATE,
		Material.CHAINMAIL_HELMET,
		Material.CHAINMAIL_LEGGINGS,

		Material.GOLDEN_BOOTS,
		Material.GOLDEN_CHESTPLATE,
		Material.GOLDEN_HELMET,
		Material.GOLDEN_LEGGINGS,

		Material.IRON_BOOTS,
		Material.IRON_CHESTPLATE,
		Material.IRON_HELMET,
		Material.IRON_LEGGINGS,

		Material.DIAMOND_BOOTS,
		Material.DIAMOND_CHESTPLATE,
		Material.DIAMOND_HELMET,
		Material.DIAMOND_LEGGINGS,

		Material.PUMPKIN,
		Material.CREEPER_HEAD,
		Material.SKELETON_SKULL,
		Material.WITHER_SKELETON_SKULL,
		Material.ZOMBIE_HEAD,
		Material.PLAYER_HEAD
	);

	// List of materials that trees can replace when they grow
	public static final Set<Material> allowedTreeReplaceMaterials = EnumSet.of(
		Material.AIR,
		Material.OAK_LEAVES,
		Material.SPRUCE_LEAVES,
		Material.BIRCH_LEAVES,
		Material.JUNGLE_LEAVES,
		Material.ACACIA_LEAVES,
		Material.DARK_OAK_LEAVES,
		Material.OAK_SAPLING,
		Material.ACACIA_SAPLING,
		Material.BIRCH_SAPLING,
		Material.DARK_OAK_SAPLING,
		Material.JUNGLE_SAPLING,
		Material.SPRUCE_SAPLING,
		Material.VINE
	);

	// List of blocks that can be interacted with using right click and generally perform some functionality
		public static final Set<Material> interactableBlocks = EnumSet.of(
	            Material.ACACIA_BUTTON,
	            Material.ACACIA_DOOR,
	            Material.ACACIA_FENCE_GATE,
	            Material.ACACIA_TRAPDOOR,
	            Material.ANVIL,
	            Material.BEACON,
	            Material.BIRCH_BUTTON,
	            Material.BIRCH_DOOR,
	            Material.BIRCH_FENCE_GATE,
	            Material.BIRCH_TRAPDOOR,
	            Material.BLACK_BED,
	            Material.BLACK_SHULKER_BOX,
	            Material.BLUE_BED,
	            Material.BLUE_SHULKER_BOX,
	            Material.BREWING_STAND,
	            Material.BROWN_BED,
	            Material.BROWN_SHULKER_BOX,
	            Material.CAKE,
	            Material.CAULDRON,
	            Material.CHAIN_COMMAND_BLOCK,
	            Material.CHEST,
	            Material.CHIPPED_ANVIL,
	            Material.COMMAND_BLOCK,
	            Material.COMPARATOR,
	            Material.CRAFTING_TABLE,
	            Material.CYAN_BED,
	            Material.CYAN_SHULKER_BOX,
	            Material.DAMAGED_ANVIL,
	            Material.DARK_OAK_BUTTON,
	            Material.DARK_OAK_DOOR,
	            Material.DARK_OAK_FENCE_GATE,
	            Material.DARK_OAK_TRAPDOOR,
	            Material.DISPENSER,
	            Material.DRAGON_EGG,
	            Material.DROPPER,
	            Material.ENCHANTING_TABLE,
	            Material.ENDER_CHEST,
	            Material.FLOWER_POT,
	            Material.FURNACE,
	            Material.GRAY_BED,
	            Material.GRAY_SHULKER_BOX,
	            Material.GREEN_BED,
	            Material.GREEN_SHULKER_BOX,
	            Material.HOPPER,
	            Material.JUKEBOX,
	            Material.JUNGLE_BUTTON,
	            Material.JUNGLE_DOOR,
	            Material.JUNGLE_FENCE_GATE,
	            Material.JUNGLE_TRAPDOOR,
	            Material.LEVER,
	            Material.LIGHT_BLUE_BED,
	            Material.LIGHT_BLUE_SHULKER_BOX,
	            Material.LIGHT_GRAY_BED,
	            Material.LIGHT_GRAY_SHULKER_BOX,
	            Material.LIME_BED,
	            Material.LIME_SHULKER_BOX,
	            Material.MAGENTA_BED,
	            Material.MAGENTA_SHULKER_BOX,
	            Material.NOTE_BLOCK,
	            Material.OAK_BUTTON,
	            Material.OAK_DOOR,
	            Material.OAK_FENCE_GATE,
	            Material.OAK_TRAPDOOR,
	            Material.ORANGE_BED,
	            Material.ORANGE_SHULKER_BOX,
	            Material.PINK_BED,
	            Material.PINK_SHULKER_BOX,
	            Material.POTTED_ACACIA_SAPLING,
	            Material.POTTED_ALLIUM,
	            Material.POTTED_AZURE_BLUET,
	            Material.POTTED_BIRCH_SAPLING,
	            Material.POTTED_BLUE_ORCHID,
	            Material.POTTED_BROWN_MUSHROOM,
	            Material.POTTED_CACTUS,
	            Material.POTTED_DANDELION,
	            Material.POTTED_DARK_OAK_SAPLING,
	            Material.POTTED_DEAD_BUSH,
	            Material.POTTED_FERN,
	            Material.POTTED_JUNGLE_SAPLING,
	            Material.POTTED_OAK_SAPLING,
	            Material.POTTED_ORANGE_TULIP,
	            Material.POTTED_OXEYE_DAISY,
	            Material.POTTED_PINK_TULIP,
	            Material.POTTED_POPPY,
	            Material.POTTED_RED_MUSHROOM,
	            Material.POTTED_RED_TULIP,
	            Material.POTTED_SPRUCE_SAPLING,
	            Material.POTTED_WHITE_TULIP,
	            Material.PUMPKIN,
	            Material.PURPLE_BED,
	            Material.PURPLE_SHULKER_BOX,
	            Material.RED_BED,
	            Material.RED_SHULKER_BOX,
	            Material.REPEATER,
	            Material.REPEATING_COMMAND_BLOCK,
	            Material.SHULKER_BOX,
	            Material.SPRUCE_BUTTON,
	            Material.SPRUCE_DOOR,
	            Material.SPRUCE_FENCE_GATE,
	            Material.SPRUCE_TRAPDOOR,
	            Material.STONE_BUTTON,
	            Material.STRUCTURE_BLOCK,
	            Material.TNT,
	            Material.TRAPPED_CHEST,
	            Material.WHITE_BED,
	            Material.WHITE_SHULKER_BOX,
	            Material.YELLOW_BED,
	            Material.YELLOW_SHULKER_BOX
	    );

	public static final Set<Material> shulkerBoxes = EnumSet.of(
		Material.SHULKER_BOX,
		Material.WHITE_SHULKER_BOX,
		Material.ORANGE_SHULKER_BOX,
		Material.MAGENTA_SHULKER_BOX,
		Material.LIGHT_BLUE_SHULKER_BOX,
		Material.YELLOW_SHULKER_BOX,
		Material.LIME_SHULKER_BOX,
		Material.PINK_SHULKER_BOX,
		Material.GRAY_SHULKER_BOX,
		Material.LIGHT_GRAY_SHULKER_BOX,
		Material.CYAN_SHULKER_BOX,
		Material.PURPLE_SHULKER_BOX,
		Material.BLUE_SHULKER_BOX,
		Material.BROWN_SHULKER_BOX,
		Material.GREEN_SHULKER_BOX,
		Material.RED_SHULKER_BOX,
		Material.BLACK_SHULKER_BOX
	);

	public enum ItemRegion {
		UNKNOWN,
		KINGS_VALLEY,
		CELSIAN_ISLES,
		MONUMENTA
	}

	public enum ItemTier {
		UNKNOWN,
		ONE,
		TWO,
		THREE,
		FOUR,
		FIVE,
		UNCOMMON,
		ENHANCED_UNCOMMON,
		PATRON_MADE,
		RARE,
		ENHANCED_RARE,
		ARTIFACT,
		RELIC,
		EPIC,
		UNIQUE,
		UNIQUE_EVENT,
		SHULKER_BOX,
		QUEST_COMPASS
	}

	public enum ItemDeathResult {
		KEEP, // Item is kept in inventory on death, takes no damage
		KEEP_DAMAGED, // Item is kept on death, with a durability loss
		KEEP_EQUIPPED, // Item is kept on death if it's in an armor/offhand/hotbar slot, with durability loss
		LOSE, // Item is dropped on death, lost when destroyed
		SAFE, // Item is dropped on death, placed in grave when destroyed, does not shatter
		SHATTER, // Item is dropped on death, placed in grave when destroyed, does shatter
		DESTROY, // Item is destroyed on death, like Curse of Vanishing
	}

	public static ItemRegion getItemRegion(ItemStack item) {
		if (item != null) {
			if (isShulkerBox(item.getType())) {
				return ItemRegion.MONUMENTA;
			}
			if (item.hasItemMeta()) {
				ItemMeta meta = item.getItemMeta();
				if (meta.hasDisplayName()) {
					String name = meta.getDisplayName();
					if (name.contains("Quest Compass")) {
						return ItemRegion.MONUMENTA;
					} else if (name.contains("Experiencinator")) {
						return ItemRegion.KINGS_VALLEY;
					} else if (name.contains("Crystallizer")) {
						return ItemRegion.CELSIAN_ISLES;
					}
				}
				if (meta.hasLore()) {
					List<String> lore = item.getLore();
					for (String loreEntry : lore) {
						String stripped = ChatColor.stripColor(loreEntry);
						if (stripped.startsWith("King's Valley :")) {
							return ItemRegion.KINGS_VALLEY;
						} else if (stripped.startsWith("Celsian Isles :")) {
							return ItemRegion.CELSIAN_ISLES;
						} else if (stripped.startsWith("Monumenta :")) {
							return ItemRegion.MONUMENTA;
						}
					}
				}
			}
		}
		return ItemRegion.UNKNOWN;
	}

	public static ItemTier getItemTier(ItemStack item) {
		if (item == null) {
			return ItemTier.UNKNOWN;
		}

		if (isShulkerBox(item.getType())) {
			return ItemTier.SHULKER_BOX;
		} else if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
			String itemName = item.getItemMeta().getDisplayName();
			if (itemName.contains("Quest Compass")) {
				return ItemTier.QUEST_COMPASS;
			} else if (itemName.contains("Experiencinator (u)")) {
				return ItemTier.ENHANCED_RARE;
			} else if (itemName.contains("Experiencinator")) {
				return ItemTier.RARE;
			} else if (itemName.contains("Perfect Crystallizer")) {
				return ItemTier.EPIC;
			} else if (itemName.contains("Crystallizer (u)")) {
				return ItemTier.ENHANCED_RARE;
			} else if (itemName.contains("Crystallizer")) {
				return ItemTier.ENHANCED_RARE;
			}
		}

		if (item.getLore() == null) {
			return ItemTier.UNKNOWN;
		}

		List<String> lore = item.getLore();
		for (String loreEntry : lore) {
			String stripped = ChatColor.stripColor(loreEntry);
			if (stripped.endsWith(": Tier I")) {
				return ItemTier.ONE;
			} else if (stripped.endsWith(": Tier II")) {
				return ItemTier.TWO;
			} else if (stripped.endsWith(": Tier II")) {
				return ItemTier.TWO;
			} else if (stripped.endsWith(": Tier III")) {
				return ItemTier.THREE;
			} else if (stripped.endsWith(": Tier IV")) {
				return ItemTier.FOUR;
			} else if (stripped.endsWith(": Tier V")) {
				return ItemTier.FIVE;
			} else if (stripped.endsWith(": Uncommon")) {
				return ItemTier.UNCOMMON;
			} else if (stripped.endsWith(": Enhanced Uncommon")) {
				return ItemTier.ENHANCED_UNCOMMON;
			} else if (stripped.endsWith(": Patron Made")) {
				return ItemTier.PATRON_MADE;
			} else if (stripped.endsWith(": Rare")) {
				return ItemTier.RARE;
			} else if (stripped.endsWith(": Enhanced Rare")) {
				return ItemTier.ENHANCED_RARE;
			} else if (stripped.endsWith(": Artifact")) {
				return ItemTier.ARTIFACT;
			} else if (stripped.endsWith(": Relic")) {
				return ItemTier.RELIC;
			} else if (stripped.endsWith(": Epic")) {
				return ItemTier.EPIC;
			} else if (stripped.endsWith(": Unique")) {
				return ItemTier.UNIQUE;
			} else if (stripped.endsWith(": Unique Event")) {
				return ItemTier.UNIQUE_EVENT;
			}
		}
		return ItemTier.UNKNOWN;
	}

	// Returns an ItemDeathResult reporting what should happen to an item when the player carrying it dies.
	public static ItemDeathResult getItemDeathResult(ItemStack item) {
		if (item.containsEnchantment(Enchantment.VANISHING_CURSE)) {
			return ItemDeathResult.DESTROY;
		} else if (item.containsEnchantment(Enchantment.BINDING_CURSE)) {
			return ItemDeathResult.LOSE;
		}
		switch (getItemRegion(item)) {
		case KINGS_VALLEY:
		case CELSIAN_ISLES:
		case MONUMENTA:
			switch (getItemTier(item)) {
			case ONE:
			case TWO:
			case THREE:
				if (Plugin.getInstance().mServerProperties.getKeepLowTierInventory()) {
					return ItemDeathResult.KEEP_EQUIPPED;
				} else {
					return ItemDeathResult.LOSE;
				}
			case FOUR:
			case FIVE:
			case UNCOMMON:
			case ENHANCED_UNCOMMON:
			case PATRON_MADE:
			case RARE:
			case ENHANCED_RARE:
			case ARTIFACT:
			case RELIC:
			case EPIC:
			case UNIQUE:
			case UNIQUE_EVENT:
			case SHULKER_BOX:
				return ItemDeathResult.SHATTER;
			case QUEST_COMPASS:
				return ItemDeathResult.KEEP;
			default:
				return ItemDeathResult.LOSE;
			}
		default:
			return ItemDeathResult.LOSE;
		}
	}

	// Returns the costs (in tier 2 currency (CXP/CCS/etc.)) for each region to reforge a list of items.
	// There is no global "MONUMENTA" currency. The calling function will determine what to do with this cost.
	public static Map<ItemRegion, Integer> getReforgeCosts(Collection<ItemStack> items) {
		Map<ItemRegion, Integer> costs = new HashMap<>();
		costs.put(ItemRegion.KINGS_VALLEY, 0);
		costs.put(ItemRegion.CELSIAN_ISLES, 0);
		costs.put(ItemRegion.MONUMENTA, 0);
		for (ItemStack item : items) {
			ItemRegion type = getItemRegion(item);
			costs.computeIfPresent(type, (k, v) -> v += getReforgeCost(item));
		}

		return costs;
	}

	// Returns the cost (in tier 2 currency (CXP/CCS/etc.)) to reforge an item.
	public static Integer getReforgeCost(ItemStack item) {
		switch (getItemTier(item)) {
		case FOUR:
			return item.getAmount() * 1;
		case FIVE:
			return item.getAmount() * 4;
		case UNCOMMON:
			return item.getAmount() * 16;
		case UNIQUE:
		case UNIQUE_EVENT:
		case ENHANCED_UNCOMMON:
			return item.getAmount() * 32;
		case RARE:
		case PATRON_MADE:
			return item.getAmount() * 64 * 1;
		case RELIC:
		case ARTIFACT:
		case SHULKER_BOX:
		case ENHANCED_RARE:
			return item.getAmount() * 64 * 2;
		case EPIC:
			return item.getAmount() * 64 * 8;
		default:
			return 0;
		}
	}

	public static EquipmentSlot getEquipmentSlot(ItemStack item) {
		switch (item.getType()) {
		case LEATHER_HELMET:
		case CHAINMAIL_HELMET:
		case IRON_HELMET:
		case GOLDEN_HELMET:
		case DIAMOND_HELMET:
		case PUMPKIN:
		case CREEPER_HEAD:
		case SKELETON_SKULL:
		case WITHER_SKELETON_SKULL:
		case ZOMBIE_HEAD:
		case PLAYER_HEAD:
			return EquipmentSlot.HEAD;
		case LEATHER_CHESTPLATE:
		case CHAINMAIL_CHESTPLATE:
		case IRON_CHESTPLATE:
		case GOLDEN_CHESTPLATE:
		case DIAMOND_CHESTPLATE:
			return EquipmentSlot.CHEST;
		case LEATHER_LEGGINGS:
		case CHAINMAIL_LEGGINGS:
		case IRON_LEGGINGS:
		case GOLDEN_LEGGINGS:
		case DIAMOND_LEGGINGS:
			return EquipmentSlot.LEGS;
		case LEATHER_BOOTS:
		case CHAINMAIL_BOOTS:
		case IRON_BOOTS:
		case GOLDEN_BOOTS:
		case DIAMOND_BOOTS:
			return EquipmentSlot.FEET;
		case SHIELD:
			return EquipmentSlot.OFF_HAND;
		default:
			return EquipmentSlot.HAND;
		}
	}

	/**
	 * Items drop if they have lore that does not contain $$$
	 */
	public static float getItemDropChance(ItemStack item) {
		if (item != null && (item.hasItemMeta() && item.getItemMeta().hasLore()) && !InventoryUtils.testForItemWithLore(item, "$$")) {
			return 100.0f;
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

		meta.setDisplayName("�r" + name);
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
		meta.setDisplayName("�r" + name);
		meta.setColor(color);
		potion.setItemMeta(meta);
	}

	// Check if item has the "* Shattered *" lore entry
	public static boolean isItemShattered(ItemStack item) {
		if (item != null) {
			List<String> lore = item.getLore();
			if (lore != null) {
				for (String loreEntry : lore) {
					if (loreEntry.contains(ChatColor.DARK_RED + "" + ChatColor.BOLD + "* SHATTERED *")) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public static boolean shatterItem(ItemStack item) {
		List<String> lore = item.getLore();
		if (getItemDeathResult(item) == ItemDeathResult.SHATTER && !isItemShattered(item)) {
			if (lore == null) {
				lore = new ArrayList<String>();
			}
			lore.add(ChatColor.DARK_RED + "" + ChatColor.BOLD + "* SHATTERED *");
			lore.add(ChatColor.DARK_RED + "Maybe a Master Repairman");
			lore.add(ChatColor.DARK_RED + "could reforge it...");
			item.setLore(lore);
			return true;
		}
		return false;
	}

	// Check if item is a wearable with the "* Shattered *" lore entry
	public static boolean isWearableItemShattered(ItemStack item) {
		return item != null && isWearable(item.getType()) && isItemShattered(item);
	}

	// Check if item is armor with the "* Shattered *" lore entry
	public static boolean isArmorItemShattered(ItemStack item) {
		return item != null && isArmorItem(item.getType()) && isItemShattered(item);
	}

	public static boolean isArmorItem(Material mat) {
		return armors.contains(mat);
	}

	public static boolean isWearable(Material mat) {
		return wearable.contains(mat);
	}

	public static boolean isAllowedTreeReplace(Material item) {
		return allowedTreeReplaceMaterials.contains(item);
	}

	public static boolean isShulkerBox(Material mat) {
		return shulkerBoxes.contains(mat);
	}
}
