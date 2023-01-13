package com.playmonumenta.plugins.utils;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.enchantments.CurseOfEphemerality;
import com.playmonumenta.plugins.listeners.GraveListener;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
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
import org.jetbrains.annotations.Nullable;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;



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

	public static boolean testForItemWithName(final @Nullable ItemStack item, final @Nullable String legacyNameText, boolean exact) {
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
				return exact ? displayName.equals(nameText) : displayName.contains(nameText);
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

	public static int removeSpecialItems(final Player player, final boolean ephemeralOnly, final boolean includeSubInventories) {
		int dropped = 0;

		final Location loc = player.getLocation();

		// Inventory
		dropped += removeSpecialItemsFromInventory(player.getInventory(), loc, ephemeralOnly, includeSubInventories);

		if (includeSubInventories) {
			// Ender Chest
			dropped += removeSpecialItemsFromInventory(player.getEnderChest(), loc, ephemeralOnly, true);
		}

		// Armor slots
		@Nullable ItemStack[] items = player.getInventory().getArmorContents();
		dropped += removeSpecialItemsFromInventory(items, loc, ephemeralOnly, false);
		player.getInventory().setArmorContents(items);

		// Extra slots (offhand, ???)
		items = player.getInventory().getExtraContents();
		dropped += removeSpecialItemsFromInventory(items, loc, ephemeralOnly, includeSubInventories);
		player.getInventory().setExtraContents(items);

		return dropped;
	}

	private static int removeSpecialItemsFromInventory(final @Nullable ItemStack @Nullable [] items, final Location loc, final boolean ephemeralOnly, final boolean includeSubInventories) {
		if (items == null) {
			return 0;
		}

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
				} else if (includeSubInventories) {
					try {
						if (item.hasItemMeta() && item.getItemMeta() instanceof BlockStateMeta meta) {
							if (meta.getBlockState() instanceof ShulkerBox shulker) {
								dropped += removeSpecialItemsFromInventory(shulker.getInventory(), loc, ephemeralOnly, true);

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

	private static int removeSpecialItemsFromInventory(final Inventory inventory, final Location loc, final boolean ephemeralOnly, final boolean includeSubInventories) {
		final @Nullable ItemStack[] items = inventory.getContents();
		final int dropped = removeSpecialItemsFromInventory(items, loc, ephemeralOnly, includeSubInventories);
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

	public static int removeSoulboundItemFromInventory(final Inventory inventory, final String itemPlainName, final int amount, final Player player) {
		if (amount <= 0) {
			return 0;
		}
		@Nullable ItemStack[] items = inventory.getContents();
		int dropped = 0;
		int total = 0;
		List<Integer> matched = new ArrayList<>();
		for (int i = 0; i < items.length; i++) {
			ItemStack slot = items[i];
			if (ItemUtils.getPlainName(slot).equals(itemPlainName) &&
				    isSoulboundToPlayer(slot, player)) {
				total += slot.getAmount();
				matched.add(i);
			}
		}
		if (total >= amount) {
			for (int i : matched) {
				ItemStack slot = items[i];
				if (slot.getAmount() > amount - dropped) {
					slot.setAmount(slot.getAmount() - (amount - dropped));
					dropped = amount;
					player.updateInventory();
					break;
				} else {
					dropped += slot.getAmount();
					slot.setAmount(0);
					player.updateInventory();
					if (dropped >= amount) {
						break;
					}
				}
			}
		}
		return dropped;
	}

	public static boolean containsSpecialLore(final ItemStack item) {
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

	public static void giveItem(final Player player, final @Nullable ItemStack item) {
		giveItem(player, item, false);
	}

	public static void giveItem(final Player player, final @Nullable ItemStack item, boolean silent) {
		if (item == null) {
			return;
		}
		final PlayerInventory inv = player.getInventory();
		if (canFitInInventory(item, inv)) {
			inv.addItem(item);
		} else {
			dropTempOwnedItem(item, player.getLocation(), player);
			if (!silent) {
				player.sendMessage(Component.text("Your inventory is full! Some items were dropped on the ground!", NamedTextColor.RED));
			}
		}
	}

	public static void giveItemFromLootTable(Player player, NamespacedKey key, int amount) {
		LootTable lt = Bukkit.getLootTable(key);
		if (lt != null) {
			LootContext.Builder builder = new LootContext.Builder(player.getLocation());
			LootContext context = builder.build();
			Collection<ItemStack> items = lt.populateLoot(FastUtils.RANDOM, context);
			if (items.size() > 0) {
				ItemStack materials = items.iterator().next();
				materials.setAmount(amount);
				InventoryUtils.giveItem(player, materials);
				return;
			}
		}
		player.sendMessage(Component.text("ERROR getting loot table. Please contact a moderator if you see this message!", NamedTextColor.RED));
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

	public static ItemStack getItemFromLootTableOrThrow(Location loc, NamespacedKey key) throws IllegalStateException {
		for (ItemStack item : getItemsFromLootTable(loc, key)) {
			return item;
		}
		throw new IllegalStateException("Could not find item from loot table " + key);
	}

	public static Collection<ItemStack> getItemsFromLootTable(Location loc, NamespacedKey key) {
		LootContext context = new LootContext.Builder(loc).build();
		LootTable table = Bukkit.getLootTable(key);
		if (table != null) {
			return table.populateLoot(FastUtils.RANDOM, context);
		}
		return Collections.emptyList();
	}

	/**
	 * Checks if the given inventory can fit all the passed-in items.
	 * The passed list of items must not have duplicates! Overfull stacks are properly supported, however.
	 */
	public static boolean canFitInInventory(List<ItemStack> items, Inventory inventory) {
		int[] remainingCount = items.stream().mapToInt(ItemStack::getAmount).toArray();
		// first find existing stacks
		for (ItemStack itemInInventory : inventory.getStorageContents()) {
			if (itemInInventory == null) {
				continue;
			}
			for (int i = 0; i < remainingCount.length; i++) {
				ItemStack item = items.get(i);
				if (itemInInventory.isSimilar(item)) {
					remainingCount[i] -= Math.max(0, item.getMaxStackSize() - itemInInventory.getAmount());
					break;
				}
			}
		}
		// then find empty slots
		for (ItemStack itemInInventory : inventory.getStorageContents()) {
			if (itemInInventory == null) {
				for (int i = 0; i < remainingCount.length; i++) {
					if (remainingCount[i] > 0) {
						remainingCount[i] -= items.get(i).getMaxStackSize();
						break;
					}
				}
			}
		}
		return Arrays.stream(remainingCount).allMatch(i -> i <= 0);
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

	public static int numCanFitInInventory(ItemStack item, Inventory inventory) {
		int count = 0;

		// getStorageContents excludes armor, offhand slots
		for (ItemStack itemInInventory : inventory.getStorageContents()) {
			if (itemInInventory == null) {
				count += item.getMaxStackSize();
			} else if (item.isSimilar(itemInInventory)) {
				count += Math.max(0, item.getMaxStackSize() - itemInInventory.getAmount());
			}
		}
		return count;
	}

	public static int numEmptySlots(Inventory inventory) {
		return (int) Arrays.stream(inventory.getStorageContents()).filter(ItemUtils::isNullOrAir).count();
	}

	public static int numInInventory(Inventory inventory, ItemStack item) {
		return Arrays.stream(inventory.getStorageContents())
			       .filter(Objects::nonNull)
			       .filter(item::isSimilar)
			       .mapToInt(ItemStack::getAmount)
			       .sum();
	}

	/**
	 * Checks whether an inventory is full, i.e. has at least one item in every slot. Does not check if the stacks are at max size.
	 */
	public static boolean isFull(Inventory inventory) {
		return Arrays.stream(inventory.getStorageContents()).noneMatch(ItemUtils::isNullOrAir);
	}

	/**
	 * Inserts an item stack into an inventory, using only the first {@code size} slots of the inventory.
	 *
	 * @param inventory The inventory to put the item into
	 * @param size      Maximum number of inventory slots to use
	 * @param itemStack The item stack to insert. This will be modified and will hold the number of items not inserted.
	 */
	public static void insertItemIntoLimitedInventory(Inventory inventory, int size, ItemStack itemStack) {
		if (itemStack == null || itemStack.getType() == Material.AIR) {
			return;
		}
		// fill existing stacks first
		for (int i = 0; i < size; i++) {
			ItemStack invItem = inventory.getItem(i);
			if (invItem != null
				    && invItem.getAmount() < invItem.getMaxStackSize()
				    && invItem.isSimilar(itemStack)) {
				int deposited = Math.min(itemStack.getAmount(), invItem.getMaxStackSize() - invItem.getAmount());
				itemStack.subtract(deposited);
				invItem.add(deposited);
				inventory.setItem(i, invItem);
				if (itemStack.getAmount() == 0) {
					return;
				}
			}
		}
		// put remaining items into first available slot
		for (int i = 0; i < size; i++) {
			ItemStack invItem = inventory.getItem(i);
			if (invItem == null || invItem.getType() == Material.AIR) {
				inventory.setItem(i, itemStack);
				itemStack.setAmount(0);
				return;
			}
		}
	}
}
