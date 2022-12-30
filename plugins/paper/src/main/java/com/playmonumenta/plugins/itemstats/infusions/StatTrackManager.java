package com.playmonumenta.plugins.itemstats.infusions;

import com.destroystokyo.paper.event.player.PlayerArmorChangeEvent;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.player.PlayerData;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.NmsUtils;
import io.papermc.paper.event.player.PlayerItemFrameChangeEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerArmorStandManipulateEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;


public class StatTrackManager implements Listener {

	public static final int PATRON_TIER = Constants.PATREON_TIER_1;

	private static final StatTrackManager INSTANCE = new StatTrackManager();

	private final Map<UUID, List<StatTrackData>> mData = new HashMap<>();

	private static class StatTrackData {
		private final ItemStack mItemStack;
		private final ItemStack mOriginalItemStackClone;
		private final InfusionType mInfusion;
		private int mUncommittedAmount = 0;

		StatTrackData(ItemStack itemStack, InfusionType infusion) {
			mItemStack = itemStack;
			mOriginalItemStackClone = ItemUtils.clone(itemStack);
			mInfusion = infusion;
		}
	}

	private StatTrackManager() {
	}

	public static StatTrackManager getInstance() {
		return INSTANCE;
	}

	private @Nullable StatTrackData getData(Player player, @Nullable ItemStack item) {
		if (ItemUtils.isNullOrAir(item)) {
			return null;
		}
		List<StatTrackData> dataList = mData.get(player.getUniqueId());
		if (dataList != null) {
			for (StatTrackData data : dataList) {
				if (NmsUtils.getVersionAdapter().isSameItem(data.mItemStack, item)) {
					return data;
				}
			}
		}
		return null;
	}

	private static void incrementStat(ItemStack item, StatTrackData data, Player player) {
		int oldStat = ItemStatUtils.getInfusionLevel(item, data.mInfusion);
		ItemStatUtils.addInfusion(item, data.mInfusion, oldStat + data.mUncommittedAmount, player.getUniqueId());
	}

	/**
	 * Updates the given item with currently accumulated, but uncommitted stat track info.
	 */
	public void update(Player player, @Nullable ItemStack item) {
		if (item == null || ItemUtils.isNullOrAir(item)) {
			return;
		}
		List<StatTrackData> dataList = mData.get(player.getUniqueId());
		if (dataList != null) {
			for (Iterator<StatTrackData> iterator = dataList.iterator(); iterator.hasNext(); ) {
				StatTrackData data = iterator.next();
				if (NmsUtils.getVersionAdapter().isSameItem(data.mItemStack, item)) {
					incrementStat(item, data, player);
					iterator.remove();
				}
			}
		}
	}

	/**
	 * Updates all items in the player's inventory, and also clears any cached data.
	 */
	public void updateInventory(Player player) {
		List<StatTrackData> dataList = mData.remove(player.getUniqueId());
		if (dataList != null) {
			dataLoop:
			for (StatTrackData data : dataList) {
				for (ItemStack item : player.getInventory()) {
					if (NmsUtils.getVersionAdapter().isSameItem(data.mItemStack, item)) {
						incrementStat(item, data, player);
						continue dataLoop;
					}
				}
				// On quit and on death (and possibly in other cases), items are updated and are no longer the same items,
				// so check for equality with the initial item stack instead (ignoring potential durability changes)
				for (ItemStack item : player.getInventory()) {
					if (item != null && ItemUtils.equalIgnoringDurability(item, data.mOriginalItemStackClone)) {
						incrementStat(item, data, player);
						continue dataLoop;
					}
				}
				MMLog.warning("Could not update stat track for player " + player.getDisplayName() + ": item='" + ItemUtils.getPlainName(data.mOriginalItemStackClone)
					              + "', infusion='" + data.mInfusion.getName() + "', lost stat track amount=" + data.mUncommittedAmount);
			}
		}
	}

	/**
	 * This method is called by the stat tracking enchantment. The method will
	 * check if the item is already waiting to be updated and either add the stat
	 * to the system, or start a new runnable to wait to be updated
	 *
	 * @param item    the item to update
	 * @param player  the player who owns the item
	 * @param enchant the stat option to adjust
	 * @param amount  the amount to increment the stat
	 */
	private void scheduleDelayedStatUpdate(ItemStack item, Player player, InfusionType enchant, int amount) {
		if (player.getGameMode() == GameMode.CREATIVE || player.getGameMode() == GameMode.SPECTATOR) {
			// Creative inventory breaks item tracking, plus also why would you need this? Spectator makes no sense too.
			return;
		}
		StatTrackData data = getData(player, item);
		if (data == null) {
			data = new StatTrackData(item, enchant);
			mData.computeIfAbsent(player.getUniqueId(), key -> new ArrayList<>()).add(data);
		}
		data.mUncommittedAmount += amount;
	}

