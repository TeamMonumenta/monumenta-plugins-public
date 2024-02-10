package com.playmonumenta.plugins.depths.rooms;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsContent;
import com.playmonumenta.plugins.depths.DepthsEndlessDifficulty;
import com.playmonumenta.plugins.depths.DepthsParty;
import com.playmonumenta.plugins.depths.DepthsPlayer;
import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.rooms.DepthsRoom.RoomDirection;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.scriptedquests.managers.SongManager;
import com.playmonumenta.structures.StructuresAPI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
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

/**
 * This class contains hard coded information about all the rooms possible in the system, including type, spawner count, load paths, etc.
 * It also is responsible for picking random rooms for the party and dynamically structure loading them into the world.
 *
 * @author ShadowVisions
 */
public abstract class RoomRepository {

	public static final int CUSTOM_FLOOR_LOBBIES = 10;

	//List of rooms for each floor
	final ArrayList<DepthsRoom> mF1NormalRooms = new ArrayList<>();
	final ArrayList<DepthsRoom> mF1EliteRooms = new ArrayList<>();
	final ArrayList<DepthsRoom> mF1UtilityRooms = new ArrayList<>();
	final ArrayList<DepthsRoom> mF1TwistedRooms = new ArrayList<>();
	final ArrayList<DepthsRoom> mF2NormalRooms = new ArrayList<>();
	final ArrayList<DepthsRoom> mF2EliteRooms = new ArrayList<>();
	final ArrayList<DepthsRoom> mF2UtilityRooms = new ArrayList<>();
	final ArrayList<DepthsRoom> mF2TwistedRooms = new ArrayList<>();
	final ArrayList<DepthsRoom> mF3NormalRooms = new ArrayList<>();
	final ArrayList<DepthsRoom> mF3EliteRooms = new ArrayList<>();
	final ArrayList<DepthsRoom> mF3UtilityRooms = new ArrayList<>();
	final ArrayList<DepthsRoom> mF3TwistedRooms = new ArrayList<>();
	@Nullable DepthsRoom mF1BossRoom;
	@Nullable DepthsRoom mF2BossRoom;
	@Nullable DepthsRoom mF3BossRoom;
	@Nullable DepthsRoom mF1RemoveRoom;
	@Nullable DepthsRoom mF2RemoveRoom;
	@Nullable DepthsRoom mF3RemoveRoom;
	@Nullable DepthsRoom mWeaponAspectRoom;

	private boolean mRoomCleanseSpawned = false;

	public RoomRepository() {
		initRooms();
	}

	/**
	 * Rooms are stored in the depths system here
	 * Rooms have an entry point that describes the offset from the door to the structure corner of the room
	 * They also have a spawner count to track player progress and a vector to represent the size.
	 */
	protected abstract void initRooms();

	protected abstract String getLobbyPath(int nextFloor);

