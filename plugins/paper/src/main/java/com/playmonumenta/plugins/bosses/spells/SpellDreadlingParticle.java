package com.playmonumenta.plugins.bosses.spells;

import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;

public class SpellDreadlingParticle extends Spell {

	private final World mWorld;
	private final LivingEntity mBoss;

	public SpellDreadlingParticle(LivingEntity boss) {
		mWorld = boss.getWorld();
		mBoss = boss;
	}

	@Override
	public void run() {
		mWorld.spawnParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 0.5, 0), 7, 0.3, 0.15, 0.3, 0);
	}

	@Override
	public int duration() {
		// This is the period of run()
		return 5;
	}
}
