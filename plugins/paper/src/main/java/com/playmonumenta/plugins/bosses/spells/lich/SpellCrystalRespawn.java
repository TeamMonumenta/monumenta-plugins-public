package com.playmonumenta.plugins.bosses.spells.lich;

import com.playmonumenta.plugins.bosses.bosses.Lich;
import com.playmonumenta.plugins.bosses.spells.Spell;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellCrystalRespawn extends Spell {
	private final Plugin mPlugin;
	private final Lich mLich;
	private int mT;
	private final Location mCenter;
	private final double mRange;
	private final List<Location> mLoc;
	private final String mCrystalNBT;
	private boolean mTrigger = false;
	private List<Player> mPlayers = new ArrayList<>();
	private static boolean mSpawned = false;

	public SpellCrystalRespawn(Plugin plugin, Lich lich, Location loc, double range, List<Location> crystalLoc, String crystalNBT) {
		mPlugin = plugin;
		mLich = lich;
		mCenter = loc;
		mRange = range;
		mLoc = crystalLoc;
		mCrystalNBT = crystalNBT;
	}

	@Override
	public void run() {
		// Calculate cooldown
		final int minCooldown = 20 * 10;
		final int maxCooldown = 20 * 30;
		double factor = Math.log(mPlayers.size());
		int spellCooldown = (int) Math.max(minCooldown, Math.round(maxCooldown / factor));

		// Update player count every 5 seconds
		if (!mTrigger) {
			mPlayers = Lich.playersInRange(mCenter, mRange, true);
			mTrigger = true;
			new BukkitRunnable() {

				@Override
				public void run() {
					mTrigger = false;
				}

			}.runTaskLater(mPlugin, 20 * 5);
		}

		// If off cooldown and not running Dies Irae, spawn crystals
		mT -= 5;
		if (mT <= 0 && !mLich.hasRunningSpellOfType(SpellDiesIrae.class)) {
			mT = spellCooldown;
			Lich.spawnCrystal(mLoc, Math.min(5, mPlayers.size() / 5), mCrystalNBT);
			mSpawned = true;
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> mSpawned = false, 2 * 20);
		}
	}

	public static boolean getmSpawned() {
		return mSpawned;
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

}
