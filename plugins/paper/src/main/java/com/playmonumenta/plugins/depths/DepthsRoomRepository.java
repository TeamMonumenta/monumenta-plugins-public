package com.playmonumenta.plugins.depths;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.depths.DepthsRoom.RoomDirection;
import com.playmonumenta.scriptedquests.Plugin;

/**
 * @author ShadowVisions
 *
 * This class contains hard coded information about all the rooms possible in the system, including type, spawner count, load paths, etc
 * It also is responsible for picking random rooms for the party and dynamically structure loading them into the world.
 *
 */
public class DepthsRoomRepository {

	public static final int CUSTOM_FLOOR_LOBBIES = 10;

	//List of rooms for each floor
	ArrayList<DepthsRoom> mF1NormalRooms;
	ArrayList<DepthsRoom> mF1EliteRooms;
	ArrayList<DepthsRoom> mF1UtilityRooms;
	ArrayList<DepthsRoom> mF2NormalRooms;
	ArrayList<DepthsRoom> mF2EliteRooms;
	ArrayList<DepthsRoom> mF2UtilityRooms;
	ArrayList<DepthsRoom> mF3NormalRooms;
	ArrayList<DepthsRoom> mF3EliteRooms;
	ArrayList<DepthsRoom> mF3UtilityRooms;
	DepthsRoom mF1BossRoom;
	DepthsRoom mF2BossRoom;
	DepthsRoom mF3BossRoom;
	DepthsRoom mWeaponAspectRoom;

	public DepthsRoomRepository(World world) {
		initRooms(world);
	}

