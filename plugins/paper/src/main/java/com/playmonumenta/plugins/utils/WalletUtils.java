package com.playmonumenta.plugins.utils;

import com.playmonumenta.plugins.inventories.Wallet;
import com.playmonumenta.plugins.inventories.WalletManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.jetbrains.annotations.Nullable;

public class WalletUtils {
	/**
	 * Details about an attempted payment, using a mix of money in inventory and in wallet
	 *
	 * @param mItem                Item that is required
	 * @param mTotalRequiredAmount Amount of items required
	 * @param mInventoryDebt       Amount of items to take from the player's inventory
	 * @param mWalletDebt          Amount of items to take from the player's wallet
	 * @param mMeetsRequirement    Whether the player has enough currency to meet the debt
	 * @param mNumInWallet         Amount of items of the required currency present in the player's wallet before the trade
	 * @param mNumInInventory      Amount of items of the required currency present in the player's inventory before the trade
	 */
	public record Debt(ItemStack mItem, int mTotalRequiredAmount, int mInventoryDebt, int mWalletDebt,
	                   boolean mMeetsRequirement, long mNumInWallet, long mNumInInventory) {
	}

	/**
	 * Calculates the amount of debt for the inventory and wallet, of the requirement item and its amount. Also returns whether the
	 * player has enough currency to complete the trade.
	 *
	 * @param requirement       the ItemStack to calculate debt for.
	 * @param inventoryContents the contents of the player's inventory.
	 * @param reqAmount         The amount of the item required.
	 * @param wallet            the Wallet of the player.
	 * @param prioritizeWallet  whether to take from the wallet first (true) or the inventory first (false)
	 * @return a Debt object with the relevant information.
	 */
	public static Debt calculateInventoryAndWalletDebt(ItemStack requirement, int reqAmount, ItemStack[] inventoryContents, @Nullable Wallet wallet, boolean prioritizeWallet) {
		// Find the requirement amounts in inventory and wallet:
		int numInInventory = InventoryUtils.numInInventory(inventoryContents, requirement);
		long numInWallet = (wallet != null && WalletManager.isCurrency(requirement)) ? wallet.count(requirement) : 0;
		boolean meetsRequirement = ((long) numInInventory + numInWallet >= (long) reqAmount);

		// Find the debt (amount to remove) for inventory and wallet:
		int inventoryDebt;
		int walletDebt;
		if (!prioritizeWallet) {
			// Prioritize inventory:
			inventoryDebt = Math.min(numInInventory, reqAmount);
			walletDebt = (int) Math.min(reqAmount - inventoryDebt, numInWallet);
		} else {
			// Prioritize wallet:
			walletDebt = (int) Math.min(numInWallet, reqAmount);
			inventoryDebt = Math.min(reqAmount - walletDebt, numInInventory);
		}

		return new Debt(requirement, reqAmount, inventoryDebt, walletDebt, meetsRequirement, numInWallet, numInInventory);
	}

	/**
	 * Calculates the amount of debt for the inventory and wallet, of the requirement item. Also returns whether the
	 * player has enough currency to complete the trade.
	 *
	 * @param requirement       the ItemStack to calculate debt for.
	 * @param inventoryContents the contents of the player's inventory.
	 * @param wallet            the Wallet of the player.
	 * @param prioritizeWallet  whether to take from the wallet first (true) or the inventory first (false)
	 * @return a Debt object with the relevant information.
	 */
	public static Debt calculateInventoryAndWalletDebt(ItemStack requirement, ItemStack[] inventoryContents, @Nullable Wallet wallet, boolean prioritizeWallet) {
		// Find the requirement amounts in inventory and wallet:
		int reqAmount = requirement.getAmount();
		int numInInventory = InventoryUtils.numInInventory(inventoryContents, requirement);
		long numInWallet = (wallet != null && WalletManager.isCurrency(requirement)) ? wallet.count(requirement) : 0;
		boolean meetsRequirement = ((long) numInInventory + numInWallet >= (long) reqAmount);

		// Find the debt (amount to remove) for inventory and wallet:
		int inventoryDebt;
		int walletDebt;
		if (!prioritizeWallet) {
			// Prioritize inventory:
			inventoryDebt = Math.min(numInInventory, reqAmount);
			walletDebt = (int) Math.min(reqAmount - inventoryDebt, numInWallet);
		} else {
			// Prioritize wallet:
			walletDebt = (int) Math.min(numInWallet, reqAmount);
			inventoryDebt = Math.min(reqAmount - walletDebt, numInInventory);
		}

		return new Debt(requirement, reqAmount, inventoryDebt, walletDebt, meetsRequirement, numInWallet, numInInventory);
	}

	public static Debt calculateInventoryAndWalletDebt(ItemStack requirement, Player player, boolean prioritizeWallet) {
		return calculateInventoryAndWalletDebt(requirement, player.getInventory().getStorageContents(), WalletManager.getWallet(player), prioritizeWallet);
	}

	public static void payDebt(Debt debt, Player player, boolean notify) {
		final int oneStack = debt.mItem.getMaxStackSize();
		if (debt.mInventoryDebt > 0) {
			if (debt.mInventoryDebt <= oneStack) {
				player.getInventory().removeItem(debt.mItem.asQuantity(debt.mInventoryDebt));
			} else {
				int left = debt.mInventoryDebt;
				while (left > 0) {
					int toRemove = Math.min(left, oneStack);
					player.getInventory().removeItem(debt.mItem.asQuantity(toRemove));
					left -= toRemove;
				}
			}
		}
		if (debt.mWalletDebt > 0) {
			if (debt.mWalletDebt <= oneStack) {
				WalletManager.getWallet(player).remove(player, debt.mItem.asQuantity(debt.mWalletDebt));
			} else {
				int left = debt.mWalletDebt;
				while (left > 0) {
					int toRemove = Math.min(left, oneStack);
					WalletManager.getWallet(player).remove(player, debt.mItem.asQuantity(toRemove));
					left -= toRemove;
				}
			}
			if (notify) {
				notifyRemovalFromWallet(debt, player);
			}
		}
	}

