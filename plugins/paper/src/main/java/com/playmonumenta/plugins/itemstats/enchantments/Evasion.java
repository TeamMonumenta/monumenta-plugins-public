package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Evasion implements Enchantment {

	private static final double AGIL_BONUS_PER_LEVEL = 0.2;
	private static final int DISTANCE = 4;

	@Override
	public @NotNull String getName() {
		return "Evasion";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.EVASION;
	}

	public static double applyEvasion(DamageEvent event, Plugin plugin, Player player) {
		LivingEntity source = event.getSource();
		if (source != null) {
			Location playerLoc = player.getLocation();
			Location mobLoc = source.getLocation();
			if (playerLoc.distance(mobLoc) >= DISTANCE) {
				return plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.EVASION) * AGIL_BONUS_PER_LEVEL;
			}
		}
		return 0;
	}

}
