package com.playmonumenta.plugins.utils;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;

public class AbilityUtils {

	private static final double PASSIVE_DAMAGE_ELITE_MODIFIER = 2.0;
	private static final double PASSIVE_DAMAGE_BOSS_MODIFIER = 1.5;
	
	public static void rogueDamageMob(Plugin mPlugin, Player player, LivingEntity damagee, double damage) {
		double correctDamage = damage;
		if (EntityUtils.isElite(damagee)) {
			correctDamage = damage * PASSIVE_DAMAGE_ELITE_MODIFIER;
		} else if (EntityUtils.isBoss(damagee)) {
			correctDamage = damage * PASSIVE_DAMAGE_BOSS_MODIFIER;
		}
		EntityUtils.damageEntity(mPlugin, damagee, correctDamage, player);
	}
	
}
