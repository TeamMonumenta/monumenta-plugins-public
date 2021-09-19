package com.playmonumenta.plugins.bosses.spells.lich.undeadplayers;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.player.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;

public class SpellCrystalParticle extends Spell {

	private LivingEntity mBoss;

	public SpellCrystalParticle(LivingEntity boss) {
		mBoss = boss;
	}

	@Override
	public void run() {
		double hp = mBoss.getHealth();
		Location pCenter = mBoss.getLocation().add(0, 2, 0);
		for (double deg = 0; deg < 360; deg += 36) {
			double c = FastUtils.cosDeg(deg);
			double s = FastUtils.sinDeg(deg);
			if (hp > 25) {
				new PartialParticle(Particle.SOUL_FIRE_FLAME, pCenter.clone().add(2 * c, 0, 2 * s), 1, 0, 0, 0, 0).spawnAsEnemy();
				new PartialParticle(Particle.SOUL_FIRE_FLAME, pCenter.clone().add(2 * c, 2 * s, 0), 1, 0, 0, 0, 0).spawnAsEnemy();
				new PartialParticle(Particle.SOUL_FIRE_FLAME, pCenter.clone().add(0, 2 * s, 2 * c), 1, 0, 0, 0, 0).spawnAsEnemy();
			} else {
				new PartialParticle(Particle.FLAME, pCenter.clone().add(2 * c, 0, 2 * s), 1, 0, 0, 0, 0).spawnAsEnemy();
				new PartialParticle(Particle.FLAME, pCenter.clone().add(2 * c, 2 * s, 0), 1, 0, 0, 0, 0).spawnAsEnemy();
				new PartialParticle(Particle.FLAME, pCenter.clone().add(0, 2 * s, 2 * c), 1, 0, 0, 0, 0).spawnAsEnemy();
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

}
