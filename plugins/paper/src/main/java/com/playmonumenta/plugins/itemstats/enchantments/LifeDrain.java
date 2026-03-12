package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class LifeDrain implements Enchantment {
	public static final double LIFE_DRAIN_CRIT_HEAL = 1;
	private static final double LIFE_DRAIN_NONCRIT_HEAL_MULTIPLIER = 0.5;

	@Override
	public String getName() {
		return "Life Drain";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.LIFE_DRAIN;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double level, DamageEvent event, LivingEntity target) {
		if (event.getType() != DamageType.MELEE) {
			return;
		}
		if (!EntityUtils.isHostileMob(target)) {
			return;
		}
		double healAmount;
		int particles;
		if (PlayerUtils.isFallingAttack(player)) {
			healAmount = LIFE_DRAIN_CRIT_HEAL * Math.sqrt(level);
			particles = 3;
		} else {
			healAmount = LIFE_DRAIN_NONCRIT_HEAL_MULTIPLIER
					* Math.sqrt(level)
					// This is * √(attack rate seconds)
					// The same as / √(1 / attack rate seconds)
					// Advancement simply says / √(attack speed)
					* Math.sqrt(player.getCooldownPeriod() / Constants.TICKS_PER_SECOND)
					* player.getCooledAttackStrength(0);
			particles = 1;
		}
		if (healAmount <= 0 || Double.isNaN(healAmount)) {
			MMLog.warning("Life Drain tried to heal an invalid amount? Amount: %f, level: %f, player: %s".formatted(healAmount, level, player.getName()));
			return;
		}
		PlayerUtils.healPlayer(plugin, player, healAmount);
		new PartialParticle(Particle.HEART, target.getEyeLocation(), particles, 0.1, 0.1, 0.1, 0.001).spawnAsPlayerActive(player);
	}
}
