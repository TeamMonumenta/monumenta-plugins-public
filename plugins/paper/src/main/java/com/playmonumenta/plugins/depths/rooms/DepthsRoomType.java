package com.playmonumenta.plugins.depths.rooms;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.Nullable;

public enum DepthsRoomType {
	ABILITY(DepthsRewardType.ABILITY, "Ability", "Ability"),
	ABILITY_ELITE(DepthsRewardType.ABILITY_ELITE, "Ability", "Elite Ability"),
	UPGRADE(DepthsRewardType.UPGRADE, "Upgrade", "Upgrade"),
	UPGRADE_ELITE(DepthsRewardType.UPGRADE_ELITE, "Upgrade", "Elite Upgrade"),
	TREASURE(null, "Treasure", "Treasure"),
	TREASURE_ELITE(null, "Treasure", "Elite Treasure"),
	UTILITY(null, "", "Utility"),
	WILDCARD(null, "", "Wildcard"),
	BOSS(null, "", "Boss"),
	// This line is awful but at least it gets rid of deprecation warnings from ChatColor
	TWISTED(DepthsRewardType.TWISTED, Component.text("XXXXXX", NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.OBFUSCATED).toString(), Component.text("XXXXXX", NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.OBFUSCATED).toString());

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
		ABILITY, ABILITY_ELITE, UPGRADE, UPGRADE_ELITE, TWISTED, PRISMATIC, GENEROSITY
	}
}

