package com.playmonumenta.plugins.depths;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsRoom.RoomDirection;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.structures.StructuresAPI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

/**
 * This class contains hard coded information about all the rooms possible in the system, including type, spawner count, load paths, etc.
 * It also is responsible for picking random rooms for the party and dynamically structure loading them into the world.
 *
 * @author ShadowVisions
 */
public class DepthsRoomRepository {

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
	@Nullable DepthsRoom mWeaponAspectRoom;

	public DepthsRoomRepository() {
		initRooms();
	}

	/**
	 * Rooms are stored in the depths system here
	 * Rooms have an entry point that describes the offset from the door to the structure corner of the room
	 * They also have a spawner count to track player progress and a vector to represent the size.
	 */
	private void initRooms() {
		//Load room details
		//F1 utility rooms
		mF1UtilityRooms.add(new DepthsRoom("depths/f1r9", DepthsRoomType.UTILITY, new Vector(38, 26, 38), new Vector(1.0, -11.0, -17.0), 0, RoomDirection.EVEN));
		mF1UtilityRooms.add(new DepthsRoom("depths/f1r19", DepthsRoomType.UTILITY, new Vector(35, 18, 27), new Vector(1.0, -4.0, -7.0), 0, RoomDirection.EVEN));
		mF1UtilityRooms.add(new DepthsRoom("depths/f1r20", DepthsRoomType.UTILITY, new Vector(34, 26, 33), new Vector(1.0, -6.0, -16.0), 0, RoomDirection.EVEN));
		mF1UtilityRooms.add(new DepthsRoom("depths/f1r22", DepthsRoomType.UTILITY, new Vector(32, 23, 31), new Vector(1.0, -6.0, -15.0), 0, RoomDirection.EVEN));
		mF1UtilityRooms.add(new DepthsRoom("depths/f1r28", DepthsRoomType.UTILITY, new Vector(36, 24, 31), new Vector(1.0, -3.0, -15.0), 0, RoomDirection.EVEN));

		//F1 normal rooms
		mF1NormalRooms.add(new DepthsRoom("depths/f1r1", DepthsRoomType.ABILITY, new Vector(32, 33, 34), new Vector(1.0, -5.0, -18.0), 12, RoomDirection.UP));
		mF1NormalRooms.add(new DepthsRoom("depths/f1r2", DepthsRoomType.ABILITY, new Vector(29, 40, 30), new Vector(1.0, -27.0, -23.0), 14, RoomDirection.DOWN));
		mF1NormalRooms.add(new DepthsRoom("depths/f1r3", DepthsRoomType.ABILITY, new Vector(39, 20, 31), new Vector(1.0, -1.0, -8.0), 8, RoomDirection.UP));
		mF1NormalRooms.add(new DepthsRoom("depths/f1r4", DepthsRoomType.ABILITY, new Vector(40, 39, 39), new Vector(1.0, -1.0, -22.0), 13, RoomDirection.UP));
		mF1NormalRooms.add(new DepthsRoom("depths/f1r5", DepthsRoomType.ABILITY, new Vector(39, 28, 37), new Vector(1.0, -3.0, -10.0), 11, RoomDirection.UP));
		mF1NormalRooms.add(new DepthsRoom("depths/f1r6", DepthsRoomType.ABILITY, new Vector(35, 34, 35), new Vector(1.0, -1.0, -29.0), 8, RoomDirection.UP));
		mF1NormalRooms.add(new DepthsRoom("depths/f1r7", DepthsRoomType.ABILITY, new Vector(38, 28, 40), new Vector(1.0, -4.0, -21.0), 11, RoomDirection.EVEN));
		mF1NormalRooms.add(new DepthsRoom("depths/f1r8", DepthsRoomType.ABILITY, new Vector(38, 26, 38), new Vector(1.0, -8.0, -17.0), 9, RoomDirection.DOWN));
		mF1NormalRooms.add(new DepthsRoom("depths/f1r10", DepthsRoomType.ABILITY, new Vector(37, 35, 38), new Vector(1.0, -18.0, -16.0), 8, RoomDirection.EVEN));
		mF1NormalRooms.add(new DepthsRoom("depths/f1r11", DepthsRoomType.ABILITY, new Vector(34, 30, 37), new Vector(1.0, -2.0, -5.0), 15, RoomDirection.EVEN));
		mF1NormalRooms.add(new DepthsRoom("depths/f1r23", DepthsRoomType.ABILITY, new Vector(31, 24, 30), new Vector(1.0, -8.0, -12.0), 8, RoomDirection.EVEN));
		mF1NormalRooms.add(new DepthsRoom("depths/f1r25", DepthsRoomType.ABILITY, new Vector(28, 40, 31), new Vector(1.0, -22.0, -7.0), 8, RoomDirection.DOWN));
		mF1NormalRooms.add(new DepthsRoom("depths/f1r29", DepthsRoomType.ABILITY, new Vector(44, 22, 23), new Vector(1.0, -10.0, -22.0), 12, RoomDirection.EVEN));
		mF1NormalRooms.add(new DepthsRoom("depths/f1r30", DepthsRoomType.ABILITY, new Vector(33, 32, 29), new Vector(1.0, -21.0, -5.0), 7, RoomDirection.DOWN));
		mF1NormalRooms.add(new DepthsRoom("depths/f1r32", DepthsRoomType.ABILITY, new Vector(36, 40, 30), new Vector(1.0, -28.0, -8.0), 12, RoomDirection.DOWN));
		mF1NormalRooms.add(new DepthsRoom("depths/f1r33", DepthsRoomType.ABILITY, new Vector(33, 37, 39), new Vector(1.0, -28.0, -25.0), 8, RoomDirection.DOWN));
		mF1NormalRooms.add(new DepthsRoom("depths/f1r35", DepthsRoomType.ABILITY, new Vector(51, 44, 44), new Vector(1.0, -30.0, -20.0), 12, RoomDirection.DOWN));
		mF1NormalRooms.add(new DepthsRoom("depths/f1r42", DepthsRoomType.ABILITY, new Vector(31, 58, 32), new Vector(1.0, -3.0, -16.0), 11, RoomDirection.UP));
		mF1NormalRooms.add(new DepthsRoom("depths/f1r45", DepthsRoomType.ABILITY, new Vector(49, 37, 52), new Vector(1.0, -6.0, -29.0), 11, RoomDirection.UP));
		mF1NormalRooms.add(new DepthsRoom("depths/f1r46", DepthsRoomType.ABILITY, new Vector(51, 56, 32), new Vector(1.0, -9.0, -14.0), 15, RoomDirection.UP));
		mF1NormalRooms.add(new DepthsRoom("depths/f1r47", DepthsRoomType.ABILITY, new Vector(45, 52, 55), new Vector(1.0, -33.0, -27.0), 11, RoomDirection.DOWN));
		mF1NormalRooms.add(new DepthsRoom("depths/f1r48", DepthsRoomType.ABILITY, new Vector(32, 35, 35), new Vector(1.0, -8.0, -20.0), 12, RoomDirection.UP));
		mF1NormalRooms.add(new DepthsRoom("depths/f1r49", DepthsRoomType.ABILITY, new Vector(38, 31, 37), new Vector(1.0, -11.0, -31.0), 12, RoomDirection.EVEN));
		mF1NormalRooms.add(new DepthsRoom("depths/f1r55", DepthsRoomType.ABILITY, new Vector(33, 39, 33), new Vector(1.0, -1.0, -7.0), 13, RoomDirection.UP));
		mF1NormalRooms.add(new DepthsRoom("depths/f1r56", DepthsRoomType.ABILITY, new Vector(51, 43, 45), new Vector(1.0, -17.0, -23.0), 11, RoomDirection.EVEN));

		//F1 elite rooms
		mF1EliteRooms.add(new DepthsRoom("depths/f1r12", DepthsRoomType.ABILITY, new Vector(40, 41, 40), new Vector(1.0, -2.0, -21.0), 23, RoomDirection.UP));
		mF1EliteRooms.add(new DepthsRoom("depths/f1r13", DepthsRoomType.ABILITY, new Vector(47, 54, 55), new Vector(1.0, -5.0, -24.0), 12, RoomDirection.EVEN));
		mF1EliteRooms.add(new DepthsRoom("depths/f1r14", DepthsRoomType.ABILITY, new Vector(49, 54, 69), new Vector(1.0, -29.0, -33.0), 34, RoomDirection.DOWN));
		mF1EliteRooms.add(new DepthsRoom("depths/f1r15", DepthsRoomType.ABILITY, new Vector(51, 43, 54), new Vector(1.0, -20.0, -11.0), 45, RoomDirection.DOWN));
		mF1EliteRooms.add(new DepthsRoom("depths/f1r17", DepthsRoomType.ABILITY, new Vector(48, 53, 36), new Vector(1.0, -9.0, -25.0), 17, RoomDirection.UP));
		mF1EliteRooms.add(new DepthsRoom("depths/f1r21", DepthsRoomType.ABILITY, new Vector(41, 56, 40), new Vector(1.0, -2.0, -19.0), 17, RoomDirection.UP));
		mF1EliteRooms.add(new DepthsRoom("depths/f1r24", DepthsRoomType.ABILITY, new Vector(51, 60, 70), new Vector(1.0, -43.0, -32.0), 39, RoomDirection.DOWN));
		mF1EliteRooms.add(new DepthsRoom("depths/f1r26", DepthsRoomType.ABILITY, new Vector(58, 39, 49), new Vector(1.0, -2.0, -26.0), 19, RoomDirection.UP));
		mF1EliteRooms.add(new DepthsRoom("depths/f1r27", DepthsRoomType.ABILITY, new Vector(63, 38, 63), new Vector(1.0, -11.0, -32.0), 32, RoomDirection.DOWN));
		mF1EliteRooms.add(new DepthsRoom("depths/f1r31", DepthsRoomType.ABILITY, new Vector(51, 47, 56), new Vector(1.0, -10.0, -42.0), 16, RoomDirection.UP));
		mF1EliteRooms.add(new DepthsRoom("depths/f1r34", DepthsRoomType.ABILITY, new Vector(51, 60, 70), new Vector(1.0, -15.0, -7.0), 21, RoomDirection.DOWN));
		mF1EliteRooms.add(new DepthsRoom("depths/f1r36", DepthsRoomType.ABILITY, new Vector(72, 53, 65), new Vector(1.0, -8.0, -49.0), 14, RoomDirection.EVEN));
		mF1EliteRooms.add(new DepthsRoom("depths/f1r37", DepthsRoomType.ABILITY, new Vector(52, 61, 71), new Vector(1.0, 0.0, -7.0), 43, RoomDirection.UP));
		mF1EliteRooms.add(new DepthsRoom("depths/f1r38", DepthsRoomType.ABILITY, new Vector(60, 29, 36), new Vector(1.0, -8.0, -17.0), 17, RoomDirection.UP));
		mF1EliteRooms.add(new DepthsRoom("depths/f1r40", DepthsRoomType.ABILITY, new Vector(30, 22, 71), new Vector(1.0, -2.0, -7.0), 23, RoomDirection.EVEN));
		mF1EliteRooms.add(new DepthsRoom("depths/f1r43", DepthsRoomType.ABILITY, new Vector(44, 23, 40), new Vector(1.0, -1.0, -32.0), 19, RoomDirection.UP));
		mF1EliteRooms.add(new DepthsRoom("depths/f1r44", DepthsRoomType.ABILITY, new Vector(34, 40, 36), new Vector(1.0, -23.0, -29.0), 26, RoomDirection.DOWN));
		mF1EliteRooms.add(new DepthsRoom("depths/f1r50", DepthsRoomType.ABILITY, new Vector(35, 54, 33), new Vector(1.0, -39.0, -18.0), 20, RoomDirection.DOWN));
		mF1EliteRooms.add(new DepthsRoom("depths/f1r51", DepthsRoomType.ABILITY, new Vector(44, 16, 71), new Vector(1.0, 0.0, -7.0), 21, RoomDirection.UP));
		mF1EliteRooms.add(new DepthsRoom("depths/f1r52", DepthsRoomType.ABILITY, new Vector(41, 39, 49), new Vector(1.0, -16.0, -10.0), 36, RoomDirection.DOWN));
		mF1EliteRooms.add(new DepthsRoom("depths/f1r53", DepthsRoomType.ABILITY, new Vector(53, 46, 73), new Vector(1.0, -7.0, -48.0), 19, RoomDirection.EVEN));
		mF1EliteRooms.add(new DepthsRoom("depths/f1r54", DepthsRoomType.ABILITY, new Vector(42, 27, 41), new Vector(1.0, -8.0, -22.0), 20, RoomDirection.DOWN));
		mF1EliteRooms.add(new DepthsRoom("depths/f1r57", DepthsRoomType.ABILITY, new Vector(62, 46, 50), new Vector(1.0, -17.0, -25.0), 19, RoomDirection.DOWN));

		//F1 twisted rooms
		mF1TwistedRooms.add(new DepthsRoom("depths/f1r41", DepthsRoomType.TWISTED, new Vector(51, 60, 70), new Vector(1.0, -3.0, -61.0), 50, RoomDirection.EVEN));

		//Special f1 room
		mWeaponAspectRoom = new DepthsRoom("depths/f1r16", DepthsRoomType.ABILITY, new Vector(43, 32, 46), new Vector(1.0, -3.0, -23.0), 0, RoomDirection.EVEN);

		//Boss f1 room
		mF1BossRoom = new DepthsRoom("depths/f1r18", DepthsRoomType.BOSS, new Vector(51, 46, 56), new Vector(1.0, -16.0, -15.0), 0, RoomDirection.EVEN);

		//F2 utility rooms
		mF2UtilityRooms.add(new DepthsRoom("depths/f2r11", DepthsRoomType.UTILITY, new Vector(33, 28, 29), new Vector(1.0, -1.0, -6.0), 0, RoomDirection.EVEN));
		mF2UtilityRooms.add(new DepthsRoom("depths/f2r14", DepthsRoomType.UTILITY, new Vector(38, 26, 38), new Vector(1.0, -11.0, -17.0), 0, RoomDirection.EVEN));
		mF2UtilityRooms.add(new DepthsRoom("depths/f2r16", DepthsRoomType.UTILITY, new Vector(34, 26, 33), new Vector(1.0, -6.0, -16.0), 0, RoomDirection.EVEN));
		mF2UtilityRooms.add(new DepthsRoom("depths/f2r17", DepthsRoomType.UTILITY, new Vector(32, 23, 31), new Vector(1.0, -6.0, -15.0), 0, RoomDirection.EVEN));
		mF2UtilityRooms.add(new DepthsRoom("depths/f2r45", DepthsRoomType.UTILITY, new Vector(36, 24, 31), new Vector(1.0, -3.0, -15.0), 0, RoomDirection.EVEN));
		//OLD CASINO UTILITY ROOM- currently unused
		//mF2UtilityRooms.add(new DepthsRoom("depths/f2r12", DepthsRoomType.UTILITY, new Vector(35, 18, 27), new Vector(1.0, -4.0, -7.0), 0, RoomDirection.EVEN));

		//F2 normal rooms
		mF2NormalRooms.add(new DepthsRoom("depths/f2r2", DepthsRoomType.ABILITY, new Vector(28, 38, 27), new Vector(1.0, -24.0, -13.0), 10, RoomDirection.DOWN));
		mF2NormalRooms.add(new DepthsRoom("depths/f2r3", DepthsRoomType.ABILITY, new Vector(27, 28, 28), new Vector(1.0, -9.0, -7.0), 12, RoomDirection.UP));
		mF2NormalRooms.add(new DepthsRoom("depths/f2r5", DepthsRoomType.ABILITY, new Vector(38, 26, 38), new Vector(1.0, -15.0, -23.0), 8, RoomDirection.DOWN));
		mF2NormalRooms.add(new DepthsRoom("depths/f2r6", DepthsRoomType.ABILITY, new Vector(38, 26, 38), new Vector(1.0, -1.0, -9.0), 7, RoomDirection.UP));
		mF2NormalRooms.add(new DepthsRoom("depths/f2r7", DepthsRoomType.ABILITY, new Vector(38, 26, 38), new Vector(1.0, -1.0, -9.0), 11, RoomDirection.UP));
		mF2NormalRooms.add(new DepthsRoom("depths/f2r8", DepthsRoomType.ABILITY, new Vector(27, 29, 31), new Vector(1.0, -0.0, -9.0), 11, RoomDirection.UP));
		mF2NormalRooms.add(new DepthsRoom("depths/f2r9", DepthsRoomType.ABILITY, new Vector(32, 35, 36), new Vector(1.0, -23.0, -11.0), 12, RoomDirection.DOWN));
		mF2NormalRooms.add(new DepthsRoom("depths/f2r10", DepthsRoomType.ABILITY, new Vector(41, 26, 39), new Vector(1.0, -13.0, -11.0), 12, RoomDirection.DOWN));
		mF2NormalRooms.add(new DepthsRoom("depths/f2r19", DepthsRoomType.ABILITY, new Vector(55, 35, 48), new Vector(1.0, -19.0, -18.0), 10, RoomDirection.EVEN));
		mF2NormalRooms.add(new DepthsRoom("depths/f2r20", DepthsRoomType.ABILITY, new Vector(57, 37, 61), new Vector(1.0, -19.0, -33.0), 9, RoomDirection.DOWN));
		mF2NormalRooms.add(new DepthsRoom("depths/f2r21", DepthsRoomType.ABILITY, new Vector(57, 37, 61), new Vector(1.0, -17.0, -26.0), 11, RoomDirection.EVEN));
		mF2NormalRooms.add(new DepthsRoom("depths/f2r24", DepthsRoomType.ABILITY, new Vector(51, 33, 36), new Vector(1.0, -4.0, -12.0), 12, RoomDirection.UP));
		mF2NormalRooms.add(new DepthsRoom("depths/f2r28", DepthsRoomType.ABILITY, new Vector(54, 46, 34), new Vector(1.0, -17.0, -16.0), 11, RoomDirection.UP));
		mF2NormalRooms.add(new DepthsRoom("depths/f2r29", DepthsRoomType.ABILITY, new Vector(45, 46, 64), new Vector(1.0, -13.0, -30.0), 9, RoomDirection.UP));
		mF2NormalRooms.add(new DepthsRoom("depths/f2r31", DepthsRoomType.ABILITY, new Vector(34, 29, 34), new Vector(1.0, -5.0, -14.0), 8, RoomDirection.UP));
		mF2NormalRooms.add(new DepthsRoom("depths/f2r36", DepthsRoomType.ABILITY, new Vector(34, 31, 37), new Vector(1.0, -14.0, -30.0), 12, RoomDirection.DOWN));
		mF2NormalRooms.add(new DepthsRoom("depths/f2r37", DepthsRoomType.ABILITY, new Vector(34, 29, 31), new Vector(1.0, -15.0, -21.0), 14, RoomDirection.DOWN));
		mF2NormalRooms.add(new DepthsRoom("depths/f2r38", DepthsRoomType.ABILITY, new Vector(47, 54, 42), new Vector(1.0, -36.0, -21.0), 17, RoomDirection.DOWN));
		mF2NormalRooms.add(new DepthsRoom("depths/f2r40", DepthsRoomType.ABILITY, new Vector(49, 29, 35), new Vector(1.0, -10.0, -13.0), 11, RoomDirection.EVEN));
		mF2NormalRooms.add(new DepthsRoom("depths/f2r41", DepthsRoomType.ABILITY, new Vector(44, 44, 43), new Vector(1.0, -26.0, -18.0), 14, RoomDirection.DOWN));
		mF2NormalRooms.add(new DepthsRoom("depths/f2r46", DepthsRoomType.ABILITY, new Vector(42, 47, 54), new Vector(1.0, -20.0, -27.0), 10, RoomDirection.DOWN));
		mF2NormalRooms.add(new DepthsRoom("depths/f2r50", DepthsRoomType.ABILITY, new Vector(45, 27, 44), new Vector(1.0, -1.0, -19.0), 12, RoomDirection.UP));

		//F2 elite rooms
		mF2EliteRooms.add(new DepthsRoom("depths/f2r1", DepthsRoomType.ABILITY, new Vector(32, 33, 35), new Vector(1.0, -3.0, -18.0), 20, RoomDirection.UP));
		mF2EliteRooms.add(new DepthsRoom("depths/f2r4", DepthsRoomType.ABILITY, new Vector(39, 44, 40), new Vector(1.0, -1.0, -19.0), 16, RoomDirection.UP));
		mF2EliteRooms.add(new DepthsRoom("depths/f2r13", DepthsRoomType.ABILITY, new Vector(49, 58, 69), new Vector(1.0, -46.0, -12.0), 29, RoomDirection.DOWN));
		mF2EliteRooms.add(new DepthsRoom("depths/f2r15", DepthsRoomType.ABILITY, new Vector(51, 46, 62), new Vector(1.0, -30.0, -11.0), 39, RoomDirection.DOWN));
		mF2EliteRooms.add(new DepthsRoom("depths/f2r18", DepthsRoomType.ABILITY, new Vector(57, 37, 53), new Vector(1.0, -16.0, -25.0), 10, RoomDirection.EVEN));
		mF2EliteRooms.add(new DepthsRoom("depths/f2r22", DepthsRoomType.ABILITY, new Vector(65, 40, 47), new Vector(1.0, -6.0, -32.0), 11, RoomDirection.UP));
		mF2EliteRooms.add(new DepthsRoom("depths/f2r23", DepthsRoomType.ABILITY, new Vector(55, 67, 43), new Vector(1.0, -47.0, -24.0), 15, RoomDirection.DOWN));
		mF2EliteRooms.add(new DepthsRoom("depths/f2r25", DepthsRoomType.ABILITY, new Vector(45, 39, 61), new Vector(1.0, -22.0, -13.0), 17, RoomDirection.DOWN));
		mF2EliteRooms.add(new DepthsRoom("depths/f2r27", DepthsRoomType.ABILITY, new Vector(51, 38, 57), new Vector(1.0, -7.0, -45.0), 20, RoomDirection.EVEN));
		mF2EliteRooms.add(new DepthsRoom("depths/f2r30", DepthsRoomType.ABILITY, new Vector(51, 60, 70), new Vector(1.0, -18.0, -13.0), 18, RoomDirection.UP));
		mF2EliteRooms.add(new DepthsRoom("depths/f2r32", DepthsRoomType.ABILITY, new Vector(44, 39, 54), new Vector(1.0, -2.0, -46.0), 18, RoomDirection.UP));
		mF2EliteRooms.add(new DepthsRoom("depths/f2r34", DepthsRoomType.ABILITY, new Vector(80, 30, 81), new Vector(1.0, -4.0, -37.0), 25, RoomDirection.EVEN));
		mF2EliteRooms.add(new DepthsRoom("depths/f2r35", DepthsRoomType.ABILITY, new Vector(49, 37, 60), new Vector(1.0, -10.0, -17.0), 21, RoomDirection.EVEN));
		mF2EliteRooms.add(new DepthsRoom("depths/f2r39", DepthsRoomType.ABILITY, new Vector(56, 45, 60), new Vector(1.0, -20.0, -29.0), 20, RoomDirection.DOWN));
		mF2EliteRooms.add(new DepthsRoom("depths/f2r42", DepthsRoomType.ABILITY, new Vector(52, 38, 71), new Vector(1.0, -29.0, -4.0), 17, RoomDirection.DOWN));
		mF2EliteRooms.add(new DepthsRoom("depths/f2r43", DepthsRoomType.ABILITY, new Vector(51, 50, 70), new Vector(1.0, -38.0, -8.0), 18, RoomDirection.DOWN));
		mF2EliteRooms.add(new DepthsRoom("depths/f2r44", DepthsRoomType.ABILITY, new Vector(46, 55, 65), new Vector(1.0, -40.0, -13.0), 27, RoomDirection.DOWN));
		mF2EliteRooms.add(new DepthsRoom("depths/f2r48", DepthsRoomType.ABILITY, new Vector(50, 30, 51), new Vector(1.0, -5.0, -25.0), 30, RoomDirection.EVEN));
		mF2EliteRooms.add(new DepthsRoom("depths/f2r49", DepthsRoomType.ABILITY, new Vector(54, 42, 68), new Vector(1.0, -2.0, -22.0), 31, RoomDirection.UP));

		//F2 twisted rooms
		mF2TwistedRooms.add(new DepthsRoom("depths/f2r33", DepthsRoomType.TWISTED, new Vector(57, 58, 57), new Vector(1.0, -25.0, -48.0), 51, RoomDirection.DOWN));
		mF2TwistedRooms.add(new DepthsRoom("depths/f2r47", DepthsRoomType.TWISTED, new Vector(106, 64, 80), new Vector(1.0, -11.0, -25.0), 61, RoomDirection.UP));

		//F2 boss room
		DepthsRoom f2r26 = new DepthsRoom("depths/f2r26", DepthsRoomType.BOSS, new Vector(73, 48, 56), new Vector(1.0, -11.0, -35.0), 0, RoomDirection.EVEN);
		mF2BossRoom = f2r26;

		//F3 normal rooms
		mF3NormalRooms.add(new DepthsRoom("depths/f3r1", DepthsRoomType.ABILITY, new Vector(39, 31, 35), new Vector(1.0, -0.0, -17.0), 9, RoomDirection.EVEN));
		mF3NormalRooms.add(new DepthsRoom("depths/f3r2", DepthsRoomType.ABILITY, new Vector(40, 24, 25), new Vector(1.0, -0.0, -17.0), 11, RoomDirection.UP));
		mF3NormalRooms.add(new DepthsRoom("depths/f3r3", DepthsRoomType.ABILITY, new Vector(33, 19, 44), new Vector(1.0, -3.0, -6.0), 13, RoomDirection.UP));
		mF3NormalRooms.add(new DepthsRoom("depths/f3r4", DepthsRoomType.ABILITY, new Vector(36, 26, 36), new Vector(1.0, -5.0, -7.0), 8, RoomDirection.UP));
		mF3NormalRooms.add(new DepthsRoom("depths/f3r6", DepthsRoomType.ABILITY, new Vector(22, 37, 35), new Vector(1.0, -26.0, -7.0), 14, RoomDirection.DOWN));
		mF3NormalRooms.add(new DepthsRoom("depths/f3r8", DepthsRoomType.ABILITY, new Vector(53, 35, 60), new Vector(1.0, -10.0, -8.0), 14, RoomDirection.EVEN));
		mF3NormalRooms.add(new DepthsRoom("depths/f3r9", DepthsRoomType.ABILITY, new Vector(50, 46, 66), new Vector(1.0, -17.0, -57.0), 15, RoomDirection.EVEN));
		mF3NormalRooms.add(new DepthsRoom("depths/f3r10", DepthsRoomType.ABILITY, new Vector(47, 32, 46), new Vector(1.0, -17.0, -24.0), 15, RoomDirection.DOWN));
		mF3NormalRooms.add(new DepthsRoom("depths/f3r13", DepthsRoomType.ABILITY, new Vector(52, 27, 44), new Vector(1.0, -4.0, -16.0), 17, RoomDirection.EVEN));
		mF3NormalRooms.add(new DepthsRoom("depths/f3r15", DepthsRoomType.ABILITY, new Vector(43, 41, 39), new Vector(1.0, -13.0, -19.0), 12, RoomDirection.UP));
		mF3NormalRooms.add(new DepthsRoom("depths/f3r26", DepthsRoomType.ABILITY, new Vector(51, 25, 25), new Vector(1.0, -1.0, -12.0), 8, RoomDirection.UP));
		mF3NormalRooms.add(new DepthsRoom("depths/f3r27", DepthsRoomType.ABILITY, new Vector(52, 34, 51), new Vector(1.0, -5.0, -7.0), 8, RoomDirection.UP));
		mF3NormalRooms.add(new DepthsRoom("depths/f3r30", DepthsRoomType.ABILITY, new Vector(40, 37, 37), new Vector(1.0, -8.0, -12.0), 9, RoomDirection.EVEN));
		mF3NormalRooms.add(new DepthsRoom("depths/f3r33", DepthsRoomType.ABILITY, new Vector(39, 50, 38), new Vector(1.0, -39.0, -13.0), 15, RoomDirection.DOWN));
		mF3NormalRooms.add(new DepthsRoom("depths/f3r34", DepthsRoomType.ABILITY, new Vector(44, 25, 36), new Vector(1.0, -14.0, -27.0), 12, RoomDirection.DOWN));
		mF3NormalRooms.add(new DepthsRoom("depths/f3r35", DepthsRoomType.ABILITY, new Vector(42, 40, 40), new Vector(1.0, -26.0, -7.0), 13, RoomDirection.DOWN));
		mF3NormalRooms.add(new DepthsRoom("depths/f3r37", DepthsRoomType.ABILITY, new Vector(48, 20, 47), new Vector(1.0, -3.0, -15.0), 12, RoomDirection.EVEN));
		mF3NormalRooms.add(new DepthsRoom("depths/f3r38", DepthsRoomType.ABILITY, new Vector(39, 21, 29), new Vector(1.0, -6.0, -19.0), 6, RoomDirection.DOWN));
		mF3NormalRooms.add(new DepthsRoom("depths/f3r39", DepthsRoomType.ABILITY, new Vector(34, 59, 33), new Vector(1.0, -6.0, -16.0), 13, RoomDirection.UP));
		mF3NormalRooms.add(new DepthsRoom("depths/f3r40", DepthsRoomType.ABILITY, new Vector(60, 34, 47), new Vector(1.0, -6.0, -26.0), 14, RoomDirection.EVEN));
		mF3NormalRooms.add(new DepthsRoom("depths/f3r41", DepthsRoomType.ABILITY, new Vector(34, 26, 42), new Vector(1.0, -2.0, -27.0), 14, RoomDirection.UP));
		mF3NormalRooms.add(new DepthsRoom("depths/f3r43", DepthsRoomType.ABILITY, new Vector(41, 47, 31), new Vector(1.0, -9.0, -12.0), 12, RoomDirection.UP));
		mF3NormalRooms.add(new DepthsRoom("depths/f3r44", DepthsRoomType.ABILITY, new Vector(32, 32, 33), new Vector(1.0, -13.0, -16.0), 11, RoomDirection.DOWN));
		mF3NormalRooms.add(new DepthsRoom("depths/f3r45", DepthsRoomType.ABILITY, new Vector(43, 69, 56), new Vector(1.0, -20.0, -27.0), 10, RoomDirection.UP));

		//F3 elite rooms
		mF3EliteRooms.add(new DepthsRoom("depths/f3r5", DepthsRoomType.ABILITY, new Vector(39, 40, 40), new Vector(1.0, -5.0, -19.0), 28, RoomDirection.UP));
		mF3EliteRooms.add(new DepthsRoom("depths/f3r7", DepthsRoomType.ABILITY, new Vector(48, 42, 58), new Vector(1.0, -9.0, -44.0), 15, RoomDirection.UP));
		mF3EliteRooms.add(new DepthsRoom("depths/f3r12", DepthsRoomType.ABILITY, new Vector(51, 47, 53), new Vector(1.0, -34.0, -45.0), 17, RoomDirection.DOWN));
		mF3EliteRooms.add(new DepthsRoom("depths/f3r14", DepthsRoomType.ABILITY, new Vector(51, 60, 41), new Vector(1.0, -5.0, -24.0), 15, RoomDirection.UP));
		mF3EliteRooms.add(new DepthsRoom("depths/f3r20", DepthsRoomType.ABILITY, new Vector(43, 57, 54), new Vector(1.0, -38.0, -24.0), 14, RoomDirection.EVEN));
		mF3EliteRooms.add(new DepthsRoom("depths/f3r21", DepthsRoomType.ABILITY, new Vector(66, 71, 67), new Vector(1.0, -34.0, -12.0), 23, RoomDirection.UP));
		mF3EliteRooms.add(new DepthsRoom("depths/f3r22", DepthsRoomType.ABILITY, new Vector(51, 43, 65), new Vector(1.0, -10.0, -8.0), 25, RoomDirection.UP));
		mF3EliteRooms.add(new DepthsRoom("depths/f3r23", DepthsRoomType.ABILITY, new Vector(64, 48, 51), new Vector(1.0, -9.0, -14.0), 22, RoomDirection.DOWN));
		mF3EliteRooms.add(new DepthsRoom("depths/f3r24", DepthsRoomType.ABILITY, new Vector(52, 36, 55), new Vector(1.0, -22.0, -30.0), 22, RoomDirection.DOWN));
		mF3EliteRooms.add(new DepthsRoom("depths/f3r28", DepthsRoomType.ABILITY, new Vector(45, 44, 37), new Vector(1.0, -24.0, -18.0), 15, RoomDirection.DOWN));
		mF3EliteRooms.add(new DepthsRoom("depths/f3r29", DepthsRoomType.ABILITY, new Vector(61, 53, 60), new Vector(1.0, -8.0, -46.0), 23, RoomDirection.EVEN));
		mF3EliteRooms.add(new DepthsRoom("depths/f3r31", DepthsRoomType.ABILITY, new Vector(53, 39, 45), new Vector(1.0, -12.0, -36.0), 18, RoomDirection.UP));
		mF3EliteRooms.add(new DepthsRoom("depths/f3r32", DepthsRoomType.ABILITY, new Vector(52, 51, 61), new Vector(1.0, -8.0, -37.0), 21, RoomDirection.UP));
		mF3EliteRooms.add(new DepthsRoom("depths/f3r42", DepthsRoomType.ABILITY, new Vector(50, 46, 63), new Vector(1.0, -29.0, -27.0), 21, RoomDirection.EVEN));
		mF3EliteRooms.add(new DepthsRoom("depths/f3r46", DepthsRoomType.ABILITY, new Vector(62, 41, 51), new Vector(1.0, -30.0, -37.0), 24, RoomDirection.DOWN));

		//F3 utility rooms
		mF3UtilityRooms.add(new DepthsRoom("depths/f3r11", DepthsRoomType.UTILITY, new Vector(34, 26, 33), new Vector(1.0, -6.0, -16.0), 0, RoomDirection.EVEN));
		mF3UtilityRooms.add(new DepthsRoom("depths/f3r16", DepthsRoomType.UTILITY, new Vector(38, 26, 38), new Vector(1.0, -11.0, -17.0), 0, RoomDirection.EVEN));
		mF3UtilityRooms.add(new DepthsRoom("depths/f3r17", DepthsRoomType.UTILITY, new Vector(32, 23, 31), new Vector(1.0, -6.0, -15.0), 0, RoomDirection.EVEN));
		mF3UtilityRooms.add(new DepthsRoom("depths/f3r18", DepthsRoomType.UTILITY, new Vector(35, 18, 27), new Vector(1.0, -4.0, -7.0), 0, RoomDirection.EVEN));
		mF3UtilityRooms.add(new DepthsRoom("depths/f3r19", DepthsRoomType.UTILITY, new Vector(31, 22, 36), new Vector(1.0, -3.0, -29.0), 0, RoomDirection.EVEN));

		//F3 twisted rooms
		mF3TwistedRooms.add(new DepthsRoom("depths/f3r36", DepthsRoomType.TWISTED, new Vector(51, 61, 71), new Vector(1.0, -45.0, -60.0), 53, RoomDirection.EVEN));

		//Boss f3 room
		mF3BossRoom = new DepthsRoom("depths/f3r25", DepthsRoomType.BOSS, new Vector(63, 58, 64), new Vector(1.0, -1.0, -32.0), 0, RoomDirection.EVEN);
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
		box.expand(BlockFace.DOWN, room.mSize.getY());
		box.expand(BlockFace.NORTH, room.mSize.getZ());
		box.expand(BlockFace.SOUTH, room.mSize.getZ());
		for (Entity e: w.getNearbyEntities(box)) {
			if (!(e instanceof Player)) {
				e.remove();
			}
		}

		party.setRoomX(spawn.getBlockX());

		Plugin.getInstance().getLogger().info("Summoning structure " + room.mLoadPath);
		StructuresAPI.loadAndPasteStructure(room.mLoadPath, spawn, true);
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
	 * @param treasure the treasure score given to the party
	 */
	public void goToNextFloor(DepthsParty party, int treasure) {

		World world = Bukkit.getWorld(party.mWorldUUID);
		if (world == null) {
			Plugin.getInstance().getLogger().info("Got null world from party's id - DepthsRoomRepository");
			return;
		}
		Location loc = new Location(world, party.mFloorLobbyLoadPoint.getX(), party.mFloorLobbyLoadPoint.getY(), party.mFloorLobbyLoadPoint.getZ());

		//Separate rooms by floor here
		int nextFloorNum = party.getFloor() + 1;
		String path;
		if (nextFloorNum > CUSTOM_FLOOR_LOBBIES) {
			path = "depths/f11lobby";
		} else {
			path = "depths/f" + nextFloorNum + "lobby";
		}

		StructuresAPI.loadAndPasteStructure(path, loc, true).whenComplete((unused, ex) -> {
			if (ex != null) {
				ex.printStackTrace();
				for (DepthsPlayer dp : party.mPlayersInParty) {
					Player p = Bukkit.getPlayer(dp.mPlayerId);
					if (p != null) {
						p.sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + ChatColor.RED + "Failed to load lobby structure " + path + ". Contact a moderator.");
					}
				}
			} else {
				Location l = new Location(world, party.mFloorLobbyLoadPlayerTpPoint.getX(), party.mFloorLobbyLoadPlayerTpPoint.getY(), party.mFloorLobbyLoadPlayerTpPoint.getZ());
				//Tp all the players to it
				for (DepthsPlayer dp : party.mPlayersInParty) {
					Player p = Bukkit.getPlayer(dp.mPlayerId);
					if (p != null) {
						l.setYaw(270f);
						p.teleport(l);
						PotionUtils.applyPotion(Plugin.getInstance(), p, new PotionEffect(PotionEffectType.BLINDNESS, 2 * 20, 2));
						p.sendMessage(DepthsUtils.DEPTHS_MESSAGE_PREFIX + "Your party earned " + treasure + " treasure score for clearing floor " + party.getFloor() + "! Sending your party to next floor.");
					}
					PlayerUtils.executeCommandOnNearbyPlayers(l, 20, "stopsound @s record");
				}
				//Reset used rooms
				party.mOldRooms.clear();
				//Just in case they get stuck, set the spawner break trigger to zero
				party.mSpawnersToBreak = 0;
			}
		});
	}
}
