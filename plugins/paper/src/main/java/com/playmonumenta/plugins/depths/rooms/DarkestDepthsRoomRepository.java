package com.playmonumenta.plugins.depths.rooms;

import org.bukkit.util.Vector;

public class DarkestDepthsRoomRepository extends RoomRepository {
	private static class Floor1 extends RepositoryFloor {
		public Floor1() {
			//F1 utility rooms
			mUtilityRooms.add(new DepthsRoom("depths/f1r9", DepthsRoomType.UTILITY, new Vector(38, 26, 38), new Vector(1.0, -11.0, -17.0), 0, DepthsRoom.RoomDirection.EVEN));
			mUtilityRooms.add(new DepthsRoom("depths/f1r19", DepthsRoomType.UTILITY, new Vector(35, 18, 27), new Vector(1.0, -4.0, -7.0), 0, DepthsRoom.RoomDirection.EVEN));
			mUtilityRooms.add(new DepthsRoom("depths/f1r20", DepthsRoomType.UTILITY, new Vector(34, 26, 33), new Vector(1.0, -6.0, -16.0), 0, DepthsRoom.RoomDirection.EVEN));
			mUtilityRooms.add(new DepthsRoom("depths/f1r22", DepthsRoomType.UTILITY, new Vector(32, 23, 31), new Vector(1.0, -6.0, -15.0), 0, DepthsRoom.RoomDirection.EVEN));
			mUtilityRooms.add(new DepthsRoom("depths/f1r28", DepthsRoomType.UTILITY, new Vector(36, 24, 31), new Vector(1.0, -3.0, -15.0), 0, DepthsRoom.RoomDirection.EVEN));

			//F1 normal rooms
			mNormalRooms.add(new DepthsRoom("depths/f1r1", DepthsRoomType.ABILITY, new Vector(32, 33, 34), new Vector(1.0, -5.0, -18.0), 12, DepthsRoom.RoomDirection.UP));
			mNormalRooms.add(new DepthsRoom("depths/f1r2", DepthsRoomType.ABILITY, new Vector(29, 40, 30), new Vector(1.0, -27.0, -23.0), 14, DepthsRoom.RoomDirection.DOWN));
			mNormalRooms.add(new DepthsRoom("depths/f1r3", DepthsRoomType.ABILITY, new Vector(39, 20, 31), new Vector(1.0, -1.0, -8.0), 8, DepthsRoom.RoomDirection.UP));
			mNormalRooms.add(new DepthsRoom("depths/f1r4", DepthsRoomType.ABILITY, new Vector(40, 39, 39), new Vector(1.0, -1.0, -22.0), 13, DepthsRoom.RoomDirection.UP));
			mNormalRooms.add(new DepthsRoom("depths/f1r5", DepthsRoomType.ABILITY, new Vector(39, 28, 37), new Vector(1.0, -3.0, -10.0), 11, DepthsRoom.RoomDirection.UP));
			mNormalRooms.add(new DepthsRoom("depths/f1r6", DepthsRoomType.ABILITY, new Vector(35, 34, 35), new Vector(1.0, -1.0, -29.0), 8, DepthsRoom.RoomDirection.UP));
			mNormalRooms.add(new DepthsRoom("depths/f1r7", DepthsRoomType.ABILITY, new Vector(38, 28, 40), new Vector(1.0, -4.0, -21.0), 11, DepthsRoom.RoomDirection.EVEN));
			mNormalRooms.add(new DepthsRoom("depths/f1r8", DepthsRoomType.ABILITY, new Vector(38, 26, 38), new Vector(1.0, -8.0, -17.0), 9, DepthsRoom.RoomDirection.DOWN));
			mNormalRooms.add(new DepthsRoom("depths/f1r10", DepthsRoomType.ABILITY, new Vector(37, 35, 38), new Vector(1.0, -18.0, -16.0), 8, DepthsRoom.RoomDirection.EVEN));
			mNormalRooms.add(new DepthsRoom("depths/f1r11", DepthsRoomType.ABILITY, new Vector(34, 30, 37), new Vector(1.0, -2.0, -5.0), 15, DepthsRoom.RoomDirection.EVEN));
			mNormalRooms.add(new DepthsRoom("depths/f1r23", DepthsRoomType.ABILITY, new Vector(31, 24, 30), new Vector(1.0, -8.0, -12.0), 8, DepthsRoom.RoomDirection.EVEN));
			mNormalRooms.add(new DepthsRoom("depths/f1r25", DepthsRoomType.ABILITY, new Vector(28, 40, 31), new Vector(1.0, -22.0, -7.0), 8, DepthsRoom.RoomDirection.DOWN));
			mNormalRooms.add(new DepthsRoom("depths/f1r29", DepthsRoomType.ABILITY, new Vector(44, 22, 23), new Vector(1.0, -10.0, -22.0), 12, DepthsRoom.RoomDirection.EVEN));
			mNormalRooms.add(new DepthsRoom("depths/f1r30", DepthsRoomType.ABILITY, new Vector(33, 32, 29), new Vector(1.0, -21.0, -5.0), 7, DepthsRoom.RoomDirection.DOWN));
			mNormalRooms.add(new DepthsRoom("depths/f1r32", DepthsRoomType.ABILITY, new Vector(36, 40, 30), new Vector(1.0, -28.0, -8.0), 12, DepthsRoom.RoomDirection.DOWN));
			mNormalRooms.add(new DepthsRoom("depths/f1r33", DepthsRoomType.ABILITY, new Vector(33, 37, 39), new Vector(1.0, -28.0, -25.0), 8, DepthsRoom.RoomDirection.DOWN));
			mNormalRooms.add(new DepthsRoom("depths/f1r35", DepthsRoomType.ABILITY, new Vector(51, 44, 44), new Vector(1.0, -30.0, -20.0), 12, DepthsRoom.RoomDirection.DOWN));
			mNormalRooms.add(new DepthsRoom("depths/f1r42", DepthsRoomType.ABILITY, new Vector(31, 58, 32), new Vector(1.0, -3.0, -16.0), 11, DepthsRoom.RoomDirection.UP));
			mNormalRooms.add(new DepthsRoom("depths/f1r45", DepthsRoomType.ABILITY, new Vector(49, 37, 52), new Vector(1.0, -6.0, -29.0), 11, DepthsRoom.RoomDirection.UP));
			mNormalRooms.add(new DepthsRoom("depths/f1r46", DepthsRoomType.ABILITY, new Vector(51, 56, 32), new Vector(1.0, -9.0, -14.0), 15, DepthsRoom.RoomDirection.UP));
			mNormalRooms.add(new DepthsRoom("depths/f1r47", DepthsRoomType.ABILITY, new Vector(45, 52, 55), new Vector(1.0, -33.0, -27.0), 11, DepthsRoom.RoomDirection.DOWN));
			mNormalRooms.add(new DepthsRoom("depths/f1r48", DepthsRoomType.ABILITY, new Vector(32, 35, 35), new Vector(1.0, -8.0, -20.0), 12, DepthsRoom.RoomDirection.UP));
			mNormalRooms.add(new DepthsRoom("depths/f1r49", DepthsRoomType.ABILITY, new Vector(38, 31, 37), new Vector(1.0, -11.0, -31.0), 12, DepthsRoom.RoomDirection.EVEN));
			mNormalRooms.add(new DepthsRoom("depths/f1r55", DepthsRoomType.ABILITY, new Vector(33, 39, 33), new Vector(1.0, -1.0, -7.0), 13, DepthsRoom.RoomDirection.UP));
			mNormalRooms.add(new DepthsRoom("depths/f1r56", DepthsRoomType.ABILITY, new Vector(51, 43, 45), new Vector(1.0, -17.0, -23.0), 11, DepthsRoom.RoomDirection.EVEN));
			mNormalRooms.add(new DepthsRoom("depths/f1r58", DepthsRoomType.ABILITY, new Vector(48, 30, 58), new Vector(1.0, -8.0, -37.0), 11, DepthsRoom.RoomDirection.EVEN));
			mNormalRooms.add(new DepthsRoom("depths/f1r60", DepthsRoomType.ABILITY, new Vector(38, 23, 45), new Vector(1.0, -5.0, -18.0), 10, DepthsRoom.RoomDirection.EVEN));

			//F1 elite rooms
			mEliteRooms.add(new DepthsRoom("depths/f1r12", DepthsRoomType.ABILITY, new Vector(40, 41, 40), new Vector(1.0, -2.0, -21.0), 23, DepthsRoom.RoomDirection.UP));
			mEliteRooms.add(new DepthsRoom("depths/f1r13", DepthsRoomType.ABILITY, new Vector(47, 54, 55), new Vector(1.0, -5.0, -24.0), 12, DepthsRoom.RoomDirection.EVEN));
			mEliteRooms.add(new DepthsRoom("depths/f1r14", DepthsRoomType.ABILITY, new Vector(49, 54, 69), new Vector(1.0, -29.0, -33.0), 34, DepthsRoom.RoomDirection.DOWN));
			mEliteRooms.add(new DepthsRoom("depths/f1r15", DepthsRoomType.ABILITY, new Vector(51, 43, 54), new Vector(1.0, -20.0, -11.0), 45, DepthsRoom.RoomDirection.DOWN));
			mEliteRooms.add(new DepthsRoom("depths/f1r17", DepthsRoomType.ABILITY, new Vector(48, 53, 36), new Vector(1.0, -9.0, -25.0), 17, DepthsRoom.RoomDirection.UP));
			mEliteRooms.add(new DepthsRoom("depths/f1r21", DepthsRoomType.ABILITY, new Vector(41, 56, 40), new Vector(1.0, -2.0, -19.0), 17, DepthsRoom.RoomDirection.UP));
			mEliteRooms.add(new DepthsRoom("depths/f1r24", DepthsRoomType.ABILITY, new Vector(51, 60, 70), new Vector(1.0, -43.0, -32.0), 39, DepthsRoom.RoomDirection.DOWN));
			mEliteRooms.add(new DepthsRoom("depths/f1r26", DepthsRoomType.ABILITY, new Vector(58, 39, 49), new Vector(1.0, -2.0, -26.0), 19, DepthsRoom.RoomDirection.UP));
			mEliteRooms.add(new DepthsRoom("depths/f1r27", DepthsRoomType.ABILITY, new Vector(63, 38, 63), new Vector(1.0, -11.0, -32.0), 32, DepthsRoom.RoomDirection.DOWN));
			mEliteRooms.add(new DepthsRoom("depths/f1r31", DepthsRoomType.ABILITY, new Vector(51, 47, 56), new Vector(1.0, -10.0, -42.0), 16, DepthsRoom.RoomDirection.UP));
			mEliteRooms.add(new DepthsRoom("depths/f1r34", DepthsRoomType.ABILITY, new Vector(51, 60, 70), new Vector(1.0, -15.0, -7.0), 21, DepthsRoom.RoomDirection.DOWN));
			mEliteRooms.add(new DepthsRoom("depths/f1r36", DepthsRoomType.ABILITY, new Vector(72, 53, 65), new Vector(1.0, -8.0, -49.0), 14, DepthsRoom.RoomDirection.EVEN));
			mEliteRooms.add(new DepthsRoom("depths/f1r37", DepthsRoomType.ABILITY, new Vector(52, 61, 71), new Vector(1.0, 0.0, -7.0), 43, DepthsRoom.RoomDirection.UP));
			mEliteRooms.add(new DepthsRoom("depths/f1r38", DepthsRoomType.ABILITY, new Vector(60, 29, 36), new Vector(1.0, -8.0, -17.0), 17, DepthsRoom.RoomDirection.UP));
			mEliteRooms.add(new DepthsRoom("depths/f1r40", DepthsRoomType.ABILITY, new Vector(30, 22, 71), new Vector(1.0, -2.0, -7.0), 23, DepthsRoom.RoomDirection.EVEN));
			mEliteRooms.add(new DepthsRoom("depths/f1r43", DepthsRoomType.ABILITY, new Vector(44, 23, 40), new Vector(1.0, -1.0, -32.0), 19, DepthsRoom.RoomDirection.UP));
			mEliteRooms.add(new DepthsRoom("depths/f1r44", DepthsRoomType.ABILITY, new Vector(34, 40, 36), new Vector(1.0, -23.0, -29.0), 26, DepthsRoom.RoomDirection.DOWN));
			mEliteRooms.add(new DepthsRoom("depths/f1r50", DepthsRoomType.ABILITY, new Vector(35, 54, 33), new Vector(1.0, -39.0, -18.0), 20, DepthsRoom.RoomDirection.DOWN));
			mEliteRooms.add(new DepthsRoom("depths/f1r51", DepthsRoomType.ABILITY, new Vector(44, 16, 71), new Vector(1.0, 0.0, -7.0), 21, DepthsRoom.RoomDirection.UP));
			mEliteRooms.add(new DepthsRoom("depths/f1r52", DepthsRoomType.ABILITY, new Vector(41, 39, 49), new Vector(1.0, -16.0, -10.0), 36, DepthsRoom.RoomDirection.DOWN));
			mEliteRooms.add(new DepthsRoom("depths/f1r53", DepthsRoomType.ABILITY, new Vector(53, 46, 73), new Vector(1.0, -7.0, -48.0), 19, DepthsRoom.RoomDirection.EVEN));
			mEliteRooms.add(new DepthsRoom("depths/f1r54", DepthsRoomType.ABILITY, new Vector(42, 27, 41), new Vector(1.0, -8.0, -22.0), 20, DepthsRoom.RoomDirection.DOWN));
			mEliteRooms.add(new DepthsRoom("depths/f1r57", DepthsRoomType.ABILITY, new Vector(62, 46, 50), new Vector(1.0, -17.0, -25.0), 19, DepthsRoom.RoomDirection.DOWN));
			mEliteRooms.add(new DepthsRoom("depths/f1r59", DepthsRoomType.ABILITY, new Vector(47, 32, 73), new Vector(1.0, -6.0, -9.0), 16, DepthsRoom.RoomDirection.UP));


			//F1 twisted rooms
			mTwistedRooms.add(new DepthsRoom("depths/f1r41", DepthsRoomType.TWISTED, new Vector(51, 60, 70), new Vector(1.0, -3.0, -61.0), 50, DepthsRoom.RoomDirection.EVEN));

			//Boss f1 room
			mBossRoom = new DepthsRoom("depths/f1r18", DepthsRoomType.BOSS, new Vector(51, 46, 56), new Vector(1.0, -16.0, -15.0), 0, DepthsRoom.RoomDirection.EVEN);
		}
	}

