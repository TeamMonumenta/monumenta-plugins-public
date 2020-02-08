package com.playmonumenta.plugins.bosses.spells;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class SpellBombToss extends Spell {

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final int mRange;
	private final int mYield;
	private final int mLobs;
	private final int mFuse;

	private final List<TNTPrimed> mTNTList = new ArrayList<TNTPrimed>();

	public SpellBombToss(Plugin plugin, LivingEntity boss, int range) {
		this(plugin, boss, range, 4, 1, 50);
	}

	public SpellBombToss(Plugin plugin, LivingEntity boss, int range, int yield, int lobs, int fuse) {
		mPlugin = plugin;
		mBoss = boss;
		mRange = range;
		mYield = yield;
		mLobs = lobs;
		mFuse = fuse;
	}

	@Override
	public void run() {
		// Choose random player within range that has line of sight to boss
		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), mRange);

		BukkitRunnable task = new BukkitRunnable() {
			int mTicks = 0;
			@Override
			public void run() {
				mTicks++;
				mTNTList.clear();

				// TODO: Add particles
				Collections.shuffle(players);
				for (Player player : players) {
					if (LocationUtils.hasLineOfSight(mBoss, player)) {
						launch(player);
						break;
					}
				}
				if (mTicks >= mLobs) {
					this.cancel();
				}
			}

		};

		task.runTaskTimer(mPlugin, 0, 15);
		mActiveRunnables.add(task);
	}

	@Override
	public int duration() {
		return 160; //8 seconds
	}

	@Override
	public void cancel() {
		super.cancel();

		for (Entity e : mTNTList) {
			if (e.isValid()) {
				e.remove();
			}
		}
	}

	public void launch(Player target) {
		Location sLoc = mBoss.getLocation();
		sLoc.setY(sLoc.getY() + 1.7f);
		sLoc.getWorld().playSound(sLoc, Sound.ENTITY_EVOKER_CAST_SPELL, 1, 1);
		try {
			TNTPrimed tnt = (TNTPrimed) EntityUtils.getSummonEntityAt(sLoc, EntityType.PRIMED_TNT, "{Fuse:" + Integer.toString(mFuse) + "}");
			mTNTList.add(tnt);
			tnt.setYield(mYield);
			Location pLoc = target.getLocation();
			Location tLoc = tnt.getLocation();
			Vector vect = new Vector(pLoc.getX() - tLoc.getX(), 0, pLoc.getZ() - tLoc.getZ());
			vect.normalize().multiply((pLoc.distance(tLoc)) / 20).setY(0.7f);
			tnt.setVelocity(vect);
		} catch (Exception e) {
			mPlugin.getLogger().warning("Failed to summon TNT for bomb toss: " + e.getMessage());
			e.printStackTrace();
		}
	}


}
