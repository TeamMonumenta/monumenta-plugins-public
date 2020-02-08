package com.playmonumenta.plugins.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.enchantments.CurseOfEphemerality;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;

public class InventoryUtils {
	private static int OFFHAND_SLOT = 40;
	private static int HELMET_SLOT = 39;
	private static int CHESTPLATE_SLOT = 38;
	private static int LEGGINGS_SLOT = 37;
	private static int BOOTS_SLOT = 36;

	public static void scheduleDelayedEquipmentCheck(Plugin plugin, Player player, Event event) {
		player.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
			@Override
			public void run() {
				ItemStack mainHand = player.getInventory().getItemInMainHand();
				ItemStack offHand = player.getInventory().getItemInOffHand();

				AbilityManager.getManager().PlayerItemHeldEvent(player, mainHand, offHand);
				plugin.mTrackingManager.mPlayers.updateEquipmentProperties(player, event);
			}
		}, 0);
	}

	public static boolean testForItemWithLore(ItemStack item, String loreText) {
		if (loreText == null || loreText.isEmpty()) {
			return true;
		}

		if (item != null) {
			ItemMeta meta = item.getItemMeta();
			if (meta != null) {
				List<String> lore = meta.getLore();
				if (lore != null && !lore.isEmpty()) {
					for (String loreEntry : lore) {
						if (loreEntry.contains(loreText)) {
							return true;
						}
					}
				}
			}
		}

		return false;
	}

	// TODO: This will *not* match items that don't have an NBT name (stick, stone sword, etc.)
	public static boolean testForItemWithName(ItemStack item, String nameText) {
		if (nameText == null || nameText.isEmpty()) {
			return true;
		}

		if (item != null) {
			ItemMeta meta = item.getItemMeta();
			if (meta != null) {
				String displayName = meta.getDisplayName();
				if (displayName != null && !displayName.isEmpty()) {
					if (displayName.contains(nameText)) {
						return true;
					}
				}
			}
		}

		return false;
	}

	public static int getCustomEnchantLevel(ItemStack item, String nameText, boolean useLevel) {
		if (nameText == null || nameText.isEmpty()) {
			return 0;
		}

		if (item != null) {
			ItemMeta meta = item.getItemMeta();
			if (meta != null) {
				List<String> lore = meta.getLore();
				if (lore != null && !lore.isEmpty()) {
					for (String loreEntry : lore) {
						if (loreEntry.startsWith(nameText)) {
							if (useLevel) {
								int offset = 1;
								int level = 0;
								while (true) {
									char c = loreEntry.charAt(loreEntry.length() - offset);
									if (c == 'I') {
										level += 1;
									} else if (c == 'V') {
										char cn = loreEntry.charAt(loreEntry.length() - offset - 1);
										if (cn == 'I') {
											level += 4;
											offset += 1;
										} else {
											level += 5;
										}
									} else if (c == 'X') {
										char cn = loreEntry.charAt(loreEntry.length() - offset - 1);
										if (cn == 'I') {
											level += 9;
											offset += 1;
										} else {
											level += 10;
										}
									} else if (c == ' ') {
										break;
									} else {
										level = 1;
										break;
									}
									offset += 1;
								}
								return level;
							} else {
								return 1;
							}
						}
					}
				}
			}
		}

		return 0;
	}

	private static final Map<String, ItemSlot> LORE_SLOT_MAPPINGS = new HashMap<String, ItemSlot>();

	static {
		LORE_SLOT_MAPPINGS.put(ChatColor.GRAY + "When in main hand:", ItemSlot.MAINHAND);
		LORE_SLOT_MAPPINGS.put(ChatColor.GRAY + "When in off hand:", ItemSlot.OFFHAND);
		LORE_SLOT_MAPPINGS.put(ChatColor.GRAY + "When on ", ItemSlot.ARMOR);
	}

	public static int getCustomAttribute(ItemStack item, String nameText, Player player, ItemSlot slot) {
		if (nameText == null || nameText.isEmpty()) {
			return 0;
		}

		if (item != null) {
			ItemMeta meta = item.getItemMeta();
			if (meta != null) {
				List<String> lore = meta.getLore();
				if (lore != null && !lore.isEmpty()) {
					for (Map.Entry<String, ItemSlot> loreSlotMapping : LORE_SLOT_MAPPINGS.entrySet()) {
						// Find the lore slot entry corresponding to the slot the item occupies
						if (slot == loreSlotMapping.getValue()) {
							boolean foundAttributeSlot = false;
							// We'll bit-shift these together later which is why we need to store the negatives as positives
							int flatPositive = 0;
							int flatNegative = 0;
							int percentPositive = 0;
							int percentNegative = 0;

							for (String loreEntry : lore) {
								// If we find the proper lore, then we found the attributes for the slot
								if (loreEntry.contains(loreSlotMapping.getKey())) {
									foundAttributeSlot = true;
								} else {
									// Check that the lore isn't any other attribute slot marker
									for (String loreKey : LORE_SLOT_MAPPINGS.keySet()) {
										if (loreEntry.contains(loreKey)) {
											foundAttributeSlot = false;
										}
									}
								}

								if (foundAttributeSlot && loreEntry.contains(nameText)) {
									// If calculating Bow Damage or Arrow Speed and player has mainhand and offhand bow, then ignore the offhand bow stats
									if ((nameText.equals(" Bow Damage") || nameText.equals(" Arrow Speed")) && slot == ItemSlot.OFFHAND
										&& player.getInventory().getItemInOffHand().getType() == Material.BOW
										&& player.getInventory().getItemInMainHand().getType() == Material.BOW) {
										return 0;
									}

									// ChatColor takes up 2 characters, so the third character (index = 2) should be one of the following characters
									if (loreEntry.charAt(2) == ' ' || loreEntry.charAt(2) == '-' || loreEntry.charAt(2) == '+') {
										// Get the number value of the attribute
										if (loreEntry.indexOf('%') == -1) {
											int flat = Integer.parseInt(loreEntry.substring(2, loreEntry.indexOf(' ', 3)).trim());
											if (flat < 0) {
												flatNegative += Math.abs(flat);
											} else {
												flatPositive += flat;
											}
										} else {
											int percent = Integer.parseInt(loreEntry.substring(2, loreEntry.indexOf('%', 3)).trim());
											if (percent < 0) {
												percentNegative += Math.abs(percent);
											} else {
												percentPositive += percent;
											}
										}
									}
								}
							}

							// Making some room here... -255%, +1023%, -64, +256 should cover all of our item design needs
							return (percentNegative << 24) | (percentPositive << 14) | (flatNegative << 8) | flatPositive;
						}
					}
				}
			}
		}

		return 0;
	}

	// Returns the attribute value stored in the "level" (due to bitshifts)
	public static double getAttributeValue(int level) {
		return Math.max(0, ((level & 0xFF) - ((level >>> 8) & 0x3F)) * (1 + (((level >>> 14) & 0x3FF) - (level >>> 24)) / 100.0));
	}

	public static boolean isAxeItem(ItemStack item) {
		if (item != null) {
			Material mat = item.getType();
			return mat == Material.WOODEN_AXE || mat == Material.STONE_AXE || mat == Material.GOLDEN_AXE
			       || mat == Material.IRON_AXE || mat == Material.DIAMOND_AXE;
		}

		return false;
	}

	public static boolean isBowItem(ItemStack item) {
		if (item != null) {
			Material mat = item.getType();
			return mat == Material.BOW;
		}

		return false;
	}

	public static boolean isSwordItem(ItemStack item) {
		if (item != null) {
			Material mat = item.getType();
			return mat == Material.WOODEN_SWORD || mat == Material.STONE_SWORD || mat == Material.GOLDEN_SWORD
			       || mat == Material.IRON_SWORD || mat == Material.DIAMOND_SWORD;
		}

		return false;
	}

	public static boolean isPickaxeItem(ItemStack item) {
		if (item != null) {
			Material mat = item.getType();
			return mat == Material.WOODEN_PICKAXE || mat == Material.STONE_PICKAXE || mat == Material.GOLDEN_PICKAXE
			       || mat == Material.IRON_PICKAXE || mat == Material.DIAMOND_PICKAXE;
		}

		return false;
	}

	public static boolean isScytheItem(ItemStack item) {
		if (item != null) {
			Material mat = item.getType();
			return mat == Material.WOODEN_HOE || mat == Material.STONE_HOE || mat == Material.GOLDEN_HOE
			       || mat == Material.IRON_HOE || mat == Material.DIAMOND_HOE;
		}

		return false;
	}

	public static boolean isShovelItem(ItemStack item) {
		if (item != null) {
			Material mat = item.getType();
			return mat == Material.WOODEN_SHOVEL || mat == Material.STONE_SHOVEL || mat == Material.GOLDEN_SHOVEL
			       || mat == Material.IRON_SHOVEL || mat == Material.DIAMOND_SHOVEL;
		}

		return false;
	}

	public static boolean isWandItem(ItemStack item) {
		if (item != null) {
			ItemMeta meta = item.getItemMeta();
			if (meta != null && meta.hasLore()) {
				List<String> lore = meta.getLore();

				if (!lore.isEmpty()) {
					for (int i = 0; i < lore.size(); i++) {
						if (lore.get(i).contains("Magic Wand")) {
							return true;
						}
					}
				}
			}
		}

		return false;
	}

	public static boolean isSoulboundToPlayer(ItemStack item, Player player) {
		// TODO: Needs to handle renames
		return testForItemWithLore(item, "* Soulbound to " + player.getName() + " *");
	}

	public static boolean isPotionItem(ItemStack item) {
		if (item != null) {
			Material mat = item.getType();
			return mat == Material.POTION || mat == Material.LINGERING_POTION || mat == Material.SPLASH_POTION;
		}

		return false;
	}

	public static void removeRandomEquipment(Random rand, LivingEntity mob, Integer piecesToRemove) {
		int[] equipment = { 0, 1, 2, 3 };
		shuffleArray(rand, equipment);

		EntityEquipment gear = mob.getEquipment();

		int removedCount = 0;
		for (int i = 0; i < equipment.length; i++) {
			if (removedCount == 2) {
				return;
			}

			//  Head Slot
			if (equipment[i] == 0) {
				if (gear.getHelmet().getType() != Material.AIR) {
					gear.setHelmet(new ItemStack(Material.AIR));
					removedCount++;
				}
			}
			//  Chestplate
			else if (equipment[i] == 1) {
				if (gear.getChestplate().getType() != Material.AIR) {
					gear.setChestplate(new ItemStack(Material.AIR));
					removedCount++;
				}
			}
			//  Legs
			else if (equipment[i] == 2) {
				if (gear.getLeggings().getType() != Material.AIR) {
					gear.setLeggings(new ItemStack(Material.AIR));
					removedCount++;
				}
			}
			//  Boots
			else if (equipment[i] == 3) {
				if (gear.getBoots().getType() != Material.AIR) {
					gear.setBoots(new ItemStack(Material.AIR));
					removedCount++;
				}
			}
		}
	}

	public static int removeSpecialItems(Player player, boolean ephemeralOnly) {
		int dropped = 0;

		Location loc = player.getLocation();

		// Inventory
		dropped += removeSpecialItemsFromInventory(player.getInventory(), loc, ephemeralOnly);

		// Ender Chest
		dropped += removeSpecialItemsFromInventory(player.getEnderChest(), loc, ephemeralOnly);

		// Armor slots
		ItemStack[] items = player.getInventory().getArmorContents();
		dropped += removeSpecialItemsFromInventory(items, loc, ephemeralOnly);
		player.getInventory().setArmorContents(items);

		// Extra slots (offhand, ???)
		items = player.getInventory().getExtraContents();
		dropped += removeSpecialItemsFromInventory(items, loc, ephemeralOnly);
		player.getInventory().setExtraContents(items);

		return dropped;
	}

	private static int removeSpecialItemsFromInventory(ItemStack[] items, Location loc, boolean ephemeralOnly) {
		int dropped = 0;

		for (int i = 0; i < items.length; i++) {
			if (items[i] != null) {
				if (!ephemeralOnly && containsSpecialLore(items[i])) {
					loc.getWorld().dropItem(loc, items[i]);
					items[i] = null;
					dropped += 1;
				} else if (CurseOfEphemerality.isEphemeral(items[i])) {
					items[i] = null;
				} else {
					if (items[i].hasItemMeta() && items[i].getItemMeta() instanceof BlockStateMeta) {
						BlockStateMeta meta = (BlockStateMeta)items[i].getItemMeta();
						if (meta.getBlockState() instanceof ShulkerBox) {
							ShulkerBox shulker = (ShulkerBox)meta.getBlockState();
							dropped += removeSpecialItemsFromInventory(shulker.getInventory(), loc, ephemeralOnly);

							meta.setBlockState(shulker);
							items[i].setItemMeta(meta);
						}
					}
				}
			}
		}

		return dropped;
	}

	private static int removeSpecialItemsFromInventory(Inventory inventory, Location loc, boolean ephemeralOnly) {
		ItemStack[] items = inventory.getContents();
		int dropped = removeSpecialItemsFromInventory(items, loc, ephemeralOnly);
		inventory.setContents(items);
		return dropped;
	}

	private static boolean containsSpecialLore(ItemStack item) {
		return  testForItemWithLore(item, "D4 Key") ||
		        testForItemWithLore(item, "D5 Key") ||
		        testForItemWithLore(item, "D6 Key") ||
		        testForItemWithLore(item, "DN Key");
	}

	public static String itemStackArrayToBase64(ItemStack[] items) throws IllegalStateException {
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

			//  Write the size of the inventory.
			dataOutput.writeInt(items.length);

			//  Save all the elements.
			for (int i = 0; i < items.length; i++) {
				dataOutput.writeObject(items[i]);
			}

			//  Serialize the array.
			dataOutput.close();
			return Base64Coder.encodeLines(outputStream.toByteArray());
		} catch (Exception e) {
			throw new IllegalStateException("Unable to save item stacks.", e);
		}
	}

	public static ItemStack[] itemStackArrayFromBase64(String data) throws IOException {
		try {
			ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
			BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
			ItemStack[] items = new ItemStack[dataInput.readInt()];

			// Read the serialized inventory
			for (int i = 0; i < items.length; i++) {
				items[i] = (ItemStack) dataInput.readObject();
			}

			dataInput.close();
			return items;
		} catch (ClassNotFoundException e) {
			throw new IOException("Unable to decode class type.", e);
		}
	}

	public static boolean isArmorSlotFromId(int slotId) {
		return slotId == OFFHAND_SLOT || slotId == HELMET_SLOT || slotId == CHESTPLATE_SLOT
		       || slotId == LEGGINGS_SLOT || slotId == BOOTS_SLOT;
	}

	static void shuffleArray(Random rand, int[] ar) {
		for (int i = ar.length - 1; i > 0; i--) {
			int index = rand.nextInt(i + 1);
			// Simple swap
			int a = ar[index];
			ar[index] = ar[i];
			ar[i] = a;
		}
	}

	public static void giveItem(Player player, ItemStack item) {
		PlayerInventory inv = player.getInventory();
		if (inv.firstEmpty() == -1) {
			Location ploc = player.getLocation();
			ploc.getWorld().dropItem(ploc, item);
			player.sendMessage(ChatColor.RED + "Your inventory is full! Some items were dropped on the ground!");
		} else {
			inv.addItem(item);
		}
	}
}
