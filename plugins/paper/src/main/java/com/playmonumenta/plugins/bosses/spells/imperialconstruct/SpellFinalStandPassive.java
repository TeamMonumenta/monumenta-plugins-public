package com.playmonumenta.plugins.bosses.spells.imperialconstruct;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SpellFinalStandPassive extends Spell {

	private static final int DAMAGE_PER_SECOND = 50;
	private int mTimer = 0;
	private int mInterval = 10;
	private int mMaxRadius = 30;

	private LivingEntity mBoss;
	private int mRadius;
	private Location mCenter;

	public SpellFinalStandPassive(LivingEntity boss, int radius, Location center) {
		mBoss = boss;
		mRadius = radius;
		mCenter = center;
	}

	@Override
	public void run() {

		if (mTimer >= mInterval) {
			mTimer = 0;
			World world = mBoss.getWorld();
			for (double deg = 0; deg < 360; deg += 3 * 2) {
				double cos = FastUtils.cosDeg(deg);
				double sin = FastUtils.sinDeg(deg);

				for (int x = mRadius; x < mMaxRadius; x += 3) {
					Location loc = mCenter.clone().add(cos * x, 0, sin * x);

					world.spawnParticle(Particle.SMOKE_NORMAL, loc, 1, 0.15, 0.15, 0.15, 0);
					if (deg % 4 == 0) {
						world.spawnParticle(Particle.BLOCK_DUST, loc, 1, 0.15, 0.1, 0.15, 0.75, Material.FIRE.createBlockData());
					} else {
						world.spawnParticle(Particle.BLOCK_DUST, loc, 1, 0.15, 0.1, 0.15, 0.75, Material.CRYING_OBSIDIAN.createBlockData());
					}

					if (deg % 30 == 0) {
						world.spawnParticle(Particle.LAVA, loc, 1, 0.15, 0.1, 0.15, 0.25);
					}

				}
			}

			for (Player p : PlayerUtils.playersInRange(mCenter, mMaxRadius, true)) {
				if (!PlayerUtils.playersInRange(mCenter, mRadius, true).contains(p)) {
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
