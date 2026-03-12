package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import org.bukkit.entity.Item;

public class Spiritshot implements Enchantment {
	@Override
	public String getName() {
		return "Spiritshot";
	}

	@Override
	public void onSpawn(Plugin plugin, Item item, double level) {
		item.remove();
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.SPIRITSHOT;
	}
}
