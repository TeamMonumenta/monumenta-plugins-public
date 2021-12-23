package com.playmonumenta.plugins.bosses.spells.lich;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.bosses.bosses.Lich;
import com.playmonumenta.plugins.bosses.spells.Spell;

public class SpellCrystalRespawn extends Spell {

	private Plugin mPlugin;
	private Lich mLich;
	private int mT;
	private double mInc;
	private int mMinCooldown = 20 * 10;
	private int mMaxCooldown = 20 * 30;
	private int mCooldown;
	private Location mCenter;
	private double mRange;
	private List<Location> mLoc;
	private String mCrystalNBT;
	private boolean mTrigger = false;
	private List<Player> mPlayers = new ArrayList<Player>();

	public SpellCrystalRespawn(Plugin plugin, Lich lich, Location loc, double range, List<Location> crystalLoc, String crystalnbt) {
		mPlugin = plugin;
		mLich = lich;
		mCenter = loc;
		mRange = range;
		mLoc = crystalLoc;
		mCrystalNBT = crystalnbt;
	}

	@Override
	public void run() {
		//update player count every 5 seconds
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

		//cooldown
		double factor = Math.log(mPlayers.size());
		mCooldown = (int) Math.max(mMinCooldown, Math.round(mMaxCooldown / factor));
		mT -= 5;
		if (mT <= 0 && !mLich.hasRunningSpellOfType(SpellDiesIrae.class)) {
			mT = mCooldown;
			mInc = Math.min(5, mPlayers.size() / 5);
			Lich.spawnCrystal(mLoc, mInc, mCrystalNBT);
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

}
