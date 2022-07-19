package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import org.bukkit.entity.Player;

public class Guard implements Enchantment {

	@Override
	public String getName() {
		return "Guard";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.GUARD;
	}

	public static double applyGuard(DamageEvent event, Plugin plugin, Player player) {
		return 0;
	}

}
