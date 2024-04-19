package com.playmonumenta.plugins.market.gui;

import com.playmonumenta.plugins.guis.GuiItem;
import com.playmonumenta.plugins.market.MarketManager;
import com.playmonumenta.plugins.market.filters.Comparator;
import com.playmonumenta.plugins.market.filters.FilterComponent;
import com.playmonumenta.plugins.market.filters.MarketFilter;
import com.playmonumenta.plugins.utils.GUIUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;

public class TabEditFilters implements MarketGuiTab {

	MarketGui mGui;
	Player mPlayer;

	static final Component TAB_TITLE = Component.text("Select A filter to edit");
	static final int TAB_SIZE = 6 * 9;

	List<MarketFilter> mPlayerFilters;
	int mSelectedFilterID;
	@Nullable MarketFilter mSelectedFilter;

	public TabEditFilters(MarketGui marketGUI) {
		this.mGui = marketGUI;
		this.mPlayer = mGui.mPlayer;
		this.mPlayerFilters = new ArrayList<>();
	}

	@Override
	public void setup() {
		// top bar is pretty much static
		mGui.setItem(0, buildBackToListingBrowserIcon()).onClick((clickEvent) -> mGui.switchToTab(mGui.TAB_BAZAAR_BROWSER));
		mGui.setItem(8, buildSaveIcon()).onClick((clickEvent) -> saveAction());

		// 2nd row: list of filters
		int i = 0;
		for (MarketFilter filter : mPlayerFilters) {
			mGui.setItem(9 + i, buildFilterSelectionIcon(filter)).onClick((clickEvent) -> clickOnFilterSelectionAction(clickEvent, filter));
			i++;
		}
		if (i < 9) {
			mGui.setItem(9 + i, buildNewFilterSelectionIcon()).onClick((clickEvent) -> clickOnNewFilterSelectionAction());
		}

		// 3+rd row: components of selected filter
		if (mSelectedFilter != null) {
			int j = 0;
			for (FilterComponent component : mSelectedFilter.getComponents()) {
				mGui.setItem(18 + j, buildComponentSelectionIcon(component)).onClick((clickEvent) -> clickOnComponentSelectionAction());
				j++;
			}
		}
	}

	private void clickOnComponentSelectionAction() {
	}

	private GuiItem buildComponentSelectionIcon(FilterComponent component) {

		ArrayList<Component> lore = new ArrayList<>();

		if (component.mComparator == Comparator.WHITELIST) {
			lore.add(Component.text("Whitelist:", NamedTextColor.WHITE).decoration(TextDecoration.BOLD, true));
		} else {
			lore.add(Component.text("Blacklist:", NamedTextColor.BLACK).decoration(TextDecoration.BOLD, true));
		}
		int i = 0;
		for (String s : component.mValuesList) {
			if (i == 9) {
				break;
			}
			lore.add(Component.text(s, NamedTextColor.GRAY));
			i++;
		}
		if (component.mValuesList.size() > 10) {
			lore.add(Component.text("and " + (component.mValuesList.size() - 10) + "more...", NamedTextColor.DARK_GRAY));
		}

		ItemStack icon = GUIUtils.createBasicItem(component.getTargetIndex().getDisplayIconMaterial(), 1, Component.text(component.getTargetIndex().toString(), component.mComparator == Comparator.WHITELIST ? NamedTextColor.WHITE : NamedTextColor.DARK_GRAY).decoration(TextDecoration.BOLD, true), lore, false);

		return new GuiItem(icon, false);

	}

	private void clickOnNewFilterSelectionAction() {

		String displayName = "New Filter #";
		int extra = 1;
		List<String> namesAlreadyExisting = getAllDisplayNamesAlreadyExisting(mPlayerFilters);
		while (namesAlreadyExisting.contains(displayName + extra)) {
			extra++;
		}
		String fullDisplayName = displayName + extra;

		mPlayerFilters.add(new MarketFilter(fullDisplayName, null));

		mGui.update();
	}

	private List<String> getAllDisplayNamesAlreadyExisting(List<MarketFilter> filters) {
		List<String> out = new ArrayList<>();
		for (MarketFilter f : filters) {
			if (f.getDisplayName() != null) {
				out.add(f.getDisplayName());
			}
		}
		return out;
	}

	private GuiItem buildNewFilterSelectionIcon() {
		ArrayList<Component> lore = new ArrayList<>();
		lore.add(Component.text("click to add a new filter.", NamedTextColor.GRAY));
		ItemStack icon = GUIUtils.createBasicItem(Material.EMERALD, 1, Component.text("Add a new filter", NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true), lore, false);
		return new GuiItem(icon, false);
	}

	private GuiItem buildFilterSelectionIcon(MarketFilter filter) {

		ArrayList<Component> lore = new ArrayList<>();

		Material mat;
		if (filter == mSelectedFilter) {
			mat = Material.SPYGLASS;
			lore.add(Component.text("This filter is currently selected!", NamedTextColor.GREEN));
		} else {
			mat = Material.BRUSH;
			lore.add(Component.text("click to select this filter.", NamedTextColor.GRAY));
		}
		lore.add(Component.text("press ", NamedTextColor.GRAY).append(Component.keybind("key.swapOffhand", NamedTextColor.WHITE).append(Component.text(" to remove this filter.", NamedTextColor.GRAY))));

		ItemStack icon;
		if (filter != null && filter.getDisplayName() != null) {
			icon = GUIUtils.createBasicItem(mat, 1, Component.text(filter.getDisplayName(), NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true), lore, false);
		} else {
			icon = GUIUtils.createBasicItem(mat, 1, Component.text("Unnamed", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true), lore, false);
		}
		return new GuiItem(icon, false);
	}

	private void clickOnFilterSelectionAction(InventoryClickEvent clickEvent, MarketFilter filter) {
		if (clickEvent.getClick().equals(ClickType.SWAP_OFFHAND)) {
			mPlayerFilters.remove(filter);
			mGui.update();
			return;
		}
		mSelectedFilter = filter;
		mGui.update();
	}

	private void saveAction() {
		MarketManager.setPlayerMarketFilters(mPlayer, mPlayerFilters);
	}

	private GuiItem buildBackToListingBrowserIcon() {
		ArrayList<Component> lore = new ArrayList<>();
		lore.add(Component.text("Your changes will not be saved!", NamedTextColor.RED).decoration(TextDecoration.BOLD, true));
		ItemStack icon = GUIUtils.createBasicItem(Material.BARRIER, 1, Component.text("Return to Main Menu", NamedTextColor.RED).decoration(TextDecoration.BOLD, true), lore, false);
		return new GuiItem(icon, false);
	}

	private GuiItem buildSaveIcon() {
		ArrayList<Component> lore = new ArrayList<>();
		lore.add(Component.text("Your changes will be saved!", NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true));
		ItemStack icon = GUIUtils.createBasicItem(Material.GREEN_STAINED_GLASS_PANE, 1, GUIUtils.formatName("Save your changes", NamedTextColor.GREEN, true),
			lore, true, "gui_checkmark");
		return new GuiItem(icon, false);
	}

	@Override
	public void onSwitch() {
		mGui.setTitle(TAB_TITLE);
		mGui.setSize(TAB_SIZE);
		mPlayer = mGui.mPlayer;
		mPlayerFilters = MarketManager.getPlayerMarketFilters(mPlayer);
		mSelectedFilterID = -1;
	}

	@Override
	public void onLeave() {

	}
}
