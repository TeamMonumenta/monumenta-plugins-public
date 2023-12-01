package com.playmonumenta.plugins.minigames.pzero;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.util.Vector;

public class PzeroCheckpointBuilder {
	private final ArrayList<PzeroCheckpoint> mCheckpoints = new ArrayList<>();
	private int mCurrentCheckpoint = 0;

	public PzeroCheckpointBuilder add(double x, double y, double z, double dx, double dy, double dz) {
		mCheckpoints.add(new PzeroCheckpoint(mCurrentCheckpoint, new Vector(x, y, z), new Vector(dx, dy, dz)));
		mCurrentCheckpoint++;
		return this;
	}

	public List<PzeroCheckpoint> build() {
		return List.copyOf(mCheckpoints);
	}
}
