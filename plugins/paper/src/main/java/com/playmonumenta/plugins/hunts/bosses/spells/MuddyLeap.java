package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.hunts.bosses.ExperimentSeventyOne;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.LocationUtils;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class MuddyLeap extends Spell {
	private static final double LEAP_STRENGTH = 1.75;

	private static final double IMPACT_RADIUS = 4;

	private static final int MUD_TIME = 12 * 20;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final World mWorld;
	private final ExperimentSeventyOne mExperiment;

	public MuddyLeap(Plugin plugin, LivingEntity boss, ExperimentSeventyOne experiment) {
		mPlugin = plugin;
		mBoss = boss;
		mWorld = boss.getWorld();
		mExperiment = experiment;
	}

	@Override
	public void run() {
		mWorld.playSound(mBoss.getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, SoundCategory.HOSTILE, 4f, 0.85f);

		if (mExperiment.getCurrentTarget() != null) {
			Location target = mExperiment.getCurrentTarget().getLocation();
			Vector launchDirection = target.subtract(mBoss.getLocation()).toVector().normalize().setY(0.5).multiply(LEAP_STRENGTH);

			mBoss.setVelocity(launchDirection);

			BukkitRunnable runnable = new BukkitRunnable() {
				int mTicks = 0;

				@Override
				public void run() {
					if (mTicks > 10 && mBoss.isOnGround()) {
						mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 4f, 0.8f);

						new PPCircle(Particle.BLOCK_CRACK, mBoss.getLocation().clone().add(0, 0.2, 0), IMPACT_RADIUS)
							.data(Material.MUD.createBlockData())
							.ringMode(false)
							.countPerMeter(10)
							.spawnAsBoss();

						for (double x = -IMPACT_RADIUS; x < IMPACT_RADIUS; x++) {
							for (double z = -IMPACT_RADIUS; z < IMPACT_RADIUS; z++) {
								if (x * x + z * z < IMPACT_RADIUS * IMPACT_RADIUS) {
									Location ground = LocationUtils.fallToGround(mBoss.getLocation().clone().add(x, 0, z), 5);
									mExperiment.placeMudBlock(ground.subtract(0, 1, 0).getBlock(), MUD_TIME);
								}
							}
						}

						this.cancel();
					}

					mTicks++;
					if (mTicks > 60 || mBoss.isDead()) {
						this.cancel();
					}
				}
			};
			runnable.runTaskTimer(mPlugin, 0, 1);
			mActiveRunnables.add(runnable);
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
