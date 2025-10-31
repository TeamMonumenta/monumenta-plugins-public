package com.playmonumenta.plugins.fishing;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.ParticleCategory;
import com.playmonumenta.plugins.utils.FastUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class RingsFM extends FishingMinigame {
	private final Color[] RING_COLORS = new Color[]{
		Color.fromRGB(255, 220, 20),
		Color.fromRGB(255, 200, 20),
		Color.fromRGB(255, 180, 20),
		Color.fromRGB(255, 160, 20),
		Color.fromRGB(255, 140, 20)
	};
	private final int[] RING_TIMINGS = new int[]{
		FastUtils.randomIntInRange(30, 50),
		FastUtils.randomIntInRange(60, 75),
		FastUtils.randomIntInRange(85, 100),
		FastUtils.randomIntInRange(105, 115),
		FastUtils.randomIntInRange(120, 125)
	};
	private final int[] RING_DURATIONS;
	private final int[] DECOY_TIMINGS;
	private final Color DECOY_COLOR = Color.fromRGB(80, 80, 80);
	private final int UPPER_LENIENCE = 10;
	private final int LOWER_LENIENCE = 8;
	private final int mRingCount;
	private final boolean mUseDurations;
	private final int mDecoyCount;
	private boolean mPrepareCancel = false;
	private boolean mPrepareInput = false;

	public RingsFM(int ringCount, boolean useDurations, int decoyCount) {
		mRingCount = ringCount;
		mUseDurations = useDurations;
		mDecoyCount = decoyCount;

		RING_DURATIONS = new int[5];
		DECOY_TIMINGS = new int[4];
		for (int i = 0; i < 5; i++) {
			RING_DURATIONS[i] = FastUtils.randomIntInRange(12, RING_TIMINGS[i]);
			if (i != 4) {
				DECOY_TIMINGS[i] = FastUtils.randomIntInRange(-10, 10) + (RING_TIMINGS[i + 1] + RING_TIMINGS[i]) / 2;
			}
		}
	}

	@Override
	protected void beginMinigame(FishingManager fishingManager, Player player, Location centre) {
		new BukkitRunnable() {
			int mTicks = 0;
			int mSuccess = 0;

			@Override
			public void run() {
				mTicks++;
				if (mPrepareCancel) {
					fishingManager.minigameFailure(player);
					this.cancel();
					return;
				}

				// Visuals and SFX
				for (int i = 0; i < mRingCount; i++) {
					int duration = mUseDurations ? RING_DURATIONS[i] : 30;
					int ringProgressRemaining = RING_TIMINGS[i] - mTicks;

					if (ringProgressRemaining <= duration && ringProgressRemaining > -3) {
						drawRing(ringProgressRemaining, centre, player, RING_COLORS[i], duration);
					}
					if (ringProgressRemaining == 2) {
						player.playSound(player.getLocation(), Sound.BLOCK_BUBBLE_COLUMN_WHIRLPOOL_INSIDE, SoundCategory.PLAYERS, 1f, 2f);
					}
				}
				for (int i = 0; i < mDecoyCount; i++) {
					int decoyProgressRemaining = DECOY_TIMINGS[i] - mTicks;

					if (Math.abs(decoyProgressRemaining) < 12) {
						drawRing(decoyProgressRemaining, centre, player, DECOY_COLOR, 12);
					}
				}
				drawRing(0, centre, player, Color.fromRGB(255, 255, 255), 30);

				// Functionality
				if (mTicks > RING_TIMINGS[mSuccess] + UPPER_LENIENCE) {
					fishingManager.minigameFailure(player);
					this.cancel();
					return;
				}

				if (!mPrepareInput) {
					return;
				}
				mPrepareInput = false;
				player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, SoundCategory.PLAYERS, 1f, 2f);

				if (mTicks < RING_TIMINGS[mSuccess] - LOWER_LENIENCE) {
					fishingManager.minigameFailure(player);
					this.cancel();
					return;
				} else {
					mSuccess++;
				}

				if (mSuccess >= mRingCount) {
					fishingManager.minigameSuccess(player);
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
		drawRing(-2, centre, player, RING_COLORS[0], 1);
	}

	@Override
	protected void onLeftClick() {
		mPrepareInput = true;
	}

	@Override
	protected void onRightClick() {
		mPrepareInput = true;
	}

	private void drawRing(int progress, Location centre, Player player, Color color, int duration) {
		new PPCircle(Particle.REDSTONE, centre, 5 * (progress + 3) / (double) duration).ringMode(true).count(20).minimumCount(10).data(new Particle.DustOptions(color, 0.75f)).spawnForPlayer(ParticleCategory.OWN_ACTIVE, player);
	}
}
