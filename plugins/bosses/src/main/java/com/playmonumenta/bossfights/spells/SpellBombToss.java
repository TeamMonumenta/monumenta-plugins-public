package com.playmonumenta.bossfights.spells;

import java.util.Collections;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.bossfights.utils.Utils;

public class SpellBombToss extends Spell {

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final int mRange;
	private final int mYield;
	private final int mLobs;
	private final int mFuse;

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
		List<Player> players = Utils.playersInRange(mBoss.getLocation(), mRange);

		new BukkitRunnable() {
			int t = 0;
			@Override
			public void run() {
				t++;

				// TODO: Add particles
				Collections.shuffle(players);
				for (Player player : players) {
					if (Utils.hasLineOfSight(mBoss.getEyeLocation(), player)) {
						launch(player);
						break;
					}
				}
				if (t >= mLobs) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 15);
	}

	@Override
	public int duration() {
		return 160; //8 seconds
	}

	public void launch(Player target) {
		Location sLoc = mBoss.getLocation();
		sLoc.setY(sLoc.getY() + 1.7f);
		sLoc.getWorld().playSound(sLoc, Sound.ENTITY_EVOKER_CAST_SPELL, 1, 1);
		try {
			TNTPrimed tnt = (TNTPrimed) Utils.summonEntityAt(sLoc, EntityType.PRIMED_TNT, "{Fuse:" + Integer.toString(mFuse) + "}");
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
