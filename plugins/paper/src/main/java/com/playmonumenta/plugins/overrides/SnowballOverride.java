package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

public class SnowballOverride extends BaseOverride {
	@Override
	public boolean blockDispenseInteraction(Plugin plugin, Block block, ItemStack dispensed) {
		return !dispensed.getItemMeta().hasDisplayName();
	}
}
