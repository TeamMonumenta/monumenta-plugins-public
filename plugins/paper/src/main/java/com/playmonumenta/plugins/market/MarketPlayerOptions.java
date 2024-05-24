package com.playmonumenta.plugins.market;

import org.jetbrains.annotations.Nullable;

public class MarketPlayerOptions {

	public enum NotificationShard {
		ALWAYS("Always sent"),
		OVERWORLD("Only in plots or overworld shards"),
		PLOTS("Only in plots shard"),
		NEVER("Never sent");

		final String mShortDisplay;

		NotificationShard(String shortDisplay) {
			this.mShortDisplay = shortDisplay;
		}

		public String getShortDisplay() {
			return this.mShortDisplay;
		}
	}


	// option that handles the shards in which the market notification message is displayed
	@Nullable NotificationShard mShardsForNotification;

	public NotificationShard getShardsForNotification() {
		if (mShardsForNotification == null) {
			return NotificationShard.OVERWORLD;
		}
		return mShardsForNotification;
	}

	public void setmShardsForNotification(@Nullable NotificationShard shardsForNotification) {
		this.mShardsForNotification = shardsForNotification;
	}
}
