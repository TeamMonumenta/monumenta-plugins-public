package com.playmonumenta.plugins.market;

public class MarketConfig {

	// represents the config for different variables used in the market plugin, that can be changed without waiting for a weekly update
	// this class is made so it can be loaded from a raw JSON, which is done when the shard boots up, or when the `/market reloadconfig` command is run
	// if the config fails to be loaded, the gui will not be accessible

	// whether the market is open or not
	public boolean mIsMarketOpen;

	/*
	 *
	 *   BAZAAR
	 *
	 */

	// represents the tax rate of bazaar listings. paid by the seller. 0.00 = 0%, 1.00 = 100%
	public double mBazaarTaxRate;

	// amount of days before a bazaar listing expires
	public int mDaysBeforeBazaarExpiry;

	// amount of listings the player can simultaneously have
	public int mAmountOfPlayerListingsSlots;

}
