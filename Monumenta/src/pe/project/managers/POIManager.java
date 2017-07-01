package pe.project.managers;

import java.util.ArrayList;
import java.util.List;

import pe.project.Main;
import pe.project.locations.poi.PointOfInterest;
import pe.project.locations.poi.POIConstants.POI;
import pe.project.point.Point;

public class POIManager {
	Main mPlugin;
	List<PointOfInterest> mPOIs = new ArrayList<PointOfInterest>();
	
	public POIManager(Main plugin) {
		mPlugin = plugin;
		_initPOIs();
	}
	
	public void updatePOIs(int ticks) {
		for (PointOfInterest poi : mPOIs) {
			poi.update(mPlugin, ticks);
		}
	}
	
	public boolean withinPOI(POI poi, Point point) {
		if (poi != POI.None && poi != POI.Total) {
			return mPOIs.get(poi.mValue).withinPOI(point);
		}
		
		return false;
	}
	
	public POI withinAnyPOI(Point point) {
		for (PointOfInterest poi : mPOIs) {
			if (poi.withinPOI(point)) {
				return poi.getPOI();
			}
		}
		
		return POI.None;
	}
	
	public PointOfInterest withinAnyPointOfInterest(Point point) {
		for (PointOfInterest poi : mPOIs) {
			if (poi.withinPOI(point)) {
				return poi;
			}
		}
		
		return null;
	}
	
	public List<PointOfInterest> allWithinAnyPointOfInterest(Point point) {
		List<PointOfInterest> pois = new ArrayList<PointOfInterest>();
		
		for (PointOfInterest poi : mPOIs) {
			if (poi.withinPOI(point)) {
				pois.add(poi);
			}
		}
		
		return pois;
	}
	
	public void saveAllPOIs() {
		for (PointOfInterest poi : mPOIs) {
			poi.save();
		}
	}
	
	public void loadAllPOIs() {
		for (PointOfInterest poi : mPOIs) {
			poi.load();
		}
	}
	
	public void refreshPOI(String dummyPlayer, int value) {
		for (PointOfInterest poi : mPOIs) {
			if (poi.getPOI().mScoreboard.contains(dummyPlayer)) {
				poi.setTimer(value);
				poi.save();
			}
		}
	}
	
