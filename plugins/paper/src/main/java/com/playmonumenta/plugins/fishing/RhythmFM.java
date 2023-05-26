package com.playmonumenta.plugins.fishing;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.particle.ParticleCategory;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class RhythmFM extends FishingMinigame {
	private final int START_TIME = 5;
	private final int END_TIME = 5;
	private final int UPPER_LENIENCE = 9;
	private final int LOWER_LENIENCE = 5;
	private static final int AUTOMATIC_TIMING = 0;
	private static final int CLICKABLE_TIMING = 1;
	private static final int CLICKED_TIMING = 2;
	private static final int LEFT_CLICKABLE_TIMING = 3;
	private static final int RIGHT_CLICKABLE_TIMING = 4;
	private static final Color[] COLORS = new Color[] {
		Color.fromRGB(200, 200, 200),
		Color.fromRGB(100, 255, 50),
		Color.fromRGB(255, 255, 255),
		Color.fromRGB(20, 255, 200),
		Color.fromRGB(255, 100, 0)
	};
	private @Nullable Vector mDirection = null;
	private final int mDuration;
	private final int mLoops;
	private final ArrayList<Integer> mTimings;
	private final ArrayList<Integer> mAutoTimings;
	private final boolean mFadeMode;
	private final boolean mTwoInputMode;
	private boolean mPrepareCancel = false;
	private boolean mPrepareInput = false;
	private boolean mIsLeftInput = false;

	public RhythmFM(boolean fade, boolean twoClick) {
		mFadeMode = fade;
		mTwoInputMode = twoClick;
		int randomInt = FastUtils.randomIntInRange(0, fade ? 5 : 7);
		switch (randomInt) {
			case 0 -> {
				mDuration = 72;
				mAutoTimings = new ArrayList<>(List.of(0, 12, 18, 24));
				mTimings = new ArrayList<>(List.of(36, 48, 54, 60));
				mLoops = 2;
			}
			case 1 -> {
				mDuration = 90;
				mAutoTimings = new ArrayList<>(List.of(0, 5, 15, 20, 30, 35, 45, 60, 75));
				mTimings = new ArrayList<>(List.of(50, 65, 80));
				mLoops = 2;
			}
			case 2 -> {
				mDuration = 80;
				mAutoTimings = new ArrayList<>(List.of(0, 5, 15, 20, 30));
				mTimings = new ArrayList<>(List.of(40, 45, 55, 60, 70));
				mLoops = 2;
			}
			case 3 -> {
				mDuration = 112;
				mAutoTimings = new ArrayList<>(List.of(0, 7, 14, 21, 28, 56, 63, 70, 77, 84));
				mTimings = new ArrayList<>(List.of(35, 42, 91, 98, 105));
				mLoops = 2;
			}
			case 4 -> {
				mDuration = 72;
				mAutoTimings = new ArrayList<>(List.of(0, 9, 18, 27));
				mTimings = new ArrayList<>(List.of(36, 45, 54, 63));
				mLoops = 2;
			}
			case 5 -> {
				mDuration = 80;
				mAutoTimings = new ArrayList<>(List.of(0, 10, 40, 45, 50));
				mTimings = new ArrayList<>(List.of(20, 25, 30, 60, 65, 70));
				mLoops = 2;
			}
			case 6 -> {
				mDuration = 126;
				mAutoTimings = new ArrayList<>(List.of(7, 14, 21, 63, 70, 77));
				mTimings = new ArrayList<>(List.of(35, 42, 49, 91, 98, 105, 119));
				mLoops = 1;
			}
			default -> {
				mDuration = 24;
				mAutoTimings = new ArrayList<>(List.of(3, 6, 9, 12, 15, 18));
				mTimings = new ArrayList<>(List.of(21));
				mLoops = 1;
			}
		}
	}

	@Override
	protected void beginMinigame(FishingManager fishingManager, Player player, Location centre) {
		if (mDirection == null) {
			fishingManager.minigameFailure(player);
			return;
		}
		Vector planeVectorX = mDirection.clone().crossProduct(new Vector(0, 1, 0)).normalize(); // Left pointing
		Vector planeVectorY = planeVectorX.clone().crossProduct(mDirection).normalize();
		int durationPerLoop = mDuration / mLoops;

		ArrayList<RhythmCircle> circles = new ArrayList<>();
		for (int i : mTimings) {
			double angle = Math.PI * 2 * i / (double) durationPerLoop + Math.PI / 2;
			Vector offsetX = planeVectorX.clone().multiply(1.5 * FastUtils.cos(angle));
			Vector offsetY = planeVectorY.clone().multiply(1.5 * FastUtils.sin(angle));
			circles.add(new RhythmCircle(player, mTwoInputMode ? FastUtils.randomIntInRange(3, 4) : 1, centre.clone().add(offsetX).add(offsetY), planeVectorX, planeVectorY, i));
		}
		for (int i : mAutoTimings) {
			double angle = Math.PI * 2 * i / (double) durationPerLoop + Math.PI / 2;
			Vector offsetX = planeVectorX.clone().multiply(1.5 * FastUtils.cos(angle));
			Vector offsetY = planeVectorY.clone().multiply(1.5 * FastUtils.sin(angle));
			circles.add(new RhythmCircle(player, 0, centre.clone().add(offsetX).add(offsetY), planeVectorX, planeVectorY, i));
		}

		new BukkitRunnable() {
			int mTicks = -START_TIME;
			int mLoop = 1;
			@Override
			public void run() {
				mTicks++;
				if (mPrepareCancel) {
					fishingManager.minigameFailure(player);
					this.cancel();
					return;
				}

				// Backdrop.
				if (mTicks % 5 == 0) {
					for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 20) {
						Vector offsetX = planeVectorX.clone().multiply(2.05 * FastUtils.cos(angle));
						Vector offsetY = planeVectorY.clone().multiply(2.05 * FastUtils.sin(angle));
						new PartialParticle(Particle.REDSTONE, centre.clone().add(offsetX).add(offsetY))
							.data(new Particle.DustOptions(COLORS[AUTOMATIC_TIMING], 0.6f))
							.count(1).minimumCount(0)
							.spawnForPlayer(ParticleCategory.OWN_ACTIVE, player);
					}
				}
				double angle = Math.PI / 2 + Math.PI * 2 * mTicks / durationPerLoop;
				Vector offsetX = planeVectorX.clone().multiply(2.15 * FastUtils.cos(angle));
				Vector offsetY = planeVectorY.clone().multiply(2.15 * FastUtils.sin(angle));
				new PartialParticle(Particle.REDSTONE, centre.clone().add(offsetX).add(offsetY))
					.data(new Particle.DustOptions(COLORS[CLICKED_TIMING], 0.7f))
					.count(3).delta(0.02).minimumCount(0)
					.spawnForPlayer(ParticleCategory.OWN_ACTIVE, player);

				// Gameplay.
				for (RhythmCircle circle : circles) {
					int circleTime = circle.mTime;
					int circleLoop = circleTime / durationPerLoop;
					int timingType = circle.mState % 5;

					// Visuals, render relevant circles.
					if (mTicks >= Math.min(circleLoop == 0 ? -START_TIME : circleLoop * durationPerLoop, circleTime - durationPerLoop / 2) && mTicks < circleTime + durationPerLoop / 2) {
						circle.draw();
					} else {
						continue;
					}

					// Visuals, fade control.
					if ((mTicks % 2 == 0 && (circle.mState == CLICKED_TIMING || circle.mState >= 5)) ||
						(mFadeMode && mLoop > 1 && mTicks % 3 == 0)) {
						circle.increaseFade();
					}
					if (mFadeMode && mLoop > 2 && mTicks % durationPerLoop == 0) {
						circle.mState = timingType;
					}

					// Visuals, automatically timed circle handling.
					if (mTicks == circleTime && timingType == AUTOMATIC_TIMING) {
						player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_COW_BELL, 1f, 1f);
						circle.mState = CLICKED_TIMING;
					}

					// Functionality
					if (!(timingType == CLICKABLE_TIMING || timingType == LEFT_CLICKABLE_TIMING || timingType == RIGHT_CLICKABLE_TIMING)) {
						continue;
					}

					// Functionality, wrong type/successful click management.
					if (mPrepareInput && mTicks <= circleTime + UPPER_LENIENCE && mTicks >= circleTime - LOWER_LENIENCE) {
						if (mTwoInputMode && ((mIsLeftInput && timingType == RIGHT_CLICKABLE_TIMING) || (!mIsLeftInput && timingType == LEFT_CLICKABLE_TIMING))) { // wrong click type applied
							fishingManager.minigameFailure(player);
							this.cancel();
							return;
						}

						if (mTwoInputMode && mIsLeftInput) {
							player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1f, 1.6f);
						} else if (mTwoInputMode) {
							player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1f, 0.8f);
						} else {
							player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, SoundCategory.PLAYERS, 1f, 1.2f);
						}

						new PartialParticle(Particle.NOTE, centre.clone().add(planeVectorY.clone().multiply(2.5)), 1).spawnForPlayer(ParticleCategory.OWN_ACTIVE, player);

						circle.mState = CLICKED_TIMING;
						mPrepareInput = false;
						mIsLeftInput = false;
					}

					// Functionality, too late click (non-clickable circles are culled by this point).
					if (mTicks > circle.mTime + UPPER_LENIENCE) {
						fishingManager.minigameFailure(player);
						this.cancel();
						return;
					}
				}

				// Early click management.
				if (mPrepareInput) {
					player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, SoundCategory.PLAYERS, 1f, 1.3f);
					fishingManager.minigameFailure(player);
					this.cancel();
					return;
				}

				if (mTicks % (mDuration / mLoops) == 0 && mTicks > 0) {
					mLoop++;
				}
				if (mTicks > mDuration + END_TIME) {
					fishingManager.minigameSuccess(player, getForcedLootTable());
					this.cancel();
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 1, 1);
	}

	@Override
	protected void cancelMinigame() {
		mPrepareCancel = true;
	}

	@Override
	protected void previewMinigame(Player player, Location centre) {
		centre.setY(centre.getY() + 2);
		mDirection = player.getEyeLocation().clone().subtract(centre).toVector().normalize();

		Vector planeVectorX = mDirection.clone().crossProduct(new Vector(0, 1, 0)).normalize();
		Vector planeVectorY = planeVectorX.clone().crossProduct(mDirection).normalize();
		for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 20) {
			Vector offsetX = planeVectorX.clone().multiply(2.05 * FastUtils.cos(angle));
			Vector offsetY = planeVectorY.clone().multiply(2.05 * FastUtils.sin(angle));
			new PartialParticle(Particle.REDSTONE, centre.clone().add(offsetX).add(offsetY))
				.data(new Particle.DustOptions(COLORS[AUTOMATIC_TIMING], 0.6f))
				.count(1).minimumCount(0)
				.spawnForPlayer(ParticleCategory.OWN_ACTIVE, player);
		}
	}

	@Override
	protected void onLeftClick() {
		mPrepareInput = true;
		if (mTwoInputMode) {
			mIsLeftInput = true;
		}
	}

	@Override
	protected void onRightClick() {
		mPrepareInput = true;
	}

	public static class RhythmCircle {
		private int mState; // 0 - automated, 1 - to click (either), 2 - clicked, 3 - to click (left), 4 - to click (right), +5 to increase fade, takes 0-29
		private final Location mLoc;
		private final Vector mUnitVectorX;
		private final Vector mUnitVectorY;
		private final Player mPlayer;
		private final int mTime;

		public RhythmCircle(Player player, int state, Location location, Vector unitVectorX, Vector unitVectorY, int time) {
			mState = state;
			mLoc = location;
			mUnitVectorX = unitVectorX;
			mUnitVectorY = unitVectorY;
			mPlayer = player;
			mTime = time;
		}

		public void increaseFade() {
			if (mState < 25) {
				mState += 5;
			}
		}

		public void draw() {
			int count = 4 * (5 - mState / 5);
			if (count == 0) {
				return;
			}
			for (double angle = 0; angle < Math.PI * 2; angle += 2 * Math.PI / count) {
				Vector offsetX = mUnitVectorX.clone().multiply(0.3 * FastUtils.cos(angle));
				Vector offsetY = mUnitVectorY.clone().multiply(0.3 * FastUtils.sin(angle));
				new PartialParticle(Particle.REDSTONE, mLoc.clone().add(offsetX).add(offsetY))
					.count(1).minimumCount(0)
					.data(new Particle.DustOptions(COLORS[mState % 5], 0.6f))
					.spawnForPlayer(ParticleCategory.OWN_ACTIVE, mPlayer);
				if ((mState % 5 == LEFT_CLICKABLE_TIMING && (angle <= Math.PI / 2.0 || angle >= 3 * Math.PI / 2.0)) ||
					(mState % 5 == RIGHT_CLICKABLE_TIMING && (angle >= Math.PI / 2.0 && angle <= 3 * Math.PI / 2.0))) {
					for (int i = 1; i < 5; i++) {
						new PartialParticle(Particle.REDSTONE, mLoc.clone().add(offsetX.clone().multiply(i / 5.0)).add(offsetY.clone().multiply(i / 5.0)))
							.count(1).minimumCount(0)
							.data(new Particle.DustOptions(COLORS[mState % 5], 0.3f))
							.spawnForPlayer(ParticleCategory.OWN_ACTIVE, mPlayer);
					}
				}
			}
		}
	}
}
