package com.playmonumenta.plugins.overrides;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;

public class WrittenBookOverride extends BaseOverride {
	@Override
	public boolean rightClickItemInteraction(Plugin plugin, Player player, Action action, ItemStack item, Block block) {
		if (item.getItemMeta().getDisplayName().contains("Personal Enchanted Book")) {
			if (player.hasPermission("monumenta.peb")) {
				player.performCommand("openpeb " + player.getDisplayName());
				return false;
			}
		}
		return true;
	}
}
