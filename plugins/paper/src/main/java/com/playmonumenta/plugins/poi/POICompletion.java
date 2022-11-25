package com.playmonumenta.plugins.poi;

public class POICompletion {

	public POI mPOI;
	public boolean mIsCompleted;

	public POICompletion(POI poi, boolean completion) {
		mPOI = poi;
		mIsCompleted = completion;
	}

	public POI getPOI() {
		return mPOI;
	}

	public boolean isCompleted() {
		return mIsCompleted;
	}

	public void complete() {
		mIsCompleted = true;
	}
}
