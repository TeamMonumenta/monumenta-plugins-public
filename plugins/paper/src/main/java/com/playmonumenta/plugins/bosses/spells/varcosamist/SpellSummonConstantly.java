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
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

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
	public SpellSummonConstantly(List<String> mobNames, int duration, int radius, int rangeFromPlayer, int spawnsPerPlayer,
	                             Location center, BossAbilityGroup summoner) {
		mMobNames = mobNames;
		mDuration = duration;
		mTimer = mDuration / 3;
		mCenter = center;
		mRadius = radius;
		mBaseSpawns = spawnsPerPlayer;
		mSummoner = summoner;

		mLocationOffsets = new ArrayList<Vector>();
		for (int y = -rangeFromPlayer / 3; y <= rangeFromPlayer / 3; y++) {
			for (int x = -rangeFromPlayer; x <= rangeFromPlayer; x++) {
				for (int z = -6; z <= 6; z++) {
					// Don't spawn very close to the player - no fun
					if (x > -4 && x < 4 && z > -4 && z < 4) {
						continue;
					}
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
			int spawnsPerPlayer = BossUtils.getPlayersInRangeForHealthScaling(mCenter, 50) - 1 + mBaseSpawns;

			mTimer = mDuration;
			Collection<ArmorStand> stands = mCenter.getNearbyEntitiesByType(ArmorStand.class, mRadius);

			// Shuffle the list once per run - all players will use same shuffled list
			Collections.shuffle(mLocationOffsets);
			for (ArmorStand armorStand : stands) {
				int numSummoned = 0;
				for (Vector offset : mLocationOffsets) {
					Location loc = armorStand.getLocation().add(offset).add(0, 1, 0);

					// Underneath block must be solid
					if (!loc.subtract(0, 1, 0).getBlock().getType().isSolid()) {
						continue;
					}

					// Blocks above summon-on block must be not solid
					if (loc.add(0, 1, 0).getBlock().getType().isSolid() || loc.add(0, 1, 0).getBlock().getType().isSolid()) {
						continue;
					}

					// Summon the mob
					int rand = FastUtils.RANDOM.nextInt(mMobNames.size());
					try {
						LivingEntity summoned = (LivingEntity)LibraryOfSoulsIntegration.summon(loc, mMobNames.get(rand));
						if (summoned != null && mSummoner instanceof VarcosaSummonerBoss) {
							// If this is the first phase of varcosa mob spawning, add a fake boss ability to this mob so the summone is damaged when it dies
							BossManager.getInstance().manuallyRegisterBoss(summoned, new VarcosaSummonedMob(Plugin.getInstance(), summoned, (VarcosaSummonerBoss)mSummoner));
						}
					} catch (Exception e) {
						e.printStackTrace();
					}


					// Stop once the right number of mobs have been summoned for this player
					numSummoned++;
					if (numSummoned >= spawnsPerPlayer) {
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
