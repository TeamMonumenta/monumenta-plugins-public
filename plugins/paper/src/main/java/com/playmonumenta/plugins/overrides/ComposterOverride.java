package com.playmonumenta.plugins.overrides;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;

public class ComposterOverride extends BaseOverride {
	@Override
	public boolean rightClickBlockInteraction(Plugin plugin, Player player, Action action, ItemStack item, Block block, PlayerInteractEvent event) {
		return item == null || !(item.hasItemMeta() && item.getItemMeta().hasLore());
	}
}
