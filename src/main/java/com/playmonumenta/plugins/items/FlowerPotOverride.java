package com.playmonumenta.plugins.items;

import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;

public class FlowerPotOverride extends OverrideItem {
	@Override
	public boolean rightClickBlockInteraction(Plugin plugin, Player player, Action action, ItemStack item, Block block) {
		if (player == null) {
			return true;
		}

		if (player.getGameMode() == GameMode.ADVENTURE) {
			return false;
		}

		// Don't allow non-creative players to put saplings with lore text in flower pots
		return (player.getGameMode() == GameMode.CREATIVE || item == null || !item.hasItemMeta() || !item.getItemMeta().hasLore());
	}
}
