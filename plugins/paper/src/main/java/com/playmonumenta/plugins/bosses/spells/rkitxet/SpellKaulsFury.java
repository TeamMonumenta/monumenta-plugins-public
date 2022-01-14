package com.playmonumenta.plugins.bosses.spells.rkitxet;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.bosses.bosses.RKitxet;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.ParticleUtils.SpawnParticleAction;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class SpellKaulsFury extends Spell {

	public static final int RADIUS = 3;
	public static final double DAMAGE_RADIUS = 3.5;
	public static final int HEIGHT = 8;
	public static final int DAMAGE = 24;

	private LivingEntity mBoss;
	private int mTicks;
	private Plugin mPlugin;
	private RKitxet mRKitxet;

	private int mTriggerInterval;
	private int mChargeTime;
	private int mImpactTime;

	public SpellKaulsFury(Plugin plugin, LivingEntity boss, RKitxet rKitxet, int triggerInterval, int chargeTime, int impactTime, int initialDelay) {
		mPlugin = plugin;
		mBoss = boss;
		mRKitxet = rKitxet;
		mTicks = triggerInterval - initialDelay;
		mTriggerInterval = triggerInterval;
		mChargeTime = chargeTime;
		mImpactTime = impactTime;
	}

	@Override
	public void run() {
		//This function runs every 5 ticks
		mTicks += 5;
		World world = mBoss.getWorld();

		if (mTicks % mTriggerInterval == 0) {
			mTicks = 0;
			List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), RKitxet.detectionRange, false);
			if (players.size() == 0) {
				return;
			}
			if (players.size() > 1 && mRKitxet.getFuryTarget() != null) {
				players.remove(mRKitxet.getAgonyTarget());
			}
			Collections.shuffle(players);
			Player target = players.get(0);
			mRKitxet.setFuryTarget(target);

			target.playSound(target.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 2, 1.5f);
			world.playSound(target.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 2);

			BukkitRunnable furyRunnable = new BukkitRunnable() {
				int mT = 0;
				Location mLocation = target.getLocation().clone().add(0, HEIGHT, 0);

				@Override
				public void run() {
					if (!mBoss.isValid() || mBoss.isDead()) {
						this.cancel();
						return;
					}

					if (mT < mChargeTime) {
						mLocation = target.getLocation().add(0, HEIGHT, 0);

						double completionRatio = ((double) mT) / mChargeTime;
						double chargingRadius = RADIUS * completionRatio;
						world.spawnParticle(Particle.SPELL_WITCH, mLocation, 5 + (int) (completionRatio * 20), chargingRadius / 2.5, chargingRadius / 2.5, chargingRadius / 2.5, 0);
						world.spawnParticle(Particle.FLAME, mLocation, 8 + (int) (completionRatio * 25), chargingRadius / 2, chargingRadius / 2, chargingRadius / 2, 0);
						world.spawnParticle(Particle.SMOKE_LARGE, mLocation, 5 + (int) (completionRatio * 20), chargingRadius / 2.5, chargingRadius / 2.5, chargingRadius / 2.5, 0);

						target.playSound(mLocation, Sound.BLOCK_CAMPFIRE_CRACKLE, 3.0f, (float) (2 - completionRatio));
						target.playSound(mLocation, Sound.BLOCK_CAMPFIRE_CRACKLE, 3.0f, (float) (2 - completionRatio));
						target.playSound(mLocation, Sound.BLOCK_CAMPFIRE_CRACKLE, 3.0f, (float) (2 - completionRatio));
						target.playSound(mLocation, Sound.BLOCK_LAVA_POP, 3.5f, (float) (1.5 * (2 - completionRatio)));
					} else if (mT < mChargeTime + mImpactTime) {
						if (mT == mChargeTime) {
							world.playSound(mLocation, Sound.ENTITY_RAVAGER_ROAR, 1.5f, 1);
						}

						// 5.0 because 5 ticks and mImpactTime is in ticks, and to make it a double
						mLocation = mLocation.subtract(0, (5.0 * HEIGHT) / mImpactTime, 0);
						world.spawnParticle(Particle.SPELL_WITCH, mLocation, 25, RADIUS / 2.5, RADIUS / 2.5, RADIUS / 2.5, 0);
						world.spawnParticle(Particle.FLAME, mLocation, 18, RADIUS / 2, RADIUS / 2, RADIUS / 2, 0);
						world.spawnParticle(Particle.CLOUD, mLocation, 18, RADIUS / 2, RADIUS / 2, RADIUS / 2, 0);
						world.spawnParticle(Particle.SMOKE_LARGE, mLocation, 25, RADIUS / 2.5, RADIUS / 2.5, RADIUS / 2.5, 0);
						world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, mLocation, 1, RADIUS / 2.5, RADIUS / 2.5, RADIUS / 2.5, 0);

						target.playSound(mLocation, Sound.BLOCK_CAMPFIRE_CRACKLE, 3.0f, 1);
						target.playSound(mLocation, Sound.BLOCK_CAMPFIRE_CRACKLE, 3.0f, 1);
						target.playSound(mLocation, Sound.BLOCK_CAMPFIRE_CRACKLE, 3.0f, 1);
						world.playSound(mLocation, Sound.ENTITY_BLAZE_SHOOT, 1, 2.0f);
					} else {
						for (Player player : PlayerUtils.playersInRange(mLocation, DAMAGE_RADIUS, true)) {
							BossUtils.bossDamage(mBoss, player, DAMAGE, mLocation, "Kaul's Fury");
						}

						//Give 0.5 blocks of leeway for hitting the boss, don't want to make it about being super precise
						if (mLocation.distance(mRKitxet.getBossLocation()) <= DAMAGE_RADIUS + 0.5) {
							mRKitxet.mShieldSpell.removeShield();
						}

						for (LivingEntity mob : EntityUtils.getNearbyMobs(mLocation, DAMAGE_RADIUS)) {
							mob.damage(DAMAGE / 2.0);
						}

						world.playSound(mLocation, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1);

						ParticleUtils.explodingRingEffect(mPlugin, mLocation, RADIUS, 1, 4,
								Arrays.asList(
										new AbstractMap.SimpleEntry<Double, SpawnParticleAction>(0.5, (Location location) -> {
											world.spawnParticle(Particle.FLAME, location, 1, 0.1, 0.1, 0.1, 0.1);
											world.spawnParticle(Particle.SMOKE_LARGE, location, 1, 0.1, 0.1, 0.1, 0.1);
										})
								));

						mRKitxet.setFuryTarget(null);
						this.cancel();
					}

					mT += 5;
				}

			};
			mActiveRunnables.add(furyRunnable);
			furyRunnable.runTaskTimer(mPlugin, 0, 5);
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
