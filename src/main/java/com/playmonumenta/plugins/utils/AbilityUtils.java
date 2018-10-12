package com.playmonumenta.plugins.utils;

import com.playmonumenta.plugins.abilities.scout.BowMastery;
import com.playmonumenta.plugins.Plugin;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

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

	private static final int BOW_MASTER_1_DAMAGE = 3;
	private static final int BOW_MASTER_2_DAMAGE = 6;

	public static int getBowMasteryDamage(Player player) {
		int bowMastery = ScoreboardUtils.getScoreboardValue(player, "BowMastery");
		if (bowMastery > 0) {
			int bonusDamage = bowMastery == 1 ? BOW_MASTER_1_DAMAGE : BOW_MASTER_2_DAMAGE;
			return bonusDamage;
		}
		return 0;
	}

}
