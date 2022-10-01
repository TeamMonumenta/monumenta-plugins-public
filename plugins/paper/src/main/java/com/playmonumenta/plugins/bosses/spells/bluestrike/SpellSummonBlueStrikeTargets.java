package com.playmonumenta.plugins.bosses.spells.bluestrike;

import com.playmonumenta.plugins.bosses.bosses.bluestrike.BlueStrikeTargetNPCBoss;
import com.playmonumenta.plugins.bosses.bosses.bluestrike.BlueStrikeTurretBoss;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

public class SpellSummonBlueStrikeTargets extends Spell {
	private final LivingEntity mBoss;
	private final int mTimeBetween;
	private final int mMobsPer;
	private int mTimer;
	private final Location mCenter;
	private final ArrayList<Vector> mLocationOffsets;
	private static final int BASE_SPAWNS = 1;
	private final List<Vector> mShulkerSpots;

	public SpellSummonBlueStrikeTargets(LivingEntity boss, Location center, int timeBetween, int mobsPer,
										int innerCircleR, int outerCircleR) {
		mBoss = boss;
		mTimeBetween = timeBetween;
		mTimer = 0; // Init first summon cast to be immediate.
		mMobsPer = mobsPer;
		mCenter = center;
		mLocationOffsets = new ArrayList<>();
		mShulkerSpots = Arrays.asList(
			new Vector(0, 10, -24),
			new Vector(0, 10, 24),
			new Vector(24, 10, 0),
			new Vector(-24, 10, 0)
		);

		int y = 1;
		for (int x = -outerCircleR; x <= outerCircleR; x++) {
			for (int z = -outerCircleR; z <= outerCircleR; z++) {
				if ((x * x) + (z * z) > outerCircleR * outerCircleR || (x * x) + (z * z) < innerCircleR * innerCircleR) {
					continue;
				}
				mLocationOffsets.add(new Vector(x + 0.5, y, z + 0.5));
			}
		}
	}

	@Override public void run() {
		mTimer--;
		if (mTimer < 0) {
			mTimer = mTimeBetween;
			mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_GHAST_SCREAM, 1.0f, 0.8f);
			int spawns = mMobsPer * PlayerUtils.playersInRange(mCenter, 100, true).size() + BASE_SPAWNS;

			if (EntityUtils.getNearbyMobs(mCenter, 100, EnumSet.of(EntityType.SHULKER)).size() == 0) {
				double rand = FastUtils.randomDoubleInRange(0, 1.0);
				if (rand > 0.5) {
					spawns -= 1;
					int randIndex = (int) Math.floor(FastUtils.randomDoubleInRange(0, 3.9999));
					Location shulkerLoc = mCenter.clone().add(mShulkerSpots.get(randIndex));
					summonShulker(shulkerLoc);
				}
			}

			for (int i = 0; i < spawns; i++) {
				Collections.shuffle(mLocationOffsets);
				Location randLoc = mCenter.clone().add(mLocationOffsets.get(FastUtils.RANDOM.nextInt(mLocationOffsets.size())));
				summonZombie(randLoc);
			}
		}
	}

	@Override public int cooldownTicks() {
		return 0;
	}

	public void summonZombie(Location loc) {
		LibraryOfSoulsIntegration.summon(loc, "MaskedAssassin");
	}

	public void summonShulker(Location loc) {
		LibraryOfSoulsIntegration.summon(loc, "ShadowTurret");
	}

	public boolean checkTargetMobs() {
		// Look for mobs with the Target Tags
		List<LivingEntity> livingEntities = EntityUtils.getNearbyMobs(mBoss.getLocation(), 100);
		for (LivingEntity e : livingEntities) {
			Set<String> tags = e.getScoreboardTags();
			if (tags.contains(BlueStrikeTargetNPCBoss.identityTag)
				|| tags.contains(BlueStrikeTurretBoss.identityTag)) {
				return true;
			}
		}
		return false;
	}
}
