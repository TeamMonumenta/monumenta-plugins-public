package com.playmonumenta.plugins.minigames.pzero;

import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public enum PzeroMap {
	MAP_NULL("NULL", new Vector(), new Vector(), new Vector(), "", "", "", "", 1, Collections.emptyList()),
	MAP_0("Test", new Vector(-1985, 212, -1175), new Vector(-2009, 206, -1172), new Vector(-1976, 229, -1210), "monumenta:events/pzero/arena0start", "monumenta:events/pzero/arena0reset", "monumenta:events/pzero/arena0win", "pzerotest", 1, getMap0Checkpoints()),
	MAP_1("Wintery Shroomland", new Vector(-1045, 101, -2982), new Vector(-989, 100, -2971), new Vector(-1094, 140, -3049), "monumenta:events/pzero/race1start", "monumenta:events/pzero/race1reset", "monumenta:events/pzero/race1win", "PZero-WinteryShroomland", 3, getMap1Checkpoints())
	;

	public final String mName;
	public final Vector mReturnPosition;
	public final Vector mSpawnPosition;
	public final Vector mSpectatePosition;
	public final String mStartFunctionName;
	public final String mResetFunctionName;
	public final String mWinFunctionName;
	public final String mLeaderboardName;
	public final int mLapCount;
	public final List<PzeroCheckpoint> mCheckpoints;
	// Finalized placements. These store the placements of players that have won or lost, so that the placements of the
	// players still in the race can respect them and show the correct number.
	private final ArrayList<PzeroPlayerPlacement> mPlacements = new ArrayList<>();
	private final HashMap<UUID, Integer> mCurrentPlacements = new HashMap<>();

	public PzeroMapState mState;
	private int mPlayerCountAtStart;

	PzeroMap(String name, Vector returnPosition, Vector spawnPosition, Vector spectatePosition, String startFunctionName, String resetFunctionName, String winFunctionName, String leaderboardName, int lapCount, List<PzeroCheckpoint> checkpoints) {
		mName = name;
		mReturnPosition = returnPosition;
		mSpawnPosition = spawnPosition;
		mSpectatePosition = spectatePosition;
		mStartFunctionName = startFunctionName;
		mResetFunctionName = resetFunctionName;
		mWinFunctionName = winFunctionName;
		mLeaderboardName = leaderboardName;
		mLapCount = lapCount;
		mCheckpoints = checkpoints;
		mState = PzeroMapState.WAITING;
		mPlayerCountAtStart = 0;
	}

	public String getName() {
		return mName;
	}

	public void setWaiting() {
		mState = PzeroMapState.WAITING;
	}

	public void setStarting() {
		mState = PzeroMapState.STARTING;
	}

	public void setRunning() {
		mState = PzeroMapState.RUNNING;
	}

	public boolean isWaiting() {
		return mState == PzeroMapState.WAITING;
	}

	public boolean isStarting() {
		return mState == PzeroMapState.STARTING;
	}

	public boolean isRunning() {
		return mState == PzeroMapState.RUNNING;
	}

	public PzeroCheckpoint getNextCheckpoint(int currentCheckpoint) {
		int nextCheckpoint = (currentCheckpoint + 1) % mCheckpoints.size();
		return mCheckpoints.get(nextCheckpoint);
	}

	public int getNextAvailableTopPosition() {
		for (int i = 1; i <= mPlayerCountAtStart; i++) {
			int finalPos = i;
			if (mPlacements.stream().anyMatch(placement -> placement.mPlacement == finalPos)) {
				continue;
			}
			return i;
		}
		return mPlayerCountAtStart;
	}

	public int getNextAvailableBottomPosition() {
		for (int i = mPlayerCountAtStart; i >= 1; i--) {
			int finalPos = i;
			if (mPlacements.stream().anyMatch(placement -> placement.mPlacement == finalPos)) {
				continue;
			}
			return i;
		}
		return 1;
	}

	public void addPlacement(PzeroPlayerPlacement placement) {
		mPlacements.add(placement);
	}

	public List<PzeroPlayerPlacement> getPlacements() {
		return new ArrayList<>(mPlacements);
	}

	public void clearPlacements() {
		mPlacements.clear();
	}

	public void setPlayerCountAtStart(int count) {
		mPlayerCountAtStart = count;
	}

	public int getCurrentPlayerPlacement(Player player) {
		return Optional.ofNullable(mCurrentPlacements.get(player.getUniqueId())).orElse(0);
	}

	public void setCurrentPlayerPlacement(Player player, int value) {
		mCurrentPlacements.put(player.getUniqueId(), value);
	}

	public void clearCurrentPlayerPlacements() {
		mCurrentPlacements.clear();
	}

	public void displayStandingsToNearbyPlayers(World world, double radius) {
		PlayerUtils.playersInRange(mReturnPosition.toLocation(world), radius, true).forEach(player -> {
			player.sendMessage(Component.text("▂▄▆ Race Results ▆▄▂", PzeroPlayer.SILVER_COLOR, TextDecoration.BOLD));
			List<PzeroPlayerPlacement> placementsCopy = getPlacements();
			if (placementsCopy.size() == 0) {
				player.sendMessage(Component.text("Race Cancelled - All players left", NamedTextColor.RED, TextDecoration.BOLD));
			}
			placementsCopy.sort(Comparator.comparingInt(placement -> placement.mPlacement));
			for (PzeroPlayerPlacement placement : placementsCopy) {
				Component leaderboardSpot = Component.text(StringUtils.intToOrdinal(placement.mPlacement), PzeroPlayer.getPlacementColor(placement.mPlacement), TextDecoration.BOLD)
					.append(Component.text(" - " + placement.mPlayer.getName(), NamedTextColor.WHITE, TextDecoration.BOLD));

				if (placement.mHasFinished) {
					leaderboardSpot = leaderboardSpot
						.append(Component.text(" - ", NamedTextColor.WHITE, TextDecoration.BOLD))
						.append(Component.text(StringUtils.intToMinuteAndSeconds(placement.mFinalTimerTicks / 20) + "." + StringUtils.ticksToMilliseconds(placement.mFinalTimerTicks), PzeroPlayer.OTHER_COLOR, TextDecoration.BOLD));
				} else {
					leaderboardSpot = leaderboardSpot.append(Component.text(" ❌", NamedTextColor.RED, TextDecoration.BOLD));
				}

				player.sendMessage(leaderboardSpot);
			}
		});
	}

	public void registerPlayerTime(PzeroPlayer pzPlayer) {
		int ticks = pzPlayer.getTimer();
		int seconds = ticks / 20;
		ticks = ticks % 20;
		int scoreboardEntry = Integer.parseInt(seconds + StringUtils.ticksToMilliseconds(ticks));
		int currentEntry = ScoreboardUtils.getScoreboardValue(pzPlayer.getPlayer(), mLeaderboardName).orElse(Integer.MAX_VALUE);
		if (scoreboardEntry < currentEntry) {
			ScoreboardUtils.setScoreboardValue(pzPlayer.getPlayer(), mLeaderboardName, scoreboardEntry);
			Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "leaderboard update " + pzPlayer.getPlayer().getName() + " " + mLeaderboardName);
		}
	}

	private static List<PzeroCheckpoint> getMap0Checkpoints() {
		return new PzeroCheckpointBuilder()
			.add(-2012, 206, -1176, 7, 3, 7)
			.add(-2027, 206, -1198, 6, 3, 6)
			.add(-1980, 206, -1213, 6, 3, 6)
			.add(-1977, 206, -1199, 6, 3, 6)
			.add(-1996, 206, -1251, 6, 3, 6)
			.add(-1978, 206, -1249, 6, 3, 6)
			.add(-1966, 206, -1198, 6, 3, 6)
			.add(-1946, 206, -1216, 6, 3, 6)
			.add(-1942, 206, -1251, 6, 3, 6)
			.add(-1907, 206, -1204, 6, 3, 6)
			.add(-1993, 206, -1192, 6, 3, 6)
			.build();
	}

	private static List<PzeroCheckpoint> getMap1Checkpoints() {
		return new PzeroCheckpointBuilder()
			.add(-1072, 102, -2995, 8, 4, 8)
			.add(-1067, 102, -3033, 8, 4, 8)
			.add(-1071, 102, -3088, 8, 4, 8)
			.add(-1040, 102, -3090, 8, 4, 8)
			.add(-1040, 102, -3120, 8, 4, 8)
			.add(-1080, 102, -3098, 8, 4, 8)
			.add(-1112, 102, -3098, 8, 4, 8)
			.add(-1126, 107, -3042, 8, 4, 8)
			.add(-1083, 107, -2980, 8, 4, 8)
			.add(-1096, 102, -2948, 8, 4, 8)
			.add(-1084, 102, -2945, 8, 4, 8)
			.build();
	}
}

