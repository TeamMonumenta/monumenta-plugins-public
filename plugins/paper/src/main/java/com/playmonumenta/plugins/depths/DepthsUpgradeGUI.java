package com.playmonumenta.plugins.depths;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.goncalomb.bukkit.mylib.utils.CustomInventory;

public class DepthsUpgradeGUI extends CustomInventory {
	private static final Material FILLER = Material.GRAY_STAINED_GLASS_PANE;

	public DepthsUpgradeGUI(Player player) {
		super(player, 27, "Select an Upgrade");

		List<DepthsAbilityItem> items = DepthsManager.getInstance().getAbilityUpgradeOptions(player);

		for (int i = 0; i < 27; i++) {
			_inventory.setItem(i, new ItemStack(FILLER, 1));
		}
		if (items != null) {
			if (items.size() >= 1) {
				_inventory.setItem(10, items.get(0).mItem);
			}
			if (items.size() >= 2) {
				_inventory.setItem(13, items.get(1).mItem);
			}
			if (items.size() >= 3) {
				_inventory.setItem(16, items.get(2).mItem);
			}
		}
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);
		if (event.getClickedInventory() != _inventory
			|| event.getCurrentItem() == null
			|| event.getCurrentItem().getType() == FILLER
			|| event.isShiftClick()) {
			return;
		}
		int slot;
		if (event.getSlot() == 10) {
			slot = 0;
		} else if (event.getSlot() == 13) {
			slot = 1;
		} else if (event.getSlot() == 16) {
			slot = 2;
		} else {
			return;
		}

		DepthsManager.getInstance().playerUpgradedItem((Player) event.getWhoClicked(), slot);
		event.getWhoClicked().closeInventory();
	}
}
