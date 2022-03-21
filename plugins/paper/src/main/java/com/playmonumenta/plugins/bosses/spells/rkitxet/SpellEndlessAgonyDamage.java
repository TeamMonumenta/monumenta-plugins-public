package com.playmonumenta.plugins.bosses.spells.rkitxet;

import com.playmonumenta.plugins.bosses.bosses.RKitxet;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SpellEndlessAgonyDamage extends Spell {
	private double RADIUS = SpellEndlessAgony.RADIUS;
	private static final double DAMAGE = 5;

	private ThreadLocalRandom mRand = ThreadLocalRandom.current();
	private RKitxet mRKitxet;
	private LivingEntity mBoss;
	private int mTicks;

	public SpellEndlessAgonyDamage(LivingEntity boss, RKitxet rKitxet) {
		mBoss = boss;
		mRKitxet = rKitxet;
		mTicks = 0;
	}

	@Override
	public void run() {
		//This function runs every 5 ticks
		mTicks += 5;

		List<Player> hitPlayers = new ArrayList<Player>();
		for (Location loc : mRKitxet.mAgonyLocations) {
			if (mTicks % 10 == 0) {
				for (Player p : PlayerUtils.playersInCylinder(loc, RADIUS, RADIUS)) {
					if (!hitPlayers.contains(p)) {
						DamageUtils.damage(mBoss, p, DamageType.MAGIC, DAMAGE, null, true, false, "Endless Agony");
						hitPlayers.add(p);
					}
				}
			}

			PPCircle indicator = new PPCircle(Particle.REDSTONE, loc, RADIUS).ringMode(true).count(20).delta(0.1, 0.05, 0.1).data(SpellEndlessAgony.ENDLESS_AGONY_COLOR);
			PPCircle indicator2 = new PPCircle(Particle.DRAGON_BREATH, loc, 0).ringMode(true).count(2).delta(0.25, 0.1, 0.25).extra(mRand.nextDouble(0.01, 0.05));
			PPCircle indicator3 = new PPCircle(Particle.REDSTONE, loc.clone().add(0, 0.5, 0), RADIUS).ringMode(true).count(8).delta(0.1, 0.05, 0.1).data(SpellEndlessAgony.ENDLESS_AGONY_COLOR);

			indicator.spawnAsBoss();
			indicator3.spawnAsBoss();
			for (double r = 1; r < RADIUS; r++) {
				indicator2.radius(r).location(loc).spawnAsBoss();
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
