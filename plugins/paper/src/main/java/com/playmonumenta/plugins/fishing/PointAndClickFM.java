package com.playmonumenta.plugins.fishing;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.particle.ParticleCategory;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import java.util.ArrayList;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class PointAndClickFM extends FishingMinigame {
	private final Color[] PROGRESS_BAR_COLORS = new Color[] {
		Color.fromRGB(100, 255, 50),
		Color.fromRGB(255, 200, 50),
		Color.fromRGB(255, 100, 0),
		Color.fromRGB(200, 0, 20)
	};
	private final Color UI_COLOR = Color.fromRGB(200, 200, 200);
	private final Color CIRCLE_COLOR = Color.fromRGB(255, 200, 50);
	private final Color LEFT_CIRCLE_COLOR = Color.fromRGB(20, 255, 200);
	private final Color RIGHT_CIRCLE_COLOR = Color.fromRGB(255, 100, 0);
	private final double BOARD_WIDTH = 10.0;
	private final double BOARD_HEIGHT = 5.0;
	private final double PROGRESS_WIDTH = 8.0;
	private final double PROGRESS_HEIGHT = 1.0;
	private final double PROGRESS_SEPARATION = 1.0;
	private final double GAME_DURATION;
	private final ArrayList<Vector> CIRCLE_LOCATIONS;
	private final ArrayList<Boolean> CIRCLE_IS_LEFT_CLICK;
	private boolean mPrepareCancel = false;
	private boolean mPrepareLeftClick = false;
	private boolean mPrepareRightClick = false;
	private final boolean mSequentialMode;
	private final boolean mTwoClickMode;
	private double mCircleRadius;
	private @Nullable Vector mDirection = null;

	public PointAndClickFM(double duration, int numCircles, double radius, boolean sequential, boolean twoClick) {
		mCircleRadius = radius;
		mSequentialMode = sequential;
		mTwoClickMode = twoClick;

		GAME_DURATION = duration;
		CIRCLE_LOCATIONS = new ArrayList<>();
		CIRCLE_IS_LEFT_CLICK = new ArrayList<>();
		for (int i = 0; i < numCircles; i++) {
			CIRCLE_LOCATIONS.add(new Vector(FastUtils.randomDoubleInRange(radius, BOARD_WIDTH - radius), FastUtils.randomDoubleInRange(radius, BOARD_HEIGHT - radius), 0));
			while (pointOverlapCircle(CIRCLE_LOCATIONS.get(i), CIRCLE_LOCATIONS)) {
				CIRCLE_LOCATIONS.set(i, new Vector(FastUtils.randomDoubleInRange(radius, BOARD_WIDTH - radius), FastUtils.randomDoubleInRange(radius, BOARD_HEIGHT - radius), 0));
			}
			CIRCLE_IS_LEFT_CLICK.add(FastUtils.RANDOM.nextBoolean());
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
		Location progressBottomCorner = centre.clone().add(planeVectorY.clone().multiply(PROGRESS_SEPARATION + BOARD_HEIGHT / 2).subtract(planeVectorX.clone().multiply(PROGRESS_WIDTH / 2)));
		Location bottomCorner = centre.clone().subtract(planeVectorX.clone().multiply(BOARD_WIDTH / 2)).subtract(planeVectorY.clone().multiply(BOARD_HEIGHT / 2));

		ArrayList<Location> adjustedCircleLocations = new ArrayList<>();
		CIRCLE_LOCATIONS.forEach((circleLocation) -> adjustedCircleLocations.add(bottomCorner.clone().add(planeVectorX.clone().multiply(circleLocation.getX())).add(planeVectorY.clone().multiply(circleLocation.getY()))));
		new BukkitRunnable() {
			int mTicks = 0;
			@Override
			public void run() {
				mTicks++;
				if (adjustedCircleLocations.isEmpty()) {
					fishingManager.minigameSuccess(player);
					this.cancel();
					return;
				}
				if (mTicks >= GAME_DURATION || mPrepareCancel) {
					fishingManager.minigameFailure(player);
					this.cancel();
					return;
				}

				// UI
				if (mTicks % 5 == 1) {
					// Draw board
					drawRectangle(bottomCorner, planeVectorX, planeVectorY, BOARD_WIDTH, BOARD_HEIGHT, 20, player);
					// Draw progress bar
					drawRectangle(progressBottomCorner, planeVectorX, planeVectorY, PROGRESS_WIDTH, PROGRESS_HEIGHT, 16, player);
					// Draw progress meter
					int colorIndex = Math.min(3, (int) (Math.log(GAME_DURATION / (GAME_DURATION - mTicks)) / Math.log(2)));
					double progress = PROGRESS_WIDTH * mTicks / GAME_DURATION;
					for (double width = progress + PROGRESS_WIDTH / 16; width < PROGRESS_WIDTH; width += PROGRESS_WIDTH / 16) {
						for (double height = PROGRESS_HEIGHT / 4; height < PROGRESS_HEIGHT; height += PROGRESS_HEIGHT / 4) {
							drawParticle(progressBottomCorner.clone().add(planeVectorX.clone().multiply(width)).add(planeVectorY.clone().multiply(height)), player, PROGRESS_BAR_COLORS[colorIndex]);
						}
					}
				}

				// Draw circles
				if (mSequentialMode) {
					drawCircle(0, adjustedCircleLocations, planeVectorX, planeVectorY, player);
				} else {
					for (int i = 0; i < adjustedCircleLocations.size(); i++) {
						drawCircle(i, adjustedCircleLocations, planeVectorX, planeVectorY, player);
					}
				}

				// Functionality
				if (mPrepareLeftClick || mPrepareRightClick) {
					Location eyeLoc = player.getEyeLocation();
					double minDistance = eyeLoc.distance(centre) * 0.8;
					double maxDistance = eyeLoc.distance(bottomCorner) * 1.2;
					Location startLoc = eyeLoc.clone().add(eyeLoc.clone().getDirection().multiply(minDistance));
					Location endLoc = eyeLoc.clone().add(eyeLoc.clone().getDirection().multiply(maxDistance));
					Hitbox hitbox = Hitbox.approximateCylinder(startLoc, endLoc, mCircleRadius * 1.15, true).accuracy(0.5);
					for (int i = 0; i < (mSequentialMode ? 1 : adjustedCircleLocations.size()); i++) {
						boolean correspondingClick = (CIRCLE_IS_LEFT_CLICK.get(i) && mPrepareLeftClick) || (!CIRCLE_IS_LEFT_CLICK.get(i) && mPrepareRightClick);
						if (hitbox.contains(adjustedCircleLocations.get(i).toVector()) && (!mTwoClickMode || correspondingClick)) {
							adjustedCircleLocations.remove(i);
							CIRCLE_IS_LEFT_CLICK.remove(i);
							player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, SoundCategory.PLAYERS, 1f, 1.2f);
							if (mSequentialMode) {
								mCircleRadius *= 0.92;
							}
							mPrepareLeftClick = false;
							mPrepareRightClick = false;
							return;
						}
					}
					mPrepareLeftClick = false;
					mPrepareRightClick = false;
					player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, SoundCategory.PLAYERS, 1f, 0.6f);
					mTicks += (int) GAME_DURATION / 16;
				}
			}
		}.runTaskTimer(Plugin.getInstance(), 1, 1);
	}

	@Override
	protected void cancelMinigame() {
		mPrepareCancel = true;
	}

	@Override
	protected void onLeftClick() {
		mPrepareLeftClick = true;
	}

	@Override
	protected void onRightClick() {
		mPrepareRightClick = true;
	}

	@Override
	protected void previewMinigame(Player player, Location centre) {
		centre.setY(centre.getY() + 3 * BOARD_HEIGHT / 5);
		mDirection = player.getEyeLocation().clone().add(0, 1.5, 0).subtract(centre).toVector().normalize();
		Vector planeVectorX = mDirection.clone().crossProduct(new Vector(0, 1, 0)).normalize(); // Left pointing
		Vector planeVectorY = planeVectorX.clone().crossProduct(mDirection).normalize();
		Location progressBottomCorner = centre.clone().add(planeVectorY.clone().multiply(PROGRESS_SEPARATION + BOARD_HEIGHT / 2).subtract(planeVectorX.clone().multiply(PROGRESS_WIDTH / 2)));
		Location bottomCorner = centre.clone().subtract(planeVectorX.clone().multiply(BOARD_WIDTH / 2)).subtract(planeVectorY.clone().multiply(BOARD_HEIGHT / 2));
		drawRectangle(bottomCorner, planeVectorX, planeVectorY, BOARD_WIDTH, BOARD_HEIGHT, 20, player);
		drawRectangle(progressBottomCorner, planeVectorX, planeVectorY, PROGRESS_WIDTH, PROGRESS_HEIGHT, 16, player);
	}

	private boolean pointOverlapCircle(Vector vector, ArrayList<Vector> circles) {
		for (Vector v : circles) {
			if (v.equals(vector)) {
				continue;
			}
			if (v.distance(vector) < mCircleRadius * 2) {
				return true;
			}
		}
		return false;
	}

	private void drawParticle(Location loc, Player player, Color color) {
		new PartialParticle(Particle.REDSTONE, loc).data(new Particle.DustOptions(color, 0.6f)).count(2).minimumCount(0).spawnForPlayer(ParticleCategory.OWN_ACTIVE, player);
	}

	private void drawRectangle(Location bottomCorner, Vector planeVectorX, Vector planeVectorY, double width, double height, int separations, Player player) {
		for (double d = 0; d < width; d += width / separations) {
			drawParticle(bottomCorner.clone().add(planeVectorX.clone().multiply(d)), player, UI_COLOR);
			drawParticle(bottomCorner.clone().add(planeVectorX.clone().multiply(d)).add(planeVectorY.clone().multiply(height)), player, UI_COLOR);
		}
		for (double d = 0; d < height; d += height / separations) {
			drawParticle(bottomCorner.clone().add(planeVectorY.clone().multiply(d)), player, UI_COLOR);
			drawParticle(bottomCorner.clone().add(planeVectorY.clone().multiply(d)).add(planeVectorX.clone().multiply(width)), player, UI_COLOR);
		}
	}

	private void drawCircle(int circleNum, ArrayList<Location> adjustedCircleLocations, Vector planeVectorX, Vector planeVectorY, Player player) {
		Color circleColor = mTwoClickMode ? (CIRCLE_IS_LEFT_CLICK.get(circleNum) ? LEFT_CIRCLE_COLOR : RIGHT_CIRCLE_COLOR) : CIRCLE_COLOR;
		for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 6) {
			drawParticle(adjustedCircleLocations.get(circleNum).clone().add(planeVectorX.clone().multiply(mCircleRadius * FastUtils.cos(angle)).add(planeVectorY.clone().multiply(mCircleRadius * FastUtils.sin(angle)))), player, circleColor);
		}
		if (mTwoClickMode) {
			for (double angle = -Math.PI / 2; angle <= Math.PI / 2; angle += Math.PI / 6) {
				drawParticle(adjustedCircleLocations.get(circleNum).clone().add(planeVectorX.clone().multiply(1.6 * mCircleRadius * FastUtils.cos(CIRCLE_IS_LEFT_CLICK.get(circleNum) ? angle : angle + Math.PI)).add(planeVectorY.clone().multiply(1.6 * mCircleRadius * FastUtils.sin(CIRCLE_IS_LEFT_CLICK.get(circleNum) ? angle : angle + Math.PI)))), player, circleColor);
			}
		}
	}
}
