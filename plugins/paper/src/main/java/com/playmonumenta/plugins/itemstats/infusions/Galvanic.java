package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.CrowdControlImmunityBoss;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.jetbrains.annotations.Nullable;

public class Galvanic implements Infusion {

	private static final double STUN_CHANCE_PER_LVL = 0.0125;
	public static final int DURATION_NORMAL = 2 * 20; // 2 seconds
	public static final int DURATION_ELITE = 10; // 0.5 seconds

	private static final Particle.DustOptions COLOR_YELLOW = new Particle.DustOptions(Color.fromRGB(251, 231, 30), 1f);
	private static final Particle.DustOptions COLOR_FAINT_YELLOW = new Particle.DustOptions(Color.fromRGB(255, 241, 110), 1f);

	@Override
	public String getName() {
		return "Galvanic";
	}

	@Override
	public double getPriorityAmount() {
		return 14;
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.GALVANIC;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		double modifiedLevel = DelveInfusionUtils.getModifiedLevel(plugin, player, (int) value);
		DamageType type = event.getType();

		// Only apply infusion to basic melee and projectile attacks.
		if (!(type == DamageType.MELEE || type == DamageType.PROJECTILE) || event.getAbility() != null) {
			return;
		}

		// If used with arrow, must be critical
		if (event.getDamager() instanceof AbstractArrow arrow && !(arrow instanceof Trident) && !arrow.isCritical()) {
			return;
		}

		apply(plugin, player, modifiedLevel, enemy, type == DamageType.MELEE);
	}

	@Override
	public void onHurt(Plugin plugin, Player player, double value, DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity enemy) {
		double modifiedLevel = DelveInfusionUtils.getModifiedLevel(plugin, player, (int) value);
		DamageType type = event.getType();

		if (!(type == DamageType.MELEE || type == DamageType.PROJECTILE) || event.isBlocked() || event.isCancelled()) {
			return;
		}

		if (enemy != null) {
			apply(plugin, player, modifiedLevel, enemy, false);
		}
	}

	public static void apply(Plugin plugin, Player player, double level, LivingEntity enemy, boolean isMelee) {
		double chance = STUN_CHANCE_PER_LVL * level * (isMelee ? player.getCooledAttackStrength(0) : 1);
		if (FastUtils.RANDOM.nextDouble() < chance) {
			if (EntityUtils.isElite(enemy)) {
				EntityUtils.applyStun(plugin, DURATION_ELITE, enemy);
			} else {
				EntityUtils.applyStun(plugin, DURATION_NORMAL, enemy);
			}

			if (!(EntityUtils.isBoss(enemy) || enemy.getScoreboardTags().contains(CrowdControlImmunityBoss.identityTag))) {
				Location loc = enemy.getLocation();
				World world = enemy.getWorld();
				world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.65f, 1.5f);
				loc = loc.add(0, 1, 0);
				world.spawnParticle(Particle.REDSTONE, loc, 12, 0.5, 0.5, 0.5, COLOR_YELLOW);
				world.spawnParticle(Particle.REDSTONE, loc, 12, 0.5, 0.5, 0.5, COLOR_FAINT_YELLOW);
				world.spawnParticle(Particle.FIREWORKS_SPARK, loc, 15, 0, 0, 0, 0.15);
			}
		}
	}
}
