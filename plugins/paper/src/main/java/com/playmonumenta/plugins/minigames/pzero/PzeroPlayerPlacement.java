package com.playmonumenta.plugins.minigames.pzero;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class PzeroPlayerPlacement implements Comparable<PzeroPlayerPlacement> {
	public final Player mPlayer;
	public final int mLap;
	public final int mCheckpoint;
	public final double mDistance;
	public final boolean mHasCrossedFinishLine;
	public final boolean mAfkKicked;

	public int mPlacement;
	public boolean mHasFinished = false;
	public int mFinalTimerTicks = 0;

	public PzeroPlayerPlacement(Player player, int lap, int checkpoint, double distance, boolean hasCrossedFinishLine) {
		this(player, lap, checkpoint, distance, hasCrossedFinishLine, 0, false);
	}

	public PzeroPlayerPlacement(Player player, int placement) {
		this(player, 0, 0, 0, false, placement, false);
	}

	public PzeroPlayerPlacement(Player player, int placement, boolean afkKicked) {
		this(player, 0, 0, 0, false, placement, afkKicked);
	}

	public PzeroPlayerPlacement(Player player, int lap, int checkpoint, double distance, boolean hasCrossedFinishLine, int placement, boolean afkKicked) {
		mPlayer = player;
		mLap = lap;
		mCheckpoint = checkpoint;
		mDistance = distance;
		mPlacement = placement;
		mHasCrossedFinishLine = hasCrossedFinishLine;
		mAfkKicked = afkKicked;
	}

	@Override
	public int compareTo(@NotNull PzeroPlayerPlacement o) {
		// Lap > Checkpoint > Lower distance to next checkpoint
		if (mLap == o.mLap) {
			if (mCheckpoint == o.mCheckpoint) {
				if (mDistance == o.mDistance) {
					return 0;
				}
				return mDistance > o.mDistance ? 1 : -1;
			} else {
				return mCheckpoint > o.mCheckpoint ? -1 : 1;
			}
		} else {
			return mLap > o.mLap ? -1 : 1;
		}
	}
}
