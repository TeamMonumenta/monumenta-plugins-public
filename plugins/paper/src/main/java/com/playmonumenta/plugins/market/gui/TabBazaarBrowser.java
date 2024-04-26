package com.playmonumenta.plugins.market.gui;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.guis.GuiItem;
import com.playmonumenta.plugins.inventories.WalletManager;
import com.playmonumenta.plugins.market.MarketListing;
import com.playmonumenta.plugins.market.MarketListingIndex;
import com.playmonumenta.plugins.market.MarketManager;
import com.playmonumenta.plugins.market.MarketRedisManager;
import com.playmonumenta.plugins.market.filters.Comparator;
import com.playmonumenta.plugins.market.filters.FilterComponent;
import com.playmonumenta.plugins.market.filters.MarketFilter;
import com.playmonumenta.plugins.market.filters.Sorter;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.SignUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.codehaus.plexus.util.StringUtils;
import org.jetbrains.annotations.Nullable;

public class TabBazaarBrowser implements MarketGuiTab {

	MarketGui mGui;

	static final Component TAB_TITLE = Component.text("Market Browser - Bazaar");
	static final int TAB_SIZE = 6 * 9;

	public TabBazaarBrowser(MarketGui marketGUI) {
		this.mGui = marketGUI;
		this.mPlayer = mGui.mPlayer;
		this.mForcedPlayerFilter = MarketManager.getInstance().getForcedFiltersOfPlayer(mPlayer);
	}

	Player mPlayer;
	int mLoadingStatus;
	int mCurrentPage;
	@Nullable List<Long> mListingsIDList;
	@Nullable List<Long> mListingsOwnedIDList;
	@Nullable List<MarketListing> mListingsForPageList;

	int mSelectedFilter;
	List<MarketFilter> mLoadedMarketFilters;
	@Nullable String mQuicksearchValue;

	// filter that is forced onto the player every time for the basic non-op browsers.
	// this part of the filter is not saved in the player data
	// later, fill this one for the anti-progskip filter
	MarketFilter mForcedPlayerFilter;

	List<MarketListingIndex> mLoadedSortbyIndexes;
	int mSelectedSortByIndex;
	boolean mSortByDesc;

