package com.playmonumenta.plugins.cosmetics;

import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class CosmeticsGUI extends CustomInventory {

	private static final int PREV_PAGE_LOC = 45;
	private static final int NEXT_PAGE_LOC = 53;
	private static final int BACK_LOC = 49;
	private static final int TITLE_LOC = 12;
	private static final int ELITE_FINISHER_LOC = 14;
	private static final int SUMMARY_LOC = 0;
	private static final int COSMETICS_START = 9;
	private static final int COSMETICS_PER_PAGE = 36;
	private static final Material FILLER = Material.GRAY_STAINED_GLASS_PANE;
	private CosmeticType mDisplayPage = null;
	private int mPageNumber = 1;

	public CosmeticsGUI(Player player) {
		this(player, player);
	}

	public CosmeticsGUI(Player requestingPlayer, Player targetPlayer) {
		super(requestingPlayer, 54, "Cosmetics Manager");
		setUpCosmetics(targetPlayer);
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);
		if (event.getClickedInventory() == mInventory) {
			//Attempt to switch page if clicked page
			ItemStack item = event.getCurrentItem();
			Player p = (Player) event.getWhoClicked();
			if (item == null || item.getType() == Material.AIR) {
				return;
			}
			// Main page filtering
			if (mDisplayPage == null && event.getSlot() == TITLE_LOC) {
				mDisplayPage = CosmeticType.TITLE;
				p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1f);
				setUpCosmetics(p);
				return;
			} else if (mDisplayPage == null && event.getSlot() == ELITE_FINISHER_LOC) {
				mDisplayPage = CosmeticType.ELITE_FINISHER;
				p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1f);
				setUpCosmetics(p);
				return;
			} else if (mDisplayPage != null && (item.getType() == mDisplayPage.getDisplayItem() || item.getType() == Material.GREEN_CONCRETE)) {
				//Get the list of cosmetics back
				// Note this only works for cosmetic types where only one cosmetic can be equipped at a time!
				List<Cosmetic> playerCosmetics = CosmeticsManager.getInstance().getCosmeticsOfTypeAlphabetical(p, mDisplayPage);
				int slot = event.getSlot();
				int index = (slot - COSMETICS_START) + (COSMETICS_PER_PAGE * (mPageNumber - 1));
				if (playerCosmetics != null) {
					for (int i = 0; i < playerCosmetics.size(); i++) {
						if (i == index) {
							if (!playerCosmetics.get(i).mEquipped) {
								p.playSound(p.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.8f, 1f);
								playerCosmetics.get(i).mEquipped = true;
							} else {
								p.playSound(p.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.8f, 1f);
								playerCosmetics.get(i).mEquipped = false;
							}
						} else {
							playerCosmetics.get(i).mEquipped = false;
						}
					}
					setUpCosmetics(p);
					return;
				}
			}

			//Page control items
			if (event.getSlot() == NEXT_PAGE_LOC && item.getType() == Material.ARROW) {
				p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1f);
				mPageNumber++;
				setUpCosmetics(p);
				return;
			}

			if (event.getSlot() == PREV_PAGE_LOC && item.getType() == Material.ARROW) {
				p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1f);
				mPageNumber--;
				setUpCosmetics(p);
				return;
			}

			if (event.getSlot() == BACK_LOC && item.getType() == Material.REDSTONE_BLOCK) {
				p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.5f, 1f);
				mDisplayPage = null;
				setUpCosmetics(p);
				return;
			}
		}
	}

	/**
	 * Sets up the cosmetic viewing and selecting page
	 * each page is specialized for the specific cosmetic type
	 * currently in the mDisplayPage variable
	 */
	public Boolean setUpCosmetics(Player targetPlayer) {

		//Set inventory to filler to start
		for (int i = 0; i < 54; i++) {
			mInventory.setItem(i, new ItemStack(FILLER, 1));
		}
		//Figure out which type of cosmetic to display
		if (mDisplayPage == null) {
			return setUpHomePage();
		}
		//Get list of cosmetics
		List<Cosmetic> playerCosmetics = CosmeticsManager.getInstance().getCosmeticsOfTypeAlphabetical(targetPlayer, mDisplayPage);
		if (playerCosmetics == null) {
			mDisplayPage = null;
			setUpCosmetics(targetPlayer);
			return true;
		}
		int numPages = (playerCosmetics.size() / COSMETICS_PER_PAGE) + 1;

		//Set up summary
		ItemStack passSummary = new ItemStack(Material.BOOK, 1);
		ItemMeta meta = passSummary.getItemMeta();
		meta.displayName(Component.text("Displaying Unlocked " + mDisplayPage.getDisplayName() + "s", TextColor.color(DepthsUtils.FROSTBORN)).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

		List<Component> lore = new ArrayList<>();
		if (CosmeticsManager.getInstance().getActiveCosmetic(targetPlayer, mDisplayPage) != null) {
			Cosmetic c = CosmeticsManager.getInstance().getActiveCosmetic(targetPlayer, mDisplayPage);
			lore.add(Component.text("Equipped Cosmetic: " + c.getName(), NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
		} else {
			lore.add(Component.text("No Equipped Cosmetic!", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
		}
		lore.add(Component.text("Click an unlocked cosmetic to equip!", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
		meta.lore(lore);
		passSummary.setItemMeta(meta);
		mInventory.setItem(SUMMARY_LOC, passSummary);

		// Individual cosmetic display
		for (int i = (mPageNumber - 1) * COSMETICS_PER_PAGE; (i < mPageNumber * COSMETICS_PER_PAGE && i < playerCosmetics.size()); i++) {
			try {
				Cosmetic c = playerCosmetics.get(i);
				if (c == null) {
					continue;
				}

				ItemStack cosmeticItem = new ItemStack(mDisplayPage.getDisplayItem(), 1);

				if (c.isEquipped()) {
					cosmeticItem = new ItemStack(Material.GREEN_CONCRETE, 1);
				}

				meta = cosmeticItem.getItemMeta();
				meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);

				meta.displayName(Component.text(c.getName(), TextColor.color(DepthsUtils.WINDWALKER)).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
				lore = new ArrayList<>();
				lore.add(Component.text("Custom " + mDisplayPage.getDisplayName(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
				if (c.isEquipped()) {
					lore.add(Component.text("Currently Equipped", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
				} else {
					lore.add(Component.text("Unequipped", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
				}

				meta.lore(lore);
				cosmeticItem.setItemMeta(meta);
				mInventory.setItem(COSMETICS_START + (i % COSMETICS_PER_PAGE), cosmeticItem);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		//Prev and next page buttons
		if (mPageNumber > 1) {
			// Display prev page
			ItemStack pageItem = new ItemStack(Material.ARROW, 1);
			meta = pageItem.getItemMeta();
			meta.displayName(Component.text("Previous Page", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
			pageItem.setItemMeta(meta);
			mInventory.setItem(PREV_PAGE_LOC, pageItem);
		}

		if (mPageNumber < numPages) {
			// Display next page
			ItemStack pageItem = new ItemStack(Material.ARROW, 1);
			meta = pageItem.getItemMeta();
			meta.displayName(Component.text("Next Page", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
			pageItem.setItemMeta(meta);
			mInventory.setItem(NEXT_PAGE_LOC, pageItem);
		}

		// Display go back button
		ItemStack pageItem = new ItemStack(Material.REDSTONE_BLOCK, 1);
		meta = pageItem.getItemMeta();
		meta.displayName(Component.text("Back to Overview", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
		pageItem.setItemMeta(meta);
		mInventory.setItem(BACK_LOC, pageItem);

		return true;
	}

	/**
	 * Sets up the display page for the home screen
	 * with items to click to browse certain cosmetic types
	 */
	private Boolean setUpHomePage() {

		ItemStack titleItem = new ItemStack(CosmeticType.TITLE.getDisplayItem(), 1);
		ItemMeta meta = titleItem.getItemMeta();
		meta.displayName(Component.text("View Unlocked Titles", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
		titleItem.setItemMeta(meta);
		mInventory.setItem(TITLE_LOC, titleItem);

		ItemStack eliteItem = new ItemStack(CosmeticType.ELITE_FINISHER.getDisplayItem(), 1);
		meta = eliteItem.getItemMeta();
		meta.displayName(Component.text("View Unlocked Elite Finishers", NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		eliteItem.setItemMeta(meta);
		mInventory.setItem(ELITE_FINISHER_LOC, eliteItem);

		return true;
	}
}
