package com.playmonumenta.plugins.depths.rooms;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.jetbrains.annotations.Nullable;

public enum DepthsRoomType {
	/* Unfortunately, this enum is polluted with Components because ChatColor is deprecated. This has knock-on
	 * effects on a bunch of other Depths methods that I have tried to fully address */
	ABILITY(DepthsRewardType.ABILITY, Component.text("Ability"), Component.text("Ability")),
	ABILITY_ELITE(DepthsRewardType.ABILITY_ELITE, Component.text("Ability"), Component.text("Elite Ability")),
	UPGRADE(DepthsRewardType.UPGRADE, Component.text("Upgrade"), Component.text("Upgrade")),
	UPGRADE_ELITE(DepthsRewardType.UPGRADE_ELITE, Component.text("Upgrade"), Component.text("Elite Upgrade")),
	TREASURE(null, Component.text("Treasure"), Component.text("Treasure")),
	TREASURE_ELITE(null, Component.text("Treasure"), Component.text("Elite Treasure")),
	UTILITY(null, Component.text(""), Component.text("Utility")),
	WILDCARD(null, Component.text(""), Component.text("Wildcard")),
	BOSS(null, Component.text(""), Component.text("Boss")),
	TWISTED(DepthsRewardType.TWISTED, Component.text("XXXXXX", NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.OBFUSCATED), Component.text("XXXXXX", NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.OBFUSCATED));

	private final @Nullable DepthsRewardType mRewardType;
	private final Component mRewardComponent;
	private final Component mRoomComponent;

	DepthsRoomType(@Nullable DepthsRewardType rewardType, Component rewardComponent, Component roomComponent) {
		mRewardType = rewardType;
		mRewardComponent = rewardComponent;
		mRoomComponent = roomComponent;
	}

	public @Nullable DepthsRewardType getRewardType() {
		return mRewardType;
	}

	public Component getRewardComponent() {
		return mRewardComponent;
	}

	public Component getRoomComponent() {
		return mRoomComponent;
	}

	public enum DepthsRewardType {
		ABILITY, ABILITY_ELITE, UPGRADE, UPGRADE_ELITE, TWISTED, PRISMATIC, GENEROSITY
	}
}

