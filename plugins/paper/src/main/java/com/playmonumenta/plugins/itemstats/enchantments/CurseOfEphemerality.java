package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

public class CurseOfEphemerality implements Enchantment {

	@Override
	public String getName() {
		return "Curse of Ephemerality";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.CURSE_OF_EPHEMERALITY;
	}

	@Override
	public void onSpawn(Plugin plugin, Item item, double level) {
		if (item.getThrower() != null) {
			item.remove();
		}
	}

	public static boolean isEphemeral(ItemStack item) {
		return ItemStatUtils.getEnchantmentLevel(item, EnchantmentType.CURSE_OF_EPHEMERALITY) != 0;
	}
}
