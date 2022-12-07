package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;

public class SpellVindictiveParticle extends Spell {

	private final LivingEntity mBoss;

	public SpellVindictiveParticle(LivingEntity boss) {
		mBoss = boss;
	}

	@Override
	public void run() {
		new PartialParticle(Particle.VILLAGER_ANGRY, mBoss.getLocation().add(0, 1, 0), 1, 0.25, 0.5, 0.25, 0).spawnAsEntityActive(mBoss);
	}

	@Override
	public int cooldownTicks() {
		// This is the period of run()
		return 5;
	}
}
