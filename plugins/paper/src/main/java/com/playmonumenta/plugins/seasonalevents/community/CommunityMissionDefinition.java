package com.playmonumenta.plugins.seasonalevents.community;

import com.playmonumenta.plugins.seasonalevents.community.CommunityEvent.TieredRewardSchema;
import org.jetbrains.annotations.Nullable;

public class CommunityMissionDefinition {
	public final CommunityMissionType mType;

	// goals
	public final int mGoalTier1;
	public final int mGoalTier2;
	public final int mGoalTier3;
	public final int mContribTier1;
	public final int mContribTier2;

	// rewards per tier
	public final @Nullable String mLootT1;
	public final int mAmountT1;
	public final @Nullable String mLootT2;
	public final int mAmountT2;
	public final @Nullable String mLootT3;
	public final int mAmountT3;

	// constructor for the missions themselves, all default
	public CommunityMissionDefinition(CommunityMissionType type) {
		this(type, type.mGoalTier1, type.mGoalTier2, type.mGoalTier3, type.mContribTier1, type.mContribTier2, null);
	}

	// constructor for custom goals, default rewards
	public CommunityMissionDefinition(CommunityMissionType type, int g1, int g2, int g3, int c1, int c2) {
		this(type, g1, g2, g3, c1, c2, null);
	}

	// constructor for default goals, custom rewards
	public CommunityMissionDefinition(CommunityMissionType type, TieredRewardSchema rewards) {
		this(type, type.mGoalTier1, type.mGoalTier2, type.mGoalTier3, type.mContribTier1, type.mContribTier2, rewards);
	}

	// master constructor
	public CommunityMissionDefinition(CommunityMissionType type, int g1, int g2, int g3, int c1, int c2, @Nullable TieredRewardSchema rewards) {
		this.mType = type;
		this.mGoalTier1 = g1;
		this.mGoalTier2 = g2;
		this.mGoalTier3 = g3;
		this.mContribTier1 = c1;
		this.mContribTier2 = c2;

		if (rewards != null) {
			this.mLootT1 = rewards.mLootT1;
			this.mAmountT1 = rewards.mAmountT1;
			this.mLootT2 = rewards.mLootT2;
			this.mAmountT2 = rewards.mAmountT2;
			this.mLootT3 = rewards.mLootT3;
			this.mAmountT3 = rewards.mAmountT3;
		} else {
			// nulls = event will generate
			this.mLootT1 = null;
			this.mAmountT1 = 0;
			this.mLootT2 = null;
			this.mAmountT2 = 0;
			this.mLootT3 = null;
			this.mAmountT3 = 0;
		}
	}

	// internal constructor for withRewards
	private CommunityMissionDefinition(CommunityMissionDefinition original, TieredRewardSchema rewards) {
		this.mType = original.mType;
		this.mGoalTier1 = original.mGoalTier1;
		this.mGoalTier2 = original.mGoalTier2;
		this.mGoalTier3 = original.mGoalTier3;
		this.mContribTier1 = original.mContribTier1;
		this.mContribTier2 = original.mContribTier2;
		this.mLootT1 = rewards.mLootT1;
		this.mAmountT1 = rewards.mAmountT1;
		this.mLootT2 = rewards.mLootT2;
		this.mAmountT2 = rewards.mAmountT2;
		this.mLootT3 = rewards.mLootT3;
		this.mAmountT3 = rewards.mAmountT3;
	}

	public CommunityMissionDefinition withRewards(TieredRewardSchema defaultRewards) {
		// if theres custom rewards, keep them instead
		if (this.mLootT1 != null) {
			return this;
		}
		return new CommunityMissionDefinition(this, defaultRewards);
	}
}
