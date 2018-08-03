package pe.project.locations.poi;

public class POIConstants {
	public enum POI {
		None(-1, null),

		Bandit1(0, "Bandit1"),
		Bandit2(1, "Bandit2"),
		Bandit3(2, "Bandit3"),
		Mine1(3, "Mine1"),
		Mine2(4, "Mine2"),
		Mine3(5, "Mine3"),
		Mine4(6, "Mine4"),
		ShrineW(7, "ShrineW"),
		ShrineF(8, "ShrineF"),
		ShrineE(9, "ShrineE"),
		ShrineA(10, "ShrineA"),
		Sink(11, "Sink"),
		TIsland(12, "TIsland"),
		Mage(13, "Mage"),
		Huts(14, "Huts"),
		Witch(15, "Witch"),
		SouthForestTown(16, "SouthForestTown"),
		LHouse(17, "LHouse"),
		House(18, "House"),
		Hawk(19, "Hawk"),
		Jaguar1(20, "Jaguar1"),
		Jaguar2(21, "Jaguar2"),
		Stick(22, "Stick"),
		VineCave(23, "VineCave"),
		WCave(24, "WCave"),
		White(25, "White"),
		Temple(26, "Temple"),
		SCenote(27, "SCenote"),
		Fountain(28, "Fountain"),
		Creeper(29, "Creeper"),
		Pass(30, "Pass"),
		Anthill(31, "Anthill"),
		SOTFFort(32, "SOTFFort"),
		HHouse(33, "HHouse"),
		Bones(34, "Bones"),
		Volcano(35, "Volcano"),
		SOTFVillage(36, "SOTFVillage"),
		Crossroad(37, "Crossroad"),
		FireCave(38, "FireCave"),
		Monastery(39, "Monastery"),
		TinyJungleRuin(40, "TinyJungleRuin"),
		Tree(41, "Tree"),
		Graveyard(42, "Graveyard"),
		LBlue(43, "LBlue"),
		LTower(44, "LTower"),
		Snake(45, "Snake"),
		FireMine(46, "FireMine"),
		Docks(47, "Docks"),
		Chasm(48, "Chasm"),
		Island(49, "Island"),
		Pond(50, "Pond"),
		Ruins(51, "Ruins"),
		MineH(52, "MineH"),
		Maw(53, "Maw"),
		MoistRuins(54, "MoistRuins"),
		JungleMine(55, "JungleMine"),
		R1Bonus(56, "R1Bonus"),
		MntnHideaway(57, "MountainHideaway"),
		MntnMine(58, "MountainMine"),
		CursedForest(59, "CursedForest"),
		Litterbox(60, "Litterbox"),
		KaulArena(61, null),
		MurkyMaze(62, "MurkyMaze"),
		Total(63, null);

		public int mValue;
		public String mScoreboard;
		private POI(int value, String scoreboard)	{
			mValue = value;
			mScoreboard = scoreboard;
		}
	}
}
