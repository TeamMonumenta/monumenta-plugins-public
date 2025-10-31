package com.playmonumenta.plugins.depths;

import com.playmonumenta.plugins.depths.rooms.DepthsRoomType.DepthsRewardType;

/**
 * A record of what kind of reward this room gives
 *
 * @param mRewardType What type of reward this room gives
 * @param mForceLevel 0 for not forced
 */
public record DepthsReward(DepthsRewardType mRewardType, int mForceLevel) {

}
