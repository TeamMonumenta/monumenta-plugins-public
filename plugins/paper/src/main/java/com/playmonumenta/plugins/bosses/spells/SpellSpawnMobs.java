package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;

public class SpellSpawnMobs extends Spell {
	public static final int DEFAULT_MOB_CAP_RADIUS = 10;

	private final double mSummonRange;
	private final double mMinSummonRange;
	private final int mCooldownTicks;
	private final String mSummonName;
	private final int mSpawns;
	private final int mMobCap;
	private final double mMobCapRange;
	private final boolean mCapMobsByName;
	private final String mMobCapName;

	private final LivingEntity mBoss;

	public SpellSpawnMobs(LivingEntity boss, int spawns, String losname, int cooldown, double range, double minrange, int mobcap) {
		this(boss, spawns, losname, cooldown, range, minrange, mobcap, DEFAULT_MOB_CAP_RADIUS, false, "");
	}

	public SpellSpawnMobs(LivingEntity boss, int spawns, String losname, int cooldown, double range, double minrange, int mobcap, double mobcaprange, boolean capmobsbyname, String mobcapname) {
		mBoss = boss;
		mSummonRange = range;
		mMinSummonRange = minrange;
		mCooldownTicks = cooldown;
		mSummonName = losname;
		mSpawns = spawns;
		mMobCap = mobcap;
		mMobCapRange = mobcaprange;
		mCapMobsByName = capmobsbyname;
		mMobCapName = mobcapname;
	}

	@Override
	public void run() {
		Location loc = mBoss.getLocation();

		for (int i = 0; i < mSpawns; i++) {
			Location sLoc = loc.clone();

			// try 20 times to find an open block
			for (int j = 0; j < 20; j++) {
				double r = FastUtils.randomDoubleInRange(mMinSummonRange, mSummonRange);
				double theta = FastUtils.randomDoubleInRange(0, 2 * Math.PI);

				double x = r * Math.cos(theta);
				double z = r * Math.sin(theta);

				Location tLoc = loc.clone().add(x, 0.25, z);

				if (mBoss.getWorld().getBlockAt(tLoc).isPassable()) {
					sLoc = tLoc.clone();
					break;
				}
			}

			// Skip summons in zones that don't allow them
			if (ZoneUtils.hasZoneProperty(sLoc, ZoneUtils.ZoneProperty.NO_SUMMONS)) {
				continue;
			}
			Entity entity = LibraryOfSoulsIntegration.summon(sLoc, mSummonName);
			if (entity != null) {
				summonPlugins(entity);
			}
		}
	}

	//overwrite this function to modify summon mobs with extra tags/stats
	public void summonPlugins(Entity summon) {

	}

	@Override
	public boolean canRun() {
		List<LivingEntity> nearbyMobs = EntityUtils.getNearbyMobs(mBoss.getLocation(), mMobCapRange);
		int mobCount = 0;
		for (LivingEntity mob : nearbyMobs) {
			if (mob.getName().equals(mMobCapName)) {
				mobCount++;
			}
		}

		if (mCapMobsByName) {
			if (mobCount >= mMobCap
				|| (ZoneUtils.hasZoneProperty(mBoss.getLocation(), ZoneUtils.ZoneProperty.NO_SUMMONS))) {
				return false;
			}
		} else {
			if (EntityUtils.getNearbyMobs(mBoss.getLocation(), mMobCapRange).size() > mMobCap
				|| (ZoneUtils.hasZoneProperty(mBoss.getLocation(), ZoneUtils.ZoneProperty.NO_SUMMONS))) {
				return false;
			}
		}
		return true;
	}

	@Override
	public int cooldownTicks() {
		return mCooldownTicks;
	}
}