	@Override
	public void setup() {
		// top bar is pretty much static
		mGui.setItem(0, MarketGuiIcons.BACK_TO_MAIN_MENU).onClick((clickEvent) -> mGui.switchToTab(mGui.TAB_MAIN_MENU));
		mGui.setItem(1, MarketGuiIcons.PLAYER_LISTINGS).onClick((clickEvent) -> mGui.switchToTab(mGui.TAB_PLAYER_LISTINGS));
		mGui.setItem(2, MarketGuiIcons.ADD_LISTING).onClick((clickEvent) -> mGui.switchToTab(mGui.TAB_ADD_LISTING));
		mGui.setItem(3, buildQuickSearchIcon()).onClick((clickEvent) -> quickSearchAction());
		mGui.setItem(4, buildFilterSelectionIcon()).onClick((clickEvent) -> clickFilterAction(clickEvent));
		mGui.setItem(5, buildSortbySelectionIcon()).onClick((clickEvent) -> clickSortbyAction(clickEvent));
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

	private GuiItem buildSortbySelectionIcon() {
		ArrayList<Component> lore = new ArrayList<>();
		lore.add(Component.text("Left click, Right click", NamedTextColor.WHITE).append(Component.text(" or any", NamedTextColor.GRAY)));
		lore.add(Component.text("of your ", NamedTextColor.GRAY).append(Component.text("Hotbar keys", NamedTextColor.WHITE).append(Component.text(" to", NamedTextColor.GRAY))));
		lore.add(Component.text("cycle though sortable fields.", NamedTextColor.GRAY));
		if (mSortByDesc) {
			lore.add(Component.text("Current order: ", NamedTextColor.GRAY).append(Component.text("▼ Descending ▼", NamedTextColor.WHITE)));
			lore.add(Component.keybind("key.swapOffhand", NamedTextColor.WHITE).append(Component.text(" to switch to Ascending", NamedTextColor.GRAY)));
		} else {
			lore.add(Component.text("Current order: ", NamedTextColor.GRAY).append(Component.text("▲ Ascending ▲", NamedTextColor.WHITE)));
			lore.add(Component.keybind("key.swapOffhand", NamedTextColor.WHITE).append(Component.text(" to switch to Descending", NamedTextColor.GRAY)));
		}
		lore.add(Component.empty());
		lore.add(Component.text("Current selection:"));

		for (int i = 0; i < mLoadedSortbyIndexes.size(); i++) {
			NamedTextColor color = NamedTextColor.GRAY;
			String header = " ";
			if (mSelectedSortByIndex == i) {
				color = NamedTextColor.GREEN;
				header = "⌲";
			}

			lore.add(Component.text(header + "[" + mLoadedSortbyIndexes.get(i).toString() + "]", color));
		}

		ItemStack icon = GUIUtils.createBasicItem(Material.MAGENTA_GLAZED_TERRACOTTA, 1, Component.text("Sort By", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true), lore, false);

		return new GuiItem(icon, false);
	}

	private void clickSortbyAction(InventoryClickEvent clickEvent) {
		if (clickEvent.getClick().equals(ClickType.SWAP_OFFHAND)) {
			mSortByDesc = !mSortByDesc;
			mLoadingStatus = 0;
			mGui.update();
			return;
		}
		int hotbarInt = clickEvent.getHotbarButton();
		if (hotbarInt >= 0 && hotbarInt < mLoadedSortbyIndexes.size()) {
			mSelectedSortByIndex = hotbarInt;
		} else {
			mSelectedSortByIndex = mGui.commonMultiplierSelection(clickEvent, mSelectedSortByIndex + 1, mLoadedSortbyIndexes.size()) - 1;
		}
		mLoadingStatus = 0;
		mGui.update();
	}

	private void quickSearchAction() {
		mQuicksearchValue = null;
		mGui.close();
		SignUtils.newMenu(List.of("", "~~~~~~~~~~~", "Enter an item name", "'*' for any letters"))
			.reopenIfFail(false)
			.response((player, lines) -> {
				if (!StringUtils.isEmpty(lines[0])) {
					mQuicksearchValue = lines[0];
				}
				mLoadingStatus = 0;
				mGui.open();
				return true;
			})
			.open(mPlayer);
	}

	private GuiItem buildQuickSearchIcon() {
		ArrayList<Component> lore = new ArrayList<>();
		if (mQuicksearchValue != null) {
			lore.add(Component.text("Current search:", NamedTextColor.WHITE));
			lore.add(Component.text(mQuicksearchValue, NamedTextColor.WHITE));
			lore.add(Component.empty());
		}
		lore.add(Component.text("click this to quickly", NamedTextColor.GRAY));
		lore.add(Component.text("search by item name", NamedTextColor.GRAY));
		lore.add(Component.empty());
		lore.add(Component.text("this quicksearch will be added", NamedTextColor.GRAY));
		lore.add(Component.text("to the selected filter", NamedTextColor.GRAY));
		lore.add(Component.empty());
		lore.add(Component.text("If you do not know the exact", NamedTextColor.GRAY));
		lore.add(Component.text("item name you are looking for,", NamedTextColor.GRAY));
		lore.add(Component.text("you can replace unknown ", NamedTextColor.GRAY));
		lore.add(Component.text("characters with '*' ", NamedTextColor.GRAY));
		lore.add(Component.text("exemple: '*corrupted*'", NamedTextColor.GRAY));
		lore.add(Component.text("will search for all items with", NamedTextColor.GRAY));
		lore.add(Component.text("its name having 'corrupted' in it.", NamedTextColor.GRAY));

		ItemStack icon = GUIUtils.createBasicItem(Material.SPYGLASS, 1, Component.text("Quicksearch Item Name", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true), lore, false);

		return new GuiItem(icon, false);
	}

	private void clickFilterAction(InventoryClickEvent clickEvent) {
		if (clickEvent.getClick().equals(ClickType.SWAP_OFFHAND)) {
			mGui.switchToTab(mGui.TAB_EDIT_FILTERS);
			return;
		}
		int hotbarInt = clickEvent.getHotbarButton();
		if (hotbarInt >= 0 && hotbarInt < mLoadedMarketFilters.size()) {
			mSelectedFilter = hotbarInt;
		} else {
			mSelectedFilter = mGui.commonMultiplierSelection(clickEvent, mSelectedFilter + 1, mLoadedMarketFilters.size()) - 1;
		}
		mLoadingStatus = 0;
		mGui.update();
	}

	private GuiItem buildFilterSelectionIcon() {

		ArrayList<Component> lore = new ArrayList<>();
		lore.add(Component.text("Left click, Right click", NamedTextColor.WHITE).append(Component.text(" or any", NamedTextColor.GRAY)));
		lore.add(Component.text("of your ", NamedTextColor.GRAY).append(Component.text("Hotbar keys", NamedTextColor.WHITE).append(Component.text(" to", NamedTextColor.GRAY))));
		lore.add(Component.text("cycle though saved filters.", NamedTextColor.GRAY));
		lore.add(Component.keybind("key.swapOffhand", NamedTextColor.WHITE).append(Component.text(" to edit your filters.", NamedTextColor.GRAY)));
		lore.add(Component.empty());
		lore.add(Component.text("Current selection:"));

		for (int i = 0; i < mLoadedMarketFilters.size(); i++) {
			NamedTextColor color = NamedTextColor.GRAY;
			String header = " ";
			if (mSelectedFilter == i) {
				color = NamedTextColor.GREEN;
				header = "⌲";
			}

			lore.add(Component.text(header + "[" + mLoadedMarketFilters.get(i).getDisplayName() + "]", color));
		}

		ItemStack icon = GUIUtils.createBasicItem(Material.BRUSH, 1, Component.text("Select a Filter", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true), lore, false);

		return new GuiItem(icon, false);
	}

	private void loadItems() {
		if (mLoadingStatus == 0) { // initial data load
			// in async, load all the listingsID that are available to the player, according to his filters,
			// and refresh the page when list is loaded
			mLoadingStatus = 1;
			Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
				MarketFilter filter = MarketFilter.mergeOf(mForcedPlayerFilter, mLoadedMarketFilters.get(mSelectedFilter));
				if (mQuicksearchValue != null) {
					MarketFilter quicksearch = new MarketFilter("Quick Search : " + mQuicksearchValue, List.of(new FilterComponent(MarketListingIndex.NAME, Comparator.WHITELIST, List.of(mQuicksearchValue))));
					filter = MarketFilter.mergeOf(filter, quicksearch);
				}
				filter.setSorter(new Sorter(mLoadedSortbyIndexes.get(mSelectedSortByIndex), mSortByDesc));

				mListingsIDList = MarketRedisManager.getAllListingsIdsMatchingFilter(filter);
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
				ArrayList<Long> listingsToRemove = new ArrayList<>();
				// check the validity of each listing
				for (MarketListing listing : listings) {
					if (listing != null) {
						MarketManager.handleListingExpiration(mPlayer, listing);
						if (listing.getPurchasableStatus(1).isError()) {
							listingsToRemove.add(listing.getId());
						} else {
							listingsForPage.add(listing);
						}
					}
				}
				searchIndex -= listingsToRemove.size();
				mListingsIDList.removeAll(listingsToRemove);
			}
		}

		return listingsForPage.subList(0, Math.min(45, listingsForPage.size()));
	}

