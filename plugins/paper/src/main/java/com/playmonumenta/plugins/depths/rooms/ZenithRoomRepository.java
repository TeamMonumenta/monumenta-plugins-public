package com.playmonumenta.plugins.depths.rooms;

import org.bukkit.util.Vector;

public class ZenithRoomRepository extends RoomRepository {
	private static class Floor1 extends RepositoryFloor {
		public Floor1() {
			//F1 utility rooms
			mUtilityRooms.add(new DepthsRoom("depths2/f1r16", DepthsRoomType.UTILITY, new Vector(34, 26, 33), new Vector(1.0, -6.0, -16.0), 0, DepthsRoom.RoomDirection.EVEN));
			mUtilityRooms.add(new DepthsRoom("depths2/f1r17", DepthsRoomType.UTILITY, new Vector(32, 23, 31), new Vector(1.0, -6.0, -15.0), 0, DepthsRoom.RoomDirection.EVEN));
			mUtilityRooms.add(new DepthsRoom("depths2/f1r18", DepthsRoomType.UTILITY, new Vector(28, 21, 27), new Vector(1.0, -3.0, -13.0), 0, DepthsRoom.RoomDirection.EVEN));
			mRemoveRoom = new DepthsRoom("depths2/f1r19", DepthsRoomType.UTILITY, new Vector(36, 24, 31), new Vector(1.0, -3.0, -15.0), 0, DepthsRoom.RoomDirection.EVEN);
			mUtilityRooms.add(mRemoveRoom);
			mUtilityRooms.add(new DepthsRoom("depths2/f1r22", DepthsRoomType.UTILITY, new Vector(34, 40, 61), new Vector(1.0, -6.0, -30.0), 0, DepthsRoom.RoomDirection.EVEN));

			//F1 normal rooms
			mNormalRooms.add(new DepthsRoom("depths2/f1r2", DepthsRoomType.ABILITY, new Vector(56, 25, 39), new Vector(1.0, -6.0, -26.0), 9, DepthsRoom.RoomDirection.EVEN));
			mNormalRooms.add(new DepthsRoom("depths2/f1r3", DepthsRoomType.ABILITY, new Vector(55, 37, 37), new Vector(1.0, -17.0, -22.0), 10, DepthsRoom.RoomDirection.DOWN));
			mNormalRooms.add(new DepthsRoom("depths2/f1r4", DepthsRoomType.ABILITY, new Vector(41, 35, 44), new Vector(1.0, -3.0, -35.0), 10, DepthsRoom.RoomDirection.UP));
			mNormalRooms.add(new DepthsRoom("depths2/f1r5", DepthsRoomType.ABILITY, new Vector(49, 31, 43), new Vector(1.0, -19.0, -25.0), 10, DepthsRoom.RoomDirection.DOWN));
			mNormalRooms.add(new DepthsRoom("depths2/f1r9", DepthsRoomType.ABILITY, new Vector(38, 32, 42), new Vector(1.0, -12.0, -22.0), 8, DepthsRoom.RoomDirection.DOWN));
			mNormalRooms.add(new DepthsRoom("depths2/f1r10", DepthsRoomType.ABILITY, new Vector(63, 33, 41), new Vector(1.0, -19.0, -16.0), 9, DepthsRoom.RoomDirection.EVEN));
			mNormalRooms.add(new DepthsRoom("depths2/f1r12", DepthsRoomType.ABILITY, new Vector(47, 54, 60), new Vector(1.0, -18.0, -12.0), 11, DepthsRoom.RoomDirection.UP));
			mNormalRooms.add(new DepthsRoom("depths2/f1r13", DepthsRoomType.ABILITY, new Vector(61, 32, 67), new Vector(1.0, -7.0, -27.0), 12, DepthsRoom.RoomDirection.EVEN));
			mNormalRooms.add(new DepthsRoom("depths2/f1r23", DepthsRoomType.ABILITY, new Vector(72, 46, 52), new Vector(1.0, -7.0, -24.0), 12, DepthsRoom.RoomDirection.UP));
			mNormalRooms.add(new DepthsRoom("depths2/f1r24", DepthsRoomType.ABILITY, new Vector(62, 38, 52), new Vector(1.0, -4.0, -25.0), 9, DepthsRoom.RoomDirection.UP));
			mNormalRooms.add(new DepthsRoom("depths2/f1r27", DepthsRoomType.ABILITY, new Vector(55, 58, 40), new Vector(1.0, -39.0, -24.0), 11, DepthsRoom.RoomDirection.DOWN));
			mNormalRooms.add(new DepthsRoom("depths2/f1r28", DepthsRoomType.ABILITY, new Vector(53, 40, 42), new Vector(1.0, -11.0, -19.0), 8, DepthsRoom.RoomDirection.EVEN));
			mNormalRooms.add(new DepthsRoom("depths2/f1r30", DepthsRoomType.ABILITY, new Vector(41, 31, 39), new Vector(1.0, -10.0, -18.0), 8, DepthsRoom.RoomDirection.EVEN));
			mNormalRooms.add(new DepthsRoom("depths2/f1r31", DepthsRoomType.ABILITY, new Vector(50, 44, 60), new Vector(1.0, -13.0, -32.0), 10, DepthsRoom.RoomDirection.UP));
			mNormalRooms.add(new DepthsRoom("depths2/f1r32", DepthsRoomType.ABILITY, new Vector(60, 44, 57), new Vector(1.0, -25.0, -22.0), 12, DepthsRoom.RoomDirection.EVEN));
			mNormalRooms.add(new DepthsRoom("depths2/f1r33", DepthsRoomType.ABILITY, new Vector(73, 47, 72), new Vector(1.0, -11.0, -18.0), 13, DepthsRoom.RoomDirection.EVEN));
			mNormalRooms.add(new DepthsRoom("depths2/f1r34", DepthsRoomType.ABILITY, new Vector(51, 50, 43), new Vector(1.0, -21.0, -22.0), 11, DepthsRoom.RoomDirection.DOWN));
			mNormalRooms.add(new DepthsRoom("depths2/f1r36", DepthsRoomType.ABILITY, new Vector(89, 47, 53), new Vector(1.0, -13.0, -19.0), 12, DepthsRoom.RoomDirection.UP));
			mNormalRooms.add(new DepthsRoom("depths2/f1r38", DepthsRoomType.ABILITY, new Vector(42, 47, 47), new Vector(1.0, -6.0, -31.0), 10, DepthsRoom.RoomDirection.UP));
			mNormalRooms.add(new DepthsRoom("depths2/f1r40", DepthsRoomType.ABILITY, new Vector(67, 34, 79), new Vector(1.0, -8.0, -48.0), 12, DepthsRoom.RoomDirection.EVEN));

			//F1 elite rooms
			mEliteRooms.add(new DepthsRoom("depths2/f1r1", DepthsRoomType.ABILITY, new Vector(53, 39, 43), new Vector(1.0, -11.0, -10.0), 25, DepthsRoom.RoomDirection.EVEN));
			mEliteRooms.add(new DepthsRoom("depths2/f1r6", DepthsRoomType.ABILITY, new Vector(39, 35, 44), new Vector(1.0, -19.0, -27.0), 12, DepthsRoom.RoomDirection.DOWN));
			mEliteRooms.add(new DepthsRoom("depths2/f1r7", DepthsRoomType.ABILITY, new Vector(124, 55, 63), new Vector(1.0, -26.0, -26.0), 34, DepthsRoom.RoomDirection.DOWN));
			mEliteRooms.add(new DepthsRoom("depths2/f1r8", DepthsRoomType.ABILITY, new Vector(71, 31, 57), new Vector(1.0, -19.0, -46.0), 26, DepthsRoom.RoomDirection.DOWN));
			mEliteRooms.add(new DepthsRoom("depths2/f1r11", DepthsRoomType.ABILITY, new Vector(97, 20, 58), new Vector(1.0, -7.0, -13.0), 31, DepthsRoom.RoomDirection.EVEN));
			mEliteRooms.add(new DepthsRoom("depths2/f1r14", DepthsRoomType.ABILITY, new Vector(122, 45, 69), new Vector(1.0, -7.0, -38.0), 26, DepthsRoom.RoomDirection.EVEN));
			mEliteRooms.add(new DepthsRoom("depths2/f1r15", DepthsRoomType.ABILITY, new Vector(86, 35, 74), new Vector(1.0, -10.0, -37.0), 16, DepthsRoom.RoomDirection.UP));
			mEliteRooms.add(new DepthsRoom("depths2/f1r25", DepthsRoomType.ABILITY, new Vector(62, 35, 43), new Vector(1.0, -8.0, -29.0), 14, DepthsRoom.RoomDirection.EVEN));
			mEliteRooms.add(new DepthsRoom("depths2/f1r26", DepthsRoomType.ABILITY, new Vector(99, 42, 77), new Vector(1.0, -11.0, -28.0), 24, DepthsRoom.RoomDirection.DOWN));
			mEliteRooms.add(new DepthsRoom("depths2/f1r29", DepthsRoomType.ABILITY, new Vector(91, 48, 47), new Vector(1.0, -5.0, -13.0), 25, DepthsRoom.RoomDirection.UP));
			mEliteRooms.add(new DepthsRoom("depths2/f1r35", DepthsRoomType.ABILITY, new Vector(49, 48, 58), new Vector(1.0, -10.0, -12.0), 21, DepthsRoom.RoomDirection.UP));
			mEliteRooms.add(new DepthsRoom("depths2/f1r37", DepthsRoomType.ABILITY, new Vector(59, 27, 61), new Vector(1.0, -9.0, -16.0), 21, DepthsRoom.RoomDirection.UP));
			mEliteRooms.add(new DepthsRoom("depths2/f1r39", DepthsRoomType.ABILITY, new Vector(60, 45, 41), new Vector(1.0, -26.0, -20.0), 25, DepthsRoom.RoomDirection.DOWN));
			mEliteRooms.add(new DepthsRoom("depths2/f1r41", DepthsRoomType.ABILITY, new Vector(63, 49, 60), new Vector(1.0, -14.0, -34.0), 21, DepthsRoom.RoomDirection.EVEN));
			mEliteRooms.add(new DepthsRoom("depths2/f1r42", DepthsRoomType.ABILITY, new Vector(62, 70, 53), new Vector(1.0, -2.0, -26.0), 22, DepthsRoom.RoomDirection.UP));
			mEliteRooms.add(new DepthsRoom("depths2/f1r43", DepthsRoomType.ABILITY, new Vector(65, 31, 71), new Vector(1.0, -12.0, -52.0), 28, DepthsRoom.RoomDirection.EVEN));
			mEliteRooms.add(new DepthsRoom("depths2/f1r44", DepthsRoomType.ABILITY, new Vector(66, 68, 69), new Vector(1.0, -51.0, -37.0), 31, DepthsRoom.RoomDirection.DOWN));

			//Boss f1 room
			mBossRoom = new DepthsRoom("depths2/f1r21", DepthsRoomType.BOSS, new Vector(51, 46, 56), new Vector(1.0, -16.0, -15.0), 0, DepthsRoom.RoomDirection.EVEN);

		}
	}

