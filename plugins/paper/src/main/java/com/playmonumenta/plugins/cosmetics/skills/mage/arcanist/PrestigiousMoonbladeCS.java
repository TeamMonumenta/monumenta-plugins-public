package com.playmonumenta.plugins.cosmetics.skills.mage.arcanist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.Cosmetic;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.skills.PrestigeCS;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPPeriodic;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class PrestigiousMoonbladeCS extends CosmicMoonbladeCS implements PrestigeCS {

	public static final String NAME = "Prestigious Moonblade";

	private static final Particle.DustOptions GOLD_COLOR1 = new Particle.DustOptions(Color.fromRGB(207, 143, 23), 1.25f);
	private static final Particle.DustOptions GOLD_COLOR2 = new Particle.DustOptions(Color.fromRGB(255, 223, 47), 1.0f);
	private static final Particle.DustOptions GOLD_COLOR3 = new Particle.DustOptions(Color.fromRGB(255, 239, 191), 0.8f);
	private static final Particle.DustOptions LIGHT_COLOR = new Particle.DustOptions(Color.fromRGB(255, 247, 207), 1.0f);
	private double mShortAngle;
	private double mLongAngle;
	private double mDShortAngle;
	private double mDLongAngle;

	@Override
	public @Nullable Cosmetic getCosmetic() {
		return new Cosmetic(CosmeticType.COSMETIC_SKILL, NAME, false, this.getAbilityName(),
			"Amber planes cleave",
			"through your foes."
		);
	}

	@Override
	public ClassAbility getAbilityName() {
		return ClassAbility.COSMIC_MOONBLADE;
	}

	@Override
	public Material getDisplayItem() {
		return Material.GOLDEN_SWORD;
	}

	@Override
	public @Nullable String getName() {
		return NAME;
	}

	@Override
	public boolean isUnlocked(Player player) {
		return player != null;
	}

	@Override
	public String[] getLockDesc() {
		return List.of("LOCKED").toArray(new String[0]);
	}

	@Override
	public int getPrice() {
		return 1;
	}

	@Override
	public void moonbladeSwingEffect(World world, Player mPlayer, Location origin, double range, int mSwings, int maxSwing) {
		float mPitch = maxSwing <= 1 ? 1.25f : ((mSwings - 1) * 0.85f + (maxSwing - mSwings) * 0.6f) / (maxSwing - 1);
		world.playSound(origin, Sound.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 0.6f, 0.7f);
		world.playSound(origin, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1.4f, mPitch);
		world.playSound(origin, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 1.1f, mPitch + 0.06f);
		world.playSound(origin, Sound.ITEM_TRIDENT_THROW, SoundCategory.PLAYERS, 0.8f, mPitch + 0.12f);
		world.playSound(origin, Sound.ITEM_TRIDENT_RIPTIDE_1, SoundCategory.PLAYERS, 0.45f, 1.6f);
		new BukkitRunnable() {
			@Override
			public void run() {
				world.playSound(origin, Sound.BLOCK_DISPENSER_DISPENSE, SoundCategory.PLAYERS, 2.8f, mPitch);
				world.playSound(origin, Sound.BLOCK_DISPENSER_DISPENSE, SoundCategory.PLAYERS, 2.4f, mPitch + 0.2f);
			}
		}.runTaskLater(Plugin.getInstance(), 3);

		Location mCenter = origin.clone().add(0, 0.125, 0);
		new PPCircle(Particle.REDSTONE, mCenter, 0.8 * range).data(LIGHT_COLOR)
			.count((int) Math.ceil(4.8 * 3.1416 * range)).ringMode(true).spawnAsPlayerActive(mPlayer);
		new PPCircle(Particle.REDSTONE, mCenter, 0.7 * range).data(GOLD_COLOR3)
			.count((int) Math.ceil(3 * 3.1416 * range * range)).ringMode(false).spawnAsPlayerActive(mPlayer);
		if (mSwings <= 1) {
			world.playSound(origin, Sound.BLOCK_BELL_USE, SoundCategory.PLAYERS, 5f, 0.8f);
			mLongAngle = FastUtils.RANDOM.nextDouble(2 * 3.1416);
			mDLongAngle = FastUtils.RANDOM.nextDouble(0.48 * 3.1416) + 0.2 * 3.1416;
			mShortAngle = FastUtils.RANDOM.nextInt(12) * 3.1416 / 6 + mLongAngle / 12;
			mDShortAngle = Math.floor(FastUtils.RANDOM.nextDouble(1.8) + 0.6) + mDLongAngle / 12;
		}
		Vector mFront = origin.getDirection().setY(0).normalize().multiply(0.8 * range);
		int uLong = (int) Math.ceil(2.8 * range);
		int uShort = (int) Math.ceil(1.6 * range);
		ParticleUtils.drawCurve(mCenter, 0, uShort, mFront,
			t -> 0.55 * t / uShort * FastUtils.cos(mShortAngle - (mSwings - 1) * mDShortAngle),
				t -> 0, t -> 0.55 * t / uShort * FastUtils.sin(mShortAngle - (mSwings - 1) * mDShortAngle),
				(l, t) -> new PartialParticle(Particle.REDSTONE, l, 4, 0.12, 0.12, 0.12, 0, GOLD_COLOR1).spawnAsPlayerActive(mPlayer)
		);
		ParticleUtils.drawCurve(mCenter, 0, uLong, mFront,
			t -> 0.95 * t / uLong * FastUtils.cos(mLongAngle - (mSwings - 1) * mDLongAngle),
				t -> 0, t -> 0.95 * t / uLong * FastUtils.sin(mLongAngle - (mSwings - 1) * mDLongAngle),
				(l, t) -> new PartialParticle(Particle.REDSTONE, l, 3, 0.05, 0.05, 0.05, 0, GOLD_COLOR2).spawnAsPlayerActive(mPlayer)
		);

		new BukkitRunnable() {
			final int mI = mSwings;
			double mRoll;
			double mD = 45;
			boolean mInit = false;
			PPPeriodic mParticle1 = new PPPeriodic(Particle.REDSTONE, origin).count(1).delta(0.1, 0.1, 0.1);
			PPPeriodic mParticle2 = new PPPeriodic(Particle.REDSTONE, origin).count(1).delta(0.1, 0.1, 0.1);
			PPPeriodic mParticle3 = new PPPeriodic(Particle.REDSTONE, origin).count(1).data(LIGHT_COLOR);

			@Override
			public void run() {
				if (!mInit) {
					if (mI % 2 == 0) {
						mRoll = (mI > 2 && mI == maxSwing) ? -37.3 : -(8 + FastUtils.RANDOM.nextDouble(4.0));
						mD = 40;
					} else {
						mRoll = (mI > 2 && mI == maxSwing) ? 36.9 : 8 + FastUtils.RANDOM.nextDouble(6.4);
						mD = 140;
					}
					mInit = true;
				}
				if (mI % 2 == 0) {
					for (double r = 1; r < range; r += (mI % 4 == 0) ? 0.45 : 0.4) {
						for (double degree = mD; degree < mD + 30; degree += 5) {
							Location l = origin.clone().add(0, 1.25, 0).add(moonbladeOffset(r, degree, mRoll, origin));
							mParticle1.location(l).data(ParticleUtils.getTransition(GOLD_COLOR1, GOLD_COLOR2, (degree - 40) / 90.0)).spawnAsPlayerActive(mPlayer);
							mParticle2.location(l).data(ParticleUtils.getTransition(GOLD_COLOR1, GOLD_COLOR2, (degree - 40) / 120.0)).spawnAsPlayerActive(mPlayer);
							if (FastUtils.RANDOM.nextDouble() < r * 0.125) {
								mParticle3.location(l).spawnAsPlayerActive(mPlayer);
							}
						}
					}

					mD += 30;
				} else {
					for (double r = 1; r < 5; r += 0.55) {
						for (double degree = mD; degree > mD - 30; degree -= 5) {
							Location l = origin.clone().add(0, 1.25, 0).add(moonbladeOffset(r, degree, mRoll, origin));
							l.setPitch(-l.getPitch());
							mParticle1.location(l).data(ParticleUtils.getTransition(GOLD_COLOR1, GOLD_COLOR2, (degree - 140) / -90.0)).spawnAsPlayerActive(mPlayer);
							mParticle2.location(l).data(ParticleUtils.getTransition(GOLD_COLOR1, GOLD_COLOR2, (degree - 140) / -120.0)).spawnAsPlayerActive(mPlayer);
							if (FastUtils.RANDOM.nextDouble() < r * 0.125) {
								mParticle3.location(l).spawnAsPlayerActive(mPlayer);
							}
						}
					}
					mD -= 30;
				}
				if ((mD >= 130 && mI % 2 == 0) || (mD <= 50 && mI % 2 > 0)) {
					this.cancel();
				}
			}

		}.runTaskTimer(Plugin.getInstance(), 0, 1);

	}
}
