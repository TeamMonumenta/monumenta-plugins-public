package com.playmonumenta.plugins.bosses.spells.bluestrike;

import com.playmonumenta.libraryofsouls.Soul;
import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.bosses.bluestrike.DropShardBoss;
import com.playmonumenta.plugins.bosses.parameters.LoSPool;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

public class SpellSummonBlueStrike extends Spell {
	private final LivingEntity mBoss;
	private final HashMap<Double, LoSPool> mWeights;
	private final int mTimeBetween;
	private final int mMobsPer;
	private int mTimer;
	private final Location mCenter;
	private final ArrayList<Vector> mLocationOffsets;
	private static final int BASE_SPAWNS = 1;
	private final HashMap<LoSPool, ArrayList<Soul>> mPoolMap = new HashMap<>();
	private final Plugin mPlugin;

	public SpellSummonBlueStrike(Plugin plugin, LivingEntity boss, Location center, HashMap<Double, LoSPool> weights, int timeBetween,
								 int innerCircleR, int outerCircleR) {
		mBoss = boss;
		mWeights = weights;
		mTimeBetween = timeBetween;
		mTimer = 3 * 4; // Init first summon cast to be in 3 seconds.
		mMobsPer = 1;
		mCenter = center;
		mPlugin = plugin;
		mLocationOffsets = new ArrayList<>();
		for (LoSPool p : weights.values()) {
			mPoolMap.put(p, new ArrayList<>(LibraryOfSoulsIntegration.getPool(p.toString()).keySet()));
		}
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


	@Override
	public void run() {
		mTimer--;
		if (mTimer < 0 && countDropShardMobs() < 25) {
			mTimer = mTimeBetween;
			mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, 1.0f, 1.0f);
			PartialParticle particles = new PartialParticle(Particle.SPELL_WITCH, mBoss.getLocation(), 50, 1, 1, 1);
			particles.spawnAsEnemy();
			BossManager bossManager = com.playmonumenta.plugins.Plugin.getInstance().mBossManager;

			// Run summon after a second of playing the sound.
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
				int spawns = mMobsPer * PlayerUtils.playersInRange(mCenter, 100, true).size() + BASE_SPAWNS;
				ArrayList<Double> keys = new ArrayList<>(mWeights.keySet());
				Collections.sort(keys);
				double totalWeight = keys.stream().mapToDouble(s -> s).sum();
				for (int i = 0; i < spawns; i++) {
					double target = FastUtils.RANDOM.nextDouble(totalWeight);
					int iteration = 0;
					while (target > keys.get(iteration)) {
						target -= keys.get(iteration);
						iteration++;
					}

					Collections.shuffle(mLocationOffsets);
					LivingEntity e = (LivingEntity) mWeights.get(keys.get(iteration)).spawn(mCenter.clone().add(mLocationOffsets.get(FastUtils.RANDOM.nextInt(mLocationOffsets.size()))));

					if (e != null) {
						bossManager.manuallyRegisterBoss(e, new DropShardBoss(mPlugin, e));
					}
				}
			}, 20);
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

	public int countDropShardMobs() {
		// Look for mobs with the DropShardTag
		List<LivingEntity> livingEntities = EntityUtils.getNearbyMobs(mBoss.getLocation(), 100);
		int count = 0;
		for (LivingEntity e : livingEntities) {
			Set<String> tags = e.getScoreboardTags();
			if (tags.contains(DropShardBoss.identityTag)) {
				count++;
			}
		}
		return count;
	}
}
