package com.playmonumenta.plugins.gallery;

import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * Enum of different gallery maps, we can store info unique to each map here
 */
public enum GalleryMap {
	SANGUINE_HALLS(1, "~Gallery1All", "~Gallery1Elite", "~Gallery1Phantom", "gallery", Collections.emptyList()),
	MARINA_NOIR(2, "~Gallery2All", "~Gallery2Elite", "~Gallery2Phantom", "marina", List.of("MN_C1", "MN_C3", "MN_C10", "MN_C5", "MN_C2"));

	private final int mIndex;
	private final String mLosPool;
	private final String mElitePool;
	private final String mSpectersPool;
	private final String mNamespace;
	private final List<String> mValidStartingChests;

	GalleryMap(int index, String losPool, String elitePool, String spectersPool, String namespace, List<String> validStartingChests) {
		mIndex = index;
		mLosPool = losPool;
		mElitePool = elitePool;
		mSpectersPool = spectersPool;
		mNamespace = namespace;
		mValidStartingChests = validStartingChests;
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

	public String getNamespace() {
		return mNamespace;
	}

	public List<String> getValidStartingChests() {
		return mValidStartingChests;
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
