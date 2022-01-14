package com.playmonumenta.plugins.commands.experiencinator;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import org.bukkit.entity.Player;

import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;

/**
 * Handles the Experiencinator settings stored as packed decimals in scoreboards.
 * <p>
 * Format: Each digit of the (decimal) value is the "settings_id" of the selected conversion for the tier corresponding to that digit.
 * The least significant digit is the first tier in "scoreboards.tier_order", with further tiers in increasingly significant digits (up to 9)
 */
public final class ExperiencinatorSettings {

	private final ExperiencinatorConfig.ScoreboardConfig mScoreboardConfig;
	private final Player mPlayer;
	private final Map<ItemUtils.ItemRegion, Integer> mPacked = new EnumMap<>(ItemUtils.ItemRegion.class);

	public ExperiencinatorSettings(ExperiencinatorConfig.ScoreboardConfig scoreboardConfig, Player player) {
		mScoreboardConfig = scoreboardConfig;
		mPlayer = player;
		boolean hadScoreboadValues = false;
		for (Map.Entry<ItemUtils.ItemRegion, String> entry : scoreboardConfig.getObjectives().entrySet()) {
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

	public void setConversion(ItemUtils.ItemRegion itemRegion, ItemUtils.ItemTier itemTier, int conversionId) {
		Integer packed = mPacked.get(itemRegion);
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

		mPacked.put(itemRegion, newPacked);

		String objectiveName = mScoreboardConfig.getObjectives().get(itemRegion);
		if (objectiveName != null) { // should not be null if everything is properly configured.
			ScoreboardUtils.setScoreboardValue(mPlayer, objectiveName, newPacked);
		}
	}

	public int getConversion(ItemUtils.ItemRegion itemRegion, ItemUtils.ItemTier itemTier) {
		Integer packed = mPacked.get(itemRegion);
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
		setConversion(ItemUtils.ItemRegion.KINGS_VALLEY, ItemUtils.ItemTier.ONE, ScoreboardUtils.checkTag(player, "NoT1") ? 0 : 1);
		setConversion(ItemUtils.ItemRegion.KINGS_VALLEY, ItemUtils.ItemTier.TWO, ScoreboardUtils.checkTag(player, "NoT2") ? 0 : 1);
		setConversion(ItemUtils.ItemRegion.KINGS_VALLEY, ItemUtils.ItemTier.THREE, ScoreboardUtils.checkTag(player, "NoT3") ? 0 : 1);
		setConversion(ItemUtils.ItemRegion.KINGS_VALLEY, ItemUtils.ItemTier.FOUR, ScoreboardUtils.checkTag(player, "NoT4") ? 0 : 1);
		setConversion(ItemUtils.ItemRegion.KINGS_VALLEY, ItemUtils.ItemTier.FIVE, ScoreboardUtils.checkTag(player, "NoT5") ? 0 : 1);
		setConversion(ItemUtils.ItemRegion.KINGS_VALLEY, ItemUtils.ItemTier.UNCOMMON, ScoreboardUtils.checkTag(player, "NoU") ? 0 : 1);

		setConversion(ItemUtils.ItemRegion.CELSIAN_ISLES, ItemUtils.ItemTier.ONE, ScoreboardUtils.checkTag(player, "2NoT1") ? 0 : 1);
		setConversion(ItemUtils.ItemRegion.CELSIAN_ISLES, ItemUtils.ItemTier.TWO, ScoreboardUtils.checkTag(player, "2NoT2") ? 0 : 1);
		setConversion(ItemUtils.ItemRegion.CELSIAN_ISLES, ItemUtils.ItemTier.THREE, ScoreboardUtils.checkTag(player, "2NoT3") ? 0 : 1);
		setConversion(ItemUtils.ItemRegion.CELSIAN_ISLES, ItemUtils.ItemTier.FOUR, ScoreboardUtils.checkTag(player, "2NoT4") ? 0 : 1);
		setConversion(ItemUtils.ItemRegion.CELSIAN_ISLES, ItemUtils.ItemTier.FIVE, ScoreboardUtils.checkTag(player, "2NoT5") ? 0 : 1);
		setConversion(ItemUtils.ItemRegion.CELSIAN_ISLES, ItemUtils.ItemTier.UNCOMMON, ScoreboardUtils.checkTag(player, "2NoU") ? 0 : 1);
	}

}
