package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;

public class SpellSpecterParticle extends Spell {

	private static final Particle.DustOptions SPECTRAL_COLOR = new Particle.DustOptions(Color.fromRGB(0, 0, 0), 1.0f);

	private final LivingEntity mBoss;

	public SpellSpecterParticle(LivingEntity boss) {
		mBoss = boss;
	}

	@Override
	public void run() {
		Location loc = mBoss.getLocation().add(0, 1, 0);
		new PartialParticle(Particle.REDSTONE, loc.clone().add(0, 0.5, 0), 15, 0.25, 0.25, 0.25, 0, SPECTRAL_COLOR).spawnAsEntityActive(mBoss);
		new PartialParticle(Particle.REDSTONE, loc.clone().add(0, -0.5, 0), 15, 0.15, 0.5, 0.15, 0, SPECTRAL_COLOR).spawnAsEntityActive(mBoss);
		new PartialParticle(Particle.SMOKE_LARGE, loc, 1, 0.2, 0.5, 0.2, 0).spawnAsEntityActive(mBoss);
	}

	@Override
	public int cooldownTicks() {
		// This is the period of run()
		return 5;
	}
}
