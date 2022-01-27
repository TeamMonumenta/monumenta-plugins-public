package com.playmonumenta.plugins.depths;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.depths.DepthsRoomType.DepthsRewardType;
import com.playmonumenta.plugins.integrations.MonumentaNetworkRelayIntegration;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DelvesUtils;
import com.playmonumenta.plugins.utils.DelvesUtils.Modifier;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.scriptedquests.Plugin;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * This class handles dynamic information about depths parties, including key information about the world like locations,
 * as well as run specific information including progress and events, and distribution of loot rooms.
 *
 * @author ShadowVisions
 */
public class DepthsParty {
	public static final int MAX_LOOT_ROOMS = 4;

	// The difference between where the player spawns in a loot room, and where the loot needs to be dropped
	// Transient- don't try to save a circular reference to players (will crash data save)
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
	public Map<Modifier, Integer> mDelveModifiers;
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

	/**
	 * Creates a new depths party with the given players
	 */
	public DepthsParty(List<DepthsPlayer> players, Location loc) {
		mPlayersInParty = players;
		mWorldUUID = loc.getWorld().getUID();

		for (DepthsPlayer dp : players) {

			//Endless mode detection
			Player p = Bukkit.getPlayer(dp.mPlayerId);
			p.removeScoreboardTag(DepthsManager.PAID_SCOREBOARD_TAG);
			if (p.getScoreboardTags().contains(DepthsManager.ENDLESS_MODE_STRING)) {
				mEndlessMode = true;
				p.removeScoreboardTag(DepthsManager.ENDLESS_MODE_STRING);
			}

			if (p.getScoreboardTags().contains(DepthsManager.RIGGED_STRING)) {
				mIsRigged = true;
				p.removeScoreboardTag(DepthsManager.RIGGED_STRING);
			}

			if (mPartyNum <= 0) {
				mPartyNum = ScoreboardUtils.getScoreboardValue(p, "DDAccess").orElse(0);
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
					DepthsManager.getInstance().getRandomAbility(p, dp, chances);
				}
			}

		}.runTaskLater(Plugin.getInstance(), 5);


		mRoomNumber = 0;
		mSpawnersToBreak = 0;
		mHasAtLeastOneAbility = false;
		mOldRooms = new ArrayList<DepthsRoom>();
		mLootRoomLocations = new ArrayList<Vector>();
		mDelveModifiers = new HashMap<>();
		mSpawnedReward = false;
		mRoomStartX = loc.getBlockX();

