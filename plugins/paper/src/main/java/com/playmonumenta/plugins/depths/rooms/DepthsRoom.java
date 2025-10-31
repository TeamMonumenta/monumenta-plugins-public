package com.playmonumenta.plugins.depths.rooms;

import org.bukkit.util.Vector;

public class DepthsRoom {

	public DepthsRoomType mRoomType;
	public Vector mSize;
	public Vector mEntry;
	public int mSpawnerCount;
	public String mLoadPath;

	//Which way the door goes for the room, to make sure players don't go off into the void
	public RoomDirection mDirection;

	public DepthsRoom(String path, DepthsRoomType type, Vector size, Vector entry, int spawnerCount, RoomDirection direction) {
		mLoadPath = path;
		mRoomType = type;
		mSize = size;
		mEntry = entry;
		mSpawnerCount = spawnerCount;
		mDirection = direction;
	}

	public String getRoomCode() {
		String[] splits = mLoadPath.split("/");
		return splits[splits.length - 1];
	}

	public enum RoomDirection {
		UP, DOWN, EVEN
	}

}
