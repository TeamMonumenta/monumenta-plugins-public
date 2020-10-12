package com.playmonumenta.plugins.bosses.spells;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellBaseSummon extends Spell {
	@FunctionalInterface
	public interface GetTargetPlayers {
		/**
		 * Given the location of a boss, get which players should be targeted by summon
		 */
		List<Player> run();
	}

	@FunctionalInterface
	public interface SummonMobAt {
		/**
		 * Given a valid location and a player to target, summon the mob
		 *
		 * Must return the bukkit runnable spawned for cancellation purposes, if any
		 *
		 * Might want to also override the cancel method here to do something to the mob
		 * if gets cancelled
		 *
		 * Note this needs to always explicitly call cancel() when done to be cleaned up correctly.
		 */
		BukkitRunnable run(Location loc, Player player);
	}

	@FunctionalInterface
	public interface SummonerAnimation {
		/**
		 * Plays an animation around the summoner about spawning
		 *
		 * Might want to also override the cancel method here to do something to the mob
		 * if gets cancelled
		 *
		 * Note this needs to always explicitly call cancel() when done to be cleaned up correctly.
		 */
		BukkitRunnable run();
	}

	@FunctionalInterface
	public interface CanRun {
		/**
		 * Used to determine if summoning is allowed
		 */
		boolean check();
	}

	private final Plugin mPlugin;
	private final int mCastTime;
	private final int mDuration;
	private final int mSpawnsPerPlayer;
	private final boolean mStopWhenHit;
	private final GetTargetPlayers mGetPlayers;
	private final SummonMobAt mSummon;
	private final SummonerAnimation mAnimation;
	private final CanRun mCanRun;
	private final List<Vector> mLocationOffsets;

	public SpellBaseSummon(Plugin plugin, int castTime, int duration, int rangeFromPlayer,
	                       int spawnsPerPlayer, boolean stopWhenHit, GetTargetPlayers getPlayers,
	                       SummonMobAt summon, SummonerAnimation animation, CanRun canRun) {
		mPlugin = plugin;
		mCastTime = castTime;
		mDuration = duration;
		mSpawnsPerPlayer = spawnsPerPlayer;
		mStopWhenHit = stopWhenHit;
		mGetPlayers = getPlayers;
		mSummon = summon;
		mAnimation = animation;
		mCanRun = canRun;

		// Calculate a reference list of offsets to randomly try when spawning mobs
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

	public void run() {
		List<Player> players = mGetPlayers.run();

		// Allow garbage collection of runnables that are no longer running
		Iterator<BukkitRunnable> iterator = mActiveRunnables.iterator();
		while (iterator.hasNext()) {
			BukkitRunnable runnable = iterator.next();
			if (runnable.isCancelled()) {
				iterator.remove();
			} else {
				mPlugin.getLogger().warning("Summon process restarted but some previous summons still running!");
			}
		}

		BukkitRunnable runnable = mAnimation.run();
		if (runnable != null) {
			mActiveRunnables.add(runnable);
		}

		// Shuffle the list once per run - all players will use same shuffled list
		Collections.shuffle(mLocationOffsets);
		for (Player player : players) {
			int numSummoned = 0;
			for (Vector offset : mLocationOffsets) {
				Location loc = player.getLocation().add(offset);

				// Underneath block must be solid
				if (!loc.subtract(0, 1, 0).getBlock().getType().isSolid()) {
					continue;
				}

				// Blocks above summon-on block must be not solid
				if (loc.add(0, 1, 0).getBlock().getType().isSolid() || loc.add(0, 1, 0).getBlock().getType().isSolid()) {
					continue;
				}

				// Summon the mob
				runnable = mSummon.run(loc, player);
				if (runnable != null) {
					mActiveRunnables.add(runnable);
				}

				// Stop once the right number of mobs have been summoned for this player
				numSummoned++;
				if (numSummoned >= mSpawnsPerPlayer) {
					break;
				}
			}
		}
	}

	@Override
	public int castTime() {
		return mCastTime;
	}

	public int duration() {
		return mDuration;
	}

	@Override
	public boolean canRun() {
		return mCanRun.check();
	}

	@Override
	public void bossDamagedByEntity(EntityDamageByEntityEvent event) {
		if (mStopWhenHit) {
			// Cancel all active runnables
			cancel();
		}
	}
}
