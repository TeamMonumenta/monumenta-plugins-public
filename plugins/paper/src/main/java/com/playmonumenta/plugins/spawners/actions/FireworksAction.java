package com.playmonumenta.plugins.spawners.actions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.spawners.SpawnerBreakAction;
import com.playmonumenta.plugins.utils.BlockUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class FireworksAction extends SpawnerBreakAction {
	public static final String IDENTIFIER = "fireworks";

	public FireworksAction() {
		super(IDENTIFIER);
		addParameter("side_count", 5);
		addParameter("launch_velocity", 1.1);
		addParameter("fuse_ticks", 40);
		addParameter("volley_count", 3);
	}

	@Override
	public void run(Player player, Block spawner, Map<String, Object> parameters, @Nullable String losPool) {
		Location spawnerLoc = BlockUtils.getCenterBlockLocation(spawner);
		int sideCount = (int) getParameter(parameters, "side_count");
		double launchVel = (double) getParameter(parameters, "launch_velocity");
		int fuseTicks = (int) getParameter(parameters, "fuse_ticks");
		int volleyCount = Math.max(1, (int) getParameter(parameters, "volley_count"));
		Entity entity = spawner.getWorld().spawnEntity(spawnerLoc, EntityType.PRIMED_TNT);
		if (entity instanceof TNTPrimed tnt) {
			tnt.setFuseTicks(fuseTicks);
			tnt.setVelocity(new Vector(0, launchVel, 0));
			aesthetics(spawnerLoc);
			new BukkitRunnable() {
				final TNTPrimed mTnt = tnt;
				final int mCount = sideCount;
				final int mVolleyModulo = Math.max(1, (fuseTicks - 1) / volleyCount);

				int mT = 1;

				@Override
				public void run() {
					if (!mTnt.isValid()) {
						cancel();
						return;
					}

					if (mT % mVolleyModulo == 0) {
						launchSides(mCount, tnt.getLocation());
					}
					new PartialParticle(Particle.FIREWORKS_SPARK, tnt.getLocation(), 3).extra(0.01).spawnAsEnemy();
					mT++;
				}

				private void launchSides(int count, Location pos) {
					double degStep = 360.0 / count;
					Vector vel = new Vector(0.5, 0, 0);
					vel = VectorUtils.rotateYAxis(vel, FastUtils.randomIntInRange(90, 270));
					for (int i = 0; i < count; i++) {
						Entity entity = pos.getWorld().spawnEntity(pos, EntityType.PRIMED_TNT);
						if (entity instanceof TNTPrimed tnt) {
							tnt.setFuseTicks(FastUtils.randomIntInRange(18, 26));
							Vector randomizedVel = vel.clone()
								.multiply(new Vector(FastUtils.randomDoubleInRange(0.85, 1.15), FastUtils.randomDoubleInRange(0.85, 1.15), FastUtils.randomDoubleInRange(0.85, 1.15)));
							tnt.setVelocity(randomizedVel);
						}
						vel = VectorUtils.rotateYAxis(vel, degStep);
					}
				}
			}.runTaskTimer(Plugin.getInstance(), 0, 1);
		}
	}

	private void aesthetics(Location spawnerLoc) {
		spawnerLoc.getWorld().playSound(spawnerLoc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.HOSTILE, 1f, 1f);
		spawnerLoc.getWorld().playSound(spawnerLoc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.HOSTILE, 1f, 1f);

		new PartialParticle(Particle.FIREWORKS_SPARK, spawnerLoc, 25).delta(0.25).extra(0.5).spawnAsEnemy();
	}

	@Override
	public void periodicAesthetics(Block spawnerBlock) {
		Location blockLoc = BlockUtils.getCenterBlockLocation(spawnerBlock);
		new PartialParticle(Particle.SMOKE_LARGE, blockLoc, 15).delta(0.25).spawnAsEnemy();
	}
}
