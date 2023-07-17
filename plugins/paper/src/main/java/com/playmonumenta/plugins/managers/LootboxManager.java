package com.playmonumenta.plugins.managers;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.CoreProtectIntegration;
import com.playmonumenta.plugins.utils.ChestUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import de.jeff_media.chestsort.api.ChestSortAPI;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTCompoundList;
import de.tr7zw.nbtapi.NBTItem;
import de.tr7zw.nbtapi.NBTListCompound;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class LootboxManager implements Listener {
	private final Plugin mPlugin;

	private static final int LOOTBOX_EPIC_MAX_SIZE = 100;
	private static final int LOOTBOX_NORMAL_MAX_SIZE = 27;
	private static final int LOOTBOX_WARN_FREE_SPACES = 6;
	private static final int LOOTBOX_UPDATE_LORE_DELAY = 10;
	private static final int LOOTBOX_RATELIMIT_TICKS = 5;
	public static final String LOOTBOX_KEY = "Lootbox";
	public static final String LOOTBOX_INDEX_KEY = "Index";
	public static final String LOOTBOX_SHARES_KEY = "Shares";
	public static final String LOOTBOX_SETTINGS_KEY = "LootboxSettings";
	public static final String LOOTBOX_TOTAL_SHARES_KEY = "ShareCount";
	public static final String LOOTBOX_MAX_SHARES_KEY = "ShareMax";
	public static final String LOOTSHARE_ITEM_KEY = "item";
	public static final String LOOTSHARE_AMOUNT_KEY = "amount";

	public static final EnumSet<InventoryType> ALLOWED_CONTAINERS = EnumSet.of(
		InventoryType.CHEST,
		InventoryType.ENDER_CHEST,
		InventoryType.SHULKER_BOX
	);

	private static final Map<UUID, BukkitRunnable> mLoreUpdateRunnables = new HashMap<>();

	private static final Set<UUID> mLock = new HashSet<>();

	public LootboxManager(Plugin plugin) {
		mPlugin = plugin;
	}

	/**
	 * Distribute a collection of items into a number of distinct buckets.
	 *
	 * Biggest challenge here is to get things sort of evenly distributed. A lot of
	 * different tuning is possible here.
	 *
	 * TODO: Someday it would be nice to have many invocations of this function
	 * somehow spread out rares evenly among players
	 * This would require some state keeping about who has gotten what. Maybe just
	 * counters based on tiers? Tricky...
	 *
	 * This is right now the most fair shuffle I can come up with
	 *
	 * Caller should take care not to actually change the returned lists, they are
	 * sublists of the original input list
	 */
	public static List<List<ItemStack>> distributeLootToBuckets(List<ItemStack> loot, int numBuckets) {
		// No reason to shuffle things around if numBuckets is 1
		if (numBuckets <= 1) {
			List<List<ItemStack>> buckets = new ArrayList<>(numBuckets);
			buckets.add(loot);
			return buckets;
		}

		List<Inventory> tempBuckets = new ArrayList<>(numBuckets);

		// Create empty buckets
		for (int bucketIndex = 0; bucketIndex < numBuckets; bucketIndex++) {
			tempBuckets.add(Bukkit.createInventory(null, 54));
		}

		if (MMLog.isLevelEnabled(Level.FINER)) {
			MMLog.finer("LOOTBOX: # of Buckets: " + numBuckets);
			MMLog.finer("LOOTBOX: Processing chest which contains:");
			loot.forEach((item) -> MMLog.finer("LOOTBOX:     " + item.toString()));
		}

		// Sort the Loot Inventory
		Inventory lootInventory = Bukkit.createInventory(null, 54);
		lootInventory.addItem(loot.toArray(new ItemStack[0]));
		ChestSortAPI.sortInventory(lootInventory);

		if (MMLog.isLevelEnabled(Level.FINER)) {
			MMLog.finer("LOOTBOX: Sorted chest which contains:");
			List<ItemStack> lootChest = Arrays.asList(lootInventory.getContents()).stream()
				.filter((item) -> !ItemUtils.isNullOrAir(item))
				.toList();
			lootChest.forEach((item) -> MMLog.finer("LOOTBOX:     " + item.toString()));
		}

		// Distribute even
		int bucketIdx = 0;
		for (ItemStack item : lootInventory.getContents()) {
			if (ItemUtils.isNullOrAir(item)) {
				continue;
			}
			int amount = item.getAmount();
			int amountThatSplitsEvenly = Math.floorDiv(amount, numBuckets);
			int amountThatDoesnt = Math.floorMod(amount, numBuckets);
			ItemStack clonedItem = item.asOne();
			// this item can be evenly split, therefore add the split to all buckets
			if (amountThatSplitsEvenly > 0) {
				ItemStack evenSplitItem = clonedItem.asQuantity(amountThatSplitsEvenly);
				for (Inventory inv : tempBuckets) {
					inv.addItem(evenSplitItem);
				}
			}
			// this item has leftovers, add it to the current bucket
			while (amountThatDoesnt > 0) {
				tempBuckets.get(bucketIdx).addItem(clonedItem);
				amountThatDoesnt--;
				bucketIdx++;
				if (bucketIdx >= numBuckets) {
					bucketIdx = 0;
				}
			}
		}

		// Shuffle the buckets themselves so the 1st player isn't more likely to get +1 item
		Collections.shuffle(tempBuckets, FastUtils.RANDOM);

		// Convert from an Inventory back into a List<ItemStack>
		List<List<ItemStack>> outputBuckets = new ArrayList<>();
		for (Inventory inv : tempBuckets) {
			// convert back to list and filter out null entries
			List<ItemStack> newBucket = Arrays.asList(inv.getContents()).stream()
				.filter((item) -> !ItemUtils.isNullOrAir(item))
				.toList();
			if (MMLog.isLevelEnabled(Level.FINER)) {
				MMLog.finer("LOOTBOX: Bucket contents");
				newBucket.forEach((item) -> MMLog.finer("LOOTBOX:     " + item.toString()));
			}
			outputBuckets.add(newBucket);
		}

		return outputBuckets;
	}

	/**
	 * Puts a player's fractional split of loot in a LOOTBOX in their inventory if
	 * present.
	 *
	 * Returns whether this was possible. If false the loot should be shared back
	 * into the generating chest.
	 */
	public static @Nullable List<ItemStack> giveShareToPlayer(ArrayList<ItemStack> loot, Player player) {
		ItemStack lootbox = getLootBox(player);
		// No lootbox space. Indicate to the caller that the items should be distributed
		// in the original chest
		if (lootbox == null) {
			return null;
		}

		// list of items that pass pickup filters
		List<ItemStack> filteredLoot = new ArrayList<>();
		// list of items that don't pass pickup filters
		List<ItemStack> rejectedLoot = new ArrayList<>();
		for (ItemStack item : loot) {
			if (ItemUtils.isNullOrAir(item)) {
				continue;
			}
			if (Plugin.getInstance().mJunkItemsListener.pickupFilter(player, item)) {
				rejectedLoot.add(item);
				continue;
			}
			filteredLoot.add(item);
		}
		createLootshare(player, lootbox, filteredLoot);

		// update the lootbox after a delay
		updateLootboxLoreDelay(player, lootbox, LOOTBOX_UPDATE_LORE_DELAY);

		return rejectedLoot;
	}

	/**
	 * Creates a lootshare inside the lootbox
	 *
	 * @param player - the player that "owns" this lootbox
	 * @param lootbox - the lootbox
	 * @param items - the list of items that are in the share
	 */
	public static void createLootshare(Player player, ItemStack lootbox, List<ItemStack> items) {
		NBTItem nbt = new NBTItem(lootbox);
		NBTCompound monumenta = nbt.addCompound(ItemStatUtils.MONUMENTA_KEY);
		NBTCompound playerModified = monumenta.addCompound(ItemStatUtils.PLAYER_MODIFIED_KEY);
		NBTCompound lootboxKey = playerModified.addCompound(LOOTBOX_KEY);
		NBTCompound index = lootboxKey.addCompound(LOOTBOX_INDEX_KEY);
		NBTCompoundList shares = lootboxKey.getCompoundList(LOOTBOX_SHARES_KEY);

		// create a new share
		createLootshareData(index, shares, items);
		// Refresh the lootbox item
		lootbox.setItemMeta(nbt.getItem().getItemMeta());
	}

	/**
	 * Gets the first lootshare from inside the lootbox
	 *
	 * @param player - the player that "owns" this lootbox
	 * @param lootbox - the lootbox
	 * @param remove - True if lootshare should be removed, false if not
	 * @return - List of items in the lootshare
	 */
	public static @Nullable List<ItemStack> getLootshare(Player player, ItemStack lootbox, boolean remove) {
		NBTItem nbt = new NBTItem(lootbox);
		NBTCompound monumenta = nbt.addCompound(ItemStatUtils.MONUMENTA_KEY);
		NBTCompound playerModified = monumenta.addCompound(ItemStatUtils.PLAYER_MODIFIED_KEY);
		NBTCompound lootboxKey = playerModified.addCompound(LOOTBOX_KEY);
		NBTCompound index = lootboxKey.addCompound(LOOTBOX_INDEX_KEY);
		NBTCompoundList shares = lootboxKey.getCompoundList(LOOTBOX_SHARES_KEY);
		if (shares == null || shares.isEmpty()) {
			return null;
		}

		List<ItemStack> items = getLootshareData(index, shares, 1);
		if (remove) {
			removeLootshareData(index, shares, 1);
		}

		lootbox.setItemMeta(nbt.getItem().getItemMeta());

		return items;
	}

	/**
	 * Removes the first lootshare from the lootbox
	 *
	 * @param player - the player that "owns" this lootbox
	 * @param lootbox - the lootbox
	 */
	public void removeLootshare(Player player, ItemStack lootbox) {
		NBTItem nbt = new NBTItem(lootbox);
		NBTCompound monumenta = nbt.addCompound(ItemStatUtils.MONUMENTA_KEY);
		NBTCompound playerModified = monumenta.addCompound(ItemStatUtils.PLAYER_MODIFIED_KEY);
		NBTCompound lootboxKey = playerModified.addCompound(LOOTBOX_KEY);
		NBTCompound index = lootboxKey.addCompound(LOOTBOX_INDEX_KEY);
		NBTCompoundList shares = lootboxKey.getCompoundList(LOOTBOX_SHARES_KEY);
		if (shares == null || shares.isEmpty()) {
			return;
		}

		removeLootshareData(index, shares, 1);

		lootbox.setItemMeta(nbt.getItem().getItemMeta());
	}

	/**
	 * Creates a new share from these items.
	 *
	 * @param index  - index of uuid to items & count
	 * @param shares - all shares
	 * @param items  - items to create a share with
	 */
	public static void createLootshareData(NBTCompound index, @Nullable NBTCompoundList shares, List<ItemStack> items) {
		if (shares == null) {
			return;
		}
		Map<String, Integer> shareItems = new HashMap<>();
		Map<String, Integer> indexAmountTotal = new HashMap<>();
		// create a new share
		NBTListCompound lootShare = shares.addCompound();
		// add all items to share
		// this accounts for duplicate items
		for (ItemStack item : items) {
			// ignore items that are invalid?
			if (ItemUtils.isNullOrAir(item) || item.getAmount() < 1) {
				continue;
			}
			// create new item (or use existing item if there are duplicates)
			NBTCompound itemData = getItemKey(index, item);
			String key = itemData.getName();
			int amount = item.getAmount();
			// add share amount to map
			int newShareAmount = Objects.requireNonNullElse(shareItems.get(key), 0) + amount;
			shareItems.put(key, newShareAmount);

			// add index amount to map
			int newAmount = Objects.requireNonNullElse(indexAmountTotal.get(key), itemData.getInteger(LOOTSHARE_AMOUNT_KEY)) + amount;
			indexAmountTotal.put(key, newAmount);
		}

		for (Map.Entry<String, Integer> entry : shareItems.entrySet()) {
			String key = entry.getKey();
			Integer amount = entry.getValue();

			lootShare.setInteger(key, amount);
		}

		for (Map.Entry<String, Integer> entry : indexAmountTotal.entrySet()) {
			String key = entry.getKey();
			Integer amount = entry.getValue();

			index.getCompound(key).setInteger(LOOTSHARE_AMOUNT_KEY, amount);
		}
	}

	/**
	 * Gets the specified number of shares and returns as a list of ItemStack
	 *
	 * @param index  - index of uuid to items & count
	 * @param shares - all shares
	 * @param count  - Number of shares to get
	 */
	public static @Nullable List<ItemStack> getLootshareData(NBTCompound index, @Nullable NBTCompoundList shares,
			int count) {
		if (shares == null || shares.isEmpty()) {
			return null;
		}
		// uuid: amount
		Map<String, Integer> itemAmountCap = new HashMap<>();
		// each share has an average of 10 items
		ArrayDeque<ItemStack> items = new ArrayDeque<>(count * 10);
		// get the desired amount of lootshares out of this
		for (int i = 0; i < count; i++) {
			// grab the lootshare
			NBTCompound lootShare = shares.get(i);

			// no more lootshares
			if (lootShare == null) {
				break;
			}

			// loop through all items in share
			Set<String> keys = lootShare.getKeys();
			for (String key : keys) {
				NBTCompound itemData = index.getCompound(key);

				// if the item doesn't exist in the lootbox anymore
				if (itemData == null) {
					continue;
				}

				int amount = lootShare.getInteger(key);
				// cap the max amount you can extract to the number of items in the index
				int maxAmount = Objects.requireNonNullElse(itemAmountCap.get(key), itemData.getInteger(LOOTSHARE_AMOUNT_KEY));
				// if there are no more items in the index, skip
				if (maxAmount <= 0) {
					continue;
				}
				itemAmountCap.put(key, Math.max(maxAmount - amount, 0));

				// add item to array
				ItemStack item = itemData.getItemStack(LOOTSHARE_ITEM_KEY);
				int maxItemAmount = Math.min(maxAmount, amount);
				item.setAmount(maxItemAmount);
				items.add(item);
			}
		}

		return new ArrayList<>(items);
	}

	/**
	 * Deletes the specified number of shares.
	 * Items inside the shares are removed from the index.
	 *
	 * @param index  - index of uuid to items & count
	 * @param shares - all shares
	 * @param count  - Number of shares to delete
	 */
	public static void removeLootshareData(NBTCompound index, @Nullable NBTCompoundList shares, int count) {
		if (shares == null || shares.isEmpty()) {
			return;
		}
		// uuid: amount
		Map<String, Integer> itemAmountTotal = new HashMap<>();
		// loop through specified number of shares
		for (int i = 0; i < count; i++) {
			// we don't want to use the count here as we are actively removing items
			ReadWriteNBT lootShare = shares.remove(0);
			// no more shares
			if (lootShare == null) {
				break;
			}

			// loop through all items in share
			Set<String> keys = lootShare.getKeys();
			for (String key : keys) {
				// store the item amount that needs to be removed
				int amount = lootShare.getInteger(key);
				int previousAmount = Objects.requireNonNullElse(itemAmountTotal.get(key), 0);
				itemAmountTotal.put(key, amount + previousAmount);
			}
		}

		// loop through all items
		// this breaks if there are any duplicate keys
		for (Map.Entry<String, Integer> entry : itemAmountTotal.entrySet()) {
			String key = entry.getKey();
			Integer amount = entry.getValue();

			NBTCompound itemData = index.getCompound(key);
			// if item doesn't exist, then our job is done!
			if (itemData == null) {
				continue;
			}

			int newAmount = itemData.getInteger(LOOTSHARE_AMOUNT_KEY) - amount;
			if (newAmount <= 0) {
				// delete item if amount is less than 1
				index.removeKey(key);
			} else {
				// otherwise set item to new amount
				itemData.setInteger(LOOTSHARE_AMOUNT_KEY, newAmount);
			}
		}
	}

	/**
	 * Get the item in the index if it exists.
	 * If it doesn't exist, create one.
	 *
	 * @param index - index of uuid to items & count
	 * @param item  - the item to index/check for
	 */
	private static NBTCompound getItemKey(NBTCompound index, ItemStack item) {
		// check if the item exists already
		String itemKey = getUUIDFromItem(item.asOne());
		NBTCompound container = getItemKeyFromItem(index, itemKey);
		if (container != null) {
			return container;
		}
		// otherwise create the item
		return createItemKey(index, item.asOne(), itemKey);
	}

	private static @Nullable NBTCompound getItemKeyFromItem(NBTCompound index, String itemKey) {
		if (index.hasKey(itemKey)) {
			return index.getCompound(itemKey);
		}
		return null;
	}

	private static NBTCompound createItemKey(NBTCompound index, ItemStack copy, String uuid) {
		// generate random uuid
		NBTCompound newEntry = index.addCompound(uuid);
		newEntry.setItemStack(LOOTSHARE_ITEM_KEY, copy);
		newEntry.setInteger(LOOTSHARE_AMOUNT_KEY, 0);
		return newEntry;
	}

	private static String getUUIDFromItem(ItemStack item) {
		return UUID.nameUUIDFromBytes(item.serializeAsBytes()).toString();
	}

	/**
	 * Creates a timer to update the lore after a certain amount of time.
	 * Cancels the previous timer if called again.
	 *
	 * @param player  - the player that owns the lootbox
	 * @param lootbox - the lootbox
	 * @param delay   - delay in ticks to wait | a delay of 0 will cancel the
	 *                previous timer and immediately update the lore
	 */
	public static void updateLootboxLoreDelay(Player player, ItemStack lootbox, long delay) {
		UUID uuid = player.getUniqueId();
		@Nullable
		BukkitRunnable previousRunnable = mLoreUpdateRunnables.remove(uuid);
		// if there is a lore update already running, cancel and remove it
		if (previousRunnable != null) {
			previousRunnable.cancel();
		}
		// if the delay is 0, just update the lore immediately
		if (delay <= 0) {
			updateLootboxLore(lootbox);
			return;
		}
		// otherwise update the lore after some time
		BukkitRunnable runnable = new BukkitRunnable() {
			@Override
			public void run() {
				updateLootboxLore(lootbox);
				mLoreUpdateRunnables.remove(uuid);
			}
		};
		mLoreUpdateRunnables.put(uuid, runnable);
		runnable.runTaskLater(Plugin.getInstance(), delay);
	}

	public static void updateLootboxLoreInventoryDelay(Player player, long delay) {
		UUID uuid = player.getUniqueId();
		@Nullable
		BukkitRunnable previousRunnable = mLoreUpdateRunnables.remove(uuid);
		// if there is a lore update already running, cancel and remove it
		if (previousRunnable != null) {
			previousRunnable.cancel();
		}
		// if the delay is 0, just update the lore immediately
		if (delay <= 0) {
			updateLootboxLoreInventory(player);
			return;
		}
		// otherwise update the lore after some time
		BukkitRunnable runnable = new BukkitRunnable() {
			@Override
			public void run() {
				updateLootboxLoreInventory(player);
				this.cancel();
				mLoreUpdateRunnables.remove(uuid);
			}
		};
		mLoreUpdateRunnables.put(uuid, runnable);
		runnable.runTaskLater(Plugin.getInstance(), delay);
	}

	public static void updateLootboxLoreInventory(Player player) {
		Inventory inventory = player.getInventory();
		for (ItemStack item : inventory.getStorageContents()) {
			if (ItemUtils.isNullOrAir(item) || !isLootbox(item)) {
				continue;
			}
			updateLootboxLore(item);
		}
	}

	/**
	 * Update the lore of the lootbox to be the current number of shares.
	 * Performance intensive! Use with caution!
	 */
	public static void updateLootboxLore(ItemStack lootbox) {
		if (!isLootbox(lootbox)) {
			return;
		}
		NBTItem nbt = new NBTItem(lootbox);
		NBTCompound monumenta = nbt.addCompound(ItemStatUtils.MONUMENTA_KEY);
		NBTCompound playerModified = monumenta.addCompound(ItemStatUtils.PLAYER_MODIFIED_KEY);
		NBTCompound lootboxKey = playerModified.addCompound(LOOTBOX_KEY);
		NBTCompoundList shares = lootboxKey.getCompoundList(LOOTBOX_SHARES_KEY);
		int previousShares = lootboxKey.getInteger(LOOTBOX_TOTAL_SHARES_KEY);

		if (shares != null && previousShares != shares.size()) {
			lootboxKey.setInteger(LOOTBOX_TOTAL_SHARES_KEY, shares.size());
			lootbox.setItemMeta(nbt.getItem().getItemMeta());
			updateLootboxLore(lootbox, shares.size());
		} else if ((shares == null || shares.isEmpty()) && previousShares != 0) {
			lootboxKey.setInteger(LOOTBOX_TOTAL_SHARES_KEY, 0);
			lootbox.setItemMeta(nbt.getItem().getItemMeta());
			updateLootboxLore(lootbox, 0);
		}
	}

	private static void updateLootboxLore(ItemStack lootbox, int amount) {
		int max = getLootboxMaxSize(lootbox);
		List<Component> lore = ItemStatUtils.getLore(lootbox);
		int loreIndex = lore.size() - 1; // just add and remove the end of the lootbox

		ItemStatUtils.removeLore(lootbox, loreIndex);
		ItemStatUtils.addLore(lootbox, loreIndex, Component.text(amount + "/" + max + " shares")
				.decoration(TextDecoration.ITALIC, false).color(NamedTextColor.WHITE));
		ItemStatUtils.generateItemStats(lootbox); // this is major performance killer
	}

	/**
	 * Get the max size of the lootbox. Uses TIER to determine size.
	 *
	 * @param lootbox - the lootbox
	 * @return size of lootbox
	 */
	private static int getLootboxMaxSize(ItemStack lootbox) {
		switch (ItemStatUtils.getTier(lootbox)) {
			case EPIC:
				return LOOTBOX_EPIC_MAX_SIZE;
			default:
				return LOOTBOX_NORMAL_MAX_SIZE;
		}
	}

	private static @Nullable ItemStack getLootBox(Player player) {
		ItemStack lootbox = null;
		int numAvailSpaces = 0;
		boolean foundLootBox = false;

		for (ItemStack item : player.getInventory().getContents()) {
			if (isLootbox(item)) {
				updateOldLootbox(item);
				foundLootBox = true;
				NBTItem nbt = new NBTItem(item);
				NBTCompound monumenta = nbt.addCompound(ItemStatUtils.MONUMENTA_KEY);
				NBTCompound playerModified = monumenta.addCompound(ItemStatUtils.PLAYER_MODIFIED_KEY);
				NBTCompound lootboxKey = playerModified.addCompound(LOOTBOX_KEY);
				NBTCompoundList items = lootboxKey.getCompoundList(LOOTBOX_SHARES_KEY);
				if (items != null) {
					int size = getLootboxMaxSize(item);
					numAvailSpaces = size - items.size();
					if (numAvailSpaces > 0) {
						lootbox = item;
					}
					break;
				}
			}
		}

		if (foundLootBox) {
			if (numAvailSpaces > LOOTBOX_WARN_FREE_SPACES) {
				// Plenty of space
				player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, SoundCategory.PLAYERS, 0.8f, 1.2f);
				MessagingUtils.sendActionBarMessage(player, "LOOTBOX chest added", NamedTextColor.GREEN);
			} else if (numAvailSpaces > 0) {
				// Only a few spaces left
				player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.PLAYERS, 0.5f, 1.5f);
				MessagingUtils.sendActionBarMessage(player, "LOOTBOX chest added, " + (numAvailSpaces - 1) + " spaces left",
						NamedTextColor.YELLOW);
			} else {
				// No space left
				player.playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.PLAYERS, 0.8f, 1.8f);
				MessagingUtils.sendActionBarMessage(player, "LOOTBOX is full", NamedTextColor.RED);
			}
		}

		return lootbox;
	}

	/**
	 * Updates an old lootbox to the new format.
	 * Ignores lootboxes with the new format.
	 * @param lootbox - the lootbox
	 * @return True if it was an old lootbox, false if not
	 */
	private static boolean updateOldLootbox(ItemStack lootbox) {
		NBTItem nbt = new NBTItem(lootbox);
		NBTCompound monumenta = nbt.addCompound(ItemStatUtils.MONUMENTA_KEY);
		NBTCompound playerModified = monumenta.addCompound(ItemStatUtils.PLAYER_MODIFIED_KEY);
		NBTCompound lootboxKey = playerModified.addCompound(LOOTBOX_KEY);
		if (lootboxKey.hasTag(LOOTBOX_INDEX_KEY) || lootboxKey.hasTag(LOOTBOX_SHARES_KEY)) {
			return false;
		}
		NBTCompound index = lootboxKey.addCompound(LOOTBOX_INDEX_KEY);
		NBTCompoundList shares = lootboxKey.getCompoundList(LOOTBOX_SHARES_KEY);
		if (isEpicLootbox(lootbox)) {
			NBTCompoundList items = playerModified.getCompoundList(ItemStatUtils.ITEMS_KEY);
			if (items != null) {
				for (ReadWriteNBT share : items) {
					ItemStack lootShare = NBT.itemStackFromNBT(share);
					if (lootShare == null) {
						continue;
					}

					if (lootShare.getItemMeta() instanceof BlockStateMeta shareBlockMeta
							&& shareBlockMeta.getBlockState() instanceof Chest chestMeta) {
						List<ItemStack> lootShareItems = Arrays.stream(chestMeta.getInventory().getContents())
								.filter((item) -> item != null && !item.getType().isAir())
								.collect(Collectors.toList());
						// since we are working directly with nbt, use underlying function instead
						createLootshareData(index, shares, lootShareItems);
					}
				}
				playerModified.removeKey(ItemStatUtils.ITEMS_KEY);
			}
		} else if (lootbox.getItemMeta() instanceof BlockStateMeta blockMeta
				&& blockMeta.getBlockState() instanceof ShulkerBox shulkerMeta && !shulkerMeta.getInventory().isEmpty()) {
			// Clear the lootbox's inventory
			ItemStack[] items = shulkerMeta.getInventory().getContents();
			for (@Nullable ItemStack share : items) {
				if (share == null || !share.getType().equals(Material.CHEST)) {
					continue;
				}
				if (share.getItemMeta() instanceof BlockStateMeta shareBlockMeta
						&& shareBlockMeta.getBlockState() instanceof Chest chestMeta) {
					List<ItemStack> lootShareItems = Arrays.stream(chestMeta.getInventory().getContents())
							.filter((item) -> item != null && !item.getType().isAir())
							.collect(Collectors.toList());
					// since we are working directly with nbt, use underlying function instead
					createLootshareData(index, shares, lootShareItems);
				}
			}
		}

		// update the lootbox data with nbt
		lootbox.setItemMeta(nbt.getItem().getItemMeta());
		// clear the shulker afterwards
		if (lootbox.getItemMeta() instanceof BlockStateMeta blockMeta
				&& blockMeta.getBlockState() instanceof ShulkerBox shulkerMeta) {
			shulkerMeta.getInventory().clear();
			blockMeta.setBlockState(shulkerMeta);
			lootbox.setItemMeta(blockMeta);
		}
		return true;
	}

	public static @Nullable ItemStack hasLootbox(Inventory inventory) {
		for (ItemStack item : inventory.getContents()) {
			if (isLootbox(item)) {
				return item;
			}
		}
		return null;
	}

	public static boolean isLootbox(ItemStack item) {
		if (item == null || !ItemUtils.isShulkerBox(item.getType()) || !item.hasItemMeta()) {
			return false;
		}

		String plainName = ItemUtils.getPlainNameIfExists(item);
		return plainName.equals("LOOTBOX") || plainName.equals("Box of Endless Echoes")
				|| plainName.equals("Mouth of the Mimic");
	}

	public static boolean isEpicLootbox(ItemStack item) {
		if (item == null || !ItemUtils.isShulkerBox(item.getType()) || !item.hasItemMeta()
				|| !item.getItemMeta().hasDisplayName()) {
			return false;
		}

		String plainName = ItemUtils.getPlainNameIfExists(item);
		return plainName.equals("Box of Endless Echoes") || plainName.equals("Mouth of the Mimic");
	}

	public static boolean isNormalLootbox(ItemStack item) {
		if (item == null || !ItemUtils.isShulkerBox(item.getType()) || !item.hasItemMeta()
				|| !item.getItemMeta().hasDisplayName()) {
			return false;
		}

		return ItemUtils.getPlainNameIfExists(item).equals("LOOTBOX");
	}

	public static void successSound(Player player) {
		player.playSound(player.getLocation(), Sound.BLOCK_CHEST_OPEN, SoundCategory.PLAYERS, 0.6f, 1f);
	}

	public static void errorSound(Player player) {
		player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
	}

	public static boolean isLocked(Player player) {
		return isLocked(player, false);
	}

	public static boolean isLocked(Player player, boolean silent) {
		UUID uuid = player.getUniqueId();
		if (mLock.contains(uuid)) {
			if (!silent) {
				MessagingUtils.sendError(player, "LOOTBOX is on cooldown!");
				errorSound(player);
			}
			return true;
		}
		mLock.add(uuid);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			mLock.remove(uuid);
		}, LOOTBOX_RATELIMIT_TICKS);
		return false;
	}

	public static boolean removeLock(Player player) {
		UUID uuid = player.getUniqueId();
		if (mLock.contains(uuid)) {
			mLock.remove(uuid);
			return true;
		}
		return false;
	}

	/**
	 * Handling for custom lootboxie!
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void inventoryClickEvent(InventoryClickEvent event) {
		Inventory inventory = event.getClickedInventory();
		ClickType click = event.getClick();
		ItemStack itemClicked = event.getCurrentItem();

		if (!(event.getWhoClicked() instanceof Player player) || // we want a player
				inventory == null || // with valid inventory
				!LootboxManager.isLootbox(itemClicked) // and they clicked a lootbox
		) {
			return;
		}

		if (isLocked(player)) {
			event.setCancelled(true);
			return;
		}

		switch (click) {
			case LEFT: {
				// update the old lootbox (and cancel event)
				if (updateOldLootbox(itemClicked)) {
					event.setCancelled(true);
					updateLootboxLoreDelay(player, itemClicked, LOOTBOX_UPDATE_LORE_DELAY);
				}
				break;
			}
			case RIGHT: {
				event.setCancelled(true);
				// Right-clicked a lootbox - dump contents into player's inventory
				updateOldLootbox(itemClicked);
				List<ItemStack> items = getLootshare(player, itemClicked, false);
				if (items == null) {
					// Lootbox empty
					MessagingUtils.sendError(player, "LOOTBOX is empty!");
					errorSound(player);
					updateLootboxLoreDelay(player, itemClicked, LOOTBOX_UPDATE_LORE_DELAY);
					break;
				}
				// determine which inventory to put the items into
				Inventory topInventory = player.getOpenInventory().getTopInventory();
				Inventory bottomInventory = player.getOpenInventory().getBottomInventory();
				Inventory selectedInventory = player.getInventory();
				Boolean avaliableSpace = false;
				if (ALLOWED_CONTAINERS.contains(topInventory.getType()) && topInventory.firstEmpty() > -1) {
					avaliableSpace = true;
					selectedInventory = topInventory;
				} else if (bottomInventory.getType().equals(InventoryType.PLAYER) && bottomInventory.firstEmpty() > -1) {
					avaliableSpace = true;
					selectedInventory = bottomInventory;
				} else if (selectedInventory.firstEmpty() > -1) {
					avaliableSpace = true;
				}
				// if there is no avaliable space in these inventories then do not give the
				// player their items
				if (!avaliableSpace) {
					MessagingUtils.sendError(player, "Cannot open lootshare. Inventory is full!");
					errorSound(player);
					updateLootboxLoreDelay(player, itemClicked, LOOTBOX_UPDATE_LORE_DELAY);
					break;
				}
				removeLootshare(player, itemClicked);
				successSound(player);
				for (ItemStack item : items) {
					if (ItemUtils.isNullOrAir(item)) {
						continue;
					}
					// this may occur if the player changes their drop filter later in their session
					if (mPlugin.mJunkItemsListener.pickupFilter(player, item)) {
						InventoryUtils.dropTempOwnedItem(item, player.getLocation(), player);
						continue;
					}
					InventoryUtils.giveItem(player, item, selectedInventory, false);
				}
				updateLootboxLoreDelay(player, itemClicked, LOOTBOX_UPDATE_LORE_DELAY);
				break;
			}
			// TODO: add mode/config here per player?
			case SWAP_OFFHAND: {
				event.setCancelled(true);
				updateLootboxLoreDelay(player, itemClicked, LOOTBOX_UPDATE_LORE_DELAY);
				GUIUtils.refreshOffhand(event);
				break;
			}
			default: {
				removeLock(player);
				break;
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = false)
	public void blockPlaceEvent(BlockPlaceEvent event) {
		Player player = event.getPlayer();
		Block block = event.getBlockPlaced();
		ItemStack itemHeld = event.getItemInHand();

		if (!isLootbox(itemHeld)) {
			return;
		}
		event.setCancelled(true);
		event.setBuild(false);

		if (isLocked(player)) {
			return;
		}

		@Nullable
		List<ItemStack> contents = getLootshare(player, itemHeld, true);
		if (contents == null) {
			// LootBox is empty
			MessagingUtils.sendError(player, "LOOTBOX is empty!");
			errorSound(player);
			return;
		}

		updateLootboxLoreInventoryDelay(player, LOOTBOX_UPDATE_LORE_DELAY);

		// Get the new chest and update that
		Bukkit.getScheduler().runTask(mPlugin, () -> {
			// Clears contents
			block.setType(Material.CHEST);

			if (block.getState() instanceof Chest chest) {
				chest.update();

				chest = (Chest) block.getState();
				ChestUtils.generateLootInventory(contents, chest.getInventory(), player, true);

				successSound(player);

				CoreProtectIntegration.logPlacement(player, chest.getLocation(), chest.getBlockData().getMaterial(),
						chest.getBlockData());
			}
		});
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	public void playerInteractEvent(PlayerInteractEvent event) {
		Block clickedBlock = event.getClickedBlock();
		ItemStack itemHeld = event.getItem();
		Player player = event.getPlayer();
		if (event.getAction() != Action.LEFT_CLICK_BLOCK || // left clicking a chest
				!isLootbox(itemHeld) ||
				clickedBlock == null ||
				ChestUtils.isChestWithLootTable(clickedBlock) // without a loottable
		) {
			return;
		}

		event.setCancelled(true);
		if (isLocked(player)) {
			return;
		}
		@Nullable
		List<ItemStack> contents = getLootshare(player, itemHeld, false);
		if (contents == null) {
			MessagingUtils.sendError(player, "LOOTBOX is empty!");
			errorSound(player);
			return;
		}

		BlockState state = clickedBlock.getState();
		if (!(state instanceof InventoryHolder)) {
			MessagingUtils.sendError(player, "This is not a valid container!");
			errorSound(player);
			return;
		}

		// make sure to only be acting on chests
		Inventory inventory = ((InventoryHolder) state).getInventory();
		if (inventory instanceof DoubleChestInventory chest) {
			inventory = (DoubleChestInventory) chest;
		} else {
			if (!ALLOWED_CONTAINERS.contains(inventory.getType())) {
				// return if not a chest?
				MessagingUtils.sendError(player, "This is not a valid container!");
				errorSound(player);
				return;
			}
		}

		// make sure it fits in inventory
		if (inventory.firstEmpty() == -1) {
			MessagingUtils.sendError(player, "Cannot open lootshare! Chest is full!");
			errorSound(player);
			return;
		}

		successSound(player);
		removeLootshare(player, itemHeld);
		updateLootboxLoreInventoryDelay(player, LOOTBOX_UPDATE_LORE_DELAY);
		for (ItemStack item : contents) {
			if (ItemUtils.isNullOrAir(item)) {
				continue;
			}
			// this may occur if the player changes their drop filter later in their session
			if (mPlugin.mJunkItemsListener.pickupFilter(player, item)) {
				InventoryUtils.dropTempOwnedItem(item, player.getLocation(), player);
				continue;
			}
			InventoryUtils.giveItem(player, item, inventory, false);
		}
	}
}
