package com.playmonumenta.plugins.bosses.spells.sirius;

import com.playmonumenta.plugins.bosses.bosses.sirius.Sirius;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPExplosion;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SpellSiriusBeams {
	private Sirius mSirius;
	private Location mBeamStartLoc;
	private Plugin mPlugin;
	private static final float SPEED = 2;

	public SpellSiriusBeams(Sirius sirius, Plugin plugin) {
		mSirius = sirius;
		mPlugin = plugin;
		mBeamStartLoc = mSirius.mBoss.getLocation().add(0, 4, 0);
		run();
	}


	private void run() {
		List<Location> mTargetLocs = new ArrayList<>();
		List<Player> mPList = mSirius.getPlayersInArena(false);
		Collections.shuffle(mPList);
		if (!mPList.isEmpty()) {
			for (int i = 0; i < mPList.size() / 4.0; i++) {
				mTargetLocs.add(mPList.get(i).getLocation());
			}
		}
		new BukkitRunnable() {
			@Override
			public void run() {
				soundEffect();
				for (Location loc : mTargetLocs) {
					Player mTarget = EntityUtils.getNearestPlayer(loc, 50);
					Vector vec;
					if (mTarget != null && mPList.contains(mTarget)) {
						vec = LocationUtils.getVectorTo(mTarget.getLocation(), loc);
					} else {
						vec = LocationUtils.getVectorTo(FastUtils.getRandomElement(mPList).getLocation(), loc);
					}
					vec.setY(0);
					if (!(vec.getX() == 0 && vec.getZ() == 0)) {
						vec.normalize();
						vec.multiply(SPEED);
						loc.add(vec);
						loc = LocationUtils.fallToGround(loc, 0);
					}
					final BoundingBox mBox = BoundingBox.of(mBeamStartLoc, 0.4, 0.4, 0.4);
					final double mStepCount = LocationUtils.getVectorTo(loc, mBeamStartLoc).length();
					Vector dir = LocationUtils.getDirectionTo(loc, mBeamStartLoc);
					for (int i = 0; i < mStepCount; i++) {
						for (Player player : PlayerUtils.playersInRange(mBox.getCenter().toLocation(mSirius.mBoss.getWorld()), 5, true)) {
							if (player.getBoundingBox().overlaps(mBox) && player.getNoDamageTicks() <= 0) {
								DamageUtils.damage(mSirius.mBoss, player, DamageEvent.DamageType.MAGIC, 50, null, false, true, "Blight Beams");
								MovementUtils.knockAway(mBox.getCenter().toLocation(mSirius.mBoss.getWorld()), player, 0.3f, 0.1f);
								PassiveStarBlight.applyStarBlight(player);
								new PPExplosion(Particle.REDSTONE, player.getLocation()).count(15).delta(2.5).data(new Particle.DustOptions(Color.fromRGB(0, 130, 130), 1f)).spawnAsBoss();
							}
						}
						mBox.shift(dir);
						Location boxLoc = mBox.getCenter().toLocation(mSirius.mBoss.getWorld());
						if (boxLoc.getBlock().isSolid()) {
							mSirius.mStarBlightConverter.convertColumn(boxLoc.getX(), boxLoc.getZ());
						}
						new PartialParticle(Particle.REDSTONE, boxLoc, 1).data(calculateColorProgress(i, (int) mStepCount)).spawnAsBoss();
					}
					new PPExplosion(Particle.SCRAPE, loc).count(5).delta(0.5f).spawnAsBoss();
					mSirius.mStarBlightConverter.convertPartialSphere(2, loc);
					//new PPLine(Particle.REDSTONE, loc, mBeamStartLoc).countPerMeter(4).data(new Particle.DustOptions(Color.fromRGB(0, 130, 130), 1f)).spawnAsBoss();
				}
				if (mSirius.mBoss.isDead()) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 10, 10);
	}

	private void soundEffect() {
		World world = mBeamStartLoc.getWorld();
		world.playSound(mBeamStartLoc, Sound.ENTITY_GUARDIAN_HURT, SoundCategory.HOSTILE, 2, 2);
		world.playSound(mBeamStartLoc, Sound.ENTITY_ENDER_DRAGON_HURT, SoundCategory.HOSTILE, 0.5f, 0.9f);
		world.playSound(mBeamStartLoc, Sound.ENTITY_ALLAY_HURT, SoundCategory.HOSTILE, 0.4f, 0.8f);
		world.playSound(mBeamStartLoc, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.HOSTILE, 0.6f, 0.9f);
		world.playSound(mBeamStartLoc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.HOSTILE, 2, 0.8f);
	}

	private Particle.DustOptions calculateColorProgress(int distance, int maxDistance) {
		Particle.DustOptions data;
		int halfDistance = maxDistance / 2;
		if (distance < halfDistance) {
			// Transition from start to mid
			data = new Particle.DustOptions(
				ParticleUtils.getTransition(Color.fromRGB(0, 80, 80), Color.fromRGB(0, 105, 105), Math.min(distance / (double) halfDistance, 1)),
				2.25f
			);
		} else {
			// Transition from mid to end
			data = new Particle.DustOptions(
				ParticleUtils.getTransition(Color.fromRGB(0, 105, 105), Color.fromRGB(0, 130, 130), Math.min((distance - halfDistance) / (double) halfDistance, 1)),
				2.25f
			);
		}
		return data;
	}
}
