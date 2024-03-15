package com.playmonumenta.plugins.market;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.guis.Gui;
import com.playmonumenta.plugins.guis.GuiItem;
import com.playmonumenta.plugins.inventories.WalletManager;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import com.playmonumenta.plugins.utils.SignUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.plugins.utils.WalletUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

public class MarketGUI extends Gui {

	// represents the current tab the player is seeing
	private MarketGuiTab mCurrentTab;

	public enum MarketGuiTab {
		MAIN_MENU(
			MarketGUI::setupMainMenu,
			Component.text("Market"),
			6 * 9),
		ACTIVE_LISTINGS_BROWSER(
			MarketGUI::setupActiveListingBrowser,
			Component.text("View All Listings"),
			6 * 9),
		PLAYER_LISTINGS(
			MarketGUI::setupPlayerListings,
			Component.text("Your Listings"),
			6 * 9),
		ADD_LISTING(
			MarketGUI::setupAddListingMenu,
			Component.text("New Listing"),
			3 * 9),
		CHOOSE_CURRENCY(
			MarketGUI::setupChooseCurrencyMenu,
			Component.text("Select Currency"),
			5*9),
		MODERATION_GLOBAL_ACTIONS(
			MarketGUI::setupModeratorMenu,
			Component.text("Moderation"),
			9),
		LISTINGS_BROWSER(
			MarketGUI::setupModeratorListingBrowser,
			Component.text("View All Listings"),
			6 * 9),
		BUY_LISTING(
			MarketGUI::setupBuyListing,
			Component.text("Buy Listing"),
			6 * 9),
		NOT_IMPLEMENTED(
			null,
			null,
			0)
		;
		private final @Nullable Component mTitle;
		private final int mSize;
		private final @Nullable Consumer<MarketGUI> mSetupMethod;

		MarketGuiTab(@Nullable Consumer<MarketGUI> setupMethod, @Nullable Component title, int size) {
			mTitle = title;
			mSize = size;
			mSetupMethod = setupMethod;
		}
	}

	static final String[][] CURRENCY_LOOTTABLE = new String[][]{
		{"epic:r1/items/currency/experience", "epic:r1/items/currency/concentrated_experience", "epic:r1/items/currency/hyper_experience"},
		{"epic:r2/items/currency/crystalline_shard", "epic:r2/items/currency/compressed_crystalline_shard", "epic:r2/items/currency/hyper_crystalline_shard"},
		{"epic:r3/items/currency/archos_ring", "epic:r3/items/currency/hyperchromatic_archos_ring"},
	};

	// represents the current page the player is seeing, inside a given tab
	// this is used for the tab 'listings' or 'your listings'
	int mCurrentPage = 1;

	// represents the item that is going to be sold
	@Nullable ItemStack mItemToSell = null;

	// represents the amount of item to be sold
	int mAmountToSell = 0;

	// represents the price per item
	int mPricePerItemAmount = 1;

	// represents the currency of the new listing
	@Nullable ItemStack mCurrencyItem = InventoryUtils.getItemFromLootTableOrWarn(mPlayer.getLocation(), NamespacedKeyUtils.fromString(CURRENCY_LOOTTABLE[1][1]));

	// status for the ongling data loading
	// 0 -> no data is loaded
	// 1 -> data is loading the list of listings to be displayed
	// 2 -> list loaded
	// 3 -> list loaded, loading the listings data of the page
	// 4 -> data loaded
	int mIsLoadingData = 0;

	List<Long> mPlayerListingsIds;

	@Nullable List<Long> mBrowserListingsIDList;
	@Nullable List<MarketListing> mBrowserListingsForPage;

	// Variables for BUY_LISTING
	@Nullable MarketListing mBuyListingFocusedListing;
	int mBuyListingMultiplier;

	boolean mIsOp;

	int mPlayerMaxListings;

	private static final HashSet<String> mMarketOngoingPlayerActions = new HashSet<>();

	public MarketGUI(Player player, MarketGuiTab startingTab) {
		super(player, 6 * 9, Component.text("Market"));
		mCurrentTab = startingTab;
		mIsOp = player.hasPermission("monumenta.command.market");
		mPlayerListingsIds = MarketManager.getInstance().getListingsOfPlayer(player);
		mPlayerMaxListings = MarketManager.getConfig().mAmountOfPlayerListingsSlots;
	}

