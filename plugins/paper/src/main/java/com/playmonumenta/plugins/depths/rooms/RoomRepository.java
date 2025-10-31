package com.playmonumenta.plugins.depths.rooms;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsContent;
import com.playmonumenta.plugins.depths.DepthsEndlessDifficulty;
import com.playmonumenta.plugins.depths.DepthsParty;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.bosses.Broodmother;
import com.playmonumenta.plugins.depths.bosses.Vesperidys;
import com.playmonumenta.plugins.depths.rooms.DepthsRoom.RoomDirection;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.scriptedquests.managers.SongManager;
import com.playmonumenta.structures.StructuresAPI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.Nullable;

/**
 * This class contains hard coded information about all the rooms possible in the system, including type, spawner count, load paths, etc.
 * It also is responsible for picking random rooms for the party and dynamically structure loading them into the world.
 *
 * @author ShadowVisions
 */
public abstract class RoomRepository {

	public static class RepositoryFloor {
		public final List<DepthsRoom> mNormalRooms = new ArrayList<>();
		public final List<DepthsRoom> mEliteRooms = new ArrayList<>();
		public final List<DepthsRoom> mUtilityRooms = new ArrayList<>();
		public final List<DepthsRoom> mTwistedRooms = new ArrayList<>();
		public @Nullable DepthsRoom mBossRoom;
		public @Nullable DepthsRoom mRemoveRoom;
	}

	public static final int CUSTOM_FLOOR_LOBBIES = 10;

	final RepositoryFloor mFloor1;
	final RepositoryFloor mFloor2;
	final RepositoryFloor mFloor3;
	final DepthsRoom mWeaponAspectRoom;

	public RoomRepository(RepositoryFloor f1, RepositoryFloor f2, RepositoryFloor f3, DepthsRoom weaponAspectRoom) {
		mFloor1 = f1;
		mFloor2 = f2;
		mFloor3 = f3;
		mWeaponAspectRoom = weaponAspectRoom;
	}

	protected abstract String getLobbyPath(int nextFloor);

	/**
	 * Spawns the next room in the physical world
	 *
	 * @param spawnPoint the coordinates to load the room
	 * @param roomType   the type of the room to select from
	 * @return the room information for the selected room.
	 */
	public DepthsRoom summonRoom(Location spawnPoint, DepthsRoomType roomType, DepthsParty party) {
		//Get a valid room from the options available to the party
		DepthsRoom room;

		int floor = party.getFloor();
		// Exception case- ascension 10+ and players must still remove abilities
		if (party.getRoomNumber() % 10 == 9 && party.getAscension() >= DepthsEndlessDifficulty.ASCENSION_ABILITY_PURGE && !party.isAscensionPurgeMet()) {
			RepositoryFloor rf = getFloor(floor);
			room = rf.mRemoveRoom;

			party.mRoomNumber--;
			party.sendMessage("Each player must remove an ability before moving on!");
			party.mNoPassiveRemoveRoomStartX = Math.min(party.mNoPassiveRemoveRoomStartX, spawnPoint.getBlockX());

			party.mSpawnedForcedCleansingRoom = true;
		} else {
			//Standard case- call valid room
			room = getValidRoom(roomType, party, spawnPoint.getY());
		}
		if (room == null) {
			throw new IllegalStateException("No valid room found to spawn!");
		}
		room.mRoomType = roomType;
		//Gets the point in the world to load it and physically summons it
		Location spawn = spawnPoint.clone().add(room.mEntry);

		//Get box of room and entities to overwrite and remove
		World w = spawnPoint.getWorld();
		BoundingBox box = w.getBlockAt(spawn).getBoundingBox();
		box.expand(BlockFace.EAST, room.mSize.getX());
		box.expand(BlockFace.UP, room.mSize.getY());
		box.expand(BlockFace.DOWN, room.mSize.getY());
		box.expand(BlockFace.NORTH, room.mSize.getZ());
		box.expand(BlockFace.SOUTH, room.mSize.getZ());
		for (Entity e : w.getNearbyEntities(box)) {
			if (!(e instanceof Player)) {
				e.remove();
			}
		}

		party.setRoomX(spawn.getBlockX());

		party.mLastLoadStartTick = Bukkit.getCurrentTick();
		MMLog.info("Summoning structure " + room.mLoadPath);
		StructuresAPI.loadAndPasteStructure(room.mLoadPath, spawn, true, true)
			.whenComplete((unused, ex) -> {
				party.mLastLoadStartTick = 0;
			});

		if (DepthsUtils.getDepthsContent() == DepthsContent.CELESTIAL_ZENITH && roomType == DepthsRoomType.BOSS) {
			List<Player> players = party.getPlayers();
			if (floor == 2) {
				SongManager.playSong(players, new SongManager.Song(Broodmother.MUSIC_TITLE_AMBIENT, SoundCategory.RECORDS, Broodmother.MUSIC_DURATION_AMBIENT, true, 1.0f, 1.0f, true), true);
			} else if (floor == 3) {
				SongManager.playSong(players, new SongManager.Song(Vesperidys.MUSIC_TITLE_AMBIENT, SoundCategory.RECORDS, Vesperidys.MUSIC_DURATION_AMBIENT, true, 1.0f, 1.0f, true), true);
			}
		}

		return room;
	}

