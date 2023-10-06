package com.playmonumenta.plugins.custominventories;

import com.playmonumenta.plugins.utils.GUIUtils;
import com.playmonumenta.scriptedquests.utils.CustomInventory;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class ExampleCustomInventory extends CustomInventory {
	//Beyond creating this file, you also need a command, set up within
	//CustomInventoriesCommands.java in this folder.

	public ExampleCustomInventory(Player player) {
		//super creates the GUI with arguments of player to open for, slots in GUI,
		//and the name of the container (top line in the chest)
		super(player, 27, "Example GUI");

		//Main setup thread, create the first page of the GUI that loads in here.
		GUIUtils.fillWithFiller(mInventory);

		ItemStack exampleItem = new ItemStack(Material.BAKED_POTATO, 1);
		mInventory.setItem(13, exampleItem);
	}

	@Override
	protected void inventoryClick(InventoryClickEvent event) {
		//Always cancel at the start if you want to avoid item removal
		event.setCancelled(true);
		GUIUtils.refreshOffhand(event);
		//Check to make sure they clicked the GUI, didn't shift click, and
		//did not click the filler item
		if (event.getClickedInventory() != mInventory
			    || event.getCurrentItem() == null
			    || event.getCurrentItem().getType() == GUIUtils.FILLER_MATERIAL
			    || event.isShiftClick()) {
			return;
		}

		if (event.getSlot() == 13) {
			event.getWhoClicked().closeInventory();
		}
	}
}
