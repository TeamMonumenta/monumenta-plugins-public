package com.playmonumenta.plugins.market.gui;

import com.playmonumenta.plugins.guis.GuiItem;
import com.playmonumenta.plugins.utils.GUIUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class TabMainMenu implements MarketGuiTab {

	MarketGui mGui;

	static final Component TAB_TITLE = Component.text("Player Market");
	static final int TAB_SIZE = 6 * 9;

	static final ItemStack MODERATOR_MENU_ICON = GUIUtils.createBasicItem(Material.REDSTONE_BLOCK, "Moderation global actions", NamedTextColor.GOLD, true);


	public TabMainMenu(MarketGui marketGUI) {
		this.mGui = marketGUI;
	}

	@Override
	public void setup() {
		mGui.setItem(2, 2, buildBazaarBrowserIcon()).onClick((clickEvent) -> mGui.switchToTab(mGui.TAB_BAZAAR_BROWSER));
		mGui.setItem(1, 4, buildAuctionBrowserIcon()).onClick((clickEvent) -> mGui.switchToTab(mGui.TAB_NOT_IMPLEMENTED));
		mGui.setItem(2, 6, buildStockBrowserIcon()).onClick((clickEvent) -> mGui.switchToTab(mGui.TAB_NOT_IMPLEMENTED));

		mGui.setItem(3, 4, buildPlayerListingsIcon()).onClick(this::clickPlayerListingsAction);

		mGui.setItem(5, 0, buildOptionsIcon()).onClick((clickEvent) -> mGui.switchToTab(mGui.TAB_EDIT_OPTIONS));

		if (mGui.mIsOp) {
			mGui.setItem(5, 8, MODERATOR_MENU_ICON).onClick((clickEvent) -> mGui.switchToTab(mGui.TAB_MODERATOR_MENU));
		}
	}

	private GuiItem buildOptionsIcon() {
		List<Component> lore = new ArrayList<>();

		Component name = Component.text("Options", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true);

		lore.add(Component.text("Left Click to edit your", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
		lore.add(Component.text("options or preferences", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));

		return new GuiItem(GUIUtils.createBasicItem(Material.OBSERVER, 1, name, lore, false), false);
	}

	private void clickPlayerListingsAction(InventoryClickEvent clickEvent) {
		if (clickEvent.isLeftClick()) {
			mGui.switchToTab(mGui.TAB_PLAYER_LISTINGS);
		} else if (clickEvent.isRightClick()) {
			mGui.switchToTab(mGui.TAB_ADD_LISTING);
		}
	}

	private GuiItem buildPlayerListingsIcon() {
		List<Component> lore = new ArrayList<>();

		Component name = Component.text("Your Listings", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true);

		lore.add(Component.text("Left Click to view your Market Listings", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
		lore.add(Component.empty());
		lore.add(Component.text("You can also Right Click", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
		lore.add(Component.text("to directly add a listing", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));

		return new GuiItem(GUIUtils.createBasicItem(Material.BOOKSHELF, 1, name, lore, false), false);
	}

	private GuiItem buildBazaarBrowserIcon() {
		List<Component> lore = new ArrayList<>();

		Component name = Component.text("Bazaar Market", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true);

		lore.add(Component.text("Click here to browse the Bazaar Market", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
		lore.add(Component.empty());
		lore.add(Component.text("Meant for general goods that are", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
		lore.add(Component.text("unfit for the other ways of trading", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));

		return new GuiItem(GUIUtils.createBasicItem(Material.BARREL, 1, name, lore, false), false);
	}

	private GuiItem buildAuctionBrowserIcon() {
		List<Component> lore = new ArrayList<>();

		Component name = Component.text("Auction Market", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.OBFUSCATED, true);

		lore.add(Component.text("Not yet implemented.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));

		return new GuiItem(GUIUtils.createBasicItem(Material.JIGSAW, 1, name, lore, false), false);
	}

	private GuiItem buildStockBrowserIcon() {
		List<Component> lore = new ArrayList<>();

		Component name = Component.text("Stock Market", NamedTextColor.GOLD).decoration(TextDecoration.BOLD, true).decoration(TextDecoration.OBFUSCATED, true);

		lore.add(Component.text("Not yet implemented.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));

		return new GuiItem(GUIUtils.createBasicItem(Material.JIGSAW, 1, name, lore, false), false);
	}

	@Override
	public void onSwitch() {
		mGui.setTitle(TAB_TITLE);
		mGui.setSize(TAB_SIZE);
	}

	@Override
	public void onLeave() {

	}
}
