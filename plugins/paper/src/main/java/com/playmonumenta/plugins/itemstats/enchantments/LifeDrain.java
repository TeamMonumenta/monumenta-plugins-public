package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class LifeDrain implements Enchantment {
	public static final double LIFE_DRAIN_CRIT_HEAL = 1;
	private static final double LIFE_DRAIN_NONCRIT_HEAL_MULTIPLIER = 0.5;
	public static final String CHARM_HEAL = "Life Drain Heal";

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
		if (PlayerUtils.isFallingAttack(player)) {
			PlayerUtils.healPlayer(plugin, player, CharmManager.calculateFlatAndPercentValue(player, CHARM_HEAL, LIFE_DRAIN_CRIT_HEAL * Math.sqrt(level)));
			new PartialParticle(Particle.HEART, target.getEyeLocation(), 3, 0.1, 0.1, 0.1, 0.001).spawnAsPlayerActive(player);
		} else {
			PlayerUtils.healPlayer(
				plugin,
				player, CharmManager.calculateFlatAndPercentValue(player, CHARM_HEAL, LIFE_DRAIN_NONCRIT_HEAL_MULTIPLIER
					                                                                      * Math.sqrt(level)
					                                                                      // This is * √(attack rate seconds)
					                                                                      // The same as / √(1 / attack rate seconds)
					                                                                      // Advancement simply says / √(attack speed)
					                                                                      * Math.sqrt(player.getCooldownPeriod() / Constants.TICKS_PER_SECOND)
					                                                                      * player.getCooledAttackStrength(0))

			);
			new PartialParticle(Particle.HEART, target.getEyeLocation(), 1, 0.1, 0.1, 0.1, 0.001).spawnAsPlayerActive(player);
		}
	}
}
