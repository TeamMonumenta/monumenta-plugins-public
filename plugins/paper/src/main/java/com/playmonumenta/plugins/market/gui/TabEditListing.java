package com.playmonumenta.plugins.market.gui;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.guis.GuiItem;
import com.playmonumenta.plugins.market.MarketListing;
import com.playmonumenta.plugins.market.MarketManager;
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

	static final Component TAB_TITLE = Component.text("Edit Listing");
	static final int TAB_SIZE = 6 * 9;

	MarketGui mGui;

	MarketListing mOldListing;

	boolean mMarkedForDeletion;
	boolean mLocked;

	public TabEditListing(MarketGui marketGUI) {
		this.mGui = marketGUI;
		this.mOldListing = new MarketListing(0L);
	}

	@Override
	public void setup() {
		mGui.setItem(0, 0, MarketGuiIcons.BACK_TO_MAIN_MENU).onClick((clickEvent) -> mGui.switchToTab(mGui.TAB_MAIN_MENU));

		mGui.setItem(0, 4, mOldListing.getItemToSell());

		mGui.setItem(0, 8, buildEditListingConfirmButton())
			.onClick((clickEvent) -> actionConfirmEdits());

		if (!mOldListing.isExpired()) {
			mGui.setItem(2, 1, buildEditListingToggleVisibility())
				.onClick((clickEvent) -> actionToggleLocked());
		}

		mGui.setItem(4, 1, buildToggleMarkedForDeletion())
			.onClick((clickEvent) -> actionToggleMarkedForDeletion());
	}

	private GuiItem buildEditListingToggleVisibility() {
		ItemStack icon;

		List<Component> lore = new ArrayList<>(List.of(
			Component.text("Click to toggle the listing's visibility.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
		));

		if (mLocked) {
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

			if (mOldListing.isLocked() != mLocked) {
				lore.add(
					mOldListing.getVisibilityAsDisplayableComponent()
						.append(MarketGuiIcons.GRAY_ARROW)
						.append(MarketListing.getVisibilityAsDisplayableComponent(mLocked))
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
					.append(Component.text(mOldListing.getAmountToSellRemaining() * mOldListing.getBundleSize(), NamedTextColor.WHITE))
					.append(MarketGuiIcons.GRAY_MULTIPLY)
					.append(ItemUtils.getDisplayName(mOldListing.getItemToSell()))
					.decoration(TextDecoration.ITALIC, false));
			}
			if (mOldListing.getAmountToClaim() > 0) {
				lore.add(MarketGuiIcons.GRAY_DASH
					.append(Component.text(mOldListing.getAmountToClaim() * mOldListing.getAmountToBuy(), NamedTextColor.WHITE))
					.append(MarketGuiIcons.GRAY_MULTIPLY)
					.append(Component.text(ItemUtils.getPlainName(mOldListing.getCurrencyToBuy()), NamedTextColor.WHITE))
					.decoration(TextDecoration.ITALIC, false));
			}
		} else {
			lore.add(Component.text("There are no items left to claim", NamedTextColor.GRAY));
		}
		lore.add(Component.text("This will then delete the listing", NamedTextColor.GRAY));
		return lore;
	}

	private void actionConfirmEdits() {
		if (MarketGui.initiatePlayerAction(mGui.mPlayer)) {
			return;
		}
		Bukkit.getScheduler().runTaskAsynchronously(Plugin.getInstance(), () -> {
			boolean editOk = MarketManager.getInstance().editListing(mGui.mPlayer, mOldListing, mMarkedForDeletion,
				listing -> listing.setLocked(mLocked));
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

	private void actionToggleLocked() {
		mLocked = !mLocked;
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
		mLocked = mGui.mFocusedListing.isLocked();

		mMarkedForDeletion = false;
	}

	@Override
	public void onLeave() {

	}
}
