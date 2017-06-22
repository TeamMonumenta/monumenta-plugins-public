package pe.project.locations.cities;

import pe.project.point.AreaBounds;
import pe.project.point.Point;

public class CityConstants {
	public enum Cities {
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

		Total(11);
		
		public int mValue;
		private Cities(int value)	{	this.mValue = value;	}
	}
	
	static final AreaBounds[] mCityBounds = {
			new AreaBounds(new Point(-1130, 10,-284), new Point(-498, 250, 344)),		//	Capital
			new AreaBounds(new Point(-179, 90, -166), new Point(-79, 120, 14)),			//	Nyr
			new AreaBounds(new Point(538, 70, 100), new Point(655, 150, 229)),			//	Farr
			new AreaBounds(new Point(1131, 120, -156), new Point(1221, 180, -76)),		//	Highwatch
			
			new AreaBounds(new Point(136, 53, -186), new Point(176, 83, -120)),			//	White Wool Lobby
			new AreaBounds(new Point(27, 64, 164), new Point(67, 94, 229)),				//	Orange Wool Lobby
			new AreaBounds(new Point(453, 12, 5), new Point(493, 42, 70)),				//	Magenta Wool Lobby
			new AreaBounds(new Point(770, 76, -366), new Point(810, 106, -301)),		//	Light Blue Wool Lobby
			new AreaBounds(new Point(1141, 39, 3), new Point(1181, 69, 68)),			//	Yellow Wool Lobby
			new AreaBounds(new Point(295, 10, -163), new Point(335, 40, -98)),			//	Bonus Wool Lobby
			
			new AreaBounds(new Point(-1584, 0, -1632), new Point(-1329, 255, -1377)),	//	Commands
	};
	
	public static boolean withinCity(Cities city, Point point) {
		return mCityBounds[city.mValue].within(point);
	}
	
	public static Cities withinSafeZone(Point point) {
		for (int i = 0; i < Cities.Total.mValue; i++) {
			if (i != Cities.None.mValue && i != Cities.Total.mValue) {
				if (mCityBounds[i].within(point)) {
					return Cities.values()[i+1];
				}
			}
		}
		
		return Cities.None;
	}
}
