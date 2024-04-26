package com.playmonumenta.plugins.market.gui;

import com.playmonumenta.plugins.guis.GuiItem;
import com.playmonumenta.plugins.inventories.WalletManager;
import com.playmonumenta.plugins.market.MarketListingIndex;
import com.playmonumenta.plugins.market.MarketManager;
import com.playmonumenta.plugins.market.filters.Comparator;
import com.playmonumenta.plugins.market.filters.ComponentConfig;
import com.playmonumenta.plugins.market.filters.FilterComponent;
import com.playmonumenta.plugins.market.filters.MarketFilter;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.SignUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.codehaus.plexus.util.StringUtils;
import org.jetbrains.annotations.Nullable;

public class TabEditFilters implements MarketGuiTab {

	MarketGui mGui;
	Player mPlayer;

	static final Component TAB_TITLE_FILTER = Component.text("Select A Filter to edit");
	static final Component TAB_TITLE_COMPONENT = Component.text("Select A Component to edit");
	static final Component TAB_TITLE_NEW_COMPONENT = Component.text("Select Component field");
	static final int TAB_SIZE = 6 * 9;

	List<MarketFilter> mPlayerFilters;
	@Nullable MarketFilter mSelectedFilter;

	@Nullable FilterComponent mSelectedComponent;

	@Nullable List<String> mComponentCustomKeysList;
	@Nullable HashMap<String, Boolean> mComponentKnownKeys;
	@Nullable List<String> mComponentKnownKeysList;
	int mComponentSelectionPage;

	boolean mIsChoosingNewComponent;

	public TabEditFilters(MarketGui marketGUI) {
		this.mGui = marketGUI;
		this.mPlayer = mGui.mPlayer;
		this.mPlayerFilters = new ArrayList<>();
	}

	@Override
	public void setup() {

		if (mSelectedComponent != null) {
			setupEditComponent();
		} else if (mIsChoosingNewComponent) {
			setupChooseNewComponent();
		} else {
			setupChooseFilterToEdit();
			if (mSelectedFilter != null) {
				setupChooseComponentToEdit();
			}
		}

	}

	private void setupEditComponent() {

		mGui.setTitle(TAB_TITLE_COMPONENT);

		computeKeysList();


		mGui.setItem(0, buildBackToListingBrowserIcon()).onClick((clickEvent) -> mGui.switchToTab(mGui.TAB_BAZAAR_BROWSER));
		mGui.setItem(2, buildBackToChooseFilterButton()).onClick((clickEvent) -> {
			mSelectedFilter = null;
			mSelectedComponent = null;
			mGui.update();
		});
		mGui.setItem(4, buildBackToChooseComponentButton()).onClick((clickEvent) -> {
			mSelectedComponent = null;
			mGui.update();
		});
		mGui.setItem(6, mGui.buildChangePageIcon(mComponentSelectionPage, getMaxPageDisplayable())).onClick((clickEvent) -> changePageAction(clickEvent));

		mGui.setItem(8, buildSaveIcon()).onClick((clickEvent) -> saveAction());


		if (mComponentKnownKeysList != null && mComponentCustomKeysList != null && mComponentKnownKeys != null) {
			for (int i = 45 * mComponentSelectionPage; i < 45 * (mComponentSelectionPage + 1) && i < mComponentCustomKeysList.size() + 1 + mComponentKnownKeysList.size(); i++) {
				if (i < mComponentCustomKeysList.size()) {
					String value = mComponentCustomKeysList.get(i);
					mGui.setItem(9 + i % 45, buildCustomComponentValueIcon(value)).onClick((clickEvent) -> {
						if (mSelectedComponent != null) {
							mSelectedComponent.removeValue(value);
						}
						mGui.update();
					});
				} else if (i == mComponentCustomKeysList.size()) {
					mGui.setItem(9 + i % 45, buildNewCustomComponentValueIcon()).onClick((clickEvent) -> newCustomComponentValueAction());
				} else {
					String value = mComponentKnownKeysList.get(i - (mComponentCustomKeysList.size() + 1));
					boolean isUsed = mComponentKnownKeys.getOrDefault(value, false);

					mGui.setItem(9 + i % 45, buildToggleComponentKnownValue(value, isUsed)).onClick((clickEvent) -> {
						if (mSelectedComponent != null) {
							if (isUsed) {
								mSelectedComponent.removeValue(value);
							} else {
								mSelectedComponent.addValue(value);
							}
						}
						mGui.update();
					});

				}
			}
		}


	}

