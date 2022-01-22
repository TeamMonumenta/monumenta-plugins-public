package com.playmonumenta.plugins.commands.experiencinator;

import com.playmonumenta.plugins.utils.ItemStatUtils.Region;
import com.playmonumenta.plugins.utils.ItemStatUtils.Tier;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import org.bukkit.entity.Player;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

/**
 * Handles the Experiencinator settings stored as packed decimals in scoreboards.
 * <p>
 * Format: Each digit of the (decimal) value is the "settings_id" of the selected conversion for the tier corresponding to that digit.
 * The least significant digit is the first tier in "scoreboards.tier_order", with further tiers in increasingly significant digits (up to 9)
 */
public final class ExperiencinatorSettings {

	private final ExperiencinatorConfig.ScoreboardConfig mScoreboardConfig;
	private final Player mPlayer;
	private final Map<Region, Integer> mPacked = new EnumMap<>(Region.class);

	public ExperiencinatorSettings(ExperiencinatorConfig.ScoreboardConfig scoreboardConfig, Player player) {
		mScoreboardConfig = scoreboardConfig;
		mPlayer = player;
		boolean hadScoreboadValues = false;
		for (Map.Entry<Region, String> entry : scoreboardConfig.getObjectives().entrySet()) {
			Optional<Integer> scoreboardValue = ScoreboardUtils.getScoreboardValue(player, entry.getValue());
			if (scoreboardValue.isPresent()) {
				hadScoreboadValues = true;
				mPacked.put(entry.getKey(), scoreboardValue.get());
			} else {
				mPacked.put(entry.getKey(), 0);
			}
		}
		if (!hadScoreboadValues) {
			setupNewPlayer(player);
		}
	}

	public void setConversion(Region region, Tier itemTier, int conversionId) {
		Integer packed = mPacked.get(region);
		int tier = mScoreboardConfig.getTierOrder().indexOf(itemTier);
		if (packed == null || tier < 0) { // this should not happen if everything is properly configured.
			return;
		}

		int digit = 1;
		for (int i = 0; i < tier; i++) {
			digit *= 10;
		}

		int newPacked = packed
				- ((packed % (10 * digit)) / digit) * digit // remove existing setting
				+ digit * conversionId; // add new setting

		mPacked.put(region, newPacked);

		String objectiveName = mScoreboardConfig.getObjectives().get(region);
		if (objectiveName != null) { // should not be null if everything is properly configured.
			ScoreboardUtils.setScoreboardValue(mPlayer, objectiveName, newPacked);
		}
	}

	public int getConversion(Region region, Tier itemTier) {
		Integer packed = mPacked.get(region);
		int tier = mScoreboardConfig.getTierOrder().indexOf(itemTier);
		if (packed == null || tier < 0) { // this should not happen if everything is properly configured.
			return 0;
		}

		int digit = 1;
		for (int i = 0; i < tier; i++) {
			digit *= 10;
		}

		return (packed % (10 * digit)) / digit;
	}

	/**
	 * Migrates settings from existing tags if any exist, or sets default values for a new player. Does not clear tags for now.
	 */
	private void setupNewPlayer(Player player) {
		setConversion(Region.VALLEY, Tier.I, ScoreboardUtils.checkTag(player, "NoT1") ? 0 : 1);
		setConversion(Region.VALLEY, Tier.II, ScoreboardUtils.checkTag(player, "NoT2") ? 0 : 1);
		setConversion(Region.VALLEY, Tier.III, ScoreboardUtils.checkTag(player, "NoT3") ? 0 : 1);
		setConversion(Region.VALLEY, Tier.IV, ScoreboardUtils.checkTag(player, "NoT4") ? 0 : 1);
		setConversion(Region.VALLEY, Tier.V, ScoreboardUtils.checkTag(player, "NoT5") ? 0 : 1);
		setConversion(Region.VALLEY, Tier.UNCOMMON, ScoreboardUtils.checkTag(player, "NoU") ? 0 : 1);

		setConversion(Region.ISLES, Tier.I, ScoreboardUtils.checkTag(player, "2NoT1") ? 0 : 1);
		setConversion(Region.ISLES, Tier.II, ScoreboardUtils.checkTag(player, "2NoT2") ? 0 : 1);
		setConversion(Region.ISLES, Tier.III, ScoreboardUtils.checkTag(player, "2NoT3") ? 0 : 1);
		setConversion(Region.ISLES, Tier.IV, ScoreboardUtils.checkTag(player, "2NoT4") ? 0 : 1);
		setConversion(Region.ISLES, Tier.V, ScoreboardUtils.checkTag(player, "2NoT5") ? 0 : 1);
		setConversion(Region.ISLES, Tier.UNCOMMON, ScoreboardUtils.checkTag(player, "2NoU") ? 0 : 1);
	}

}
