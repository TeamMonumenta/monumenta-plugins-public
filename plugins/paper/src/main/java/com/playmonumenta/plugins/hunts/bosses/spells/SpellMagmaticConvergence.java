package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hunts.bosses.CoreElemental;
import com.playmonumenta.plugins.particle.PPParametric;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellMagmaticConvergence extends Spell implements CoreElemental.CoreElementalBase {
	// Time needed to charge/ telegraph
	private static final int CHARGE_TIME = 20 * 2;
	// Time needed for the circle to shrink to the minimum radius
	private static final int SHRINK_TIME = 50;
	// Damage dealt every 0.5 seconds, to players outside the circle
	private static final int DAMAGE = 40;
	// Initial radius of the circle
	private static final int RADIUS = 30;
	// Minimum radius of the circle
	private static final int MINIMUM_RADIUS = 6;
	private static final Particle.DustOptions ORANGE_PARTICLE = new Particle.DustOptions(Color.fromRGB(252, 94, 3), 5);
	private static final Particle.DustOptions ORANGE_SMALL = new Particle.DustOptions(Color.fromRGB(252, 94, 3), 2);
	private boolean mOnCooldown = false;
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final CoreElemental mQuarry;
	private final Location mStartLoc;
	private Location mCenterLoc;

	public SpellMagmaticConvergence(Plugin plugin, LivingEntity boss, CoreElemental quarry, Location startLoc) {
		mPlugin = plugin;
		mBoss = boss;
		mQuarry = quarry;
		mStartLoc = startLoc;
		mCenterLoc = startLoc;
	}

	@Override
	public void run() {
		// Cooldown
		mCenterLoc = LocationUtils.fallToGround(mBoss.getLocation(), mStartLoc.getY() - 5);
		PlayerUtils.playersInRange(mCenterLoc, RADIUS + 10, true).forEach(player -> player.playSound(player, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.HOSTILE, 2f, 0.2f));
		mOnCooldown = true;
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> mOnCooldown = false, cooldownTicks() + 20 * 3);

		// Telegraph
		BukkitRunnable runnable = new BukkitRunnable() {
			int mTick = 0;

			@Override
			public void run() {
				if (mTick % 10 == 0) {
					for (int i = 1; i < 16; i++) {
						int height = i;
						new PPParametric(Particle.REDSTONE, mCenterLoc, (t, builder) -> {
							double radius = RADIUS + (100.0 / (height - 30)) + 100.0 / 30;
							Location finalLocation = mCenterLoc.clone().add(radius * FastUtils.cosDeg(t * 360), height, radius * FastUtils.sinDeg(t * 360));
							builder.location(finalLocation);
						})
							.delta(0.25)
							.count(RADIUS * 2)
							.data(ORANGE_PARTICLE)
							.spawnAsEntityActive(mBoss);
					}
				}

				double radius = (double) RADIUS * (CHARGE_TIME - mTick) / (double) CHARGE_TIME;
				new PPParametric(Particle.EXPLOSION_NORMAL, mCenterLoc,
					(t, builder) -> {
						Location finalLocation = LocationUtils.fallToGround(mCenterLoc.clone().add(radius * FastUtils.cosDeg(t * 360), 15, radius * FastUtils.sinDeg(t * 360)), mStartLoc.getY() - 5);
						builder.location(finalLocation);
					}).count(8).delta(0.5, 0, 0.5).spawnAsEntityActive(mBoss);

				if (mTick++ >= CHARGE_TIME) {
					this.cancel();
					mActiveRunnables.remove(this);
					shrinkSphere();
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	private void shrinkSphere() {
		// Shrink particles
		for (Player player : PlayerUtils.playersInRange(mCenterLoc, RADIUS + 10, true)) {
			player.playSound(player, Sound.ITEM_ELYTRA_FLYING, SoundCategory.HOSTILE, 1f, 0.7f);
			player.playSound(player, Sound.BLOCK_LAVA_AMBIENT, SoundCategory.HOSTILE, 3f, 1.34f);
		}
		BukkitRunnable runnable = new BukkitRunnable() {
			final ArrayList<Player> mPlayers = new ArrayList<>(PlayerUtils.playersInRange(mCenterLoc, 70, true));
			double mRadius = RADIUS;
			int mT = 0;
			final double mCoefficient = (double) (RADIUS - MINIMUM_RADIUS) / (SHRINK_TIME + 0.03 * SHRINK_TIME * SHRINK_TIME);

			@Override
			public void run() {
				// Effects
				new PPParametric(Particle.SMOKE_LARGE, mCenterLoc,
					(t, builder) -> {
						Location finalLocation = LocationUtils.fallToGround(mCenterLoc.clone().add(mRadius * FastUtils.cosDeg(t * 360), 15, mRadius * FastUtils.sinDeg(t * 360)), mStartLoc.getY() - 5);
						Vector finalVector = LocationUtils.getDirectionTo(mCenterLoc, finalLocation);
						builder.location(finalLocation);
						builder.offset(finalVector.getX(), finalVector.getY(), finalVector.getZ());
					}).count((int) (mRadius * 3)).delta(0, 0.5, 0).directionalMode(true).spawnAsEntityActive(mBoss);
				new PPParametric(Particle.REDSTONE, mCenterLoc,
					(t, builder) -> {
						Location finalLocation = LocationUtils.fallToGround(mCenterLoc.clone().add(MINIMUM_RADIUS * FastUtils.cosDeg(t * 360), 15, MINIMUM_RADIUS * FastUtils.sinDeg(t * 360)), mStartLoc.getY() - 5);
						builder.location(finalLocation);
					}).count(MINIMUM_RADIUS * 4).data(ORANGE_SMALL).spawnAsEntityActive(mBoss);

				if (mT % 20 == 0) {
					for (int i = 1; i < 16; i++) {
						int height = i;
						new PPParametric(Particle.REDSTONE, mCenterLoc, (t, builder) -> {
							double radius = mRadius + (100.0 / (height - 30)) + (double) 100 / 30;
							Location finalLocation = mCenterLoc.clone().add(radius * FastUtils.cosDeg(t * 360), height, radius * FastUtils.sinDeg(t * 360));
							builder.location(finalLocation);
						})
							.delta(0.25)
							.count((int) (mRadius * 2))
							.data(ORANGE_PARTICLE)
							.spawnAsEntityActive(mBoss);
					}
				}
				if (mT % 10 == 0) {
					new PPParametric(Particle.EXPLOSION_LARGE, mCenterLoc, (t, b) -> {
						Vector direction = VectorUtils.randomUnitVector().multiply(mRadius);
						direction.setY(Math.abs(direction.getY()));
						Location finalLocation = mCenterLoc.clone().add(direction);
						b.location(finalLocation);
						mBoss.getWorld().playSound(finalLocation, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 0.6f, 0.5f + (float) (1 - mRadius / RADIUS) / 2);
					})
						.count(15)
						.spawnAsEntityActive(mBoss);
				}
				if (mT % 5 == 0) {
					// Pull back boss
					if (mBoss.getLocation().distanceSquared(mCenterLoc) > mRadius * mRadius) {
						Vector finalVelocity = LocationUtils.getVectorTo(mCenterLoc, mBoss.getLocation()).multiply(0.12).setY(0.3);
						mBoss.setVelocity(finalVelocity);
					}
					// Pull back players and damage them
					for (Player player : mPlayers) {
						if (player.getLocation().distanceSquared(mCenterLoc) > mRadius * mRadius
							&& !PlayerUtils.playerIsInPOI(player)) {
							Vector finalVelocity = LocationUtils.getVectorTo(mCenterLoc, player.getLocation()).multiply(0.08).setY(0.3);
							player.setVelocity(finalVelocity);
							DamageUtils.damage(mBoss, player, DamageEvent.DamageType.MAGIC, DAMAGE, null, false, false, getSpellName());

							// Effects
							PotionUtils.applyPotion(com.playmonumenta.plugins.Plugin.getInstance(), player, new PotionEffect(PotionEffectType.BLINDNESS, 40, 1, false, false, false));
							player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_HURT, SoundCategory.HOSTILE, 2f, 1.26f);
							player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, SoundCategory.HOSTILE, 2f, 1.54f);
						}
					}
				}
				if (mRadius <= MINIMUM_RADIUS) {
					for (Player player : PlayerUtils.playersInRange(mCenterLoc, RADIUS + 10, true)) {
						player.playSound(player, Sound.BLOCK_BEACON_DEACTIVATE, SoundCategory.HOSTILE, 2f, 0.2f);
						player.stopSound(Sound.ITEM_ELYTRA_FLYING, SoundCategory.HOSTILE);
					}
					this.cancel();
					mActiveRunnables.remove(this);
				} else {
					mRadius = RADIUS - mCoefficient * mT * (1 + 0.03 * mT);
					mT++;
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);

	}

	@Override
	public int cooldownTicks() {
		return 20 * 12;
	}

	@Override
	public boolean canRun() {
		return !mOnCooldown && !mQuarry.mIsCastingBanish;
	}

	@Override
	public String getSpellName() {
		return "Magmatic Convergence";
	}

	@Override
	public String getSpellChargePrefix() {
		return "Charging";
	}

	@Override
	public int getChargeDuration() {
		return CHARGE_TIME;
	}

	@Override
	public int getSpellDuration() {
		return SHRINK_TIME;
	}
}