	@Override
	protected void setup() {

		if (mCurrentTab.mSetupMethod == null) {
			this.mPlayer.sendMessage(Component.text("Tab " + mCurrentTab + " Not implemented", NamedTextColor.DARK_RED).decoration(TextDecoration.BOLD, true));
			mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_SHULKER_HURT, 1.0f, 0.7f);
			mCurrentTab = MarketGuiTab.MAIN_MENU;
			setup();
		} else {
			setTitle(mCurrentTab.mTitle != null ? mCurrentTab.mTitle : Component.text("Null"));
			setSize(mCurrentTab.mSize);
			mCurrentTab.mSetupMethod.accept(this);
		}
	}

	private void switchToTab(MarketGuiTab nextTab) {
		// insert specific actions to do when leaving a tab here
		switch (mCurrentTab) {
			case ACTIVE_LISTINGS_BROWSER:
			case LISTINGS_BROWSER:
			case BUY_LISTING:
				mBrowserListingsIDList = null;
				mBrowserListingsForPage = null;
				mIsLoadingData = 0;
				break;
			default:
				//do nothing
				break;
		}

		// insert specific actions to do when entering a tab here
		switch (nextTab) {
			case BUY_LISTING:
				mIsLoadingData = 2;
				mBuyListingMultiplier = 1;
				break;
			case ACTIVE_LISTINGS_BROWSER:
			case LISTINGS_BROWSER:
				mBrowserListingsIDList = null;
				mBrowserListingsForPage = null;
				mIsLoadingData = 0;
				break;
			case PLAYER_LISTINGS:
				mIsLoadingData = 2;
				break;
			default:
				//do nothing
				break;
		}

		// insert here things to do every time the tab gets changed
		mCurrentPage = 0;

		mCurrentTab = nextTab;
		update();
	}

	private void commonChangePageClickEvent(InventoryClickEvent event, int maxPage) {
		if (event.getClick() == ClickType.RIGHT) {
			mCurrentPage++;
			if (mCurrentPage + 1 > maxPage) {
				mCurrentPage = 0;
			}
		} else if (event.getClick() == ClickType.LEFT) {
			mCurrentPage--;
			if (mCurrentPage < 0) {
				mCurrentPage = maxPage - 1;
			}
		}
		mIsLoadingData = 2;
		update();
	}

	private void setupBuyListing() {

		if (mBuyListingFocusedListing == null) {
			mPlayer.sendMessage("Buy listing tab does not have a focused listing. this shouldnt happen, but will not lead to issues. sending viewer back to browser");
			switchToTab(MarketGuiTab.ACTIVE_LISTINGS_BROWSER);
			return;
		}

		if (mIsLoadingData == 2) {
			// in async, refresh the listing that is wanted by the gui,
			// and refresh the gui when listing is loaded
			mIsLoadingData = 3;
			Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
				if (mBuyListingFocusedListing != null) {
					mBuyListingFocusedListing = MarketRedisManager.getListing(mBuyListingFocusedListing.getId());
					if (mCurrentTab != MarketGuiTab.BUY_LISTING) {
						// player might have canceled the search, no need to keep going
						return;
					}
					mIsLoadingData = 4;
					update();
				} else {
					switchToTab(MarketGuiTab.ACTIVE_LISTINGS_BROWSER);
				}
			});
		}

		ItemStack currency = mBuyListingFocusedListing.getItemToBuy().clone();
		currency.setAmount(mBuyListingFocusedListing.getAmountToBuy());

		WalletUtils.Debt debt = WalletUtils.calculateInventoryAndWalletDebt(currency, mPlayer, true);

		long amountAvailable = Math.min(debt.mNumInWallet + debt.mNumInInventory, (long)mBuyListingFocusedListing.getAmountToSellRemaining() * mBuyListingFocusedListing.getAmountToBuy());
		int maxMultiplier = (int)(amountAvailable / mBuyListingFocusedListing.getAmountToBuy());

		// currency display
		setItem(2, 2, currency);

		// multiplier button
		setItem(2, 4, createTradeMultiplierButton(maxMultiplier))
			.onClick((inventoryClickEvent) -> {
				int offset = 1;
				if (inventoryClickEvent.isShiftClick()) {
					offset = 8;
				}
				mBuyListingMultiplier = modifyTradeMultiplier(mBuyListingMultiplier, inventoryClickEvent.isLeftClick() ? -offset : offset, maxMultiplier);
				update();
			});

		// itemToBeSold display
		ItemStack listingItemStack = mBuyListingFocusedListing.getItemToSell();
		setItem(2, 6, listingItemStack);


		// cancel button
		setItem(4, 3, GUIUtils.createCancel(List.of(Component.text("Return to the previous page.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))))
			.onClick((clickEvent) -> {
				switchToTab(MarketGuiTab.ACTIVE_LISTINGS_BROWSER);
			});

		// refresh button
		if (mIsLoadingData <= 3) {
			setItem(4, 4, GUIUtils.createBasicItem(Material.SUNFLOWER, "Loading", NamedTextColor.GOLD, true));
		} else {
			setItem(4, 4, GUIUtils.createBasicItem(Material.YELLOW_STAINED_GLASS_PANE, "Refresh", NamedTextColor.GOLD, true))
				.onClick((clickEvent) -> {
					mIsLoadingData = 2;
					update();
				});
		}

		// confirm button
		List<Component> errorLoreLines = new ArrayList<>();

		// these errors shouldn't happen often
		if (mIsLoadingData <= 3) {
			errorLoreLines.add(Component.text("Wait for the data to finish loading", NamedTextColor.GOLD));
		}

		MarketListingStatus purchasableStatus = mBuyListingFocusedListing.getPurchasableStatus(mBuyListingMultiplier);
		if (purchasableStatus.isError()) {
			switch (purchasableStatus) {
				case LOCKED:
					errorLoreLines.add(Component.text("Item is locked, you cannot buy it.", NamedTextColor.DARK_RED));
					break;
				case EXPIRED:
					errorLoreLines.add(Component.text("Item is expired, you cannot buy it.", NamedTextColor.DARK_RED));
					break;
				case NOT_ENOUGH_REMAINING:
					errorLoreLines.add(Component.text("not enough in stock! max: " + mBuyListingFocusedListing.getAmountToSellRemaining(), NamedTextColor.DARK_RED));
					break;
				default:
					//other purchasableStatuses aren't used here
					break;
			}
		}

		if (!debt.mMeetsRequirement) {
			errorLoreLines.add(Component.text("not enough Money to buy!", NamedTextColor.DARK_RED));
		}

		if (errorLoreLines.isEmpty()) {
			List<Component> confirmLore = new ArrayList<>();
			confirmLore.add(Component.text("Buy " + mBuyListingMultiplier + " " + ItemUtils.getPlainNameOrDefault(listingItemStack) + " for: ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
			confirmLore.add(Component.text((mBuyListingMultiplier * mBuyListingFocusedListing.getAmountToBuy()) + " " + ItemUtils.getPlainNameOrDefault(mBuyListingFocusedListing.getItemToBuy()), NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
			setItem(4, 5, GUIUtils.createConfirm(confirmLore))
				.onClick((clickEvent) -> {
					if (initiatePlayerAction(mPlayer)) {
						return;
					}
					Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
						if (mBuyListingFocusedListing != null) {
							MarketManager.performPurchase(mPlayer, mBuyListingFocusedListing, mBuyListingMultiplier);
						}
						mIsLoadingData = 2;
						endPlayerAction(mPlayer);
						update();
					});
				});
		} else {
			setItem(4, 5, GUIUtils.createExclamation(errorLoreLines));
		}

		if (mIsOp) {
			// moderation buttons
			List<Component> desc;

			if (mBuyListingFocusedListing.isExpired()) {

				desc = new ArrayList<>();
				desc.add(Component.text("Click this to attempt to de-expire the listing").decoration(TextDecoration.ITALIC, false));
				desc.add(Component.text("The listing will once again be visible/tradable").decoration(TextDecoration.ITALIC, false));
				desc.add(Component.text("But if the listing naturally got expired beforehand").decoration(TextDecoration.ITALIC, false));
				desc.add(Component.text("It will soon automatically expire again").decoration(TextDecoration.ITALIC, false));
				setItem(4, GUIUtils.createBasicItem(Material.TOTEM_OF_UNDYING, 1, Component.text("Un-expire the listing", NamedTextColor.GOLD), desc, true))
					.onClick((clickEvent) -> {
						Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
								if (mBuyListingFocusedListing != null) {
									MarketManager.unexpireListing(mPlayer, mBuyListingFocusedListing);
								}
							mIsLoadingData = 2;
							update();
						});
					});

			} else {

				if (mBuyListingFocusedListing.isLocked()) {
					desc = new ArrayList<>();
					desc.add(Component.text("Click this to unlock the listing").decoration(TextDecoration.ITALIC, false));
					desc.add(Component.text("The listing will once again be visible/tradable").decoration(TextDecoration.ITALIC, false));
					setItem(2, GUIUtils.createBasicItem(Material.GLASS, 1, Component.text("Unlock the listing", NamedTextColor.GOLD), desc, true))
						.onClick((clickEvent) -> {
							Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
								if (mBuyListingFocusedListing != null) {
									MarketManager.unlockListing(mPlayer, mBuyListingFocusedListing);
								}
								mIsLoadingData = 2;
								update();
							});
						});
				} else {
					desc = new ArrayList<>();
					desc.add(Component.text("Click this to lock the listing").decoration(TextDecoration.ITALIC, false));
					desc.add(Component.text("The listing will become hidden and not tradable").decoration(TextDecoration.ITALIC, false));
					desc.add(Component.text("until someone (usually the owner) unlocks it").decoration(TextDecoration.ITALIC, false));
					setItem(2, GUIUtils.createBasicItem(Material.IRON_BLOCK, 1, Component.text("Lock the listing", NamedTextColor.GOLD), desc, true))
						.onClick((clickEvent) -> {
							Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
								if (mBuyListingFocusedListing != null) {
									MarketManager.lockListing(mPlayer, mBuyListingFocusedListing);
								}
								mIsLoadingData = 2;
								update();
							});
						});
				}

				desc = new ArrayList<>();
				desc.add(Component.text("Click this to expire the listing").decoration(TextDecoration.ITALIC, false));
				desc.add(Component.text("The listing will become hidden and not tradable").decoration(TextDecoration.ITALIC, false));
				desc.add(Component.text("The owner will not be able to revert it").decoration(TextDecoration.ITALIC, false));
				desc.add(Component.text("The may only claim back the items for himself").decoration(TextDecoration.ITALIC, false));
				setItem(4, GUIUtils.createBasicItem(Material.WITHER_ROSE, 1, Component.text("Expire the listing", NamedTextColor.GOLD), desc, true))
					.onClick((clickEvent) -> {
						Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
							if (mBuyListingFocusedListing != null) {
								MarketManager.expireListing(mPlayer, mBuyListingFocusedListing);
							}
							mIsLoadingData = 2;
							update();
						});
					});

			}

			desc = new ArrayList<>();
			desc.add(Component.text("Click this to delete the listing").decoration(TextDecoration.ITALIC, false));
			desc.add(Component.text("THIS ACTION IS NOT REVERTABLE", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
			desc.add(Component.text("You will recieve the listing's claimable items,").decoration(TextDecoration.ITALIC, false));
			desc.add(Component.text("like if you were the owner").decoration(TextDecoration.ITALIC, false));
			desc.add(Component.text("The listing won't exist anymore").decoration(TextDecoration.ITALIC, false));
			setItem(6, GUIUtils.createBasicItem(Material.LAVA_BUCKET, 1, Component.text("Claim and delete the listing", NamedTextColor.GOLD), desc, true))
				.onClick((clickEvent) -> {
					if (initiatePlayerAction(mPlayer)) {
						return;
					}
					Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
						if (mBuyListingFocusedListing != null) {
							MarketManager.claimEverythingAndDeleteListing(mPlayer, mBuyListingFocusedListing);
						}
						endPlayerAction(mPlayer);
					});
					switchToTab(MarketGuiTab.MAIN_MENU);
				});

		}

	}

	private int modifyTradeMultiplier(int baseModifier, int offset, int maxMultiplier) {
		// Normal trade multiplier:
		if (baseModifier + offset > maxMultiplier) {
			mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.NEUTRAL, 0.6f, 0.8f);
			return 1;
		}
		if (baseModifier + offset < 1) {
			mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, SoundCategory.NEUTRAL, 0.6f, 1.2f);
			return maxMultiplier;
		}
		baseModifier += offset;
		mPlayer.playSound(mPlayer.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.NEUTRAL, 0.6f, 1.3f);
		return baseModifier;
	}

	private ItemStack createTradeMultiplierButton(int maxMultiplier) {
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
			bannerMeta.displayName(Component.text("Trade Multiplier: (" + mBuyListingMultiplier + "x)", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
			List<Component> lore = new ArrayList<>();
			lore.add(Component.text("Left click to decrease, right click to increase.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
			lore.add(Component.text("Hold shift to offset by 8.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
			if (mBuyListingMultiplier == 1) {
				lore.add(Component.empty());
				lore.add(Component.text("Left click to calculate your max multiplier.", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
			}
			if (mBuyListingMultiplier == maxMultiplier) {
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

	private void setupTopBar(boolean playerListingsVisible, int maxPage) {
		setItem(0, GUIUtils.createBasicItem(Material.BARRIER, "Back to main menu", NamedTextColor.GOLD, true)).onClick((clickEvent) -> switchToTab(MarketGuiTab.MAIN_MENU));
		if (playerListingsVisible) {
			setItem(1, GUIUtils.createBasicItem(Material.WRITTEN_BOOK, "Your Listings", NamedTextColor.GOLD, true)).onClick((clickEvent) -> switchToTab(MarketGuiTab.PLAYER_LISTINGS));
		}
		setItem(2, GUIUtils.createBasicItem(Material.WRITABLE_BOOK, "Add a listing", NamedTextColor.GOLD, true)).onClick((clickEvent) -> switchToTab(MarketGuiTab.ADD_LISTING));
		ArrayList<String> lore = new ArrayList<>();
		lore.add("Current page: " + (mCurrentPage + 1) + "/" + (maxPage == 0 ? "?" : maxPage));
		lore.add("Right click to go to next page");
		lore.add("Left click to go to previous page");
		setItem(8, GUIUtils.createBasicItem(Material.ARROW, Component.text("Change page", NamedTextColor.GOLD), lore, NamedTextColor.GRAY)).onClick((clickEvent) -> this.commonChangePageClickEvent(clickEvent, maxPage));
	}

	private void setupPlayerListings() {
		mBrowserListingsIDList = MarketManager.getInstance().getListingsOfPlayer(mPlayer);

		// top bar is pretty much static
		int maxpage = (int)(mIsLoadingData >= 2 ? Math.ceil((double)Math.max(mBrowserListingsIDList.size(), mPlayerMaxListings)/45) : 0);
		setupTopBar(false, maxpage);

		if (mIsLoadingData <= 3) {
			setItem(3, 4, GUIUtils.createBasicItem(Material.SUNFLOWER, "Loading", NamedTextColor.GOLD, true));
		} else if (mBrowserListingsForPage != null) {
			// display items
			Iterator<MarketListing> listingsIter = mBrowserListingsForPage.iterator();
			for (int i = 9; i < 54; i++) {
				if (listingsIter.hasNext()) {
					MarketListing listing = listingsIter.next();
					setItem(i, listing.getListingDisplayItemStack(mPlayer, MarketGuiTab.PLAYER_LISTINGS))
						.onClick((clickEvent) -> {
							if (initiatePlayerAction(mPlayer)) {
								return;
							}
							switch (clickEvent.getClick()) {
								case LEFT:
									if (listing.getAmountToClaim() != 0) {
										Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
											MarketManager.claimClaimable(mPlayer, listing);
											mIsLoadingData = 2;
											endPlayerAction(mPlayer);
											update();
										});
									} else {
										endPlayerAction(mPlayer);
									}
									break;
								case RIGHT:
									Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
										if (listing.isLocked()) {
											MarketManager.unlockListing(mPlayer, listing);
										} else {
											MarketManager.lockListing(mPlayer, listing);
										}
										mIsLoadingData = 2;
										endPlayerAction(mPlayer);
										update();
									});
									break;
								case SWAP_OFFHAND:
									Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
										MarketManager.claimEverythingAndDeleteListing(mPlayer, listing);
										mIsLoadingData = 2;
										endPlayerAction(mPlayer);
										update();
									});
									break;
								default:
									// Do nothing
									endPlayerAction(mPlayer);
							}
						});
				} else {
					if ((mCurrentPage * 45) + (i - 9) <= mPlayerMaxListings) {
						ArrayList<String> lores = new ArrayList<>();
						lores.add("This slot is available.");
						lores.add("click add a new listing");
						setItem(i, GUIUtils.createBasicItem(Material.LIME_STAINED_GLASS_PANE, Component.text("Empty slot", NamedTextColor.GOLD), lores, NamedTextColor.GRAY))
							.onClick((clickEvent) -> switchToTab(MarketGuiTab.ADD_LISTING));
					}
				}
			}
		}

		if (mIsLoadingData == 2) {
			// in async, load the listings that are wanted by the listingIDList, according to the current page,
			// and refresh the page when list is loaded
			mIsLoadingData = 3;
			Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
				try {
					Thread.sleep(50);
				} catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
				commonLoadListingsInPageFromLoadedListingsIdList(mCurrentTab);
			});
		}

	}

	private void setupModeratorListingBrowser() {
		// top bar is pretty much static
		int maxpage = (int) (mIsLoadingData >= 2 && mBrowserListingsIDList != null ? Math.ceil((double) mBrowserListingsIDList.size() / 45) : 0);
		setupTopBar(true, maxpage);

		if (mIsLoadingData <= 3) {
			setItem(3, 4, GUIUtils.createBasicItem(Material.SUNFLOWER, "Loading", NamedTextColor.GOLD, true));
		} else if (mBrowserListingsForPage != null) {
			// display items
			int i = 9;
			for (MarketListing listing : mBrowserListingsForPage) {
				if (i >= 54) {
					break;
				}
				setItem(i++, listing.getListingDisplayItemStack(mPlayer, MarketGuiTab.LISTINGS_BROWSER))
					.onClick((clickEvent) -> {
						mBuyListingFocusedListing = listing;
						switchToTab(MarketGuiTab.BUY_LISTING);
					});
			}
		}

		if (mIsLoadingData == 0) {
			// in async, load all the listingsID that are available to the player, according to his filters,
			// and refresh the page when list is loaded
			mIsLoadingData = 1;
			Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
				mBrowserListingsIDList = MarketRedisManager.getAllListingsIds();
				Collections.sort(mBrowserListingsIDList);
				if (mCurrentTab != MarketGuiTab.LISTINGS_BROWSER) {
					// player might have canceled the search, no need to keep going
					return;
				}
				mIsLoadingData = 2;
				update();
			});

		}

		if (mIsLoadingData == 2) {
			// in async, load the listings that are wanted by the listingIDList, according to the current page,
			// and refresh the page when list is loaded
			mIsLoadingData = 3;
			Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
				commonLoadListingsInPageFromLoadedListingsIdList(mCurrentTab);
			});
		}
	}

	private void setupActiveListingBrowser() {
		// top bar is pretty much static
		int maxpage = (int) (mIsLoadingData >= 2 && mBrowserListingsIDList != null ? Math.ceil((double) mBrowserListingsIDList.size() / 45) : 0);
		setupTopBar(true, maxpage);

		// top bar is pretty much static
		setItem(0, GUIUtils.createBasicItem(Material.BARRIER, "Back to main menu", NamedTextColor.GOLD, true))
			.onClick((clickEvent) -> switchToTab(MarketGuiTab.MAIN_MENU));
		setItem(1, GUIUtils.createBasicItem(Material.WRITTEN_BOOK, "Your Listings", NamedTextColor.GOLD, true))
			.onClick((clickEvent) -> switchToTab(MarketGuiTab.PLAYER_LISTINGS));
		setItem(2, GUIUtils.createBasicItem(Material.WRITABLE_BOOK, "Add a listing", NamedTextColor.GOLD, true))
			.onClick((clickEvent) -> switchToTab(MarketGuiTab.ADD_LISTING));

		if (mIsLoadingData <= 3) {
			setItem(3, 4, GUIUtils.createBasicItem(Material.SUNFLOWER, "Loading", NamedTextColor.GOLD, true));
		} else if (mBrowserListingsForPage != null) {
			// display items
			int i = 9;
			for (MarketListing listing : mBrowserListingsForPage) {
				if (i >= 54) {
					break;
				}
				setItem(i++, listing.getListingDisplayItemStack(mPlayer, MarketGuiTab.ACTIVE_LISTINGS_BROWSER))
					.onClick((clickEvent) -> {
						mBuyListingFocusedListing = listing;
						switchToTab(MarketGuiTab.BUY_LISTING);
					});
			}
		}

		if (mIsLoadingData == 0) {
			// in async, load the listingsID that are available to the player, according to his filters,
			// and refresh the page when list is loaded
			mIsLoadingData = 1;
			Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
				mBrowserListingsIDList = MarketListingIndex.ACTIVE_LISTINGS.getListingsFromIndex(null, true);
				if (mCurrentTab != MarketGuiTab.ACTIVE_LISTINGS_BROWSER) {
					// player might have canceled the search, no need to keep going
					return;
				}
				mIsLoadingData = 2;
				update();
			});

		}

		if (mIsLoadingData == 2) {
			// in async, load the listings that are wanted by the listingIDList, according to the current page,
			// and refresh the page when list is loaded
			mIsLoadingData = 3;
			Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
				commonLoadListingsInPageFromLoadedListingsIdList(mCurrentTab);
			});
		}


	}

	private void commonLoadListingsInPageFromLoadedListingsIdList(MarketGuiTab tabAtStart) {
		if (mCurrentTab != tabAtStart) {
			// player might have canceled the search, no need to keep going
			return;
		}

		int searchIndex = mCurrentPage * 45;
		ArrayList<MarketListing> listingsForPage = new ArrayList<>();

		while (listingsForPage.size() < 45 && mBrowserListingsIDList != null && searchIndex < mBrowserListingsIDList.size()) {
			int tries = (45 - listingsForPage.size()) + 5;
			List<Long> browserListingsIDSublist = mBrowserListingsIDList.subList(searchIndex, Math.min(searchIndex + tries, mBrowserListingsIDList.size()));
			searchIndex += browserListingsIDSublist.size();

			if (!browserListingsIDSublist.isEmpty()) {
				List<MarketListing> listings = MarketRedisManager.getListings(browserListingsIDSublist.toArray(new Long[0]));
				if (mCurrentTab != tabAtStart) {
					// player might have canceled the search, no need to keep going
					return;
				}
				ArrayList<Long> listingsToRemove = new ArrayList<>();
				// check the validity of each listing
				int i = 0;
				for (MarketListing listing : listings) {
					if (listing == null) {
						if (tabAtStart == MarketGuiTab.PLAYER_LISTINGS) {
							MarketManager.getInstance().unlinkListingFromPlayerData(mPlayer, mBrowserListingsIDList.get(i));
							continue;
						}
					} else {
						if (listing.getPurchasableStatus(1).isError() && tabAtStart != MarketGuiTab.LISTINGS_BROWSER && tabAtStart != MarketGuiTab.PLAYER_LISTINGS) {
							listingsToRemove.add(listing.getId());
						} else {
							listingsForPage.add(listing);
						}
					}
					i++;
				}
				searchIndex -= listingsToRemove.size();
				mBrowserListingsIDList.removeAll(listingsToRemove);
			}
		}

		if (mCurrentTab != tabAtStart) {
			// player might have canceled the search, no need to keep going
			return;
		}

		mBrowserListingsForPage = listingsForPage.subList(0, Math.min(45, listingsForPage.size()));
		mIsLoadingData = 4;
		update();
	}


	private void setupAddListingMenu() {

		if (mPlayerListingsIds.size() >= mPlayerMaxListings) {
			mPlayer.sendMessage(Component.text("You do not have enough slots to add a new listing", NamedTextColor.RED));
			switchToTab(MarketGuiTab.PLAYER_LISTINGS);
			return;
		}

		// Minimum value of price per item is 0
		if (mPricePerItemAmount < 0) {
			mPricePerItemAmount = 0;
		}

		// Clamp the amount to between 1 and number of items in player inventory
		if (!ItemUtils.isNullOrAir(mItemToSell) && (mAmountToSell < 1 || !mPlayer.getInventory().containsAtLeast(mItemToSell, mAmountToSell))) {
			int numInInv = InventoryUtils.numInInventory(mPlayer.getInventory(), mItemToSell);
			if (numInInv == 0) {
				// the player will never have this item, even if we bring the wanted amount to 1.
				mItemToSell = null;
				mAmountToSell = 0;
			} else {
				// amount may be negative when changing the amount using the GuiItem
				mAmountToSell = Math.min(Math.max(mAmountToSell, 1), numInInv);
			}
		}

		// Modify the item to be sold
		if (ItemUtils.isNullOrAir(mItemToSell)) {
			setItem(1, 1, GUIUtils.createBasicItem(Material.STRUCTURE_VOID, "Nothing selected", NamedTextColor.RED, true, "Click on an item in your inventory to select the item to be sold!", NamedTextColor.GRAY));
		} else {
			List<Component> sellItemLore = new ArrayList<>();
			sellItemLore.add(mItemToSell.displayName());
			List<Component> lore = mItemToSell.lore();
			if (lore != null) {
				sellItemLore.addAll(lore);
			}

			ItemStack sellItemStack = GUIUtils.createBasicItem(mItemToSell.getType(), 1, "Item to be sold:", NamedTextColor.GOLD, true, sellItemLore, true);
			ItemUtils.setPlainName(sellItemStack, ItemUtils.getPlainName(mItemToSell));

			setItem(1, 1, new GuiItem(sellItemStack, false))
				.onClick((clickEvent) -> {
					mItemToSell = null;
					update();
				});
		}

        // Amounts item
		ItemStack amountItemStack = new ItemStack(Material.DARK_OAK_SIGN);
		ItemMeta meta = amountItemStack.getItemMeta();
		if (ItemUtils.isNullOrAir(mItemToSell)) {
			meta.displayName(Component.text("Put an item first!", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
		} else {
			meta.displayName(Component.text("Amount: ", NamedTextColor.GOLD, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false).append(Component.text(mAmountToSell, NamedTextColor.GOLD, TextDecoration.BOLD)));
			meta.lore(List.of(
				Component.text("Left click", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false).append(Component.text(" to decrease by 1", NamedTextColor.WHITE)),
				Component.text("Right click", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false).append(Component.text(" to increase by 1", NamedTextColor.WHITE)),
				Component.text("Shift click", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false).append(Component.text(" to offset by 64 instead", NamedTextColor.WHITE)),
				Component.keybind("key.swapOffhand", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false).append(Component.text(" to specify the amount", NamedTextColor.WHITE))
			));
		}
		amountItemStack.setItemMeta(meta);

		setItem(1, 2, amountItemStack)
			.onClick((clickEvent) -> {
				int changeMultiplier = 1;
				if (!ItemUtils.isNullOrAir(mItemToSell)) {
					switch (clickEvent.getClick()) {
						case SHIFT_LEFT:
							changeMultiplier = 64;
							// fall through to actually change the amount
						case LEFT:
							mAmountToSell -= changeMultiplier;
							update();
							break;
						case SHIFT_RIGHT:
							changeMultiplier = 64;
							// fall through to actually change the amount
						case RIGHT:
							mAmountToSell += changeMultiplier;
							update();
							break;
						case SWAP_OFFHAND:
							close();
							SignUtils.newMenu(List.of("", "~~~~~~~~~~~", "Enter amount of", "the item to be sold"))
								.reopenIfFail(false)
								.response((player, lines) -> {
									try {
										mAmountToSell = Integer.parseInt(lines[0]);
										open();
										return true;
									} catch (NumberFormatException e) {
										player.sendMessage(Component.text("Please enter a valid number.", NamedTextColor.RED));
										open();
										return false;
									}
								})
								.open(mPlayer);
							break;
						default:
							// Do nothing
					}
					update();
				}
			});

		// Change currency item
		setItem(1, 4, GUIUtils.createBasicItem(Material.PORKCHOP, 1, "Current Currency: " + StringUtils.getCurrencyShortForm(mCurrencyItem),
			NamedTextColor.GOLD, true, Component.text("Left click", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false).append(Component.text(" to select the currency", NamedTextColor.WHITE)), 32, true))
			.onLeftClick(() -> switchToTab(MarketGuiTab.CHOOSE_CURRENCY));


		// Price per item
		List<Component> currencyLore = new ArrayList<>(List.of(
			Component.text(mPricePerItemAmount + " " + ItemUtils.getPlainName(mCurrencyItem)).decoration(TextDecoration.ITALIC, false),
			Component.text("Left click", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false).append(Component.text(" to decrease by 1", NamedTextColor.WHITE)),
			Component.text("Right click", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false).append(Component.text(" to increase by 1", NamedTextColor.WHITE)),
			Component.text("Shift click", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false).append(Component.text(" to offset by 16 instead", NamedTextColor.WHITE)),
			Component.keybind("key.swapOffhand", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false).append(Component.text(" to specify the amount", NamedTextColor.WHITE)),
			Component.text("   (may be a simple calculation)", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)
		));

		ItemStack currencyItemStack = GUIUtils.createBasicItem(mCurrencyItem != null ? mCurrencyItem.getType() : Material.AIR, 1, "Price per item:", NamedTextColor.GOLD, true, currencyLore, true);
		ItemUtils.setPlainName(currencyItemStack, ItemUtils.getPlainName(mCurrencyItem));

		setItem(1, 5, new GuiItem(currencyItemStack, false))
			.onClick((clickEvent) -> {
				int changeMultiplier = 1;
				switch (clickEvent.getClick()) {
					case SHIFT_LEFT:
						changeMultiplier = 16;
						// fall through to actually change the amount
					case LEFT:
						mPricePerItemAmount -= changeMultiplier;
						update();
						break;
					case SHIFT_RIGHT:
						changeMultiplier = 16;
						// fall through to actually change the amount
					case RIGHT:
						mPricePerItemAmount += changeMultiplier;
						update();
						break;
					case SWAP_OFFHAND:
						close();
						SignUtils.newMenu(List.of("", "~~~~~~~~~~~", "Enter price per", "item in " + StringUtils.getCurrencyShortForm(mCurrencyItem)))
							.reopenIfFail(false)
							.response((player, lines) -> {
								try {
									mPricePerItemAmount = (int) WalletManager.parseDoubleOrCalculation(lines[0]);
									if (mPricePerItemAmount < 0) {
										player.sendMessage(Component.text("Please enter a valid number.", NamedTextColor.RED));
										open();
										return false;
									}
									open();
									return true;
								} catch (NumberFormatException e) {
									player.sendMessage(Component.text("Please enter a valid number.", NamedTextColor.RED));
									open();
									return false;
								}
							})
							.open(mPlayer);
						break;
					default:
						// do nothing
				}
			});

		// small middleman check
		if (mItemToSell != null && mCurrencyItem != null) {
			List<String> errorMessages = MarketManager.itemIsSellable(mPlayer, mItemToSell, mCurrencyItem);
			if (!errorMessages.isEmpty()) {
				for (String message : errorMessages) {
					mPlayer.sendMessage(Component.text(message, NamedTextColor.RED).decoration(TextDecoration.BOLD, true));
				}
				mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1, 1);
				mItemToSell = null;
				update();
				return;
			}
		}

		// confirm button
		ArrayList<Component> confirmLoreList = new ArrayList<>();
		boolean invalidAmounts = mAmountToSell < 0 || mAmountToSell > 36*64 /*fullInvOfItems*/ || mPricePerItemAmount < 0 || mPricePerItemAmount > 36*64*64*8 /* full inv of HXP */;
		if (!invalidAmounts) {
			long total = (long)mAmountToSell * (long)mPricePerItemAmount;
			if (total < 0 || total > (long)Integer.MAX_VALUE) {
				invalidAmounts = true;
			}
		}
		if (!invalidAmounts && mPricePerItemAmount > 0 && mAmountToSell > 0 && mItemToSell != null && mCurrencyItem != null) {
			// amount parameter in MarketManager#calculateTaxDebt must be > 0 or else error occurs
			WalletUtils.Debt taxDebt = MarketManager.getInstance().calculateTaxDebt(mPlayer, mCurrencyItem, mPricePerItemAmount * mAmountToSell);
			confirmLoreList.add(Component.text("To create a listing, you will need to pay a tax of:", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
			confirmLoreList.add(Component.text(taxDebt.mTotalRequiredAmount + " " + ItemUtils.getPlainName(taxDebt.mItem) + " ", NamedTextColor.WHITE)
				.append(Component.text(taxDebt.mMeetsRequirement ? "✓" : "✗", (taxDebt.mMeetsRequirement ? NamedTextColor.GREEN : NamedTextColor.RED)))
				.append(Component.text(taxDebt.mWalletDebt > 0 ? " (" + taxDebt.mNumInWallet + " in wallet)" : "", NamedTextColor.GRAY)).decoration(TextDecoration.ITALIC, false));
			confirmLoreList.add(Component.text(String.format("Tax Rate: %f%%", MarketManager.getConfig().mBazaarTaxRate*100), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
			if (taxDebt.mMeetsRequirement) {
				// Confirmation of adding a new listing
				setItem(1, 7, GUIUtils.createConfirm(confirmLoreList))
					.onClick((clickEvent) -> {
						if (initiatePlayerAction(mPlayer)) {
							return;
						}
						if (mItemToSell != null && mCurrencyItem != null) {
							MarketManager.getInstance().addNewListing(mPlayer, mItemToSell, mAmountToSell, mPricePerItemAmount, mCurrencyItem, taxDebt);
							endPlayerAction(mPlayer);
							switchToTab(MarketGuiTab.PLAYER_LISTINGS);
						}
					});
			} else {
				setItem(1, 7, GUIUtils.createExclamation(confirmLoreList));
			}
		} else {
			confirmLoreList.addAll(List.of(
				Component.text("Please either select an item, a valid amount,", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false),
				Component.text("or a valid price per item.", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
			));
			if (invalidAmounts) {
				confirmLoreList.add(Component.text("It seems the amounts of either the item or price leads to an error.", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
			}
			setItem(1, 7, GUIUtils.createExclamation(confirmLoreList));
		}

		setItem(0, 0, GUIUtils.createBasicItem(Material.BARRIER, "Back to main menu", NamedTextColor.GOLD, true))
			.onClick((clickEvent) -> switchToTab(MarketGuiTab.MAIN_MENU));
	}

	private void setupChooseCurrencyMenu() {
		for (int region = 0; region < CURRENCY_LOOTTABLE.length; region++) {
			for (int compression = 0; compression < CURRENCY_LOOTTABLE[region].length; compression++) {
				ItemStack currency = InventoryUtils.getItemFromLootTable(mPlayer, NamespacedKeyUtils.fromString(CURRENCY_LOOTTABLE[region][compression]));
				if (currency == null) {
					continue;
				}
				ItemStack currencyButton = currency.clone();
				currencyButton.lore(List.of(
					Component.text("Left click", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false).append(Component.text(" to select this currency", NamedTextColor.WHITE))
				));

				setItem(1 + region, 3 + compression, currencyButton)
					.onLeftClick(() -> {
						mCurrencyItem = currency;
						switchToTab(MarketGuiTab.ADD_LISTING);
					});
			}
		}
	}

	private void setupMainMenu() {
		setItem(1, 4, GUIUtils.createBasicItem(Material.BOOKSHELF, "Listings Browser", NamedTextColor.GOLD, true)).onClick((clickEvent) -> switchToTab(MarketGuiTab.ACTIVE_LISTINGS_BROWSER));
		setItem(3, 3, GUIUtils.createBasicItem(Material.WRITTEN_BOOK, "Your Listings", NamedTextColor.GOLD, true)).onClick((clickEvent) -> switchToTab(MarketGuiTab.PLAYER_LISTINGS));
		setItem(3, 5, GUIUtils.createBasicItem(Material.WRITABLE_BOOK, "Add a Listing", NamedTextColor.GOLD, true)).onClick((clickEvent) -> switchToTab(MarketGuiTab.ADD_LISTING));
		if (mIsOp) {
			setItem(5, 8, GUIUtils.createBasicItem(Material.REDSTONE_BLOCK, "Moderation global actions", NamedTextColor.GOLD, true)).onClick((clickEvent) -> switchToTab(MarketGuiTab.MODERATION_GLOBAL_ACTIONS));
		}
	}

	@Override
	protected void onPlayerInventoryClick(InventoryClickEvent event) {
		ItemStack item = event.getCurrentItem();
		if (mCurrentTab == MarketGuiTab.ADD_LISTING && !ItemUtils.isNullOrAir(item) && mCurrencyItem != null) {

			List<String> errorMessages = MarketManager.itemIsSellable(mPlayer, event.getCurrentItem(), mCurrencyItem);
			if (!errorMessages.isEmpty()) {
				for (String message : errorMessages) {
					mPlayer.sendMessage(Component.text(message, NamedTextColor.RED).decoration(TextDecoration.BOLD, true));
				}
				mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1, 1);
			} else {
				// Change the item to be sold, when the "Add listing" tab is open
				mAmountToSell = item.getAmount();
				mItemToSell = item.asQuantity(1);
				update();
			}
		}
	}

	private static boolean initiatePlayerAction(Player player) {
		if (mMarketOngoingPlayerActions.contains(player.getName())) {
			return true;
		}
		mMarketOngoingPlayerActions.add(player.getName());
		return false;
	}

	private static void endPlayerAction(Player player) {
		mMarketOngoingPlayerActions.remove(player.getName());
	}


	private void setupModeratorMenu() {
		setItem(0, GUIUtils.createBasicItem(Material.BARRIER, "Back to main menu", NamedTextColor.GOLD, true)).onClick((clickEvent) -> switchToTab(MarketGuiTab.MAIN_MENU));
		setItem(1, GUIUtils.createBasicItem(Material.BOOKSHELF, "Browse ALL listings", NamedTextColor.GOLD, true)).onClick((clickEvent) -> switchToTab(MarketGuiTab.LISTINGS_BROWSER));
		setItem(2, GUIUtils.createBasicItem(Material.BOOK, "Refresh all indexes", NamedTextColor.GOLD, true)).onClick((clickEvent) -> MarketListingIndex.resyncAllIndexes());
		setItem(6, GUIUtils.createBasicItem(Material.CHAIN, "Lock all trades", NamedTextColor.RED, true)).onClick((clickEvent) -> MarketManager.lockAllListings(mPlayer));
		setItem(7, GUIUtils.createBasicItem(Material.IRON_BARS, "Read-only mode", NamedTextColor.RED, true)).onClick((clickEvent) -> switchToTab(MarketGuiTab.NOT_IMPLEMENTED));
		setItem(8, GUIUtils.createBasicItem(Material.BARRIER, "Close market", NamedTextColor.RED, true)).onClick((clickEvent) -> switchToTab(MarketGuiTab.NOT_IMPLEMENTED));
	}
}
