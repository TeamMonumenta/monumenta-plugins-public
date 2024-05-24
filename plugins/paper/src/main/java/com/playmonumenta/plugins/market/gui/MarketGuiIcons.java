package com.playmonumenta.plugins.market.gui;

import com.playmonumenta.plugins.utils.GUIUtils;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class MarketGuiIcons {

	static final ItemStack BACK_TO_MAIN_MENU = GUIUtils.createBasicItem(Material.BARRIER, GUIUtils.formatName("Return to Main Menu", NamedTextColor.GOLD, true), "gui_redReturnArrow");
	static final ItemStack LOADING = GUIUtils.createBasicItem(Material.SUNFLOWER, GUIUtils.formatName("Loading...", NamedTextColor.GOLD, true), "gui_loading");
	static final ItemStack ADD_LISTING = GUIUtils.createBasicItem(Material.WRITABLE_BOOK, 1, GUIUtils.formatName("Add Listing", NamedTextColor.GOLD, true), List.of(Component.text("Click here to sell your item(s)", NamedTextColor.GRAY), Component.text("on the Player Market.", NamedTextColor.GRAY)), true, "gui_greenPlus");
	static final ItemStack REFRESH = GUIUtils.createBasicItem(Material.ENDER_EYE, GUIUtils.formatName("Refresh", NamedTextColor.GOLD, true), "gui_refresh");
	static final ItemStack PLAYER_LISTINGS = GUIUtils.createBasicItem(Material.WRITTEN_BOOK, "Active Listings", NamedTextColor.GOLD, true, "Click to view your active Market Listings", NamedTextColor.GRAY);
	static final ItemStack BAZAAR_LISTING_BROWSER = GUIUtils.createBasicItem(Material.BOOKSHELF, "Browse Market", NamedTextColor.GOLD, true, "Click here to browse the Player Market", NamedTextColor.GRAY);
	static final ItemStack MISSING_ITEM_SELECTION = GUIUtils.createBasicItem(Material.STRUCTURE_VOID, "Nothing selected", NamedTextColor.RED, true, "Click on an item in your inventory to select the item to be sold!", NamedTextColor.GRAY);
	static final ItemStack ADDLISTING_MULTIPLIER_MISSING_ITEM = GUIUtils.createBasicItem(Material.DARK_OAK_SIGN, "Put an item first!", NamedTextColor.GOLD, true);

	public static ItemStack buildChooseCurrencyIcon(ItemStack source) {
		ItemStack icon = source.asQuantity(1);
		icon.lore(List.of(
			Component.text("Left click", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
				.append(Component.text(" to select this currency", NamedTextColor.WHITE))
		));
		return icon;
	}

	public static final Component GRAY_ARROW = Component.text(" ➜ ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
	public static final Component GRAY_DASH = Component.text(" - ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
	public static final Component GRAY_MULTIPLY = Component.text(" * ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
	public static final Component GREEN_CHECKBOX = Component.text("✓", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false);
	public static final Component RED_XMARK = Component.text("✗", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false);

	public static Component getCheckboxOrXmark(boolean checkbox) {
		return checkbox ? GREEN_CHECKBOX : RED_XMARK;
	}

}
