package com.playmonumenta.plugins.market;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.inventories.WalletManager;
import com.playmonumenta.plugins.itemstats.enchantments.CurseOfEphemerality;
import com.playmonumenta.plugins.itemstats.enums.Tier;
import com.playmonumenta.plugins.itemstats.infusions.Shattered;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.managers.LootboxManager;
import com.playmonumenta.plugins.market.filters.ComponentConfig;
import com.playmonumenta.plugins.market.filters.MarketFilter;
import com.playmonumenta.plugins.market.gui.MarketGui;
import com.playmonumenta.plugins.market.gui.TabBazaarBrowserState;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.FileUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.WalletUtils;
import com.playmonumenta.redissync.MonumentaRedisSyncAPI;
import com.playmonumenta.redissync.event.PlayerSaveEvent;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.nbtapi.iface.ReadableNBTList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

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
			MarketManager instance = getInstance();
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
		return getInstance().mConfig;
	}

	public static boolean claimClaimable(Player player, long id) {
		// WARNING: Call this in an async thread
		MarketListing oldListing = MarketRedisManager.getListing(id);
		if (oldListing.getAmountToClaim() == 0) {
			// nothing to claim
			return false;
		}
		if (oldListing.getEditLocked() != null) {
			return false;
		}
		MarketRedisManager.EditedListing editedListing = MarketRedisManager.atomicEditListing2(id, l -> l.setAmountToClaim(0));
		if (editedListing == null) {
			return false;
		}
		MarketListing listingBeforeUpdate = editedListing.mBeforeEdit();
		int amountToGive = listingBeforeUpdate.getAmountToClaim() * listingBeforeUpdate.getAmountToBuy();
		Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
			WalletManager.giveCurrencyToPlayer(player, listingBeforeUpdate.getCurrencyToBuy().asQuantity(amountToGive), true);
		});
		MarketAudit.logClaim(player, listingBeforeUpdate, amountToGive);
		return true;
	}

	public static boolean unlockListing(Player player, MarketListing listing) {
		// WARNING: Call this in an async thread
		MarketListing newListing = MarketRedisManager.atomicEditListing(listing.getId(), l -> l.setLocked(false));
		if (newListing == null) {
			return false;
		}
		MarketListingIndex.ACTIVE_LISTINGS.addListingToIndexIfMatching(newListing);
		MarketAudit.logUnlockAction(player, listing);
		return true;
	}

	public static boolean lockListing(Player player, MarketListing listing) {
		// WARNING: Call this in an async thread
		MarketListing newListing = MarketRedisManager.atomicEditListing(listing.getId(), l -> l.setLocked(true));
		if (newListing == null) {
			return false;
		}
		MarketListingIndex.ACTIVE_LISTINGS.removeListingFromIndex(newListing);
		MarketAudit.logLockAction(player, listing);
		return true;
	}

	public static boolean claimEverythingAndDeleteListing(Player player, long id) {
		// WARNING: Call this in an async thread
		MarketRedisManager.EditedListing editedListing = MarketRedisManager.atomicEditListing2(id, l -> {
			l.setAmountToClaim(0);
			l.setAmountToSellRemaining(0);
		});
		if (editedListing == null) {
			return false;
		}
		MarketListing listingBeforeUpdate = editedListing.mBeforeEdit();
		MarketListing newListing = editedListing.mAfterEdit();
		int currencyToGive = listingBeforeUpdate.getAmountToClaim() * listingBeforeUpdate.getAmountToBuy();
		int itemsToGive = listingBeforeUpdate.getAmountToSellRemaining() * listingBeforeUpdate.getBundleSize();
		MarketPlayerData marketPlayerData = mMarketPlayerDataInstances.get(player);
		if (marketPlayerData != null) {
			marketPlayerData.removeListingIDFromPlayer(newListing.getId());
		}
		MarketRedisManager.deleteListing(newListing);
		getInstance().unlinkListingFromPlayerData(player, newListing.getId());
		Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
			WalletManager.giveCurrencyToPlayer(player, newListing.getCurrencyToBuy().asQuantity(currencyToGive), true);
			InventoryUtils.giveItemWithStacksizeCheck(player, newListing.getItemToSell().asQuantity(itemsToGive));
		});
		MarketAudit.logClaimAndDelete(player, newListing, itemsToGive, currencyToGive);
		return true;
	}

	public static boolean expireListing(Player player, MarketListing listing, String reason) {
		// WARNING: Call this in an async thread
		MarketListing newListing = MarketRedisManager.atomicEditListing(listing.getId(), l -> l.setExpired(true));
		if (newListing == null) {
			return false;
		}
		MarketListingIndex.ACTIVE_LISTINGS.removeListingFromIndex(newListing);
		MarketAudit.logExpire(player, listing, reason);
		return true;
	}

	public static boolean unexpireListing(Player player, MarketListing listing) {
		// WARNING: Call this in an async thread
		MarketListing newListing = MarketRedisManager.atomicEditListing(listing.getId(), l -> l.setExpired(false));
		if (newListing == null) {
			return false;
		}
		MarketListingIndex.ACTIVE_LISTINGS.addListingToIndexIfMatching(newListing);
		MarketAudit.logUnexpire(player, listing);
		return true;
	}

	public static List<String> itemIsSellable(Player mPlayer, ItemStack currentItem, ItemStack currency) {
		ArrayList<String> errors = new ArrayList<>();

		if (CurseOfEphemerality.isEphemeral(currentItem)) {
			errors.add("You cannot sell items with Curse of Ephemerality.");
		}

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
		if (LootboxManager.isLootbox(currentItem)) {
			if (LootboxManager.getLootshare(mPlayer, currentItem, false) != null) {
				errors.add("You cannot sell a Lootbox that contains items.");
			}
		}
		if (currentItem.getType() == Material.WRITTEN_BOOK || currentItem.getType() == Material.WRITABLE_BOOK) { // WRITABLE_BOOK is a book and quill.
			if (ItemStatUtils.getTier(currentItem) == Tier.NONE) {
				errors.add("You cannot sell player-made books."); // direct the player to the correct place to advertise.
			} // Otherwise we'd be blocking things like Wolfswood tome. We assume players cannot apply arbitrary tiers to random items.
		}

		if (ItemUtils.getDamagePercent(currentItem) > 0.0f) {
			errors.add("You cannot sell damaged items. Repair it first!");
		}

		if (currency != null) {
			WalletManager.CompressionInfo infoCurrentItem = WalletManager.getCompressionInfo(currentItem);
			ItemStack tmpCurrentItem = currentItem.asOne();
			if (infoCurrentItem != null) {
				tmpCurrentItem = infoCurrentItem.mBase.asOne();
			}
			WalletManager.CompressionInfo infoCurrencyItem = WalletManager.getCompressionInfo(currency);
			ItemStack tmpCurrencyItem = currency.asOne();
			if (infoCurrencyItem != null) {
				tmpCurrencyItem = infoCurrencyItem.mBase.asOne();
			}
			if (ItemUtils.getPlainName(tmpCurrencyItem).equals(ItemUtils.getPlainName(tmpCurrentItem))) {
				errors.add("You cannot sell items with currency being the same item itself. Change either the used currency, or the item!");
			}
		}

		NBT.get(currentItem, nbt -> {
			ReadableNBTList<ReadWriteNBT> itemsList = ItemStatUtils.getItemList(nbt);
			if (itemsList != null && !itemsList.isEmpty()) {
				errors.add("You cannot sell items which contain other items (quiver, lootbox, etc). Empty the item first!");
			}
		});

		return errors;
	}

	public static void openNewMarketGUI(Player player) {
		String error = null;
		if (ScoreboardUtils.getScoreboardValue(player, "White").orElse(0) == 0
			|| ScoreboardUtils.getScoreboardValue(player, "Orange").orElse(0) == 0
			|| ScoreboardUtils.getScoreboardValue(player, "Magenta").orElse(0) == 0
		) {
			error = "You need to have completed the White, Orange, and Magenta Wool Dungeons to access the Marketplace.";
		} else if (ScoreboardUtils.getScoreboardValue(player, "MarketPluginBanned").orElse(0) != 0) {
			error = "You are currently banned from the player market. Contact a moderator if you believe this is wrong, or for an appeal.";
		} else if (!player.hasPermission("monumenta.marketaccess")) {
			error = "You do not have market access. Try again later, or contact a moderator if you believe this is not normal.";
		} else if (!getInstance().mConfig.mIsMarketOpen) {
			error = "Market is currently closed.";
		}

		if (error != null) {
			player.sendMessage(Component.text(error, NamedTextColor.RED));
			player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1, 1);
		} else {
			new MarketGui(player).open();
		}
	}

	public static void lockAllListings(Player player) {
		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			player.sendMessage("Resyncing all indexes");
			MarketListingIndex.resyncAllIndexes();
			player.sendMessage("Fetching listing list");
			List<Long> listings = MarketListingIndex.ACTIVE_LISTINGS.getListingsFromIndex(false);
			player.sendMessage("Going through the " + listings.size() + "active listings");
			int amountLocked = 0;
			int amountErrors = 0;
			for (Long id : listings) {
				String oldListingRaw = MarketRedisManager.getListingRaw(id);
				MarketListing oldListing = MarketRedisManager.parseListing(oldListingRaw);
				MarketListing newListing = new MarketListing(oldListing);
				newListing.setLocked(true);
				if (oldListing.getEditLocked() == null
					    && MarketRedisManager.atomicCompareAndSwapListing(oldListingRaw, newListing)) {
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

	// band-aid method to unlink the listings from a player;
	// if a 'listingID' is in idArray, but the matching listing is not found in 'listings'
	// then the listingID will be removed from the player owned listings
	public static void unlinkListingsFromPlayerIfNotInList(Player player, List<Long> idList, List<MarketListing> listings) {
		ArrayList<Long> unmetIDs = new ArrayList<>();
		for (Long id : idList) {
			boolean found = false;
			for (MarketListing listing : listings) {
				if (listing != null && listing.getId() == id) {
					found = true;
					break;
				}
			}
			if (!found) {
				unmetIDs.add(id);
			}
		}

		for (Long id : unmetIDs) {
			getInstance().unlinkListingFromPlayerData(player, id);
		}
	}

	public static void handleListingExpiration(Player player, MarketListing listing) {
		if (!listing.isExpired()) {
			if (listing.isExpirationDateInPast()) {
				expireListing(player, listing, "old age");
				listing.setExpired(true);
			}
		}
	}

	public static List<MarketFilter> getPlayerMarketFilters(Player player) {
		MarketPlayerData marketPlayerData = mMarketPlayerDataInstances.get(player);
		if (marketPlayerData == null) {
			Plugin.getInstance().getLogger().warning("ERROR: FAILED TO GET MARKET DATA OF " + player.getName() + ": NO MARKET INSTANCE. CONTACT A MODERATOR IMMEDIATELY, SOMETHING IS WRONG WITH YOUR PLUGIN DATA");
			return new ArrayList<>();
		}
		return marketPlayerData.getPlayerFiltersList();
	}

	public static MarketPlayerOptions getMarketPlayerOptions(Player player) {
		MarketPlayerData marketPlayerData = mMarketPlayerDataInstances.get(player);
		if (marketPlayerData == null) {
			Plugin.getInstance().getLogger().warning("ERROR: FAILED TO GET MARKET DATA OF " + player.getName() + ": NO MARKET INSTANCE. CONTACT A MODERATOR IMMEDIATELY, SOMETHING IS WRONG WITH YOUR PLUGIN DATA");
			return new MarketPlayerOptions();
		}
		MarketPlayerOptions marketPlayerOptions = marketPlayerData.getPlayerOptions();
		if (marketPlayerOptions == null) {
			marketPlayerOptions = new MarketPlayerOptions();
			marketPlayerData.setPlayerOptions(marketPlayerOptions);
		}
		return marketPlayerOptions;
	}

	public static void setPlayerMarketFilters(Player player, List<MarketFilter> playerFilters) {
		MarketPlayerData marketPlayerData = mMarketPlayerDataInstances.get(player);
		if (marketPlayerData == null) {
			Plugin.getInstance().getLogger().warning("ERROR: FAILED TO GET MARKET DATA OF " + player.getName() + ": NO MARKET INSTANCE. CONTACT A MODERATOR IMMEDIATELY, SOMETHING IS WRONG WITH YOUR PLUGIN DATA");
			return;
		}
		marketPlayerData.setPlayerFiltersList(playerFilters);
	}

	public static TabBazaarBrowserState getTabBazaarBrowserState(Player player) {
		MarketPlayerData marketPlayerData = mMarketPlayerDataInstances.get(player);
		if (marketPlayerData == null) {
			Plugin.getInstance().getLogger().warning("ERROR: FAILED TO GET MARKET DATA OF " + player.getName() + ": NO MARKET INSTANCE. CONTACT A MODERATOR IMMEDIATELY, SOMETHING IS WRONG WITH YOUR PLUGIN DATA");
			return new TabBazaarBrowserState(null);
		}
		return marketPlayerData.getTabBazaarBrowserState();
	}

	// calls redis -> use in an async env.
	public static void collectAllAndRemoveExpiredFromListingList(Player player, List<MarketListing> listingsList) {

		int listingsClaimed = 0;
		int listingsExpired = 0;

		for (MarketListing listing : listingsList) {
			// get a fresh version of the listing
			listing = MarketRedisManager.getListing(listing.getId());
			if (listing.isExpired()) {
				if (claimEverythingAndDeleteListing(player, listing.getId())) {
					listingsExpired++;
				}
			} else if (listing.getAmountToClaim() > 0) {
				if (claimClaimable(player, listing.getId())) {
					listingsClaimed++;
				}
			}
		}

		player.sendMessage(String.format("Operation finished:%s%s%s%s.",
			(listingsClaimed == 0 && listingsExpired == 0 ? " Nothing claimed. this... shouldn't have happened? if you can reproduce it, bugreport" : ""),
			(listingsClaimed > 0 ? " Claimed money from " + listingsClaimed + " listing" + (listingsClaimed > 1 ? "s" : "") : ""),
			(listingsClaimed > 0 && listingsExpired > 0 ? " and " : ""),
			(listingsExpired > 0 ? " Collected and deleted " + listingsExpired + " listing" + (listingsExpired > 1 ? "s" : "") : "")
			));

	}

	public void playerJoinEvent(PlayerJoinEvent event) {

		// initialise what we can
		if (mMarketPlayerDataInstances == null) {
			mMarketPlayerDataInstances = new HashMap<>();
		}

		UUID uuid = event.getPlayer().getUniqueId();
		JsonObject data = MonumentaRedisSyncAPI.getPlayerPluginData(uuid, KEY_PLUGIN_DATA);
		MarketPlayerData marketPlayerData = MarketPlayerData.fromJson(data);
		mMarketPlayerDataInstances.put(event.getPlayer(), marketPlayerData);

		// launch the notification calculation/display, for later
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			notificationCalcAndDisplay(event.getPlayer());
		}, 5 * 20L /*5 seconds*/);

	}

	private void notificationCalcAndDisplay(Player player) {

		ArrayList<Component> messages = new ArrayList<>();
		Component marketHeader = Component.text("[MARKET] ", NamedTextColor.YELLOW).decoration(TextDecoration.BOLD, true);
		List<Long> ownedListingsID = null;

		String shardName = ServerProperties.getShardName().toLowerCase(Locale.ROOT);
		MarketPlayerOptions options = getMarketPlayerOptions(player);

		MarketPlayerOptions.NotificationShard shardsForNotif = options.getShardsForNotification();
		if (shardsForNotif == MarketPlayerOptions.NotificationShard.ALWAYS
			|| (shardsForNotif == MarketPlayerOptions.NotificationShard.OVERWORLD && (shardName.contains("valley") || shardName.contains("isles") || shardName.contains("ring") || shardName.contains("plots") || shardName.contains("dev1")))
			|| (shardsForNotif == MarketPlayerOptions.NotificationShard.PLOTS && shardName.contains("plots"))) {
			ownedListingsID = getListingsOfPlayer(player);
		}

		if (ownedListingsID != null && !ownedListingsID.isEmpty()) {
			List<MarketListing> ownedListings = MarketRedisManager.getListings(ownedListingsID.toArray(new Long[0]));
			int amountClaimable = 0;
			int amountExpired = 0;
			for (MarketListing l : ownedListings) {
				if (l.isExpired()) {
					amountExpired++;
				} else if (l.getAmountToClaim() > 0) {
					amountClaimable++;
				}
			}

			if (amountExpired > 0) {
				messages.add(marketHeader
					.append(Component.text("You have ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, false))
					.append(Component.text(amountExpired, NamedTextColor.WHITE).decoration(TextDecoration.BOLD, true))
					.append(Component.text(" Expired Listings.", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, false)));
			}
			if (amountClaimable > 0) {
				messages.add(marketHeader
					.append(Component.text("You can claim money from ", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, false))
					.append(Component.text(amountClaimable, NamedTextColor.WHITE).decoration(TextDecoration.BOLD, true))
					.append(Component.text(" Listings.", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, false)));
			}
		}

		if (!messages.isEmpty()) {
			player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 1.0f, 1.0f);
			for (Component c : messages) {
				player.sendMessage(c);
			}
		}

	}

	public void playerSaveEvent(PlayerSaveEvent event) {
		MarketPlayerData marketPlayerData = mMarketPlayerDataInstances.getOrDefault(event.getPlayer(), new MarketPlayerData());
		if (marketPlayerData == null) {
			Plugin.getInstance().getLogger().warning("ERROR FAILED TO SAVE MARKET DATA OF " + event.getPlayer().getName() + ": NO MARKET INSTANCE");
			AuditListener.logMarket("ERROR FAILED TO SAVE MARKET DATA OF " + event.getPlayer().getName() + ": NO MARKET INSTANCE");
			return;
		}
		event.setPluginData(KEY_PLUGIN_DATA, marketPlayerData.toJson());
	}

	public void onLogout(Player player) {
		// delay the data removal by 20 ticks, as we need it for the playersave event, launched after logout event
		UUID playerId = player.getUniqueId();
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
			if (!Bukkit.getOfflinePlayer(playerId).isOnline()) {
				mMarketPlayerDataInstances.remove(player);
			}
		}, 20L);
	}

	// Verifies that the player has the item that he wants to sell. take it from them.
	//  if so, attempts to create a listing in redis.
	//  on success, link that listing to the player data and take the item from the player,
	//  on fail, give back the item
	// returns true if the listing creation was successful, false otherwise
	//
	// this method may take some time, due to the call to redis
	// the usage of that method in an async environment is thus heavily recommended
	public void addNewListing(Player player, ItemStack itemToSell, int itemsPerTrade, int amountOfTrades, int pricePerTrade, ItemStack currencyItemStack, WalletUtils.Debt taxDebt) {

		// check that the item about to be sold is actually sellable
		List<String> errorMessages = itemIsSellable(player, itemToSell, currencyItemStack);
		if (!errorMessages.isEmpty()) {
			for (String message : errorMessages) {
				player.sendMessage(Component.text("Something went wrong: " + message + ". listing creation cancelled", NamedTextColor.RED).decoration(TextDecoration.BOLD, true));
			}
		}

		// check that the player has the items they want to sell
		if (!player.getInventory().containsAtLeast(itemToSell, itemsPerTrade * amountOfTrades)) {
			player.sendMessage("Something went wrong: you do not have the listing items in your inventory. listing creation cancelled");
			return;
		}

		if (!WalletUtils.tryToPayFromInventoryAndWallet(player, taxDebt.mItem.asQuantity(taxDebt.mTotalRequiredAmount), true, true)) {
			player.sendMessage("Something went wrong: you do not have enough money to pay the tax. listing creation cancelled");
			return;
		}

		// remove the items from player inventory
		HashMap<?, ?> failedToRemove = player.getInventory().removeItem(itemToSell.asQuantity(itemsPerTrade * amountOfTrades));
		if (!failedToRemove.isEmpty()) {
			player.sendMessage("Something went wrong: Failed to remove the listing items from your inventory. listing creation cancelled");
			// destroy the already existing listing
			return;
		}

		MarketListing createdListing = MarketRedisManager.createAndAddNewListing(player, itemToSell, itemsPerTrade, amountOfTrades, pricePerTrade, currencyItemStack);
		if (createdListing == null) {
			// creation failed on the redis side
			player.sendMessage("Something went wrong: Server failed to create the listing. You need to contact a moderator for tax refund. amount is given in logs");
			AuditListener.logMarket("!ERROR! Player " + player.getName() + "needs a " + taxDebt.mTotalRequiredAmount + "*" + ItemUtils.getPlainName(taxDebt.mItem) + "tax refund, because the listing failed to be created in redis");
			return;
		}
		getInstance().linkListingToPlayerData(player, createdListing.getId());
		MarketAudit.logCreate(player, createdListing, taxDebt);
	}

	private static @Nullable WalletUtils.Debt checkCanAfford(Player player, MarketListing listing, int tradeMultAmount) {
		ItemStack currency = listing.getCurrencyToBuy().clone();
		currency.setAmount(listing.getAmountToBuy() * tradeMultAmount);
		WalletUtils.Debt debt = WalletUtils.calculateInventoryAndWalletDebt(currency, player, true);
		if (!debt.mMeetsRequirement) {
			player.sendMessage(Component.text("You don't have enough currency to purchase this."));
			return null;
		}
		return debt;
	}

	public static void performPurchase(Player player, MarketListing originalListing, int tradeMultAmount, Runnable onCancel, Runnable afterComplete) {

		// Initial inventory + wallet check (on main thread)
		// A second check is run just before the payment is actually deducted from the player
		if (checkCanAfford(player, originalListing, tradeMultAmount) == null) {
			onCancel.run();
			return;
		}

		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			// Grab an up-to-date edit-locked listing from Redis asynchronously
			long startTimestamp = System.currentTimeMillis();
			String editLockedString = player.getName() + "-" + startTimestamp;
			MarketListing lockedListing = MarketRedisManager.atomicEditListing(originalListing.getId(), l -> l.setEditLocked(editLockedString));
			if (lockedListing == null) {
				player.sendMessage(Component.text("Failed to purchase from this listing, please retry momentarily.", NamedTextColor.RED));
				onCancel.run();
				return;
			}

			// Check if the listing can still be bought, unlock and abort if not
			MarketListingStatus purchasableStatus = lockedListing.getPurchasableStatus(tradeMultAmount);
			if (purchasableStatus.isError()) {
				MarketRedisManager.removeEditLockAndUpdateListing(lockedListing);
				player.sendMessage(purchasableStatus.getFormattedAssociatedMessage());
				onCancel.run();
				return;
			}

			Bukkit.getScheduler().scheduleSyncDelayedTask(Plugin.getInstance(), () -> {
				// Pay cost and give items to player on main thread

				WalletUtils.Debt debt = checkCanAfford(player, lockedListing, tradeMultAmount);
				if (debt == null) {
					// If the player doesn't have enough currency anymore, unlock the listing and abort
					MarketRedisManager.removeEditLockAndUpdateListing(lockedListing);
					onCancel.run();
					return;
				}
				WalletUtils.payDebt(debt, player, true);

				int amountToGive = tradeMultAmount * lockedListing.getBundleSize();
				InventoryUtils.giveItemWithStacksizeCheck(player, lockedListing.getItemToSell().asQuantity(amountToGive));

				player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 0.7f);

				MarketAudit.logBuyAction(player, lockedListing, amountToGive, debt.mTotalRequiredAmount, debt.mItem);

				Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
					// Finally, update and unlock the listing async
					lockedListing.setAmountToSellRemaining(lockedListing.getAmountToSellRemaining() - tradeMultAmount);
					lockedListing.setAmountToClaim(lockedListing.getAmountToClaim() + tradeMultAmount);
					MarketRedisManager.removeEditLockAndUpdateListing(lockedListing);

					Bukkit.getScheduler().scheduleSyncDelayedTask(Plugin.getInstance(), afterComplete);

					// If the new listing values makes it so that the listing cannot be bought anymore,
					// remove it from the active_listings index. This does not need to be instant and thus is done at the end.
					if (lockedListing.getPurchasableStatus(1).isError()) {
						MarketListingIndex.ACTIVE_LISTINGS.removeListingFromIndex(lockedListing);
					}

				});
			});
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
		famount = Math.ceil(famount * getConfig().mBazaarTaxRate);

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

	public boolean editListing(Player player, MarketListing oldListing, boolean delete, Consumer<MarketListing> updater) {
		// WARNING: Call this in an async thread

		if (delete) {
			return claimEverythingAndDeleteListing(player, oldListing.getId());
		}

		List<String> errors = new ArrayList<>();

		MarketListing newListing = new MarketListing(oldListing);
		updater.accept(newListing);
		if (oldListing.isSimilar(newListing)) {
			errors.add("No edits detected! Make some changes!");
		}

		if (!errors.isEmpty()) {

			player.playSound(player.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1, 1);
			for (String error : errors) {
				player.sendMessage(Component.text(error, NamedTextColor.RED).decoration(TextDecoration.BOLD, true));
			}
			return false;
		} else {
			// no errors found, proceed with the edit

			newListing = MarketRedisManager.atomicEditListing(oldListing.getId(), updater);
			if (newListing == null) {
				player.sendMessage("Failed to edit listing, please retry momentarily.");
				return false;
			}

			// redis edit ok
			for (MarketListingIndex index : MarketListingIndex.values()) {
				if (index.mMatchMethod.apply(oldListing) && !index.mMatchMethod.apply(newListing)) {
					index.removeListingFromIndex(newListing);
				} else if (!index.mMatchMethod.apply(oldListing) && index.mMatchMethod.apply(newListing)) {
					index.addListingToIndex(newListing);
				}
			}

		}
		return true;

	}

	public void resyncOwnership(Player player) {
		// go through all listings, and check if the creator is the player
		// if so, it is expected that the owner is the player, thus, relink ownership
		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			// reset current ownership
			mMarketPlayerDataInstances.put(player, new MarketPlayerData());

			// get all listings
			List<Long> ids = MarketRedisManager.getAllListingsIds(true);

			List<Long> batch = new ArrayList<>();
			for (Long id : ids) {
				batch.add(id);
				if (batch.size() >= 100) {
					resyncListingOwnership(player, batch);
					batch = new ArrayList<>();
				}
			}
			resyncListingOwnership(player, batch);
		});

	}

	private void resyncListingOwnership(Player player, List<Long> batch) {
		List<MarketListing> listings = MarketRedisManager.getListings(batch.toArray(new Long[0]));

		for (MarketListing listing : listings) {
			if (player.getUniqueId().toString().equals(listing.getOwnerUUID())) {
				MarketAudit.logManualLinking(player, listing.getId());
				getInstance().linkListingToPlayerData(player, listing.getId());
			}
		}

	}

	public void getAllFiltersData(Player targetPlayer) {

		Gson gson = new GsonBuilder().setPrettyPrinting().create();

		MarketFilter forced = getForcedFiltersOfPlayer(targetPlayer);
		List<MarketFilter> filters = getPlayerMarketFilters(targetPlayer);

		targetPlayer.sendMessage("Forced Filter:");
		targetPlayer.sendMessage(gson.toJson(forced));

		targetPlayer.sendMessage("Player Filters:");
		for (MarketFilter filter : filters) {
			targetPlayer.sendMessage(gson.toJson(filter));
		}

	}

	public MarketFilter getForcedFiltersOfPlayer(Player player) {
		return ComponentConfig.buildForcedBlacklistFilterForPlayer(player);
	}

	public void resetPlayerFilters(Player player) {
		MarketPlayerData marketPlayerData = mMarketPlayerDataInstances.get(player);
		if (marketPlayerData == null) {
			Plugin.getInstance().getLogger().warning("ERROR: FAILED TO GET MARKET DATA OF " + player.getName() + ": NO MARKET INSTANCE. CONTACT A MODERATOR IMMEDIATELY, SOMETHING IS WRONG WITH YOUR PLUGIN DATA");
			return;
		}
		marketPlayerData.resetPlayerFiltersList();
	}
}