	/**
	 * Spawns the next room in the physical world
	 * @param spawnPoint the coordinates to load the room
	 * @param roomType the type of the room to select from
	 * @return the room information for the selected room
	 */
	public DepthsRoom summonRoom(Location spawnPoint, DepthsRoomType roomType, DepthsParty party) {
		//Get a valid room from the options available to the party
		DepthsRoom room = null;

		int floor = party.getFloor();
		// Exception case- ascension 10+ and players must still remove abilities
		if (party.getRoomNumber() % 10 == 9 && party.getAscension() >= DepthsEndlessDifficulty.ASCENSION_ABILITY_PURGE && !party.isAscensionPurgeMet()) {
			if(this.mRoomCleanseSpawned) {
				// TODO: Find a better way to terminate room spawn
				throw new IllegalStateException("Each player must remove an ability before moving on!");
			}

			// Rain: Why are we doing this wrap-around thing here?
			if (floor % 3 == 1) {
				room = mF1RemoveRoom;
			}
			if (floor % 3 == 2) {
				room = mF2RemoveRoom;
			}
			if (floor % 3 == 0) {
				room = mF3RemoveRoom;
			}
			party.mRoomNumber--;
			party.sendMessage("Each player must remove an ability before moving on!");
			this.mRoomCleanseSpawned = true; // Set the flag that we've spawned a forced washroom already
			party.mNoPassiveRemoveRoomStartX = Math.min(party.mNoPassiveRemoveRoomStartX, spawnPoint.getBlockX());
		} else {
			//Standard case- call valid room
			this.mRoomCleanseSpawned = false; // Unset the flag
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
		for (Entity e: w.getNearbyEntities(box)) {
			if (!(e instanceof Player)) {
				e.remove();
			}
		}

		party.setRoomX(spawn.getBlockX());

		MMLog.info("Summoning structure " + room.mLoadPath);
		StructuresAPI.loadAndPasteStructure(room.mLoadPath, spawn, true, true);

		if (DepthsUtils.getDepthsContent() == DepthsContent.CELESTIAL_ZENITH && roomType == DepthsRoomType.BOSS) {
			List<Player> players = party.getPlayers();
			if (floor == 2) {
				SongManager.playSong(players, new SongManager.Song("epic:music.broodmother_ambient", SoundCategory.RECORDS, 2 * 60, true, 1.0f, 1.0f, true), true);
			} else if (floor == 3) {
				SongManager.playSong(players, new SongManager.Song("epic:music.vesperidys_ambient", SoundCategory.RECORDS, 1 * 60 + 27, true, 1.0f, 1.0f, true), true);
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

		//Get floor number- +1 if they're in the lobby after beating boss
		int floor = party.getFloor();
		if (party.mRoomNumber % 10 == 0) {
			floor++;
		}

		//Give them a boss room if they are on a boss

		if (roomType == DepthsRoomType.BOSS) {
			if (floor % 3 == 1) {
				return mF1BossRoom;
			}
			if (floor % 3 == 2) {
				return mF2BossRoom;
			}
			if (floor % 3 == 0) {
				return mF3BossRoom;
			}
		}
		//Switch room types by floor
		if (floor % 3 == 1) {
			if (roomType == DepthsRoomType.ABILITY || roomType == DepthsRoomType.TREASURE || roomType == DepthsRoomType.UPGRADE) {
				return randomRoom(party, mF1NormalRooms, yLevel);
			} else if (roomType == DepthsRoomType.ABILITY_ELITE || roomType == DepthsRoomType.TREASURE_ELITE || roomType == DepthsRoomType.UPGRADE_ELITE) {
				return randomRoom(party, mF1EliteRooms, yLevel);
			} else if (roomType == DepthsRoomType.UTILITY) {
				return randomRoom(party, mF1UtilityRooms, yLevel);
			} else if (roomType == DepthsRoomType.TWISTED) {
				return randomRoom(party, mF1TwistedRooms, yLevel);
			}
		} else if (floor % 3 == 2) {
			if (roomType == DepthsRoomType.ABILITY || roomType == DepthsRoomType.TREASURE || roomType == DepthsRoomType.UPGRADE) {
				return randomRoom(party, mF2NormalRooms, yLevel);
			} else if (roomType == DepthsRoomType.ABILITY_ELITE || roomType == DepthsRoomType.TREASURE_ELITE || roomType == DepthsRoomType.UPGRADE_ELITE) {
				return randomRoom(party, mF2EliteRooms, yLevel);
			} else if (roomType == DepthsRoomType.UTILITY) {
				return randomRoom(party, mF2UtilityRooms, yLevel);
			} else if (roomType == DepthsRoomType.TWISTED) {
				return randomRoom(party, mF2TwistedRooms, yLevel);
			}
		} else if (floor % 3 == 0) {
			if (roomType == DepthsRoomType.ABILITY || roomType == DepthsRoomType.TREASURE || roomType == DepthsRoomType.UPGRADE) {
				return randomRoom(party, mF3NormalRooms, yLevel);
			} else if (roomType == DepthsRoomType.ABILITY_ELITE || roomType == DepthsRoomType.TREASURE_ELITE || roomType == DepthsRoomType.UPGRADE_ELITE) {
				return randomRoom(party, mF3EliteRooms, yLevel);
			} else if (roomType == DepthsRoomType.UTILITY) {
				return randomRoom(party, mF3UtilityRooms, yLevel);
			} else if (roomType == DepthsRoomType.TWISTED) {
				return randomRoom(party, mF3TwistedRooms, yLevel);
			}
		}
		//No valid room found, big oof
		return null;
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
	 * @param party the party to send to the next floor
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
		int nextFloorNum = party.getFloor() + 1;
		String path = getLobbyPath(nextFloorNum);

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
				PlayerUtils.executeCommandOnNearbyPlayers(l, 20, "stopsound @s record");
				//Tp all the players to it
				for (DepthsPlayer dp : party.mPlayersInParty) {
					Player p = Bukkit.getPlayer(dp.mPlayerId);
					if (p == null) {
						dp.offlineTeleport(l);
					} else {
						p.teleport(l, PlayerTeleportEvent.TeleportCause.UNKNOWN);
						PotionUtils.applyPotion(Plugin.getInstance(), p, new PotionEffect(PotionEffectType.BLINDNESS, 2 * 20, 2));
					}
					PlayerUtils.executeCommandOnNearbyPlayers(l, 20, "stopsound @s record");
				}
				party.sendMessage("Your party earned " + treasure + " treasure score for clearing floor " + party.getFloor() + "! Sending your party to next floor.");
				//Reset used rooms
				party.mOldRooms.clear();
				//Just in case they get stuck, set the spawner break trigger to zero
				party.mSpawnersToBreak = 0;
				party.mNoPassiveRemoveRoomStartX = Integer.MAX_VALUE;
			}
		});
	}
}
