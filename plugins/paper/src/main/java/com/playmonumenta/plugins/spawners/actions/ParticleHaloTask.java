package com.playmonumenta.plugins.spawners.actions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPCircle;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

public class ParticleHaloTask extends BukkitRunnable {
	private final LivingEntity mMob;
	private final Particle mParticle;


	public ParticleHaloTask(LivingEntity mob, Particle particle) {
		this.mMob = mob;
		this.mParticle = particle;
	}

	@Override
	public void run() {
		if (mMob.isDead() || !mMob.isValid()) {
			this.cancel();
			return;
		}

		Location location = mMob.getLocation().add(0, mMob.getHeight() / 2, 0);
		double radius = mMob.getWidth();
		new PPCircle(mParticle, location, radius)
			.countPerMeter(2)
			.directionalMode(false)
			.rotateDelta(true)
			.spawnAsEnemy();
	}

	public void start() {
		this.runTaskTimer(Plugin.getInstance(), 0, 10);
	}
}
