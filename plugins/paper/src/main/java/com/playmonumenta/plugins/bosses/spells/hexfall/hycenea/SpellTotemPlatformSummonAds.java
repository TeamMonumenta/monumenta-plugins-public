package com.playmonumenta.plugins.bosses.spells.hexfall.hycenea;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.hexfall.HyceneaRageOfTheWolf;
import com.playmonumenta.plugins.bosses.parameters.LoSPool;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MMLog;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class SpellTotemPlatformSummonAds extends Spell {
	private static final int MOB_CAP = 25;
	private static final LoSPool NORMAL_POOL = new LoSPool.LibraryPool("~HexfallHycenea");
	private final Location mCenterLoc;
	private final int mRadius;
	private final int mCooldown;
	private final int mCount;
	private final ArmorStand mCenterStand;

	public SpellTotemPlatformSummonAds(Location centerLoc, int radius, int cooldown, int count, ArmorStand centerStand) {
		mCenterLoc = centerLoc;
		mRadius = radius;
		mCooldown = cooldown;
		mCount = count;
		mCenterStand = centerStand;
	}

	@Override
	public void run() {
		if (EntityUtils.getNearbyMobs(mCenterStand.getLocation(), HyceneaRageOfTheWolf.detectionRange).size() >= MOB_CAP) {
			return;
		}

		World world = mCenterLoc.getWorld();
		List<LivingEntity> summoned = new ArrayList<>();
		for (int i = 0; i < mCount; i++) {
			Location loc = findSpawnLocation(0);

			if (loc == null) {
				MMLog.fine("Took too many attempts to find location to spawn mob in Totem Platform Summon, aborting the spell.");
				break;
			}

			world.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.HOSTILE, 0.5f, 0.4f);
			loc.subtract(0, 0.5, 0);

			Entity e = NORMAL_POOL.spawn(loc);
			if (e instanceof LivingEntity mob) {
				mob.setAI(false);
				mob.setInvulnerable(true);
				summoned.add(mob);
			}
		}

		new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {
				if (mT++ >= 20) {
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
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);

	}

	private @Nullable Location findSpawnLocation(int attempts) {
		if (attempts > 20) {
			return null;
		}
		Location loc = LocationUtils.randomLocationInDonut(mCenterLoc, 2, mRadius);
		int i = 0;
		while (loc.getBlock().isSolid() && i < 4) {
			loc.add(0, 1, 1);
			i++;
		}

		loc = LocationUtils.fallToGround(loc, mCenterLoc.getBlockY());

		if (loc.getBlock().isSolid()) {
			return findSpawnLocation(attempts + 1);
		} else {
			return loc;
		}
	}

	@Override
	public int cooldownTicks() {
		return mCooldown;
	}
}
