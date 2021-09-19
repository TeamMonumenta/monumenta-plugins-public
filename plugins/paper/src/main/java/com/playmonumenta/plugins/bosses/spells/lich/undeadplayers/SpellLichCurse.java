package com.playmonumenta.plugins.bosses.spells.lich.undeadplayers;

import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.player.PartialParticle;

public class SpellLichCurse extends Spell {

	private LivingEntity mBoss;
	private int mT = 0;

	public SpellLichCurse(LivingEntity boss) {
		mBoss = boss;
	}

	@Override
	public void run() {
		mT += 5;
		new PartialParticle(Particle.SOUL, mBoss.getLocation().add(0, 0.25, 0), 2, 0.2, 0.2, 0.2, 0.01).spawnAsEnemy();
		new PartialParticle(Particle.SOUL_FIRE_FLAME, mBoss.getLocation().add(0, 0.25, 0), 2, 0.2, 0.2, 0.2, 0.01).spawnAsEnemy();
		if (mT >= 20 * 20) {
			mBoss.remove();
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

}
