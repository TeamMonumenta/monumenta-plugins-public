package com.playmonumenta.plugins.itemindex;

import com.playmonumenta.plugins.Plugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;

public class IndexInventory {
	private final Player mPlayer;
	private final Inventory mInventory;
	private ItemStack[] mContents;
	private MonumentaItem[] mItems;
	private ArrayList<ItemIndexFilter> mFilters;
	private int mCurrentPage;

	IndexInventory(Player player) {
		this.mPlayer = player;
		this.mInventory = Bukkit.createInventory(this.mPlayer, 54, "Monumenta Item Index");
		this.mFilters = new ArrayList<>();
	}

	void openTable() {
		this.renderMainView();
		this.mPlayer.openInventory(this.mInventory);
	}

	private void addBottomHUD() {
		this.mContents[50] = this.makeFilterHUDItem();
		this.mContents[52] = this.makePrevPageHUDItem();
		this.mContents[53] = this.makeNextPageHUDItem();
	}

	private ItemStack makePrevPageHUDItem() {
		ItemStack out = new ItemStack(Material.ARROW);
		ItemMeta meta = out.getItemMeta();
		meta.setDisplayName(String.format("%s%sPrev Page", ChatColor.GOLD, ChatColor.BOLD));
		meta.setLore(this.makePageChangersLore());
		out.setItemMeta(meta);
		return out;
	}

	private ItemStack makeNextPageHUDItem() {
		ItemStack out = new ItemStack(Material.ARROW);
		ItemMeta meta = out.getItemMeta();
		meta.setDisplayName(String.format("%s%sNext Page", ChatColor.GOLD, ChatColor.BOLD));
		meta.setLore(this.makePageChangersLore());
		out.setItemMeta(meta);
		return out;
	}

	private ArrayList<String> makePageChangersLore() {
		ArrayList<String> lore = new ArrayList<>();
		lore.add(String.format("%sCurrent Page: %s%d%s / %d", ChatColor.GRAY, ChatColor.GOLD, this.mCurrentPage + 1,
			ChatColor.GRAY, (this.mItems.length - 1) / 45 + 1));
		lore.add(String.format("%sCurrently showing items [ %d - %d ] / %d", ChatColor.GRAY, this.mCurrentPage * 45 + 1,
			Math.min((this.mCurrentPage + 1) * 45, this.mItems.length), this.mItems.length));
		return lore;
	}

	private ItemStack makeFilterHUDItem() {
		ItemStack out = new ItemStack(Material.COBWEB);
		ItemMeta meta = out.getItemMeta();
		meta.setDisplayName(ChatColor.AQUA + "" + ChatColor.BOLD + "Loaded Filters");
		ArrayList<String> filtersList = this.getFiltersAsList();
		filtersList.add(ChatColor.GRAY + "Click this item to remove all filters");
		meta.setLore(filtersList);
		out.setItemMeta(meta);
		return out;
	}

	private ArrayList<String> getFiltersAsList() {
		ArrayList<String> out = new ArrayList<>();
		return out;
	}

	private void buildMainViewContents() {
		this.mContents = new ItemStack[54];
		if (this.mCurrentPage > (this.mItems.length - 1) / 45 + 1) {
			this.mCurrentPage = 0;
		} else if (this.mCurrentPage == -1) {
			this.mCurrentPage = (this.mItems.length - 1) / 45 + 1;
		}
		for (int i = 0; i < 45 && i + this.mCurrentPage * 45 < this.mItems.length; i++) {
			MonumentaItem item = this.mItems[i + this.mCurrentPage * 45];
			item.setEdits(null);
			this.mContents[i] = item.toItemStack();
		}
		this.addBottomHUD();
		this.mInventory.setContents(this.mContents);
	}

	private void renderMainView() {
		this.mItems = Plugin.getInstance().mItemManager.getItemArray();
		this.readData();
	}

	private void readData() {
		this.applyFilters();
		this.showData();
	}

	private void showData() {
		this.buildMainViewContents();
		this.mPlayer.updateInventory();
	}

	private void applyFilters() {
		ArrayList<MonumentaItem> out = new ArrayList<>();
		if (this.mFilters.size() == 0) {
			return;
		}
		for (MonumentaItem item : this.mItems) {
			for (ItemIndexFilter filter : this.mFilters) {
				if (filter.match(item)) {
					out.add(item);
				}
			}
		}
		this.mItems = out.toArray(new MonumentaItem[0]);
	}

	public Inventory getInventory() {
		return this.mInventory;
	}

	public void pageUp() {
		this.mCurrentPage += 1;
		this.showData();
	}

	public void pageDown() {
		this.mCurrentPage -= 1;
		this.showData();
	}

	public void resetFilters() {
		this.mFilters = new ArrayList<>();
		this.readData();
	}

	public void pickItem(int slot) {
		this.mPlayer.getInventory().addItem(this.mInventory.getItem(slot));
	}
}