	private void displayItems() {
		// display items
		if (mListingsForPageList != null) {
			int i = 9;
			for (MarketListing listing : mListingsForPageList) {
				if (i >= 54) {
					break;
				}
				mGui.setItem(i++, listing.getListingDisplayItemStack(mGui.mPlayer, mGui.TAB_BAZAAR_BROWSER))
					.onClick((clickEvent) -> switchToBuyListingAction(listing));
			}
		}
	}

	private void switchToBuyListingAction(MarketListing listing) {
		if (mListingsOwnedIDList != null && mListingsOwnedIDList.contains(listing.getId())) { // operators can buy their own items.
			if (!mGui.mIsOp) {
				mPlayer.sendMessage(Component.text("(!) You cannot buy your own listing.", NamedTextColor.RED));
				return;
			}
		}
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

	@Override
	public void onSwitch() {
		mGui.setTitle(TAB_TITLE);
		mGui.setSize(TAB_SIZE);

		mPlayer = mGui.mPlayer;

		mLoadedMarketFilters = new ArrayList<>();
		mLoadedMarketFilters.add(MarketFilter.EMPTY_FILTER); // default filter
		mLoadedMarketFilters.addAll(MarketManager.getPlayerMarketFilters(mPlayer));

		mSelectedFilter = 0;

		mListingsOwnedIDList = MarketManager.getInstance().getListingsOfPlayer(mPlayer);

		mQuicksearchValue = null;

		mLoadedSortbyIndexes = MarketListingIndex.getAllPlayerSortable();
		mSortByDesc = true;

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
