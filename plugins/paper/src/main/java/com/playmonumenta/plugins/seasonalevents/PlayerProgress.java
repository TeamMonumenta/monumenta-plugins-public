package com.playmonumenta.plugins.seasonalevents;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.playmonumenta.plugins.seasonalevents.gui.PassesView;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.MessagingUtils;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class PlayerProgress {
	public static class PassProgress {
		// Stored in json as mission_progress for legacy reasons
		private final Map<Integer, List<Integer>> mWeeklyMissionProgress = new TreeMap<>();
		// Multi-week mission progress
		private final List<Integer> mLongMissionProgress = new ArrayList<>();
		private int mEarnedPoints = 0;
		private int mClaimedPoints = 0;

		public PassProgress() {
		}

		public PassProgress(JsonObject passJson) {
			if (passJson.get("earned_points") instanceof JsonPrimitive missionPointsJson && missionPointsJson.isNumber()) {
				mEarnedPoints = missionPointsJson.getAsInt();
			}

			if (passJson.get("claimed_points") instanceof JsonPrimitive missionPointsJson && missionPointsJson.isNumber()) {
				mClaimedPoints = missionPointsJson.getAsInt();
			}

			if (passJson.get("mission_progress") instanceof JsonObject weekProgressJson) {
				for (Map.Entry<String, JsonElement> weekEntryJson : weekProgressJson.entrySet()) {
					if (!(weekEntryJson.getValue() instanceof JsonArray weekJsonObject)) {
						continue;
					}

					int weekNumber = Integer.parseInt(weekEntryJson.getKey());
					List<Integer> weekProgress = mWeeklyMissionProgress.computeIfAbsent(weekNumber, k -> new ArrayList<>());

					for (JsonElement missionEntryJson : weekJsonObject) {
						if (missionEntryJson instanceof JsonPrimitive missionProgressJson
							&& missionProgressJson.isNumber()) {
							weekProgress.add(missionProgressJson.getAsInt());
						} else {
							weekProgress.add(0);
						}
					}
				}
			}

			if (passJson.get("long_mission_progress") instanceof JsonArray longProgressArray) {
				for (JsonElement longMissionElement : longProgressArray) {
					if (longMissionElement instanceof JsonPrimitive longMissionPrimitive
						&& longMissionPrimitive.isNumber()) {
						mLongMissionProgress.add(longMissionPrimitive.getAsInt());
					} else {
						mLongMissionProgress.add(0);
					}
				}
			}
		}

		public PassProgress(PassProgress from) {
			mEarnedPoints = from.mEarnedPoints;
			mClaimedPoints = from.mClaimedPoints;
			for (Map.Entry<Integer, List<Integer>> weekEntry : from.mWeeklyMissionProgress.entrySet()) {
				mWeeklyMissionProgress.put(weekEntry.getKey(), new ArrayList<>(new ArrayList<>(weekEntry.getValue())));
			}
			mLongMissionProgress.addAll(from.mLongMissionProgress);
		}

		public JsonObject toJson() {
			JsonObject result = new JsonObject();

			result.addProperty("earned_points", mEarnedPoints);
			result.addProperty("claimed_points", mClaimedPoints);

			JsonObject missionProgressJson = new JsonObject();
			for (Map.Entry<Integer, List<Integer>> weekProgress : mWeeklyMissionProgress.entrySet()) {
				JsonArray weekProgressJson = new JsonArray();
				for (Integer missionProgress : weekProgress.getValue()) {
					weekProgressJson.add(missionProgress);
				}
				missionProgressJson.add(weekProgress.getKey().toString(), weekProgressJson);
			}
			result.add("mission_progress", missionProgressJson);

			if (!mLongMissionProgress.isEmpty()) {
				JsonArray longMissionArray = new JsonArray();
				for (int missionProgress : mLongMissionProgress) {
					longMissionArray.add(missionProgress);
				}
				result.add("long_mission_progress", longMissionArray);
			}

			return result;
		}

		public int getMissionPoints() {
			return mEarnedPoints;
		}

		public void addMissionPoints(int amount) {
			mEarnedPoints += amount;
		}

		public int getClaimedPoints() {
			return mClaimedPoints;
		}

		public void setClaimedPoints(int amount) {
			mClaimedPoints = amount;
		}

		public @Nullable List<Integer> getWeekProgress(int week) {
			return mWeeklyMissionProgress.get(week);
		}

		public List<Integer> getOrComputeWeekProgress(int week) {
			return mWeeklyMissionProgress.computeIfAbsent(week, k -> new ArrayList<>());
		}

		public int getLongMissionProgress(int index) {
			if (index < 0 || index >= mLongMissionProgress.size()) {
				return 0;
			}
			return mLongMissionProgress.get(index);
		}

		public static List<Component> diff(@Nullable PassProgress old, @Nullable PassProgress updated) {
			List<Component> results = new ArrayList<>();

			if (old == null) {
				old = new PassProgress();
			}

			if (updated == null) {
				updated = new PassProgress();
			}

			TreeSet<Integer> allKeys = new TreeSet<>(old.mWeeklyMissionProgress.keySet());
			allKeys.addAll(updated.mWeeklyMissionProgress.keySet());

			for (int week : allKeys) {
				List<Integer> oldWeekProgress = old.mWeeklyMissionProgress.get(week);
				List<Integer> newWeekProgress = updated.mWeeklyMissionProgress.get(week);

				if (oldWeekProgress == null) {
					diffWeeklyMissionLine(results, true, week, Objects.requireNonNull(newWeekProgress));
				} else if (newWeekProgress == null) {
					diffWeeklyMissionLine(results, false, week, oldWeekProgress);
				} else if (!oldWeekProgress.equals(newWeekProgress)) {
					diffWeeklyMissionLine(results, false, week, oldWeekProgress);
					diffWeeklyMissionLine(results, true, week, newWeekProgress);
				}
			}

			diffLongMissions(results, old.mLongMissionProgress, updated.mLongMissionProgress);

			if (!results.isEmpty()) {
				results.add(Component.text("-", NamedTextColor.RED)
					.append(Component.text(" MP: " + old.mEarnedPoints)));
				results.add(Component.text("+", NamedTextColor.GREEN)
					.append(Component.text(" MP: " + updated.mEarnedPoints)));
			}

			return results;
		}

		private static void diffWeeklyMissionLine(List<Component> diffLines, boolean isAdded, int week, List<Integer> weekProgress) {
			Component header = Component.text(isAdded ? "+" : "-", isAdded ? NamedTextColor.GREEN : NamedTextColor.RED)
				.append(Component.text(" Week " + week));
			appendIntegerList(diffLines, header, weekProgress);
		}

		private static void diffLongMissions(List<Component> diffLines, List<Integer> old, List<Integer> updated) {
			if (old.equals(updated)) {
				return;
			}

			Component header;

			header = Component.text("-", NamedTextColor.RED)
				.append(Component.text(" Long Missions"));
			appendIntegerList(diffLines, header, old);

			header = Component.text("+", NamedTextColor.GREEN)
				.append(Component.text(" Long Missions"));
			appendIntegerList(diffLines, header, updated);
		}

		private static void appendIntegerList(List<Component> diffLines, Component header, List<Integer> list) {
			Component line = header;
			for (int missionProgress : list) {
				line = line.append(Component.text(", " + missionProgress));
			}
			if (list.isEmpty()) {
				line = line.append(Component.text("<not set>"));
			}
			diffLines.add(line);
		}
	}

	private final Map<LocalDateTime, PassProgress> mAllPassProgress = new TreeMap<>();

	public PlayerProgress() {
	}

	public PlayerProgress(Player player, @Nullable JsonObject object) {
		if (object == null) {
			return;
		}
		for (Map.Entry<String, JsonElement> passProgressJson : object.entrySet()) {
			String passStartStr = passProgressJson.getKey();
			try {
				LocalDateTime passStart = LocalDateTime.parse(passStartStr + "T00:00:00");
				PassProgress passProgress;
				if (passProgressJson.getValue() instanceof JsonObject passProgressEntriesJson) {
					passProgress = new PassProgress(passProgressEntriesJson);
				} else {
					passProgress = new PassProgress();
				}
				mAllPassProgress.put(passStart, passProgress);
			} catch (Exception ex) {
				String msg = "Failed to load " + player.getName() + "'s pass progress for " + passStartStr + ":";
				player.sendMessage(Component.text(msg, NamedTextColor.RED));
				MMLog.warning(msg);
				MessagingUtils.sendStackTrace(player, ex);
			}
		}
	}

	public PlayerProgress(PlayerProgress from) {
		for (Map.Entry<LocalDateTime, PassProgress> passEntry : from.mAllPassProgress.entrySet()) {
			mAllPassProgress.put(passEntry.getKey(), new PassProgress(passEntry.getValue()));
		}
	}

	public JsonObject toJson() {
		JsonObject result = new JsonObject();

		for (Map.Entry<LocalDateTime, PassProgress> passProgressEntry : mAllPassProgress.entrySet()) {
			LocalDateTime passStart = passProgressEntry.getKey();
			String startDate = passStart.format(DateTimeFormatter.ISO_LOCAL_DATE);
			result.add(startDate, passProgressEntry.getValue().toJson());
		}

		return result;
	}

	public @Nullable PassProgress getPassProgress(SeasonalPass pass) {
		return mAllPassProgress.get(pass.mPassStart);
	}

	public Optional<Integer> getWeeklyMissionProgress(LocalDateTime missionDate, int missionIndex) {
		if (missionIndex < 0) {
			return Optional.empty();
		}

		SeasonalPass pass = SeasonalEventManager.getPass(missionDate);
		if (pass == null) {
			return Optional.empty();
		}
		int week = pass.getWeekOfPass(missionDate);

		PassProgress passProgress = mAllPassProgress.get(pass.mPassStart);
		if (passProgress == null) {
			return Optional.empty();
		}
		List<Integer> weekProgress = passProgress.getWeekProgress(week);
		if (weekProgress == null || missionIndex >= weekProgress.size()) {
			return Optional.empty();
		}
		return Optional.of(weekProgress.get(missionIndex));
	}

	public void setWeeklyMissionProgress(LocalDateTime missionDate, int missionIndex, int value) {
		if (missionIndex < 0) {
			return;
		}

		SeasonalPass pass = SeasonalEventManager.getPass(missionDate);
		if (pass == null) {
			return;
		}
		int week = pass.getWeekOfPass(missionDate);
		List<WeeklyMission> weeklyMissions = pass.getActiveWeeklyMissions(week);
		int numMissions = weeklyMissions.size();
		if (missionIndex > numMissions) {
			return;
		}
		WeeklyMission mission = weeklyMissions.get(missionIndex);

		PassProgress passProgress;
		passProgress = mAllPassProgress.computeIfAbsent(pass.mPassStart, k -> new PassProgress());

		List<Integer> weekProgress = passProgress.getOrComputeWeekProgress(week);
		while (weekProgress.size() < numMissions) {
			weekProgress.add(0);
		}

		boolean wasComplete = weekProgress.get(missionIndex) < 0;
		boolean isComplete = value < 0 || value >= mission.mAmount;
		if (!isComplete) {
			if (wasComplete) {
				passProgress.addMissionPoints(-mission.mMP);
			}
		} else {
			value = -1;
			if (!wasComplete) {
				passProgress.addMissionPoints(mission.mMP);
			}
		}

		weekProgress.set(missionIndex, value);
	}

	public int addWeeklyMissionProgress(LocalDateTime missionDate, int missionIndex, int value) {
		if (missionIndex < 0) {
			return 0;
		}

		SeasonalPass pass = SeasonalEventManager.getPass(missionDate);
		if (pass == null) {
			return 0;
		}
		int week = pass.getWeekOfPass(missionDate);
		List<WeeklyMission> weeklyMissions = pass.getActiveWeeklyMissions(week);
		int numMissions = weeklyMissions.size();
		if (missionIndex > numMissions) {
			return 0;
		}
		WeeklyMission mission = weeklyMissions.get(missionIndex);

		PassProgress passProgress;
		passProgress = mAllPassProgress.computeIfAbsent(pass.mPassStart, k -> new PassProgress());

		List<Integer> weekProgress = passProgress.getOrComputeWeekProgress(week);
		while (weekProgress.size() < numMissions) {
			weekProgress.add(0);
		}

		int oldValue = weekProgress.get(missionIndex);
		if (value < 0 && oldValue < 0) {
			// Exception for mod work; only used when reverting progress
			value = mission.mAmount + value;
		} else {
			value += oldValue;
		}

		boolean wasComplete = oldValue < 0;
		boolean isComplete = value < 0 || value >= mission.mAmount;
		if (!isComplete) {
			if (wasComplete) {
				passProgress.addMissionPoints(-mission.mMP);
			}
		} else {
			value = -1;
			if (!wasComplete) {
				passProgress.addMissionPoints(mission.mMP);
			}
		}

		weekProgress.set(missionIndex, value);
		return value;
	}

	public int getLongMissionProgress(LocalDateTime missionDate, int missionIndex) {
		SeasonalPass pass = SeasonalEventManager.getPass(missionDate);
		if (pass == null) {
			return 0;
		}

		PassProgress passProgress = mAllPassProgress.get(pass.mPassStart);
		if (passProgress == null) {
			return 0;
		}

		return passProgress.getLongMissionProgress(missionIndex);
	}

	public void setLongMissionProgress(LocalDateTime missionDate, int missionIndex, int value) {
		if (missionIndex < 0) {
			return;
		}

		SeasonalPass pass = SeasonalEventManager.getPass(missionDate);
		if (pass == null) {
			return;
		}

		List<LongMission> longMissions = pass.getLongMissions();
		int numMissions = longMissions.size();
		if (missionIndex >= numMissions) {
			return;
		}
		LongMission mission = longMissions.get(missionIndex);

		PassProgress passProgress;
		passProgress = mAllPassProgress.computeIfAbsent(pass.mPassStart, k -> new PassProgress());

		while (passProgress.mLongMissionProgress.size() < numMissions) {
			passProgress.mLongMissionProgress.add(0);
		}

		int oldValue = passProgress.mLongMissionProgress.get(missionIndex);

		boolean wasComplete = oldValue < 0;
		boolean isComplete = value < 0 || value >= mission.mAmount;
		if (!isComplete) {
			if (wasComplete) {
				passProgress.addMissionPoints(-mission.mMP);
			}
		} else {
			value = -1;
			if (!wasComplete) {
				passProgress.addMissionPoints(mission.mMP);
			}
		}

		passProgress.mLongMissionProgress.set(missionIndex, value);
	}

	public int addLongMissionProgress(LocalDateTime missionDate, int missionIndex, int value) {
		if (missionIndex < 0) {
			return 0;
		}

		SeasonalPass pass = SeasonalEventManager.getPass(missionDate);
		if (pass == null) {
			return 0;
		}
		List<LongMission> longMissions = pass.getLongMissions();
		int numMissions = longMissions.size();
		if (missionIndex > numMissions) {
			return 0;
		}
		LongMission mission = longMissions.get(missionIndex);

		PassProgress passProgress;
		passProgress = mAllPassProgress.computeIfAbsent(pass.mPassStart, k -> new PassProgress());

		while (passProgress.mLongMissionProgress.size() < numMissions) {
			passProgress.mLongMissionProgress.add(0);
		}

		int oldValue = passProgress.mLongMissionProgress.get(missionIndex);
		if (value < 0 && oldValue < 0) {
			// Exception for mod work; only used when reverting progress
			value = mission.mAmount + value;
		} else {
			value += oldValue;
		}

		boolean wasComplete = oldValue < 0;
		boolean isComplete = value < 0 || value >= mission.mAmount;
		if (!isComplete) {
			if (wasComplete) {
				passProgress.addMissionPoints(-mission.mMP);
			}
		} else {
			value = -1;
			if (!wasComplete) {
				passProgress.addMissionPoints(mission.mMP);
			}
		}

		passProgress.mLongMissionProgress.set(missionIndex, value);
		return value;
	}

	public void copyClaimedPointsFrom(PlayerProgress from) {
		TreeSet<LocalDateTime> passStartTimes = new TreeSet<>(from.mAllPassProgress.keySet());
		passStartTimes.addAll(mAllPassProgress.keySet());

		for (LocalDateTime passStartTime : passStartTimes) {
			PassProgress oldPassProgress = from.mAllPassProgress.get(passStartTime);
			PassProgress newPassProgress = mAllPassProgress.get(passStartTime);

			if (oldPassProgress == null) {
				Objects.requireNonNull(newPassProgress).setClaimedPoints(0);
			} else if (newPassProgress == null) {
				int oldClaimedPoints = oldPassProgress.getClaimedPoints();
				if (oldClaimedPoints != 0) {
					newPassProgress = new PassProgress();
					newPassProgress.setClaimedPoints(oldClaimedPoints);
					mAllPassProgress.put(passStartTime, newPassProgress);
				}
			} else {
				int oldClaimedPoints = oldPassProgress.getClaimedPoints();
				newPassProgress.setClaimedPoints(oldClaimedPoints);
			}
		}
	}

	public List<Component> diff(PlayerProgress updated) {
		List<Component> result = new ArrayList<>();

		TreeSet<LocalDateTime> passStartTimes = new TreeSet<>(mAllPassProgress.keySet());
		passStartTimes.addAll(updated.mAllPassProgress.keySet());

		for (LocalDateTime passStartTime : passStartTimes) {
			PassProgress oldPassProgress = mAllPassProgress.get(passStartTime);
			PassProgress newPassProgress = updated.mAllPassProgress.get(passStartTime);
			List<Component> passDiff = PassProgress.diff(oldPassProgress, newPassProgress);
			if (!passDiff.isEmpty()) {
				result.add(Component.text("Diff for pass starting on "
						+ passStartTime.format(PassesView.DATE_FORMAT)
						+ ":",
					NamedTextColor.YELLOW,
					TextDecoration.BOLD));
				result.addAll(passDiff);
			}
		}

		return result;
	}

	public boolean isEmpty() {
		return mAllPassProgress.isEmpty();
	}
}
