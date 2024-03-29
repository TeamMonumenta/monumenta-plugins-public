package com.playmonumenta.plugins.market.gui;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.guis.GuiItem;
import com.playmonumenta.plugins.market.MarketListing;
import com.playmonumenta.plugins.market.MarketManager;
import com.playmonumenta.plugins.market.MarketRedisManager;
import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public class TabEditListing implements MarketGuiTab {

	MarketGui mGui;

	static final Component TAB_TITLE = Component.text("Edit Listing");
	static final int TAB_SIZE = 6 * 9;

	public TabEditListing(MarketGui marketGUI) {
		this.mGui = marketGUI;
		this.mOldListing = new MarketListing(0L);
		this.mNewListing = new MarketListing(0L);
	}

	MarketListing mOldListing;
	MarketListing mNewListing;

	boolean mMarkedForDeletion;

	@Override
	public void setup() {
		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			mOldListing = MarketRedisManager.getListing(mOldListing.getId());
		});

		mGui.setItem(0, 0, MarketGuiIcons.BACK_TO_MAIN_MENU).onClick((clickEvent) -> mGui.switchToTab(mGui.TAB_MAIN_MENU));

		mGui.setItem(0, 4, mOldListing.getItemToSell());

		mGui.setItem(0, 8, buildEditListingConfirmButton())
			.onClick((clickEvent) -> actionConfirmEdits());

		if (!mOldListing.isExpired()) {
			mGui.setItem(2, 1, buildEditListingToggleVisibility())
				.onClick((clickEvent) -> actionSetLocked());
		}

		mGui.setItem(4, 1, buildToggleMarkedForDeletion())
			.onClick((clickEvent) -> actionToggleMarkedForDeletion());
	}

	private GuiItem buildEditListingToggleVisibility() {
		ItemStack icon;

		List<Component> lore = new ArrayList<>(List.of(
			Component.text("Click to toggle the listing's visibility.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
		));

		if (mNewListing.isLocked()) {
			lore.addAll(
				List.of(
					Component.text("This listing is currently ", NamedTextColor.GRAY).append(Component.text("Invisible", NamedTextColor.RED)).decoration(TextDecoration.ITALIC, false),
					Component.text("And will not show up in the browsers.", NamedTextColor.GRAY)
				)
			);
			icon = GUIUtils.createBasicItem(Material.GLASS, 1, GUIUtils.formatName("Invisible", NamedTextColor.RED, true),
				lore, true, "gui_invisible");
		} else {
			lore.addAll(
				List.of(
					Component.text("This listing is currently ", NamedTextColor.GRAY).append(Component.text("Visible", NamedTextColor.GREEN)).decoration(TextDecoration.ITALIC, false),
					Component.text("And will show up in the browsers.", NamedTextColor.GRAY)
				)
			);
			icon = GUIUtils.createBasicItem(Material.BEACON, 1, GUIUtils.formatName("Visible", NamedTextColor.GREEN, true),
				lore, true, "gui_visible");
		}

		return new GuiItem(icon, false);
	}

	private GuiItem buildEditListingConfirmButton() {
		List<Component> lore = new ArrayList<>();

		if (mMarkedForDeletion) {
			lore.addAll(calculateMarkedForDeletionLoreLines());
		} else {

			if (mOldListing.isLocked() != mNewListing.isLocked()) {
				lore.add(
					mOldListing.getVisibilityAsDisplayableComponent()
						.append(MarketGuiIcons.GRAY_ARROW)
						.append(mNewListing.getVisibilityAsDisplayableComponent())
				);
			}

		}

		return new GuiItem(GUIUtils.createBasicItem(Material.GREEN_STAINED_GLASS_PANE, 1, GUIUtils.formatName("Confirm the edits", NamedTextColor.GREEN, true),
			lore, true, "gui_checkmark"), false);
	}

	private void actionToggleMarkedForDeletion() {
		mMarkedForDeletion = !mMarkedForDeletion;
		mGui.update();
	}

	private GuiItem buildToggleMarkedForDeletion() {
		ItemStack icon;

		List<Component> lore = new ArrayList<>(List.of(
			Component.text("Click to toggle the listing's deletion.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
			Component.empty()
		));

		if (mMarkedForDeletion) {
			lore.addAll(calculateMarkedForDeletionLoreLines());
			icon = GUIUtils.createBasicItem(Material.LAVA_BUCKET, 1, GUIUtils.formatName("Delete Listing", NamedTextColor.RED, true),
				lore, true, "gui_deleteListing");
		} else {
			lore.add(Component.text("This listing is currently ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
			lore.add(Component.text("Not marked for deletion", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));

			icon = GUIUtils.createBasicItem(Material.BUCKET, 1, GUIUtils.formatName("Keep Listing", NamedTextColor.GREEN, true),
				lore, true, "gui_keepListing");
		}

		return new GuiItem(icon, false);
	}

	private List<Component> calculateMarkedForDeletionLoreLines() {
		List<Component> lore = new ArrayList<>();
		lore.add(Component.text("This listing is currently ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
		lore.add(Component.text("Marked for deletion", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
		if (mOldListing.getAmountToSellRemaining() > 0 || mOldListing.getAmountToClaim() > 0) {
			lore.add(Component.text("Upon confirming, you will be given: ", NamedTextColor.GRAY)
				.decoration(TextDecoration.ITALIC, false));
			if (mOldListing.getAmountToSellRemaining() > 0) {
				lore.add(MarketGuiIcons.GRAY_DASH
					.append(Component.text(mOldListing.getAmountToSellRemaining(), NamedTextColor.WHITE))
					.append(MarketGuiIcons.GRAY_MULTIPLY)
					.append(Component.text(ItemUtils.getPlainName(mOldListing.getItemToSell()), NamedTextColor.WHITE))
					.decoration(TextDecoration.ITALIC, false));
			}
			if (mOldListing.getAmountToClaim() > 0) {
				lore.add(MarketGuiIcons.GRAY_DASH
					.append(Component.text(mOldListing.getAmountToClaim(), NamedTextColor.WHITE))
					.append(MarketGuiIcons.GRAY_MULTIPLY)
					.append(Component.text(ItemUtils.getPlainName(mOldListing.getItemToBuy()), NamedTextColor.WHITE))
					.decoration(TextDecoration.ITALIC, false));
			}
		} else {
			lore.add(Component.text("There is no items left to claim", NamedTextColor.GRAY));
		}
		lore.add(Component.text("This will then delete the listing", NamedTextColor.GRAY));
		return lore;
	}

	private void actionConfirmEdits() {
		if (MarketGui.initiatePlayerAction(mGui.mPlayer)) {
			return;
		}
		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			boolean editOk = MarketManager.getInstance().editListing(mGui.mPlayer, mMarkedForDeletion, mOldListing, mNewListing);
			if (editOk) {
				Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
					MarketGui.endPlayerAction(mGui.mPlayer);
					mGui.switchToTab(mGui.TAB_PLAYER_LISTINGS);
				}, 0L);
			} else {
				MarketGui.endPlayerAction(mGui.mPlayer);
				mGui.update();
			}
		});
	}

	private void actionSetLocked() {
		mNewListing.setLocked(!mNewListing.isLocked());
		mGui.update();
	}

	@Override
	public void onSwitch() {
		mGui.setTitle(TAB_TITLE);
		mGui.setSize(TAB_SIZE);
		if (mGui.mFocusedListing == null) {
			mGui.switchToTab(mGui.TAB_MAIN_MENU);
			return;
		}
		mOldListing = new MarketListing(mGui.mFocusedListing);
		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			mOldListing = MarketRedisManager.getListing(mOldListing.getId());
		});
		mNewListing = new MarketListing(mGui.mFocusedListing);

		mMarkedForDeletion = false;
	}

	@Override
	public void onLeave() {

	}
}
