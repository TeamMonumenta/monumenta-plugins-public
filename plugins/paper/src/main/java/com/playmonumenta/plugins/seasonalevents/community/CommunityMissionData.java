package com.playmonumenta.plugins.seasonalevents.community;

import org.jetbrains.annotations.Nullable;

public class CommunityMissionData {
	public final CommunityMissionDefinition mDef;
	public final CommunityMissionType mType;
	public final long mTotalContribution;
	public final long mPersonalContribution;

	public int mRank = -1;
	public final long mTotalParticipants;
	public boolean mIsTop25 = false;
	public boolean mIsTop10 = false;

	public CommunityMissionData(CommunityMissionDefinition def, long total, long personal, @Nullable Long rankIndex, Long totalParticipants) {
		this.mDef = def;
		this.mType = def.mType;
		this.mTotalContribution = total;
		this.mPersonalContribution = personal;
		this.mTotalParticipants = (totalParticipants != null) ? totalParticipants : 0;

		if (personal > 0 && rankIndex != null && this.mTotalParticipants > 0) {
			this.mRank = rankIndex.intValue() + 1;
			boolean qualified = personal >= def.mContribTier2;

			if (qualified) {
				int top10Count = (int) Math.ceil(this.mTotalParticipants * 0.1);
				int top25Count = (int) Math.ceil(this.mTotalParticipants * 0.25);

				this.mIsTop10 = rankIndex < top10Count;
				this.mIsTop25 = rankIndex < top25Count;
			} else {
				this.mIsTop10 = false;
				this.mIsTop25 = false;
			}
		}
	}

	public double getRewardMultiplier() {
		if (mPersonalContribution < mDef.mContribTier1) {
			return 0.0;
		}
		if (mPersonalContribution < mDef.mContribTier2) {
			return 0.5;
		}
		if (mIsTop10) {
			return 2.0;
		}
		if (mIsTop25) {
			return 1.5;
		}
		return 1.0;
	}

	public int getLeaderboardPoints() {
		if (mPersonalContribution < mDef.mContribTier2) {
			return 0;
		}

		if (mRank == 1) {
			return 15;
		}
		if (mRank == 2) {
			return 12;
		}
		if (mRank == 3) {
			return 10;
		}
		if (mRank == 4) {
			return 8;
		}
		if (mRank == 5) {
			return 7;
		}
		if (mRank > 0 && mRank <= 10) {
			return 5;
		}
		if (mIsTop10) {
			return 3;
		}
		if (mIsTop25) {
			return 1;
		}
		return 0;
	}
}
