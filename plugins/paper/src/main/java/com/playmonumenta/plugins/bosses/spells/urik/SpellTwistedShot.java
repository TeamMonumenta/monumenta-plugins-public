package com.playmonumenta.plugins.bosses.spells.urik;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
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
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class SpellTwistedShot extends Spell {

	private static final int MARKER_RADIUS = 1;
	private static final String MARKER_KEY = "TwistedShot";
	private Plugin mPlugin;
	private LivingEntity mBoss;
	private int mCharge;
	private int mDelay;
	private int mCast;
	private double mRange;

	public SpellTwistedShot(Plugin plugin, LivingEntity boss, int charge, int delay, int cast, double range) {
		mPlugin = plugin;
		mBoss = boss;
		mCharge = charge;
		mDelay = delay;
		mCast = cast;
		mRange = range;
	}

	@Override
	public void run() {
		// charge up attack for x seconds putting down a marker at all player locations every y seconds z times

		// charge up attack and place markers
		new BukkitRunnable() {
			int mTicks = 0;
			// list of markers to execute at end of charge up
			List<Location> markers = new ArrayList<Location>();
			List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), mRange);
			@Override
			public void run() {
				mTicks++;

				for (Location loc : markers) {
					mBoss.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, loc.clone(), 1, 0,0,0,0);
					for (double rotation = 0; rotation < 360; rotation += 8) {
						double radian = Math.toRadians(rotation);
						loc.add(FastUtils.cos(radian) * MARKER_RADIUS, 0, FastUtils.sin(radian) * MARKER_RADIUS);
						mBoss.getWorld().spawnParticle(Particle.VILLAGER_HAPPY, loc.clone(), 1, 0,0,0,0);
						loc.subtract(FastUtils.cos(radian) * MARKER_RADIUS, 0, FastUtils.sin(radian) * MARKER_RADIUS);
					}
				}

				if (mTicks % mDelay == 0) {
					for (Player p : players) {
						Bukkit.broadcastMessage("Marker added!" + (mTicks % mDelay == 0) + " " + mTicks + " " + mDelay);
						markers.add(p.getLocation());
					}
				}

				if (mTicks > mCharge) {
					this.cancel();

					// CAST ABILITY
					new BukkitRunnable() {
						int mTime = 0;
						@Override
						public void run() {
							mTime++;
							if (mTime == 1) {
								for(Location loc : markers) {
									loc.setY(loc.getY() + 12);
									Arrow arrow = loc.getWorld().spawnArrow(loc, new Vector(0,-1,0), 1f, 0);
									arrow.setDamage(9);
									arrow.setPickupStatus(PickupStatus.CREATIVE_ONLY);
									arrow.setShooter(mBoss);
									arrow.setMetadata(MARKER_KEY, new FixedMetadataValue(mPlugin,null));
								}
							}



							if (mTime > mCast) {
								this.cancel();

								for (Entity e : mBoss.getNearbyEntities(mRange, mRange, mRange)) {
									if (e instanceof Arrow && e.hasMetadata(MARKER_KEY) && e.isOnGround()) {
										e.remove();
										e.getWorld().spawnParticle(Particle.FLAME, e.getLocation(), 150, 0, 0, 0, 0.165);
										e.getWorld().spawnParticle(Particle.SMOKE_LARGE, e.getLocation(), 65, 0, 0, 0, 0.1);
										e.getWorld().spawnParticle(Particle.EXPLOSION_LARGE, e.getLocation(), 1, 0, 0, 0, 0);
										e.getWorld().playSound(e.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 2, 0.85f);

										for (Player player : PlayerUtils.playersInRange(e.getLocation(), 4)) {
												BossUtils.bossDamage(mBoss, player, 35);
										}
									}
								}

							}


						}

					}.runTaskTimer(mPlugin, 0, 1);

				}

			}

		}.runTaskTimer(mPlugin, 0, 1);

		// summon an arrow above all marker locations that detonates on impact with the ground


	}

	@Override
	public int duration() {
		// TODO Auto-generated method stub
		return 20*20;
	}

}
