package com.playmonumenta.plugins.depths;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.delves.DelvesModifier;
import com.playmonumenta.plugins.delves.DelvesUtils;
import com.playmonumenta.plugins.depths.loot.DepthsLoot;
import com.playmonumenta.plugins.depths.loot.ZenithLoot;
import com.playmonumenta.plugins.depths.rooms.DepthsRoom;
import com.playmonumenta.plugins.depths.rooms.DepthsRoomType;
import com.playmonumenta.plugins.depths.rooms.DepthsRoomType.DepthsRewardType;
import com.playmonumenta.plugins.events.MonumentaEvent;
import com.playmonumenta.plugins.integrations.MonumentaNetworkRelayIntegration;
import com.playmonumenta.plugins.seasonalevents.SeasonalEventListener;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;


/**
 * This class handles dynamic information about depths parties, including key information about the world like locations,
 * as well as run specific information including progress and events, and distribution of loot rooms.
 *
 * @author ShadowVisions
 */
public class DepthsParty {
	public static final int MAX_LOOT_ROOMS = 4;
	public static final int MAX_LOOT_ROOMS_EXPANDED = 6;
	public static final String ENDLESS_LEADERBOARD = "DepthsEndless";
	public static final String EXPANDED_LEADERBOARD = "DepthsEndless6";
	public static final String ASCENSION_LEADERBOARD = "ZenithAscension";
	public static final String SIX_ASCENSION_LEADERBOARD = "ZenithAscension6";
	public static final double ZENITH_HEALTH_INCREASE_PER_ASCENSION = 0.03;
	public static final double ZENITH_DAMAGE_INCREASE_PER_ASCENSION = 0.03;


	// The difference between where the player spawns in a loot room, and where the loot needs to be dropped
	// Transient - don't try to save a circular reference to players (will crash data save)
	public transient List<DepthsPlayer> mPlayersInParty;
	// The current room number the party is on
	public int mRoomNumber;
	// The number of spawners remaining in the current room to break for the party to proceed
	public int mSpawnersToBreak;
	// The current type of room players are in
	public @Nullable DepthsRoomType mCurrentRoomType;
	// The saved choices for the party's next room options
	public @Nullable EnumSet<DepthsRoomType> mNextRoomChoices;
	// The actual room object for the current room
	public @Nullable DepthsRoom mCurrentRoom;
	// Where in the room to spawn the next room
	public @Nullable Vector mRoomSpawnerLocation;
	// Party unique identifier- gets set on creation
	public long mPartyNum = -1;
	// Treasure score accumulated by the party
	public int mTreasureScore;
	//The location at which the lobby for each floor should be loaded. This is marked by an armor stand
	public @Nullable Vector mFloorLobbyLoadPoint;
	//Where to tp the players at the start of the next floor
	public @Nullable Vector mFloorLobbyLoadPlayerTpPoint;
	//Used to determine if the party can receive an upgrade reward
	public boolean mHasAtLeastOneAbility;
	//Locations of the loot rooms to tp players to (should be 4)
	public List<Vector> mLootRoomLocations;
	//Number of loot rooms the party has used
	public int mLootRoomsUsed;
	//Rooms the party has already cleared (do not spawn them again)
	public List<DepthsRoom> mOldRooms;
	//Whether or not the reward chest has been spawned for this room already
	public boolean mSpawnedReward;
	//Whether or not the party is playing in endless mode
	public boolean mEndlessMode = false;
	//Whether or not the party is playing with rigged talismans in endless
	public boolean mIsRigged = false;
	//Keep track of the delve modifiers for the party
	public Map<DelvesModifier, Integer> mDelveModifiers;
	//Used to keep track of if you have beaten the boss (should spawn a room of next floor)
	public boolean mBeatBoss = false;
	//Used to track pickup of treasure rewards from rooms
	public boolean mCanGetTreasureReward = false;
	//The world the party is in
	public final UUID mWorldUUID;
	//Whether the party got a twisted room already this floor
	public boolean mTwistedThisFloor = false;
	//Write the 4 players that started the party
	public transient ArrayList<String> mInitialPlayers = new ArrayList<>();
	//The X value of the start side of the current room
	public int mRoomStartX;
	//The X value of the start side of the (first) a10+ remove room in this floor (Integer.MAX_VALUE if unset)
	public int mNoPassiveRemoveRoomStartX = Integer.MAX_VALUE;
	//Whether the party has exceeded four players
	public boolean mIsSixPlayerMode;
	//The Depths content this party is in
	public DepthsContent mContent;
	public @Nullable Vector mDeathWaitingRoomPoint;
	public int mAscension;
	//Whether or not the party is currently loading a room - used to prevent players from spawning 2 rooms on top of each other
	public boolean mIsLoadingRoom = false;

