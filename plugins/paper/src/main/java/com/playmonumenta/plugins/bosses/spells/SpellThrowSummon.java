package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.List;
import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellThrowSummon extends Spell {

	private final int mMobCapRange;
	private final int mMobCap;
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final EntityTargets mTargets;
	private final int mLobs;
	private final int mCooldown;
	private final String mSummonName;
	private final boolean mFromPool;
	private final int mLobDelay;
	private final double mHeightOffset;
	private final float mYVelocity;
	private final double mThrowVariance;
	private final double mThrowYVariance;
	private final double mDistanceScalar;
	private final ParticlesList mThrowParticle;
	private final SoundsList mThrowSound;

	public SpellThrowSummon(Plugin plugin, LivingEntity boss, EntityTargets targets, int lobs, int cooldownTicks,
							String summonName, boolean fromPool, int lobDelay, double heightOffset, float yVelocity,
							double variance, double yVariance, double distanceScalar,
							int mobCapRange, int mobCap,
							ParticlesList particles, SoundsList sounds) {
		mPlugin = plugin;
		mBoss = boss;

		mTargets = targets;
		mLobs = lobs;
		mCooldown = cooldownTicks;
		mSummonName = summonName;
		mFromPool = fromPool;
		mLobDelay = lobDelay;
		mHeightOffset = heightOffset;
		mYVelocity = yVelocity;
		mThrowVariance = variance;
		mThrowYVariance = yVariance;
		mDistanceScalar = distanceScalar;
		mMobCapRange = mobCapRange;
		mMobCap = mobCap;

		mThrowParticle = particles;
		mThrowSound = sounds;
	}

	@Override
	public void run() {
		BukkitRunnable task = new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				mTicks++;

				for (LivingEntity entity : mTargets.getTargetsList(mBoss)) {
					launch(entity);
				}
				if (mTicks >= mLobs) {
					this.cancel();
				}
			}

		};

		task.runTaskTimer(mPlugin, 0, mLobDelay);
		mActiveRunnables.add(task);
	}

	@Override
	public int cooldownTicks() {
		return mCooldown;
	}

	public void launch(LivingEntity target) {
		Location sLoc = mBoss.getEyeLocation();

		if (mHeightOffset != 0) {
			sLoc.add(0, mHeightOffset, 0);
		}

		mThrowParticle.spawn(mBoss, sLoc);
		mThrowSound.play(sLoc);

		try {
			String soul = mSummonName;
			if (mFromPool) {
				List<String> souls = LibraryOfSoulsIntegration.getPool(mSummonName).keySet().stream().map((x) -> x.getLabel()).toList();
				soul = souls.get(FastUtils.RANDOM.nextInt(souls.size()));
			}
			Entity e = Objects.requireNonNull(LibraryOfSoulsIntegration.summon(sLoc, soul));

			Location pLoc = target.getLocation();
			Location tLoc = e.getLocation();

			if (mThrowVariance != 0) {
				double r = FastUtils.randomDoubleInRange(0, mThrowVariance);
				double theta = FastUtils.randomDoubleInRange(0, 2 * Math.PI);
				double x = r * Math.cos(theta);
				double z = r * Math.sin(theta);
				tLoc.add(x, 0, z);
			}

			double yVariance = mYVelocity;
			if (mThrowYVariance != 0) {
				double delta = FastUtils.randomDoubleInRange(-mThrowYVariance, mThrowYVariance);
				yVariance += delta;
			}

			Vector vect = new Vector(pLoc.getX() - tLoc.getX(), 0, pLoc.getZ() - tLoc.getZ());
			vect.normalize();
			if (!Double.isFinite(vect.getX())) {
				vect.setX(0);
				vect.setY(tLoc.getY() <= pLoc.getY() ? 1 : -1);
				vect.setZ(0);
			}
			vect.multiply(pLoc.distance(tLoc) * (1 + 0.5 * (0.7f - yVariance)) * mDistanceScalar / 10).setY(yVariance);
			e.setVelocity(vect);

		} catch (Exception e) {
			mPlugin.getLogger().warning("Failed to summon entity for throw summon: " + e.getMessage());
			e.printStackTrace();
		}
	}

	@Override
	public boolean canRun() {
		if (EntityUtils.getNearbyMobs(mBoss.getLocation(), mMobCapRange).size() > mMobCap) {
			return false;
		}
		return true;
	}
}