	public static void refundDebt(Debt debt, Player player, boolean notify) {
		final int oneStack = debt.mItem.getMaxStackSize();
		if (debt.mInventoryDebt > 0) {
			if (debt.mInventoryDebt <= oneStack) {
				InventoryUtils.giveItem(player, debt.mItem.asQuantity(debt.mInventoryDebt));
			} else {
				int left = debt.mInventoryDebt;
				while (left > 0) {
					int toGive = Math.min(left, oneStack);
					InventoryUtils.giveItem(player, debt.mItem.asQuantity(toGive));
					left -= toGive;
				}
			}
		}
		if (debt.mWalletDebt > 0) {
			if (debt.mWalletDebt <= oneStack) {
				WalletManager.getWallet(player).add(player, debt.mItem.asQuantity(debt.mWalletDebt));
			} else {
				int left = debt.mWalletDebt;
				while (left > 0) {
					int toGive = Math.min(left, oneStack);
					WalletManager.getWallet(player).add(player, debt.mItem.asQuantity(toGive));
					left -= toGive;
				}
			}
			if (notify) {
				notifyDepositIntoWallet(debt, player);
			}
		}
	}

	public static void notifyRemovalFromWallet(Debt debt, Player player) {
		if (debt.mWalletDebt > 0) {
			player.sendMessage(Component.text("Removed ", NamedTextColor.GREEN).append(
				Component.text(debt.mWalletDebt + " " + ItemUtils.getPlainNameOrDefault(debt.mItem), NamedTextColor.WHITE).append(
					Component.text(" from your wallet. ", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)).append(
					Component.text("(Remaining: " + (debt.mNumInWallet - debt.mWalletDebt) + ")", NamedTextColor.GRAY)
				)));
		}
	}

	public static void notifyDepositIntoWallet(Debt debt, Player player) {
		if (debt.mWalletDebt > 0) {
			player.sendMessage(Component.text("Deposited ", NamedTextColor.GREEN).append(
				Component.text(debt.mWalletDebt + " " + ItemUtils.getPlainNameOrDefault(debt.mItem), NamedTextColor.WHITE).append(
					Component.text(" to your wallet. ", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false)).append(
					Component.text("(Total: " + (debt.mNumInWallet - debt.mWalletDebt) + ")", NamedTextColor.GRAY)
				)));
		}
	}

	public static boolean tryToPayFromInventoryAndWallet(Player player, ItemStack cost) {
		return tryToPayFromInventoryAndWallet(player, List.of(cost));
	}

	public static boolean tryToPayFromInventoryAndWallet(Player player, List<ItemStack> costs) {
		return tryToPayFromInventoryAndWallet(player, costs.stream().collect(Collectors.toMap(Function.identity(), ItemStack::getAmount)), false, true);
	}

	public static boolean tryToPayFromInventoryAndWallet(Player player, Map<ItemStack, Integer> costs) {
		return tryToPayFromInventoryAndWallet(player, costs, false, true);
	}

	private static boolean tryToPayFromInventoryAndWallet(Player player, Map<ItemStack, Integer> costs, boolean prioritizeWallet, boolean notify) {
		PlayerInventory playerInventory = player.getInventory();
		Wallet wallet = WalletManager.getWallet(player);
		List<Debt> debts = new ArrayList<>(costs.size());
		for (Map.Entry<ItemStack, Integer> cost : costs.entrySet()) {
			if (ItemUtils.isNullOrAir(cost.getKey())) {
				continue;
			}
			Debt debt = calculateInventoryAndWalletDebt(cost.getKey(), cost.getValue(), playerInventory.getStorageContents(), wallet, prioritizeWallet);
			if (!debt.mMeetsRequirement) {
				return false;
			}
			debts.add(debt);
		}
		for (Debt debt : debts) {
			payDebt(debt, player, notify);
		}
		return true;
	}

	public static boolean tryToPayFromInventoryAndWallet(Player player, List<ItemStack> costs, boolean prioritizeWallet, boolean notify) {
		PlayerInventory playerInventory = player.getInventory();
		Wallet wallet = WalletManager.getWallet(player);
		List<Debt> debts = new ArrayList<>(costs.size());
		for (ItemStack cost : costs) {
			if (ItemUtils.isNullOrAir(cost)) {
				continue;
			}
			Debt debt = calculateInventoryAndWalletDebt(cost, playerInventory.getStorageContents(), wallet, prioritizeWallet);
			if (!debt.mMeetsRequirement) {
				return false;
			}
			debts.add(debt);
		}
		for (Debt debt : debts) {
			payDebt(debt, player, notify);
		}
		return true;
	}

	public static boolean tryToPayFromInventoryAndWallet(Player player, ItemStack cost, boolean prioritizeWallet, boolean notify) {
		PlayerInventory playerInventory = player.getInventory();
		Wallet wallet = WalletManager.getWallet(player);
		Debt debt = calculateInventoryAndWalletDebt(cost, playerInventory.getStorageContents(), wallet, prioritizeWallet);
		if (!debt.mMeetsRequirement) {
			return false;
		}
		payDebt(debt, player, notify);
		return true;
	}

}
