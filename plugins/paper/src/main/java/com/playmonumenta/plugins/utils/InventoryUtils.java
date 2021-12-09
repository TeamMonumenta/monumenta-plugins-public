package com.playmonumenta.plugins.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.enchantments.curses.CurseOfEphemerality;
import com.playmonumenta.plugins.enchantments.curses.TwoHanded;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;



public class InventoryUtils {
	private static int OFFHAND_SLOT = 40;
	private static int HELMET_SLOT = 39;
	private static int CHESTPLATE_SLOT = 38;
	private static int LEGGINGS_SLOT = 37;
	private static int BOOTS_SLOT = 36;

	//TODO Fix exploit where changing hotbar slots and then firing a projectile uses the old slot's attributes
	public static void scheduleDelayedEquipmentCheck(final Plugin plugin, final Player player, final @Nullable Event event) {
		new BukkitRunnable() {
			@Override
			public void run() {
				final ItemStack mainHand = player.getInventory().getItemInMainHand();
				final ItemStack offHand = player.getInventory().getItemInOffHand();

				AbilityManager.getManager().playerItemHeldEvent(player, mainHand, offHand);
				plugin.mTrackingManager.mPlayers.updateEquipmentProperties(player, event);
			}
		}.runTaskLater(plugin, 0);
	}

	//Updates equipment enchants for one specific slot
	public static void scheduleDelayedEquipmentSlotCheck(final Plugin plugin, final Player player, final int slot) {
		new BukkitRunnable() {
			@Override
			public void run() {
				final ItemStack mainHand = player.getInventory().getItemInMainHand();
				final ItemStack offHand = player.getInventory().getItemInOffHand();

				AbilityManager.getManager().playerItemHeldEvent(player, mainHand, offHand);
				plugin.mTrackingManager.mPlayers.updateItemSlotProperties(player, slot);
			}
		}.runTaskLater(plugin, 0);
	}

	public static boolean testForItemWithLore(final ItemStack item, final String legacyLoreText) {
		// TODO START Remove this block when all legacy text is updated to use Adventure or plain text.
		if (legacyLoreText == null || legacyLoreText.isEmpty()) {
			return true;
		}
		final String loreText = ItemUtils.toPlainTagText(legacyLoreText);
		// TODO END

		if (loreText == null || loreText.isEmpty()) {
			return true;
		}

		if (item == null) {
			return false;
		}

		final List<String> lore = ItemUtils.getPlainLore(item);
		if (lore == null || lore.isEmpty()) {
			return false;
		}

		for (final String loreEntry : lore) {
			if (loreEntry.contains(loreText)) {
				return true;
			}
		}

		return false;
	}

	// TODO: This will *not* match items that don't have an NBT name (stick, stone sword, etc.)
	public static boolean testForItemWithName(final ItemStack item, final String legacyNameText) {
		// TODO START Remove this block when all legacy text is updated to use Adventure or plain text.
		if (legacyNameText == null || legacyNameText.isEmpty()) {
			return true;
		}
		final String nameText = ItemUtils.toPlainTagText(legacyNameText);
		// TODO END

		if (nameText == null || nameText.isEmpty()) {
			return true;
		}

		if (item != null) {
			final String displayName = ItemUtils.getPlainName(item);
			if (displayName != null && !displayName.isEmpty()) {
				if (displayName.contains(nameText)) {
					return true;
				}
			}
		}

		return false;
	}

