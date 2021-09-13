package com.playmonumenta.plugins.bosses.spells.lich;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.bosses.bosses.Lich;
import com.playmonumenta.plugins.bosses.spells.Spell;

public class SpellFinalSwarm extends Spell {
	private Plugin mPlugin;
	private double mT;
	private double mCount;
	private int mSoloCooldown = 20 * 9;
	private int mMinSpawnCount = 2;
	private double mMaxFactor = 2;
	private double mCooldown;
	private Location mCenter;
	private double mRange;
	private boolean mTrigger;
	private List<Player> mPlayers = new ArrayList<Player>();

	public SpellFinalSwarm(Plugin plugin, Location loc, double range) {
		mPlugin = plugin;
		mCenter = loc;
		mRange = range;
	}

	@Override
	public void run() {
		World world = mCenter.getWorld();
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
		double cooldownFactor = Math.min(mMaxFactor, (Math.log10(mPlayers.size()) + 1) / 1.2);
		mCooldown = mSoloCooldown / cooldownFactor;
		mT -= 5;
		if (mT <= 0) {
			mT += mCooldown;
			mCount = Math.max(mMinSpawnCount, Math.sqrt(mPlayers.size()));

			while (mCount > 0) {
				Location loc = SpellRaiseDead.getRandomCenteration(mCenter, 39);
				SpellRaiseDead.riseUndead(loc, mPlugin);
				world.playSound(loc, Sound.BLOCK_GRAVEL_HIT, SoundCategory.HOSTILE, 1, 1);
				mCount--;
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

}
