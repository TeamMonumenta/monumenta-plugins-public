package com.playmonumenta.plugins.fishing;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.particle.ParticleCategory;
import com.playmonumenta.plugins.utils.FastUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class DirectionalFM extends FishingMinigame {
	private final Color BAR_COLOR = Color.fromRGB(200, 200, 200);
	private final Color FISH_COLOR = Color.fromRGB(20, 255, 200);
	private final Color LIMITER_COLOR;
	private final boolean SPEED_UP;
	private final double GAME_WIDTH = 5.0;
	private final double GAME_HEIGHT = 0.7;
	private final double GAME_MIN_WIDTH;
	private final int GAME_DURATION;
	private boolean mPrepareCancel = false;
	private boolean mPrepareLeftInput = false;
	private boolean mPrepareRightInput = false;
	private double mAcceleration;
	private double mLimiterPositions = GAME_WIDTH;
	private @Nullable Vector mDirection = null;

	public DirectionalFM(int duration, double closeInFraction, Color limiterColor, double initialAcceleration, boolean speedUp) {
		GAME_DURATION = duration;
		GAME_MIN_WIDTH = closeInFraction * GAME_WIDTH;
		LIMITER_COLOR = limiterColor;
		SPEED_UP = speedUp;
		mAcceleration = initialAcceleration * Math.signum(FastUtils.randomDoubleInRange(-1, 1));
	}

	@Override
	protected void beginMinigame(FishingManager fishingManager, Player player, Location centre) {
		if (mDirection == null) {
			fishingManager.minigameFailure(player);
			return;
		}
		Vector offsetZ = mDirection.clone().multiply(0.3);
		Vector planeVectorX = mDirection.clone().crossProduct(new Vector(0, 1, 0)).normalize(); // Left pointing
		Vector planeVectorY = planeVectorX.clone().crossProduct(mDirection).normalize();
		Location fishLocation = centre.clone().add(offsetZ);

		new BukkitRunnable() {
			int mTicks = 0;
			double mVelocity = 0;
			double mPosition = 0;
			@Override
			public void run() {
				mTicks++;
				if (Math.abs(mVelocity) < 0.4) {
					mVelocity += mAcceleration;
				}
				if (mPrepareCancel) {
					fishingManager.minigameFailure(player);
					this.cancel();
					return;
				}

				// Visuals
				// Draw backdrop board.
				if (mTicks % 5 == 1) {
					for (double width = -GAME_WIDTH; width <= GAME_WIDTH; width += GAME_WIDTH / 20) {
						Vector offsetX = planeVectorX.clone().multiply(width);
						Vector offsetY = planeVectorY.clone().multiply(0.2);
						drawParticle(centre.clone().add(offsetX.clone().add(offsetY)), player, BAR_COLOR);
						drawParticle(centre.clone().add(offsetX.clone().subtract(offsetY)), player, BAR_COLOR);
					}
					for (double width = -GAME_WIDTH; width <= GAME_WIDTH; width += GAME_WIDTH) {
						Vector offsetX = planeVectorX.clone().multiply(width);
						for (double height = -GAME_HEIGHT; height <= GAME_HEIGHT; height += GAME_HEIGHT / 4) {
							Vector offsetY = planeVectorY.clone().multiply(height);
							drawParticle(centre.clone().add(offsetX.clone().add(offsetY)), player, BAR_COLOR);
						}
					}
				}
				// Draw fish circle.
				for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 6) {
					Vector offsetX = planeVectorX.clone().multiply(0.15 * FastUtils.cos(angle));
					Vector offsetY = planeVectorY.clone().multiply(0.15 * FastUtils.sin(angle));
					drawParticle(fishLocation.clone().add(offsetX).add(offsetY), player, FISH_COLOR);
				}
				// Draw limiters.
				for (double width = -mLimiterPositions; width <= mLimiterPositions; width += 2 * mLimiterPositions) {
					Vector offsetX = planeVectorX.clone().multiply(width);
					for (double height = -GAME_HEIGHT; height <= GAME_HEIGHT; height += GAME_HEIGHT / 4) {
						Vector offsetY = planeVectorY.clone().multiply(height);
						drawParticle(centre.clone().add(offsetX.clone().add(offsetY).add(offsetZ)), player, LIMITER_COLOR);
					}
				}

				// Functionality
				fishLocation.add(planeVectorX.clone().multiply(mVelocity));
				mPosition += mVelocity;
				mLimiterPositions -= (GAME_WIDTH - GAME_MIN_WIDTH) / GAME_DURATION;

				if (Math.abs(mPosition) > mLimiterPositions) {
					fishingManager.minigameFailure(player);
					this.cancel();
					return;
				}

				if ((mPosition > 0 && mPrepareLeftInput) || (mPosition < 0 && mPrepareRightInput)) { // fish on same side of click
					mVelocity *= 1.25 * Math.signum(mVelocity) * Math.signum(mPosition);
					mAcceleration *= 1.25;
					player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BANJO, SoundCategory.PLAYERS, 1f, 2f);
				} else if ((mPrepareLeftInput && mVelocity < 0) || (mPrepareRightInput && mVelocity > 0)) { // fish on opposite side of click / moving away from centre
					mAcceleration *= -1;
					mVelocity *= 0.35;
					player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, SoundCategory.PLAYERS, 1f, 1.2f);
				} else if (mPrepareRightInput || mPrepareLeftInput) { // fish on opposite side of click / moving towards centre
					mVelocity *= 1.6;
					mAcceleration *= 1.25;
					player.playSound(player, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, SoundCategory.PLAYERS, 1f, 2f);
				}
				mPrepareLeftInput = false;
				mPrepareRightInput = false;

				if (SPEED_UP && mTicks % 10 == 0) {
					mAcceleration *= 1.15;
				}

				if (mTicks > GAME_DURATION) {
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
		centre.setY(centre.getY() + 1.5);
		mDirection = player.getEyeLocation().clone().subtract(centre).toVector().normalize();
		Vector planeVectorX = mDirection.clone().crossProduct(new Vector(0, 1, 0)).normalize();
		Vector planeVectorY = planeVectorX.clone().crossProduct(mDirection).normalize();
		Vector offsetZ = mDirection.clone().multiply(0.3);
		for (double width = -GAME_WIDTH; width <= GAME_WIDTH; width += GAME_WIDTH / 20) {
			Vector offsetX = planeVectorX.clone().multiply(width);
			Vector offsetY = planeVectorY.clone().multiply(0.2);
			drawParticle(centre.clone().add(offsetX.clone().add(offsetY)), player, BAR_COLOR);
			drawParticle(centre.clone().add(offsetX.clone().subtract(offsetY)), player, BAR_COLOR);
		}
		for (double width = -GAME_WIDTH; width <= GAME_WIDTH; width += GAME_WIDTH) {
			Vector offsetX = planeVectorX.clone().multiply(width);
			for (double height = -GAME_HEIGHT; height <= GAME_HEIGHT; height += GAME_HEIGHT / 4) {
				Vector offsetY = planeVectorY.clone().multiply(height);
				drawParticle(centre.clone().add(offsetX.clone().add(offsetY)), player, BAR_COLOR);
			}
		}
		for (double width = -mLimiterPositions; width <= mLimiterPositions; width += 2 * mLimiterPositions) {
			Vector offsetX = planeVectorX.clone().multiply(width);
			for (double height = -GAME_HEIGHT; height <= GAME_HEIGHT; height += GAME_HEIGHT / 4) {
				Vector offsetY = planeVectorY.clone().multiply(height);
				drawParticle(centre.clone().add(offsetX.clone().add(offsetY).add(offsetZ)), player, LIMITER_COLOR);
			}
		}
	}

	@Override
	protected void onLeftClick() {
		mPrepareLeftInput = true;
	}

	@Override
	protected void onRightClick() {
		mPrepareRightInput = true;
	}

	private void drawParticle(Location loc, Player player, Color color) {
		new PartialParticle(Particle.REDSTONE, loc).data(new Particle.DustOptions(color, 0.6f)).count(2).minimumCount(0).spawnForPlayer(ParticleCategory.OWN_ACTIVE, player);
	}
}
