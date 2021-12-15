package com.playmonumenta.plugins.overrides;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.playmonumenta.plugins.Plugin;

public class FishingRodOverride extends BaseOverride {
	@Override
	public boolean rightClickItemInteraction(Plugin plugin, Player player, Action action, ItemStack item, @Nullable Block block) {
		if (block == null || player == null) {
			return true;
		}

		if (action == Action.RIGHT_CLICK_BLOCK) {
			//  If this is an interactable block it means they didn't really want to be fishing! :D
			if (block.getType().isInteractable()) {
				if (plugin.mTrackingManager.mFishingHook.containsEntity(player)) {
					plugin.mTrackingManager.mFishingHook.removeEntity(player);
				}
			}
		}

		return true;
	}
}
