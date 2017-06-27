package pe.project.locations.safezones;

import pe.project.point.AreaBounds;
import pe.project.point.Point;

public class SafeZoneConstants {
	public enum SafeZones {
		None(-1),
		Capital(0),
		Nyr(1),
		Farr(2),
		Highwatch(3),
		
		WhiteWoolLobby(4),
		OrangeWoolLobby(5),
		MagentaWoolLobby(6),
		LightBlueWoolLobby(7),
		YellowWoolLobby(8),
		BonusWoolLobby(9),
		
		Commands(10),
		
		SiegeOfHighwatch(11),
		Ctaz(12),
		Hermy(13),

		Total(14);
		
		public int mValue;
		private SafeZones(int value)	{	this.mValue = value;	}
	}
	
	static final AreaBounds[] mSafeZoneBounds = {
			new AreaBounds(new Point(-1130, 10,-284), new Point(-498, 250, 344)),		//	Capital
			new AreaBounds(new Point(-179, 80, -166), new Point(-79, 120, 14)),			//	Nyr
			new AreaBounds(new Point(538, 70, 100), new Point(655, 150, 229)),			//	Farr
			new AreaBounds(new Point(1131, 120, -156), new Point(1221, 180, -76)),		//	Highwatch
			
			new AreaBounds(new Point(136, 53, -186), new Point(176, 83, -120)),			//	White Wool Lobby
			new AreaBounds(new Point(27, 64, 164), new Point(67, 94, 229)),				//	Orange Wool Lobby
			new AreaBounds(new Point(453, 12, 5), new Point(493, 42, 70)),				//	Magenta Wool Lobby
			new AreaBounds(new Point(770, 76, -366), new Point(810, 106, -301)),		//	Light Blue Wool Lobby
			new AreaBounds(new Point(1141, 39, 3), new Point(1181, 69, 68)),			//	Yellow Wool Lobby
			new AreaBounds(new Point(295, 10, -163), new Point(335, 40, -98)),			//	Bonus Wool Lobby
			
			new AreaBounds(new Point(-1584, 0, -1632), new Point(-1329, 255, -1377)),	//	Commands
			
			new AreaBounds(new Point(1505, 102, -178), new Point(1631, 256, -16)),		// Siege Of Highwatch
			new AreaBounds(new Point(232, 68, 294), new Point(249, 96, 318)),			// Ctaz
			new AreaBounds(new Point(-331, 86, 334), new Point(-310, 110, 355)),		// Hermy
	};
	
	public static boolean withinSafeZone(SafeZones city, Point point) {
		return mSafeZoneBounds[city.mValue].within(point);
	}
	
	public static SafeZones withinAnySafeZone(Point point) {
		for (int i = 0; i < SafeZones.Total.mValue; i++) {
			if (i != SafeZones.None.mValue && i != SafeZones.Total.mValue) {
				if (mSafeZoneBounds[i].within(point)) {
					return SafeZones.values()[i+1];
				}
			}
		}
		
		return SafeZones.None;
	}
	
	public static boolean safeZoneAppliesEffects(SafeZones safeZone) {
		if (safeZone == SafeZones.SiegeOfHighwatch
			|| safeZone == SafeZones.Ctaz
			|| safeZone == SafeZones.Hermy) {
			return false;
		}
		
		return true;
	}
}
