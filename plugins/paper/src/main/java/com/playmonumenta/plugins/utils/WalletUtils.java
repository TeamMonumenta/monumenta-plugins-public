package com.playmonumenta.plugins.utils;

import com.playmonumenta.plugins.inventories.WalletManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class WalletUtils {
	public static class Debt {
		// Amount of items to take from the player's inventory
		public final int mInventoryDebt;
		// Amount of items to take from the player's wallet
		public final int mWalletDebt;
		// Whether the player has enough currency to meet the debt
		public final boolean mMeetsRequirement;
		// Amount of items of the required currency present in the player's wallet before the trade
		public final int mNumInWallet;

		public Debt(int inventoryDebt, int walletDebt, boolean meetsRequirement, int numInWallet) {
			mInventoryDebt = inventoryDebt;
			mWalletDebt = walletDebt;
			mMeetsRequirement = meetsRequirement;
			mNumInWallet = numInWallet;
		}
	}

	/**
	 * Calculates the amount of debt for the inventory and wallet, of the requirement item. Also returns whether the
	 * player has enough currency to complete the trade.
	 * @param requirement the ItemStack to calculate debt for.
	 * @param inventoryContents the contents of the player's inventory.
	 * @param inventoryWallet the Wallet inventory of the player.
	 * @param prioritizeWallet whether to take from the wallet first (true) or the inventory first (false)
	 * @return a Debt object with the relevant information.
	 */
	public static Debt calculateInventoryAndWalletDebt(ItemStack requirement, ItemStack[] inventoryContents, @Nullable WalletManager.InventoryWallet inventoryWallet, boolean prioritizeWallet) {
		// Find the requirement amounts in inventory and wallet:
		int reqAmount = requirement.getAmount();
		int numInInventory = InventoryUtils.numInInventory(inventoryContents, requirement);
		int numInWallet = (inventoryWallet != null && WalletManager.isCurrency(requirement)) ? inventoryWallet.numInWallet(requirement) : 0;
		boolean meetsRequirement = ((long) numInInventory + (long) numInWallet >= (long) reqAmount);

		// Find the debt (amount to remove) for inventory and wallet:
		int inventoryDebt;
		int walletDebt;
		if (!prioritizeWallet) {
			// Prioritize inventory:
			inventoryDebt = Math.min(numInInventory, reqAmount);
			walletDebt = Math.min(reqAmount - inventoryDebt, numInWallet);
		} else {
			// Prioritize wallet:
			walletDebt = Math.min(numInWallet, reqAmount);
			inventoryDebt = Math.min(reqAmount - walletDebt, numInInventory);
		}

		return new Debt(inventoryDebt, walletDebt, meetsRequirement, numInWallet);
	}

	public static void notifyRemovalFromWallet(Debt debt, Player player, ItemStack itemRequirement) {
		if (debt.mWalletDebt > 0) {
			player.sendMessage(Component.text("Removed ", NamedTextColor.GREEN).append(
				Component.text(debt.mWalletDebt + " " + ItemUtils.getPlainNameOrDefault(itemRequirement), NamedTextColor.WHITE).append(
					Component.text(" from your wallet. ", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)).append(
					Component.text("(Remaining: " + (debt.mNumInWallet - debt.mWalletDebt) + ")", NamedTextColor.GRAY)
				)));
		}
	}
}