	//A flag for if the forced cleansing room has already been spawned in A10+. This is to prevent players from just spamming cleanse rooms (they do nothing, but still).
	public boolean mSpawnedForcedCleansingRoom = false;

	/**
	 * Creates a new depths party with the given players
	 */
	public DepthsParty(List<DepthsPlayer> players, Location loc) {
		mPlayersInParty = players;
		mWorldUUID = loc.getWorld().getUID();
		if (players != null && players.size() > 4) {
			mIsSixPlayerMode = true;
		}

		for (DepthsPlayer dp : Objects.requireNonNull(players)) {

			//Endless mode detection
			Player p = Bukkit.getPlayer(dp.mPlayerId);
			Objects.requireNonNull(p).removeScoreboardTag(DepthsManager.PAID_SCOREBOARD_TAG);
			if (p.getScoreboardTags().contains(DepthsManager.ENDLESS_MODE_STRING)) {
				mEndlessMode = true;
				p.removeScoreboardTag(DepthsManager.ENDLESS_MODE_STRING);
			}

			if (p.getScoreboardTags().contains(DepthsManager.RIGGED_STRING)) {
				mIsRigged = true;
				p.removeScoreboardTag(DepthsManager.RIGGED_STRING);
			}

			int currentAscension = ScoreboardUtils.getScoreboardValue(p, "CurrentAscension").orElse(0);
			if (currentAscension > 0) {
				mAscension = currentAscension;
				ScoreboardUtils.setScoreboardValue(p, "CurrentAscension", 0);
			}
			mContent = DepthsUtils.getDepthsContent();

			if (mPartyNum <= 0) {
				if (getContent() == DepthsContent.DARKEST_DEPTHS) {
					mPartyNum = ScoreboardUtils.getScoreboardValue(p, DepthsManager.DEPTHS_ACCESS).orElse(0);
				} else if (getContent() == DepthsContent.CELESTIAL_ZENITH) {
					mPartyNum = ScoreboardUtils.getScoreboardValue(p, DepthsManager.ZENITH_ACCESS).orElse(0);
				}
				if (mPartyNum <= 0) {
					mPartyNum = UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE;
				}
			}
			dp.mPartyNum = this.mPartyNum;
			mInitialPlayers.add(p.getName());
		}

		//Run this later so it finishes constructing the party first
		new BukkitRunnable() {

			@Override
			public void run() {
				for (DepthsPlayer dp : players) {
					Player p = Bukkit.getPlayer(dp.mPlayerId);
					//Get random ability to start
					int[] chances = {80, 15, 5, 0, 0};
					DepthsManager.getInstance().getRandomAbility(Objects.requireNonNull(p), dp, chances, false, false);
				}
			}

		}.runTaskLater(Plugin.getInstance(), 5);

		mRoomNumber = 0;
		mSpawnersToBreak = 0;
		mHasAtLeastOneAbility = false;
		mOldRooms = new ArrayList<>();
		mLootRoomLocations = new ArrayList<>();
		mDelveModifiers = new HashMap<>();
		mSpawnedReward = false;
		mRoomStartX = loc.getBlockX();
		mContent = DepthsUtils.getDepthsContent();

		//Attempt to set locations for the next floor lobby to load
		Collection<ArmorStand> nearbyStands = loc.getNearbyEntitiesByType(ArmorStand.class, 100.0);
		for (ArmorStand stand : nearbyStands) {
			if (stand.getName().contains(DepthsManager.PLAYER_SPAWN_STAND_NAME)) {
				mFloorLobbyLoadPlayerTpPoint = stand.getLocation().toVector();
				stand.remove();
			}
			if (stand.getName().contains(DepthsManager.FLOOR_LOBBY_LOAD_STAND_NAME)) {
				mFloorLobbyLoadPoint = stand.getLocation().toVector();
				stand.remove();
			}

			if (stand.getName().contains(DepthsManager.LOOT_ROOM_STAND_NAME)) {
				mLootRoomLocations.add(stand.getLocation().toVector());
				stand.remove();
			}

			if (stand.getName().contains(DepthsManager.DEATH_WAITING_ROOM_STAND_NAME)) {
				mDeathWaitingRoomPoint = stand.getLocation().toVector();
				stand.remove();
			}
		}
	}

