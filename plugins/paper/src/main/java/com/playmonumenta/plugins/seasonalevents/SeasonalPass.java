package com.playmonumenta.plugins.seasonalevents;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.cosmetics.CosmeticType;
import com.playmonumenta.plugins.cosmetics.CosmeticsManager;
import com.playmonumenta.plugins.seasonalevents.PlayerProgress.PassProgress;
import com.playmonumenta.plugins.utils.DateUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.SpawnEggMeta;

public class SeasonalPass {
	// cost of rewards, in Metamorphosis Tokens
	private static final int[] DUMMY_COSTS = {2, 3};
	protected static final ImmutableMap<CosmeticType, int[]> COSMETIC_COSTS = ImmutableMap.of(
		CosmeticType.TITLE, new int[] {1, 1, 2, 2, 3, 3},
		CosmeticType.ELITE_FINISHER, new int[] {2, 3, 4},
		CosmeticType.PLOT_BORDER, new int[] {5}
	);

	public static final int MP_PER_LEVEL = 75;
	// Loot tables for rewards
	public static final String ITEM_SKIN_KEY = "epic:pass/metamorphosis_token";
	public static final String TREASURE_WHEEL_KEY = "epic:pass/treasure_wheel_token";
	public static final String RELIC_WHEEL_KEY = "epic:pass/relic_wheel_token";
	// Missions and rewards arrays
	public final LocalDateTime mPassStart;
	public String mName;
	public Material mDisplayItem;
	public TextColor mNameColor;
	public int mNumberOfWeeks = 0;
	public int mTotalMp = 0;
	public final Map<Integer, List<WeeklyMission>> mWeeklyMissions = new HashMap<>();
	public final List<LongMission> mLongMissions = new ArrayList<>();
	public final List<SeasonalReward> mRewards = new ArrayList<>();

	/**
	 * Loads a season pass for a given week from its two json files
	 *
	 * @param missionsJson The contents of a missions json file for this pass
	 * @param rewardsJson  The contents of a rewards json file for this pass
	 */
	public SeasonalPass(final CommandSender sender, LocalDateTime passStart, JsonObject missionsJson, JsonObject rewardsJson, boolean showWarnings) {
		mPassStart = passStart;
		loadRewards(sender, rewardsJson, showWarnings);
		loadMissions(sender, missionsJson, showWarnings);
	}

	/**
	 * Load reward objects from json parsing
	 */
	private void loadRewards(final CommandSender sender, JsonObject data, boolean showWarnings) {
		int dummiesSoFar = 0;
		Map<CosmeticType, Integer> rewardsSoFar = new HashMap<>();

		String startDateStr = data.get("start_date").getAsString();
		if (showWarnings && !mPassStart.getDayOfWeek().equals(DayOfWeek.FRIDAY)) {
			sender.sendMessage(Component.text("[SeasonPass] loadRewards for " + startDateStr + " "
				+ mName + ": Does not start on a Friday (starts on "
				+ mPassStart.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
				+ " instead)", NamedTextColor.RED));
		}
		JsonArray rewardParse = data.get("rewards").getAsJsonArray();
		for (JsonElement rewardElement : rewardParse) {
			try {

				SeasonalReward reward = new SeasonalReward(sender,
					startDateStr,
					rewardsSoFar,
					rewardElement,
					showWarnings);

				if (SeasonalRewardType.LOOT_TABLE.equals(reward.mType)
					&& reward.mLootTable != null
					&& reward.mLootTable.getItemMeta() instanceof SpawnEggMeta) {
					reward.mCost = DUMMY_COSTS[Math.min(dummiesSoFar, DUMMY_COSTS.length - 1)];
					dummiesSoFar++;
				}

				mRewards.add(reward);
			} catch (IgnoredEntryException e) {
				if (showWarnings) {
					sender.sendMessage(Component.text(e.getMessage(), NamedTextColor.RED)
						.hoverEvent(Component.text(rewardElement.toString(), NamedTextColor.RED)));
				}
			} catch (Exception e) {
				MessagingUtils.sendStackTrace(sender, e);
			}
		}
	}

