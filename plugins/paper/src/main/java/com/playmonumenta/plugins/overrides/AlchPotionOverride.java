package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ItemUtils;
import org.bukkit.block.Block;
import org.bukkit.inventory.ItemStack;

public class AlchPotionOverride extends BaseOverride {
	@Override
	public boolean blockDispenseInteraction(Plugin plugin, Block block, ItemStack dispensed) {
		return !ItemUtils.isAlchemistItem(dispensed);
	}
}