	private GuiItem buildBackToChooseComponentButton() {
		ArrayList<Component> lore = new ArrayList<>();
		ItemStack icon = GUIUtils.createBasicItem(Material.BARRIER, 1, Component.text("Back to Component selection menu", NamedTextColor.RED).decoration(TextDecoration.BOLD, true), lore, false);
		return new GuiItem(icon, false);
	}

	private void changePageAction(InventoryClickEvent clickEvent) {
		int maxPage = getMaxPageDisplayable();

		if (clickEvent.getClick() == ClickType.SWAP_OFFHAND) {
			mGui.close();
			SignUtils.newMenu(List.of("", "~~~~~~~~~~~", "Enter a number", "between 1 and " + maxPage))
				.reopenIfFail(false)
				.response((player, lines) -> {
					try {
						mComponentSelectionPage = (int) WalletManager.parseDoubleOrCalculation(lines[0]) - 1;
						if (mComponentSelectionPage >= maxPage) {
							mComponentSelectionPage = 0;
						}
						if (mComponentSelectionPage < 0) {
							mComponentSelectionPage = maxPage;
						}
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
			mComponentSelectionPage = mGui.commonMultiplierSelection(clickEvent, mComponentSelectionPage + 1, maxPage) - 1;
			mGui.update();
		}
	}

	private int getMaxPageDisplayable() {
		if (mComponentCustomKeysList != null && mComponentKnownKeysList != null) {
			return (mComponentCustomKeysList.size() + 1 + mComponentKnownKeysList.size()) / 45 + 1;
		}
		return 0;
	}

	private GuiItem buildCustomComponentValueIcon(String value) {
		ArrayList<Component> lore = new ArrayList<>();
		lore.add(Component.text("Click this to remove"));
		lore.add(Component.text("this value from the"));
		lore.add(Component.text("component."));

		ItemStack icon = GUIUtils.createBasicItem(Material.SPRUCE_SIGN, 1, Component.text("Custom value: " + value, NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true), lore, false);

		return new GuiItem(icon, false);
	}

	private GuiItem buildToggleComponentKnownValue(String value, boolean isUsed) {

		ComponentConfig.ComponentConfigObject config = null;
		if (mSelectedComponent != null && mSelectedComponent.mField.getComponentConfig() != null) {
			config = mSelectedComponent.mField.getComponentConfig().get(value);
		}

		ArrayList<Component> lore = new ArrayList<>();

		if (config != null) {
			lore.add(Component.text("Real value:", NamedTextColor.DARK_GRAY));
			lore.add(Component.text(value, NamedTextColor.DARK_GRAY));
		}

		if (isUsed) {
			lore.add(Component.text("Click this to remove", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
			lore.add(Component.text("this value from the", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
			lore.add(Component.text("component.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
		} else {
			lore.add(Component.text("Click this to add", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
			lore.add(Component.text("this value to the", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
			lore.add(Component.text("component.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
		}

		ItemStack icon;

		if (isUsed) {
			if (config != null && config.getDisplayItemStack() != null && config.getDisplayName() != null) {
				icon = config.getDisplayItemStack();
				ItemUtils.setName(icon, Component.text(config.getDisplayName(), NamedTextColor.GREEN));
				ItemUtils.setLore(icon, lore);
			} else {
				icon = GUIUtils.createBasicItem(Material.GREEN_CONCRETE, 1, Component.text(value, NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true), lore, false);
			}
		} else {
			if (config != null && config.getDisplayItemStack() != null && config.getDisplayName() != null) {
				icon = config.getDisplayItemStack();
				ItemUtils.setName(icon, Component.text(config.getDisplayName(), NamedTextColor.GRAY));
				ItemUtils.setLore(icon, lore);
			} else {
				icon = GUIUtils.createBasicItem(Material.GRAY_CONCRETE, 1, Component.text(value, NamedTextColor.GRAY).decoration(TextDecoration.BOLD, true), lore, false);
			}
		}

		return new GuiItem(icon, false);
	}

	private void newCustomComponentValueAction() {
		mGui.close();
		SignUtils.newMenu(List.of("", "~~~~~~~~~~~", "Enter a value", "'*' for any letters"))
			.reopenIfFail(false)
			.response((player, lines) -> {
				if (!StringUtils.isEmpty(lines[0])) {
					if (mSelectedComponent != null) {
						mSelectedComponent.addValue(lines[0]);
					}
				}
				mGui.open();
				return true;
			})
			.open(mPlayer);
	}

	private GuiItem buildNewCustomComponentValueIcon() {
		ArrayList<Component> lore = new ArrayList<>();
		lore.add(Component.text("click this to add", NamedTextColor.GRAY));
		lore.add(Component.text("a custom value", NamedTextColor.GRAY));
		lore.add(Component.empty());
		lore.add(Component.text("you can use '*', which", NamedTextColor.GRAY));
		lore.add(Component.text("corresponds to any character.", NamedTextColor.GRAY));
		lore.add(Component.text("example: '*corrupted*'", NamedTextColor.GRAY));
		lore.add(Component.text("will search for all values with", NamedTextColor.GRAY));
		lore.add(Component.text("its name having 'corrupted' in it.", NamedTextColor.GRAY));

		ItemStack icon = GUIUtils.createBasicItem(Material.CHERRY_HANGING_SIGN, 1, Component.text("Add a Custom Value", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true), lore, false);

		return new GuiItem(icon, false);
	}

	private void computeKeysList() {

		if (mSelectedComponent == null) {
			return;
		}

		List<String> indexKnownKeys;

		if (mComponentKnownKeysList == null) {
			// we just entered this part of the gui. need to fetch the index values from redis
			indexKnownKeys = mSelectedComponent.getTargetIndex().getListingsKeysFromIndex(false);
		} else {
			// the fetch already made is considered recent enough
			indexKnownKeys = mComponentKnownKeysList;
		}

		// create a boolean map, stating wether or not the key is used in the component
		// init with everything at false
		mComponentKnownKeys = new HashMap<>();
		mComponentKnownKeysList = new ArrayList<>();
		for (String key : indexKnownKeys) {
			mComponentKnownKeys.put(key, false);
			mComponentKnownKeysList.add(key);
		}
		mComponentCustomKeysList = new ArrayList<>();

		// from the full component value list, check if the key is present in the KnownComponentKey map.
		if (mSelectedComponent.mValuesList != null) {
			for (String usedKey : mSelectedComponent.mValuesList) {
				if (mComponentKnownKeys.containsKey(usedKey)) {
					// if it is, the key is known, and used in the component.
					mComponentKnownKeys.put(usedKey, true);
				} else {
					// if it isn't, the key is considered a custom key.
					mComponentCustomKeysList.add(usedKey);
				}
			}
		}

		mComponentCustomKeysList.sort(java.util.Comparator.naturalOrder());


		// first sort alphabetically, to be able to properly sort unordered values
		mComponentKnownKeysList.sort(java.util.Comparator.naturalOrder());


		if (mSelectedComponent.mField != MarketListingIndex.NAME && mSelectedComponent.mField.getComponentConfig() != null) {
			Map<String, ComponentConfig.ComponentConfigObject> config = mSelectedComponent.mField.getComponentConfig();
			mComponentKnownKeysList.sort((a, b) -> {
				ComponentConfig.ComponentConfigObject configA = config.get(a);
				ComponentConfig.ComponentConfigObject configB = config.get(b);
				return (configA == null ? 999999 : configA.getOrder()) - (configB == null ? 999999 : configB.getOrder());
			});
		}

	}

	private void setupChooseNewComponent() {
		mGui.setTitle(TAB_TITLE_NEW_COMPONENT);
		mGui.setItem(0, buildBackToListingBrowserIcon()).onClick((clickEvent) -> mGui.switchToTab(mGui.TAB_BAZAAR_BROWSER));
		mGui.setItem(2, buildBackToChooseFilterButton()).onClick((clickEvent) -> {
			mSelectedFilter = null;
			mGui.update();
		});
		mGui.setItem(8, buildSaveIcon()).onClick((clickEvent) -> saveAction());

		int i = 0;
		for (MarketListingIndex idx : MarketListingIndex.getAllPlayerSelectable()) {
			mGui.setItem(10 + i, buildNewComponentSelectionIcon(false, idx)).onClick((clickEvent) -> clickOnNewComponentSelectionAction(false, idx));
			mGui.setItem(19 + i, buildNewComponentSelectionIcon(true, idx)).onClick((clickEvent) -> clickOnNewComponentSelectionAction(true, idx));
			i++;
		}
	}

	private GuiItem buildNewComponentSelectionIcon(boolean isWhitelist, MarketListingIndex idx) {

		ArrayList<Component> lore = new ArrayList<>();

		if (isWhitelist) {
			lore.add(Component.text("Create a new ", NamedTextColor.GRAY).append(Component.text("WHITELIST", NamedTextColor.WHITE).decoration(TextDecoration.BOLD, true)).append(Component.text(" Component", NamedTextColor.GRAY)));
		} else {
			lore.add(Component.text("Create a new ", NamedTextColor.GRAY).append(Component.text("BLACKLIST", NamedTextColor.DARK_GRAY).decoration(TextDecoration.BOLD, true)).append(Component.text(" Component", NamedTextColor.GRAY)));
		}
		lore.add(Component.text("for the ", NamedTextColor.GRAY).append(Component.text(idx.toString(), NamedTextColor.WHITE).decoration(TextDecoration.BOLD, true)).append(Component.text(" field", NamedTextColor.GRAY)));

		ItemStack icon = GUIUtils.createBasicItem(idx.getDisplayIconMaterial(), 1, Component.text(idx.toString(), isWhitelist ? NamedTextColor.WHITE : NamedTextColor.DARK_GRAY).decoration(TextDecoration.BOLD, true), lore, false);

		return new GuiItem(icon, false);

	}

	private void clickOnNewComponentSelectionAction(boolean isWhitelist, MarketListingIndex idx) {
		if (mSelectedFilter != null) {
			mSelectedFilter.addComponent(new FilterComponent(idx, isWhitelist ? Comparator.WHITELIST : Comparator.BLACKLIST, null));
		}
		mIsChoosingNewComponent = false;
		mGui.update();
	}


	private void setupChooseComponentToEdit() {

		mGui.setItem(0, buildBackToListingBrowserIcon()).onClick((clickEvent) -> mGui.switchToTab(mGui.TAB_BAZAAR_BROWSER));
		mGui.setItem(2, buildBackToChooseFilterButton()).onClick((clickEvent) -> {
			mSelectedFilter = null;
			mGui.update();
		});
		mGui.setItem(8, buildSaveIcon()).onClick((clickEvent) -> saveAction());

		mGui.setItem(27, buildEditNameIcon()).onClick((clickEvent) -> clickOnEditNameAction());

		int i = 0;
		if (mSelectedFilter != null && mSelectedFilter.getComponents() != null) {
			for (FilterComponent component : mSelectedFilter.getComponents()) {
				mGui.setItem(28 + i, buildComponentSelectionIcon(component)).onClick((clickEvent) -> clickOnComponentSelectionAction(clickEvent, component));
				i++;
			}
			if (i < 35) {
				mGui.setItem(28 + i, buildNewComponentIcon()).onClick((clickEvent) -> {
					mIsChoosingNewComponent = true;
					mGui.update();
				});
			}
		}

	}

	private void clickOnEditNameAction() {
		mGui.close();
		SignUtils.newMenu(List.of("", "~~~~~~~~~~~", "Enter an filter name", ""))
			.reopenIfFail(false)
			.response((player, lines) -> {
				if (!StringUtils.isEmpty(lines[0])) {
					if (mSelectedFilter != null) {
						mSelectedFilter.setDisplayName(lines[0]);
					}
				}
				mGui.open();
				return true;
			})
			.open(mPlayer);
	}

	private GuiItem buildEditNameIcon() {
		ArrayList<Component> lore = new ArrayList<>();
		lore.add(Component.text("Current filter name:", NamedTextColor.GRAY));
		if (mSelectedFilter != null && mSelectedFilter.getDisplayName() != null) {
			lore.add(Component.text(mSelectedFilter.getDisplayName(), NamedTextColor.WHITE));
		}
		lore.add(Component.text("click to change the filter name.", NamedTextColor.GRAY));
		ItemStack icon = GUIUtils.createBasicItem(Material.ACACIA_HANGING_SIGN, 1, Component.text("Edit filter name", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true), lore, false);
		return new GuiItem(icon, false);
	}

	private GuiItem buildBackToChooseFilterButton() {
		ArrayList<Component> lore = new ArrayList<>();
		ItemStack icon = GUIUtils.createBasicItem(Material.BARRIER, 1, Component.text("Back to Filter selection menu", NamedTextColor.RED).decoration(TextDecoration.BOLD, true), lore, false);
		return new GuiItem(icon, false);
	}

	private void setupChooseFilterToEdit() {

		mGui.setTitle(TAB_TITLE_FILTER);

		mGui.setItem(0, buildBackToListingBrowserIcon()).onClick((clickEvent) -> mGui.switchToTab(mGui.TAB_BAZAAR_BROWSER));
		mGui.setItem(8, buildSaveIcon()).onClick((clickEvent) -> saveAction());

		// 2nd row: list of filters
		int i = 0;
		for (MarketFilter filter : mPlayerFilters) {
			mGui.setItem(9 + i, buildFilterSelectionIcon(filter)).onClick((clickEvent) -> clickOnFilterSelectionAction(clickEvent, filter));
			i++;
		}
		if (i < 9) {
			mGui.setItem(9 + i, buildNewComponentIcon()).onClick((clickEvent) -> clickOnNewFilterSelectionAction());
		}

	}

	private GuiItem buildNewComponentIcon() {
		ArrayList<Component> lore = new ArrayList<>();
		lore.add(Component.text("click to add a new component.", NamedTextColor.GRAY));
		ItemStack icon = GUIUtils.createBasicItem(Material.EMERALD, 1, Component.text("Add a new Component", NamedTextColor.GREEN).decoration(TextDecoration.BOLD, true), lore, false);
		return new GuiItem(icon, false);
	}

	private void clickOnComponentSelectionAction(InventoryClickEvent clickEvent, FilterComponent component) {
		if (clickEvent.getClick().equals(ClickType.SWAP_OFFHAND)) {
			if (mSelectedFilter != null && mSelectedFilter.getComponents() != null) {
				mSelectedFilter.getComponents().remove(component);
			}
		} else {
			mSelectedComponent = component;
			mComponentCustomKeysList = null;
			mComponentKnownKeys = null;
			mComponentKnownKeysList = null;
		}
		mGui.update();
	}

	private GuiItem buildComponentSelectionIcon(FilterComponent component) {

		ArrayList<Component> lore = new ArrayList<>();

		if (component.mComparator == Comparator.WHITELIST) {
			lore.add(Component.text("Whitelist:", NamedTextColor.WHITE).decoration(TextDecoration.BOLD, true));
		} else {
			lore.add(Component.text("Blacklist:", NamedTextColor.BLACK).decoration(TextDecoration.BOLD, true));
		}
		int i = 0;
		if (component.mValuesList != null) {
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
		mGui.switchToTab(mGui.TAB_BAZAAR_BROWSER);
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
		mGui.setTitle(TAB_TITLE_FILTER);
		mGui.setSize(TAB_SIZE);
		mPlayer = mGui.mPlayer;
		mPlayerFilters = new ArrayList<>(MarketManager.getPlayerMarketFilters(mPlayer));
		mSelectedFilter = null;
		mSelectedComponent = null;
		mIsChoosingNewComponent = false;
	}

	@Override
	public void onLeave() {

	}
}