	/**
	 * Load mission objects from json parsing
	 */
	private void loadMissions(final CommandSender sender, JsonObject data, boolean showWarnings) {
		int numberOfWeeks = 0;
		String startDateStr = data.get("start_date").getAsString();
		if (showWarnings && !mPassStart.getDayOfWeek().equals(DayOfWeek.FRIDAY)) {
			sender.sendMessage(Component.text("[SeasonPass] loadMissions for " + startDateStr + " "
				+ mName + ": Does not start on a Friday (starts on "
				+ mPassStart.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
				+ " instead)", NamedTextColor.RED));
		}
		mName = data.get("pass_name").getAsString();
		mDisplayItem = Material.getMaterial(data.get("pass_displayitem").getAsString());
		String nameColorStr = data.get("pass_namecolor").getAsString();
		TextColor nameColor = MessagingUtils.colorFromString(nameColorStr);
		if (nameColor == null) {
			nameColor = NamedTextColor.GOLD;
			if (showWarnings) {
				sender.sendMessage(Component.text("[SeasonPass] loadMissions for " + startDateStr + " "
					+ mName + ": No such name color " + nameColorStr, NamedTextColor.RED));
			}
		}
		mNameColor = nameColor;
		JsonArray missionParse = data.get("missions").getAsJsonArray();
		for (JsonElement missionElement : missionParse) {
			try {
				JsonObject missionObject = missionElement.getAsJsonObject();
				Mission mission = Mission.loadMission(sender, startDateStr, mName, missionObject, showWarnings);
				if (!mission.mIsBonus) {
					mTotalMp += mission.mMP;
				}
				numberOfWeeks = Integer.max(numberOfWeeks, mission.lastWeek());
				if (mission instanceof WeeklyMission weeklyMission) {
					mWeeklyMissions.computeIfAbsent(weeklyMission.mWeek, k -> new ArrayList<>()).add(weeklyMission);
				} else if (mission instanceof LongMission longMission) {
					mLongMissions.add(longMission);
				}
			} catch (Exception e) {
				MessagingUtils.sendStackTrace(sender, e);
			}
		}
		mNumberOfWeeks = numberOfWeeks;
	}

	/**
	 * Claim the rewards previously earned for the current pass
	 */
	public void claimMP(Player p) {
		PlayerProgress playerProgress = SeasonalEventManager.getPlayerProgress(p);
		if (playerProgress == null) {
			return;
		}
		PassProgress passProgress = playerProgress.getPassProgress(this);
		if (passProgress == null) {
			return;
		}

		//Check if they earned any new rewards from this
		int claimedMP = passProgress.getClaimedPoints();
		int newMP = passProgress.getMissionPoints();
		int claimedLevel = getLevelFromMP(claimedMP);
		int newLevel = getLevelFromMP(newMP);
		//Give any unclaimed rewards
		for (int i = claimedLevel + 1; i <= newLevel; i++) {
			p.sendMessage(Component.text("You earned a new reward!", NamedTextColor.GOLD)
				.decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));

			EntityUtils.fireworkAnimation(p);
			givePassReward(p, i);
		}

		// Special case - finished all missions
		if (newMP >= mTotalMp && !CosmeticsManager.getInstance().playerHasCosmetic(p, CosmeticType.TITLE, SeasonalEventManager.ALL_MISSIONS_TITLE_NAME)) {
			p.sendMessage(Component.text("You fully completed this pass!", NamedTextColor.GOLD)
				.decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
			EntityUtils.fireworkAnimation(p);
			CosmeticsManager.getInstance().addCosmetic(p, CosmeticType.TITLE, SeasonalEventManager.ALL_MISSIONS_TITLE_NAME);
		}

		// Update claimed MP (unless this would allow re-claiming rewards)
		if (newMP > claimedMP) {
			passProgress.setClaimedPoints(newMP);
		}
	}

	/**
	 * Note that this can give weeks outside the range of the season pass
	 *
	 * @return week number within or beyond a season pass
	 */
	public int getWeekOfPass() {
		LocalDateTime currentTime = DateUtils.localDateTime();
		return getWeekOfPass(currentTime);
	}

	public int getWeekOfPass(LocalDateTime localDateTime) {
		return (int) (DateUtils.getWeeklyVersion(localDateTime) - DateUtils.getWeeklyVersion(mPassStart) + 1);
	}

	/**
	 * Returns true if this is the current season pass, else false.
	 */
	public boolean isActive() {
		int weekNumber = getWeekOfPass();
		return 1 <= weekNumber && weekNumber <= mNumberOfWeeks;
	}

	public boolean isActive(LocalDateTime localDateTime) {
		int weekNumber = getWeekOfPass(localDateTime);
		return 1 <= weekNumber && weekNumber <= mNumberOfWeeks;
	}

	/**
	 * Returns true if the pass started, even if it has since ended
	 */
	public boolean hasStarted() {
		return 1 <= getWeekOfPass();
	}

	/**
	 * Returns true if the pass ended
	 */
	public boolean hasEnded() {
		return getWeekOfPass() > mNumberOfWeeks;
	}

	/**
	 * Returns weekly missions for the current week of the pass
	 */
	public List<WeeklyMission> getActiveWeeklyMissions() {
		return getActiveWeeklyMissions(getWeekOfPass());
	}

	/**
	 * Returns weekly missions for given week
	 */
	public List<WeeklyMission> getActiveWeeklyMissions(int week) {
		return mWeeklyMissions.getOrDefault(week, new ArrayList<>());
	}

