package com.playmonumenta.plugins.bosses.spells.imperialconstruct;

import com.playmonumenta.plugins.bosses.bosses.ImperialConstruct;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SpellFinalStandPassive extends Spell {

	private static final int DAMAGE_PER_SECOND = 50;
	private int mTimer = 0;
	private final int mInterval = 10;
	private final int mMaxRadius = 30;

	private final LivingEntity mBoss;
	private final ImperialConstruct mConstruct;
	private final int mRadius;
	private final Location mCenter;

	public SpellFinalStandPassive(LivingEntity boss, ImperialConstruct construct, int radius, Location center) {
		mBoss = boss;
		mConstruct = construct;
		mRadius = radius;
		mCenter = center;
	}

	@Override
	public void run() {

		if (mTimer >= mInterval) {
			mTimer = 0;
			for (double deg = 0; deg < 360; deg += 3 * 2) {
				double cos = FastUtils.cosDeg(deg);
				double sin = FastUtils.sinDeg(deg);

				for (int x = mRadius; x < mMaxRadius; x += 3) {
					Location loc = mCenter.clone().add(cos * x, 0, sin * x);

					new PartialParticle(Particle.SMOKE_NORMAL, loc, 1, 0.15, 0.15, 0.15, 0).spawnAsEntityActive(mBoss);
					if (deg % 4 == 0) {
						new PartialParticle(Particle.BLOCK_CRACK, loc, 1, 0.15, 0.1, 0.15, 0.75, Material.FIRE.createBlockData()).spawnAsEntityActive(mBoss);
					} else {
						new PartialParticle(Particle.BLOCK_CRACK, loc, 1, 0.15, 0.1, 0.15, 0.75, Material.CRYING_OBSIDIAN.createBlockData()).spawnAsEntityActive(mBoss);
					}

					if (deg % 30 == 0) {
						new PartialParticle(Particle.LAVA, loc, 1, 0.15, 0.1, 0.15, 0.25).spawnAsEntityActive(mBoss);
					}

				}
			}

			for (Player p : mConstruct.getArenaPlayers()) {
				if (mCenter.distanceSquared(p.getLocation()) > mRadius * mRadius) {
					DamageUtils.damage(mBoss, p, DamageEvent.DamageType.MAGIC, DAMAGE_PER_SECOND, null, true, true, "Instability");
					MovementUtils.knockAway(mCenter, p, 0, .25f, false);
				}
			}
		}

		mTimer += 2;
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
