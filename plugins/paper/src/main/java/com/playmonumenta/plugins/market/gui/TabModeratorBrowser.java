package com.playmonumenta.plugins.market.gui;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.market.MarketListing;
import com.playmonumenta.plugins.market.MarketRedisManager;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;

public class TabModeratorBrowser implements MarketGuiTab {

	MarketGui mGui;

	static final Component TAB_TITLE = Component.text("Moderation Listing Browser");
	static final int TAB_SIZE = 6 * 9;

	public TabModeratorBrowser(MarketGui marketGUI) {
		this.mGui = marketGUI;
	}

	@Override
	public void setup() {
		// top bar is pretty much static
		int maxpage = (int) (mGui.mIsLoadingData >= 2 && mGui.mBrowserListingsIDList != null ? Math.ceil((double) mGui.mBrowserListingsIDList.size() / 45) : 0);
		mGui.setupTopBar(true, maxpage);

		if (mGui.mIsLoadingData <= 3) {
			mGui.setItem(3, 4, MarketGuiIcons.LOADING);
		} else if (mGui.mBrowserListingsForPage != null) {
			// display items
			int i = 9;
			for (MarketListing listing : mGui.mBrowserListingsForPage) {
				if (i >= 54) {
					break;
				}
				mGui.setItem(i++, listing.getListingDisplayItemStack(mGui.mPlayer, mGui.TAB_BAZAAR_BROWSER))
					.onClick((clickEvent) -> {
						mGui.mBuyListingFocusedListing = listing;
						mGui.switchToTab(mGui.TAB_BUY_LISTING);
					});
			}
		}

		if (mGui.mIsLoadingData == 0) {
			// in async, load all the listingsID that are available to the player, according to his filters,
			// and refresh the page when list is loaded
			mGui.mIsLoadingData = 1;
			Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
				mGui.mBrowserListingsIDList = MarketRedisManager.getAllListingsIds();
				if (!(mGui.mCurrentTab instanceof TabModeratorBrowser)) {
					// player might have canceled the search, no need to keep going
					return;
				}
				mGui.mIsLoadingData = 2;
				mGui.update();
			});

		}

		if (mGui.mIsLoadingData == 2) {
			// in async, load the listings that are wanted by the listingIDList, according to the current page,
			// and refresh the page when list is loaded
			mGui.mIsLoadingData = 3;
			Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
				mGui.commonLoadListingsInPageFromLoadedListingsIdList(mGui.mCurrentTab);
			});
		}
	}

	@Override
	public void onSwitch() {
		mGui.setTitle(TAB_TITLE);
		mGui.setSize(TAB_SIZE);
		mGui.mBrowserListingsIDList = null;
		mGui.mBrowserListingsForPage = null;
		mGui.mIsLoadingData = 0;
		mGui.mCurrentPage = 0;
	}
}
