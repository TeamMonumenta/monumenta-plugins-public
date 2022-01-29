package com.playmonumenta.plugins.itemstats.enchantments;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;

public class Shielding implements Enchantment {

	private static final double ARMOR_BONUS_PER_LEVEL = 0.2;
	private static final double DISTANCE = 2;

	@Override
	public @NotNull String getName() {
		return "Shielding";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.SHIELDING;
	}

	public static double applyShielding(DamageEvent event, Plugin plugin, Player player) {
		LivingEntity source = event.getSource();
		if (source != null) {
			Location playerLoc = player.getLocation();
			Location mobLoc = source.getLocation();
			if (playerLoc.distance(mobLoc) <= DISTANCE) {
				return plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.SHIELDING) * ARMOR_BONUS_PER_LEVEL;
			}
		}
		return 0;
	}

}