	/**
	 * Rooms are stored in the depths system here
	 * Rooms have an entry point that describes the offset from the door to the structure corner of the room
	 * They also have a spawner count to track player progress and a vector to represent the size.
	 * @param world
	 */
	private void initRooms(World world) {
		//Init room arrays
		mF1NormalRooms = new ArrayList<>();
		mF1EliteRooms = new ArrayList<>();
		mF1UtilityRooms = new ArrayList<>();
		mF2NormalRooms = new ArrayList<>();
		mF2EliteRooms = new ArrayList<>();
		mF2UtilityRooms = new ArrayList<>();
		mF3NormalRooms = new ArrayList<>();
		mF3EliteRooms = new ArrayList<>();
		mF3UtilityRooms = new ArrayList<>();

		//Load room details
		//F1 utility rooms
		DepthsRoom f1r9 = new DepthsRoom("\"depths/f1r9\"", DepthsRoomType.UTILITY, new Vector(38, 26, 38), new Location(world, 1.0, -11.0, -17.0), 0, RoomDirection.EVEN);
		DepthsRoom f1r19 = new DepthsRoom("\"depths/f1r19\"", DepthsRoomType.UTILITY, new Vector(35, 18, 27), new Location(world, 1.0, -4.0, -7.0), 0, RoomDirection.EVEN);
		DepthsRoom f1r20 = new DepthsRoom("\"depths/f1r20\"", DepthsRoomType.UTILITY, new Vector(34, 26, 33), new Location(world, 1.0, -6.0, -16.0), 0, RoomDirection.EVEN);
		DepthsRoom f1r22 = new DepthsRoom("\"depths/f1r22\"", DepthsRoomType.UTILITY, new Vector(32, 23, 31), new Location(world, 1.0, -6.0, -15.0), 0, RoomDirection.EVEN);
		DepthsRoom f1r28 = new DepthsRoom("\"depths/f1r28\"", DepthsRoomType.UTILITY, new Vector(36, 24, 31), new Location(world, 1.0, -3.0, -15.0), 0, RoomDirection.EVEN);

		mF1UtilityRooms.add(f1r9);
		mF1UtilityRooms.add(f1r19);
		mF1UtilityRooms.add(f1r20);
		mF1UtilityRooms.add(f1r22);
		mF1UtilityRooms.add(f1r28);

		//F1 normal rooms
		DepthsRoom f1r1 = new DepthsRoom("\"depths/f1r1\"", DepthsRoomType.ABILITY, new Vector(32, 33, 34), new Location(world, 1.0, -5.0, -18.0), 12, RoomDirection.UP);
		DepthsRoom f1r2 = new DepthsRoom("\"depths/f1r2\"", DepthsRoomType.ABILITY, new Vector(29, 40, 30), new Location(world, 1.0, -27.0, -23.0), 14, RoomDirection.DOWN);
		DepthsRoom f1r3 = new DepthsRoom("\"depths/f1r3\"", DepthsRoomType.ABILITY, new Vector(39, 20, 31), new Location(world, 1.0, -1.0, -8.0), 8, RoomDirection.UP);
		DepthsRoom f1r4 = new DepthsRoom("\"depths/f1r4\"", DepthsRoomType.ABILITY, new Vector(40, 39, 39), new Location(world, 1.0, -1.0, -22.0), 13, RoomDirection.UP);
		DepthsRoom f1r5 = new DepthsRoom("\"depths/f1r5\"", DepthsRoomType.ABILITY, new Vector(39, 28, 37), new Location(world, 1.0, -3.0, -10.0), 11, RoomDirection.UP);
		DepthsRoom f1r6 = new DepthsRoom("\"depths/f1r6\"", DepthsRoomType.ABILITY, new Vector(35, 34, 35), new Location(world, 1.0, -1.0, -29.0), 8, RoomDirection.UP);
		DepthsRoom f1r7 = new DepthsRoom("\"depths/f1r7\"", DepthsRoomType.ABILITY, new Vector(38, 28, 40), new Location(world, 1.0, -4.0, -21.0), 11, RoomDirection.EVEN);
		DepthsRoom f1r8 = new DepthsRoom("\"depths/f1r8\"", DepthsRoomType.ABILITY, new Vector(38, 26, 38), new Location(world, 1.0, -8.0, -17.0), 9, RoomDirection.DOWN);
		DepthsRoom f1r10 = new DepthsRoom("\"depths/f1r10\"", DepthsRoomType.ABILITY, new Vector(37, 35, 38), new Location(world, 1.0, -18.0, -16.0), 8, RoomDirection.EVEN);
		DepthsRoom f1r11 = new DepthsRoom("\"depths/f1r11\"", DepthsRoomType.ABILITY, new Vector(34, 30, 37), new Location(world, 1.0, -2.0, -5.0), 15, RoomDirection.EVEN);
		DepthsRoom f1r23 = new DepthsRoom("\"depths/f1r23\"", DepthsRoomType.ABILITY, new Vector(31, 24, 30), new Location(world, 1.0, -8.0, -12.0), 8, RoomDirection.EVEN);
		DepthsRoom f1r25 = new DepthsRoom("\"depths/f1r25\"", DepthsRoomType.ABILITY, new Vector(28, 40, 31), new Location(world, 1.0, -22.0, -7.0), 8, RoomDirection.DOWN);
		DepthsRoom f1r29 = new DepthsRoom("\"depths/f1r29\"", DepthsRoomType.ABILITY, new Vector(44, 22, 23), new Location(world, 1.0, -10.0, -22.0), 12, RoomDirection.EVEN);
		DepthsRoom f1r30 = new DepthsRoom("\"depths/f1r30\"", DepthsRoomType.ABILITY, new Vector(33, 32, 29), new Location(world, 1.0, -21.0, -5.0), 7, RoomDirection.DOWN);
		DepthsRoom f1r33 = new DepthsRoom("\"depths/f1r33\"", DepthsRoomType.ABILITY, new Vector(33, 37, 39), new Location(world, 1.0, -28.0, -25.0), 8, RoomDirection.DOWN);
		DepthsRoom f1r35 = new DepthsRoom("\"depths/f1r35\"", DepthsRoomType.ABILITY, new Vector(51, 44, 44), new Location(world, 1.0, -30.0, -20.0), 12, RoomDirection.DOWN);


		mF1NormalRooms.add(f1r1);
		mF1NormalRooms.add(f1r2);
		mF1NormalRooms.add(f1r3);
		mF1NormalRooms.add(f1r4);
		mF1NormalRooms.add(f1r5);
		mF1NormalRooms.add(f1r6);
		mF1NormalRooms.add(f1r7);
		mF1NormalRooms.add(f1r8);
		mF1NormalRooms.add(f1r10);
		mF1NormalRooms.add(f1r11);
		mF1NormalRooms.add(f1r23);
		mF1NormalRooms.add(f1r25);
		mF1NormalRooms.add(f1r29);
		mF1NormalRooms.add(f1r30);
		mF1NormalRooms.add(f1r33);
		mF1NormalRooms.add(f1r35);


		//F1 elite rooms
		DepthsRoom f1r12 = new DepthsRoom("\"depths/f1r12\"", DepthsRoomType.ABILITY, new Vector(40, 41, 40), new Location(world, 1.0, -2.0, -21.0), 23, RoomDirection.UP);
		DepthsRoom f1r13 = new DepthsRoom("\"depths/f1r13\"", DepthsRoomType.ABILITY, new Vector(47, 54, 55), new Location(world, 1.0, -5.0, -24.0), 12, RoomDirection.EVEN);
		DepthsRoom f1r14 = new DepthsRoom("\"depths/f1r14\"", DepthsRoomType.ABILITY, new Vector(49, 54, 69), new Location(world, 1.0, -29.0, -33.0), 34, RoomDirection.DOWN);
		DepthsRoom f1r15 = new DepthsRoom("\"depths/f1r15\"", DepthsRoomType.ABILITY, new Vector(51, 43, 54), new Location(world, 1.0, -20.0, -11.0), 45, RoomDirection.DOWN);
		DepthsRoom f1r17 = new DepthsRoom("\"depths/f1r17\"", DepthsRoomType.ABILITY, new Vector(48, 53, 36), new Location(world, 1.0, -9.0, -25.0), 17, RoomDirection.UP);
		DepthsRoom f1r21 = new DepthsRoom("\"depths/f1r21\"", DepthsRoomType.ABILITY, new Vector(41, 56, 40), new Location(world, 1.0, -2.0, -19.0), 17, RoomDirection.UP);
		DepthsRoom f1r24 = new DepthsRoom("\"depths/f1r24\"", DepthsRoomType.ABILITY, new Vector(51, 60, 70), new Location(world, 1.0, -43.0, -32.0), 39, RoomDirection.DOWN);
		DepthsRoom f1r26 = new DepthsRoom("\"depths/f1r26\"", DepthsRoomType.ABILITY, new Vector(58, 39, 49), new Location(world, 1.0, -2.0, -26.0), 19, RoomDirection.UP);
		DepthsRoom f1r27 = new DepthsRoom("\"depths/f1r27\"", DepthsRoomType.ABILITY, new Vector(63, 38, 63), new Location(world, 1.0, -11.0, -32.0), 32, RoomDirection.DOWN);
		DepthsRoom f1r31 = new DepthsRoom("\"depths/f1r31\"", DepthsRoomType.ABILITY, new Vector(51, 47, 56), new Location(world, 1.0, -10.0, -42.0), 16, RoomDirection.UP);
		DepthsRoom f1r32 = new DepthsRoom("\"depths/f1r32\"", DepthsRoomType.ABILITY, new Vector(36, 40, 30), new Location(world, 1.0, -28.0, -8.0), 12, RoomDirection.DOWN);
		DepthsRoom f1r34 = new DepthsRoom("\"depths/f1r34\"", DepthsRoomType.ABILITY, new Vector(51, 60, 70), new Location(world, 1.0, -15.0, -7.0), 21, RoomDirection.DOWN);
		DepthsRoom f1r36 = new DepthsRoom("\"depths/f1r36\"", DepthsRoomType.ABILITY, new Vector(72, 53, 65), new Location(world, 1.0, -8.0, -49.0), 14, RoomDirection.EVEN);

		mF1EliteRooms.add(f1r12);
		mF1EliteRooms.add(f1r13);
		mF1EliteRooms.add(f1r14);
		mF1EliteRooms.add(f1r15);
		mF1EliteRooms.add(f1r17);
		mF1EliteRooms.add(f1r21);
		mF1EliteRooms.add(f1r24);
		mF1EliteRooms.add(f1r26);
		mF1EliteRooms.add(f1r27);
		mF1EliteRooms.add(f1r31);
		mF1EliteRooms.add(f1r32);
		mF1EliteRooms.add(f1r34);
		mF1EliteRooms.add(f1r36);

		//Special f1 room
		DepthsRoom f1r16 = new DepthsRoom("\"depths/f1r16\"", DepthsRoomType.ABILITY, new Vector(43, 32, 46), new Location(world, 1.0, -3.0, -23.0), 0, RoomDirection.EVEN);
		mWeaponAspectRoom = f1r16;

		//Boss f1 room
		DepthsRoom f1Boss = new DepthsRoom("\"depths/f1r18\"", DepthsRoomType.BOSS, new Vector(51, 46, 56), new Location(world, 1.0, -16.0, -15.0), 0, RoomDirection.EVEN);
		mF1BossRoom = f1Boss;

		//F2 utility rooms
		DepthsRoom f2r11 = new DepthsRoom("\"depths/f2r11\"", DepthsRoomType.UTILITY, new Vector(33, 28, 29), new Location(world, 1.0, -1.0, -6.0), 0, RoomDirection.EVEN);
		DepthsRoom f2r12 = new DepthsRoom("\"depths/f2r12\"", DepthsRoomType.UTILITY, new Vector(35, 18, 27), new Location(world, 1.0, -4.0, -7.0), 0, RoomDirection.EVEN);
		DepthsRoom f2r14 = new DepthsRoom("\"depths/f2r14\"", DepthsRoomType.UTILITY, new Vector(38, 26, 38), new Location(world, 1.0, -11.0, -17.0), 0, RoomDirection.EVEN);
		DepthsRoom f2r16 = new DepthsRoom("\"depths/f2r16\"", DepthsRoomType.UTILITY, new Vector(34, 26, 33), new Location(world, 1.0, -6.0, -16.0), 0, RoomDirection.EVEN);
		DepthsRoom f2r17 = new DepthsRoom("\"depths/f2r17\"", DepthsRoomType.UTILITY, new Vector(32, 23, 31), new Location(world, 1.0, -6.0, -15.0), 0, RoomDirection.EVEN);

		mF2UtilityRooms.add(f2r11);
		mF2UtilityRooms.add(f2r12);
		mF2UtilityRooms.add(f2r14);
		mF2UtilityRooms.add(f2r16);
		mF2UtilityRooms.add(f2r17);

		//F2 normal rooms

		DepthsRoom f2r2 = new DepthsRoom("\"depths/f2r2\"", DepthsRoomType.ABILITY, new Vector(28, 38, 27), new Location(world, 1.0, -24.0, -13.0), 10, RoomDirection.DOWN);
		DepthsRoom f2r3 = new DepthsRoom("\"depths/f2r3\"", DepthsRoomType.ABILITY, new Vector(27, 28, 28), new Location(world, 1.0, -9.0, -7.0), 12, RoomDirection.UP);
		DepthsRoom f2r5 = new DepthsRoom("\"depths/f2r5\"", DepthsRoomType.ABILITY, new Vector(38, 26, 38), new Location(world, 1.0, -15.0, -23.0), 8, RoomDirection.DOWN);
		DepthsRoom f2r6 = new DepthsRoom("\"depths/f2r6\"", DepthsRoomType.ABILITY, new Vector(38, 26, 38), new Location(world, 1.0, -1.0, -9.0), 7, RoomDirection.UP);
		DepthsRoom f2r7 = new DepthsRoom("\"depths/f2r7\"", DepthsRoomType.ABILITY, new Vector(38, 26, 38), new Location(world, 1.0, -1.0, -9.0), 11, RoomDirection.UP);
		DepthsRoom f2r8 = new DepthsRoom("\"depths/f2r8\"", DepthsRoomType.ABILITY, new Vector(27, 29, 31), new Location(world, 1.0, -0.0, -9.0), 11, RoomDirection.UP);
		DepthsRoom f2r10 = new DepthsRoom("\"depths/f2r10\"", DepthsRoomType.ABILITY, new Vector(41, 26, 39), new Location(world, 1.0, -13.0, -11.0), 12, RoomDirection.DOWN);
		DepthsRoom f2r19 = new DepthsRoom("\"depths/f2r19\"", DepthsRoomType.ABILITY, new Vector(55, 35, 48), new Location(world, 1.0, -19.0, -18.0), 10, RoomDirection.EVEN);
		DepthsRoom f2r20 = new DepthsRoom("\"depths/f2r20\"", DepthsRoomType.ABILITY, new Vector(57, 37, 61), new Location(world, 1.0, -19.0, -33.0), 9, RoomDirection.DOWN);
		DepthsRoom f2r21 = new DepthsRoom("\"depths/f2r21\"", DepthsRoomType.ABILITY, new Vector(57, 37, 61), new Location(world, 1.0, -17.0, -26.0), 11, RoomDirection.EVEN);
		DepthsRoom f2r24 = new DepthsRoom("\"depths/f2r24\"", DepthsRoomType.ABILITY, new Vector(51, 33, 36), new Location(world, 1.0, -4.0, -12.0), 12, RoomDirection.UP);
		DepthsRoom f2r28 = new DepthsRoom("\"depths/f2r28\"", DepthsRoomType.ABILITY, new Vector(54, 46, 34), new Location(world, 1.0, -17.0, -16.0), 11, RoomDirection.UP);
		DepthsRoom f2r29 = new DepthsRoom("\"depths/f2r29\"", DepthsRoomType.ABILITY, new Vector(45, 46, 64), new Location(world, 1.0, -13.0, -30.0), 9, RoomDirection.UP);

		mF2NormalRooms.add(f2r2);
		mF2NormalRooms.add(f2r3);
		mF2NormalRooms.add(f2r5);
		mF2NormalRooms.add(f2r6);
		mF2NormalRooms.add(f2r7);
		mF2NormalRooms.add(f2r8);
		mF2NormalRooms.add(f2r10);
		mF2NormalRooms.add(f2r19);
		mF2NormalRooms.add(f2r20);
		mF2NormalRooms.add(f2r21);
		mF2NormalRooms.add(f2r24);
		mF2NormalRooms.add(f2r28);
		mF2NormalRooms.add(f2r29);

		//F2 elite rooms
		DepthsRoom f2r1 = new DepthsRoom("\"depths/f2r1\"", DepthsRoomType.ABILITY, new Vector(32, 33, 35), new Location(world, 1.0, -3.0, -18.0), 20, RoomDirection.UP);
		DepthsRoom f2r4 = new DepthsRoom("\"depths/f2r4\"", DepthsRoomType.ABILITY, new Vector(39, 44, 40), new Location(world, 1.0, -1.0, -19.0), 16, RoomDirection.UP);
		DepthsRoom f2r9 = new DepthsRoom("\"depths/f2r9\"", DepthsRoomType.ABILITY, new Vector(32, 35, 36), new Location(world, 1.0, -23.0, -11.0), 12, RoomDirection.DOWN);
		DepthsRoom f2r13 = new DepthsRoom("\"depths/f2r13\"", DepthsRoomType.ABILITY, new Vector(49, 58, 69), new Location(world, 1.0, -46.0, -12.0), 29, RoomDirection.DOWN);
		DepthsRoom f2r15 = new DepthsRoom("\"depths/f2r15\"", DepthsRoomType.ABILITY, new Vector(51, 46, 62), new Location(world, 1.0, -30.0, -11.0), 39, RoomDirection.DOWN);
		DepthsRoom f2r18 = new DepthsRoom("\"depths/f2r18\"", DepthsRoomType.ABILITY, new Vector(57, 37, 53), new Location(world, 1.0, -16.0, -25.0), 10, RoomDirection.EVEN);
		DepthsRoom f2r22 = new DepthsRoom("\"depths/f2r22\"", DepthsRoomType.ABILITY, new Vector(65, 40, 47), new Location(world, 1.0, -6.0, -32.0), 11, RoomDirection.UP);
		DepthsRoom f2r23 = new DepthsRoom("\"depths/f2r23\"", DepthsRoomType.ABILITY, new Vector(55, 67, 43), new Location(world, 1.0, -47.0, -24.0), 15, RoomDirection.DOWN);
		DepthsRoom f2r25 = new DepthsRoom("\"depths/f2r25\"", DepthsRoomType.ABILITY, new Vector(45, 39, 61), new Location(world, 1.0, -22.0, -13.0), 17, RoomDirection.DOWN);
		DepthsRoom f2r27 = new DepthsRoom("\"depths/f2r27\"", DepthsRoomType.ABILITY, new Vector(51, 38, 57), new Location(world, 1.0, -7.0, -45.0), 20, RoomDirection.EVEN);
		DepthsRoom f2r30 = new DepthsRoom("\"depths/f2r30\"", DepthsRoomType.ABILITY, new Vector(51, 60, 70), new Location(world, 1.0, -18.0, -13.0), 18, RoomDirection.UP);

		mF2EliteRooms.add(f2r1);
		mF2EliteRooms.add(f2r4);
		mF2EliteRooms.add(f2r9);
		mF2EliteRooms.add(f2r13);
		mF2EliteRooms.add(f2r15);
		mF2EliteRooms.add(f2r18);
		mF2EliteRooms.add(f2r22);
		mF2EliteRooms.add(f2r23);
		mF2EliteRooms.add(f2r25);
		mF2EliteRooms.add(f2r27);
		mF2EliteRooms.add(f2r30);

		//F2 boss room
		DepthsRoom f2r26 = new DepthsRoom("\"depths/f2r26\"", DepthsRoomType.BOSS, new Vector(73, 48, 56), new Location(world, 1.0, -11.0, -35.0), 0, RoomDirection.EVEN);
		mF2BossRoom = f2r26;

		//F3 normal rooms
		DepthsRoom f3r1 = new DepthsRoom("\"depths/f3r1\"", DepthsRoomType.ABILITY, new Vector(39, 31, 35), new Location(world, 1.0, -0.0, -17.0), 9, RoomDirection.EVEN);
		DepthsRoom f3r2 = new DepthsRoom("\"depths/f3r2\"", DepthsRoomType.ABILITY, new Vector(40, 24, 25), new Location(world, 1.0, -0.0, -17.0), 11, RoomDirection.UP);
		DepthsRoom f3r4 = new DepthsRoom("\"depths/f3r4\"", DepthsRoomType.ABILITY, new Vector(36, 26, 36), new Location(world, 1.0, -5.0, -7.0), 8, RoomDirection.UP);
		DepthsRoom f3r6 = new DepthsRoom("\"depths/f3r6\"", DepthsRoomType.ABILITY, new Vector(22, 37, 35), new Location(world, 1.0, -26.0, -7.0), 14, RoomDirection.DOWN);
		DepthsRoom f3r8 = new DepthsRoom("\"depths/f3r8\"", DepthsRoomType.ABILITY, new Vector(53, 35, 60), new Location(world, 1.0, -10.0, -8.0), 14, RoomDirection.EVEN);
		DepthsRoom f3r9 = new DepthsRoom("\"depths/f3r9\"", DepthsRoomType.ABILITY, new Vector(50, 46, 66), new Location(world, 1.0, -17.0, -57.0), 15, RoomDirection.EVEN);
		DepthsRoom f3r10 = new DepthsRoom("\"depths/f3r10\"", DepthsRoomType.ABILITY, new Vector(47, 32, 46), new Location(world, 1.0, -17.0, -24.0), 15, RoomDirection.DOWN);
		DepthsRoom f3r13 = new DepthsRoom("\"depths/f3r13\"", DepthsRoomType.ABILITY, new Vector(52, 27, 44), new Location(world, 1.0, -4.0, -16.0), 17, RoomDirection.EVEN);
		DepthsRoom f3r15 = new DepthsRoom("\"depths/f3r15\"", DepthsRoomType.ABILITY, new Vector(43, 41, 39), new Location(world, 1.0, -13.0, -19.0), 12, RoomDirection.UP);
		DepthsRoom f3r26 = new DepthsRoom("\"depths/f3r26\"", DepthsRoomType.ABILITY, new Vector(51, 25, 25), new Location(world, 1.0, -1.0, -12.0), 8, RoomDirection.UP);
		DepthsRoom f3r27 = new DepthsRoom("\"depths/f3r27\"", DepthsRoomType.ABILITY, new Vector(52, 34, 51), new Location(world, 1.0, -5.0, -7.0), 8, RoomDirection.UP);
		DepthsRoom f3r30 = new DepthsRoom("\"depths/f3r30\"", DepthsRoomType.ABILITY, new Vector(40, 37, 37), new Location(world, 1.0, -8.0, -12.0), 9, RoomDirection.EVEN);
		DepthsRoom f3r33 = new DepthsRoom("\"depths/f3r33\"", DepthsRoomType.ABILITY, new Vector(39, 50, 38), new Location(world, 1.0, -39.0, -13.0), 15, RoomDirection.DOWN);
		DepthsRoom f3r34 = new DepthsRoom("\"depths/f3r34\"", DepthsRoomType.ABILITY, new Vector(44, 25, 36), new Location(world, 1.0, -14.0, -27.0), 12, RoomDirection.DOWN);

		mF3NormalRooms.add(f3r1);
		mF3NormalRooms.add(f3r2);
		mF3NormalRooms.add(f3r4);
		mF3NormalRooms.add(f3r6);
		mF3NormalRooms.add(f3r8);
		mF3NormalRooms.add(f3r9);
		mF3NormalRooms.add(f3r10);
		mF3NormalRooms.add(f3r13);
		mF3NormalRooms.add(f3r15);
		mF3NormalRooms.add(f3r26);
		mF3NormalRooms.add(f3r27);
		mF3NormalRooms.add(f3r30);
		mF3NormalRooms.add(f3r33);
		mF3NormalRooms.add(f3r34);

		//F3 elite rooms
		DepthsRoom f3r5 = new DepthsRoom("\"depths/f3r5\"", DepthsRoomType.ABILITY, new Vector(39, 40, 40), new Location(world, 1.0, -5.0, -19.0), 28, RoomDirection.UP);
		DepthsRoom f3r7 = new DepthsRoom("\"depths/f3r7\"", DepthsRoomType.ABILITY, new Vector(48, 42, 58), new Location(world, 1.0, -9.0, -44.0), 15, RoomDirection.UP);
		DepthsRoom f3r12 = new DepthsRoom("\"depths/f3r12\"", DepthsRoomType.ABILITY, new Vector(51, 47, 53), new Location(world, 1.0, -34.0, -45.0), 17, RoomDirection.DOWN);
		DepthsRoom f3r14 = new DepthsRoom("\"depths/f3r14\"", DepthsRoomType.ABILITY, new Vector(51, 60, 41), new Location(world, 1.0, -5.0, -24.0), 15, RoomDirection.UP);
		DepthsRoom f3r20 = new DepthsRoom("\"depths/f3r20\"", DepthsRoomType.ABILITY, new Vector(43, 57, 54), new Location(world, 1.0, -38.0, -24.0), 14, RoomDirection.EVEN);
		DepthsRoom f3r21 = new DepthsRoom("\"depths/f3r21\"", DepthsRoomType.ABILITY, new Vector(66, 71, 67), new Location(world, 1.0, -34.0, -12.0), 23, RoomDirection.UP);
		DepthsRoom f3r22 = new DepthsRoom("\"depths/f3r22\"", DepthsRoomType.ABILITY, new Vector(51, 43, 65), new Location(world, 1.0, -10.0, -8.0), 28, RoomDirection.UP);
		DepthsRoom f3r23 = new DepthsRoom("\"depths/f3r23\"", DepthsRoomType.ABILITY, new Vector(64, 48, 51), new Location(world, 1.0, -9.0, -14.0), 22, RoomDirection.DOWN);
		DepthsRoom f3r24 = new DepthsRoom("\"depths/f3r24\"", DepthsRoomType.ABILITY, new Vector(52, 36, 55), new Location(world, 1.0, -22.0, -30.0), 22, RoomDirection.DOWN);
		DepthsRoom f3r28 = new DepthsRoom("\"depths/f3r28\"", DepthsRoomType.ABILITY, new Vector(45, 44, 37), new Location(world, 1.0, -24.0, -18.0), 15, RoomDirection.DOWN);
		DepthsRoom f3r29 = new DepthsRoom("\"depths/f3r29\"", DepthsRoomType.ABILITY, new Vector(61, 53, 60), new Location(world, 1.0, -8.0, -46.0), 23, RoomDirection.EVEN);
		DepthsRoom f3r31 = new DepthsRoom("\"depths/f3r31\"", DepthsRoomType.ABILITY, new Vector(53, 39, 45), new Location(world, 1.0, -12.0, -36.0), 18, RoomDirection.UP);
		DepthsRoom f3r32 = new DepthsRoom("\"depths/f3r32\"", DepthsRoomType.ABILITY, new Vector(52, 51, 61), new Location(world, 1.0, -8.0, -37.0), 21, RoomDirection.UP);

		mF3EliteRooms.add(f3r5);
		mF3EliteRooms.add(f3r7);
		mF3EliteRooms.add(f3r12);
		mF3EliteRooms.add(f3r14);
		mF3EliteRooms.add(f3r20);
		mF3EliteRooms.add(f3r21);
		mF3EliteRooms.add(f3r22);
		mF3EliteRooms.add(f3r23);
		mF3EliteRooms.add(f3r24);
		mF3EliteRooms.add(f3r28);
		mF3EliteRooms.add(f3r29);
		mF3EliteRooms.add(f3r31);
		mF3EliteRooms.add(f3r32);

		//F3 utility rooms
		DepthsRoom f3r11 = new DepthsRoom("\"depths/f3r11\"", DepthsRoomType.UTILITY, new Vector(34, 26, 33), new Location(world, 1.0, -6.0, -16.0), 0, RoomDirection.EVEN);
		DepthsRoom f3r16 = new DepthsRoom("\"depths/f3r16\"", DepthsRoomType.UTILITY, new Vector(38, 26, 38), new Location(world, 1.0, -11.0, -17.0), 0, RoomDirection.EVEN);
		DepthsRoom f3r17 = new DepthsRoom("\"depths/f3r17\"", DepthsRoomType.UTILITY, new Vector(32, 23, 31), new Location(world, 1.0, -6.0, -15.0), 0, RoomDirection.EVEN);
		DepthsRoom f3r18 = new DepthsRoom("\"depths/f3r18\"", DepthsRoomType.UTILITY, new Vector(35, 18, 27), new Location(world, 1.0, -4.0, -7.0), 0, RoomDirection.EVEN);
		DepthsRoom f3r19 = new DepthsRoom("\"depths/f3r19\"", DepthsRoomType.UTILITY, new Vector(31, 22, 36), new Location(world, 1.0, -3.0, -29.0), 0, RoomDirection.EVEN);

		mF3UtilityRooms.add(f3r11);
		mF3UtilityRooms.add(f3r16);
		mF3UtilityRooms.add(f3r17);
		mF3UtilityRooms.add(f3r18);
		mF3UtilityRooms.add(f3r19);

		//Boss f3 room
		DepthsRoom f3Boss = new DepthsRoom("\"depths/f3r25\"", DepthsRoomType.BOSS, new Vector(63, 58, 64), new Location(world, 1.0, -1.0, -32.0), 0, RoomDirection.EVEN);
		mF3BossRoom = f3Boss;


	}

