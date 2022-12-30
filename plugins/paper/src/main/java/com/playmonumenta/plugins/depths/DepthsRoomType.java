package com.playmonumenta.plugins.depths;

import net.md_5.bungee.api.ChatColor;
import org.jetbrains.annotations.Nullable;

public enum DepthsRoomType {
	ABILITY(DepthsRewardType.ABILITY, "Ability", "Ability"),
	ABILITY_ELITE(DepthsRewardType.ABILITY_ELITE, "Ability", "Elite Ability"),
	UPGRADE(DepthsRewardType.UPGRADE, "Upgrade", "Upgrade"),
	UPGRADE_ELITE(DepthsRewardType.UPGRADE_ELITE, "Upgrade", "Elite Upgrade"),
	TREASURE(null, "Treasure", "Treasure"),
	TREASURE_ELITE(null, "Treasure", "Elite Treasure"),
	UTILITY(null, "", "Utility"),
	BOSS(null, "", "Boss"),
	TWISTED(DepthsRewardType.TWISTED, ChatColor.MAGIC + "XXXXXX" + ChatColor.LIGHT_PURPLE, ChatColor.MAGIC + "XXXXXX" + ChatColor.LIGHT_PURPLE);

	private final @Nullable DepthsRewardType mRewardType;
	private final String mRewardString;
	private final String mRoomString;

	DepthsRoomType(@Nullable DepthsRewardType rewardType, String rewardString, String roomString) {
		mRewardType = rewardType;
		mRewardString = rewardString;
		mRoomString = roomString;
	}

	public @Nullable DepthsRewardType getRewardType() {
		return mRewardType;
	}

	public String getRewardString() {
		return mRewardString;
	}

	public String getRoomString() {
		return mRoomString;
	}

	public enum DepthsRewardType {
		ABILITY, ABILITY_ELITE, UPGRADE, UPGRADE_ELITE, TWISTED;
	}
}

