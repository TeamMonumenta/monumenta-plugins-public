package com.playmonumenta.plugins.guis.peb;

import com.playmonumenta.plugins.commands.ToggleSwap;
import com.playmonumenta.plugins.itemstats.enchantments.Multitool;
import com.playmonumenta.plugins.listeners.BlockInteractionsListener;
import org.bukkit.Material;

final class InteractableOptionsPage extends PebPage {
	InteractableOptionsPage(PebGui gui) {
		super(gui, Material.LEVER, "Trigger/Interactable Options", "Control triggers & block interactions");
	}

	@Override
	protected void render() {
		super.render();

		entry(
			Material.DIRT,
			"Filtered Pickup and Disabled Drop",
			"Click to choose your pickup and disabled drop preferences."
		).switchTo(PebGui.PICKUP_AND_DISABLE_DROP_PAGE).set(3, 2);

		entry(
			Material.LOOM,
			"Block Interactions",
			"Click to disable or enable interactions with blocks (looms, crafting tables, beds, etc.)."
		).invertedToggle("Block interactions: ", BlockInteractionsListener.DISABLE_TAG).set(3, 3);

		entry(
			Material.IRON_PICKAXE,
			"Multitool Trigger",
			"Click to change the trigger of swapping a held Multitool item."
		).cycle(Multitool.MULTITOOL_TRIGGER_OPTION_SCORE, "Right click", "Swap", "Drop", "Disabled").set(3, 4);

		entry(
			Material.SHIELD,
			"Offhand Swapping",
			"Click to toggle whether pressing your swap key will be fully cancelled or only cancelled when a spellcast does so."
		).invertedToggle("Offhand swapping: ", ToggleSwap.SWAP_TAG).set(3, 5);

		entry(
			Material.SHIELD,
			"Offhand Swapping in Inventory",
			"Click to toggle whether pressing your swap key in an inventory will perform its vanilla action."
		).invertedToggle("Offhand swapping in inventory: ", ToggleSwap.SWAP_INVENTORY_TAG).set(3, 6);
	}
}

