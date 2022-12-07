package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;

public class SpellDreadnaughtParticle extends Spell {

	private static final Particle.DustOptions DREADFUL_COLOR = new Particle.DustOptions(Color.fromRGB(0, 0, 0), 1.0f);

	private final LivingEntity mBoss;

	public SpellDreadnaughtParticle(LivingEntity boss) {
		mBoss = boss;
	}

	@Override
	public void run() {
		Location loc = mBoss.getLocation().add(0, 1, 0);
		Location eyeLoc = mBoss.getEyeLocation();
		double yaw = loc.getYaw();
		new PartialParticle(Particle.FLAME, eyeLoc.clone().add(-0.2 * FastUtils.cos(yaw), 0.2, -0.2 * FastUtils.sin(yaw)), 1, 0, 0, 0, 0).spawnAsEntityActive(mBoss);
		new PartialParticle(Particle.FLAME, eyeLoc.clone().add(0.2 * FastUtils.cos(yaw), 0.2, 0.2 * FastUtils.sin(yaw)), 1, 0, 0, 0, 0).spawnAsEntityActive(mBoss);
		new PartialParticle(Particle.REDSTONE, loc.clone().add(0, -0.5, 0), 30, 0.2, 0.5, 0.2, 0, DREADFUL_COLOR).spawnAsEntityActive(mBoss);
		new PartialParticle(Particle.SMOKE_LARGE, loc, 2, 0.4, 0.5, 0.4, 0).spawnAsEntityActive(mBoss);
	}

	@Override
	public int cooldownTicks() {
		// This is the period of run()
		return 5;
	}
}
