package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.CrowdControlImmunityBoss;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemStatUtils.Slot;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.EnumSet;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Guardian;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;

public class ThunderAspect implements Enchantment {
	public static final double CHANCE = 0.1;
	public static final int DURATION_MELEE = 40;
	public static final int DURATION_PROJ = 10;
	public static final int DURATION_MELEE_ELITE = 10;

	private static final Particle.DustOptions COLOR_YELLOW = new Particle.DustOptions(Color.fromRGB(251, 231, 30), 1f);
	private static final Particle.DustOptions COLOR_FAINT_YELLOW = new Particle.DustOptions(Color.fromRGB(255, 241, 110), 1f);

	@Override
	public String getName() {
		return "Thunder Aspect";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.THUNDER_ASPECT;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND);
	}

	@Override
	public double getPriorityAmount() {
		return 14;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		DamageType type = event.getType();
		if ((type == DamageType.MELEE && ItemStatUtils.hasMeleeDamage(player.getInventory().getItemInMainHand())) || type == DamageType.PROJECTILE) {
			// If used with arrow, must be critical
			if (event.getDamager() instanceof AbstractArrow arrow && !(arrow instanceof Trident) && !arrow.isCritical()) {
				return;
			}

			if (enemy instanceof IronGolem || enemy instanceof Guardian) {
				event.setDamage(event.getDamage() + 1.0);
			}

			double rand = FastUtils.RANDOM.nextDouble();
			World world = enemy.getWorld();

			double chance = CHANCE * value * (type == DamageType.MELEE ? player.getCooledAttackStrength(0) : 1);

			if (rand < chance) {
				if (EntityUtils.isElite(enemy)) {
					if (type == DamageType.PROJECTILE) {
						return;
					}
					EntityUtils.applyStun(plugin, DURATION_MELEE_ELITE, enemy);
				} else if (type == DamageType.MELEE) {
					EntityUtils.applyStun(plugin, DURATION_MELEE, enemy);
				} else {
					EntityUtils.applyStun(plugin, DURATION_PROJ, enemy);
				}

				if (!(EntityUtils.isBoss(enemy) || enemy.getScoreboardTags().contains(CrowdControlImmunityBoss.identityTag))) {
					// Do different effects for melee and projectiles because people probably like the way it is
					if (type == DamageType.MELEE) {
						Location loc = enemy.getLocation();
						world.playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, 0.65f, 1.5f);
						loc = loc.add(0, 1, 0);
						world.spawnParticle(Particle.REDSTONE, loc, 12, 0.5, 0.5, 0.5, COLOR_YELLOW);
						world.spawnParticle(Particle.REDSTONE, loc, 12, 0.5, 0.5, 0.5, COLOR_FAINT_YELLOW);
						world.spawnParticle(Particle.FIREWORKS_SPARK, loc, 15, 0, 0, 0, 0.15);
					} else {
						Location halfHeightLocation = LocationUtils.getHalfHeightLocation(enemy);
						double widerWidthDelta = PartialParticle.getWidthDelta(enemy) * 1.5;
						// /particle dust 1 0.945 0.431 1 7053 78.9 7069 0.225 0.45 0.225 0 10
						PartialParticle partialParticle = new PartialParticle(
							Particle.REDSTONE,
							halfHeightLocation,
							10,
							widerWidthDelta,
							PartialParticle.getHeightDelta(enemy),
							widerWidthDelta,
							0,
							COLOR_FAINT_YELLOW
						).spawnAsEnemy();
						// /particle dust 0.984 0.906 0.118 1 7053 78.9 7069 0.225 0.45 0.225 0 10
						partialParticle.mExtra = 1;
						partialParticle.mData = COLOR_YELLOW;
						partialParticle.spawnAsEnemy();
						// /particle firework 7053 78.9 7069 0.225 0.45 0.225 0.5 0
						partialParticle.mParticle = Particle.FIREWORKS_SPARK;
						partialParticle.mCount = 15;
						partialParticle.mExtra = 0.4;
						partialParticle.mData = null;
						partialParticle.mDirectionalMode = true;
						partialParticle.mExtraVariance = 0.1;
						partialParticle.deltaVariance(true, false, true);
						partialParticle.mVaryPositiveY = true;
						partialParticle.spawnAsEnemy();

						Location enemyLocation = enemy.getLocation();
						// /playsound entity.firework_rocket.twinkle master @p ~ ~ ~ 0.5 1.5
						world.playSound(
							enemyLocation,
							Sound.ENTITY_FIREWORK_ROCKET_TWINKLE,
							SoundCategory.PLAYERS,
							0.5f,
							1.5f
						);
						// /playsound entity.firework_rocket.twinkle_far master @p ~ ~ ~ 0.5 1.2
						world.playSound(
							enemyLocation,
							Sound.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR,
							SoundCategory.PLAYERS,
							0.5f,
							1.2f
						);
					}
				}
			}
		}
	}
}


