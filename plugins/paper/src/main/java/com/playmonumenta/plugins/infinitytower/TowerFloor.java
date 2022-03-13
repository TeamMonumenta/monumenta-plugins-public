package com.playmonumenta.plugins.infinitytower;

import com.google.gson.JsonObject;
import org.bukkit.util.Vector;

public class TowerFloor {
	protected final Vector mVector;
	protected final int mXSize;
	protected final int mZSize;

	protected int mMin = 0;
	protected int mMax = Integer.MAX_VALUE;

	public TowerFloor(Vector vector, int x, int z) {
		mVector = vector;
		mXSize = x;
		mZSize = z;
	}

	public static TowerFloor fromJson(JsonObject object) {

		double x = object.getAsJsonPrimitive("x").getAsDouble();
		double y = object.getAsJsonPrimitive("y").getAsDouble();
		double z = object.getAsJsonPrimitive("z").getAsDouble();

		int sizeX = object.getAsJsonPrimitive("xSize").getAsInt();
		int sizeZ = object.getAsJsonPrimitive("zSize").getAsInt();
		TowerFloor floor = new TowerFloor(new Vector(x, y, z), sizeX, sizeZ);

		if (object.getAsJsonPrimitive("min") != null) {
			floor.mMin = object.getAsJsonPrimitive("min").getAsInt();
		}

		if (object.getAsJsonPrimitive("max") != null) {
			floor.mMax = object.getAsJsonPrimitive("max").getAsInt();
		}

		return floor;

	}

}