	private static class Floor2 extends RepositoryFloor {
		public Floor2() {
			//F2 utility rooms
			mUtilityRooms.add(new DepthsRoom("depths2/f2r12", DepthsRoomType.UTILITY, new Vector(34, 26, 33), new Vector(1.0, -6.0, -16.0), 0, DepthsRoom.RoomDirection.EVEN));
			mUtilityRooms.add(new DepthsRoom("depths2/f2r13", DepthsRoomType.UTILITY, new Vector(32, 23, 31), new Vector(1.0, -6.0, -15.0), 0, DepthsRoom.RoomDirection.EVEN));
			mUtilityRooms.add(new DepthsRoom("depths2/f2r14", DepthsRoomType.UTILITY, new Vector(28, 21, 27), new Vector(1.0, -3.0, -13.0), 0, DepthsRoom.RoomDirection.EVEN));
			mRemoveRoom = new DepthsRoom("depths2/f2r15", DepthsRoomType.UTILITY, new Vector(36, 24, 31), new Vector(1.0, -3.0, -15.0), 0, DepthsRoom.RoomDirection.EVEN);
			mUtilityRooms.add(mRemoveRoom);
			mUtilityRooms.add(new DepthsRoom("depths2/f2r19", DepthsRoomType.UTILITY, new Vector(34, 40, 61), new Vector(1.0, -6.0, -30.0), 0, DepthsRoom.RoomDirection.EVEN));

			//F2 normal rooms
			mNormalRooms.add(new DepthsRoom("depths2/f2r1", DepthsRoomType.ABILITY, new Vector(40, 27, 30), new Vector(1.0, -8.0, -14.0), 8, DepthsRoom.RoomDirection.DOWN));
			mNormalRooms.add(new DepthsRoom("depths2/f2r2", DepthsRoomType.ABILITY, new Vector(41, 24, 39), new Vector(1.0, -7.0, -10.0), 8, DepthsRoom.RoomDirection.EVEN));
			mNormalRooms.add(new DepthsRoom("depths2/f2r3", DepthsRoomType.ABILITY, new Vector(37, 18, 32), new Vector(1.0, -3.0, -5.0), 10, DepthsRoom.RoomDirection.UP));
			mNormalRooms.add(new DepthsRoom("depths2/f2r4", DepthsRoomType.ABILITY, new Vector(49, 26, 41), new Vector(1.0, -9.0, -28.0), 10, DepthsRoom.RoomDirection.DOWN));
			mNormalRooms.add(new DepthsRoom("depths2/f2r11", DepthsRoomType.ABILITY, new Vector(38, 29, 40), new Vector(1.0, -3.0, -24.0), 15, DepthsRoom.RoomDirection.UP));
			mNormalRooms.add(new DepthsRoom("depths2/f2r16", DepthsRoomType.ABILITY, new Vector(47, 28, 37), new Vector(1.0, -14.0, -18.0), 13, DepthsRoom.RoomDirection.DOWN));
			mNormalRooms.add(new DepthsRoom("depths2/f2r17", DepthsRoomType.ABILITY, new Vector(37, 33, 49), new Vector(1.0, -16.0, -29.0), 11, DepthsRoom.RoomDirection.DOWN));
			mNormalRooms.add(new DepthsRoom("depths2/f2r21", DepthsRoomType.ABILITY, new Vector(38, 31, 29), new Vector(1.0, -20.0, -12.0), 4, DepthsRoom.RoomDirection.DOWN));
			mNormalRooms.add(new DepthsRoom("depths2/f2r25", DepthsRoomType.ABILITY, new Vector(45, 69, 52), new Vector(1.0, -1.0, -26.0), 15, DepthsRoom.RoomDirection.UP));
			mNormalRooms.add(new DepthsRoom("depths2/f2r26", DepthsRoomType.ABILITY, new Vector(50, 47, 48), new Vector(1.0, -14.0, -41.0), 16, DepthsRoom.RoomDirection.UP));
			mNormalRooms.add(new DepthsRoom("depths2/f2r27", DepthsRoomType.ABILITY, new Vector(45, 40, 49), new Vector(1.0, -23.0, -26.0), 14, DepthsRoom.RoomDirection.DOWN));
			mNormalRooms.add(new DepthsRoom("depths2/f2r30", DepthsRoomType.ABILITY, new Vector(67, 29, 36), new Vector(1.0, -10.0, -19.0), 16, DepthsRoom.RoomDirection.DOWN));
			mNormalRooms.add(new DepthsRoom("depths2/f2r34", DepthsRoomType.ABILITY, new Vector(69, 48, 75), new Vector(1.0, -8.0, -37.0), 15, DepthsRoom.RoomDirection.UP));

			//F2 elite rooms
			mEliteRooms.add(new DepthsRoom("depths2/f2r5", DepthsRoomType.ABILITY, new Vector(56, 21, 66), new Vector(1.0, -3.0, -54.0), 24, DepthsRoom.RoomDirection.EVEN));
			mEliteRooms.add(new DepthsRoom("depths2/f2r6", DepthsRoomType.ABILITY, new Vector(52, 55, 92), new Vector(1.0, -5.0, -75.0), 30, DepthsRoom.RoomDirection.EVEN));
			mEliteRooms.add(new DepthsRoom("depths2/f2r7", DepthsRoomType.ABILITY, new Vector(73, 58, 71), new Vector(1.0, -43.0, -28.0), 39, DepthsRoom.RoomDirection.DOWN));
			mEliteRooms.add(new DepthsRoom("depths2/f2r8", DepthsRoomType.ABILITY, new Vector(90, 43, 66), new Vector(1.0, -13.0, -38.0), 30, DepthsRoom.RoomDirection.UP));
			mEliteRooms.add(new DepthsRoom("depths2/f2r9", DepthsRoomType.ABILITY, new Vector(57, 57, 62), new Vector(1.0, -3.0, -21.0), 37, DepthsRoom.RoomDirection.UP));
			mEliteRooms.add(new DepthsRoom("depths2/f2r10", DepthsRoomType.ABILITY, new Vector(56, 48, 34), new Vector(1.0, -3.0, -17.0), 16, DepthsRoom.RoomDirection.UP));
			mEliteRooms.add(new DepthsRoom("depths2/f2r20", DepthsRoomType.ABILITY, new Vector(71, 43, 74), new Vector(1.0, -29.0, -41.0), 26, DepthsRoom.RoomDirection.DOWN));
			mEliteRooms.add(new DepthsRoom("depths2/f2r22", DepthsRoomType.ABILITY, new Vector(80, 60, 129), new Vector(1.0, -45.0, -105.0), 33, DepthsRoom.RoomDirection.DOWN));
			mEliteRooms.add(new DepthsRoom("depths2/f2r23", DepthsRoomType.ABILITY, new Vector(107, 65, 80), new Vector(1.0, -48.0, -45.0), 34, DepthsRoom.RoomDirection.DOWN));
			mEliteRooms.add(new DepthsRoom("depths2/f2r24", DepthsRoomType.ABILITY, new Vector(37, 36, 49), new Vector(1.0, -13.0, -15.0), 15, DepthsRoom.RoomDirection.UP));
			mEliteRooms.add(new DepthsRoom("depths2/f2r28", DepthsRoomType.ABILITY, new Vector(81, 47, 43), new Vector(1.0, -17.0, -23.0), 22, DepthsRoom.RoomDirection.UP));
			mEliteRooms.add(new DepthsRoom("depths2/f2r29", DepthsRoomType.ABILITY, new Vector(95, 76, 140), new Vector(1.0, -34.0, -42.0), 25, DepthsRoom.RoomDirection.DOWN));
			mEliteRooms.add(new DepthsRoom("depths2/f2r31", DepthsRoomType.ABILITY, new Vector(73, 71, 72), new Vector(1.0, -4.0, -36.0), 23, DepthsRoom.RoomDirection.UP));
			mEliteRooms.add(new DepthsRoom("depths2/f2r32", DepthsRoomType.ABILITY, new Vector(53, 90, 60), new Vector(1.0, -58.0, -32.0), 24, DepthsRoom.RoomDirection.DOWN));
			mEliteRooms.add(new DepthsRoom("depths2/f2r33", DepthsRoomType.ABILITY, new Vector(67, 41, 67), new Vector(1.0, -18.0, -32.0), 27, DepthsRoom.RoomDirection.DOWN));
			mEliteRooms.add(new DepthsRoom("depths2/f2r35", DepthsRoomType.ABILITY, new Vector(61, 62, 63), new Vector(1.0, -29.0, -19.0), 18, DepthsRoom.RoomDirection.DOWN));

			//Boss f2 room
			mBossRoom = new DepthsRoom("depths2/f2r18", DepthsRoomType.BOSS, new Vector(148, 48, 71), new Vector(1.0, -8.0, -33.0), 0, DepthsRoom.RoomDirection.EVEN);

		}
	}

