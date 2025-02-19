package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class Reverb implements Enchantment {
	private static final int DETECTION_RADIUS = 8;
	private static final double OVERKILL_DAMAGE_MULTIPLIER_PER_LEVEL = 0.1;
	private static final double HIGHEST_DAMAGE_MULTIPLIER_PER_LEVEL = 0.05;

	private double mDamageThisTick = 0;
	private double mHighestDamageThisTick = 0;
	private double mEnemyHealth = 0;
	private @Nullable LivingEntity mEntity;

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.REVERB;
	}

	@Override
	public String getName() {
		return "Reverb";
	}

	// Event occurs before the actual damages are applied to the mob, on the same tick.
	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		if (EntityUtils.isHostileMob(enemy) &&
			(event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK ||
			event.getCause() == EntityDamageEvent.DamageCause.PROJECTILE)) {
			mEnemyHealth = enemy.getHealth();
			mEntity = enemy;
			mDamageThisTick = 0;
		}
	}

	// Applies after the actual damages are applied to the mob, on the same tick. At the very least this code will run for the struck entity on the ENTITY ATTACK.
	@Override
	public void onDamageDelayed(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		// Don't want to include damage done to other entities, so we make sure the only one that's counted is the one that receives the ENTITY_ATTACK.
		// Also, don't include 'Coup de Grace', it deals 9000 damage and doesn't make sense here anyway.
		if (enemy != mEntity || event.getAbility() == ClassAbility.COUP_DE_GRACE) {
			return;
		}

		// Calculate damage dealt this tick by adding to mDamageThisTick for every valid DamageEvent.
		double damage = event.getDamage();
		mDamageThisTick += damage;
		mHighestDamageThisTick = FastMath.max(damage, mHighestDamageThisTick);

		DamageEvent.DamageType type = event.getType();
		World world = enemy.getWorld();
		Location loc = enemy.getLocation();
		if (type == DamageEvent.DamageType.MELEE) {
			world.playSound(loc, Sound.ENTITY_ALLAY_ITEM_TAKEN, SoundCategory.PLAYERS, 0.4f, 1.1f);
			world.playSound(loc, Sound.ENTITY_ENDER_EYE_DEATH, SoundCategory.PLAYERS, 0.1f, 0.7f);
			world.playSound(loc, "minecraft:block.amethyst_block.resonate", SoundCategory.PLAYERS, 1.0f, 0.5f);
		}

		// Start the task 1 tick later, to give ample time to sum the damage, and check if the mob is definitely dead.
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			if (mDamageThisTick != 0) {
				// Calculate overkill damage, if it's less than 0 (thus not a kill), exit.
				double overkill = FastMath.max(mDamageThisTick - mEnemyHealth, 0);
				double highestDamage = mHighestDamageThisTick;
				// mDamageThisTick, mHighestDamageThisTick, mEnemyHealth, mEntity are no longer needed, we can get rid of them safely since 1 tick has passed.
				// Also prevents the task from running multiple times with the above check.
				resetValues();
				if (!enemy.isDead()) {
					return;
				}

				final Location enemyLocation = enemy.getEyeLocation().clone();
				LivingEntity hitMob = EntityUtils.getNearestHostileTargetable(enemyLocation, DETECTION_RADIUS);
				if (hitMob == null) {
					return;
				}

				world.playSound(enemyLocation, Sound.ENTITY_ALLAY_ITEM_TAKEN, SoundCategory.PLAYERS, 2.0f, 0.7f);
				world.playSound(enemyLocation, Sound.ENTITY_VEX_HURT, SoundCategory.PLAYERS, 5.0f, 0.4f);
				world.playSound(enemyLocation, "minecraft:block.amethyst_block.resonate", SoundCategory.PLAYERS, 0.2f, 2.0f);

				// Start the particle show, then apply damage.
				new BukkitRunnable() {
					int mTicks = 0;
					double mScaledTicks;
					final int mBoltDurations = 7;
					final double[] mRotation = { -3 * Math.PI / 8, 0, 3 * Math.PI / 8 };
					final Color[] mColor = { rollColor(), rollColor(), rollColor() };
					@Override
					public void run() {
						// Scaled ticks calculated compressed to [0, 1] and used to do vector magic.
						mScaledTicks = (double) mTicks / (double) mBoltDurations;
						Location targetLocation = hitMob.getEyeLocation().clone();
						Vector direction = targetLocation.clone().subtract(enemyLocation.clone()).getDirection();
						Vector normal = direction.getCrossProduct(new Vector(0, 1, 0)).getCrossProduct(direction);

						// When the animation is done:
						if (mTicks >= mBoltDurations) {
							new PartialParticle(Particle.SOUL, targetLocation, 25, 0.3, 0.3, 0.3, 0.05).spawnAsEnemy();
							world.playSound(targetLocation, Sound.ENTITY_ENDER_EYE_DEATH, SoundCategory.PLAYERS, 0.8f, 0.4f);
							world.playSound(targetLocation, Sound.ENTITY_ELDER_GUARDIAN_HURT, SoundCategory.PLAYERS, 1.3f, 1.6f);
							world.playSound(targetLocation, Sound.ITEM_TRIDENT_RETURN, SoundCategory.PLAYERS, 1.0f, 0.6f);

							double finalDamage = value * (overkill * OVERKILL_DAMAGE_MULTIPLIER_PER_LEVEL + highestDamage * HIGHEST_DAMAGE_MULTIPLIER_PER_LEVEL);
							DamageUtils.damage(player, hitMob, DamageEvent.DamageType.OTHER, finalDamage, ClassAbility.REVERB, true, false);

							this.cancel();
							return;
						}

						// The animation, repeated 3 times for all 3 bolts:
						for (int i = 0; i < 3; i++) {
							Vector variance = normal.clone().rotateAroundNonUnitAxis(direction, mRotation[i]);

							drawParticle(enemyLocation.clone()
								.add(targetLocation.clone().subtract(enemyLocation).multiply(mScaledTicks))
								.add(variance.clone().multiply(arcFunction(mScaledTicks))), mColor[i]);
						}
						mTicks++;
					}
				}.runTaskTimer(plugin, 0, 1);
			}
		}, 1);
	}

	private void resetValues() {
		mDamageThisTick = 0;
		mHighestDamageThisTick = 0;
		mEnemyHealth = 0;
		mEntity = null;
	}

	private void drawParticle(Location location, Color color) {
		new PartialParticle(Particle.REDSTONE, location, 20, 0.1, 0.1, 0.1,
			new Particle.DustOptions(color, 0.75f))
			.spawnAsEnemy();
	}

	// 2D function of the arc shape, defined on t in [0, 1]
	private double arcFunction(double t) {
		return -5 * t * (t - 1);
	}

	// Gives a nice random color. Well. Random between a shade of blue or purple.
	private Color rollColor() {
		int randInt = FastUtils.randomIntInRange(0, 60);
		if (FastUtils.randomIntInRange(0, 1) == 0) {
			return Color.fromRGB(80, 140 + randInt, 200);
		}
		return Color.fromRGB(80 + randInt, 80, 200);
	}

	@Override
	public void onProjectileLaunch(Plugin plugin, Player player, double value, ProjectileLaunchEvent event, Projectile projectile) {
		if (EntityUtils.isAbilityTriggeringProjectile(projectile, false) && !AbilityUtils.isVolley(player, projectile)) {
			World world = player.getWorld();
			Location loc = player.getLocation();
			AbilityUtils.playPassiveAbilitySound(loc, Sound.ENTITY_ALLAY_ITEM_TAKEN, 1.5f, 0.7f);
			AbilityUtils.playPassiveAbilitySound(loc, Sound.ENTITY_ENDER_EYE_DEATH, 0.4f, 0.4f);
			AbilityUtils.playPassiveAbilitySound(loc, Sound.ENTITY_VEX_HURT, 2.0f, 0.6f);
			AbilityUtils.playPassiveAbilitySound(loc, Sound.ENTITY_ALLAY_HURT, 0.2f, 0.6f);
			world.playSound(loc, "minecraft:block.amethyst_block.resonate", SoundCategory.PLAYERS, 0.5f, 0.7f);
		}
	}
}
