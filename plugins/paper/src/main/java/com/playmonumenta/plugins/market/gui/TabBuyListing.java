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
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;

public class TabBuyListing implements MarketGuiTab {

	MarketGui mGui;

	static final Component TAB_TITLE = Component.text("Buy Listing");
	static final int TAB_SIZE = 6 * 9;

	int mBuyListingMultiplier;

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
		mGui.setItem(2, 4, createTradeMultiplierButton(mBuyListingMultiplier, maxMultiplier))
			.onClick((inventoryClickEvent) -> {
				int offset = 1;
				if (inventoryClickEvent.isShiftClick()) {
					offset = 8;
				}
				mBuyListingMultiplier = mGui.modifyTradeMultiplier(mBuyListingMultiplier, inventoryClickEvent.isLeftClick() ? -offset : offset, maxMultiplier);
				mGui.update();
			});

		// itemToBeSold display
		ItemStack listingItemStack = mGui.mFocusedListing.getItemToSell();
		if (mGui.mFocusedListing.getBundleSize() > 1) {
			ItemMeta meta = listingItemStack.getItemMeta();
			meta.displayName(Component.text(mGui.mFocusedListing.getBundleSize() + " * ", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false).append(ItemUtils.getDisplayName(mGui.mFocusedListing.getItemToSell())));
			listingItemStack.setItemMeta(meta);
			listingItemStack.setAmount(mGui.mFocusedListing.getBundleSize());
			ItemUtils.setPlainName(listingItemStack, ItemUtils.getPlainName(mGui.mFocusedListing.getItemToSell()));
		}
		mGui.setItem(2, 6, new GuiItem(listingItemStack, false));


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

		if (mBuyListingMultiplier <= 0 || mGui.mFocusedListing.getAmountToSellRemaining() <= 0) {
			errorLoreLines.add(Component.text("This listing ran out of stock!", NamedTextColor.DARK_RED));
		}

		if (!debt.mMeetsRequirement) {
			errorLoreLines.add(Component.text("Not enough Money to buy!", NamedTextColor.DARK_RED));
		}
		if (InventoryUtils.isFull(mGui.mPlayer.getInventory())) {
			errorLoreLines.add(Component.text("Your inventory is full.", NamedTextColor.DARK_RED));
		}

		if (errorLoreLines.isEmpty()) {
			MarketListingStatus purchasableStatus = mGui.mFocusedListing.getPurchasableStatus(mBuyListingMultiplier);
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
			confirmLore.add(Component.text("Buy " + mBuyListingMultiplier + " " + ItemUtils.getPlainNameOrDefault(listingItemStack) + " for: ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
			confirmLore.add(Component.text((mBuyListingMultiplier * mGui.mFocusedListing.getAmountToBuy()) + " " + ItemUtils.getPlainNameOrDefault(mGui.mFocusedListing.getCurrencyToBuy()), NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
			mGui.setItem(4, 5, GUIUtils.createConfirm(confirmLore))
				.onClick((clickEvent) -> {
					if (MarketGui.initiatePlayerAction(mGui.mPlayer)) {
						return;
					}
					Runnable endAction = () -> {
						mGui.mIsLoadingData = 2;
						MarketGui.endPlayerAction(mGui.mPlayer);
						mGui.update();
					};
					if (mGui.mFocusedListing != null) {
						Runnable cancelAction = () -> MarketGui.endPlayerAction(mGui.mPlayer);
						MarketManager.performPurchase(mGui.mPlayer, mGui.mFocusedListing, mBuyListingMultiplier, cancelAction, endAction);
					} else {
						endAction.run();
					}
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
								if (!MarketManager.unexpireListing(mGui.mPlayer, mGui.mFocusedListing)) {
									mGui.mPlayer.sendMessage(Component.text("Failed to un-expire listing, please retry momentarily.", NamedTextColor.RED));
								}
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
									if (!MarketManager.unlockListing(mGui.mPlayer, mGui.mFocusedListing)) {
										mGui.mPlayer.sendMessage(Component.text("Failed to unlock listing, please retry momentarily.", NamedTextColor.RED));
									}
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
									if (!MarketManager.lockListing(mGui.mPlayer, mGui.mFocusedListing)) {
										mGui.mPlayer.sendMessage(Component.text("Failed to lock listing, please retry momentarily.", NamedTextColor.RED));
									}
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
								if (!MarketManager.expireListing(mGui.mPlayer, mGui.mFocusedListing, "manual expiry")) {
									mGui.mPlayer.sendMessage(Component.text("Failed to expire listing, please retry momentarily.", NamedTextColor.RED));
								}
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
							if (!MarketManager.claimEverythingAndDeleteListing(mGui.mPlayer, mGui.mFocusedListing.getId())) {
								mGui.mPlayer.sendMessage(Component.text("Failed to delete listing, please retry momentarily.", NamedTextColor.RED));
							}
						}
						MarketGui.endPlayerAction(mGui.mPlayer);
					});
					mGui.switchToTab(mGui.TAB_MAIN_MENU);
				});

		}
	}

	ItemStack createTradeMultiplierButton(int currentMult, int maxMultiplier) {
		// Regular trade multiplier button:
		ItemStack banner = new ItemStack(Material.LIGHT_BLUE_BANNER);
		ItemMeta itemMeta = banner.getItemMeta();
		if (itemMeta instanceof BannerMeta bannerMeta) {
			// Add patterns for right-arrow:
			bannerMeta.addPattern(new Pattern(DyeColor.WHITE, PatternType.STRIPE_RIGHT));
			bannerMeta.addPattern(new Pattern(DyeColor.WHITE, PatternType.STRIPE_MIDDLE));
			bannerMeta.addPattern(new Pattern(DyeColor.LIGHT_BLUE, PatternType.STRIPE_TOP));
			bannerMeta.addPattern(new Pattern(DyeColor.LIGHT_BLUE, PatternType.STRIPE_BOTTOM));
			bannerMeta.addPattern(new Pattern(DyeColor.LIGHT_BLUE, PatternType.CURLY_BORDER));
			// Change name and lore:
			bannerMeta.displayName(Component.text("Trade Multiplier: (" + currentMult + "x)", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
			List<Component> lore = new ArrayList<>();
			lore.add(Component.text("Left click to decrease, right click to increase.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
			lore.add(Component.text("Hold shift to offset by 8.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
			if (currentMult == 1) {
				lore.add(Component.empty());
				lore.add(Component.text("Left click to calculate your max multiplier.", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
			}
			if (currentMult == maxMultiplier) {
				lore.add(Component.empty());
				lore.add(Component.text("right click to go back to multiplier x1.", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
			}
			bannerMeta.lore(lore);
			// Hide patterns:
			bannerMeta.addItemFlags(ItemFlag.HIDE_ITEM_SPECIFICS); // banner patterns are actually the same 'data' as potion effects, lmao
			// Finalize:
			banner.setItemMeta(bannerMeta);
		}
		return banner;
	}

	@Override
	public void onSwitch() {
		mGui.setTitle(TAB_TITLE);
		mGui.setSize(TAB_SIZE);
		mGui.mIsLoadingData = 2;
		mBuyListingMultiplier = 1;
	}

	@Override
	public void onLeave() {

	}
}
