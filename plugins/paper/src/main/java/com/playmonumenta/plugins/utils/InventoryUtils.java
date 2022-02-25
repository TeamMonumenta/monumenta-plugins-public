package com.playmonumenta.plugins.utils;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.enchantments.CurseOfEphemerality;
import com.playmonumenta.plugins.listeners.GraveListener;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import javax.annotation.Nullable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTable;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;



public class InventoryUtils {
	private static final int OFFHAND_SLOT = 40;
	private static final int HELMET_SLOT = 39;
	private static final int CHESTPLATE_SLOT = 38;
	private static final int LEGGINGS_SLOT = 37;
	private static final int BOOTS_SLOT = 36;

	public static void scheduleDelayedEquipmentCheck(final Plugin plugin, final Player player, final @Nullable Event event) {
		new BukkitRunnable() {
			@Override
			public void run() {
				plugin.mTrackingManager.mPlayers.updateEquipmentProperties(player, event);
			}
		}.runTaskLater(plugin, 0);
	}

	//Updates equipment enchants for one specific slot
	public static void scheduleDelayedEquipmentSlotCheck(final Plugin plugin, final Player player, final int slot) {
		new BukkitRunnable() {
			@Override
			public void run() {
				plugin.mTrackingManager.mPlayers.updateItemSlotProperties(player, slot);
			}
		}.runTaskLater(plugin, 0);
	}

	public static boolean testForItemWithLore(final @Nullable ItemStack item, final @Nullable String legacyLoreText) {
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
	public static boolean testForItemWithName(final @Nullable ItemStack item, final @Nullable String legacyNameText) {
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

	public static boolean isSoulboundToPlayer(final ItemStack item, final Player player) {
		if (player == null || item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore()) {
			return false;
		}
		List<String> lore = item.getLore();
		if (lore != null) {
			for (String line : lore) {
				if (line.contains("Soulbound to") && line.contains(player.getName())) {
					return true;
				}
			}
		}
		return false;
	}

	public static int removeSpecialItems(final Player player, final boolean ephemeralOnly) {
		int dropped = 0;

		final Location loc = player.getLocation();

		// Inventory
		dropped += removeSpecialItemsFromInventory(player.getInventory(), loc, ephemeralOnly);

		// Ender Chest
		dropped += removeSpecialItemsFromInventory(player.getEnderChest(), loc, ephemeralOnly);

		// Armor slots
		@Nullable ItemStack[] items = player.getInventory().getArmorContents();
		dropped += removeSpecialItemsFromInventory(items, loc, ephemeralOnly);
		player.getInventory().setArmorContents(items);

		// Extra slots (offhand, ???)
		items = player.getInventory().getExtraContents();
		dropped += removeSpecialItemsFromInventory(items, loc, ephemeralOnly);
		player.getInventory().setExtraContents(items);

		return dropped;
	}

	private static int removeSpecialItemsFromInventory(final @Nullable ItemStack[] items, final Location loc, final boolean ephemeralOnly) {
		int dropped = 0;

		for (int i = 0; i < items.length; i++) {
			ItemStack item = items[i];
			if (item != null) {
				if (!ephemeralOnly && containsSpecialLore(item)) {
					loc.getWorld().dropItem(loc, item);
					items[i] = null;
					dropped += 1;
				} else if (CurseOfEphemerality.isEphemeral(item)) {
					items[i] = null;
				} else {
					try {
						if (item.hasItemMeta() && item.getItemMeta() instanceof BlockStateMeta) {
							final BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
							if (meta.getBlockState() instanceof ShulkerBox) {
								final ShulkerBox shulker = (ShulkerBox) meta.getBlockState();
								dropped += removeSpecialItemsFromInventory(shulker.getInventory(), loc, ephemeralOnly);

								meta.setBlockState(shulker);
								item.setItemMeta(meta);
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
		final @Nullable ItemStack[] items = inventory.getContents();
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
		@Nullable ItemStack[] items = inventory.getContents();

		for (int i = 0; i < items.length; i++) {
			ItemStack item = items[i];
			if (item != null) {
				if (item.hasItemMeta() && item.getItemMeta().getDisplayName().equals(name)) {
					items[i] = null;
				} else {
					if (item.hasItemMeta() && item.getItemMeta() instanceof BlockStateMeta) {
						final BlockStateMeta meta = (BlockStateMeta) item.getItemMeta();
						if (meta.getBlockState() instanceof ShulkerBox) {
							final ShulkerBox shulker = (ShulkerBox) meta.getBlockState();
							@Nullable ItemStack[] shulkerItems = shulker.getInventory().getContents();
							for (int j = 0; j < shulkerItems.length; j++) {
								if (shulkerItems[j] != null && shulkerItems[j].hasItemMeta() && shulkerItems[j].getItemMeta().getDisplayName().equals(name)) {
									shulkerItems[j] = null;
								}
							}

							shulker.getInventory().setContents(shulkerItems);
							meta.setBlockState(shulker);
							item.setItemMeta(meta);
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
			dropTempOwnedItem(item, player.getLocation(), player);
			player.sendMessage(Component.text("Your inventory is full! Some items were dropped on the ground!", NamedTextColor.RED));
		}
	}

	/**
	 * Drops an item that can only be picked up by the given player for the first 10 secconds, and any player afterwards.
	 * The item will also count as dropped by the player for graving purposes.
	 *
	 * @param item     The item to drop
	 * @param location Location to drop the item at
	 * @param player   Player that can immediately pick up the item
	 * @return The dropped item
	 */
	public static Item dropTempOwnedItem(final ItemStack item, Location location, final Player player) {
		Item droppedItem = location.getWorld().dropItem(location, item);
		droppedItem.setPickupDelay(0);
		droppedItem.setCanMobPickup(false);
		droppedItem.setOwner(player.getUniqueId());
		droppedItem.setThrower(player.getUniqueId());
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			if (droppedItem.isValid()) {
				droppedItem.setOwner(null);
			}
		}, 200);
		GraveListener.itemDropped(player, droppedItem);
		return droppedItem;
	}

	public static boolean rogueTriggerCheck(Plugin plugin, @Nullable Player player) {
		if (player == null) {
			return false;
		}

		ItemStack mainhand = player.getInventory().getItemInMainHand();
		ItemStack offhand = player.getInventory().getItemInOffHand();

		if (ItemUtils.isSword(mainhand)) {
			if (plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.TWO_HANDED) > 0) {
				return offhand == null || offhand.getType().isAir() || (ItemUtils.isSword(offhand) && plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.WEIGHTLESS) > 0);
			} else {
				return ItemUtils.isSword(offhand);
			}
		}
		return false;
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
				remainingCount -= Math.max(0, item.getMaxStackSize() - itemInInventory.getAmount());
				if (remainingCount <= 0) {
					return true;
				}
			}
		}
		return false;
	}
}
