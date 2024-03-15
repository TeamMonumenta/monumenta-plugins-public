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
			"Listing : " + targetListing.getId() + "\n" +
			"Player : " + player.getName() + "\n" +
			"Bought : " + amountItemToSell + " * " + getItemNameFromItemStack(targetListing.getItemToSell()) + "\n" +
			"For : " + amountToPay + " * " + ItemUtils.getPlainName(currencyToPay);
		AuditListener.logMarket(message);
	}

	public static void logUnlockAction(Player player, MarketListing targetListing) {
		String message =
			"UNLOCK \n" +
			"Listing : " + targetListing.getId() + "\n" +
			"Unlocked by : " + player.getName();
		AuditListener.logMarket(message);
	}

	public static void logLockAction(Player player, MarketListing targetListing) {
		String message =
			"LOCK \n" +
			"Listing : " + targetListing.getId() + "\n" +
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
			"Listing : " + targetListing.getId() + "\n" +
			"Claimed Items : " + itemsToGive + " * " + getItemNameFromItemStack(targetListing.getItemToSell()) + "\n" +
			"Claimed Currency : " + currencyToGive + " * " + ItemUtils.getPlainName(targetListing.getItemToBuy());
		AuditListener.logMarket(message);
	}

	public static void logClaim(Player player, MarketListing targetListing, int amountToGive) {
		String message = "CLAIM\n" +
			"Player : " + player.getName() + "\n" +
			"Listing : " + targetListing.getId() + "\n" +
			"Claimed Currency : " + amountToGive + " * " + ItemUtils.getPlainName(targetListing.getItemToBuy());
		AuditListener.logMarket(message);
	}

	public static void logCreate(Player player, MarketListing targetListing, WalletUtils.Debt taxDebt) {
		String message = "CREATE\n" +
			"Player : " + player.getName() + "\n" +
			"Listing : " + targetListing.getId() + "\n" +
			"To Sell : " + targetListing.getAmountToSellRemaining() + " * " + getItemNameFromItemStack(targetListing.getItemToSell()) + "\n" +
		    "Price : " + targetListing.getAmountToBuy() + " * " + ItemUtils.getPlainName(targetListing.getItemToBuy()) + "\n" +
			"Tax paid : " + taxDebt.mTotalRequiredAmount + " * " + ItemUtils.getPlainName(taxDebt.mItem);
		AuditListener.logMarket(message);
	}

	public static void logExpire(Player player, MarketListing targetListing) {
		String message =
			"EXPIRE \n" +
			"Listing : " + targetListing.getId() + "\n" +
			"Expired by : " + player.getName();
		AuditListener.logMarket(message);
	}

	public static void logUnexpire(Player player, MarketListing targetListing) {
		String message =
			"UNEXPIRE \n" +
			"Listing : " + targetListing.getId() + "\n" +
			"Unexpired by : " + player.getName();
		AuditListener.logMarket(message);
	}
}
