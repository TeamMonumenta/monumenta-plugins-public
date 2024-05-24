package com.playmonumenta.plugins.market.gui;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.guis.GuiItem;
import com.playmonumenta.plugins.inventories.WalletManager;
import com.playmonumenta.plugins.market.MarketListing;
import com.playmonumenta.plugins.market.MarketManager;
import com.playmonumenta.plugins.market.MarketRedisManager;
import com.playmonumenta.plugins.utils.SignUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.jetbrains.annotations.Nullable;

public class TabModeratorBrowser implements MarketGuiTab {

	MarketGui mGui;

	static final Component TAB_TITLE = Component.text("Moderation Listing Browser");
	static final int TAB_SIZE = 6 * 9;

	public TabModeratorBrowser(MarketGui marketGUI) {
		this.mGui = marketGUI;
	}

	Player mPlayer;
	int mLoadingStatus;
	int mCurrentPage;
	@Nullable List<Long> mListingsIDList;
	@Nullable List<MarketListing> mListingsForPageList;

	@Override
	public void setup() {
		if (mLoadingStatus == -1) {
			// failsafe in case player cancels/leaves the page
			return;
		}

		mGui.setItem(0, MarketGuiIcons.BACK_TO_MAIN_MENU).onClick((clickEvent) -> mGui.switchToTab(mGui.TAB_MAIN_MENU));
		mGui.setItem(1, MarketGuiIcons.PLAYER_LISTINGS).onClick((clickEvent) -> mGui.switchToTab(mGui.TAB_PLAYER_LISTINGS));
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
		if (mLoadingStatus == 0) { // initial data load
			// in async, load all the listingsID that are available to the player, according to his filters,
			// and refresh the page when list is loaded
			mLoadingStatus = 1;
			Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
				mListingsIDList = MarketRedisManager.getAllListingsIds(true);
				mLoadingStatus = 2;
				mGui.update();
			});
		}

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

	private void displayItems() {
		if (mListingsForPageList != null) {
			int i = 9;
			for (MarketListing listing : mListingsForPageList) {
				if (i >= 54) {
					break;
				}
				mGui.setItem(i++, new GuiItem(listing.getListingDisplayItemStack(mPlayer, mGui.TAB_BAZAAR_BROWSER), false))
					.onClick((clickEvent) -> switchToBuyListingAction(listing));
			}
		}
	}

	void switchToBuyListingAction(MarketListing listing) {
		mGui.mFocusedListing = listing;
		mGui.switchToTab(mGui.TAB_BUY_LISTING);
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

	List<MarketListing> loadListingsInPageFromLoadedListingsIdList() {

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

	@Override
	public void onSwitch() {
		mGui.setTitle(TAB_TITLE);
		mGui.setSize(TAB_SIZE);

		mPlayer = mGui.mPlayer;
		mLoadingStatus = 0;
		mCurrentPage = 0;
		mListingsIDList = null;
		mListingsForPageList = null;
	}

	@Override
	public void onLeave() {
		mLoadingStatus = -1;
	}
}
