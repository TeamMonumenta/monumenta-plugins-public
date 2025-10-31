package com.playmonumenta.plugins.market;

import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.WalletUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.codehaus.plexus.util.StringUtils;

public class MarketAudit {

	private static String getItemNameFromItemStack(ItemStack item) {
		String itemToSellName = ItemUtils.getPlainName(item);
		if (StringUtils.isEmpty(itemToSellName)) {
			itemToSellName = item.getType().toString();
		}
		return itemToSellName;
	}

	public static void logBuyAction(Player player, MarketListing targetListing, int amountItemToSell, int amountToPay, ItemStack currencyToPay) {
		String message =
			"BUY \n" +
				"Listing : " + targetListing.getDisplayId() + "\n" +
				"Player : " + player.getName() + "\n" +
				"Bought : " + amountItemToSell + " * " + getItemNameFromItemStack(targetListing.getItemToSell()) + "\n" +
				"For : " + amountToPay + " * " + ItemUtils.getPlainName(currencyToPay);
		AuditListener.logMarket(message);
	}

	public static void logUnlockAction(Player player, MarketListing targetListing) {
		String message =
			"UNLOCK \n" +
				"Listing : " + targetListing.getDisplayId() + "\n" +
				"Unlocked by : " + player.getName();
		AuditListener.logMarket(message);
	}

	public static void logLockAction(Player player, MarketListing targetListing) {
		String message =
			"LOCK \n" +
				"Listing : " + targetListing.getDisplayId() + "\n" +
				"Locked by : " + player.getName();
		AuditListener.logMarket(message);
	}

	public static void logLockAllAction(Player player, int amountLocked) {
		String message =
			"LOCK ALL LISTINGS\n" +
				"Player : " + player.getName() + "\n" +
				"Locked : " + amountLocked + " listings";
		AuditListener.logMarket(message);
	}

	public static void logClaimAndDelete(Player player, MarketListing targetListing, int itemsToGive, int currencyToGive) {
		String message = "CLAIM AND DELETE\n" +
			"Player : " + player.getName() + "\n" +
			"Listing : " + targetListing.getDisplayId() + "\n" +
			"Claimed Items : " + itemsToGive + " * " + getItemNameFromItemStack(targetListing.getItemToSell()) + "\n" +
			"Claimed Currency : " + currencyToGive + " * " + ItemUtils.getPlainName(targetListing.getCurrencyToBuy());
		AuditListener.logMarket(message);
	}

	public static void logClaim(Player player, MarketListing targetListing, int amountToGive) {
		String message = "CLAIM\n" +
			"Player : " + player.getName() + "\n" +
			"Listing : " + targetListing.getDisplayId() + "\n" +
			"Claimed Currency : " + amountToGive + " * " + ItemUtils.getPlainName(targetListing.getCurrencyToBuy());
		AuditListener.logMarket(message);
	}

	public static void logCreate(Player player, MarketListing targetListing, WalletUtils.Debt taxDebt) {
		String message = "CREATE\n" +
			"Player : " + player.getName() + "\n" +
			"Listing : " + targetListing.getDisplayId() + "\n" +
			"Trade : " + targetListing.getBundleSize() + " * " + getItemNameFromItemStack(targetListing.getItemToSell()) + "\n" +
			"Amount of Trades : " + targetListing.getAmountToSellRemaining() + "\n" +
			"Price Per Trade : " + targetListing.getAmountToBuy() + " * " + ItemUtils.getPlainName(targetListing.getCurrencyToBuy()) + "\n" +
			"Tax paid : " + taxDebt.mTotalRequiredAmount() + " * " + ItemUtils.getPlainName(taxDebt.mItem());
		AuditListener.logMarket(message);
	}

	public static void logExpire(Player player, MarketListing targetListing, String reason) {
		String message =
			"EXPIRE \n" +
				"Listing : " + targetListing.getDisplayId() + "\n" +
				"Expired by : " + player.getName() + "\n" +
				"Reason : " + reason;
		AuditListener.logMarket(message);
	}

	public static void logUnexpire(Player player, MarketListing targetListing) {
		String message =
			"UNEXPIRE \n" +
				"Listing : " + targetListing.getDisplayId() + "\n" +
				"Unexpired by : " + player.getName();
		AuditListener.logMarket(message);
	}

	public static void logManualLinking(Player player, long listingId) {
		String message =
			"MANUAL LINKING \n" +
				"Listing : " + listingId + "\n" +
				"Linked To : " + player.getName();
		AuditListener.logMarket(message);
	}

	public static void logManualUnlinking(Player player, long listingId) {
		String message =
			"MANUAL UNLINKING \n" +
				"Listing : " + listingId + "\n" +
				"Unlinked From : " + player.getName();
		AuditListener.logMarket(message);
	}
}
