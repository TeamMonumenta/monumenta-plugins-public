package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import org.bukkit.Material;
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

			// Some crossbows still have vanilla piercing. While fixing them would be preferable, this hack prevents their vanilla piercing from being applied.
			if (player.getInventory().getItemInMainHand().getType() == Material.CROSSBOW) {
				level -= player.getInventory().getItemInMainHand().getEnchantmentLevel(org.bukkit.enchantments.Enchantment.PIERCING);
			}

			// Some skills can add piercing
			arrow.setPierceLevel(arrow.getPierceLevel() + (int) level);
		}
	}
}
