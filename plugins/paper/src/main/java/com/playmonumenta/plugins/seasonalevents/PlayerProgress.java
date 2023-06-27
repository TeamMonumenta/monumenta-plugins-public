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
		private final Map<Integer, List<Integer>> mMissionProgress = new TreeMap<>();
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
					List<Integer> weekProgress = mMissionProgress.computeIfAbsent(weekNumber, k -> new ArrayList<>());

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
		}

		public PassProgress(PassProgress from) {
			mEarnedPoints = from.mEarnedPoints;
			mClaimedPoints = from.mClaimedPoints;
			for (Map.Entry<Integer, List<Integer>> weekEntry : from.mMissionProgress.entrySet()) {
				mMissionProgress.put(weekEntry.getKey(), new ArrayList<>(new ArrayList<>(weekEntry.getValue())));
			}
		}

		public JsonObject toJson() {
			JsonObject result = new JsonObject();

			result.addProperty("earned_points", mEarnedPoints);
			result.addProperty("claimed_points", mClaimedPoints);

			JsonObject missionProgressJson = new JsonObject();
			for (Map.Entry<Integer, List<Integer>> weekProgress : mMissionProgress.entrySet()) {
				JsonArray weekProgressJson = new JsonArray();
				for (Integer missionProgress : weekProgress.getValue()) {
					weekProgressJson.add(missionProgress);
				}
				missionProgressJson.add(weekProgress.getKey().toString(), weekProgressJson);
			}
			result.add("mission_progress", missionProgressJson);

			return result;
		}

		public int getMissionPoints() {
			return mEarnedPoints;
		}

		public void setMissionPoints(int amount) {
			mEarnedPoints = amount;
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
			return mMissionProgress.get(week);
		}

		public List<Integer> getOrComputeWeekProgress(int week) {
			return mMissionProgress.computeIfAbsent(week, k -> new ArrayList<>());
		}

		public static List<Component> diff(@Nullable PassProgress old, @Nullable PassProgress updated) {
			List<Component> results = new ArrayList<>();

			if (old != null && updated != null) {
				TreeSet<Integer> allKeys = new TreeSet<>(old.mMissionProgress.keySet());
				allKeys.addAll(updated.mMissionProgress.keySet());

				for (int week : allKeys) {
					List<Integer> oldWeekProgress = old.mMissionProgress.get(week);
					List<Integer> newWeekProgress = updated.mMissionProgress.get(week);

					if (oldWeekProgress == null) {
						diffLine(results, true, week, Objects.requireNonNull(newWeekProgress));
					} else if (newWeekProgress == null) {
						diffLine(results, false, week, oldWeekProgress);
					} else if (!oldWeekProgress.equals(newWeekProgress)) {
						diffLine(results, false, week, oldWeekProgress);
						diffLine(results, true, week, newWeekProgress);
					}
				}

				if (!results.isEmpty()) {
					results.add(Component.text("-", NamedTextColor.RED)
						.append(Component.text(" MP: " + old.mEarnedPoints)));
					results.add(Component.text("+", NamedTextColor.GREEN)
						.append(Component.text(" MP: " + updated.mEarnedPoints)));
				}
			} else if (old != null) {
				for (Map.Entry<Integer, List<Integer>> weekEntry : old.mMissionProgress.entrySet()) {
					int week = weekEntry.getKey();
					List<Integer> weekProgress = weekEntry.getValue();
					diffLine(results, false, week, weekProgress);
				}
				results.add(Component.text("-", NamedTextColor.RED)
					.append(Component.text(" MP: " + old.mEarnedPoints)));
			} else if (updated != null) {
				for (Map.Entry<Integer, List<Integer>> weekEntry : updated.mMissionProgress.entrySet()) {
					int week = weekEntry.getKey();
					List<Integer> weekProgress = weekEntry.getValue();
					diffLine(results, true, week, weekProgress);
				}
				results.add(Component.text("+", NamedTextColor.GREEN)
					.append(Component.text(" MP: " + updated.mEarnedPoints)));
			}

			return results;
		}

		private static void diffLine(List<Component> diffLines, boolean isAdded, int week, List<Integer> weekProgress) {
			Component line = Component.text(isAdded ? "+" : "-", isAdded ? NamedTextColor.GREEN : NamedTextColor.RED)
				.append(Component.text(" Week " + week));
			for (int missionProgress : weekProgress) {
				line = line.append(Component.text(", " + missionProgress));
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

	public PassProgress getOrCreatePassProgress(SeasonalPass pass) {
		return mAllPassProgress.computeIfAbsent(pass.mPassStart, k -> new PassProgress());
	}

	public @Nullable PassProgress getPassProgress(SeasonalPass pass) {
		return mAllPassProgress.get(pass.mPassStart);
	}

	public Optional<Integer> getPassMissionProgress(LocalDateTime missionDate, int missionIndex) {
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

	public void setPassMissionProgress(LocalDateTime missionDate, int missionIndex, int value) {
		if (missionIndex < 0) {
			return;
		}

		SeasonalPass pass = SeasonalEventManager.getPass(missionDate);
		if (pass == null) {
			return;
		}
		int week = pass.getWeekOfPass(missionDate);
		List<WeeklyMission> weeklyMissions = pass.getMissionsInWeek(week);
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

	public int addPassMissionProgress(LocalDateTime missionDate, int missionIndex, int value) {
		if (missionIndex < 0) {
			return 0;
		}

		SeasonalPass pass = SeasonalEventManager.getPass(missionDate);
		if (pass == null) {
			return 0;
		}
		int week = pass.getWeekOfPass(missionDate);
		List<WeeklyMission> weeklyMissions = pass.getMissionsInWeek(week);
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
