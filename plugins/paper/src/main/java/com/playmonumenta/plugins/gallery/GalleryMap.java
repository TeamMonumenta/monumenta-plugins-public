package com.playmonumenta.plugins.gallery;

import org.jetbrains.annotations.Nullable;

/**
 * Enum of different gallery maps, we can store info unique to each map here
 */
public enum GalleryMap {
	SANGUINE_HALLS(1, "~gallery1all", "~Gallery1Elite", "~galleryphantom");

	private final int mIndex;
	private final String mLosPool;
	private final String mElitePool;
	private final String mSpectersPool;

	GalleryMap(int index, String losPool, String elitePool, String spectersPool) {
		mIndex = index;
		mLosPool = losPool;
		mElitePool = elitePool;
		mSpectersPool = spectersPool;
	}

	public int getIndex() {
		return mIndex;
	}

	public String getLosPool() {
		return mLosPool;
	}

	public String getElitePool() {
		return mElitePool;
	}

	public String getSpectersPool() {
		return mSpectersPool;
	}

	public static @Nullable GalleryMap fromID(int id) {
		for (GalleryMap map : values()) {
			if (map.mIndex == id) {
				return map;
			}
		}
		return null;
	}

	public static @Nullable GalleryMap fromName(String name) {
		for (GalleryMap map : values()) {
			if (map.name().equals(name)) {
				return map;
			}
		}
		return null;
	}

}