	/**
	 * Immediately applies the given stat track to the item. Only use this if the item is not in the player's hands, or will be updated anyway.
	 */
	public void incrementStatImmediately(ItemStack item, Player player, InfusionType enchant, int amount) {
		int oldStat = ItemStatUtils.getInfusionLevel(item, enchant);
		if (oldStat != 0 && !ItemStatUtils.hasInfusion(item, InfusionType.STAT_TRACK)) {
			// fix broken items from a past bug
			ItemStatUtils.removeInfusion(item, enchant);
			return;
		}
		if (oldStat == 0 || !isPlayersItem(item, player)) {
			return;
		}
		List<StatTrackData> dataList = mData.get(player.getUniqueId());
		if (dataList != null) {
			for (Iterator<StatTrackData> iterator = dataList.iterator(); iterator.hasNext(); ) {
				StatTrackData data = iterator.next();
				if (NmsUtils.getVersionAdapter().isSameItem(data.mItemStack, item)) {
					ItemStatUtils.addInfusion(item, enchant, oldStat + amount + data.mUncommittedAmount, player.getUniqueId());
					iterator.remove();
					return;
				}
			}
		}
		ItemStatUtils.addInfusion(item, enchant, oldStat + amount, player.getUniqueId());
	}

	/**
	 * Performs the logic of updating the item by changing it to a new stat
	 * This method verifies the player is an active patron and is on the item
	 *
	 * @param item    the item to update
	 * @param player  the player to check scores for
	 * @param enchant the stat type to change
	 * @param amount  the numerical value of the stat
	 */
	public static void incrementStat(ItemStack item, Player player, InfusionType enchant, int amount) {
		// Check that the player is a patron and the item has their name on it and that it has the right enchant type
		if (amount != 0
			    && PlayerData.getPatreonDollars(player) >= PATRON_TIER
			    && isPlayersItem(item, player)
			    && ItemStatUtils.getInfusionLevel(item, enchant) > 0) {
			// Call the manager to update the stat when the player is done using the item
			INSTANCE.scheduleDelayedStatUpdate(item, player, enchant, amount);
		}
	}

	/**
	 * Gets the active stat tracked enchant on the given item
	 *
	 * @param item item to check for
	 * @return the stat track enchant on the given item, or null if none found
	 */
	public static @Nullable InfusionType getTrackingType(ItemStack item) {
		for (InfusionType type : InfusionType.STAT_TRACK_OPTIONS) {
			if (ItemStatUtils.getInfusionLevel(item, type) > 0) {
				return type;
			}
		}
		return null;
	}

	/**
	 * Returns true if the stat tracked item is owned by the player
	 *
	 * @param item   the stat tracking item
	 * @param player the player to check for
	 * @return whether the player owns the item's tracking
	 */
	public static boolean isPlayersItem(ItemStack item, Player player) {
		if (player == null || item == null) {
			return false;
		}
		UUID infuser = ItemStatUtils.getInfuser(item, InfusionType.STAT_TRACK);
		return player.getUniqueId().equals(infuser);
	}

	// ----- event handlers -----

	// Update entire inventory on any inventory click (to have updated items before they might leave the player's inventory)
	// Also handle cancelled events to update items on first click even if in a GUI for example, as the player's item may be changed by the GUI.
	// LOWEST priority to update items before any GUI might use them.
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void inventoryClickEvent(InventoryClickEvent event) {
		if (event.getWhoClicked() instanceof Player player) {
			updateInventory(player);
		}
	}

	// Update entire inventory on inventory close (because why not - the item update cannot interrupt the player here)
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void inventoryCloseEvent(InventoryCloseEvent event) {
		if (event.getPlayer() instanceof Player player) {
			updateInventory(player);
		}
	}

	// Update both main and offhand items when the player swaps them
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void playerSwapHandItemsEvent(PlayerSwapHandItemsEvent event) {
		update(event.getPlayer(), event.getMainHandItem());
		update(event.getPlayer(), event.getOffHandItem());
	}

	// Update armor when the player (un)equips it
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void playerArmorChangeEvent(PlayerArmorChangeEvent event) {
		update(event.getPlayer(), event.getNewItem());
		update(event.getPlayer(), event.getOldItem());
	}

	// Update an item when the player swaps away from it
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void playerItemHeldEvent(PlayerItemHeldEvent event) {
		update(event.getPlayer(), event.getPlayer().getInventory().getItem(event.getPreviousSlot()));
	}

	// Update an item when putting it on an armor stand
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void playerArmorStandManipulateEvent(PlayerArmorStandManipulateEvent event) {
		update(event.getPlayer(), event.getPlayerItem());
	}

	// Update an item when putting it in an item frame
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void playerItemFrameChangeEvent(PlayerItemFrameChangeEvent event) {
		update(event.getPlayer(), event.getItemStack());
	}

	// Update an item when dropping it
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void playerDropItemEvent(PlayerDropItemEvent event) {
		update(event.getPlayer(), event.getItemDrop().getItemStack());
	}

	// Update entire inventory when a player quits
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void playerQuitEvent(PlayerQuitEvent event) {
		updateInventory(event.getPlayer());
	}

	// Update entire inventory on death
	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
	public void playerDeathEvent(PlayerDeathEvent event) {
		updateInventory(event.getPlayer());
	}

	// Update the consumed item (for the case of infinite consumables), need to also handle cancelled events as some consumables cancel it
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void playerItemConsumeEvent(PlayerItemConsumeEvent event) {
		update(event.getPlayer(), event.getReplacement());
		update(event.getPlayer(), event.getItem());
	}

	// Update entire inventory when a player switches to creative or spectator (creative inventory breaks item tracking)
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void playerGameModeChangeEvent(PlayerGameModeChangeEvent event) {
		if (event.getNewGameMode() == GameMode.CREATIVE || event.getNewGameMode() == GameMode.SPECTATOR) {
			updateInventory(event.getPlayer());
		}
	}

}
