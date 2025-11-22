package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.DevourBoss;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.EvokerFangs;
import org.bukkit.entity.LivingEntity;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellDevour extends Spell {

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final DevourBoss.Parameters mParameters;

	public SpellDevour(Plugin plugin, LivingEntity boss, DevourBoss.Parameters parameters) {
		mPlugin = plugin;
		mBoss = boss;
		mParameters = parameters;
	}

	@Override
	public void run() {
		// Run Initial Particles
		mParameters.PARTICLES_INITIAL.spawn(mBoss, mBoss.getLocation().clone().add(0, 1, 0));
		mParameters.SOUND_INITIAL.play(mBoss.getLocation());

		// Initial Loop
		for (LivingEntity target : mParameters.TARGETS.getTargetsList(mBoss)) {
			// Init Locations
			Location targetLocation = target.getLocation();
			Location bossLocation = mBoss.getLocation();
			Vector dir = LocationUtils.getDirectionTo(targetLocation, bossLocation);
			double yaw = Math.toDegrees(-Math.atan2(dir.getX(), dir.getZ()));

			BukkitRunnable runnableIteration = new BukkitRunnable() {
				int mIterations = 0;
				double mRadius = Math.max(0.1, mParameters.INITIAL_RADIUS);

				@Override
				public void run() {
					if (mIterations >= mParameters.NUM_ITERATION) {
						this.cancel();
						return;
					}

					for (int i = 0; i < mParameters.RING_THICKNESS; i++) {
						double loopRadius = mRadius;
						double circumference = 2 * Math.PI * loopRadius;
						double splits = circumference / mParameters.RING_SPACING;
						double dAngle = 360 / splits;

						// Create loop
						BukkitRunnable runnableLoop = new BukkitRunnable() {
							int mLoopTicks = 0;

							@Override
							public void run() {
								double startingOffset = ((mParameters.FINAL_ANGLE - mParameters.INITIAL_ANGLE) % dAngle) / 2;

								if (mLoopTicks >= mParameters.INDICATOR_DELAY) {
									// Spawn Fangs.
									Vector vec2;
									for (double degree1 = mParameters.INITIAL_ANGLE + startingOffset; degree1 <= mParameters.FINAL_ANGLE; degree1 += dAngle) {
										double radian2 = Math.toRadians(degree1);
										vec2 = new Vector(Math.cos(radian2) * loopRadius, mParameters.Y_OFFSET, Math.sin(radian2) * loopRadius);
										vec2 = VectorUtils.rotateXAxis(vec2, 0);
										vec2 = VectorUtils.rotateYAxis(vec2, yaw + 90);

										Location loc = mBoss.getLocation().clone().add(vec2);
										EvokerFangs fangs = loc.getWorld().spawn(loc, EvokerFangs.class);
										if (!mParameters.SOUNDS_EVOKER_FANGS) {
											fangs.setSilent(true);
										}
										fangs.getPersistentDataContainer().set(new NamespacedKey(mPlugin, "evoker-fang-damage"), PersistentDataType.DOUBLE, mParameters.DAMAGE);
									}

									this.cancel();
									return;
								}

								// Spawn Particles.
								if (mLoopTicks % 5 == 0) {
									mParameters.SOUND_INDICATOR.play(mBoss.getLocation());

									Vector vec2;
									for (double degree1 = mParameters.INITIAL_ANGLE + startingOffset; degree1 <= mParameters.FINAL_ANGLE; degree1 += dAngle) {
										double radian2 = Math.toRadians(degree1);
										vec2 = new Vector(Math.cos(radian2) * loopRadius, mParameters.PARTICLES_INDICATOR_Y_OFFSET, Math.sin(radian2) * loopRadius);
										vec2 = VectorUtils.rotateXAxis(vec2, 0);
										vec2 = VectorUtils.rotateYAxis(vec2, yaw + 90);

										Location loc = mBoss.getLocation().clone().add(vec2);
										mParameters.PARTICLES_INDICATOR.spawn(mBoss, loc);
									}
								}

								mLoopTicks += 1;
							}
						};
						runnableLoop.runTaskTimer(mPlugin, 0, 1);

						mRadius += mParameters.RADIUS_INCREMENT;
					}

					mIterations += 1;
				}
			};
			runnableIteration.runTaskTimer(mPlugin, mParameters.INITIAL_DELAY, mParameters.ITERATION_DELAY);
			mActiveRunnables.add(runnableIteration);
		}
	}

	@Override
	public int cooldownTicks() {
		return mParameters.COOLDOWN;
	}
}