	private static class Floor3 extends RepositoryFloor {
		public Floor3() {
			//F3 utility rooms
			mUtilityRooms.add(new DepthsRoom("depths2/f3r11", DepthsRoomType.UTILITY, new Vector(34, 26, 33), new Vector(1.0, -6.0, -16.0), 0, DepthsRoom.RoomDirection.EVEN));
			mUtilityRooms.add(new DepthsRoom("depths2/f3r12", DepthsRoomType.UTILITY, new Vector(32, 23, 31), new Vector(1.0, -6.0, -15.0), 0, DepthsRoom.RoomDirection.EVEN));
			mUtilityRooms.add(new DepthsRoom("depths2/f3r13", DepthsRoomType.UTILITY, new Vector(28, 21, 27), new Vector(1.0, -3.0, -13.0), 0, DepthsRoom.RoomDirection.EVEN));
			mRemoveRoom = new DepthsRoom("depths2/f3r14", DepthsRoomType.UTILITY, new Vector(36, 24, 31), new Vector(1.0, -3.0, -15.0), 0, DepthsRoom.RoomDirection.EVEN);
			mUtilityRooms.add(mRemoveRoom);
			mUtilityRooms.add(new DepthsRoom("depths2/f3r19", DepthsRoomType.UTILITY, new Vector(34, 40, 61), new Vector(1.0, -6.0, -30.0), 0, DepthsRoom.RoomDirection.EVEN));

			//F3 normal rooms
			mNormalRooms.add(new DepthsRoom("depths2/f3r1", DepthsRoomType.ABILITY, new Vector(52, 27, 40), new Vector(1.0, -7.0, -27.0), 10, DepthsRoom.RoomDirection.EVEN));
			mNormalRooms.add(new DepthsRoom("depths2/f3r3", DepthsRoomType.ABILITY, new Vector(42, 51, 43), new Vector(1.0, -18.0, -11.0), 12, DepthsRoom.RoomDirection.DOWN));
			mNormalRooms.add(new DepthsRoom("depths2/f3r5", DepthsRoomType.ABILITY, new Vector(51, 50, 54), new Vector(1.0, -23.0, -26.0), 14, DepthsRoom.RoomDirection.DOWN));
			mNormalRooms.add(new DepthsRoom("depths2/f3r6", DepthsRoomType.ABILITY, new Vector(44, 15, 37), new Vector(1.0, -1.0, -10.0), 10, DepthsRoom.RoomDirection.EVEN));
			mNormalRooms.add(new DepthsRoom("depths2/f3r8", DepthsRoomType.ABILITY, new Vector(76, 62, 75), new Vector(1.0, -39.0, -37.0), 17, DepthsRoom.RoomDirection.EVEN));
			mNormalRooms.add(new DepthsRoom("depths2/f3r24", DepthsRoomType.ABILITY, new Vector(85, 82, 74), new Vector(1.0, -16.0, -41.0), 15, DepthsRoom.RoomDirection.UP));
			mNormalRooms.add(new DepthsRoom("depths2/f3r26", DepthsRoomType.ABILITY, new Vector(81, 60, 82), new Vector(1.0, -17.0, -48.0), 15, DepthsRoom.RoomDirection.EVEN));
			mNormalRooms.add(new DepthsRoom("depths2/f3r27", DepthsRoomType.ABILITY, new Vector(36, 90, 35), new Vector(1.0, -61.0, -17.0), 14, DepthsRoom.RoomDirection.DOWN));
			mNormalRooms.add(new DepthsRoom("depths2/f3r28", DepthsRoomType.ABILITY, new Vector(84, 108, 89), new Vector(1.0, -91.0, -44.0), 17, DepthsRoom.RoomDirection.DOWN));
			mNormalRooms.add(new DepthsRoom("depths2/f3r29", DepthsRoomType.ABILITY, new Vector(73, 62, 68), new Vector(1.0, -19.0, -34.0), 13, DepthsRoom.RoomDirection.EVEN));
			mNormalRooms.add(new DepthsRoom("depths2/f3r30", DepthsRoomType.ABILITY, new Vector(70, 69, 71), new Vector(1.0, -21.0, -33.0), 18, DepthsRoom.RoomDirection.UP));
			mNormalRooms.add(new DepthsRoom("depths2/f3r31", DepthsRoomType.ABILITY, new Vector(53, 65, 59), new Vector(1.0, -11.0, -33.0), 13, DepthsRoom.RoomDirection.UP));
			mNormalRooms.add(new DepthsRoom("depths2/f3r32", DepthsRoomType.ABILITY, new Vector(70, 65, 71), new Vector(1.0, -17.0, -33.0), 20, DepthsRoom.RoomDirection.EVEN));
			mNormalRooms.add(new DepthsRoom("depths2/f3r35", DepthsRoomType.ABILITY, new Vector(48, 45, 43), new Vector(1.0, -23.0, -22.0), 5, DepthsRoom.RoomDirection.DOWN));
			mNormalRooms.add(new DepthsRoom("depths2/f3r36", DepthsRoomType.ABILITY, new Vector(79, 64, 57), new Vector(1.0, -33.0, -39.0), 10, DepthsRoom.RoomDirection.DOWN));
			mNormalRooms.add(new DepthsRoom("depths2/f3r37", DepthsRoomType.ABILITY, new Vector(34, 88, 63), new Vector(1.0, -12.0, -29.0), 16, DepthsRoom.RoomDirection.UP));
			mNormalRooms.add(new DepthsRoom("depths2/f3r39", DepthsRoomType.ABILITY, new Vector(64, 62, 74), new Vector(1.0, -6.0, -51.0), 18, DepthsRoom.RoomDirection.UP));
			mNormalRooms.add(new DepthsRoom("depths2/f3r40", DepthsRoomType.ABILITY, new Vector(43, 40, 68), new Vector(1.0, -4.0, -44.0), 18, DepthsRoom.RoomDirection.DOWN));

			//F3 elite rooms
			mEliteRooms.add(new DepthsRoom("depths2/f3r2", DepthsRoomType.ABILITY, new Vector(54, 42, 45), new Vector(1.0, -2.0, -22.0), 19, DepthsRoom.RoomDirection.UP));
			mEliteRooms.add(new DepthsRoom("depths2/f3r4", DepthsRoomType.ABILITY, new Vector(65, 50, 62), new Vector(1.0, -33.0, -39.0), 57, DepthsRoom.RoomDirection.DOWN));
			mEliteRooms.add(new DepthsRoom("depths2/f3r7", DepthsRoomType.ABILITY, new Vector(146, 53, 112), new Vector(1.0, -5.0, -85.0), 34, DepthsRoom.RoomDirection.EVEN));
			mEliteRooms.add(new DepthsRoom("depths2/f3r9", DepthsRoomType.ABILITY, new Vector(116, 56, 79), new Vector(1.0, -24.0, -13.0), 21, DepthsRoom.RoomDirection.EVEN));
			mEliteRooms.add(new DepthsRoom("depths2/f3r10", DepthsRoomType.ABILITY, new Vector(85, 62, 96), new Vector(1.0, -13.0, -47.0), 26, DepthsRoom.RoomDirection.UP));
			mEliteRooms.add(new DepthsRoom("depths2/f3r15", DepthsRoomType.ABILITY, new Vector(86, 66, 71), new Vector(1.0, -6.0, -58.0), 31, DepthsRoom.RoomDirection.UP));
			mEliteRooms.add(new DepthsRoom("depths2/f3r17", DepthsRoomType.ABILITY, new Vector(115, 46, 73), new Vector(1.0, -7.0, -36.0), 24, DepthsRoom.RoomDirection.UP));
			mEliteRooms.add(new DepthsRoom("depths2/f3r20", DepthsRoomType.ABILITY, new Vector(77, 81, 83), new Vector(1.0, -19.0, -66.0), 33, DepthsRoom.RoomDirection.UP));
			mEliteRooms.add(new DepthsRoom("depths2/f3r21", DepthsRoomType.ABILITY, new Vector(97, 88, 94), new Vector(1.0, -11.0, -50.0), 35, DepthsRoom.RoomDirection.UP));
			mEliteRooms.add(new DepthsRoom("depths2/f3r22", DepthsRoomType.ABILITY, new Vector(147, 86, 170), new Vector(1.0, -27.0, -28.0), 25, DepthsRoom.RoomDirection.EVEN));
			mEliteRooms.add(new DepthsRoom("depths2/f3r25", DepthsRoomType.ABILITY, new Vector(131, 90, 110), new Vector(1.0, -30.0, -87.0), 30, DepthsRoom.RoomDirection.EVEN));
			mEliteRooms.add(new DepthsRoom("depths2/f3r33", DepthsRoomType.ABILITY, new Vector(94, 77, 139), new Vector(1.0, -58.0, -59.0), 33, DepthsRoom.RoomDirection.DOWN));
			mEliteRooms.add(new DepthsRoom("depths2/f3r34", DepthsRoomType.ABILITY, new Vector(76, 88, 65), new Vector(1.0, -61.0, -23.0), 31, DepthsRoom.RoomDirection.DOWN));
			mEliteRooms.add(new DepthsRoom("depths2/f3r38", DepthsRoomType.ABILITY, new Vector(107, 60, 80), new Vector(1.0, -25.0, -10.0), 5, DepthsRoom.RoomDirection.DOWN));
			mEliteRooms.add(new DepthsRoom("depths2/f3r41", DepthsRoomType.ABILITY, new Vector(71, 70, 70), new Vector(1.0, -2.0, -35.0), 28, DepthsRoom.RoomDirection.UP));
			mEliteRooms.add(new DepthsRoom("depths2/f3r42", DepthsRoomType.ABILITY, new Vector(71, 70, 70), new Vector(1.0, -17.0, -5.0), 19, DepthsRoom.RoomDirection.UP));

			//Boss f3 room
			mBossRoom = new DepthsRoom("depths2/f3r16", DepthsRoomType.BOSS, new Vector(103, 137, 103), new Vector(1.0, -74.0, -51.0), 0, DepthsRoom.RoomDirection.EVEN);
		}
	}

	public ZenithRoomRepository() {
		super(new Floor1(), new Floor2(), new Floor3(), new DepthsRoom("depths2/f1r20", DepthsRoomType.ABILITY, new Vector(43, 38, 59), new Vector(1.0, -1.0, -27.0), 0, DepthsRoom.RoomDirection.EVEN));
	}

	@Override
	protected String getLobbyPath(int nextFloor) {
		if (nextFloor == 2) {
			return "depths2/f2lobby";
		} else if (nextFloor == 3) {
			return "depths2/f3lobby";
		} else {
			return "depths/f1lobby";
		}
	}
}
