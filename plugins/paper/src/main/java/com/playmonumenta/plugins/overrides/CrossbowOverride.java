package com.playmonumenta.plugins.overrides;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ZoneUtils;
import com.playmonumenta.plugins.utils.ZoneUtils.ZoneProperty;

public class CrossbowOverride extends BaseOverride {
	@Override
	public boolean rightClickItemInteraction(Plugin plugin, Player player, Action action, ItemStack item, Block block) {
		if (player.getInventory().getItemInMainHand().getType() == Material.FIREWORK_ROCKET || player.getInventory().getItemInOffHand().getType() == Material.FIREWORK_ROCKET) {
			return false;
		}
		return true;
	}
}
