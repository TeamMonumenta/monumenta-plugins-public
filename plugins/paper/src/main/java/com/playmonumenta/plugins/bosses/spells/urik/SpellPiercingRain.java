package com.playmonumenta.plugins.bosses.spells.urik;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class SpellPiercingRain extends Spell {


	private static final int MAX_RADIUS = 3;
	private static final int R_INTERVAL = 1;
	private static final String MARKER_KEY = "PiercingRain";
	private static final int REMOVAL_DELAY = 40;

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private int mGrace;
	private int mDuration;
	private double mRange;

	public SpellPiercingRain(Plugin plugin, LivingEntity boss, int grace, int duration, double range) {
		mPlugin = plugin;
		mBoss = boss;
		mGrace = grace;
		mDuration = duration;
		mRange = range;
	}



	@Override
	public void run() {
		int selPlayerCount = 0;
		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), 50);
		if (players.size() < 0) {
			return;
		} else {
			selPlayerCount = (int)Math.ceil(players.size()/2);
		}


		List<Player> targets = new ArrayList<Player>();
		Collections.shuffle(players);
		Random rand = new Random();

		// CHANGE THIS FROM <= TO < ON RELEASE
		while (targets.size() <= selPlayerCount) {
			Player player = players.get(rand.nextInt(players.size()));
			if (!targets.contains(player)) {
				targets.add(player);
			}
		}
		if (targets.size() > 0) {
			for (int i = 0; i < targets.size(); i++) {
				rain(targets.get(i));
			}
		}


	}

	private void rain(Player p) {
		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				mTicks += 2;
				Location loc = p.getLocation();
				loc.setY(loc.getY()+12);
				for (double radius = MAX_RADIUS; radius > 0; radius -= R_INTERVAL) {
					for (double rotation =0; rotation <360; rotation+=45) {
						double radian = Math.toRadians(rotation);
						loc.add(FastUtils.cos(radian) * radius, 0, FastUtils.sin(radian) * radius);
						if (mTicks % 20 == 0) {
							loc.getWorld().spawnParticle(Particle.FLAME, loc, 1, 0, 0, 0, 0);
							Arrow arrow = loc.getWorld().spawnArrow(loc, new Vector(0,-1,0), 0.6f, 0);
							arrow.setDamage(5);
							arrow.setPickupStatus(PickupStatus.CREATIVE_ONLY);
							arrow.setShooter(mBoss);
							arrow.setBounce(false);
							arrow.setMetadata(MARKER_KEY, new FixedMetadataValue(mPlugin, null));
						}

						loc.subtract(FastUtils.cos(radian) * radius, 0, FastUtils.sin(radian) * radius);
					}

			}

			if (mTicks > mDuration) {
				this.cancel();

				new BukkitRunnable(){
					int mTicks = 0;
					@Override
					public void run() {
						mTicks++;

						if (mTicks < REMOVAL_DELAY && mTicks % 2 == 0) {
							for (Entity e : mBoss.getNearbyEntities(mRange, mRange, mRange)) {
								if (e instanceof Arrow) {
									if (FastUtils.randomDoubleInRange(0, 1) < 0.5) {
										if (e.hasMetadata(MARKER_KEY)) {
											e.remove();
										}
									}
								}
							}
						}

						if (mTicks == REMOVAL_DELAY) {
							for (Entity e : mBoss.getNearbyEntities(mRange, mRange, mRange)) {
								if (e.hasMetadata(MARKER_KEY)) {
									e.remove();
								}
							}
						}

						if (mTicks > REMOVAL_DELAY) {
							this.cancel();
						}
					}

				}.runTaskTimer(mPlugin, 0, 1);

				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public int duration() {

		return mDuration;
	}

}