	/**
	 * Returns all long missions for the pass
	 */
	public List<LongMission> getLongMissions() {
		return mLongMissions;
	}

	/**
	 * Returns long missions that are active for the current week of the pass
	 */
	public List<LongMission> getActiveLongMissions() {
		return getActiveLongMissions(getWeekOfPass());
	}

	/**
	 * Returns long missions that are active for a given week
	 */
	public List<LongMission> getActiveLongMissions(int week) {
		List<LongMission> results = new ArrayList<>();
		for (LongMission mission : getLongMissions()) {
			if (mission.isActive(week)) {
				results.add(mission);
			}
		}
		return results;
	}

	/**
	 * Gives out reward for specific level depending
	 * on its type implementation
	 */
	public void givePassReward(Player p, int level) {
		mRewards.get(level - 1).give(p);
	}

	public int getMP(Player p) {
		PlayerProgress playerProgress = SeasonalEventManager.getPlayerProgress(p);
		if (playerProgress == null) {
			return 0;
		}
		PassProgress passProgress = playerProgress.getPassProgress(this);
		if (passProgress == null) {
			return 0;
		}
		return passProgress.getMissionPoints();
	}

	public int getLevelFromMP(int mp) {
		return Math.min(mRewards.size(), mp / MP_PER_LEVEL);
	}

	/**
	 * Triggered by multiple event handlers, this method adds an amount of progress
	 * to the provided weekly mission number for that week
	 */
	public void addWeeklyMissionProgress(Player p, int missionIndex, int amount) {
		LocalDateTime now = DateUtils.localDateTime();
		int week = getWeekOfPass();
		int numWeeklyMissions = mWeeklyMissions.getOrDefault(week, List.of()).size();
		if (missionIndex < 0 || missionIndex >= numWeeklyMissions) {
			return;
		}

		PlayerProgress playerProgress = SeasonalEventManager.getPlayerProgress(p);
		if (playerProgress == null) {
			MMLog.warning("No season pass player progress found for " + p.getName());
			return;
		}
		int missionProgress = playerProgress.getWeeklyMissionProgress(now, missionIndex).orElse(0);
		if (missionProgress < 0) {
			// Mission already completed
			return;
		}
		missionProgress = playerProgress.addWeeklyMissionProgress(now, missionIndex, amount);
		if (missionProgress < 0) {
			//Play an animation and notify player
			p.sendMessage(Component.text(
					"You completed a weekly mission! Open the Seasonal Pass menu to claim your progress!",
					NamedTextColor.GOLD,
					TextDecoration.BOLD));
			EntityUtils.fireworkAnimation(p);
		}
	}

	public void addLongMissionProgress(Player p, int missionIndex, int amount) {
		LocalDateTime now = DateUtils.localDateTime();
		int week = getWeekOfPass();
		int numLongMissions = mLongMissions.size();
		if (missionIndex < 0 || missionIndex >= numLongMissions) {
			return;
		}
		if (!mLongMissions.get(missionIndex).isActive(week)) {
			return;
		}

		PlayerProgress playerProgress = SeasonalEventManager.getPlayerProgress(p);
		if (playerProgress == null) {
			MMLog.warning("No season pass player progress found for " + p.getName());
			return;
		}
		int missionProgress = playerProgress.getLongMissionProgress(now, missionIndex);
		if (missionProgress < 0) {
			// Mission already completed
			return;
		}
		missionProgress = playerProgress.addLongMissionProgress(now, missionIndex, amount);
		if (missionProgress < 0) {
			//Play an animation and notify player
			p.sendMessage(Component.text(
				"You completed a multi-week mission! Open the Seasonal Pass menu to claim your progress!",
				NamedTextColor.GOLD,
				TextDecoration.BOLD));
			EntityUtils.fireworkAnimation(p);
		}
	}

	// Time Utils

	public int getDaysUntilMissionEnd() {
		if (!isActive()) {
			return 0;
		}
		return (int) DateUtils.getDaysLeftInWeeklyVersion() - 1;
	}

	public int getHoursUntilMissionEnd() {
		if (!isActive()) {
			return 0;
		}
		return (int) DateUtils.untilNewWeek(ChronoUnit.HOURS) % 24;
	}

	public int getDaysUntilPassEnd() {
		if (!isActive()) {
			return 0;
		}
		return (int) DateUtils.untilNewWeek(ChronoUnit.DAYS) + 7 * (mNumberOfWeeks - getWeekOfPass());
	}

	public int getHoursUntilPassEnd() {
		if (!isActive()) {
			return 0;
		}
		return (int) DateUtils.untilNewDay(ChronoUnit.HOURS);
	}

	@Override
	public String toString() {
		return mPassStart.toLocalDate().toString() + ": " + mName;
	}
}
