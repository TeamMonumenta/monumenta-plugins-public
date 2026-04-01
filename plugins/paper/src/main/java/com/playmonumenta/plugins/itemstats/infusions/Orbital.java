package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import java.util.List;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class Orbital implements Infusion {
	public static final double DAMAGE_REDUCTION_PER_LEVEL = 0.015;
	private static final Vector DOWNWARD_KNOCK = new Vector(0, -0.3, 0);
	private static final List<DamageEvent.DamageType> ALLOWED_TYPES = List.of(
		DamageEvent.DamageType.MELEE,
		DamageEvent.DamageType.PROJECTILE
	);

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.ORBITAL;
	}

	@Override
	public String getName() {
		return "Orbital";
	}

	@Override
	public double getPriorityAmount() {
		return 150;
	}

	@Override
	public void onHurt(Plugin plugin, Player player, double value, DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (source != null && !source.isOnGround()) {
			event.updateDamageWithMultiplier(getDamageTakenMultiplier(value));
			knockDown(player, source);
		}
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		if (ALLOWED_TYPES.contains(event.getType()) && !enemy.isOnGround()) {
			knockDown(player, enemy);
		}
	}

	private void knockDown(Player player, LivingEntity enemy) {
		MovementUtils.knockAwayDirection(DOWNWARD_KNOCK.multiply(EntityUtils.isFlyingMob(enemy) ? 3 : 1), enemy, 0.8f);

		final double multiplier = Math.min(1 - EntityUtils.getAttributeOrDefault(enemy, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 0), 1)
			* Math.min(0.3 + 0.07 * player.getLocation().distance(enemy.getLocation()), 1);
		if (multiplier > 0) {
			new PPCircle(Particle.DUST_PLUME, enemy.getEyeLocation(), enemy.getWidth())
				.delta(0, 0.4, 0)
				.directionalMode(true)
				.extra(1)
				.ringMode(false)
				.count((int) (12 * multiplier))
				.spawnAsPlayerActive(player);
			new PPCircle(Particle.DRAGON_BREATH, enemy.getEyeLocation(), 0.1)
				.delta(0.7, 2, 0)
				.deltaVariance(true, false, true, false, false, false)
				.rotateDelta(true)
				.directionalMode(true)
				.extra(0.1)
				.ringMode(false)
				.count((int) (14 * multiplier))
				.spawnAsPlayerActive(player);
		}
	}

	public static double getDamageTakenMultiplier(double level) {
		return 1 - DAMAGE_REDUCTION_PER_LEVEL * level;
	}
}