	private static class Floor2 extends RepositoryFloor {
		public Floor2() {
			// ANVIL ROOM - disabled
			//mUtilityRooms.add(new DepthsRoom("depths/f2r11", DepthsRoomType.UTILITY, new Vector(33, 28, 29), new Vector(1.0, -1.0, -6.0), 0, RoomDirection.EVEN));
			mUtilityRooms.add(new DepthsRoom("depths/f2r14", DepthsRoomType.UTILITY, new Vector(38, 26, 38), new Vector(1.0, -11.0, -17.0), 0, DepthsRoom.RoomDirection.EVEN));
			mUtilityRooms.add(new DepthsRoom("depths/f2r16", DepthsRoomType.UTILITY, new Vector(34, 26, 33), new Vector(1.0, -6.0, -16.0), 0, DepthsRoom.RoomDirection.EVEN));
			mUtilityRooms.add(new DepthsRoom("depths/f2r17", DepthsRoomType.UTILITY, new Vector(32, 23, 31), new Vector(1.0, -6.0, -15.0), 0, DepthsRoom.RoomDirection.EVEN));
			mUtilityRooms.add(new DepthsRoom("depths/f2r45", DepthsRoomType.UTILITY, new Vector(36, 24, 31), new Vector(1.0, -3.0, -15.0), 0, DepthsRoom.RoomDirection.EVEN));
			//Casino depths room
			mUtilityRooms.add(new DepthsRoom("depths/f2r12", DepthsRoomType.UTILITY, new Vector(35, 18, 27), new Vector(1.0, -4.0, -7.0), 0, DepthsRoom.RoomDirection.EVEN));

			//F2 normal rooms
			mNormalRooms.add(new DepthsRoom("depths/f2r2", DepthsRoomType.ABILITY, new Vector(28, 38, 27), new Vector(1.0, -24.0, -13.0), 10, DepthsRoom.RoomDirection.DOWN));
			mNormalRooms.add(new DepthsRoom("depths/f2r3", DepthsRoomType.ABILITY, new Vector(27, 28, 28), new Vector(1.0, -9.0, -7.0), 12, DepthsRoom.RoomDirection.UP));
			mNormalRooms.add(new DepthsRoom("depths/f2r5", DepthsRoomType.ABILITY, new Vector(38, 26, 38), new Vector(1.0, -15.0, -23.0), 8, DepthsRoom.RoomDirection.DOWN));
			mNormalRooms.add(new DepthsRoom("depths/f2r6", DepthsRoomType.ABILITY, new Vector(38, 26, 38), new Vector(1.0, -1.0, -9.0), 7, DepthsRoom.RoomDirection.UP));
			mNormalRooms.add(new DepthsRoom("depths/f2r7", DepthsRoomType.ABILITY, new Vector(38, 26, 38), new Vector(1.0, -1.0, -9.0), 11, DepthsRoom.RoomDirection.UP));
			mNormalRooms.add(new DepthsRoom("depths/f2r8", DepthsRoomType.ABILITY, new Vector(27, 29, 31), new Vector(1.0, -0.0, -9.0), 11, DepthsRoom.RoomDirection.UP));
			mNormalRooms.add(new DepthsRoom("depths/f2r9", DepthsRoomType.ABILITY, new Vector(32, 35, 36), new Vector(1.0, -23.0, -11.0), 12, DepthsRoom.RoomDirection.DOWN));
			mNormalRooms.add(new DepthsRoom("depths/f2r10", DepthsRoomType.ABILITY, new Vector(41, 26, 39), new Vector(1.0, -13.0, -11.0), 12, DepthsRoom.RoomDirection.DOWN));
			mNormalRooms.add(new DepthsRoom("depths/f2r19", DepthsRoomType.ABILITY, new Vector(55, 35, 48), new Vector(1.0, -19.0, -18.0), 10, DepthsRoom.RoomDirection.EVEN));
			mNormalRooms.add(new DepthsRoom("depths/f2r20", DepthsRoomType.ABILITY, new Vector(57, 37, 61), new Vector(1.0, -19.0, -33.0), 9, DepthsRoom.RoomDirection.DOWN));
			mNormalRooms.add(new DepthsRoom("depths/f2r21", DepthsRoomType.ABILITY, new Vector(57, 37, 61), new Vector(1.0, -17.0, -26.0), 11, DepthsRoom.RoomDirection.EVEN));
			mNormalRooms.add(new DepthsRoom("depths/f2r24", DepthsRoomType.ABILITY, new Vector(51, 33, 36), new Vector(1.0, -4.0, -12.0), 12, DepthsRoom.RoomDirection.UP));
			mNormalRooms.add(new DepthsRoom("depths/f2r28", DepthsRoomType.ABILITY, new Vector(54, 46, 34), new Vector(1.0, -17.0, -16.0), 11, DepthsRoom.RoomDirection.UP));
			mNormalRooms.add(new DepthsRoom("depths/f2r29", DepthsRoomType.ABILITY, new Vector(45, 46, 64), new Vector(1.0, -13.0, -30.0), 9, DepthsRoom.RoomDirection.UP));
			mNormalRooms.add(new DepthsRoom("depths/f2r31", DepthsRoomType.ABILITY, new Vector(34, 29, 34), new Vector(1.0, -5.0, -14.0), 8, DepthsRoom.RoomDirection.UP));
			mNormalRooms.add(new DepthsRoom("depths/f2r36", DepthsRoomType.ABILITY, new Vector(34, 31, 37), new Vector(1.0, -14.0, -30.0), 12, DepthsRoom.RoomDirection.DOWN));
			mNormalRooms.add(new DepthsRoom("depths/f2r37", DepthsRoomType.ABILITY, new Vector(34, 29, 31), new Vector(1.0, -15.0, -21.0), 14, DepthsRoom.RoomDirection.DOWN));
			mNormalRooms.add(new DepthsRoom("depths/f2r38", DepthsRoomType.ABILITY, new Vector(47, 54, 42), new Vector(1.0, -36.0, -21.0), 17, DepthsRoom.RoomDirection.DOWN));
			mNormalRooms.add(new DepthsRoom("depths/f2r40", DepthsRoomType.ABILITY, new Vector(49, 29, 35), new Vector(1.0, -10.0, -13.0), 11, DepthsRoom.RoomDirection.EVEN));
			mNormalRooms.add(new DepthsRoom("depths/f2r41", DepthsRoomType.ABILITY, new Vector(44, 44, 43), new Vector(1.0, -26.0, -18.0), 14, DepthsRoom.RoomDirection.DOWN));
			mNormalRooms.add(new DepthsRoom("depths/f2r46", DepthsRoomType.ABILITY, new Vector(42, 47, 54), new Vector(1.0, -20.0, -27.0), 10, DepthsRoom.RoomDirection.DOWN));
			mNormalRooms.add(new DepthsRoom("depths/f2r50", DepthsRoomType.ABILITY, new Vector(45, 27, 44), new Vector(1.0, -1.0, -19.0), 12, DepthsRoom.RoomDirection.UP));

			//F2 elite rooms
			mEliteRooms.add(new DepthsRoom("depths/f2r1", DepthsRoomType.ABILITY, new Vector(32, 33, 35), new Vector(1.0, -3.0, -18.0), 20, DepthsRoom.RoomDirection.UP));
			mEliteRooms.add(new DepthsRoom("depths/f2r4", DepthsRoomType.ABILITY, new Vector(39, 44, 40), new Vector(1.0, -1.0, -19.0), 16, DepthsRoom.RoomDirection.UP));
			mEliteRooms.add(new DepthsRoom("depths/f2r13", DepthsRoomType.ABILITY, new Vector(49, 58, 69), new Vector(1.0, -46.0, -12.0), 29, DepthsRoom.RoomDirection.DOWN));
			mEliteRooms.add(new DepthsRoom("depths/f2r15", DepthsRoomType.ABILITY, new Vector(51, 46, 62), new Vector(1.0, -30.0, -11.0), 39, DepthsRoom.RoomDirection.DOWN));
			mEliteRooms.add(new DepthsRoom("depths/f2r18", DepthsRoomType.ABILITY, new Vector(57, 37, 53), new Vector(1.0, -16.0, -25.0), 10, DepthsRoom.RoomDirection.EVEN));
			mEliteRooms.add(new DepthsRoom("depths/f2r22", DepthsRoomType.ABILITY, new Vector(65, 40, 47), new Vector(1.0, -6.0, -32.0), 11, DepthsRoom.RoomDirection.UP));
			mEliteRooms.add(new DepthsRoom("depths/f2r23", DepthsRoomType.ABILITY, new Vector(55, 67, 43), new Vector(1.0, -47.0, -24.0), 15, DepthsRoom.RoomDirection.DOWN));
			mEliteRooms.add(new DepthsRoom("depths/f2r25", DepthsRoomType.ABILITY, new Vector(45, 39, 61), new Vector(1.0, -22.0, -13.0), 17, DepthsRoom.RoomDirection.DOWN));
			mEliteRooms.add(new DepthsRoom("depths/f2r27", DepthsRoomType.ABILITY, new Vector(51, 38, 57), new Vector(1.0, -7.0, -45.0), 20, DepthsRoom.RoomDirection.EVEN));
			mEliteRooms.add(new DepthsRoom("depths/f2r30", DepthsRoomType.ABILITY, new Vector(51, 60, 70), new Vector(1.0, -18.0, -13.0), 18, DepthsRoom.RoomDirection.UP));
			mEliteRooms.add(new DepthsRoom("depths/f2r32", DepthsRoomType.ABILITY, new Vector(44, 39, 54), new Vector(1.0, -2.0, -46.0), 18, DepthsRoom.RoomDirection.UP));
			mEliteRooms.add(new DepthsRoom("depths/f2r34", DepthsRoomType.ABILITY, new Vector(80, 30, 81), new Vector(1.0, -4.0, -37.0), 25, DepthsRoom.RoomDirection.EVEN));
			mEliteRooms.add(new DepthsRoom("depths/f2r35", DepthsRoomType.ABILITY, new Vector(49, 37, 60), new Vector(1.0, -10.0, -17.0), 21, DepthsRoom.RoomDirection.EVEN));
			mEliteRooms.add(new DepthsRoom("depths/f2r39", DepthsRoomType.ABILITY, new Vector(56, 45, 60), new Vector(1.0, -20.0, -29.0), 20, DepthsRoom.RoomDirection.DOWN));
			mEliteRooms.add(new DepthsRoom("depths/f2r42", DepthsRoomType.ABILITY, new Vector(52, 38, 71), new Vector(1.0, -29.0, -4.0), 17, DepthsRoom.RoomDirection.DOWN));
			mEliteRooms.add(new DepthsRoom("depths/f2r43", DepthsRoomType.ABILITY, new Vector(51, 50, 70), new Vector(1.0, -38.0, -8.0), 18, DepthsRoom.RoomDirection.DOWN));
			mEliteRooms.add(new DepthsRoom("depths/f2r44", DepthsRoomType.ABILITY, new Vector(46, 55, 65), new Vector(1.0, -40.0, -13.0), 27, DepthsRoom.RoomDirection.DOWN));
			mEliteRooms.add(new DepthsRoom("depths/f2r48", DepthsRoomType.ABILITY, new Vector(50, 30, 51), new Vector(1.0, -5.0, -25.0), 30, DepthsRoom.RoomDirection.EVEN));
			mEliteRooms.add(new DepthsRoom("depths/f2r49", DepthsRoomType.ABILITY, new Vector(54, 42, 68), new Vector(1.0, -2.0, -22.0), 31, DepthsRoom.RoomDirection.UP));
			mEliteRooms.add(new DepthsRoom("depths/f2r51", DepthsRoomType.ABILITY, new Vector(51, 23, 40), new Vector(1.0, -3.0, -20.0), 17, DepthsRoom.RoomDirection.EVEN));
			mEliteRooms.add(new DepthsRoom("depths/f2r52", DepthsRoomType.ABILITY, new Vector(53, 45, 43), new Vector(1.0, -16.0, -21.0), 22, DepthsRoom.RoomDirection.UP));
			mEliteRooms.add(new DepthsRoom("depths/f2r53", DepthsRoomType.ABILITY, new Vector(50, 60, 49), new Vector(1.0, -30.0, -24.0), 25, DepthsRoom.RoomDirection.DOWN));
			mEliteRooms.add(new DepthsRoom("depths/f2r54", DepthsRoomType.ABILITY, new Vector(52, 36, 59), new Vector(1.0, -4.0, -22.0), 21, DepthsRoom.RoomDirection.UP));
			mEliteRooms.add(new DepthsRoom("depths/f2r55", DepthsRoomType.ABILITY, new Vector(61, 33, 51), new Vector(1.0, -11.0, -39.0), 16, DepthsRoom.RoomDirection.UP));
			mEliteRooms.add(new DepthsRoom("depths/f2r56", DepthsRoomType.ABILITY, new Vector(106, 43, 55), new Vector(1.0, -12.0, -14.0), 17, DepthsRoom.RoomDirection.EVEN));

			//F2 twisted rooms
			mTwistedRooms.add(new DepthsRoom("depths/f2r33", DepthsRoomType.TWISTED, new Vector(57, 58, 57), new Vector(1.0, -25.0, -48.0), 51, DepthsRoom.RoomDirection.DOWN));
			mTwistedRooms.add(new DepthsRoom("depths/f2r47", DepthsRoomType.TWISTED, new Vector(106, 64, 80), new Vector(1.0, -11.0, -25.0), 61, DepthsRoom.RoomDirection.UP));

			//F2 boss room
			mBossRoom = new DepthsRoom("depths/f2r26", DepthsRoomType.BOSS, new Vector(73, 48, 56), new Vector(1.0, -11.0, -35.0), 0, DepthsRoom.RoomDirection.EVEN);

		}
	}