	public static int getCustomEnchantLevel(final ItemStack item, final String legacyNameText, final boolean useLevel) {
		// TODO START Remove this block when all legacy text is updated to use Adventure or plain text.
		if (legacyNameText == null || legacyNameText.isEmpty()) {
			return 0;
		}
		final String nameText = ItemUtils.toPlainTagText(legacyNameText);
		// TODO END

		if (nameText == null || nameText.isEmpty()) {
			return 0;
		}

		if (item == null || item.getAmount() <= 0) {
			return 0;
		}

		final List<String> lore = ItemUtils.getPlainLore(item);
		if (lore == null || lore.isEmpty()) {
			return 0;
		}

		for (final String loreEntry : lore) {
			if (loreEntry.startsWith(nameText)) {
				if (useLevel) {
					int offset = 1;
					int level = 0;
					// Does not account for L (50) or higher symbols, which some items have for non-custom enchants
					while (true) {
						final char c = loreEntry.charAt(loreEntry.length() - offset);
						if (c == 'I') {
							level += 1;
						} else if (c == 'V') {
							final char cn = loreEntry.charAt(loreEntry.length() - offset - 1);
							if (cn == 'I') {
								level += 4;
								offset += 1;
							} else {
								level += 5;
							}
						} else if (c == 'X') {
							final char cn = loreEntry.charAt(loreEntry.length() - offset - 1);
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

		return 0;
	}

	public static void removeCustomEnchant(final ItemStack item, final String nameText) {
		if (!nameText.isEmpty() && item != null) {
			final ItemMeta meta = item.getItemMeta();
			if (meta != null) {
				final List<String> lore = meta.getLore();
				if (lore != null && !lore.isEmpty()) {
					final List<String> newLore = new ArrayList<>();
					for (final String line : lore) {
						if (!line.startsWith(nameText)) {
							newLore.add(line);
						}
					}
					meta.setLore(newLore);
					item.setItemMeta(meta);
					ItemUtils.setPlainLore(item);
				}
			}
		}
	}

	public static boolean isAxeItem(final ItemStack item) {
		if (item != null) {
			final Material mat = item.getType();
			return mat == Material.WOODEN_AXE || mat == Material.STONE_AXE || mat == Material.GOLDEN_AXE
				   || mat == Material.IRON_AXE || mat == Material.DIAMOND_AXE || mat == Material.NETHERITE_AXE;
		}

		return false;
	}

	public static boolean isBowItem(final ItemStack item) {
		if (item != null) {
			final Material mat = item.getType();
			return mat == Material.BOW || mat == Material.CROSSBOW;
		}

		return false;
	}

	public static boolean isSwordItem(final ItemStack item) {
		if (item != null) {
			final Material mat = item.getType();
			return mat == Material.WOODEN_SWORD || mat == Material.STONE_SWORD || mat == Material.GOLDEN_SWORD
				   || mat == Material.IRON_SWORD || mat == Material.DIAMOND_SWORD || mat == Material.NETHERITE_SWORD;
		}

		return false;
	}

	public static boolean isPickaxeItem(final ItemStack item) {
		if (item != null) {
			final Material mat = item.getType();
			return mat == Material.WOODEN_PICKAXE || mat == Material.STONE_PICKAXE || mat == Material.GOLDEN_PICKAXE
				   || mat == Material.IRON_PICKAXE || mat == Material.DIAMOND_PICKAXE || mat == Material.NETHERITE_PICKAXE;
		}

		return false;
	}

	public static boolean isScytheItem(final ItemStack item) {
		if (item != null) {
			if (ItemUtils.isItemShattered(item)) {
				return false;
			}

			final Material mat = item.getType();
			return mat == Material.WOODEN_HOE || mat == Material.STONE_HOE || mat == Material.GOLDEN_HOE
				   || mat == Material.IRON_HOE || mat == Material.DIAMOND_HOE || mat == Material.NETHERITE_HOE;
		}

		return false;
	}

	public static boolean isShovelItem(final ItemStack item) {
		if (item != null) {
			final Material mat = item.getType();
			return mat == Material.WOODEN_SHOVEL || mat == Material.STONE_SHOVEL || mat == Material.GOLDEN_SHOVEL
				   || mat == Material.IRON_SHOVEL || mat == Material.DIAMOND_SHOVEL;
		}

		return false;
	}

	public static boolean isWandItem(final ItemStack item) {
		if (item == null) {
			return false;
		}

		final List<String> lore = ItemUtils.getPlainLore(item);
		if (lore == null || lore.isEmpty()) {
			return false;
		}

		if (ItemUtils.isItemShattered(item)) {
			return false;
		}

		for (final String loreEntry : lore) {
			if (loreEntry.contains("Magic Wand")) {
				return true;
			}
		}

		return false;
	}

	public static boolean isSoulboundToPlayer(final ItemStack item, final Player player) {
		// TODO: Needs to handle renames
		return testForItemWithLore(item, "* Soulbound to " + player.getName() + " *");
	}

	public static int removeSpecialItems(final Player player, final boolean ephemeralOnly) {
		int dropped = 0;

		final Location loc = player.getLocation();

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

	private static int removeSpecialItemsFromInventory(final ItemStack @Nullable [] items, final Location loc, final boolean ephemeralOnly) {
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
					try {
						if (items[i].hasItemMeta() && items[i].getItemMeta() instanceof BlockStateMeta) {
							final BlockStateMeta meta = (BlockStateMeta)items[i].getItemMeta();
							if (meta.getBlockState() instanceof ShulkerBox) {
								final ShulkerBox shulker = (ShulkerBox)meta.getBlockState();
								dropped += removeSpecialItemsFromInventory(shulker.getInventory(), loc, ephemeralOnly);

								meta.setBlockState(shulker);
								items[i].setItemMeta(meta);
							}
						}
					} catch (Exception ex) {
						/* There's a weird exception within Paper here that sometimes happens
						 * Need to make sure this doesn't cause item dropping to fail
						 */
						Plugin.getInstance().getLogger().warning("Caught exception trying to remove special items from inventory: " + ex.getMessage());
						ex.printStackTrace();
					}
				}
			}
		}

		return dropped;
	}

	private static int removeSpecialItemsFromInventory(final Inventory inventory, final Location loc, final boolean ephemeralOnly) {
		final ItemStack @Nullable [] items = inventory.getContents();
		final int dropped = removeSpecialItemsFromInventory(items, loc, ephemeralOnly);
		inventory.setContents(items);
		return dropped;
	}

	public static int removeNamedItems(Player player, String name) {
		int dropped = removeNamedItemsFromInventory(player.getInventory(), name);
		dropped += removeNamedItemsFromInventory(player.getEnderChest(), name);
		return dropped;
	}

	private static int removeNamedItemsFromInventory(final Inventory inventory, final String name) {
		int dropped = 0;
		ItemStack @Nullable [] items = inventory.getContents();

		for (int i = 0; i < items.length; i++) {
			if (items[i] != null) {
				if (items[i].hasItemMeta() && items[i].getItemMeta().getDisplayName().equals(name)) {
					items[i] = null;
				} else {
					if (items[i].hasItemMeta() && items[i].getItemMeta() instanceof BlockStateMeta) {
						final BlockStateMeta meta = (BlockStateMeta)items[i].getItemMeta();
						if (meta.getBlockState() instanceof ShulkerBox) {
							final ShulkerBox shulker = (ShulkerBox) meta.getBlockState();
							ItemStack @Nullable [] shulkerItems = shulker.getInventory().getContents();
							for (int j = 0; j < shulkerItems.length; j++) {
								if (shulkerItems[j] != null && shulkerItems[j].hasItemMeta() && shulkerItems[j].getItemMeta().getDisplayName().equals(name)) {
									shulkerItems[j] = null;
								}
							}

							shulker.getInventory().setContents(shulkerItems);
							meta.setBlockState(shulker);
							items[i].setItemMeta(meta);
						}
					}
				}
			}
		}

		inventory.setContents(items);

		return dropped;
	}

	private static boolean containsSpecialLore(final ItemStack item) {
		return testForItemWithLore(item, "Taking this item outside of the dungeon");
	}

	public static String itemStackArrayToBase64(final ItemStack[] items) throws IllegalStateException {
		try {
			final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			final BukkitObjectOutputStream dataOutput = new BukkitObjectOutputStream(outputStream);

			//  Write the size of the inventory.
			dataOutput.writeInt(items.length);

			//  Save all the elements.
			for (int i = 0; i < items.length; i++) {
				dataOutput.writeObject(items[i]);
			}

			//  Serialize the array.
			dataOutput.close();
			return Base64Coder.encodeLines(outputStream.toByteArray());
		} catch (final Exception e) {
			throw new IllegalStateException("Unable to save item stacks.", e);
		}
	}

	public static ItemStack[] itemStackArrayFromBase64(final String data) throws IOException {
		try {
			final ByteArrayInputStream inputStream = new ByteArrayInputStream(Base64Coder.decodeLines(data));
			final BukkitObjectInputStream dataInput = new BukkitObjectInputStream(inputStream);
			final ItemStack[] items = new ItemStack[dataInput.readInt()];

			// Read the serialized inventory
			for (int i = 0; i < items.length; i++) {
				items[i] = (ItemStack) dataInput.readObject();
			}

			dataInput.close();
			return items;
		} catch (final ClassNotFoundException e) {
			throw new IOException("Unable to decode class type.", e);
		}
	}

	public static boolean isArmorSlotFromId(final int slotId) {
		return slotId == OFFHAND_SLOT || slotId == HELMET_SLOT || slotId == CHESTPLATE_SLOT
			   || slotId == LEGGINGS_SLOT || slotId == BOOTS_SLOT;
	}

	static void shuffleArray(final int[] ar) {
		for (int i = ar.length - 1; i > 0; i--) {
			final int index = FastUtils.RANDOM.nextInt(i + 1);
			// Simple swap
			final int a = ar[index];
			ar[index] = ar[i];
			ar[i] = a;
		}
	}

	public static void giveItem(final Player player, final ItemStack item) {
		final PlayerInventory inv = player.getInventory();
		if (canFitInInventory(item, inv)) {
			inv.addItem(item);
		} else {
			final Location ploc = player.getLocation();
			ploc.getWorld().dropItem(ploc, item);
			player.sendMessage(Component.text("Your inventory is full! Some items were dropped on the ground!", NamedTextColor.RED));
		}
	}

	public static boolean rogueTriggerCheck(final ItemStack mainhand, final ItemStack offhand) {
		boolean isMainhand = ItemUtils.isSword(mainhand);
		boolean isOffhand = ItemUtils.isSword(offhand);
		return (isMainhand && isOffhand) || (isMainhand && testForItemWithLore(mainhand, TwoHanded.PROPERTY_NAME) && offhand.getType() == Material.AIR);
	}

	/* Note this should only be used with loot tables that contain a single item */
	public static @Nullable ItemStack getItemFromLootTable(Entity entity, NamespacedKey key) {
		return getItemFromLootTable(entity.getLocation(), key);
	}

	/* Note this should only be used with loot tables that contain a single item */
	public static @Nullable ItemStack getItemFromLootTable(Location loc, NamespacedKey key) {
		for (ItemStack item : getItemsFromLootTable(loc, key)) {
			return item;
		}
		return null;
	}

	public static Collection<ItemStack> getItemsFromLootTable(Location loc, NamespacedKey key) {
		LootContext context = new LootContext.Builder(loc).build();
		LootTable table = Bukkit.getLootTable(key);
		if (table != null) {
			return table.populateLoot(FastUtils.RANDOM, context);
		}
		return Collections.emptyList();
	}

	public static boolean canFitInInventory(ItemStack item, Inventory inventory) {
		int remainingCount = item.getAmount();

		// getStorageContents excludes armor, offhand slots
		for (ItemStack itemInInventory : inventory.getStorageContents()) {
			if (itemInInventory == null) {
				return true;
			} else if (item.isSimilar(itemInInventory)) {
				remainingCount -= item.getMaxStackSize() - itemInInventory.getAmount();
				if (remainingCount <= 0) {
					return true;
				}
			}
		}
		return false;
	}
}