	public void addPlayerToParty(DepthsPlayer player) {
		mPlayersInParty.add(player);
		player.mPartyNum = this.mPartyNum;
		if (mPlayersInParty.size() > 4) {
			mIsSixPlayerMode = true;
		}

		//Endless mode detection
		Player p = Bukkit.getPlayer(player.mPlayerId);
		if (p != null) {
			p.removeScoreboardTag(DepthsManager.PAID_SCOREBOARD_TAG);
			if (p.getScoreboardTags().contains(DepthsManager.ENDLESS_MODE_STRING)) {
				mEndlessMode = true;
				p.removeScoreboardTag(DepthsManager.ENDLESS_MODE_STRING);
			}
		}
	}

	/**
	 * This method is called when a member of a depths party breaks a spawner
	 * If the spawner was the last they needed to clear the room, a reward chest is spawned
	 */
	public void partyBrokeSpawner(Location l) {

		if (mSpawnersToBreak > 0) {
			mSpawnersToBreak--;
		}

		if (mCurrentRoom == null) {
			return;
		}

		//Check if the room is complete and we didnt already reward them
		if (mSpawnersToBreak == 0 && !mSpawnedReward && mCurrentRoom.mSpawnerCount > 0) {

			DepthsRewardType rewardType = DepthsUtils.rewardFromRoom(mCurrentRoomType);
			if (rewardType != null) {
				mPlayersInParty.forEach(dp -> dp.mEarnedRewards.add(rewardType));
			}
			sendMessage("This room's " + DepthsUtils.rewardString(mCurrentRoomType) + " reward has been found!");

			if (mCurrentRoomType == DepthsRoomType.TREASURE || mCurrentRoomType == DepthsRoomType.TREASURE_ELITE) {
				mCanGetTreasureReward = true;
			}

			//Allow players to get the reward

			mSpawnedReward = true;
			//For ability and upgrade rewards, spawn a chest to interface with them

			l.getWorld().playSound(l, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 0.8f, 1.0f);
			l.getWorld().playSound(l, Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0f, 1.0f);
			EntityUtils.fireworkAnimation(l);
			spawnRoomReward(l, rewardType != null);
		}
	}

	public void giveTreasureReward(@Nullable Location l, int score) {

		mTreasureScore += score;
		mCanGetTreasureReward = false;

		for (DepthsPlayer dp : mPlayersInParty) {

			Player p = Bukkit.getPlayer(dp.mPlayerId);
			if (p != null) {
				p.playSound(p.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 1.0f, 1.0f);
				p.sendActionBar(Component.text(score + " treasure score added to loot room!", NamedTextColor.GOLD));
			}
		}

		if (l != null) {
			l.getBlock().setType(Material.AIR);
		}
	}

