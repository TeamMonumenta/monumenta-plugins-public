package com.playmonumenta.plugins.depths;

import com.playmonumenta.scriptedquests.utils.CustomInventory;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class DepthsAbilitiesGUI extends CustomInventory {
	private static final Material FILLER = Material.GRAY_STAINED_GLASS_PANE;

	public DepthsAbilitiesGUI(Player player) {
		super(player, 27, "Select an Ability");


		List<DepthsAbilityItem> items = DepthsManager.getInstance().getAbilityUnlocks(player);
		if (items == null || items.size() < 1) {
			return;
		}

		for (int i = 0; i < 27; i++) {
			mInventory.setItem(i, new ItemStack(FILLER, 1));
		}

		mInventory.setItem(10, items.get(0).mItem);

		if (items.size() > 1) {
			mInventory.setItem(13, items.get(1).mItem);
		}

		if (items.size() > 2) {
			mInventory.setItem(16, items.get(2).mItem);
		}
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		event.setCancelled(true);
		if (event.getClickedInventory() != mInventory ||
			    event.getCurrentItem() == null ||
			    event.getCurrentItem().getType() == FILLER ||
			    event.isShiftClick()) {
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

		DepthsManager.getInstance().playerChoseItem((Player) event.getWhoClicked(), slot);
		event.getWhoClicked().closeInventory();
	}
}
