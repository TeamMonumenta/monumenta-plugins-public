package com.playmonumenta.plugins.bosses.spells;

import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;

public class SpellVindictiveParticle extends Spell {

	private final LivingEntity mBoss;

	public SpellVindictiveParticle(LivingEntity boss) {
		mBoss = boss;
	}

	@Override
	public void run() {
		mBoss.getWorld().spawnParticle(Particle.VILLAGER_ANGRY, mBoss.getLocation().add(0, 1, 0), 1, 0.25, 0.5, 0.25, 0);
	}

	@Override
	public int duration() {
		// This is the period of run()
		return 5;
	}
}
