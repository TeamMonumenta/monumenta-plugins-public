package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class Reverb implements Enchantment {
	public static final int DETECTION_RADIUS = 8;
	public static final double DAMAGE_MULTIPLIER_PER_LEVEL = 0.1;

	public static final String CHARM_RADIUS = "Reverb Radius";
	public static final String CHARM_DAMAGE = "Reverb Damage Modifier";

	private double mDamageThisTick = 0;
	private double mEnemyHealth = 0;
	private @Nullable LivingEntity mEntity;

	@Override
	public ItemStatUtils.EnchantmentType getEnchantmentType() {
		return ItemStatUtils.EnchantmentType.REVERB;
	}

	@Override
	public String getName() {
		return "Reverb";
	}

	// Event occurs before the actual damages are applied to the mob, on the same tick.
	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		if (event.getCause() == EntityDamageEvent.DamageCause.ENTITY_ATTACK && EntityUtils.isHostileMob(enemy)) {
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
		mDamageThisTick += event.getDamage();

		// Start the task 1 tick later, to give ample time to sum the damage, and check if the mob is definitely dead.
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			if (mDamageThisTick != 0) {
				// Calculate overkill damage, if it's less than 0 (thus not a kill), exit.
				double overkill = mDamageThisTick - mEnemyHealth;
				// mDamageThisTick, mEnemyHealth, mEntity are no longer needed, we can get rid of them safely since 1 tick has passed.
				// Also prevents the task from running multiple times with the above check.
				resetValues();
				if (overkill <= 0 || !enemy.isDead()) {
					return;
				}

				final Location enemyLocation = enemy.getEyeLocation().clone();
				LivingEntity hitMob = EntityUtils.getNearestMob(enemyLocation, CharmManager.getRadius(player, CHARM_RADIUS, DETECTION_RADIUS));
				if (hitMob == null) {
					return;
				}

				// Start the particle show, then apply damage.
				new BukkitRunnable() {
					int mTicks = 0;
					double mScaledTicks;
					final int mBoltDurations = 10;
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
							player.playSound(player.getLocation(), Sound.BLOCK_SOUL_SAND_STEP, SoundCategory.PLAYERS, 1.5f, 0.5f);
							new PartialParticle(Particle.SOUL, targetLocation, 25, 0.3, 0.3, 0.3, 0.05).spawnAsEnemy();

							DamageUtils.damage(player,
								hitMob,
								DamageEvent.DamageType.OTHER,
								Math.round((CharmManager.getLevelPercentDecimal(player, CHARM_DAMAGE) + value * DAMAGE_MULTIPLIER_PER_LEVEL) * overkill),
								ClassAbility.REVERB,
								true,
								false);
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
}
