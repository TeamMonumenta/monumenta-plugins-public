package com.playmonumenta.plugins.depths;

import com.playmonumenta.plugins.depths.rooms.DepthsRoomType.DepthsRewardType;

public class DepthsReward {

	public final DepthsRewardType mRewardType;
	public final int mForceLevel; // 0 for not forced

	public DepthsReward(DepthsRewardType rewardType, int forceLevel) {
		mRewardType = rewardType;
		mForceLevel = forceLevel;
	}
}