	private static class Floor3 extends RepositoryFloor {
		public Floor3() {
			//F3 normal rooms
			mNormalRooms.add(new DepthsRoom("depths/f3r1", DepthsRoomType.ABILITY, new Vector(39, 31, 35), new Vector(1.0, -0.0, -17.0), 9, DepthsRoom.RoomDirection.EVEN));
			mNormalRooms.add(new DepthsRoom("depths/f3r2", DepthsRoomType.ABILITY, new Vector(40, 24, 25), new Vector(1.0, -0.0, -17.0), 11, DepthsRoom.RoomDirection.UP));
			mNormalRooms.add(new DepthsRoom("depths/f3r3", DepthsRoomType.ABILITY, new Vector(33, 19, 44), new Vector(1.0, -3.0, -6.0), 13, DepthsRoom.RoomDirection.UP));
			mNormalRooms.add(new DepthsRoom("depths/f3r4", DepthsRoomType.ABILITY, new Vector(36, 26, 36), new Vector(1.0, -5.0, -7.0), 8, DepthsRoom.RoomDirection.UP));
			mNormalRooms.add(new DepthsRoom("depths/f3r6", DepthsRoomType.ABILITY, new Vector(22, 37, 35), new Vector(1.0, -26.0, -7.0), 14, DepthsRoom.RoomDirection.DOWN));
			mNormalRooms.add(new DepthsRoom("depths/f3r8", DepthsRoomType.ABILITY, new Vector(53, 35, 60), new Vector(1.0, -10.0, -8.0), 14, DepthsRoom.RoomDirection.EVEN));
			mNormalRooms.add(new DepthsRoom("depths/f3r9", DepthsRoomType.ABILITY, new Vector(50, 46, 66), new Vector(1.0, -17.0, -57.0), 15, DepthsRoom.RoomDirection.EVEN));
			mNormalRooms.add(new DepthsRoom("depths/f3r10", DepthsRoomType.ABILITY, new Vector(47, 32, 46), new Vector(1.0, -17.0, -24.0), 15, DepthsRoom.RoomDirection.DOWN));
			mNormalRooms.add(new DepthsRoom("depths/f3r13", DepthsRoomType.ABILITY, new Vector(52, 27, 44), new Vector(1.0, -4.0, -16.0), 17, DepthsRoom.RoomDirection.EVEN));
			mNormalRooms.add(new DepthsRoom("depths/f3r15", DepthsRoomType.ABILITY, new Vector(43, 41, 39), new Vector(1.0, -13.0, -19.0), 12, DepthsRoom.RoomDirection.UP));
			mNormalRooms.add(new DepthsRoom("depths/f3r26", DepthsRoomType.ABILITY, new Vector(51, 25, 25), new Vector(1.0, -1.0, -12.0), 8, DepthsRoom.RoomDirection.UP));
			mNormalRooms.add(new DepthsRoom("depths/f3r27", DepthsRoomType.ABILITY, new Vector(52, 34, 51), new Vector(1.0, -5.0, -7.0), 8, DepthsRoom.RoomDirection.UP));
			mNormalRooms.add(new DepthsRoom("depths/f3r30", DepthsRoomType.ABILITY, new Vector(40, 37, 37), new Vector(1.0, -8.0, -12.0), 9, DepthsRoom.RoomDirection.EVEN));
			mNormalRooms.add(new DepthsRoom("depths/f3r33", DepthsRoomType.ABILITY, new Vector(39, 50, 38), new Vector(1.0, -39.0, -13.0), 15, DepthsRoom.RoomDirection.DOWN));
			mNormalRooms.add(new DepthsRoom("depths/f3r34", DepthsRoomType.ABILITY, new Vector(44, 25, 36), new Vector(1.0, -14.0, -27.0), 12, DepthsRoom.RoomDirection.DOWN));
			mNormalRooms.add(new DepthsRoom("depths/f3r35", DepthsRoomType.ABILITY, new Vector(42, 40, 40), new Vector(1.0, -26.0, -7.0), 13, DepthsRoom.RoomDirection.DOWN));
			mNormalRooms.add(new DepthsRoom("depths/f3r37", DepthsRoomType.ABILITY, new Vector(48, 20, 47), new Vector(1.0, -3.0, -15.0), 12, DepthsRoom.RoomDirection.EVEN));
			mNormalRooms.add(new DepthsRoom("depths/f3r38", DepthsRoomType.ABILITY, new Vector(39, 21, 29), new Vector(1.0, -6.0, -19.0), 6, DepthsRoom.RoomDirection.DOWN));
			mNormalRooms.add(new DepthsRoom("depths/f3r39", DepthsRoomType.ABILITY, new Vector(34, 59, 33), new Vector(1.0, -6.0, -16.0), 13, DepthsRoom.RoomDirection.UP));
			mNormalRooms.add(new DepthsRoom("depths/f3r40", DepthsRoomType.ABILITY, new Vector(60, 34, 47), new Vector(1.0, -6.0, -26.0), 14, DepthsRoom.RoomDirection.EVEN));
			mNormalRooms.add(new DepthsRoom("depths/f3r41", DepthsRoomType.ABILITY, new Vector(34, 26, 42), new Vector(1.0, -2.0, -27.0), 14, DepthsRoom.RoomDirection.UP));
			mNormalRooms.add(new DepthsRoom("depths/f3r43", DepthsRoomType.ABILITY, new Vector(41, 47, 31), new Vector(1.0, -9.0, -12.0), 12, DepthsRoom.RoomDirection.UP));
			mNormalRooms.add(new DepthsRoom("depths/f3r44", DepthsRoomType.ABILITY, new Vector(32, 32, 33), new Vector(1.0, -13.0, -16.0), 11, DepthsRoom.RoomDirection.DOWN));
			mNormalRooms.add(new DepthsRoom("depths/f3r45", DepthsRoomType.ABILITY, new Vector(43, 55, 56), new Vector(1.0, -20.0, -27.0), 10, DepthsRoom.RoomDirection.UP));
			mNormalRooms.add(new DepthsRoom("depths/f3r47", DepthsRoomType.ABILITY, new Vector(50, 36, 73), new Vector(1.0, -3.0, -36.0), 13, DepthsRoom.RoomDirection.UP));

			//F3 elite rooms
			mEliteRooms.add(new DepthsRoom("depths/f3r5", DepthsRoomType.ABILITY, new Vector(39, 40, 40), new Vector(1.0, -5.0, -19.0), 28, DepthsRoom.RoomDirection.UP));
			mEliteRooms.add(new DepthsRoom("depths/f3r7", DepthsRoomType.ABILITY, new Vector(48, 42, 58), new Vector(1.0, -9.0, -44.0), 15, DepthsRoom.RoomDirection.UP));
			mEliteRooms.add(new DepthsRoom("depths/f3r12", DepthsRoomType.ABILITY, new Vector(51, 47, 53), new Vector(1.0, -34.0, -45.0), 17, DepthsRoom.RoomDirection.DOWN));
			mEliteRooms.add(new DepthsRoom("depths/f3r14", DepthsRoomType.ABILITY, new Vector(51, 60, 41), new Vector(1.0, -5.0, -24.0), 15, DepthsRoom.RoomDirection.UP));
			mEliteRooms.add(new DepthsRoom("depths/f3r20", DepthsRoomType.ABILITY, new Vector(43, 57, 54), new Vector(1.0, -38.0, -24.0), 14, DepthsRoom.RoomDirection.EVEN));
			mEliteRooms.add(new DepthsRoom("depths/f3r21", DepthsRoomType.ABILITY, new Vector(66, 71, 67), new Vector(1.0, -34.0, -12.0), 23, DepthsRoom.RoomDirection.UP));
			mEliteRooms.add(new DepthsRoom("depths/f3r22", DepthsRoomType.ABILITY, new Vector(51, 43, 65), new Vector(1.0, -10.0, -8.0), 25, DepthsRoom.RoomDirection.UP));
			mEliteRooms.add(new DepthsRoom("depths/f3r23", DepthsRoomType.ABILITY, new Vector(64, 48, 51), new Vector(1.0, -9.0, -14.0), 22, DepthsRoom.RoomDirection.DOWN));
			mEliteRooms.add(new DepthsRoom("depths/f3r24", DepthsRoomType.ABILITY, new Vector(52, 36, 55), new Vector(1.0, -22.0, -30.0), 22, DepthsRoom.RoomDirection.DOWN));
			mEliteRooms.add(new DepthsRoom("depths/f3r28", DepthsRoomType.ABILITY, new Vector(45, 44, 37), new Vector(1.0, -24.0, -18.0), 15, DepthsRoom.RoomDirection.DOWN));
			mEliteRooms.add(new DepthsRoom("depths/f3r29", DepthsRoomType.ABILITY, new Vector(61, 53, 60), new Vector(1.0, -8.0, -46.0), 23, DepthsRoom.RoomDirection.EVEN));
			mEliteRooms.add(new DepthsRoom("depths/f3r31", DepthsRoomType.ABILITY, new Vector(53, 39, 45), new Vector(1.0, -12.0, -36.0), 18, DepthsRoom.RoomDirection.UP));
			mEliteRooms.add(new DepthsRoom("depths/f3r32", DepthsRoomType.ABILITY, new Vector(52, 51, 61), new Vector(1.0, -8.0, -37.0), 21, DepthsRoom.RoomDirection.UP));
			mEliteRooms.add(new DepthsRoom("depths/f3r42", DepthsRoomType.ABILITY, new Vector(50, 46, 63), new Vector(1.0, -29.0, -27.0), 21, DepthsRoom.RoomDirection.EVEN));
			mEliteRooms.add(new DepthsRoom("depths/f3r46", DepthsRoomType.ABILITY, new Vector(62, 41, 51), new Vector(1.0, -30.0, -37.0), 24, DepthsRoom.RoomDirection.DOWN));
			mEliteRooms.add(new DepthsRoom("depths/f3r48", DepthsRoomType.ABILITY, new Vector(61, 43, 53), new Vector(1.0, -17.0, -24.0), 26, DepthsRoom.RoomDirection.DOWN));

			//F3 utility rooms
			mUtilityRooms.add(new DepthsRoom("depths/f3r11", DepthsRoomType.UTILITY, new Vector(34, 26, 33), new Vector(1.0, -6.0, -16.0), 0, DepthsRoom.RoomDirection.EVEN));
			mUtilityRooms.add(new DepthsRoom("depths/f3r16", DepthsRoomType.UTILITY, new Vector(38, 26, 38), new Vector(1.0, -11.0, -17.0), 0, DepthsRoom.RoomDirection.EVEN));
			mUtilityRooms.add(new DepthsRoom("depths/f3r17", DepthsRoomType.UTILITY, new Vector(32, 23, 31), new Vector(1.0, -6.0, -15.0), 0, DepthsRoom.RoomDirection.EVEN));
			mUtilityRooms.add(new DepthsRoom("depths/f3r18", DepthsRoomType.UTILITY, new Vector(35, 18, 27), new Vector(1.0, -4.0, -7.0), 0, DepthsRoom.RoomDirection.EVEN));
			mUtilityRooms.add(new DepthsRoom("depths/f3r19", DepthsRoomType.UTILITY, new Vector(31, 22, 36), new Vector(1.0, -3.0, -29.0), 0, DepthsRoom.RoomDirection.EVEN));

			//F3 twisted rooms
			mTwistedRooms.add(new DepthsRoom("depths/f3r36", DepthsRoomType.TWISTED, new Vector(51, 61, 71), new Vector(1.0, -45.0, -60.0), 53, DepthsRoom.RoomDirection.EVEN));

			//Boss f3 room
			mBossRoom = new DepthsRoom("depths/f3r25", DepthsRoomType.BOSS, new Vector(63, 58, 64), new Vector(1.0, -1.0, -32.0), 0, DepthsRoom.RoomDirection.EVEN);

		}
	}

	public DarkestDepthsRoomRepository() {
		super(new Floor1(), new Floor2(), new Floor3(), new DepthsRoom("depths/f1r16", DepthsRoomType.ABILITY, new Vector(43, 32, 46), new Vector(1.0, -3.0, -23.0), 0, DepthsRoom.RoomDirection.EVEN));
	}

	@Override
	protected String getLobbyPath(int nextFloor) {
		if (nextFloor > CUSTOM_FLOOR_LOBBIES) {
			return "depths/f11lobby";
		} else {
			return "depths/f" + nextFloor + "lobby";
		}
	}
}
