package com.playmonumenta.plugins.market.gui;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.guis.GuiItem;
import com.playmonumenta.plugins.inventories.WalletManager;
import com.playmonumenta.plugins.market.MarketListing;
import com.playmonumenta.plugins.market.MarketManager;
import com.playmonumenta.plugins.market.MarketRedisManager;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.SignUtils;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.Nullable;

public class TabPlayerListings implements MarketGuiTab {

	MarketGui mGui;

	static final Component TAB_TITLE = Component.text("Your Listings");
	static final int TAB_SIZE = 6 * 9;

	public TabPlayerListings(MarketGui marketGUI) {
		this.mGui = marketGUI;
	}

	Player mPlayer;
	int mLoadingStatus;
	List<Long> mListingsIDList;
	@Nullable List<MarketListing> mListingsForPageList;
	int mCurrentPage;

	@Override
	public void setup() {
		mGui.setItem(0, MarketGuiIcons.BACK_TO_MAIN_MENU).onClick((clickEvent) -> mGui.switchToTab(mGui.TAB_MAIN_MENU));
		mGui.setItem(2, MarketGuiIcons.ADD_LISTING).onClick((clickEvent) -> mGui.switchToTab(mGui.TAB_ADD_LISTING));
		mGui.setItem(8, mGui.buildChangePageIcon(mCurrentPage, getMaxPageDisplayable())).onClick((clickEvent) -> changePageAction(clickEvent));

		if (mLoadingStatus == 4) {
			// items ready for display
			displayItems();
		} else {
			// items are not ready for display
			mGui.setItem(3, 4, MarketGuiIcons.LOADING);
		}

		loadItems();
	}

	private void loadItems() {
		if (mLoadingStatus == 2) {
			// in async, load the listings that are wanted by the listingIDList, according to the current page,
			// and refresh the page when list is loaded
			mLoadingStatus = 3;
			Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
				mListingsForPageList = loadListingsInPageFromLoadedListingsIdList();
				mLoadingStatus = 4;
				mGui.update();
			});
		}
	}

	private List<MarketListing> loadListingsInPageFromLoadedListingsIdList() {
		int searchIndex = mCurrentPage * 45;
		ArrayList<MarketListing> listingsForPage = new ArrayList<>();

		while (listingsForPage.size() < 45 && mListingsIDList != null && searchIndex < mListingsIDList.size()) {
			int tries = (45 - listingsForPage.size()) + 5;
			List<Long> browserListingsIDSublist = mListingsIDList.subList(searchIndex, Math.min(searchIndex + tries, mListingsIDList.size()));
			searchIndex += browserListingsIDSublist.size();

			if (!browserListingsIDSublist.isEmpty()) {
				List<MarketListing> listings = MarketRedisManager.getListings(browserListingsIDSublist.toArray(new Long[0]));
				MarketManager.unlinkListingsFromPlayerIfNotInList(mPlayer, browserListingsIDSublist, listings);
				// check the validity of each listing
				for (MarketListing listing : listings) {
					if (listing != null) {
						MarketManager.handleListingExpiration(mPlayer, listing);
						listingsForPage.add(listing);
					}
				}
			}
		}

		return listingsForPage.subList(0, Math.min(45, listingsForPage.size()));
	}

	private void displayItems() {
		if (mListingsForPageList != null) {
			Iterator<MarketListing> listingsIter = mListingsForPageList.iterator();
			for (int i = 9; i < 54; i++) {
				if (listingsIter.hasNext()) {
					MarketListing listing = listingsIter.next();
					mGui.setItem(i, listing.getListingDisplayItemStack(mPlayer, mGui.TAB_PLAYER_LISTINGS))
						.onClick((clickEvent) -> clickOnListingAction(clickEvent, listing));
				} else {
					if ((mCurrentPage * 45) + (i - 9) < mGui.mPlayerMaxListings) {
						mGui.setItem(i, buildOpenSlotIcon()).onClick((clickEvent) -> mGui.switchToTab(mGui.TAB_ADD_LISTING));
					}
				}
			}
		}
	}

	private void clickOnListingAction(InventoryClickEvent clickEvent, MarketListing listing) {
		if (MarketGui.initiatePlayerAction(mPlayer)) {
			return;
		}
		switch (clickEvent.getClick()) {
			case LEFT:
				mGui.mFocusedListing = listing;
				MarketGui.endPlayerAction(mPlayer);
				mGui.switchToTab(mGui.TAB_EDIT_LISTING);
				break;
			case RIGHT:
				if (listing.getAmountToClaim() != 0) {
					Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
						MarketManager.claimClaimable(mPlayer, listing);
						mLoadingStatus = 2;
						MarketGui.endPlayerAction(mPlayer);
						mGui.update();
					});
				} else {
					MarketGui.endPlayerAction(mPlayer);
				}
				break;
			default:
				// Do nothing
				MarketGui.endPlayerAction(mPlayer);
		}
	}

	private GuiItem buildOpenSlotIcon() {
		ArrayList<Component> lore = new ArrayList<>();
		lore.add(Component.text("This slot is available,", NamedTextColor.GRAY));
		lore.add(Component.text("Left Click to list an item.", NamedTextColor.GRAY));
		return new GuiItem(GUIUtils.createBasicItem(Material.LIME_STAINED_GLASS_PANE, 1, Component.text("Open Slot", NamedTextColor.GOLD), lore, true, "gui_openSlot"), false);
	}

	private void changePageAction(InventoryClickEvent clickEvent) {

		int maxPage = getMaxPageDisplayable();

		if (clickEvent.getClick() == ClickType.SWAP_OFFHAND) {
			mGui.close();
			SignUtils.newMenu(List.of("", "~~~~~~~~~~~", "Enter a number", "between 1 and " + maxPage))
				.reopenIfFail(false)
				.response((player, lines) -> {
					try {
						mCurrentPage = (int) WalletManager.parseDoubleOrCalculation(lines[0]) - 1;
						if (mCurrentPage >= maxPage) {
							mCurrentPage = 0;
						}
						if (mCurrentPage < 0) {
							mCurrentPage = maxPage;
						}
						mLoadingStatus = 2;
						mGui.open();
						return true;
					} catch (NumberFormatException e) {
						player.sendMessage(Component.text("Please enter a valid number.", NamedTextColor.RED));
						mGui.open();
						return false;
					}
				})
				.open(mPlayer);
		} else {
			mCurrentPage = mGui.commonMultiplierSelection(clickEvent, mCurrentPage + 1, maxPage) - 1;
			mLoadingStatus = 2;
			mGui.update();
		}

	}

	int getMaxPageDisplayable() {
		return (int) (mLoadingStatus >= 2 && mListingsIDList != null ? Math.ceil((double) mListingsIDList.size() / 45) : 0);
	}

	@Override
	public void onSwitch() {
		mGui.setTitle(TAB_TITLE);
		mGui.setSize(TAB_SIZE);

		mPlayer = mGui.mPlayer;
		mLoadingStatus = 2;
		mCurrentPage = 0;

		mListingsIDList = MarketManager.getInstance().getListingsOfPlayer(mPlayer);
	}

	@Override
	public void onLeave() {
		mLoadingStatus = -1;
	}
}
