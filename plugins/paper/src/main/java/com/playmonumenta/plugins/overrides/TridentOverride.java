package com.playmonumenta.plugins.overrides;

import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.LocationUtils;

public class TridentOverride extends BaseOverride {
	@Override
	public boolean rightClickItemInteraction(Plugin plugin, Player player, Action action, ItemStack item, Block block) {
		if (player == null) {
			return true;
		}

		if (item.getEnchantmentLevel(Enchantment.RIPTIDE) > 0 && !LocationUtils.isLocationInWater(player.getEyeLocation())) {
			return false;
		}

		return true;
	}
}
