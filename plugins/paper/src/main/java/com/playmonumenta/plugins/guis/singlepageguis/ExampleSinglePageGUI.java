package com.playmonumenta.plugins.guis.singlepageguis;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.playmonumenta.plugins.guis.SinglePageGUI;

public class ExampleSinglePageGUI extends SinglePageGUI {

	public ExampleSinglePageGUI(@Nullable Player player, String[] args) {
		super(player, args);
	}

	/*
	 * You MUST call this method from the SinglePageGUIManager. The example
	 * call for this example GUI is there for reference.
	 */
	@Override
	public void registerCommand() {
		/*
		 * The first string is the command, the other two strings are the
		 * descriptors for the two arguments the command takes. They don't have
		 * meaning outside of semantics, the important part is that there are
		 * two required arguments to open this GUI.
		 *
		 * For example, you could call "/openexamplegui @s five steak" or
		 * "/openexamplegui @s hello world", but you could not call
		 * "/openexamplegui @s onlyoneargumentprovided"
		 *
		 * If you wanted a command with no arguments, then you could do
		 * something like `registerCommand("opennoargumentgui");`
		 */
		registerCommand("openexamplegui", "rows", "item");
	}

	@Override
	public SinglePageGUI constructGUI(Player player, String[] args) {
		return new ExampleSinglePageGUI(player, args);
	}

	@Override
	public Inventory getInventory(Player player, String[] args) {
		/*
		 * This is the inventory that will be displayed.
		 *
		 * If you specified string arguments, you can use them to alter the GUI
		 * depending on the command input (as well as any info stored to the
		 * player such as scores or tags).
		 *
		 * Here, we generate an empty inventory with 3 rows if the first
		 * argument provided to the command is "three." Otherwise, the
		 * inventory has 6 rows.
		 *
		 * If the second argument is "fish," we set the first slot of the
		 * inventory to fish.
		 */
		Inventory inventory = Bukkit.createInventory(null, args[0].equals("three") ? 27 : 54, "Example GUI");

		if (args[1].equals("fish")) {
			inventory.setItem(0, new ItemStack(Material.COD, 1));
		}

		return inventory;
	}

	@Override
	public void processClick(InventoryClickEvent event) {
		/*
		 * This is the singular method that handles clicks, but it's probably
		 * a good idea to break it up into several helper methods (you can see
		 * the GUI in DelvesUtils.DelveModifierSelectionGUI for an example).
		 *
		 * Here, we just send a message to the player telling them which slot
		 * they clicked on, and an additional message if they clicked on a
		 * fish.
		 *
		 * mInventory (stored in this object) and event.getInventory()
		 * represent the same inventory (not necessarily the same object, but
		 * that doesn't matter).
		 */
		mPlayer.sendMessage("You clicked slot " + event.getSlot());

		ItemStack clickedItem = event.getCurrentItem();
		if (clickedItem != null && clickedItem.getType() == Material.COD) {
			mPlayer.sendMessage("You clicked a fish");
		}
	}

}