	// Spawns the chest in the location of the final spawner
	private void spawnRoomReward(Location l, boolean spawnSlime) {

		BukkitRunnable itemEffects = new ItemEffects(l, null);
		if (spawnSlime) {
			Entity e = l.getWorld().spawnEntity(l.clone().add(0.5, 0.25, 0.5), EntityType.SLIME);
			if (e instanceof Slime slime) {
				slime.setSize(1);
				slime.setAI(false);
				slime.setGlowing(true);
				slime.setInvulnerable(true);
				slime.setInvisible(true);
				slime.addScoreboardTag(AbilityUtils.IGNORE_TAG);
				slime.addScoreboardTag("boss_delveimmune");
			}
			itemEffects = new ItemEffects(l, e);
		}
		itemEffects.runTaskTimer(Plugin.getInstance(), 0, 20);

		BukkitRunnable placeChest = new PlaceChest(l);
		// Delay by one tick to wait out the spawner break event
		placeChest.runTaskLater(Plugin.getInstance(), 1);

	}

	/**
	 * Gets some summary information about the party
	 *
	 * @return info about the party as a string to print
	 */
	public Component getSummaryComponent() {
		Component result = Component.empty().color(NamedTextColor.GOLD)
			.append(Component.text("Depths Party Summary:", NamedTextColor.LIGHT_PURPLE));
		StringBuilder partyMembers = new StringBuilder("Players in party:");
		boolean endlessModeUnlocked = false;
		for (DepthsPlayer dp : mPlayersInParty) {
			Player p = Bukkit.getPlayer(dp.mPlayerId);
			if (p != null && p.isOnline()) {
				partyMembers.append(" ").append(p.getName());
				if (ScoreboardUtils.getScoreboardValue(p, "Depths").orElse(0) > 0) {
					endlessModeUnlocked = true;
				}
			}
		}

		result = result
			.append(Component.newline())
			.append(Component.text(partyMembers.toString(), NamedTextColor.GREEN))

			.append(Component.newline())
			.append(Component.text("Spawners left to break: "))
			.append(Component.text(mSpawnersToBreak, NamedTextColor.WHITE))

			.append(Component.newline())
			.append(Component.text("Floor number: "))
			.append(Component.text(getFloor(), NamedTextColor.WHITE))

			.append(Component.newline())
			.append(Component.text("Room number: "))
			.append(Component.text(mRoomNumber, NamedTextColor.WHITE))

			.append(Component.newline())
			.append(Component.text("Party number: "))
			.append(Component.text(mPartyNum, NamedTextColor.WHITE))

			.append(Component.newline())
			.append(Component.text("Treasure score: "))
			.append(Component.text(mTreasureScore, NamedTextColor.WHITE));

		if ((mEndlessMode || endlessModeUnlocked) && mContent == DepthsContent.DARKEST_DEPTHS) {
			result = result
				.append(Component.newline())
				.append(Component.text("Endless mode: "))
				.append(Component.text(mEndlessMode, NamedTextColor.WHITE));
		}

		if (mContent == DepthsContent.CELESTIAL_ZENITH) {
			result = result
				.append(Component.newline())
				.append(Component.text("Ascension level: "))
				.append(Component.text(mAscension, NamedTextColor.WHITE));
		}

		return result;
	}