	/**
	 * This method figures out which room to give the players depending on their type selection and some random chance.
	 *
	 * @param roomType type of room to summon
	 * @param party    depths party to check info for
	 * @param yLevel   what level the spawn point is currently at, to avoid going into the void or sky
	 * @return a valid room object to summon
	 */
	public @Nullable DepthsRoom getValidRoom(DepthsRoomType roomType, DepthsParty party, double yLevel) {
		//Give them the first room if they just started
		if (party.mRoomNumber == 0) {
			return mWeaponAspectRoom;
		}

		// floor will automatically be incremented after boss is defeated
		int floor = party.getFloor();
		RepositoryFloor rf = getFloor(floor);

		//Give them a boss room if they are on a boss
		if (roomType == DepthsRoomType.BOSS) {
			return rf.mBossRoom;
		}

		//Switch room types by floor
		List<DepthsRoom> rooms = null;
		if (roomType == DepthsRoomType.ABILITY || roomType == DepthsRoomType.TREASURE || roomType == DepthsRoomType.UPGRADE) {
			rooms = rf.mNormalRooms;
		} else if (roomType == DepthsRoomType.ABILITY_ELITE || roomType == DepthsRoomType.TREASURE_ELITE || roomType == DepthsRoomType.UPGRADE_ELITE) {
			rooms = rf.mEliteRooms;
		} else if (roomType == DepthsRoomType.UTILITY) {
			rooms = rf.mUtilityRooms;
		} else if (roomType == DepthsRoomType.TWISTED) {
			rooms = rf.mTwistedRooms;
		}
		if (rooms == null) {
			return null;
		}
		return randomRoom(party, rooms, yLevel);
	}

	//This method actually does the randomization aspect after finding the right array to roll from
	public DepthsRoom randomRoom(DepthsParty party, List<DepthsRoom> roomList, double yLevel) {
		List<DepthsRoom> oldRooms = party.mOldRooms;
		//Copy the list and shuffle it
		List<DepthsRoom> roomOptions = new ArrayList<>(roomList);
		Collections.shuffle(roomOptions);
		//Get the first room in the list the party hasn't gotten already
		for (DepthsRoom room : roomOptions) {
			//Account for height

			if ((yLevel < 70.0 && room.mDirection == RoomDirection.DOWN) || (yLevel > 446.0 && room.mDirection == RoomDirection.UP)) {
				continue;
			}
			if (!oldRooms.contains(room)) {
				return room;
			}
		}

		//No valid non-repeat rooms left, just return the next room that won't take them into void

		for (DepthsRoom room : roomOptions) {
			//Account for height

			if ((yLevel < 70.0 && room.mDirection == RoomDirection.DOWN) || (yLevel > 446.0 && room.mDirection == RoomDirection.UP)) {
				continue;
			}
			return room;
		}

		return roomOptions.get(0);
	}

	/**
	 * Spawns the lobby for the next floor for the given party, and teleports
	 * players to it/sets them up to continue playing
	 *
	 * @param party    the party to send to the next floor
	 * @param treasure the treasure score given to the party
	 */
	public void goToNextFloor(DepthsParty party, int treasure) {

		World world = Bukkit.getWorld(party.mWorldUUID);
		if (world == null) {
			MMLog.warning("Got null world from party's id - DepthsRoomRepository");
			return;
		}
		if (party.mFloorLobbyLoadPoint == null) {
			MMLog.severe("Depths party's mFloorLobbyLoadPoint not set, cannot go to next floor!");
			return;
		}
		Location loc = new Location(world, party.mFloorLobbyLoadPoint.getX(), party.mFloorLobbyLoadPoint.getY(), party.mFloorLobbyLoadPoint.getZ());

		//Separate rooms by floor here
		String path = getLobbyPath(party.getFloor());

		StructuresAPI.loadAndPasteStructure(path, loc, true, true).whenComplete((unused, ex) -> {
			if (ex != null) {
				ex.printStackTrace();
				party.sendMessage(Component.text("Failed to load lobby structure " + path + ". Contact a moderator.", NamedTextColor.RED));
			} else {
				if (party.mFloorLobbyLoadPlayerTpPoint == null) {
					MMLog.severe("Depths party's mFloorLobbyLoadPlayerTpPoint is not set, cannot go to next floor!");
					return;
				}
				Location l = new Location(world, party.mFloorLobbyLoadPlayerTpPoint.getX(), party.mFloorLobbyLoadPlayerTpPoint.getY(), party.mFloorLobbyLoadPlayerTpPoint.getZ(), 270.0f, 0.0f);
				for (Player player : party.getPlayers()) {
					player.stopSound(SoundCategory.RECORDS);
				}
				//Tp all the players to it
				for (DepthsPlayer dp : party.mPlayersInParty) {
					Player p = Bukkit.getPlayer(dp.mPlayerId);
					if (p == null || dp.mDead) {
						dp.offlineTeleport(l);
					} else {
						p.teleport(l, PlayerTeleportEvent.TeleportCause.UNKNOWN);
						PotionUtils.applyPotion(Plugin.getInstance(), p, new PotionEffect(PotionEffectType.BLINDNESS, 2 * 20, 2));
					}
				}
				party.sendMessage("Your party earned " + treasure + " treasure score for clearing floor " + (party.getFloor() - 1) + "! Sending your party to next floor.");
				//Reset used rooms
				party.mOldRooms.clear();
				//Just in case they get stuck, set the spawner break trigger to zero
				party.mSpawnersToBreak = 0;
				party.mNoPassiveRemoveRoomStartX = Integer.MAX_VALUE;
			}
		});
	}

	public RepositoryFloor getFloor(int floor) {
		int mod = floor % 3;
		if (mod == 1) {
			return mFloor1;
		} else if (mod == 2) {
			return mFloor2;
		} else {
			return mFloor3;
		}
	}
}
