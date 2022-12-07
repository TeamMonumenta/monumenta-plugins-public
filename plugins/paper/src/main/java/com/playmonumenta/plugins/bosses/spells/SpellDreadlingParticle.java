package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;

public class SpellDreadlingParticle extends Spell {

	private final LivingEntity mBoss;

	public SpellDreadlingParticle(LivingEntity boss) {
		mBoss = boss;
	}

	@Override
	public void run() {
		new PartialParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 0.5, 0), 7, 0.3, 0.15, 0.3, 0).spawnAsEntityActive(mBoss);
	}

	@Override
	public int cooldownTicks() {
		// This is the period of run()
		return 5;
	}
}
