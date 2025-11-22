package com.playmonumenta.plugins.spawners.actions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPLine;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;

public class ParticleLineTask extends BukkitRunnable {
	private final LivingEntity mMob;
	private final Particle mParticle;
	private final Block mSpawner;


	public ParticleLineTask(LivingEntity mob, Particle particle, Block spawner) {
		this.mMob = mob;
		this.mSpawner = spawner;
		this.mParticle = particle;
	}

	@Override
	public void run() {
		if (mMob.isDead() || !mMob.isValid() || mSpawner.getBlockData().getMaterial() != Material.SPAWNER) {
			this.cancel();
			return;
		}

		Location mobLoc = mMob.getLocation().add(0, mMob.getHeight() / 2, 0);
		Location spawnerLoc = mSpawner.getLocation().add(0.5, 0.5, 0.5);
		new PPLine(mParticle, mobLoc, spawnerLoc)
			.countPerMeter(2)
			.spawnAsEnemy();
	}

	public void start() {
		this.runTaskTimer(Plugin.getInstance(), 0, 10);
	}
}
