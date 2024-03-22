package com.playmonumenta.plugins.market.gui;

import com.playmonumenta.plugins.guis.GuiItem;
import com.playmonumenta.plugins.inventories.WalletManager;
import com.playmonumenta.plugins.market.MarketManager;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.SignUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.plugins.utils.WalletUtils;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;

public class TabAddListing implements MarketGuiTab {

	MarketGui mGui;

	static final Component TAB_TITLE = Component.text("New Bazaar Listing");
	static final int TAB_SIZE = 3 * 9;

	public TabAddListing(MarketGui marketGUI) {
		this.mGui = marketGUI;
	}

	@Override
	public void setup() {
		mGui.mPlayerListingsIds = MarketManager.getInstance().getListingsOfPlayer(mGui.mPlayer);
		if (mGui.mPlayerListingsIds.size() >= mGui.mPlayerMaxListings) {
			mGui.mPlayer.sendMessage(Component.text("You do not have enough slots to add a new listing", NamedTextColor.RED));
			mGui.switchToTab(mGui.TAB_PLAYER_LISTINGS);
			return;
		}

		// Minimum value of price per item is 0
		if (mGui.mPricePerItemAmount < 0) {
			mGui.mPricePerItemAmount = 0;
		}

		// Clamp the amount to between 1 and number of items in player inventory
		if (!ItemUtils.isNullOrAir(mGui.mItemToSell) && (mGui.mAmountToSell < 1 || !mGui.mPlayer.getInventory().containsAtLeast(mGui.mItemToSell, mGui.mAmountToSell))) {
			int numInInv = InventoryUtils.numInInventory(mGui.mPlayer.getInventory(), mGui.mItemToSell);
			if (numInInv == 0) {
				// the player will never have this item, even if we bring the wanted amount to 1.
				mGui.mItemToSell = null;
				mGui.mAmountToSell = 0;
			} else {
				// amount may be negative when changing the amount using the GuiItem
				mGui.mAmountToSell = Math.min(Math.max(mGui.mAmountToSell, 1), numInInv);
			}
		}

		mGui.setItem(0, 0, MarketGuiIcons.BACK_TO_MAIN_MENU).onClick((clickEvent) -> mGui.switchToTab(mGui.TAB_MAIN_MENU));

		// Modify the item to be sold
		if (ItemUtils.isNullOrAir(mGui.mItemToSell)) {
			mGui.setItem(1, 1, MarketGuiIcons.MISSING_ITEM_SELECTION);
		} else {
			mGui.setItem(1, 1, new GuiItem(MarketGuiIcons.buildAddListingItemToBeSoldIcon(mGui.mItemToSell), false))
				.onClick((clickEvent) -> {
					mGui.mItemToSell = null;
					mGui.update();
				});
		}

		// item to be sold multiplier
		mGui.setItem(1, 2, MarketGuiIcons.buildAddListingItemToBeSoldMultiplierIcon(mGui.mItemToSell, mGui.mAmountToSell))
			.onClick((clickEvent) -> {
				int changeMultiplier = 1;
				if (!ItemUtils.isNullOrAir(mGui.mItemToSell)) {
					switch (clickEvent.getClick()) {
						case SHIFT_LEFT:
							changeMultiplier = 64;
							// fall through to actually change the amount
						case LEFT:
							mGui.mAmountToSell -= changeMultiplier;
							mGui.update();
							break;
						case SHIFT_RIGHT:
							changeMultiplier = 64;
							// fall through to actually change the amount
						case RIGHT:
							mGui.mAmountToSell += changeMultiplier;
							mGui.update();
							break;
						case SWAP_OFFHAND:
							mGui.close();
							SignUtils.newMenu(List.of("", "~~~~~~~~~~~", "Enter amount of", "the item to be sold"))
								.reopenIfFail(false)
								.response((player, lines) -> {
									try {
										mGui.mAmountToSell = Integer.parseInt(lines[0]);
										mGui.open();
										return true;
									} catch (NumberFormatException e) {
										player.sendMessage(Component.text("Please enter a valid number.", NamedTextColor.RED));
										mGui.open();
										return false;
									}
								})
								.open(mGui.mPlayer);
							break;
						default:
							// Do nothing
					}
					mGui.update();
				}
			});

		// Change currency item
		mGui.setItem(1, 4, new GuiItem(MarketGuiIcons.buildChangeCurrencyIcon(mGui.mCurrencyItem), false))
			.onLeftClick(() -> mGui.switchToTab(mGui.TAB_CHOOSE_CURRENCY));


		// Price per item multiplier
		mGui.setItem(1, 5, new GuiItem(MarketGuiIcons.buildAddListingPricePerItemMultiplierIcon(mGui.mCurrencyItem, mGui.mPricePerItemAmount), false))
			.onClick((clickEvent) -> {
				int changeMultiplier = 1;
				switch (clickEvent.getClick()) {
					case SHIFT_LEFT:
						changeMultiplier = 16;
						// fall through to actually change the amount
					case LEFT:
						mGui.mPricePerItemAmount -= changeMultiplier;
						mGui.update();
						break;
					case SHIFT_RIGHT:
						changeMultiplier = 16;
						// fall through to actually change the amount
					case RIGHT:
						mGui.mPricePerItemAmount += changeMultiplier;
						mGui.update();
						break;
					case SWAP_OFFHAND:
						mGui.close();
						SignUtils.newMenu(List.of("", "~~~~~~~~~~~", "Enter price per", "item in " + StringUtils.getCurrencyShortForm(mGui.mCurrencyItem)))
							.reopenIfFail(false)
							.response((player, lines) -> {
								try {
									mGui.mPricePerItemAmount = (int) WalletManager.parseDoubleOrCalculation(lines[0]);
									if (mGui.mPricePerItemAmount < 0) {
										player.sendMessage(Component.text("Please enter a valid number.", NamedTextColor.RED));
										mGui.open();
										return false;
									}
									mGui.open();
									return true;
								} catch (NumberFormatException e) {
									player.sendMessage(Component.text("Please enter a valid number.", NamedTextColor.RED));
									mGui.open();
									return false;
								}
							})
							.open(mGui.mPlayer);
						break;
					default:
						// do nothing
				}
			});

		// small middleman check
		if (mGui.mItemToSell != null && mGui.mCurrencyItem != null) {
			List<String> errorMessages = MarketManager.itemIsSellable(mGui.mPlayer, mGui.mItemToSell, mGui.mCurrencyItem);
			if (!errorMessages.isEmpty()) {
				for (String message : errorMessages) {
					mGui.mPlayer.sendMessage(Component.text(message, NamedTextColor.RED).decoration(TextDecoration.BOLD, true));
				}
				mGui.mPlayer.playSound(mGui.mPlayer.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1, 1);
				mGui.mItemToSell = null;
				mGui.update();
				return;
			}
		}

		// confirm button
		boolean invalidAmounts = mGui.mAmountToSell < 0 || mGui.mAmountToSell > 36*64 /*fullInvOfItems*/ || mGui.mPricePerItemAmount < 0 || mGui.mPricePerItemAmount > 36*64*64*8 /* full inv of HXP */;
		if (!invalidAmounts) {
			long total = (long)mGui.mAmountToSell * (long)mGui.mPricePerItemAmount;
			if (total < 0 || total > (long)Integer.MAX_VALUE) {
				invalidAmounts = true;
			}
		}
		if (!invalidAmounts && mGui.mPricePerItemAmount > 0 && mGui.mAmountToSell > 0 && mGui.mItemToSell != null && mGui.mCurrencyItem != null) {
			// amount parameter in MarketManager#calculateTaxDebt must be > 0 or else error occurs
			WalletUtils.Debt taxDebt = MarketManager.getInstance().calculateTaxDebt(mGui.mPlayer, mGui.mCurrencyItem, mGui.mPricePerItemAmount * mGui.mAmountToSell);
			GuiItem guiItem = mGui.setItem(1, 7, new GuiItem(MarketGuiIcons.buildAddListingConfirmWithTaxStatusIcon(taxDebt), false));
			if (taxDebt.mMeetsRequirement) {
				// only do the click action if player can pay the tax
				guiItem.onClick((clickEvent) -> {
					if (MarketGui.initiatePlayerAction(mGui.mPlayer)) {
						return;
					}
					if (mGui.mItemToSell != null && mGui.mCurrencyItem != null) {
						MarketManager.getInstance().addNewListing(mGui.mPlayer, mGui.mItemToSell, mGui.mAmountToSell, mGui.mPricePerItemAmount, mGui.mCurrencyItem, taxDebt);
						MarketGui.endPlayerAction(mGui.mPlayer);
						mGui.switchToTab(mGui.TAB_PLAYER_LISTINGS);
					}
				});
			}
		} else {
			mGui.setItem(1, 7, new GuiItem(MarketGuiIcons.ADDLISTING_CONFIRM_ERROR_WRONG_PARAMS, false));
		}
	}

	@Override
	public void onSwitch() {
		mGui.setTitle(TAB_TITLE);
		mGui.setSize(TAB_SIZE);
	}
}
