package com.playmonumenta.plugins.fishing;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.particle.ParticleCategory;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.math3.util.FastMath;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class MinesweeperFM extends FishingMinigame {
	private final Color[] PROGRESS_BAR_COLORS = new Color[]{
		Color.fromRGB(100, 255, 50),
		Color.fromRGB(255, 200, 50),
		Color.fromRGB(255, 100, 0),
		Color.fromRGB(200, 0, 20)
	};
	private final Color[] DIGIT_COLORS = new Color[]{
		Color.WHITE, Color.BLUE, Color.GREEN, Color.ORANGE, Color.NAVY, Color.MAROON, Color.AQUA, Color.BLACK, Color.GRAY
	};
	private final Color UI_COLOR = Color.fromRGB(200, 200, 200);
	private final double PROGRESS_WIDTH = 4.0;
	private final double PROGRESS_HEIGHT = 1.0;
	private final double PROGRESS_SEPARATION = 1.0;
	private static final double INFO_SEPARATION = 1.5;
	private final double GAME_DURATION;
	private final int MINE_COUNT;
	private final int GRID_DIMENSIONS;
	private final double BOARD_SIZE;
	private @Nullable Vector mDirection = null;
	private boolean mPrepareCancel = false;
	private boolean mPrepareLeftClick = false;
	private boolean mPrepareRightClick = false;
	private final int[][] mGameBoard;
	private final int[][] mVisibleBoard;
	private int mLives = 2;
	private int mFlags;

	public MinesweeperFM(double duration, int mines, int dimensions) {
		GAME_DURATION = duration;
		MINE_COUNT = mines;
		GRID_DIMENSIONS = dimensions;
		BOARD_SIZE = dimensions;

		mGameBoard = new int[dimensions][dimensions];
		mVisibleBoard = new int[dimensions][dimensions];

		mFlags = mines;

		generateBoard();
	}

	@Override
	protected void beginMinigame(FishingManager fishingManager, Player player, Location centre) {
		if (mDirection == null) {
			fishingManager.minigameFailure(player);
			return;
		}
		Vector planeVectorX = mDirection.clone().crossProduct(new Vector(0, 1, 0)).normalize(); // Left pointing
		Vector planeVectorY = planeVectorX.clone().crossProduct(mDirection).normalize();
		Location progressBottomCorner = centre.clone().add(planeVectorY.clone().multiply(PROGRESS_SEPARATION + BOARD_SIZE / 2).subtract(planeVectorX.clone().multiply(PROGRESS_WIDTH / 2)));
		Location bottomCorner = centre.clone().subtract(planeVectorX.clone().multiply(BOARD_SIZE / 2)).subtract(planeVectorY.clone().multiply(BOARD_SIZE / 2));
		Location mineCountLocation = bottomCorner.clone().subtract(planeVectorX.clone().multiply(INFO_SEPARATION)).add(planeVectorY.clone().multiply(BOARD_SIZE / 3));
		Location lifeCountLocation = bottomCorner.clone().subtract(planeVectorX.clone().multiply(INFO_SEPARATION)).add(planeVectorY.clone().multiply(2 * BOARD_SIZE / 3));

		HashMap<Vector, int[]> gridButtons = new HashMap<>();
		Location adjustedBottomCorner = bottomCorner.clone().add(planeVectorX.clone().multiply(BOARD_SIZE / (2 * GRID_DIMENSIONS))).add(planeVectorY.clone().multiply(BOARD_SIZE / (2 * GRID_DIMENSIONS)));

		for (int i = 0; i < mGameBoard.length; i++) {
			for (int j = 0; j < mGameBoard[0].length; j++) {
				Location location = adjustedBottomCorner.clone().add(planeVectorX.clone().multiply(i)).add(planeVectorY.clone().multiply(j));
				gridButtons.put(location.toVector(), new int[]{
					i, j
				});
			}
		}

		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				mTicks++;
				if (mTicks >= GAME_DURATION || mPrepareCancel) {
					fishingManager.minigameFailure(player);
					this.cancel();
					return;
				}

				if (mPrepareLeftClick || mPrepareRightClick) {
					Location eyeLoc = player.getEyeLocation();
					double minDistance = eyeLoc.distance(centre) * 0.6;
					double maxDistance = eyeLoc.distance(bottomCorner) * 1.4;
					Location startLoc = eyeLoc.clone().add(eyeLoc.clone().getDirection().multiply(minDistance));
					Location endLoc = eyeLoc.clone().add(eyeLoc.clone().getDirection().multiply(maxDistance));
					Hitbox hitbox = Hitbox.approximateCylinder(startLoc, endLoc, BOARD_SIZE / (2 * GRID_DIMENSIONS), false);

					for (Vector loc : gridButtons.keySet()) {
						if (hitbox.contains(loc)) {
							int[] coords = gridButtons.get(loc);

							if (mPrepareRightClick) {
								if (mVisibleBoard[coords[0]][coords[1]] == 0) {
									mVisibleBoard[coords[0]][coords[1]] = -1;
									mFlags--;
								} else if (mVisibleBoard[coords[0]][coords[1]] == -1) {
									mVisibleBoard[coords[0]][coords[1]] = 0;
									mFlags++;
								}
								player.playSound(player, Sound.ITEM_BOOK_PAGE_TURN, SoundCategory.PLAYERS, 1.0f, 1.6f);
							} else if (mPrepareLeftClick && mVisibleBoard[coords[0]][coords[1]] == 0) {
								// User clicked an unrevealed square on the board
								if (mGameBoard[coords[0]][coords[1]] == -1) {
									// Clicked mine procedure
									mLives--;
									player.playSound(player, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.0f, 1.0f);
									new PartialParticle(Particle.EXPLOSION_HUGE, loc.toLocation(player.getWorld()), 3).spawnForPlayer(ParticleCategory.OWN_ACTIVE, player);

									if (mLives == 0) {
										fishingManager.minigameFailure(player);
										this.cancel();
										return;
									}

									// Flag a mine that was just clicked automatically
									mVisibleBoard[coords[0]][coords[1]] = -1;
									mFlags--;
								} else {
									// User clicked a 0-8
									mVisibleBoard[coords[0]][coords[1]] = 1;
									if (mGameBoard[coords[0]][coords[1]] == 0) {
										propagateZeroesAround(coords[0], coords[1]);
									}
									if (checkWin()) {
										fishingManager.minigameSuccess(player);
										this.cancel();
										return;
									}
									player.playSound(player, Sound.BLOCK_TRIPWIRE_CLICK_ON, SoundCategory.PLAYERS, 1.0f, 2.0f);
								}
							} else {
								player.playSound(player, Sound.BLOCK_TRIPWIRE_DETACH, SoundCategory.PLAYERS, 1.0f, 1.6f);
							}

							break;
						}
					}

					mPrepareRightClick = false;
					mPrepareLeftClick = false;
				}

				if (mTicks % 5 == 1) {
					// Draw board
					drawRectangle(bottomCorner, planeVectorX, planeVectorY, BOARD_SIZE, BOARD_SIZE, 20, player);
					drawGrid(bottomCorner, planeVectorX, planeVectorY, player);
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

					for (int i = 0; i < mGameBoard.length; i++) {
						for (int j = 0; j < mGameBoard[0].length; j++) {
							Location adjustedBottomCorner = bottomCorner.clone().add(planeVectorX.clone().multiply(BOARD_SIZE / (2 * GRID_DIMENSIONS))).add(planeVectorY.clone().multiply(BOARD_SIZE / (2 * GRID_DIMENSIONS)));
							Location centre = adjustedBottomCorner.add(planeVectorX.clone().multiply(i)).add(planeVectorY.clone().multiply(j));
							if (mVisibleBoard[i][j] == -1) {
								drawRedX(centre, planeVectorX, planeVectorY, player);
							} else if (mVisibleBoard[i][j] == 1) {
								drawNumberOnGrid(centre, mGameBoard[i][j], player);
							}
						}
					}

					// Draw info panel
					drawRedX(mineCountLocation, planeVectorX, planeVectorY, player);
					ParticleUtils.drawSevenSegmentDigit(Math.max(mFlags, 0), mineCountLocation.clone().subtract(planeVectorX.clone().multiply(1.4)), player, 0.55, Particle.REDSTONE, new Particle.DustOptions(Color.LIME, 0.5f));
					new PartialParticle(Particle.HEART, lifeCountLocation, 2).delta(0.1).spawnForPlayer(ParticleCategory.OWN_ACTIVE, player);
					ParticleUtils.drawSevenSegmentDigit(mLives, lifeCountLocation.clone().subtract(planeVectorX.clone().multiply(1.4)), player, 0.55, Particle.REDSTONE, new Particle.DustOptions(Color.LIME, 0.5f));
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
		centre.setY(centre.getY() + 3 * BOARD_SIZE / 5);
		mDirection = player.getEyeLocation().clone().add(0, 1.5, 0).subtract(centre).toVector().normalize();
		centre.subtract(mDirection.clone().multiply(2));
		Vector planeVectorX = mDirection.clone().crossProduct(new Vector(0, 1, 0)).normalize(); // Left pointing
		Vector planeVectorY = planeVectorX.clone().crossProduct(mDirection).normalize();
		Location progressBottomCorner = centre.clone().add(planeVectorY.clone().multiply(PROGRESS_SEPARATION + BOARD_SIZE / 2).subtract(planeVectorX.clone().multiply(PROGRESS_WIDTH / 2)));
		Location bottomCorner = centre.clone().subtract(planeVectorX.clone().multiply(BOARD_SIZE / 2)).subtract(planeVectorY.clone().multiply(BOARD_SIZE / 2));
		drawRectangle(bottomCorner, planeVectorX, planeVectorY, BOARD_SIZE, BOARD_SIZE, 20, player);
		drawRectangle(progressBottomCorner, planeVectorX, planeVectorY, PROGRESS_WIDTH, PROGRESS_HEIGHT, 16, player);
		drawGrid(bottomCorner, planeVectorX, planeVectorY, player);
	}

	@Override
	protected void onLeftClick() {
		mPrepareLeftClick = true;
	}

	@Override
	protected void onRightClick() {
		mPrepareRightClick = true;
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

	private void drawGrid(Location bottomCorner, Vector planeVectorX, Vector planeVectorY, Player player) {
		for (double d = BOARD_SIZE / GRID_DIMENSIONS; d < BOARD_SIZE; d += BOARD_SIZE / GRID_DIMENSIONS) {
			for (double e = BOARD_SIZE / 20; e < BOARD_SIZE; e += BOARD_SIZE / 20) {
				drawParticle(bottomCorner.clone().add(planeVectorX.clone().multiply(d)).add(planeVectorY.clone().multiply(e)), player, UI_COLOR);
				drawParticle(bottomCorner.clone().add(planeVectorY.clone().multiply(d)).add(planeVectorX.clone().multiply(e)), player, UI_COLOR);
			}
		}
	}

	private void drawNumberOnGrid(Location centre, int digit, Player player) {
		ParticleUtils.drawSevenSegmentDigit(digit, centre, player, 0.32, Particle.REDSTONE, new Particle.DustOptions(DIGIT_COLORS[digit], 0.6f));
	}

	private void drawRedX(Location centre, Vector planeVectorX, Vector planeVectorY, Player player) {
		Vector xDiff = planeVectorX.clone().multiply(BOARD_SIZE / (2.5 * GRID_DIMENSIONS));
		Vector yDiff = planeVectorY.clone().multiply(BOARD_SIZE / (2.5 * GRID_DIMENSIONS));
		Location topLeft = centre.clone().add(xDiff).add(yDiff);
		Location botLeft = centre.clone().add(xDiff).subtract(yDiff);
		Location topRight = centre.clone().subtract(xDiff).add(yDiff);
		Location botRight = centre.clone().subtract(xDiff).subtract(yDiff);

		new PPLine(Particle.REDSTONE, topLeft, botRight).countPerMeter(8).data(new Particle.DustOptions(Color.RED, 0.6f)).count(2).minimumCount(0).spawnForPlayer(ParticleCategory.OWN_ACTIVE, player);
		new PPLine(Particle.REDSTONE, topRight, botLeft).countPerMeter(8).data(new Particle.DustOptions(Color.RED, 0.6f)).count(2).minimumCount(0).spawnForPlayer(ParticleCategory.OWN_ACTIVE, player);
	}

	private void generateBoard() {
		boolean generatedWithInfo = false;
		List<Integer> boardPositions = new ArrayList<>();
		for (int i = 0; i < GRID_DIMENSIONS * GRID_DIMENSIONS; i++) {
			mGameBoard[i % GRID_DIMENSIONS][i / GRID_DIMENSIONS] = 0;
			mVisibleBoard[i % GRID_DIMENSIONS][i / GRID_DIMENSIONS] = 0;
			boardPositions.add(i);
		}

		Collections.shuffle(boardPositions);
		for (int i = 0; i < GRID_DIMENSIONS * GRID_DIMENSIONS; i++) {
			if (i < MINE_COUNT) {
				int minePosition = boardPositions.get(i);
				mGameBoard[minePosition % GRID_DIMENSIONS][minePosition / GRID_DIMENSIONS] = -1;
				for (int j = 0; j < 8; j++) {
					int directionX = (int) FastMath.signum((float) Math.round(FastUtils.cos(j * Math.PI / 4)));
					int directionY = (int) FastMath.signum((float) Math.round(FastUtils.sin(j * Math.PI / 4)));
					int xCoord = (minePosition % GRID_DIMENSIONS) + directionX;
					int yCoord = (minePosition / GRID_DIMENSIONS) + directionY;
					if (xCoord < 0 || xCoord >= GRID_DIMENSIONS || yCoord < 0 || yCoord >= GRID_DIMENSIONS || mGameBoard[xCoord][yCoord] == -1) {
						continue;
					} else {
						mGameBoard[xCoord][yCoord]++;
					}
				}
			} else {
				int boardPosition = boardPositions.get(i);
				if (mGameBoard[boardPosition % GRID_DIMENSIONS][boardPosition / GRID_DIMENSIONS] == 0) {
					// Give starting info for the player to work with
					mVisibleBoard[boardPosition % GRID_DIMENSIONS][boardPosition / GRID_DIMENSIONS] = 1;
					propagateZeroesAround(boardPosition % GRID_DIMENSIONS, boardPosition / GRID_DIMENSIONS);
					generatedWithInfo = true;

					if (checkWin()) {
						generateBoard();
					}
					break;
				}
			}
		}

		if (!generatedWithInfo) {
			generateBoard();
		}
	}

	private void propagateZeroesAround(int x, int y) {
		for (int j = 0; j < 8; j++) {
			int directionX = (int) FastMath.signum((float) Math.round(FastUtils.cos(j * Math.PI / 4)));
			int directionY = (int) FastMath.signum((float) Math.round(FastUtils.sin(j * Math.PI / 4)));
			int xCoord = x + directionX;
			int yCoord = y + directionY;
			if (xCoord < 0 || xCoord >= GRID_DIMENSIONS || yCoord < 0 || yCoord >= GRID_DIMENSIONS || mVisibleBoard[xCoord][yCoord] != 0) {
				continue;
			} else {
				mVisibleBoard[xCoord][yCoord] = 1;
				if (mGameBoard[xCoord][yCoord] == 0) {
					propagateZeroesAround(xCoord, yCoord);
				}
			}
		}
	}

	private boolean checkWin() {
		for (int i = 0; i < GRID_DIMENSIONS; i++) {
			for (int j = 0; j < GRID_DIMENSIONS; j++) {
				if (mGameBoard[i][j] >= 0 && mVisibleBoard[i][j] != 1) {
					return false;
				}
			}
		}
		return true;
	}
}
