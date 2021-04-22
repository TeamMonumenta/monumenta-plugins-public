package com.playmonumenta.plugins.enchantments;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.StatTrack.StatTrackOptions;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

public class StatTrackManager {

	//The system will attempt to update the item for one minute after it's first used
	public static final int NUM_RETRIES = 60;

	public static final int PATRON_TIER = 5;

	private static StatTrackManager mInstance;

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
	public void scheduleDelayedStatUpdate(ItemStack item, Player player, StatTrackOptions enchant, int amount) {
		//Make sure the item has a display name we can use
		if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
			return;
		}
		//Check if the player that is updating the stat is the same one
		if (mStatUpdates.contains(item.getItemMeta().displayName().toString(), player.getUniqueId())) {
			//The item is in the system, so we add our increment to the amount waiting to be updated
			mStatUpdates.put(item.getItemMeta().displayName().toString(), player.getUniqueId(), new Integer(mStatUpdates.get(item.getItemMeta().displayName().toString(), player.getUniqueId()).intValue() + amount));
			//Reset the timeout timer on the runnable
			StatDelayedUpdateCheck runnable = mStatRunnables.get(item.getItemMeta().displayName().toString(), player.getUniqueId());
			if (runnable != null) {
				runnable.mRetries = 0;
			}
		} else {
			//The item is not in the system, schedule a delayed update check for it
			StatDelayedUpdateCheck delayedUpdate = new StatDelayedUpdateCheck(item, player, enchant);
			mStatRunnables.put(item.getItemMeta().displayName().toString(), player.getUniqueId(), delayedUpdate);
			//Run check every second
			delayedUpdate.runTaskTimer(Plugin.getInstance(), 0, 20);
			//Add number to update to the map
			mStatUpdates.put(item.getItemMeta().displayName().toString(), player.getUniqueId(), new Integer(amount));
		}
	}

	/**
	 * Increments the stat by getting the current value on the item, and increasing it
	 * by the amount currently stored in the map
	 * @param item the item to update
	 * @param enchant which stat option to adjust
	 */
	public void incrementStatInternal(String itemName, StatTrackOptions enchant, Player player) {

		//Get the item from the player inventory with the given name
		ItemStack item = getStatTrackItemFromInventory(player, itemName, enchant);
		if (item == null) {
			return;
		}
		//Get the old stat on the item
		int stat = parseValue(item, enchant);
		//Add the stats waiting to update
		Integer num = mStatUpdates.get(item.getItemMeta().displayName().toString(), player.getUniqueId());
		if (num == null) {
			return;
		}

		stat += num.intValue();

		//Replace the old line of lore with the new one
		List<String> lore = item.getLore();
		List<String> newLore = new ArrayList<>();
		for (String line : lore) {
			if (line.contains(enchant.getEnchantName())) {
				line = ChatColor.RED + enchant.getEnchantName() + ": " + stat;
			}
			newLore.add(line);
		}
		item.setLore(newLore);

		//Remove the item from the system
		mStatUpdates.remove(item.getItemMeta().displayName().toString(), player.getUniqueId());
	}

	/**
	 * Parses the current stat value on the item
	 * @param item the item to parse
	 * @param enchant the enchant to check the stat for
	 * @return the number of the stat
	 */
	public static int parseValue(ItemStack item, StatTrackOptions enchant) {
		List<String> lore = item.getLore();
		int stat = 0;
		for (String line : lore) {
			if (line.contains(enchant.getEnchantName())) {
				Matcher matcher = Pattern.compile("\\d+").matcher(line);
				matcher.find();
				stat = Integer.valueOf(matcher.group());
			}
		}
		return stat;
	}

	/**
	 * Performs the logic of updating the item by changing it to a new stat
	 * This method verifies the player is an active patron and is on the item
	 * @param item the item to update
	 * @param player the player to check scores for
	 * @param enchant the stat type to change
	 * @param stat the numerical value of the stat
	 */
	public static void incrementStat(ItemStack item, Player player, StatTrackOptions enchant, int amount) {

		//Check that the player is a patron and the item has their name on it and that it has the right enchant type
		if (ScoreboardUtils.getScoreboardValue(player, "Patreon") < PATRON_TIER || !isPlayersItem(item, player) || !(getTrackingType(item).equals(enchant))) {
			return;
		}
		//Call the manager to update the stat when the player is done using the item
		StatTrackManager.getInstance().scheduleDelayedStatUpdate(item, player, enchant, amount);
	}

	/**
	 * Gets the active stat tracked enchant on the given item
	 * @param item item to check for
	 * @return the stat track enchant on the given item, or null if none found
	 */
	public static StatTrackOptions getTrackingType(ItemStack item) {
		List<String> lore = item.getLore();

		for (String line : lore) {
			for (StatTrackOptions stat : StatTrackOptions.values()) {
				if (line.contains(stat.getEnchantName())) {
					return stat;
				}
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
		List<String> lore = item.getLore();

		for (String line : lore) {
			if (line.contains("Tracked by") && line.contains(player.getName())) {
				return true;
			}

		}
		return false;
	}

	/**
	 * This method parses the player inventory to get the most current version of the item to update
	 * @param player player to update
	 * @param name name of the item to get back
	 * @param stat stat to make sure is on the item
	 * @return the item to update
	 */
	public static ItemStack getStatTrackItemFromInventory(Player player, String name, StatTrackOptions stat) {
		Inventory i = player.getInventory();
		for (ItemStack item : i.getContents()) {
			if (item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
				continue;
			}
			if (item.getItemMeta().displayName().toString().equals(name) && getTrackingType(item) == stat) {
				return item;
			}
		}
		return null;
	}

	/**
	 * This runnable checks once per second to see if the user has switched to holding a new item
	 * After 60 seconds of not switching the item or being able to update it, it is removed from the map
	 *
	 */
	public class StatDelayedUpdateCheck extends BukkitRunnable {

		Player mPlayer;
		String mItemName;
		StatTrackOptions mEnchant;
		//How many attempts we have made at updating the item
		int mRetries;

		public StatDelayedUpdateCheck(ItemStack item, Player player, StatTrackOptions enchant) {
			mPlayer = player;
			mItemName = item.getItemMeta().displayName().toString();
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

				if (currentItem.getItemMeta().displayName().toString().equals(mItemName)) {
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
