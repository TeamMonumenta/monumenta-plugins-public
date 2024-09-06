package com.playmonumenta.plugins.bosses.spells.hexfall.ruten;

import com.playmonumenta.plugins.bosses.bosses.hexfall.Ruten;
import com.playmonumenta.plugins.bosses.spells.Spell;
import org.bukkit.Location;

public class SpellCreepingDeathSpread extends Spell {
	private static final Integer UPDATE_RATE_TICKS = 10;
	private final Ruten mRuten;
	private int mTicks = 0;
	private int mTicksUpdate = 0;
	private final Location mCenterLoc;
	private double mSpreadTicks;

	public SpellCreepingDeathSpread(Ruten ruten, Location startLoc) {
		mRuten = ruten;
		mCenterLoc = startLoc;
		mSpreadTicks = Math.sqrt(2 * mRuten.mDeathCount) * 1.5;
	}

	@Override
	public void run() {
		mSpreadTicks = Math.sqrt(2 * mRuten.mDeathCount) * 3;
		if (mTicks >= mSpreadTicks) {
			for (int i = 0; i < mRuten.mTendencyMatrix.length; i++) {
				for (int j = 0; j < mRuten.mTendencyMatrix[0].length; j++) {
					if (mRuten.mTendencyMatrix[i][j] == 1) {
						switch ((int) (Math.random() * 8)) {
							case 0:
								if (i - 1 >= 0 && mRuten.mTendencyMatrix[i - 1][j] == 2) {
									mRuten.mTendencyMatrix[i - 1][j] = 3;
								}
								break;
							case 1:
								if (i + 1 <= mRuten.mTendencyMatrix.length && mRuten.mTendencyMatrix[i + 1][j] == 2) {
									mRuten.mTendencyMatrix[i + 1][j] = 3;
								}
								break;
							case 2:
								if (j - 1 >= 0 && mRuten.mTendencyMatrix[i][j - 1] == 2) {
									mRuten.mTendencyMatrix[i][j - 1] = 3;
								}
								break;
							case 3:
								if (j + 1 <= mRuten.mTendencyMatrix[0].length && mRuten.mTendencyMatrix[i][j + 1] == 2) {
									mRuten.mTendencyMatrix[i][j + 1] = 3;
								}
								break;

							case 4:
								if (i - 1 >= 0 && mRuten.mTendencyMatrix[i - 1][j] == 2) {
									mRuten.mTendencyMatrix[i - 1][j] = 3;
									mRuten.mTendencyMatrix[i][j] = 4;
								}
								break;
							case 5:
								if (i + 1 <= mRuten.mTendencyMatrix.length && mRuten.mTendencyMatrix[i + 1][j] == 2) {
									mRuten.mTendencyMatrix[i + 1][j] = 3;
									mRuten.mTendencyMatrix[i][j] = 4;
								}
								break;
							case 6:
								if (j - 1 >= 0 && mRuten.mTendencyMatrix[i][j - 1] == 2) {
									mRuten.mTendencyMatrix[i][j - 1] = 3;
									mRuten.mTendencyMatrix[i][j] = 4;
								}
								break;
							case 7:
								if (j + 1 <= mRuten.mTendencyMatrix[0].length && mRuten.mTendencyMatrix[i][j + 1] == 2) {
									mRuten.mTendencyMatrix[i][j + 1] = 3;
									mRuten.mTendencyMatrix[i][j] = 4;
								}
								break;
							default:
								break;
						}
					}
				}
			}

			for (int i = 0; i < mRuten.mTendencyMatrix.length; i++) {
				for (int j = 0; j < mRuten.mTendencyMatrix[0].length; j++) {
					if (mRuten.mTendencyMatrix[i][j] == 3) {
						Ruten.modifyAnimaAtLocation(mCenterLoc.clone().add(i - Ruten.matrixCoordsFromCenterOffset, -1, j - Ruten.matrixCoordsFromCenterOffset), Ruten.AnimaTendency.DEATH);
						mRuten.mTendencyMatrix[i][j] = 1;
					}
					if (mRuten.mTendencyMatrix[i][j] == 4) {
						Ruten.modifyAnimaAtLocation(mCenterLoc.clone().add(i - Ruten.matrixCoordsFromCenterOffset, -1, j - Ruten.matrixCoordsFromCenterOffset), Ruten.AnimaTendency.LIFE);
						mRuten.mTendencyMatrix[i][j] = 2;
					}
				}
			}
			mTicks = 0;
		}
		mTicks++;


		if (mTicksUpdate >= UPDATE_RATE_TICKS) {
			Ruten.AnimaTendency tendency;
			mRuten.mDeathCount = 0;
			for (int i = 0; i < mRuten.mTendencyMatrix.length; i++) {
				for (int j = 0; j < mRuten.mTendencyMatrix[0].length; j++) {
					tendency = Ruten.getTendencyAtLocation(mCenterLoc.clone().add(i - Ruten.matrixCoordsFromCenterOffset, -1, j - Ruten.matrixCoordsFromCenterOffset).getBlock().getLocation());
					if (tendency == Ruten.AnimaTendency.LIFE) {
						mRuten.mTendencyMatrix[i][j] = 2;
					} else if (tendency == Ruten.AnimaTendency.DEATH) {
						mRuten.mDeathCount++;
						mRuten.mTendencyMatrix[i][j] = 1;
					} else {
						mRuten.mTendencyMatrix[i][j] = 0;
					}

				}
			}
			mTicksUpdate = 0;
		}
		mTicksUpdate++;
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
