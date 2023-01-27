package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.NotNull;

public class SpellSpawnMobs extends Spell {
	public static final int DEFAULT_MOB_CAP_RADIUS = 10;

	private final int mSummonRange;
	private final int mMinSummonRange;
	private final int mCooldownTicks;
	private final String mSummonName;
	private final int mSpawns;
	private final int mMobCap;
	private final int mMobCapRange;

	private final LivingEntity mBoss;

	public SpellSpawnMobs(LivingEntity boss, int spawns, String losname, int cooldown, int range, int minrange, int mobcap) {
		this(boss, spawns, losname, cooldown, range, minrange, mobcap, DEFAULT_MOB_CAP_RADIUS);
	}

	public SpellSpawnMobs(LivingEntity boss, int spawns, String losname, int cooldown, int range, int minrange, int mobcap, int mobcaprange) {
		mBoss = boss;
		mSummonRange = range;
		mMinSummonRange = minrange;
		mCooldownTicks = cooldown;
		mSummonName = losname;
		mSpawns = spawns;
		mMobCap = mobcap;
		mMobCapRange = mobcaprange;
	}

	@Override
	public void run() {
		Location loc = mBoss.getLocation();

		for (int i = 0; i < mSpawns; i++) {
			double r = FastUtils.randomDoubleInRange(mMinSummonRange, mSummonRange);
			double theta = FastUtils.randomDoubleInRange(0, 2 * Math.PI);

			double x = r * Math.cos(theta);
			double z = r * Math.sin(theta);

			Location sLoc = loc.clone().add(x, 0.25, z);
			//Can skip some summons but doesn't matter I don't think - this is the edgiest of edge cases
			if (ZoneUtils.hasZoneProperty(sLoc, ZoneUtils.ZoneProperty.RESIST_5)) {
				continue;
			}
			Entity entity = LibraryOfSoulsIntegration.summon(sLoc, mSummonName);
			if (entity != null) {
				summonPlugins(entity);
			}
		}
	}

	//overwrite this function to modify summon mobs with extra tags/stats
	public void summonPlugins(@NotNull Entity summon) {

	}

	@Override
	public boolean canRun() {
		if (EntityUtils.getNearbyMobs(mBoss.getLocation(), mMobCapRange).size() > mMobCap
				|| ZoneUtils.hasZoneProperty(mBoss.getLocation(), ZoneUtils.ZoneProperty.RESIST_5)) {
			return false;
		}
		return true;
	}

	@Override
	public int cooldownTicks() {
		return mCooldownTicks;
	}
}
