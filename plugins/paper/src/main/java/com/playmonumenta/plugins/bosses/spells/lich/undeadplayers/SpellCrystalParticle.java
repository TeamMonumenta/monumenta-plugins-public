package com.playmonumenta.plugins.bosses.spells.lich.undeadplayers;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;

public class SpellCrystalParticle extends Spell {

	private LivingEntity mBoss;
	private Location mSpawnLoc;
	private PartialParticle mSoul;
	private PartialParticle mFlame;

	public SpellCrystalParticle(LivingEntity boss, Location spawn) {
		mBoss = boss;
		mSpawnLoc = spawn;
		mSoul = new PartialParticle(Particle.SOUL_FIRE_FLAME, mBoss.getLocation(), 1, 0, 0, 0, 0);
		mFlame = new PartialParticle(Particle.FLAME, mBoss.getLocation(), 1, 0, 0, 0, 0);
	}

	@Override
	public void run() {
		mBoss.teleport(mSpawnLoc);
		double hp = mBoss.getHealth();
		Location pCenter = mBoss.getLocation().add(0, 2, 0);
		for (double deg = 0; deg < 360; deg += 36) {
			double c = FastUtils.cosDeg(deg);
			double s = FastUtils.sinDeg(deg);
			if (hp > 25) {
				mSoul.location(pCenter.clone().add(2 * c, 0, 2 * s)).spawnAsEnemy();
				mSoul.location(pCenter.clone().add(2 * c, 2 * s, 0)).spawnAsEnemy();
				mSoul.location(pCenter.clone().add(0, 2 * s, 2 * c)).spawnAsEnemy();
			} else {
				mFlame.location(pCenter.clone().add(2 * c, 0, 2 * s)).spawnAsEnemy();
				mFlame.location(pCenter.clone().add(2 * c, 2 * s, 0)).spawnAsEnemy();
				mFlame.location(pCenter.clone().add(0, 2 * s, 2 * c)).spawnAsEnemy();
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

}