	private void _initPOIs() {
		mPOIs.add(new PointOfInterest(POI.Bandit1, "Bandit Stronghold", new Point(-888, 105, -419), 120, null));
		mPOIs.add(new PointOfInterest(POI.Bandit2, "Bandit Camp", new Point(-379, 107, -318), 90, null));
		mPOIs.add(new PointOfInterest(POI.Bandit3, "Mercenary Fort", new Point(-267, 107, -265), 90, null));
		
		mPOIs.add(new PointOfInterest(POI.Mine1, "Northern Mine", new Point(-799, 68, -415), 100, null));
		mPOIs.add(new PointOfInterest(POI.Mine2, "Eastern Mine", new Point(-299, 75, -126), 100, null));
		mPOIs.add(new PointOfInterest(POI.Mine3, "Southeastern Mine", new Point(-358, 64, 253), 90, null));
		mPOIs.add(new PointOfInterest(POI.Mine4, "Southern Mine", new Point(-527, 78, 508), 90, null));
				
		mPOIs.add(new PointOfInterest(POI.ShrineW, "Water Shrine", new Point(-263, 88, -63), 90, null));
		mPOIs.add(new PointOfInterest(POI.ShrineF, "Fire Shrine", new Point(-234, 92, 140), 100, null));
		
		mPOIs.add(new PointOfInterest(POI.ShrineE, "Earth Shrine", new Point(276, 62, 202), 120, null));
		mPOIs.add(new PointOfInterest(POI.ShrineA, "Air Shrine", new Point(466, 140, -8), 100, null));
		mPOIs.add(new PointOfInterest(POI.Sink, "Swamp Sinkhole", new Point(-669, 80, -351), 90, null));
		
		mPOIs.add(new PointOfInterest(POI.TIsland, "Waterfall Island", new Point(-667, 90, -408), 60, null));
		mPOIs.add(new PointOfInterest(POI.Mage, "Mage Tower", new Point(-550, 120, -500), 110, null));
		mPOIs.add(new PointOfInterest(POI.Huts, "Fishing Huts", new Point(-369, 93, 18), 80, null));
		
		mPOIs.add(new PointOfInterest(POI.Witch, "Witch Village", new Point(-394, 102, 436), 90, null));
		mPOIs.add(new PointOfInterest(POI.SouthForestTown, "Southern Village", new Point(-642, 97, 417), 90, null));
		mPOIs.add(new PointOfInterest(POI.LHouse, "Lighthouse", new Point(-842, 124, 464), 120, null));

		mPOIs.add(new PointOfInterest(POI.House, "Ruined Mansion", new Point(-8, 85, -185), 85, null));
		mPOIs.add(new PointOfInterest(POI.Hawk, "Hawk Village", new Point(88, 145, -125), 100, null));
		mPOIs.add(new PointOfInterest(POI.Jaguar1, "Northern Jaguar Village", new Point(312, 95, 30), 90, null));
		
		mPOIs.add(new PointOfInterest(POI.Jaguar2, "Southern Jaguar Village", new Point(52, 97, 107), 100, null));
		mPOIs.add(new PointOfInterest(POI.Stick, "Mysterious Cenote", new Point(169, 84, -70), 90, null));
		mPOIs.add(new PointOfInterest(POI.VineCave, "Overgrown Cave", new Point(164, 90, -4), 120, null));
		
		mPOIs.add(new PointOfInterest(POI.WCave, "Water Cavern", new Point(102, 48, 13), 80, null));
		mPOIs.add(new PointOfInterest(POI.White, "Tlaxan Ziggurat", new Point(214, 137, -149), 90, null));
		mPOIs.add(new PointOfInterest(POI.Temple, "Lowland Temple", new Point(480, 87, 161), 90, null));
		
		mPOIs.add(new PointOfInterest(POI.SCenote, "Southern Cenote", new Point(550, 45, 325), 90, null));
		mPOIs.add(new PointOfInterest(POI.Fountain, "Corrupted Caves", new Point(498, 61, 399), 110, null));
		mPOIs.add(new PointOfInterest(POI.Creeper, "Creeper Tower", new Point(769, 96, 320), 90, null));

		mPOIs.add(new PointOfInterest(POI.Pass, "Eastern Pass", new Point(908, 88, 325), 130, null));
		mPOIs.add(new PointOfInterest(POI.Anthill, "Anthill", new Point(794, 108, 179), 120, null));
		mPOIs.add(new PointOfInterest(POI.SOTFFort, "Verdant Fortress", new Point(973, 106, 186), 140, null));

		mPOIs.add(new PointOfInterest(POI.HHouse, "Infested House", new Point(1020, 117, 90), 70, null));
		mPOIs.add(new PointOfInterest(POI.Bones, "Cave of Bones", new Point(1005, 75, -35), 90, null));
		mPOIs.add(new PointOfInterest(POI.Volcano, "Volcano", new Point(937, 103, -147), 100, null));

		mPOIs.add(new PointOfInterest(POI.SOTFVillage, "Suspicious Village", new Point(804, 67, -83), 110, null));
		mPOIs.add(new PointOfInterest(POI.Crossroad, "Crossroad Ruins", new Point(621, 69, -95), 80, null));
		mPOIs.add(new PointOfInterest(POI.FireCave, "Fire Cave", new Point(574, 69, -248), 120, null));
		
		mPOIs.add(new PointOfInterest(POI.Monastery, "Axtan Monastery", new Point(710, 126, -257), 120, null));
		mPOIs.add(new PointOfInterest(POI.TinyJungleRuin, "Tiny Jungle Ruin", new Point(506, 77, -178), 80, null));
		mPOIs.add(new PointOfInterest(POI.Tree, "Hawk Fortress", new Point(411, 180, 114), 130, null));

		mPOIs.add(new PointOfInterest(POI.Graveyard, "Graveyard", new Point(1099, 125, -166), 120, null));
		mPOIs.add(new PointOfInterest(POI.LBlue, "Creeper Farm", new Point(789, 159, -324), 85, null));
		mPOIs.add(new PointOfInterest(POI.LTower, "Collapsing Tower", new Point(871, 169, -325), 80, null));

		mPOIs.add(new PointOfInterest(POI.Snake, "Serpent Ruins", new Point(47, 124, 168), 80, null));
		mPOIs.add(new PointOfInterest(POI.FireMine, "Abandoned Tunnels", new Point(88, 108, 185), 105, null));
		mPOIs.add(new PointOfInterest(POI.Docks, "Docks", new Point(-392, 94, 197), 110, null));

		mPOIs.add(new PointOfInterest(POI.Chasm, "Molten Chasm", new Point(788, 30, 96), 110, null));
		mPOIs.add(new PointOfInterest(POI.Island, "Corrupted Island", new Point(615, 76, 475), 100, null));
		mPOIs.add(new PointOfInterest(POI.Pond, "Hallowed Pond", new Point(439, 72, 240), 115, null));

		mPOIs.add(new PointOfInterest(POI.Ruins, "Ancient Ruin", new Point(357, 77, -60), 100, null));
		mPOIs.add(new PointOfInterest(POI.MineH, "Haunted Mine", new Point(-678, 102, 577), 115, null));
		mPOIs.add(new PointOfInterest(POI.Maw, "The Grand Maw", new Point(385, 45, 96), 100, null));

		mPOIs.add(new PointOfInterest(POI.MoistRuins, "Moist Ruins", new Point(836, 40, -228), 90, null));
		mPOIs.add(new PointOfInterest(POI.JungleMine, "Jungle Mine", new Point(558, 62, -69), 70, null));
		mPOIs.add(new PointOfInterest(POI.R1Bonus, "Cave of Secrets", new Point(430, 23, -129), 120, null));
		
		mPOIs.add(new PointOfInterest(POI.MntnHideaway, "Mountain Hideaway", new Point(613, 145, -351), 80, null));
		mPOIs.add(new PointOfInterest(POI.MntnMine, "Mountain Mine", new Point(953, 144, -362), 85, null));
	}
}
