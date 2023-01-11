package com.playmonumenta.plugins.bosses.spells.bluestrike;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.particle.PartialParticle;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

public class SpellCraftDaggerAnimation extends Spell {

	private final Location mDaggerLoc;
	private LivingEntity mBoss;

	private final Particle PARTICLE = Particle.FIREWORKS_SPARK;

	private List<Location> mParticleLocationList;

	private int mTicks; // Animation Ticks.

	public SpellCraftDaggerAnimation(LivingEntity boss, Location daggerLoc) {
		mDaggerLoc = daggerLoc;
		mBoss = boss;
		mTicks = 0;

		mParticleLocationList = new ArrayList<>();
	}

	@Override
	public void run() {
		// Every 2 seconds or so, spawn a new particle.
		if (mTicks % 10 == 0) {
			mParticleLocationList.add(mBoss.getLocation().add(0, 1, 0));
			mBoss.getWorld().playSound(mBoss.getLocation(), Sound.BLOCK_BEACON_AMBIENT, SoundCategory.HOSTILE, 1, 1);
		}

		// For every location:
		// Increment towards mDaggerLoc by a small distance.
		// Spawn PartialParticle.
		if (mTicks % 2 == 0) {
			for (Location loc : mParticleLocationList) {
				Vector daggerVector = mDaggerLoc.toVector();
				Vector particleVector = loc.toVector();

				Vector direction = daggerVector.subtract(particleVector).normalize();
				loc.add(direction);

				PartialParticle partialParticle = new PartialParticle(PARTICLE, loc, 1);
				partialParticle.spawnAsEnemy();
			}
			mParticleLocationList.removeIf(loc -> loc.distance(mDaggerLoc) < 2);
		}

		mTicks++;
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
