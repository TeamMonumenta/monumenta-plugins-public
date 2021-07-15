package com.playmonumenta.plugins.depths;

import org.bukkit.Location;
import org.bukkit.util.Vector;

public class DepthsRoom {

	public DepthsRoomType mRoomType;
	public transient Vector mSize;
	public Vector mEntry;
	public int mSpawnerCount;
	public String mLoadPath;

	//Which way the door goes for the room, to make sure players don't go off into the void
	public RoomDirection mDirection;

	public DepthsRoom(String path, DepthsRoomType type, Vector size, Location entry, int spawnerCount, RoomDirection direction) {
		mLoadPath = path;
		mRoomType = type;
		mSize = size;
		mEntry = new Vector(entry.getX(), entry.getY(), entry.getZ());
		mSpawnerCount = spawnerCount;
		mDirection = direction;
	}

	public enum RoomDirection {
		UP, DOWN, EVEN;
	}

}
