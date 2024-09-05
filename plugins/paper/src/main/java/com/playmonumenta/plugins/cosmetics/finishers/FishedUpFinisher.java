package com.playmonumenta.plugins.cosmetics.finishers;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class FishedUpFinisher implements EliteFinisher {
	public static final String NAME = "Fished Up";
	private static final Particle.DustOptions BROWN = new Particle.DustOptions(Color.fromRGB(92, 64, 51), 3.0f);
	private static final Particle.DustOptions LINE_COLOR = new Particle.DustOptions(Color.fromRGB(0, 0, 0), 0.75f);
	private static final Particle.DustOptions REEL_COLOR = new Particle.DustOptions(Color.fromRGB(83, 83, 83), 1.5f);
	private static final float RADIUS = 1f;

	@Override
	public void run(Player p, Entity killedMob, Location loc) {
		if (!(killedMob instanceof LivingEntity le)) {
			return;
		}

		Location mEyeLoc = p.getEyeLocation();
		Vector mDirection = mEyeLoc.getDirection();
		Vector mLookDirection = new Vector(mDirection.getX(), 0, mDirection.getZ()).normalize();
		int mRand = FastUtils.randomIntInRange(0, 1);
		Vector mRotatedLookDirection;
		//Rotate it to the left or right side of the players screen
		if (mRand == 0) {
			mRotatedLookDirection = VectorUtils.rotateYAxis(mLookDirection.clone(), 90);
		} else {
			mRotatedLookDirection = VectorUtils.rotateYAxis(mLookDirection.clone(), 270);
		}

		Vector finalMRotatedLookDirection = mRotatedLookDirection;
		new BukkitRunnable() {
			int mTicks = 0;
			final Location mFishingRodTopLocation = loc.clone().add(0, 10 + killedMob.getHeight(), 0);
			final Location mFishingRodEndLocation = loc.clone().add(finalMRotatedLookDirection.clone().multiply(7)).add(0, 3 + killedMob.getHeight(), 0);
			final Location mReelLocation = loc.clone().add(finalMRotatedLookDirection.clone().multiply(4)).add(0, 4 + killedMob.getHeight(), 0);
			final Location mHookLocation = mFishingRodTopLocation.clone();
			@Nullable LivingEntity mClonedKilledMob;

			@Override public void run() {
				if (mTicks == 0) {
					mFishingRodTopLocation.getWorld().playSound(mFishingRodTopLocation, Sound.ENTITY_FISHING_BOBBER_THROW, 2, 1);
					killedMob.remove();
					mClonedKilledMob = EliteFinishers.createClonedMob(le, p, NamedTextColor.DARK_BLUE);
				}
				new PPLine(Particle.REDSTONE, mFishingRodTopLocation, mHookLocation).data(LINE_COLOR).count(15).spawnAsPlayerActive(p);
				if (mTicks < 20) {
					mFishingRodTopLocation.getWorld().playSound(mFishingRodTopLocation, Sound.ENTITY_FISHING_BOBBER_RETRIEVE, SoundCategory.PLAYERS, 1.6f, 2);
					mHookLocation.subtract(0, 0.5, 0);
				} else {
					if (mTicks == 20) {
						mHookLocation.getWorld().playSound(mHookLocation, Sound.ENTITY_PLAYER_SPLASH_HIGH_SPEED, SoundCategory.PLAYERS, 1.6f, 2);
						mHookLocation.getWorld().playSound(mHookLocation, Sound.ENTITY_FISHING_BOBBER_THROW, SoundCategory.PLAYERS, 2, 2);
						mHookLocation.getWorld().playSound(mHookLocation, Sound.ENTITY_FISHING_BOBBER_SPLASH, SoundCategory.PLAYERS, 1.6f, 2);
					} else {
						mFishingRodTopLocation.getWorld().playSound(mFishingRodTopLocation, Sound.ENTITY_FISHING_BOBBER_RETRIEVE, SoundCategory.PLAYERS, 1.6f, 2);
					}
					mHookLocation.add(0, 0.5, 0);
					if (mClonedKilledMob != null) {
						mClonedKilledMob.teleport(mHookLocation.clone().subtract(0, mClonedKilledMob.getHeight(), 0));
					}
				}
				//Particle generation code
				if (mTicks % 5 == 0) {
					//Rod
					new PPLine(Particle.REDSTONE, mFishingRodEndLocation, mFishingRodTopLocation).data(BROWN).count(25).spawnAsBoss();
					//Reel
					for (int radian = 0; radian < 16; radian++) {
						new PPLine(Particle.REDSTONE, mReelLocation.clone().add(finalMRotatedLookDirection.clone().multiply(FastUtils.cos((radian * Math.PI) / 16f) * RADIUS)).add(0, FastUtils.sin((Math.PI * radian) / 16f) * RADIUS, 0), mReelLocation.clone().add(finalMRotatedLookDirection.clone().multiply(FastUtils.cos((-radian * Math.PI) / 16f) * RADIUS)).add(0, FastUtils.sin((Math.PI * -radian) / 16f) * RADIUS, 0)).data(REEL_COLOR).count(10).spawnAsPlayerActive(p);
					}
				}
				if (mTicks >= 2 * 20) {
					mFishingRodTopLocation.getWorld().playSound(mFishingRodTopLocation, Sound.ENTITY_PLAYER_LEVELUP, 2, 2);
					if (mClonedKilledMob != null) {
						mClonedKilledMob.remove();
					}
					this.cancel();
				}
				mTicks++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	@Override public Material getDisplayItem() {
		return Material.FISHING_ROD;
	}
}
