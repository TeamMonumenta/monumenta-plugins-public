package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

public class SpellSpawnMobs extends Spell {
	public static final int MOB_DETECTION_RADIUS = 10;
	public static final int MOB_CAP = 15;

	private final int mSummonRange;
	private final int mCooldownTicks;
	private final String mSummonName;
	private final int mSpawns;

	private final LivingEntity mBoss;

	public SpellSpawnMobs(LivingEntity boss, int spawns, String losname, int cooldown, int range) {
		mBoss = boss;
		mSummonRange = range;
		mCooldownTicks = cooldown;
		mSummonName = losname;
		mSpawns = spawns;
	}

	@Override
	public void run() {
		Location loc = mBoss.getLocation();
		for (int i = 0; i < mSpawns; i++) {
			double x = FastUtils.randomDoubleInRange(-mSummonRange, mSummonRange);
			double z = FastUtils.randomDoubleInRange(-mSummonRange, mSummonRange);
			Location sLoc = loc.clone().add(x, 0.25, z);
			LibraryOfSoulsIntegration.summon(sLoc, mSummonName);
		}
	}

	@Override
	public boolean canRun() {
		if (EntityUtils.getNearbyMobs(mBoss.getLocation(), MOB_DETECTION_RADIUS).size() > MOB_CAP) {
			return false;
		}
		return true;
	}

	@Override
	public int cooldownTicks() {
		return mCooldownTicks;
	}
}
