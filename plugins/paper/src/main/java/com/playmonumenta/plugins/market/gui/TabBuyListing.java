package com.playmonumenta.plugins.market.gui;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.guis.GuiItem;
import com.playmonumenta.plugins.market.MarketListingStatus;
import com.playmonumenta.plugins.market.MarketManager;
import com.playmonumenta.plugins.market.MarketRedisManager;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.WalletUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class TabBuyListing implements MarketGuiTab {

	MarketGui mGui;

	static final Component TAB_TITLE = Component.text("Buy Listing");
	static final int TAB_SIZE = 6 * 9;

	public TabBuyListing(MarketGui marketGUI) {
		this.mGui = marketGUI;
	}

	@Override
	public void setup() {

		if (mGui.mFocusedListing == null) {
			mGui.mPlayer.sendMessage("Buy listing tab does not have a focused listing. this shouldnt happen, but will not lead to issues. sending viewer back to browser");
			mGui.switchToTab(mGui.TAB_BAZAAR_BROWSER);
			return;
		}

		if (mGui.mIsLoadingData == 2) {
			// in async, refresh the listing that is wanted by the gui,
			// and refresh the gui when listing is loaded
			mGui.mIsLoadingData = 3;
			Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
				if (mGui.mFocusedListing != null) {
					mGui.mFocusedListing = MarketRedisManager.getListing(mGui.mFocusedListing.getId());
					if (mGui.mCurrentTab != mGui.TAB_BUY_LISTING) {
						// player might have canceled the search, no need to keep going
						return;
					}
					mGui.mIsLoadingData = 4;
					mGui.update();
				} else {
					mGui.switchToTab(mGui.TAB_BAZAAR_BROWSER);
				}
			});
		}

		ItemStack currency = mGui.mFocusedListing.getCurrencyToBuy().clone();
		currency.setAmount(mGui.mFocusedListing.getAmountToBuy());

		WalletUtils.Debt debt = WalletUtils.calculateInventoryAndWalletDebt(currency, mGui.mPlayer, true);

		long amountAvailable = Math.min(debt.mNumInWallet + debt.mNumInInventory, (long)mGui.mFocusedListing.getAmountToSellRemaining() * mGui.mFocusedListing.getAmountToBuy());
		int maxMultiplier = (int)(amountAvailable / mGui.mFocusedListing.getAmountToBuy());

		// currency display
		mGui.setItem(2, 2, currency);

		// multiplier button
		mGui.setItem(2, 4, mGui.createTradeMultiplierButton(maxMultiplier))
			.onClick((inventoryClickEvent) -> {
				int offset = 1;
				if (inventoryClickEvent.isShiftClick()) {
					offset = 8;
				}
				mGui.mBuyListingMultiplier = mGui.modifyTradeMultiplier(mGui.mBuyListingMultiplier, inventoryClickEvent.isLeftClick() ? -offset : offset, maxMultiplier);
				mGui.update();
			});

		// itemToBeSold display
		ItemStack listingItemStack = mGui.mFocusedListing.getItemToSell();
		mGui.setItem(2, 6, listingItemStack);


		// cancel button
		mGui.setItem(4, 3, GUIUtils.createCancel(List.of(Component.text("Return to the previous page.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))))
			.onClick((clickEvent) -> {
				mGui.switchToTab(mGui.TAB_BAZAAR_BROWSER);
			});

		// refresh button
		if (mGui.mIsLoadingData <= 3) {
			mGui.setItem(4, 4, MarketGuiIcons.LOADING);
		} else {
			mGui.setItem(4, 4, MarketGuiIcons.REFRESH)
				.onClick((clickEvent) -> {
					mGui.mIsLoadingData = 2;
					mGui.update();
				});
		}

		// confirm button
		List<Component> errorLoreLines = new ArrayList<>();

		// these errors shouldn't happen often
		if (mGui.mIsLoadingData <= 3) {
			errorLoreLines.add(Component.text("Wait for the data to finish loading", NamedTextColor.GOLD));
		}

		if (mGui.mBuyListingMultiplier <= 0 || mGui.mFocusedListing.getAmountToSellRemaining() <= 0) {
			errorLoreLines.add(Component.text("This listing ran out of stock!", NamedTextColor.DARK_RED));
		}

		if (!debt.mMeetsRequirement) {
			errorLoreLines.add(Component.text("Not enough Money to buy!", NamedTextColor.DARK_RED));
		}
		if (InventoryUtils.isFull(mGui.mPlayer.getInventory())) {
			errorLoreLines.add(Component.text("Your inventory is full.", NamedTextColor.DARK_RED));
		}

		if (errorLoreLines.isEmpty()) {
			MarketListingStatus purchasableStatus = mGui.mFocusedListing.getPurchasableStatus(mGui.mBuyListingMultiplier);
			if (purchasableStatus.isError()) {
				switch (purchasableStatus) {
					case LOCKED:
						errorLoreLines.add(Component.text("Item is locked, you cannot buy it.", NamedTextColor.DARK_RED));
						break;
					case EXPIRED:
						errorLoreLines.add(Component.text("Item is expired, you cannot buy it.", NamedTextColor.DARK_RED));
						break;
					case NOT_ENOUGH_REMAINING:
						errorLoreLines.add(Component.text("Not enough in stock! max: " + mGui.mFocusedListing.getAmountToSellRemaining(), NamedTextColor.DARK_RED));
						break;
					default:
						//other purchasableStatuses aren't used here
						break;
				}
			}
		}

		if (errorLoreLines.isEmpty()) {
			List<Component> confirmLore = new ArrayList<>();
			confirmLore.add(Component.text("Buy " + mGui.mBuyListingMultiplier + " " + ItemUtils.getPlainNameOrDefault(listingItemStack) + " for: ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
			confirmLore.add(Component.text((mGui.mBuyListingMultiplier * mGui.mFocusedListing.getAmountToBuy()) + " " + ItemUtils.getPlainNameOrDefault(mGui.mFocusedListing.getCurrencyToBuy()), NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
			mGui.setItem(4, 5, GUIUtils.createConfirm(confirmLore))
				.onClick((clickEvent) -> {
					if (MarketGui.initiatePlayerAction(mGui.mPlayer)) {
						return;
					}
					Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
						if (mGui.mFocusedListing != null) {
							MarketManager.performPurchase(mGui.mPlayer, mGui.mFocusedListing, mGui.mBuyListingMultiplier);
						}
						mGui.mIsLoadingData = 2;
						MarketGui.endPlayerAction(mGui.mPlayer);
						mGui.update();
					});
				});
		} else {
			mGui.setItem(4, 5, new GuiItem(GUIUtils.createExclamation(errorLoreLines), false));
		}

		if (mGui.mIsOp) {
			// moderation buttons
			List<Component> desc;

			if (mGui.mFocusedListing.isExpired()) {

				desc = new ArrayList<>();
				desc.add(Component.text("Click this to attempt to de-expire the listing").decoration(TextDecoration.ITALIC, false));
				desc.add(Component.text("The listing will once again be visible/tradable").decoration(TextDecoration.ITALIC, false));
				desc.add(Component.text("But if the listing naturally got expired beforehand").decoration(TextDecoration.ITALIC, false));
				desc.add(Component.text("It will soon automatically expire again").decoration(TextDecoration.ITALIC, false));
				mGui.setItem(4, GUIUtils.createBasicItem(Material.TOTEM_OF_UNDYING, 1, Component.text("Un-expire the listing", NamedTextColor.GOLD), desc, true))
					.onClick((clickEvent) -> {
						Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
							if (mGui.mFocusedListing != null) {
								MarketManager.unexpireListing(mGui.mPlayer, mGui.mFocusedListing);
							}
							mGui.mIsLoadingData = 2;
							mGui.update();
						});
					});

			} else {

				if (mGui.mFocusedListing.isLocked()) {
					desc = new ArrayList<>();
					desc.add(Component.text("Click this to unlock the listing").decoration(TextDecoration.ITALIC, false));
					desc.add(Component.text("The listing will once again be visible/tradable").decoration(TextDecoration.ITALIC, false));
					mGui.setItem(2, GUIUtils.createBasicItem(Material.GLASS, 1, Component.text("Unlock the listing", NamedTextColor.GOLD), desc, true))
						.onClick((clickEvent) -> {
							Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
								if (mGui.mFocusedListing != null) {
									MarketManager.unlockListing(mGui.mPlayer, mGui.mFocusedListing);
								}
								mGui.mIsLoadingData = 2;
								mGui.update();
							});
						});
				} else {
					desc = new ArrayList<>();
					desc.add(Component.text("Click this to lock the listing").decoration(TextDecoration.ITALIC, false));
					desc.add(Component.text("The listing will become hidden and not tradable").decoration(TextDecoration.ITALIC, false));
					desc.add(Component.text("until someone (usually the owner) unlocks it").decoration(TextDecoration.ITALIC, false));
					mGui.setItem(2, GUIUtils.createBasicItem(Material.IRON_BLOCK, 1, Component.text("Lock the listing", NamedTextColor.GOLD), desc, true))
						.onClick((clickEvent) -> {
							Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
								if (mGui.mFocusedListing != null) {
									MarketManager.lockListing(mGui.mPlayer, mGui.mFocusedListing);
								}
								mGui.mIsLoadingData = 2;
								mGui.update();
							});
						});
				}

				desc = new ArrayList<>();
				desc.add(Component.text("Click this to expire the listing").decoration(TextDecoration.ITALIC, false));
				desc.add(Component.text("The listing will become hidden and not tradable").decoration(TextDecoration.ITALIC, false));
				desc.add(Component.text("The owner will not be able to revert it").decoration(TextDecoration.ITALIC, false));
				desc.add(Component.text("The may only claim back the items for himself").decoration(TextDecoration.ITALIC, false));
				mGui.setItem(4, GUIUtils.createBasicItem(Material.WITHER_ROSE, 1, Component.text("Expire the listing", NamedTextColor.GOLD), desc, true))
					.onClick((clickEvent) -> {
						Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
							if (mGui.mFocusedListing != null) {
								MarketManager.expireListing(mGui.mPlayer, mGui.mFocusedListing, "manual expiry");
							}
							mGui.mIsLoadingData = 2;
							mGui.update();
						});
					});

			}

			desc = new ArrayList<>();
			desc.add(Component.text("Click this to delete the listing").decoration(TextDecoration.ITALIC, false));
			desc.add(Component.text("THIS ACTION IS NOT REVERTABLE", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
			desc.add(Component.text("You will recieve the listing's claimable items,").decoration(TextDecoration.ITALIC, false));
			desc.add(Component.text("like if you were the owner").decoration(TextDecoration.ITALIC, false));
			desc.add(Component.text("The listing won't exist anymore").decoration(TextDecoration.ITALIC, false));
			mGui.setItem(6, GUIUtils.createBasicItem(Material.LAVA_BUCKET, 1, Component.text("Claim and delete the listing", NamedTextColor.GOLD), desc, true))
				.onClick((clickEvent) -> {
					if (MarketGui.initiatePlayerAction(mGui.mPlayer)) {
						return;
					}
					Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
						if (mGui.mFocusedListing != null) {
							MarketManager.claimEverythingAndDeleteListing(mGui.mPlayer, mGui.mFocusedListing);
						}
						MarketGui.endPlayerAction(mGui.mPlayer);
					});
					mGui.switchToTab(mGui.TAB_MAIN_MENU);
				});

		}
	}

	@Override
	public void onSwitch() {
		mGui.setTitle(TAB_TITLE);
		mGui.setSize(TAB_SIZE);
		mGui.mIsLoadingData = 2;
		mGui.mBuyListingMultiplier = 1;
	}

	@Override
	public void onLeave() {

	}
}
