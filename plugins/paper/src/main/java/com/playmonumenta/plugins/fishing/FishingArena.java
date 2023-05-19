package com.playmonumenta.plugins.fishing;

import com.google.common.collect.ImmutableList;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

enum FishingArena {
	ARENA_1(27, new Vector(-1918.5, 193, -1258.5),
		ImmutableList.of(
			new Vector(-1931.5, 193, -1262.5),
			new Vector(-1930.5, 193, -1265.5),
			new Vector(-1928.5, 193, -1266.5),
			new Vector(-1925.5, 193, -1273.5),
			new Vector(-1923.5, 194, -1271.5),
			new Vector(-1919.5, 193, -1275.5),
			new Vector(-1921.5, 193, -1267.5),
			new Vector(-1902.5, 193, -1266.5),
			new Vector(-1902.5, 193, -1261.5),
			new Vector(-1908.5, 193, -1262.5),
			new Vector(-1911.5, 193, -1259.5),
			new Vector(-1911.5, 194, -1251.5),
			new Vector(-1908.5, 193, -1248.5),
			new Vector(-1914.5, 193.5, -1244.5),
			new Vector(-1918.5, 194, -1241.5),
			new Vector(-1920.5, 193, -1245.5),
			new Vector(-1925.5, 193, -1243.5)
		)),
	ARENA_2(25, new Vector(-2358.5, 197, -1255.5),
		ImmutableList.of(
			new Vector(-2345.5, 197, -1253.5),
			new Vector(-2348.5, 198, -1262.5),
			new Vector(-2349.5, 197, -1264.5),
			new Vector(-2351.5, 197, -1263.5),
			new Vector(-2354.5, 198, -1265.5),
			new Vector(-2358.5, 197, -1265.5),
			new Vector(-2359.5, 198, -1269.5),
			new Vector(-2364.5, 197, -1264.5),
			new Vector(-2368.5, 198, -1261.5),
			new Vector(-2369.5, 197, -1256.5),
			new Vector(-2370.5, 197, -1250.5),
			new Vector(-2363.5, 197, -1244.5),
			new Vector(-2358.5, 198, -1243.5),
			new Vector(-2354.5, 197, -1246.5),
			new Vector(-2350.5, 197, -1244.5),
			new Vector(-2347.5, 198, -1247.5)
		)),
	ARENA_3(25, new Vector(-2345.5, 252, -1871.5),
		ImmutableList.of(
			new Vector(-2354.5, 253, -1878.5),
			new Vector(-2351.5, 253, -1880.5),
			new Vector(-2350.5, 253, -1877.5),
			new Vector(-2348.5, 253, -1878.5),
			new Vector(-2347.5, 253, -1875.5),
			new Vector(-2344.5, 253, -1878.5),
			new Vector(-2343.5, 253, -1874.5),
			new Vector(-2341.5, 253, -1876.5),
			new Vector(-2338.5, 253, -1877.5),
			new Vector(-2338.5, 253, -1874.5),
			new Vector(-2340.5, 253, -1871.5),
			new Vector(-2354.5, 253, -1874.5),
			new Vector(-2353.5, 253, -1882.5),
			new Vector(-2348.5, 253, -1881.5),
			new Vector(-2342.5, 253, -1879.5),
			new Vector(-2336.5, 253, -1880.5)
		));

	final Vector mCoordinates;
	final ImmutableList<Vector> mSummonCoordinates;
	final int mRadius;
	boolean mOccupied = false;
	boolean mActive = false;
	int mWave = 0;
	int mDifficulty = 0;
	@Nullable Location mOrigin = null;
	@Nullable Player mOwner = null;

	FishingArena(int radius, Vector coordinates, ImmutableList<Vector> summonCoordinates) {
		mCoordinates = coordinates;
		mSummonCoordinates = summonCoordinates;
		mRadius = radius;
	}
}
