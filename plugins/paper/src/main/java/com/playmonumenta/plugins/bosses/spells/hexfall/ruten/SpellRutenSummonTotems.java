package com.playmonumenta.plugins.bosses.spells.hexfall.ruten;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.hexfall.Ruten;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MMLog;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class SpellRutenSummonTotems extends Spell {
	private static final int MOB_CAP = 25;
	private final Location mCenter;
	private final int mSummonCount;
	private final int mRange;
	private final int mCooldownTicks;
	private int mTimer = 0;
	private final Location mSpawnLoc;

	public SpellRutenSummonTotems(Location center, int range, int summonCount, int cooldownTicks, Location spawnLoc) {
		mSummonCount = summonCount;
		mRange = range;
		mCenter = center;
		mCooldownTicks = cooldownTicks;
		mSpawnLoc = spawnLoc;
	}

	@Override
	public void run() {
		mTimer++;
		if (mTimer % mCooldownTicks == 0) {
			if (EntityUtils.getNearbyMobs(mSpawnLoc, Ruten.arenaRadius).size() >= MOB_CAP) {
				return;
			}

			World world = mCenter.getWorld();
			List<LivingEntity> summoned = new ArrayList<>();
			for (int i = 0; i < mSummonCount; i++) {
				Location loc = findSpawnLocation(0);

				if (loc == null) {
					MMLog.fine("Took too many attempts to find location to spawn mob in Totem Golem Spirit Summon, aborting the spell.");
					break;
				}

				world.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.HOSTILE, 0.5f, 0.4f);
				loc.subtract(0, 0.5, 0);

				String summonString = "LifeGolem";

				Entity e = LibraryOfSoulsIntegration.summon(loc, summonString);
				if (e instanceof LivingEntity mob) {
					mob.setAI(false);
					summoned.add(mob);
				}
			}

			for (int i = 0; i < mSummonCount; i++) {
				Location loc = findSpawnLocation(0);

				if (loc == null) {
					MMLog.fine("Took too many attempts to find location to spawn mob in Teal Spirit Summon, aborting the spell.");
					break;
				}

				world.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.HOSTILE, 0.5f, 0.4f);
				loc.subtract(0, 0.5, 0);

				String summonString = "DeathGolem";

				Entity e = LibraryOfSoulsIntegration.summon(loc, summonString);
				if (e instanceof LivingEntity mob) {
					mob.setAI(false);
					summoned.add(mob);
				}
			}

			new BukkitRunnable() {
				int mT = 0;

				@Override
				public void run() {
					if (mT >= 40) {
						for (LivingEntity summon : summoned) {
							summon.setAI(true);
							summon.setInvulnerable(false);
							world.playSound(summon.getLocation(), Sound.ENTITY_PUFFER_FISH_BLOW_UP, SoundCategory.HOSTILE, 0.5f, 0.5f);
						}
						this.cancel();
						return;
					}

					for (LivingEntity summon : summoned) {
						Location summonLoc = summon.getLocation();
						summon.teleport(summonLoc.add(0, 0.05, 0));
						world.playSound(summonLoc, Sound.BLOCK_CALCITE_BREAK, SoundCategory.HOSTILE, 0.5f, 1.5f);
					}

					mT += 2;
				}
			}.runTaskTimer(Plugin.getInstance(), 0, 2);
		}
	}

	private @Nullable Location findSpawnLocation(int attempts) {
		if (attempts > 20) {
			return null;
		}
		Location loc = LocationUtils.randomLocationInCircle(mCenter, (double) mRange / 2);
		int i = 0;
		while (loc.getBlock().isSolid() && i < 4) {
			loc.add(0, 1, 1);
			i++;
		}

		loc = LocationUtils.fallToGround(loc, mCenter.getBlockY());

		if (loc.getBlock().isSolid()) {
			return findSpawnLocation(attempts + 1);
		} else {
			return loc;
		}
	}


	@Override
	public int cooldownTicks() {
		return mCooldownTicks;
	}
}
