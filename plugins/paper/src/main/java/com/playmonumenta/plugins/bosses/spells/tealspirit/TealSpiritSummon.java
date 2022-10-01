package com.playmonumenta.plugins.bosses.spells.tealspirit;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.parameters.LoSPool;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MMLog;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.checkerframework.checker.nullness.qual.Nullable;

public class TealSpiritSummon extends Spell {
	private static final double RADIUS = 25;
	private static final int COUNT = 5;
	private static final LoSPool NORMAL_POOL = new LoSPool("~TealNormalMobs");
	private static final LoSPool ELITE_POOL = new LoSPool("~TealEliteMobs");
	private static final double ELITE_CHANCE = 0.05;

	private final Location mCenter;
	private final int mCooldownTicks;

	private int mTimer = 0;

	public TealSpiritSummon(Location center, int cooldownTicks) {
		mCenter = center;
		mCooldownTicks = cooldownTicks;
	}

	@Override
	public void run() {
		mTimer += 5;
		if (mTimer % mCooldownTicks == 0) {
			World world = mCenter.getWorld();
			List<LivingEntity> summoned = new ArrayList<>();
			for (int i = 0; i < COUNT; i++) {
				Location loc = findSpawnLocation(0);

				if (loc == null) {
					MMLog.fine("Took too many attempts to find location to spawn mob in Teal Spirit Summon, aborting the spell.");
					break;
				}

				world.playSound(loc, Sound.BLOCK_BEACON_ACTIVATE, 0.5f, 0.4f);
				loc.subtract(0, 1, 0);

				LoSPool pool = FastUtils.RANDOM.nextDouble() < ELITE_CHANCE ? ELITE_POOL : NORMAL_POOL;
				Entity e = pool.spawn(loc);
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
					if (mT >= 40) {
						for (LivingEntity summon : summoned) {
							summon.setAI(true);
							summon.setInvulnerable(false);
							world.playSound(summon.getLocation(), Sound.ENTITY_PUFFER_FISH_BLOW_UP, 0.5f, 0.5f);
						}
						this.cancel();
						return;
					}

					for (LivingEntity summon : summoned) {
						Location summonLoc = summon.getLocation();
						summon.teleport(summonLoc.add(0, 0.05, 0));
						world.playSound(summonLoc, Sound.BLOCK_CALCITE_BREAK, 0.5f, 1.5f);
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
		Location loc = LocationUtils.randomLocationInCircle(mCenter, RADIUS);
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
		return 0;
	}
}