	/**
	 * Spawns the next room in the physical world
	 * @param spawnPoint the coordinates to load the room
	 * @param roomType the type of the room to select from
	 * @return the room information for the selected room
	 */
	public DepthsRoom summonRoom(Location spawnPoint, DepthsRoomType roomType, DepthsParty party) {
		//Get a valid room from the options available to the party
		DepthsRoom room = getValidRoom(roomType, party, spawnPoint.getY());
		room.mRoomType = roomType;
		//Gets the point in the world to load it and physically summons it
		Location spawn = spawnPoint.clone().add(room.mEntry);

		//Get box of room and entities to overwrite and remove
		World w = spawnPoint.getWorld();
		BoundingBox box = w.getBlockAt(spawn).getBoundingBox();
		box.expand(BlockFace.EAST, room.mSize.getX());
		box.expand(BlockFace.UP, room.mSize.getY());
		box.expand(BlockFace.DOWN, room.mSize.getZ());
		for (Entity e: w.getNearbyEntities(box)) {
			if (!(e instanceof Player)) {
				e.remove();
			}
		}

		Plugin.getInstance().getLogger().info("Summoning structure " + room.mLoadPath);
		Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "loadstructure " + room.mLoadPath + " " + spawn.getX() + " " + spawn.getY() + " " + spawn.getZ() + " true");
		return room;
	}

	/**
	 * This method figures out which room to give the players depending on their type selection and some random chance.
	 * @param roomType type of room to summon
	 * @param party depths party to check info for
	 * @param yLevel what level the spawn point is currently at, to avoid going into the void or sky
	 * @return a valid room object to summon
	 */
	public DepthsRoom getValidRoom(DepthsRoomType roomType, DepthsParty party, double yLevel) {
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
			}
		} else if (floor % 3 == 2) {
			if (roomType == DepthsRoomType.ABILITY || roomType == DepthsRoomType.TREASURE || roomType == DepthsRoomType.UPGRADE) {
				return randomRoom(party, mF2NormalRooms, yLevel);
			} else if (roomType == DepthsRoomType.ABILITY_ELITE || roomType == DepthsRoomType.TREASURE_ELITE || roomType == DepthsRoomType.UPGRADE_ELITE) {
				return randomRoom(party, mF2EliteRooms, yLevel);
			} else if (roomType == DepthsRoomType.UTILITY) {
				return randomRoom(party, mF2UtilityRooms, yLevel);
			}
		} else if (floor % 3 == 0) {
			if (roomType == DepthsRoomType.ABILITY || roomType == DepthsRoomType.TREASURE || roomType == DepthsRoomType.UPGRADE) {
				return randomRoom(party, mF3NormalRooms, yLevel);
			} else if (roomType == DepthsRoomType.ABILITY_ELITE || roomType == DepthsRoomType.TREASURE_ELITE || roomType == DepthsRoomType.UPGRADE_ELITE) {
				return randomRoom(party, mF3EliteRooms, yLevel);
			} else if (roomType == DepthsRoomType.UTILITY) {
				return randomRoom(party, mF3UtilityRooms, yLevel);
			}
		}
		//No valid room found, big oof
		return null;
	}

	//This method actually does the randomization aspect after finding the right array to roll from
	public DepthsRoom randomRoom(DepthsParty party, ArrayList<DepthsRoom> roomList, double yLevel) {
		List<DepthsRoom> oldRooms = party.mOldRooms;
		//Copy the list and shuffle it
		List<DepthsRoom> roomOptions = new ArrayList<>(roomList);
		Collections.shuffle(roomOptions);
		//Get the first room in the list the party hasn't gotten already
		for (DepthsRoom room : roomOptions) {
			//Account for height
			if ((yLevel < 70.0 && room.mDirection == RoomDirection.DOWN) || (yLevel > 186.0 && room.mDirection == RoomDirection.UP)) {
				continue;
			}
			if (!oldRooms.contains(room)) {
				return room;
			}
		}

		//No valid non-repeat rooms left, just return the next room that won't take them into void

		for (DepthsRoom room : roomOptions) {
			//Account for height
			if ((yLevel < 70.0 && room.mDirection == RoomDirection.DOWN) || (yLevel > 186.0 && room.mDirection == RoomDirection.UP)) {
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
	 */
	public void goToNextFloor(DepthsParty party, int treasure) {

		//Load the room in the world
		World world = Bukkit.getPlayer(party.mPlayersInParty.get(0).mPlayerId).getWorld();
		Location loc = new Location(world, party.mFloorLobbyLoadPoint.getX(), party.mFloorLobbyLoadPoint.getY(), party.mFloorLobbyLoadPoint.getZ());

		//Separate rooms by floor here
		int nextFloorNum = party.getFloor() + 1;
		if (nextFloorNum > CUSTOM_FLOOR_LOBBIES) {
			Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "loadstructure \"depths/f11lobby\" " + loc.getX() + " " + loc.getY() + " " + loc.getZ() + " false");
		} else {
			Bukkit.getServer().dispatchCommand(Bukkit.getServer().getConsoleSender(), "loadstructure \"depths/f" + nextFloorNum + "lobby\" " + loc.getX() + " " + loc.getY() + " " + loc.getZ() + " false");
		}

		//Tp all the players to it
		for (DepthsPlayer dp : party.mPlayersInParty) {

			Player p = Bukkit.getPlayer(dp.mPlayerId);
			if (p != null) {
				Location l = new Location(world, party.mFloorLobbyLoadPlayerTpPoint.getX(), party.mFloorLobbyLoadPlayerTpPoint.getY(), party.mFloorLobbyLoadPlayerTpPoint.getZ());
				l.setYaw(270f);
				p.teleport(l);
				p.sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + "Your party earned " + treasure + " treasure score for clearing floor " + party.getFloor() + "! Sending your party to next floor.");
			}
		}
		//Reset used rooms
		party.mOldRooms.clear();
		//Just in case they get stuck, set the spawner break trigger to zero
		party.mSpawnersToBreak = 0;
	}
}
