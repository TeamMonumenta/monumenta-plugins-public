package com.playmonumenta.plugins.bosses.spells.urik;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.FastUtils;

public class SpellSpiralVolley extends Spell{

	private static final String MARKER_KEY = "SpiralVolley";
	private static final String SPIRAL_VOLLEY_TAG_ONE = "UrikSpiralVolleyOne";
	private static final int RADIUS = 16;
	private static final int REMOVAL_DELAY = 40;
	private Plugin mPlugin;
	private LivingEntity mBoss;
	private int mDelay;
	private int mDuration;
	private int mGrace;
	private double mRange;



	public SpellSpiralVolley(Plugin plugin, LivingEntity boss, int delay, int duration, int grace,
			double range) {

		mPlugin = plugin;
		mBoss = boss;
		mDelay = delay;
		mDuration = duration;
		mGrace = grace;
		mRange = range;


	}

	@Override
	public void run() {
		// Iterate through all armorstands within range of boss
		List<ArmorStand> centerpoints = new ArrayList<ArmorStand>();
		for (Entity e : mBoss.getNearbyEntities(mRange, mRange, mRange)) {
			if (e.getScoreboardTags().contains(SPIRAL_VOLLEY_TAG_ONE)) {
				centerpoints.add((ArmorStand) e);
			}
		}


		// If there is an armorstand in list, get location of level boss is on.
		if (!centerpoints.isEmpty()) {
			// TODO select armor stand point based on level boss is at.
			ArmorStand point = centerpoints.get(0);
			Location center = point.getLocation().add(new Vector(0,1,0));

			// PARTICLE MARKER RUNNABLE
			new BukkitRunnable() {
				int mPTicks = 0;
				@Override
				public void run() {
					mPTicks += 2;
					for (double radius = 20; radius > 0; radius -= 2) {
						for (double rotation = 0; rotation < 360; rotation += 8) {
							double radian = Math.toRadians(rotation);
							center.add(FastUtils.cos(radian) * radius, 0, FastUtils.sin(radian) * radius);
							if ((mPTicks % 20 == 0 || mPTicks % 10 == 0) && mPTicks <= mDelay/2 && (((rotation >= 0) && (rotation < 90)) || ((rotation >= 180) && (rotation < 270))) || (mPTicks % 20 == 0 || mPTicks % 10 == 0) && mPTicks > (mDelay/2 + mGrace) && (((rotation >= 90) && (rotation < 180)) || ((rotation >= 270) && (rotation < 360)))) {
								mBoss.getWorld().spawnParticle(Particle.CLOUD, center, 1, 0, 0, 0, 0);
							}
							center.subtract(FastUtils.cos(radian) * radius, 0, FastUtils.sin(radian) * radius);
						}
					}

					if (mPTicks >= mDelay + mGrace) {
						this.cancel();

						// ARROW SPAWNING RUNNABLE
						new BukkitRunnable() {
							int mTicks = 0;
							@Override
							public void run() {
								mTicks += 2;


								for (double rotation = 0; rotation < 360; rotation += 8) {
									double radian = Math.toRadians(rotation);
									center.add(FastUtils.cos(radian) * RADIUS, 0, FastUtils.sin(radian) * RADIUS);
									if ((mTicks % 20 == 0 || mTicks % 10 == 0) && mTicks <= mDuration/2 && (((rotation >= 0) && (rotation < 90)) || ((rotation >= 180) && (rotation < 270)))) {
										mBoss.getWorld().spawnParticle(Particle.FLAME, center, 1, 0, 0, 0, 0);
										Arrow arrow = mBoss.getWorld().spawnArrow(center, point.getLocation().toVector().subtract(center.toVector()), 2, 0);
										arrow.setGravity(false);
										arrow.setDamage(5);
										arrow.setPickupStatus(PickupStatus.CREATIVE_ONLY);
										arrow.setBounce(false);
										arrow.setShooter(mBoss);
										arrow.setMetadata(MARKER_KEY, new FixedMetadataValue(mPlugin, null));
									}
									if ((mTicks % 20 == 0 || mTicks % 10 == 0) && mTicks > (mDuration/2 + mGrace) && (((rotation >= 90) && (rotation < 180)) || ((rotation >= 270) && (rotation < 360)))) {
										mBoss.getWorld().spawnParticle(Particle.FLAME, center, 1, 0, 0, 0, 0);
										Arrow arrow = mBoss.getWorld().spawnArrow(center, point.getLocation().toVector().subtract(center.toVector()), 2, 0);
										arrow.setGravity(false);
										arrow.setDamage(5);
										arrow.setPickupStatus(PickupStatus.CREATIVE_ONLY);
										arrow.setBounce(false);
										arrow.setShooter(mBoss);
										arrow.setMetadata(MARKER_KEY, new FixedMetadataValue(mPlugin, null));
									}
									center.subtract(FastUtils.cos(radian) * RADIUS, 0, FastUtils.sin(radian) * RADIUS);
								}
								if (mTicks > mDuration + mGrace*2) {
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

						}.runTaskTimer(mPlugin,0,2);
					}

				}

			}.runTaskTimer(mPlugin, 0, 1);


		}

	}

	@Override
	public int duration() {
		// TODO Auto-generated method stub
		return mDelay + mDuration + mGrace*2;
	}

}
