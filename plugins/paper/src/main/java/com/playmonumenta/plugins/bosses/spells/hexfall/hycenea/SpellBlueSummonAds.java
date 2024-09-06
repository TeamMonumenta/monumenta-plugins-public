package com.playmonumenta.plugins.bosses.spells.hexfall.hycenea;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.hexfall.BlueDamageIncreaseBoss;
import com.playmonumenta.plugins.bosses.bosses.hexfall.HarrakfarGodOfLife;
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
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Wolf;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class SpellBlueSummonAds extends Spell {
	private static final int MOB_CAP = 25;
	private static final LoSPool NORMAL_POOL = new LoSPool.LibraryPool("~HexfallHarrakfar");
	private final Location mCenterLoc;
	private final int mRadius;
	private final int mCooldown;
	private final int mCount;
	private final Plugin mPlugin;
	private int mTimer;

	public SpellBlueSummonAds(Plugin plugin, Location centerLoc, int radius, int cooldown, int count) {
		mCenterLoc = centerLoc;
		mRadius = radius;
		mCooldown = cooldown;
		mCount = count;
		mPlugin = plugin;
	}

	@Override
	public void run() {
		mTimer++;

		if (mTimer > mCooldown && mCenterLoc.getNearbyEntities(HarrakfarGodOfLife.detectionRange, HarrakfarGodOfLife.detectionRange, HarrakfarGodOfLife.detectionRange)
			.stream().filter(entity -> EntityUtils.isHostileMob(entity) || entity instanceof Wolf).toList().size() < MOB_CAP) {
			mTimer = 0;

			World world = mCenterLoc.getWorld();
			List<LivingEntity> summoned = new ArrayList<>();
			for (int i = 0; i < mCount; i++) {
				Location loc = findSpawnLocation(0);

				if (loc == null) {
					MMLog.fine("Took too many attempts to find location to spawn mob in Blue Summon, aborting the spell.");
					break;
				}

				world.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, SoundCategory.HOSTILE, 0.5f, 0.4f);
				loc.subtract(0, 0.5, 0);

				Entity e = NORMAL_POOL.spawn(loc);
				if (e instanceof LivingEntity mob) {
					mob.setAI(false);
					summoned.add(mob);
					mob.addScoreboardTag("boss_bluedamageincrease");
					mPlugin.mBossManager.manuallyRegisterBoss(mob, new BlueDamageIncreaseBoss(mPlugin, mob));
				}
			}

			new BukkitRunnable() {
				int mT = 0;

				@Override
				public void run() {
					if (mT >= 40) {
						for (LivingEntity summon : summoned) {
							summon.setAI(true);
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
