package com.playmonumenta.plugins.market.gui;

import com.playmonumenta.plugins.market.MarketManager;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.plugins.utils.WalletUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

public class MarketGuiIcons {

	static final ItemStack BACK_TO_MAIN_MENU = GUIUtils.createBasicItem(Material.BARRIER, GUIUtils.formatName("Return to Main Menu", NamedTextColor.GOLD, true), "gui_redReturnArrow");
	static final ItemStack LOADING = GUIUtils.createBasicItem(Material.SUNFLOWER, GUIUtils.formatName("Loading...", NamedTextColor.GOLD, true), "gui_loading");
	static final ItemStack ADD_LISTING = GUIUtils.createBasicItem(Material.WRITABLE_BOOK, 1, GUIUtils.formatName("Add Listing", NamedTextColor.GOLD, true), List.of(Component.text("Click here to sell your item(s)", NamedTextColor.GRAY), Component.text("on the Player Market.", NamedTextColor.GRAY)), true, "gui_greenPlus");
	static final ItemStack REFRESH = GUIUtils.createBasicItem(Material.ENDER_EYE, GUIUtils.formatName("Refresh", NamedTextColor.GOLD, true), "gui_refresh");
	static final ItemStack PLAYER_LISTINGS = GUIUtils.createBasicItem(Material.WRITTEN_BOOK, "Active Listings", NamedTextColor.GOLD, true, "Click to view your active Market Listings", NamedTextColor.GRAY);
	static final ItemStack BAZAAR_LISTING_BROWSER = GUIUtils.createBasicItem(Material.BOOKSHELF, "Browse Market", NamedTextColor.GOLD, true, "Click here to browse the Player Market", NamedTextColor.GRAY);
	static final ItemStack MODERATOR_MENU = GUIUtils.createBasicItem(Material.REDSTONE_BLOCK, "Moderation global actions", NamedTextColor.GOLD, true);
	static final ItemStack MISSING_ITEM_SELECTION = GUIUtils.createBasicItem(Material.STRUCTURE_VOID, "Nothing selected", NamedTextColor.RED, true, "Click on an item in your inventory to select the item to be sold!", NamedTextColor.GRAY);
	static final ItemStack ADDLISTING_MULTIPLIER_MISSING_ITEM = GUIUtils.createBasicItem(Material.DARK_OAK_SIGN, "Put an item first!", NamedTextColor.GOLD, true);
	static final ItemStack ADDLISTING_CONFIRM_ERROR_WRONG_PARAMS = GUIUtils.createBasicItem(Material.GOLD_INGOT, 1, GUIUtils.formatName("Wrong parameters !", NamedTextColor.RED, true),
		List.of(Component.text("Something is wrong in either : ", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false),
			Component.text("- the item to be sold,", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false),
			Component.text("- the amount of items to be sold,", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false),
			Component.text("- the price per item", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false),
			Component.text("Which leads to an error in the final calculation", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false),
			Component.text("Please check your numbers", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
			), true, "gui_exclamation_mark");

	public static ItemStack buildChooseCurrencyIcon(ItemStack source) {
		ItemStack icon = source.asQuantity(1);
		icon.lore(List.of(
			Component.text("Left click", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
				.append(Component.text(" to select this currency", NamedTextColor.WHITE))
		));
		return icon;
	}

	public static ItemStack buildAddListingItemToBeSoldIcon(ItemStack itemToBeSold) {
		List<Component> sellItemLore = new ArrayList<>();
		sellItemLore.add(itemToBeSold.displayName());
		List<Component> lore = itemToBeSold.lore();
		if (lore != null) {
			sellItemLore.addAll(lore);
		}

		ItemStack icon = GUIUtils.createBasicItem(itemToBeSold.getType(), 1, "Item to be sold:", NamedTextColor.GOLD, true, sellItemLore, true);
		ItemUtils.setPlainName(icon, ItemUtils.getPlainName(itemToBeSold));
		return icon;
	}

	public static ItemStack buildAddListingItemToBeSoldMultiplierIcon(@Nullable ItemStack itemToSell, int multiplier) {

		ItemStack icon = ADDLISTING_MULTIPLIER_MISSING_ITEM.asQuantity(1);

		if (ItemUtils.isNullOrAir(itemToSell)) {
			return icon;
		}

		ItemMeta meta = icon.getItemMeta();

		meta.displayName(Component.text("Amount: ", NamedTextColor.GOLD, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false).append(Component.text(multiplier, NamedTextColor.GOLD, TextDecoration.BOLD)));
		meta.lore(List.of(
			Component.text("Left click", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false).append(Component.text(" to decrease by 1", NamedTextColor.WHITE)),
			Component.text("Right click", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false).append(Component.text(" to increase by 1", NamedTextColor.WHITE)),
			Component.text("Shift click", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false).append(Component.text(" to offset by 64 instead", NamedTextColor.WHITE)),
			Component.keybind("key.swapOffhand", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false).append(Component.text(" to specify the amount", NamedTextColor.WHITE))
		));

		icon.setItemMeta(meta);

		return icon;
	}

	public static ItemStack buildChangeCurrencyIcon(@Nullable ItemStack currentSelection) {
		return GUIUtils.createBasicItem(
			Material.FLOWER_POT, 1,
			GUIUtils.formatName("Current Currency: " + StringUtils.getCurrencyShortForm(currentSelection), NamedTextColor.GOLD, true),
			List.of(Component.text("Left click", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false).append(Component.text(" to select the currency", NamedTextColor.WHITE))),
			true, "Bag of Hoarding");
	}

	public static ItemStack buildAddListingPricePerItemMultiplierIcon(@Nullable ItemStack currencyItem, int multiplier) {
		List<Component> currencyLore = List.of(
			Component.text(multiplier + " " + ItemUtils.getPlainName(currencyItem)).decoration(TextDecoration.ITALIC, false),
			Component.text("Left click", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false).append(Component.text(" to decrease by 1", NamedTextColor.WHITE)),
			Component.text("Right click", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false).append(Component.text(" to increase by 1", NamedTextColor.WHITE)),
			Component.text("Shift click", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false).append(Component.text(" to offset by 16 instead", NamedTextColor.WHITE)),
			Component.keybind("key.swapOffhand", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false).append(Component.text(" to specify the amount", NamedTextColor.WHITE)),
			Component.text("   (may be a simple calculation)", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false)
		);

		ItemStack icon = GUIUtils.createBasicItem(currencyItem != null ? currencyItem.getType() : Material.AIR, 1, "Price per item", NamedTextColor.GOLD, true, currencyLore, true);
		ItemUtils.setPlainName(icon, ItemUtils.getPlainName(currencyItem));

		return icon;
	}

	public static ItemStack buildAddListingConfirmWithTaxStatusIcon(WalletUtils.Debt taxDebt) {
		ArrayList<Component> loreList = new ArrayList<>();
		// always displayed
		loreList.add(Component.text("To create a listing, you will need to pay a tax of:", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
		loreList.add(Component.text(taxDebt.mTotalRequiredAmount + " " + ItemUtils.getPlainName(taxDebt.mItem) + " ", NamedTextColor.WHITE)
			.append(getCheckboxOrXmark(taxDebt.mMeetsRequirement))
			.append(Component.text(taxDebt.mWalletDebt > 0 ? " (" + taxDebt.mNumInWallet + " in wallet)" : "", NamedTextColor.GRAY)).decoration(TextDecoration.ITALIC, false));
		loreList.add(Component.text(String.format("Tax Rate: %.1f%%", MarketManager.getConfig().mBazaarTaxRate*100), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));

		// depends on tax status
		if (taxDebt.mMeetsRequirement) {
			return GUIUtils.createBasicItem(Material.GREEN_STAINED_GLASS_PANE, 1, GUIUtils.formatName("Confirm the trade", NamedTextColor.GREEN, true),
				loreList, true, "gui_checkmark");
		} else {
			loreList.add(Component.text("You do not have enough money to pay the tax!", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
			return GUIUtils.createBasicItem(Material.GOLD_INGOT, 1, GUIUtils.formatName("Not enough money", NamedTextColor.RED, true),
				loreList, true, "gui_exclamation_mark");
		}
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