	/**
	 * Called when a new room is spawned. Sets spawner count and resets reward availability for players.
	 *
	 * @param room the room that was just spawned for the party
	 */
	public void setNewRoom(DepthsRoom room, boolean wildcard) {
		mCurrentRoom = room;
		mCurrentRoomType = room.mRoomType;
		//Set to percentage of total spawners in the room
		mSpawnersToBreak = (int) (room.mSpawnerCount * DepthsManager.ROOM_SPAWNER_PERCENT);
		mRoomNumber++;
		mSpawnedReward = false;

		//Logic for entering a new floor
		if (mRoomNumber % 10 == 1) {
			for (DepthsPlayer p : mPlayersInParty) {
				//Reset chaos and wheel eligibility
				p.mUsedChaosThisFloor = false;
				p.mUsedWheelThisFloor = false;
				//Reset ability removal eligibility
				p.mUsedAbilityDeletion = false;
				p.mUsedAbilityMutation = false;
				p.mUsedGenerosity = false;
			}
			mTwistedThisFloor = false;
		}

		if (mCurrentRoomType == DepthsRoomType.TWISTED) {
			mTwistedThisFloor = true;
		}

		//Add to room history so party can't get it again
		mOldRooms.add(room);

		//Give extra rewards to all players if it's the starter room in depths 2

		if (mRoomNumber == 1 && mContent == DepthsContent.CELESTIAL_ZENITH) {
			try {
				DepthsRewardType rewardType = DepthsUtils.rewardFromRoom(room.mRoomType);
				if (rewardType != null) {
					for (DepthsPlayer dp : mPlayersInParty) {
						dp.mEarnedRewards.add(DepthsRewardType.PRISMATIC);
						dp.mEarnedRewards.add(DepthsRewardType.ABILITY);
						dp.mEarnedRewards.add(DepthsRewardType.ABILITY);
						dp.mEarnedRewards.add(DepthsRewardType.ABILITY);
					}
				}
				// Add delve points for ascension
				if (mAscension > 0) {
					int totalPoints = 0;
					for (int x : DepthsEndlessDifficulty.ASCENSION_DELVE_POINTS) {
						if (x <= mAscension) {
							totalPoints += DepthsEndlessDifficulty.ASCENSION_DELVE_POINTS_AMOUNT;
						}
					}
					DepthsEndlessDifficulty.applyDelvePointsToParty(this, totalPoints / 2, mDelveModifiers, false);
					if (mAscension >= 13) {
						DepthsEndlessDifficulty.applyDelvePointsToParty(this, totalPoints / 2, mDelveModifiers, true);
					}

				}
			} catch (Exception e) {
				MMLog.warning("Null depths party member");
				e.printStackTrace();
			}
		}

		//Make all players eligible for rewards again if spawner count is 0
		if (room.mSpawnerCount == 0) {
			try {
				DepthsRewardType rewardType = DepthsUtils.rewardFromRoom(room.mRoomType);
				if (rewardType != null) {
					for (DepthsPlayer dp : mPlayersInParty) {
						dp.mEarnedRewards.add(rewardType);
					}
				}
			} catch (Exception e) {
				MMLog.warning("Null depths party member");
				e.printStackTrace();
			}
		}

		sendMessage("Spawned new " + room.mRoomType.getRoomString() + " room" + (wildcard ? " (Wildcard)" : "") + "!");

		mBeatBoss = false;
	}

	//Spawns the reward chest in the given location - needs a 1s delay to not be overwritten
	//By spawner break event
	public static class PlaceChest extends BukkitRunnable {

		Location mLocation;

		public PlaceChest(Location l) {
			mLocation = l;
		}

		@Override
		public void run() {
			mLocation.getBlock().setType(Material.CHEST);
		}
	}

	//Slime chest runnable
	public static class ItemEffects extends BukkitRunnable {

		Location mLocation;
		int mSeconds;
		@Nullable Entity mSlime;

		public ItemEffects(Location l, @Nullable Entity e) {
			mLocation = l;
			mSeconds = 0;
			mSlime = e;
		}

		@Override
		public void run() {
			if (!mLocation.isWorldLoaded()) {
				this.cancel();
				return;
			}
			World world = mLocation.getWorld();

			if (mSeconds == 2 || mSeconds == 4 || mSeconds == 6) {
				world.playSound(mLocation, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 0.8f, 1.0f);
			}

			if (mSeconds >= 60 && mSlime != null) {
				mSlime.remove();
				this.cancel();
			}

			mSeconds += 1;
		}
	}

