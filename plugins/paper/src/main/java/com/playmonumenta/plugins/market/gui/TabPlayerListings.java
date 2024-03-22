package com.playmonumenta.plugins.market.gui;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.market.MarketListing;
import com.playmonumenta.plugins.market.MarketManager;
import com.playmonumenta.plugins.utils.GUIUtils;
import java.util.ArrayList;
import java.util.Iterator;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;

public class TabPlayerListings implements MarketGuiTab {

	MarketGui mGui;

	static final Component TAB_TITLE = Component.text("Your Listings");
	static final int TAB_SIZE = 6 * 9;

	public TabPlayerListings(MarketGui marketGUI) {
		this.mGui = marketGUI;
	}

	@Override
	public void setup() {
		mGui.mBrowserListingsIDList = MarketManager.getInstance().getListingsOfPlayer(mGui.mPlayer);

		// top bar is pretty much static
		int maxpage = (int) (mGui.mIsLoadingData >= 2 ? Math.ceil((double) Math.max(mGui.mBrowserListingsIDList.size(), mGui.mPlayerMaxListings) / 45) : 0);
		mGui.setupTopBar(false, maxpage);

		if (mGui.mIsLoadingData <= 3) {
			mGui.setItem(3, 4, MarketGuiIcons.LOADING);
		} else if (mGui.mBrowserListingsForPage != null) {
			// display items
			Iterator<MarketListing> listingsIter = mGui.mBrowserListingsForPage.iterator();
			for (int i = 9; i < 54; i++) {
				if (listingsIter.hasNext()) {
					MarketListing listing = listingsIter.next();
					mGui.setItem(i, listing.getListingDisplayItemStack(mGui.mPlayer, mGui.TAB_PLAYER_LISTINGS))
						.onClick((clickEvent) -> {
							if (MarketGui.initiatePlayerAction(mGui.mPlayer)) {
								return;
							}
							switch (clickEvent.getClick()) {
								case LEFT:
									if (listing.getAmountToClaim() != 0) {
										Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
											MarketManager.claimClaimable(mGui.mPlayer, listing);
											mGui.mIsLoadingData = 2;
											MarketGui.endPlayerAction(mGui.mPlayer);
											mGui.update();
										});
									} else {
										MarketGui.endPlayerAction(mGui.mPlayer);
									}
									break;
								case RIGHT:
									Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
										if (listing.isLocked()) {
											MarketManager.unlockListing(mGui.mPlayer, listing);
										} else {
											MarketManager.lockListing(mGui.mPlayer, listing);
										}
										mGui.mIsLoadingData = 2;
										MarketGui.endPlayerAction(mGui.mPlayer);
										mGui.update();
									});
									break;
								case SWAP_OFFHAND:
									Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
										MarketManager.claimEverythingAndDeleteListing(mGui.mPlayer, listing);
										mGui.mIsLoadingData = 2;
										MarketGui.endPlayerAction(mGui.mPlayer);
										mGui.update();
									});
									break;
								default:
									// Do nothing
									MarketGui.endPlayerAction(mGui.mPlayer);
							}
						});
				} else {
					if ((mGui.mCurrentPage * 45) + (i - 9) < mGui.mPlayerMaxListings) {
						ArrayList<String> lores = new ArrayList<>();
						lores.add("This slot is available.");
						lores.add("click add a new listing");
						mGui.setItem(i, GUIUtils.createBasicItem(Material.LIME_STAINED_GLASS_PANE, Component.text("Empty slot", NamedTextColor.GOLD), lores, NamedTextColor.GRAY))
							.onClick((clickEvent) -> mGui.switchToTab(mGui.TAB_ADD_LISTING));
					}
				}
			}
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
		mGui.mIsLoadingData = 2;
		mGui.mCurrentPage = 0;
	}
}
