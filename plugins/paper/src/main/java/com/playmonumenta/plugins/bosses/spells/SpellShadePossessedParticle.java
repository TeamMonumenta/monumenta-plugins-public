package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.particle.PartialParticle;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;

public class SpellShadePossessedParticle extends Spell {

	private final LivingEntity mBoss;

	public SpellShadePossessedParticle(LivingEntity boss) {
		mBoss = boss;
	}

	@Override
	public void run() {
		Location loc = mBoss.getLocation().add(0, 1, 0);
		new PartialParticle(Particle.SOUL_FIRE_FLAME, loc, 6, 0.25, 0.6, 0.25, 0).spawnAsEntityActive(mBoss);
	}

	@Override
	public int cooldownTicks() {
		// This is the period of run()
		return 5;
	}
}
