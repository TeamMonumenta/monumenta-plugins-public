package com.playmonumenta.plugins.custominventories;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

import com.goncalomb.bukkit.mylib.utils.CustomInventory;

public class ExampleCustomInventory extends CustomInventory {
	private static final Material FILLER = Material.GRAY_STAINED_GLASS_PANE;

	//Beyond creating this file, you also need a command, set up within
	//CustomInventoriesCommands.java in this folder.

	public ExampleCustomInventory(Player player) {
		//super creates the GUI with arguments of player to open for, slots in GUI,
		//and the name of the container (top line in the chest)
		super(player, 27, "Example GUI");

		//Main setup thread, create the first page of the GUI that loads in here.
		for (int i = 0; i < 27; i++) {
			_inventory.setItem(i, new ItemStack(FILLER, 1));
		}

		ItemStack exampleItem = new ItemStack(Material.BAKED_POTATO, 1);
		_inventory.setItem(13, exampleItem);
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		//Always cancel at the start if you want to avoid item removal
		event.setCancelled(true);
		//Check to make sure they clicked the GUI, didn't shift click, and
		//did not click the filler item
		if (event.getClickedInventory() != _inventory
			|| event.getCurrentItem() == null
			|| event.getCurrentItem().getType() == FILLER
			|| event.isShiftClick()) {
			return;
		}

		if (event.getSlot() == 13) {
			event.getWhoClicked().closeInventory();
		}
	}
}
