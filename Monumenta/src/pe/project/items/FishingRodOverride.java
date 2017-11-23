package pe.project.items;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

import pe.project.Plugin;
import pe.project.utils.ItemUtils;

public class FishingRodOverride extends OverrideItem {
	@Override
	public boolean rightClickItemInteraction(Plugin plugin, Player player, Action action, ItemStack item, Block block) {
		if (block == null || player == null) {
			return true;
		}

		Material blockType = block.getType();

		if (action == Action.RIGHT_CLICK_BLOCK) {
			//	If this is an interactable block it means they didn't really want to be fishing! :D
			if (ItemUtils.isInteractable(blockType)) {
				if (plugin.mTrackingManager.mFishingHook.containsEntity(player)) {
					plugin.mTrackingManager.mFishingHook.removeEntity(player);
				}
			}
		}

		return true;
	}
}
