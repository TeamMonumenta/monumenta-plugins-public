package com.playmonumenta.plugins.bosses.spells.varcosamist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.bosses.VarcosaSummonedMob;
import com.playmonumenta.plugins.bosses.bosses.VarcosaSummonerBoss;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class SpellSummonConstantly extends Spell {
	private final List<String> mMobNames;
	private final int mDuration;
	private int mTimer;
	private final int mRadius;
	private final int mBaseSpawns;
	private final Location mCenter;
	private final List<Vector> mLocationOffsets;
	private final BossAbilityGroup mSummoner;

	/*
	 * summoner = the instance of the boss ability that cast this spell
	 */
	public SpellSummonConstantly(List<String> mobNames, int duration, int radius, int rangeFromArmorStand, int baseSpawns,
	                             Location center, BossAbilityGroup summoner) {
		mMobNames = mobNames;
		mDuration = duration;
		mTimer = mDuration / 3;
		mCenter = center;
		mRadius = radius;
		mBaseSpawns = baseSpawns;
		mSummoner = summoner;

		mLocationOffsets = new ArrayList<>();
		for (int x = -rangeFromArmorStand; x <= rangeFromArmorStand; x++) {
			for (int y = -2; y <= 2; y++) {
				for (int z = -rangeFromArmorStand; z <= rangeFromArmorStand; z++) {
					mLocationOffsets.add(new Vector(x + 0.5, y, z + 0.5));
				}
			}
		}
	}

	@Override
	public void run() {
		mTimer--;
		if (mTimer < 0) {
			for (Player player : mCenter.getNearbyPlayers(50)) {
				player.playSound(player.getLocation(), Sound.ENTITY_EVOKER_PREPARE_SUMMON, SoundCategory.HOSTILE, 1, 1);
			}

			//Hopefully shouldn't break - the idea is that it will refresh how many mobs per player after each death, but it will be based upon a final, unchanging number mBaseSpawns. Also it only changes it when the spell is cast
			int spawnsPerArmorStand = BossUtils.getPlayersInRangeForHealthScaling(mCenter, 50) - 1 + mBaseSpawns;

			mTimer = mDuration;
			Collection<ArmorStand> stands = mCenter.getNearbyEntitiesByType(ArmorStand.class, mRadius);

			// Shuffle the list once per run - all players will use same shuffled list
			Collections.shuffle(mLocationOffsets);
			for (ArmorStand armorStand : stands) {
				if (!armorStand.getScoreboardTags().contains("summon_constantly_stand")
					    && !armorStand.getScoreboardTags().contains("varcosa_center")) {
					continue;
				}
				int numSummoned = 0;
				for (Vector offset : mLocationOffsets) {
					Location loc = armorStand.getLocation().add(offset);

					// Underneath block must be solid
					if (!loc.clone().add(0, -1, 0).getBlock().isSolid()) {
						continue;
					}

					// Blocks above summon-on block must be not solid
					if (loc.getBlock().isSolid()
						    || loc.clone().add(0, 1, 0).getBlock().isSolid()) {
						continue;
					}

					// must not be close to a player
					if (!PlayerUtils.playersInRange(loc, 3, true).isEmpty()) {
						continue;
					}

					// Summon the mob
					int rand = FastUtils.RANDOM.nextInt(mMobNames.size());
					try {
						LivingEntity summoned = (LivingEntity) LibraryOfSoulsIntegration.summon(loc, mMobNames.get(rand));
						if (summoned != null && mSummoner instanceof VarcosaSummonerBoss) {
							// If this is the first phase of varcosa mob spawning, add a fake boss ability to this mob so the summone is damaged when it dies
							BossManager.getInstance().manuallyRegisterBoss(summoned, new VarcosaSummonedMob(Plugin.getInstance(), summoned, (VarcosaSummonerBoss) mSummoner));
						}
					} catch (Exception e) {
						e.printStackTrace();
					}

					// Stop once the right number of mobs have been summoned for this armor stand
					numSummoned++;
					if (numSummoned >= spawnsPerArmorStand) {
						break;
					}
				}
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