		//Attempt to set locations for the next floor lobby to load
		World world = Plugin.getInstance().mWorld;
		Collection<ArmorStand> nearbyStands = world.getNearbyEntitiesByType(ArmorStand.class, loc, 60.0);
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
		}
	}

	public void addPlayerToParty(DepthsPlayer player) {
		mPlayersInParty.add(player);
		player.mPartyNum = this.mPartyNum;

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

		boolean spawnSlime = false;
		//Check if the room is complete and we didnt already reward them
		if (mSpawnersToBreak == 0 && !mSpawnedReward && mCurrentRoom.mSpawnerCount > 0) {

			for (DepthsPlayer dp : mPlayersInParty) {
				if (dp != null) {
					DepthsRewardType rewardType = DepthsUtils.rewardFromRoom(mCurrentRoomType);
					if (rewardType != null) {
						dp.mEarnedRewards.add(rewardType);
						spawnSlime = true;
					}
					Player partyMember = Bukkit.getPlayer(dp.mPlayerId);
					if (partyMember != null) {
						partyMember.sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + "This room's " + DepthsUtils.rewardString(mCurrentRoomType) + " reward has been found!");
					}
				}
			}
			if (mCurrentRoomType == DepthsRoomType.TREASURE || mCurrentRoomType == DepthsRoomType.TREASURE_ELITE) {
				mCanGetTreasureReward = true;
			}

			//Allow players to get the reward

			mSpawnedReward = true;
			//For ability and upgrade rewards, spawn a chest to interface with them

			l.getWorld().playSound(l, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.8f, 1.0f);
			l.getWorld().playSound(l, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
			DepthsUtils.animate(l);
			spawnRoomReward(l, spawnSlime);
		}
	}

	public void giveTreasureReward(Location l, int score) {

		mTreasureScore += score;
		mCanGetTreasureReward = false;

		for (DepthsPlayer dp : mPlayersInParty) {

			Player p = Bukkit.getPlayer(dp.mPlayerId);
			if (p != null) {
				p.playSound(p.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 1.0f, 1.0f);
				p.sendActionBar(ChatColor.GOLD + "" + score + " treasure score added to loot room!");
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
	 * @return info about the party as a string to print
	 */
	public String getSummaryString() {
		String lineStart = "\n" + ChatColor.GOLD;
		String ret = ChatColor.LIGHT_PURPLE + "Depths Party Summary:";
		ret += lineStart + "Players in party: " + ChatColor.GREEN;
		boolean endlessModeUnlocked = false;
		for (DepthsPlayer dp : mPlayersInParty) {
			Player p = Bukkit.getPlayer(dp.mPlayerId);
			if (p != null && p.isOnline()) {
				ret += "" + p.getName() + " ";
				if (ScoreboardUtils.getScoreboardValue(p, "Depths").orElse(0) > 0) {
					endlessModeUnlocked = true;
				}
			}
		}
		ret += lineStart + "Spawners left to break: " + ChatColor.WHITE + mSpawnersToBreak;
		ret += lineStart + "Floor number: " + ChatColor.WHITE + getFloor();
		ret += lineStart + "Room number: " + ChatColor.WHITE + mRoomNumber;
		ret += lineStart + "Party number: " + ChatColor.WHITE + mPartyNum;
		ret += lineStart + "Treasure score: " + ChatColor.WHITE + mTreasureScore;
		if (mEndlessMode || endlessModeUnlocked) {
			ret += lineStart + "Endless mode: " + ChatColor.WHITE + mEndlessMode;
		}
		return ret;
	}

	/**
	 * Called when a new room is spawned. Sets spawner count and resets reward availability for players.
	 * @param room the room that was just spawned for the party
	 */
	public void setNewRoom(DepthsRoom room) {
		mCurrentRoom = room;
		mCurrentRoomType = room.mRoomType;
		//Set to percentage of total spawners in the room
		mSpawnersToBreak = (int) (room.mSpawnerCount * DepthsManager.ROOM_SPAWNER_PERCENT);
		mRoomNumber++;
		mSpawnedReward = false;

		//Logic for entering a new floor
		if (mRoomNumber % 10 == 1) {
			for (DepthsPlayer p : mPlayersInParty) {
				//Reset chaos eligibility
				p.mUsedChaosThisFloor = false;
				//Reset ability removal eligibility
				p.mUsedAbilityDeletion = false;
			}
			mTwistedThisFloor = false;
		}

		if (mCurrentRoomType == DepthsRoomType.TWISTED) {
			mTwistedThisFloor = true;
		}

		//Add to room history so party can't get it again
		mOldRooms.add(room);

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
				Plugin.getInstance().getLogger().info("Null depths party member");
			}
		}

		for (DepthsPlayer dp : mPlayersInParty) {
			Player p = Bukkit.getPlayer(dp.mPlayerId);
			if (p != null) {
				Bukkit.getPlayer(dp.mPlayerId).sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + "Spawned new " + DepthsUtils.roomString(room.mRoomType) + " room!");
			}
		}

		mBeatBoss = false;
	}

	//Spawns the reward chest in the given location- needs a 1s delay to not be overwritten
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
		Entity mSlime;

		public ItemEffects(Location l, Entity e) {
			mLocation = l;
			mSeconds = 0;
			mSlime = e;
		}

		@Override
		public void run() {
			World world = mLocation.getWorld();

			if (mSeconds == 2 || mSeconds == 4 || mSeconds == 6) {
				world.playSound(mLocation, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.8f, 1.0f);
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
		if (mLootRoomLocations.size() > mLootRoomsUsed && MAX_LOOT_ROOMS > mLootRoomsUsed) {
			int roomReached = mRoomNumber;
			Location lootRoomLoc = new Location(p.getWorld(), mLootRoomLocations.get(mLootRoomsUsed).getX(), mLootRoomLocations.get(mLootRoomsUsed).getY(), mLootRoomLocations.get(mLootRoomsUsed).getZ());
			p.teleport(lootRoomLoc);
			p.setBedSpawnLocation(lootRoomLoc, true);
			p.addScoreboardTag(Constants.Tags.NO_TRANSPOSING);
			mLootRoomsUsed++;
			p.sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + "Sending you to loot room " + mLootRoomsUsed);
			//Calculate their treasure score, remove them from the depths system/party, spawn loot into the room
			int treasureScore = -1;
			//Remove delve score if applicable
			new BukkitRunnable() {

				@Override
				public void run() {
					DelvesUtils.setDelveScore(p, ServerProperties.getShardName(), 0);
					DelvesUtils.removeDelveInfo(p);
				}

			}.runTaskLater(Plugin.getInstance(), 80);

			p.removeScoreboardTag(DepthsManager.ENDLESS_MODE_STRING);
			DepthsPlayer dp = DepthsManager.getInstance().mPlayers.get(p.getUniqueId());
			if (dp != null) {
				treasureScore = dp.mFinalTreasureScore;
				if (treasureScore == -1) {
					//Player beat floor 3- give them the party's current treasure score
					treasureScore = mTreasureScore;
				}
				DepthsManager.getInstance().deletePlayer(p);
				mPlayersInParty.remove(dp);
				if (roomReached > 120) {
					DepthsLoot.generateLoot(lootRoomLoc.clone().add(DepthsLoot.LOOT_ROOM_LOOT_OFFSET), treasureScore, p, true);
				} else {
					DepthsLoot.generateLoot(lootRoomLoc.clone().add(DepthsLoot.LOOT_ROOM_LOOT_OFFSET), treasureScore, p, false);
				}
			}
			//Set their highest room score and do announcements
			int highestRoom = ScoreboardUtils.getScoreboardValue(p, "DepthsEndless").orElse(0);
			if (roomReached > highestRoom) {
				ScoreboardUtils.setScoreboardValue(p, "DepthsEndless", roomReached);
				Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "leaderboard update " + p.getDisplayName() + " DepthsEndless");
			}
			if (victory) {
				MonumentaNetworkRelayIntegration.broadcastCommand("tellraw @a [\"\",{\"text\":\"" + p.getDisplayName() + "\",\"color\":\"gold\",\"bold\":false,\"italic\":true},{\"text\":\" defeated the Darkest Depths! (Endless Room Reached: " + roomReached + ")\",\"color\":\"white\",\"italic\":true,\"bold\":false}]");
			}

		} else {
			p.sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + "Max loot rooms reached! This should never happen- please contact a moderator.");
			DepthsPlayer dp = DepthsManager.getInstance().mPlayers.get(p.getUniqueId());
			if (dp != null) {
				//Remove player from their party anyway
				DepthsManager.getInstance().deletePlayer(p);
				mPlayersInParty.remove(dp);
			}
		}
	}

	public int getRoomX() {
		return mRoomStartX;
	}

	public void setRoomX(int x) {
		mRoomStartX = x;
	}
}