	public int getFloor() {
		if (mRoomNumber <= 0) {
			return 1;
		}
		return ((mRoomNumber - 1) / 10) + 1;
	}

	public int getRoomNumber() {
		return mRoomNumber;
	}

	//Sends player to, and fills, the next open loot room
	public void populateLootRoom(Player p, boolean victory) {
		int maxLootRooms = mIsSixPlayerMode ? MAX_LOOT_ROOMS_EXPANDED : MAX_LOOT_ROOMS;
		DepthsPlayer dp = DepthsManager.getInstance().getDepthsPlayer(p);
		if (dp == null) {
			// This should be very impossible
			MMLog.warning(p.getName() + " respawned in Depths while being in a party but not in the depths system");
			return;
		}
		if (mLootRoomLocations.size() > mLootRoomsUsed && maxLootRooms > mLootRoomsUsed) {
			Location lootRoomLoc = new Location(p.getWorld(), mLootRoomLocations.get(mLootRoomsUsed).getX(), mLootRoomLocations.get(mLootRoomsUsed).getY(), mLootRoomLocations.get(mLootRoomsUsed).getZ());
			p.teleport(lootRoomLoc, PlayerTeleportEvent.TeleportCause.UNKNOWN);
			p.setBedSpawnLocation(lootRoomLoc, true);
			p.addScoreboardTag(Constants.Tags.NO_TRANSPOSING);
			mLootRoomsUsed++;
			dp.sendMessage("Sending you to loot room " + mLootRoomsUsed);

			//Remove delve score if applicable
			Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> DelvesUtils.clearDelvePlayerByShard(null, p, ServerProperties.getShardName()), 80);

			p.removeScoreboardTag(DepthsManager.ENDLESS_MODE_STRING);

			int treasureScore = dp.mFinalTreasureScore;
			if (treasureScore == -1) {
				//Player beat floor 3 - give them the party's current treasure score
				treasureScore = mTreasureScore;
			}

			int roomReached = dp.getDeathRoom();

			DepthsManager.getInstance().deletePlayer(p);
			mPlayersInParty.remove(dp);
			int lootRoomTreasure = mIsSixPlayerMode ? (int) Math.ceil(treasureScore * 0.75) : treasureScore;
			if (getContent() == DepthsContent.DARKEST_DEPTHS) {
				DepthsLoot.generateLoot(lootRoomLoc.clone().add(DepthsLoot.LOOT_ROOM_LOOT_OFFSET), lootRoomTreasure, p, (roomReached > 120) && !mIsSixPlayerMode);
			} else {
				ZenithLoot.generateLoot(lootRoomLoc.clone().add(ZenithLoot.LOOT_ROOM_LOOT_OFFSET), lootRoomTreasure, p, getAscension() >= 15 && !mIsSixPlayerMode && victory, getAscension(), victory);
			}

			//Set their highest room score and do announcements
			int highestRoom;
			int maxAscension;
			if (!mIsSixPlayerMode) {
				if (getContent() == DepthsContent.DARKEST_DEPTHS) {
					highestRoom = ScoreboardUtils.getScoreboardValue(p, ENDLESS_LEADERBOARD).orElse(0);
					if (roomReached > highestRoom) {
						ScoreboardUtils.setScoreboardValue(p, ENDLESS_LEADERBOARD, roomReached);
						Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "leaderboard update " + p.getName() + " " + ENDLESS_LEADERBOARD);
					}
					if (roomReached > 30) {
						if (roomReached > highestRoom) {
							MonumentaNetworkRelayIntegration.broadcastCommand("tellraw @a[all_worlds=true] [\"\",{\"text\":\"" + p.getName() + "\",\"color\":\"gold\",\"bold\":false,\"italic\":true},{\"text\":\" defeated the Darkest Depths with a new personal best! (Endless Room Reached: " + roomReached + ")\",\"color\":\"white\",\"italic\":true,\"bold\":false}]");
						} else {
							Bukkit.getServer().sendMessage(Component.empty()
								.append(Component.text(p.getName(), NamedTextColor.GOLD, TextDecoration.ITALIC))
								.append(Component.text(" defeated the Darkest Depths! (Endless Room Reached: "
									                       + roomReached + ")", NamedTextColor.YELLOW, TextDecoration.ITALIC)));
						}
					}
				} else if (getContent() == DepthsContent.CELESTIAL_ZENITH) {
					maxAscension = ScoreboardUtils.getScoreboardValue(p, ASCENSION_LEADERBOARD).orElse(0);
					if (victory && getAscension() > maxAscension) {
						ScoreboardUtils.setScoreboardValue(p, ASCENSION_LEADERBOARD, getAscension());
						Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "leaderboard update " + p.getName() + " " + ASCENSION_LEADERBOARD);
						MonumentaNetworkRelayIntegration.broadcastCommand("tellraw @a[all_worlds=true] [\"\",{\"text\":\"" + p.getName() + "\",\"color\":\"gold\",\"bold\":false,\"italic\":true},{\"text\":\" defeated the Celestial Zenith with a new personal best! (Ascension Level: " + getAscension() + ")\",\"color\":\"white\",\"italic\":true,\"bold\":false}]");
					} else if (victory && getAscension() > 0) {
						Bukkit.getServer().sendMessage(Component.empty()
							.append(Component.text(p.getName(), NamedTextColor.GOLD, TextDecoration.ITALIC))
							.append(Component.text(" defeated the Celestial Zenith! (Ascension Level: "
								                       + getAscension() + ")", NamedTextColor.YELLOW, TextDecoration.ITALIC)));
					}
				}

			} else {
				// 6 player mode handler
				if (getContent() == DepthsContent.DARKEST_DEPTHS) {
					highestRoom = ScoreboardUtils.getScoreboardValue(p, EXPANDED_LEADERBOARD).orElse(0);
					if (roomReached > highestRoom) {
						ScoreboardUtils.setScoreboardValue(p, EXPANDED_LEADERBOARD, roomReached);
						NmsUtils.getVersionAdapter().runConsoleCommandSilently("leaderboard update " + p.getName() + " " + EXPANDED_LEADERBOARD);
					}
					if (roomReached > 30) {
						if (roomReached > highestRoom) {
							MonumentaNetworkRelayIntegration.broadcastCommand("tellraw @a[all_worlds=true] [\"\",{\"text\":\"" + p.getName() + "\",\"color\":\"gold\",\"bold\":false,\"italic\":true},{\"text\":\" defeated the Darkest Depths with a new personal best in six player mode! (Endless Room Reached: " + roomReached + ")\",\"color\":\"white\",\"italic\":true,\"bold\":false}]");
						} else {
							Bukkit.getServer().sendMessage(Component.empty()
								.append(Component.text(p.getName(), NamedTextColor.GOLD, TextDecoration.ITALIC))
								.append(Component.text(" defeated the Darkest Depths! (Endless Room Reached: "
									                       + roomReached + ")", NamedTextColor.YELLOW, TextDecoration.ITALIC)));
						}
					}
				} else if (getContent() == DepthsContent.CELESTIAL_ZENITH) {
					maxAscension = ScoreboardUtils.getScoreboardValue(p, SIX_ASCENSION_LEADERBOARD).orElse(0);
					if (victory && getAscension() > maxAscension) {
						ScoreboardUtils.setScoreboardValue(p, SIX_ASCENSION_LEADERBOARD, getAscension());
						Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "leaderboard update " + p.getName() + " " + SIX_ASCENSION_LEADERBOARD);
						MonumentaNetworkRelayIntegration.broadcastCommand("tellraw @a[all_worlds=true] [\"\",{\"text\":\"" + p.getName() + "\",\"color\":\"gold\",\"bold\":false,\"italic\":true},{\"text\":\" defeated the Celestial Zenith with a new personal best in six player mode! (Ascension Level: " + getAscension() + ")\",\"color\":\"white\",\"italic\":true,\"bold\":false}]");
					} else if (victory) {
						Bukkit.getServer().sendMessage(Component.empty()
							.append(Component.text(p.getName(), NamedTextColor.GOLD, TextDecoration.ITALIC))
							.append(Component.text(" defeated the Celestial Zenith! (Ascension Level: "
								                       + getAscension() + ")", NamedTextColor.YELLOW, TextDecoration.ITALIC)));
					}
				}

			}
			if (getContent() == DepthsContent.DARKEST_DEPTHS) {
				SeasonalEventListener.playerCompletedDepths(p, roomReached);
			} else {
				SeasonalEventListener.playerCompletedZenith(p, roomReached, mAscension);
			}

			if (victory && getContent() == DepthsContent.DARKEST_DEPTHS) {
				Bukkit.getPluginManager().callEvent(new MonumentaEvent(p, "depths"));
			} else if (victory && getContent() == DepthsContent.CELESTIAL_ZENITH) {
				Bukkit.getPluginManager().callEvent(new MonumentaEvent(p, "zenith"));
			}


		} else {
			dp.sendMessage("Max loot rooms reached! This should never happen- please contact a moderator.");
			//Remove player from their party anyway
			DepthsManager.getInstance().deletePlayer(p);
			mPlayersInParty.remove(dp);
		}
	}

	public int getAscension() {
		if (mContent == DepthsContent.CELESTIAL_ZENITH) {
			return mAscension;
		}
		return 0;
	}

	// For ascension purging, checks if all depths players active have removed an ability this floor
	public boolean isAscensionPurgeMet() {
		for (DepthsPlayer p : mPlayersInParty) {
			// They can pass without removing only if they have no actives and no unclaimed rewards
			if (!p.mUsedAbilityDeletion && (!p.mEarnedRewards.isEmpty() || DepthsManager.getInstance().hasActiveAbility(p))) {
				return false;
			}
		}
		return true;
	}

	public int getRoomX() {
		return mRoomStartX;
	}

	public void setRoomX(int x) {
		mRoomStartX = x;
	}

	public void sendMessage(String message) {
		sendMessage(message, dp -> true);
	}

	public void sendMessage(Component message) {
		sendMessage(message, dp -> true);
	}

	public void sendMessage(String message, Predicate<DepthsPlayer> pred) {
		sendMessage(Component.text(message), pred);
	}

	public void sendMessage(Component message, Predicate<DepthsPlayer> pred) {
		mPlayersInParty.stream().filter(pred).forEach(dp -> dp.sendMessage(message));
	}

	public double getZenithHealthIncreaseMultiplier() {
		return 1 + mAscension * ZENITH_HEALTH_INCREASE_PER_ASCENSION;
	}

	public double getZenithDamageIncreaseMultiplier() {
		return 1 + mAscension * ZENITH_DAMAGE_INCREASE_PER_ASCENSION;
	}

	public DepthsContent getContent() {
		return mContent;
	}

	public static double getAscensionScaledHealth(double baseHealth, @Nullable DepthsParty party) {
		return baseHealth * (party != null ? party.getZenithHealthIncreaseMultiplier() : 1);
	}

	public static double getAscensionScaledDamage(double baseDamage, @Nullable DepthsParty party) {
		return baseDamage * (party != null ? party.getZenithDamageIncreaseMultiplier() : 1);
	}

	public static int getAscensionEightCooldown(int baseCooldown, @Nullable DepthsParty party) {
		return (party != null && party.getAscension() >= 8) ? baseCooldown - 40 : baseCooldown;
	}

	public List<Player> getPlayers() {
		return mPlayersInParty.stream().map(DepthsPlayer::getPlayer).filter(Objects::nonNull).toList();
	}
}
