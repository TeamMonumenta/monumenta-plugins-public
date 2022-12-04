package com.playmonumenta.plugins.itemstats.infusions;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.player.PlayerData;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import com.playmonumenta.plugins.utils.ItemUtils;
import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;


public class StatTrackManager {

	//The system will attempt to update the item for one minute after it's first used
	public static final int NUM_RETRIES = 60;

	public static final int PATRON_TIER = Constants.PATREON_TIER_1;

	private static @Nullable StatTrackManager mInstance;

	//A table that stores the amount to update each item's stat
	//The first key is the item name and the second key is the player uuid
	public Table<String, UUID, Integer> mStatUpdates;
	//A table that stores the runnable active for the item
	public Table<String, UUID, StatDelayedUpdateCheck> mStatRunnables;


	private StatTrackManager() {
		mStatUpdates = HashBasedTable.create();
		mStatRunnables = HashBasedTable.create();
	}

	public static StatTrackManager getInstance() {
		if (mInstance == null) {
			mInstance = new StatTrackManager();
		}

		return mInstance;
	}

	/**
	 * This method is called by the stat tracking enchantment. The method will
	 * check if the item is already waiting to be updated and either add the stat
	 * to the system, or start a new runnable to wait to be updated
	 * @param item the item to update
	 * @param player the player who owns the item
	 * @param enchant the stat option to adjust
	 * @param amount the amount to increment the stat
	 */
	public void scheduleDelayedStatUpdate(ItemStack item, Player player, InfusionType enchant, int amount) {
		//Make sure the item has a display name we can use
		if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
			return;
		}
		String itemName = getItemName(item);
		//Check if the player that is updating the stat is the same one
		if (mStatUpdates.contains(itemName, player.getUniqueId())) {
			//The item is in the system, so we add our increment to the amount waiting to be updated
			mStatUpdates.put(
				itemName,
				player.getUniqueId(),
				mStatUpdates.get(itemName, player.getUniqueId()).intValue() + amount
			);
			//Reset the timeout timer on the runnable
			StatDelayedUpdateCheck runnable = mStatRunnables.get(itemName, player.getUniqueId());
			if (runnable != null) {
				runnable.mRetries = 0;
			}
		} else {
			//The item is not in the system, schedule a delayed update check for it
			StatDelayedUpdateCheck delayedUpdate = new StatDelayedUpdateCheck(item, player, enchant);
			mStatRunnables.put(itemName, player.getUniqueId(), delayedUpdate);
			//Run check every second
			delayedUpdate.runTaskTimer(Plugin.getInstance(), 0, 20);
			//Add number to update to the map
			mStatUpdates.put(
				itemName,
				player.getUniqueId(),
				amount
			);
		}
	}

	/**
	 * Increments the stat by getting the current value on the item, and increasing it
	 * by the amount currently stored in the map
	 * @param itemName the item to update
	 * @param enchant which stat option to adjust
	 */
	public void incrementStatInternal(String itemName, InfusionType enchant, Player player) {
		//Get the item from the player inventory with the given name
		ItemStack item = getStatTrackItemFromInventory(player, itemName, enchant);
		if (item == null) {
			return;
		}
		//Get the old stat on the item
		int stat = ItemStatUtils.getInfusionLevel(item, enchant);
		//Add the stats waiting to update
		Integer num = mStatUpdates.get(itemName, player.getUniqueId());
		if (num == null) {
			return;
		}

		stat += num.intValue();

		//Replace the old line of lore with the new one
		ItemStatUtils.addInfusion(item, enchant, stat, player.getUniqueId());

		//Remove the item from the system
		mStatUpdates.remove(itemName, player.getUniqueId());
	}

	/**
	 * Performs the logic of updating the item by changing it to a new stat
	 * This method verifies the player is an active patron and is on the item
	 * @param item the item to update
	 * @param player the player to check scores for
	 * @param enchant the stat type to change
	 * @param amount the numerical value of the stat
	 */
	public static void incrementStat(ItemStack item, Player player, InfusionType enchant, int amount) {
		//Check that the player is a patron and the item has their name on it and that it has the right enchant type
		if (
			PlayerData.getPatreonDollars(player) < PATRON_TIER
					|| !isPlayersItem(item, player)
					|| getTrackingType(item) != enchant
		) {
			return;
		}
		//Call the manager to update the stat when the player is done using the item
		StatTrackManager.getInstance().scheduleDelayedStatUpdate(item, player, enchant, amount);
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
	 * @param item the stat tracking item
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

	/**
	 * This method parses the player inventory to get the most current version of the item to update
	 *
	 * @param player player to update
	 * @param name   name of the item to get back
	 * @param stat   stat to make sure is on the item
	 * @return the item to update
	 */
	public static @Nullable ItemStack getStatTrackItemFromInventory(Player player, String name, InfusionType stat) {
		Inventory i = player.getInventory();
		for (ItemStack item : i.getContents()) {
			if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
				continue;
			}
			if (getItemName(item).startsWith(name) && getTrackingType(item) == stat) {
				return item;
			}
		}
		return null;
	}

	/**
	 * NOTES: hard coded for "Alchemist's Bag (X)" since the name change base on the numbers of charge.
	 * a better solution should be found if other items of this kind are created
	 */
	private static String getItemName(ItemStack item) {
		String itemName = ItemUtils.getPlainName(item);
		itemName = itemName.startsWith("Alchemist's Bag") ? "Alchemist's Bag" : itemName;
		return itemName;
	}

	/**
	 * This runnable checks once per second to see if the user has switched to holding a new item
	 * After 60 seconds of not switching the item or being able to update it, it is removed from the map
	 *
	 */
	public static class StatDelayedUpdateCheck extends BukkitRunnable {

		Player mPlayer;
		String mItemName;
		InfusionType mEnchant;
		//How many attempts we have made at updating the item
		int mRetries;

		public StatDelayedUpdateCheck(ItemStack item, Player player, InfusionType enchant) {
			mPlayer = player;
			mItemName = getItemName(item);
			mEnchant = enchant;
		}

		@Override
		public void run() {
			try {
				if (mItemName == null) {
					this.cancel();
					return;
				}
				//Check if the player has switched items
				ItemStack currentItem = mPlayer.getInventory().getItemInMainHand();
				//If the player is not holding an item or is offline, update the item
				if (!currentItem.hasItemMeta() || !currentItem.getItemMeta().hasDisplayName() || !mPlayer.isOnline()) {
					//Not holding an item- go straight to success
					StatTrackManager.getInstance().incrementStatInternal(mItemName, mEnchant, mPlayer);
					StatTrackManager.getInstance().mStatRunnables.remove(mItemName, mPlayer.getUniqueId());
					this.cancel();
					return;
				}

				if (getItemName(currentItem).startsWith(mItemName)) {
					//The player is still using the item, so we'll try again later
					mRetries++;
					if (mRetries > StatTrackManager.NUM_RETRIES) {
						//Time to give up if we've been doing this for too long
						tearDown();
					}
				} else {
					//It's safe to update the item
					StatTrackManager.getInstance().incrementStatInternal(mItemName, mEnchant, mPlayer);
					StatTrackManager.getInstance().mStatRunnables.remove(mItemName, mPlayer.getUniqueId());
					this.cancel();
				}
			} catch (Exception e) {
				//Log exception
				Plugin.getInstance().getLogger().severe("Error in stat tracking: " + e.getMessage());
				e.printStackTrace();
				//Something broke, let's kill the process gracefully so they can use their item again later
				tearDown();
			}
		}

		//Remove the item from the system
		public void tearDown() {
			StatTrackManager.getInstance().mStatUpdates.remove(mItemName, mPlayer.getUniqueId());
			StatTrackManager.getInstance().mStatRunnables.remove(mItemName, mPlayer.getUniqueId());
			this.cancel();
		}
	}
}
