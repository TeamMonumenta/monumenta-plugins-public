package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Trident;
import org.bukkit.event.entity.ProjectileLaunchEvent;

public class Piercing implements Enchantment {

	@Override
	public String getName() {
		return "Piercing";
	}

	@Override
	public ItemStatUtils.EnchantmentType getEnchantmentType() {
		return ItemStatUtils.EnchantmentType.PIERCING;
	}

	@Override
	public void onProjectileLaunch(Plugin plugin, Player player, double level, ProjectileLaunchEvent event, Projectile proj) {
		if (proj instanceof AbstractArrow arrow && !(proj instanceof Trident)) {
			// Some skills can add piercing
			arrow.setPierceLevel(arrow.getPierceLevel() + (int) level);
		}
	}
}
