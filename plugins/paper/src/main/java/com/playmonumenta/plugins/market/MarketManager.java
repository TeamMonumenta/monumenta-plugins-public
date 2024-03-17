package com.playmonumenta.plugins.market;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.inventories.WalletManager;
import com.playmonumenta.plugins.itemstats.infusions.Shattered;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.utils.FileUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.WalletUtils;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.redissync.event.PlayerSaveEvent;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.nbtapi.iface.ReadableNBTList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.OptionalInt;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;

public class MarketManager {

	public static final MarketManager INSTANCE = new MarketManager();
	private static final String KEY_PLUGIN_DATA = "Market";
	private static HashMap<Player, MarketPlayerData> mMarketPlayerDataInstances = new HashMap<>();

	public MarketConfig mConfig = new MarketConfig();

	public static MarketManager getInstance() {
		return INSTANCE;
	}

	public static void reloadConfig() {
		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			MarketManager instance = MarketManager.getInstance();
			String json = null;
			try {
				json = FileUtils.readFile(Plugin.getInstance().getDataFolder().getPath() + "/market.json");
			} catch (Exception e) {
				MMLog.severe("Caught Market plugin exception while loading the config : ", e);
			}
			if (json == null) {
				instance.mConfig = new MarketConfig();
				instance.mConfig.mIsMarketOpen = false;
			} else {
				instance.mConfig = new Gson().fromJson(json, MarketConfig.class);
			}
		});
	}

	public static MarketConfig getConfig() {
		return MarketManager.getInstance().mConfig;
	}

	public static void claimClaimable(Player player, MarketListing listing) {
		// WARNING: Call this in an async thread
		MarketListing oldListing = MarketRedisManager.getListing(listing.getId());
		if (oldListing.getAmountToClaim() == 0) {
			// nothing to claim
			return;
		}
		MarketListing newListing = new MarketListing(oldListing);
		int amountToGive = oldListing.getAmountToClaim() * oldListing.getAmountToBuy();
		newListing.setAmountToClaim(0);
		if (!MarketRedisManager.updateListingSafe(player, oldListing, newListing)) {
			return;
		}
		Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
			InventoryUtils.giveItemWithStacksizeCheck(player, listing.getItemToBuy().asQuantity(amountToGive));
		});
		MarketAudit.logClaim(player, listing, amountToGive);
	}

	public static void unlockListing(Player player, MarketListing listing) {
		// WARNING: Call this in an async thread
		MarketListing oldListing = MarketRedisManager.getListing(listing.getId());
		MarketListing newListing = new MarketListing(oldListing);
		newListing.setLocked(false);
		MarketRedisManager.updateListingSafe(player, oldListing, newListing);
		MarketListingIndex.ACTIVE_LISTINGS.addListingToIndexIfMatching(newListing);
		MarketAudit.logUnlockAction(player, listing);
	}

	public static void lockListing(Player player, MarketListing listing) {
		// WARNING: Call this in an async thread
		MarketListing oldListing = MarketRedisManager.getListing(listing.getId());
		MarketListing newListing = new MarketListing(oldListing);
		newListing.setLocked(true);
		MarketRedisManager.updateListingSafe(player, oldListing, newListing);
		MarketListingIndex.ACTIVE_LISTINGS.removeListingFromIndex(newListing);
		MarketAudit.logLockAction(player, listing);
	}

	public static void claimEverythingAndDeleteListing(Player player, MarketListing listing) {
		// WARNING: Call this in an async thread
		MarketListing oldListing = MarketRedisManager.getListing(listing.getId());
		MarketListing newListing = new MarketListing(oldListing);
		int currencyToGive = oldListing.getAmountToClaim() * oldListing.getAmountToBuy();
		int itemsToGive = oldListing.getAmountToSellRemaining();
		newListing.setAmountToClaim(0);
		newListing.setAmountToSellRemaining(0);
		if (MarketRedisManager.updateListingSafe(player, oldListing, newListing)) {
			MarketPlayerData marketPlayerData = mMarketPlayerDataInstances.get(player);
			if (marketPlayerData != null) {
				marketPlayerData.removeListingIDFromPlayer(newListing.getId());
			}
			MarketRedisManager.deleteListing(newListing);
			Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
				InventoryUtils.giveItemWithStacksizeCheck(player, newListing.getItemToBuy().asQuantity(currencyToGive));
				InventoryUtils.giveItemWithStacksizeCheck(player, newListing.getItemToSell().asQuantity(itemsToGive));
			});
			MarketAudit.logClaimAndDelete(player, newListing, itemsToGive, currencyToGive);
		}
	}

	public static void expireListing(Player player, MarketListing listing) {
		// WARNING: Call this in an async thread
		MarketListing oldListing = MarketRedisManager.getListing(listing.getId());
		MarketListing newListing = new MarketListing(oldListing);
		newListing.setExpired(true);
		MarketRedisManager.updateListingSafe(player, oldListing, newListing);
		MarketAudit.logExpire(player, listing);
	}

	public static void unexpireListing(Player player, MarketListing listing) {
		// WARNING: Call this in an async thread
		MarketListing oldListing = MarketRedisManager.getListing(listing.getId());
		MarketListing newListing = new MarketListing(oldListing);
		newListing.setExpired(false);
		MarketRedisManager.updateListingSafe(player, oldListing, newListing);
		MarketAudit.logUnexpire(player, listing);
	}

	public static List<String> itemIsSellable(Player mPlayer, ItemStack currentItem, ItemStack currency) {
		ArrayList<String> errors = new ArrayList<>();

		if (Shattered.isShattered(currentItem)) {
			errors.add("You cannot sell a shattered item. Repair it first!");
		}

		ReadableNBTList<ReadWriteNBT> containedItems = ItemUtils.getContainerItems(currentItem);
		if (containedItems != null && !containedItems.isEmpty()) {
			errors.add("You cannot sell an item containing other items. Empty it first!");
		}

		ReadableNBTList<String> pages = ItemUtils.getPages(currentItem);
		if (pages != null && !pages.isEmpty()) {
			errors.add("You cannot sell a book-like item containing filled pages. Empty it first!");
		}

		List<Component> signContents = ItemUtils.getSignContents(currentItem);
		if (signContents != null) {
			for (Component c : signContents) {
				if (!MessagingUtils.plainText(c).isEmpty()) {
					errors.add("You cannot sell a sign that contains text.");
					break;
				}
			}
		}

		if (ItemUtils.getDamagePercent(currentItem) > 0.0f) {
			errors.add("You cannot sell damaged items. Repair it first!");
		}

		if (currency != null) {
			WalletManager.CompressionInfo infoCurrentItem = WalletManager.getAsMaxUncompressed(currentItem);
			ItemStack tmpCurrentItem = currentItem.asOne();
			if (infoCurrentItem != null) {
				tmpCurrentItem = infoCurrentItem.mBase.asOne();
			}
			WalletManager.CompressionInfo infoCurrencyItem = WalletManager.getAsMaxUncompressed(currency);
			ItemStack tmpCurrencyItem = currency.asOne();
			if (infoCurrencyItem != null) {
				tmpCurrencyItem = infoCurrencyItem.mBase.asOne();
			}
			if (ItemUtils.getPlainName(tmpCurrencyItem).equals(ItemUtils.getPlainName(tmpCurrentItem))) {
				errors.add("You cannot sell items with currency being the same item itself. Change either the used currency, or the item!");
			}
		}

		return errors;
	}

	public static void openNewMarketGUI(Player player) {
		OptionalInt bannedScore = ScoreboardUtils.getScoreboardValue(player, "MarketPluginBanned");
		OptionalInt magentaScore = ScoreboardUtils.getScoreboardValue(player, "Magenta");
		String error = null;
		if (magentaScore.isEmpty() || magentaScore.getAsInt() == 0) {
			error = "You need to have completed the Magenta Wool Dungeon to use the Marketplace. Aim for the 'Roots of the Plague' quest to unlock this Dungeon.";
		} else if (bannedScore.isPresent() && bannedScore.getAsInt() != 0) {
			error = "You are currently banned from the player market. Contact a moderator if you believe this is wrong, or for an appeal.";
		} else if (!player.hasPermission("monumenta.marketaccess")) {
			error = "You do not have market access. Try again later, or contact a moderator if you believe this is not normal.";
		} else if (!MarketManager.getInstance().mConfig.mIsMarketOpen) {
			error = "Market is currently closed.";
		}

		if (error != null) {
			player.sendMessage(Component.text(error, NamedTextColor.RED));
			player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1, 1);
		} else {
			new MarketGUI(player, MarketGUI.MarketGuiTab.MAIN_MENU).open();
		}
	}

	public static void lockAllListings(Player player) {
		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			player.sendMessage("Resyncing all indexes");
			MarketListingIndex.resyncAllIndexes();
			player.sendMessage("Fetching listing list");
			List<Long> listings = MarketListingIndex.ACTIVE_LISTINGS.getListingsFromIndex(null, false);
			player.sendMessage("Going through the " + listings.size() + "active listings");
			int amountLocked = 0;
			int amountErrors = 0;
			for (Long id : listings) {
				MarketListing oldListing = MarketRedisManager.getListing(id);
				MarketListing newListing = new MarketListing(oldListing);
				newListing.setLocked(true);
				if (MarketRedisManager.updateListingSafe(player, oldListing, newListing)) {
					amountLocked++;
				} else {
					amountErrors++;
				}
			}
			player.sendMessage("Resyncing all indexes");
			MarketListingIndex.resyncAllIndexes();
			player.sendMessage(Component.text("LockAll action finished", NamedTextColor.GREEN));
			player.sendMessage(amountLocked + " listings locked");
			MarketAudit.logLockAllAction(player, amountLocked);
			if (amountErrors > 0) {
				player.sendMessage(Component.text(amountErrors + " listings could not be locked. they were probably being used by other players. see logs above. If that number is too high, re running the LockAll action might fix, if not, contact ray, and if the locking is urgent, close the market.", NamedTextColor.RED));
			}

		});
	}

	public void playerJoinEvent(PlayerJoinEvent event) {
		// delay the data fetch by 5 ticks, as variables are not updated until onJoin event is called
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			MarketPlayerData marketPlayerData = mMarketPlayerDataInstances.get(event.getPlayer());
			if (marketPlayerData == null) {
				Plugin.getInstance().getLogger().warning("ERROR FAILED TO LOAD MARKET DATA OF " + event.getPlayer().getName() + ": NO MARKET INSTANCE");
				return;
			}
			UUID uuid = event.getPlayer().getUniqueId();
			JsonObject data = MonumentaRedisSyncAPI.getPlayerPluginData(uuid, KEY_PLUGIN_DATA);
			if (data != null) {
				// load the owned player listings
				for (JsonElement elem : data.getAsJsonArray("playerListings")) {
					marketPlayerData.addListingIDToPlayer(elem.getAsString());
				}
			}
		}, 5L);

		// initialise what we can
		if (mMarketPlayerDataInstances == null) {
			mMarketPlayerDataInstances = new HashMap<>();
		}
		MarketPlayerData marketPlayerData = new MarketPlayerData();
		mMarketPlayerDataInstances.put(event.getPlayer(), marketPlayerData);
	}

	public void playerSaveEvent(PlayerSaveEvent event) {
		MarketPlayerData marketPlayerData = mMarketPlayerDataInstances.get(event.getPlayer());
		if (marketPlayerData == null) {
			Plugin.getInstance().getLogger().warning("ERROR FAILED TO SAVE MARKET DATA OF " + event.getPlayer().getName() + ": NO MARKET INSTANCE");
			AuditListener.logMarket("ERROR FAILED TO SAVE MARKET DATA OF " + event.getPlayer().getName() + ": NO MARKET INSTANCE");
			return;
		}
		JsonObject data = new JsonObject();
		JsonArray array = new JsonArray();
		for (Long id : marketPlayerData.getOwnedListingsIDList()) {
			if (id != null) {
				array.add(String.valueOf(id));
			}
		}
		data.add("playerListings", array);
		event.setPluginData(KEY_PLUGIN_DATA, data);
	}

	public void onLogout(Player player) {
		// delay the data removal by 10 ticks, as we need it for the playersave event, launched after logout event
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> mMarketPlayerDataInstances.remove(player), 10L);
	}

	// Verifies that the player has the item that he wants to sell. take it from them.
	//  if so, attempts to create a listing in redis.
	//  on success, link that listing to the player data and take the item from the player,
	//  on fail, give back the item
	// returns true if the listing creation was successful, false otherwise
	//
	// this method may take some time, due to the call to redis
	// the usage of that method in an async environment is thus heavily recommended
	public void addNewListing(Player player, ItemStack itemToSell, int amountToSell, int pricePerItemAmount, ItemStack currencyItemStack, WalletUtils.Debt taxDebt) {

		// check that the item about to be sold is actually sellable
		List<String> errorMessages = MarketManager.itemIsSellable(player, itemToSell, currencyItemStack);
		if (!errorMessages.isEmpty()) {
			for (String message : errorMessages) {
				player.sendMessage(Component.text("Something went wrong: " + message + ". listing creation cancelled", NamedTextColor.RED).decoration(TextDecoration.BOLD, true));
			}
		}

		// check that the player has the items they want to sell
		if (!player.getInventory().containsAtLeast(itemToSell, amountToSell)) {
			player.sendMessage("Something went wrong: you do not have the listing items in your inventory. listing creation cancelled");
			return;
		}

		if (!WalletUtils.tryToPayFromInventoryAndWallet(player, taxDebt.mItem.asQuantity(taxDebt.mTotalRequiredAmount), true, true)) {
			player.sendMessage("Something went wrong: you do not have enough money to pay the tax. listing creation cancelled");
			return;
		}

		// remove the items from player inventory
		HashMap<?, ?> failedToRemove = player.getInventory().removeItem(itemToSell.asQuantity(amountToSell));
		if (!failedToRemove.isEmpty()) {
			player.sendMessage("Something went wrong: Failed to remove the listing items from your inventory. listing creation cancelled");
			// destroy the already existing listing
			return;
		}

		MarketListing createdListing = MarketRedisManager.createAndAddNewListing(player, itemToSell, amountToSell, pricePerItemAmount, currencyItemStack);
		if (createdListing == null) {
			// creation failed on the redis side
			player.sendMessage("Something went wrong: Server failed to create the listing. You need to contact a moderator for tax refund. amount is given in logs");
			AuditListener.logMarket("!ERROR! Player " + player.getName() + "needs a " + taxDebt.mTotalRequiredAmount + "*" + ItemUtils.getPlainName(taxDebt.mItem) + "tax refund, because the listing failed to be created in redis");
			return;
		}
		MarketManager.getInstance().linkListingToPlayerData(player, createdListing.getId());
		MarketAudit.logCreate(player, createdListing, taxDebt);
	}

	public static void performPurchase(Player player, MarketListing listing, int amount) {
		// WARNING: Call this in an async thread

		MarketListing oldListing = MarketRedisManager.getListing(listing.getId());
		MarketListing newListing = new MarketListing(oldListing);

		// buyability checks
		MarketListingStatus purchasableStatus = oldListing.getPurchasableStatus(amount);
		if (purchasableStatus.isError()) {
			player.sendMessage(purchasableStatus.getFormattedAssociatedMessage());
			return;
		}

		ItemStack currency = oldListing.getItemToBuy().clone();
		currency.setAmount(oldListing.getAmountToBuy() * amount);
		WalletUtils.Debt debt = WalletUtils.calculateInventoryAndWalletDebt(currency, player, true);

		if (!debt.mMeetsRequirement) {
			player.sendMessage(Component.text("You don't have enough currency to purchase this."));
			return;
		}

		// update the listing in redis
		newListing.setAmountToSellRemaining(oldListing.getAmountToSellRemaining() - amount);
		newListing.setAmountToClaim(oldListing.getAmountToClaim() + amount);
		if (!MarketRedisManager.updateListingSafe(player, oldListing, newListing)) {
			player.sendMessage(Component.text("Impossible to buy listing: Update failed"));
			return;
		}

		if (newListing.getPurchasableStatus(1).isError()) {
			// the new listing values makes it so the listing is not able to be bought anymore
			// remove it from the active_listings index, does not need to be instant.
			Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
				MarketListingIndex.ACTIVE_LISTINGS.removeListingFromIndex(listing);
			});
		}

		// give items to player
		Bukkit.getScheduler().scheduleSyncDelayedTask(Plugin.getInstance(), () -> {
			WalletUtils.payDebt(debt, player, true);
			Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
				InventoryUtils.giveItemWithStacksizeCheck(player, oldListing.getItemToSell().asQuantity(amount));
			});
			player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 0.7f);
			MarketAudit.logBuyAction(player, oldListing, amount, debt.mTotalRequiredAmount, debt.mItem);
		});

	}

	public void linkListingToPlayerData(Player player, long listingID) {
		MarketPlayerData marketPlayerData = mMarketPlayerDataInstances.get(player);
		if (marketPlayerData == null) {
			Plugin.getInstance().getLogger().warning("ERROR: FAILED TO GET MARKET DATA OF " + player.getName() + ": NO MARKET INSTANCE. CONTACT A MODERATOR IMMEDIATELY, SOMETHING IS WRONG WITH YOUR PLUGIN DATA");
			return;
		}
		marketPlayerData.addListingIDToPlayer(listingID);
	}

	public void unlinkListingFromPlayerData(Player player, long listingID) {
		MarketPlayerData marketPlayerData = mMarketPlayerDataInstances.get(player);
		if (marketPlayerData == null) {
			Plugin.getInstance().getLogger().warning("ERROR: FAILED TO GET MARKET DATA OF " + player.getName() + ": NO MARKET INSTANCE. CONTACT A MODERATOR IMMEDIATELY, SOMETHING IS WRONG WITH YOUR PLUGIN DATA");
			return;
		}
		marketPlayerData.removeListingIDFromPlayer(listingID);
	}

	public List<Long> getListingsOfPlayer(Player player) {
		MarketPlayerData marketPlayerData = mMarketPlayerDataInstances.get(player);
		if (marketPlayerData == null) {
			Plugin.getInstance().getLogger().warning("ERROR: FAILED TO GET MARKET DATA OF " + player.getName() + ": NO MARKET INSTANCE. CONTACT A MODERATOR IMMEDIATELY, SOMETHING IS WRONG WITH YOUR PLUGIN DATA");
			return new ArrayList<>();
		}
		return marketPlayerData.getOwnedListingsIDList();
	}

	public WalletUtils.Debt calculateTaxDebt(Player player, ItemStack currencyItem, int amount) {
		// find an applicable de-compressed alternative to the given currency, for more precision
		double famount = amount;
		WalletManager.CompressionInfo compressionInfo = WalletManager.getCompressionInfo(currencyItem);
		while (compressionInfo != null && famount * compressionInfo.mAmount < Integer.MAX_VALUE) {
			currencyItem = compressionInfo.mBase.asOne();
			famount *= compressionInfo.mAmount;
			compressionInfo = WalletManager.getCompressionInfo(currencyItem);
		}
		// at this point, we should have the most decompressed possible currency, with an appropriately scaled amount
		// apply tax rate
		famount = Math.ceil(famount * MarketManager.getConfig().mBazaarTaxRate);

		// calculate the debt
		WalletUtils.Debt debt = WalletUtils.calculateInventoryAndWalletDebt(currencyItem.asQuantity((int)famount), player, true);

		if (famount <= 0) { //overflow security
			player.sendMessage("The amount is too low. please increase it");
			player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1, 1);
			debt = new WalletUtils.Debt(debt.mItem, debt.mTotalRequiredAmount, Integer.MAX_VALUE, Integer.MAX_VALUE, false, 0, 0);
		}

		if (famount >= (long)Integer.MAX_VALUE) { //overflow security
			player.sendMessage("The amount is too much. please reduce it");
			player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1, 1);
			debt = new WalletUtils.Debt(debt.mItem, debt.mTotalRequiredAmount, Integer.MAX_VALUE, Integer.MAX_VALUE, false, 0, 0);
		}

		return debt;
	}
}
