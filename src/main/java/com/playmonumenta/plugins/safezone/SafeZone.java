package com.playmonumenta.plugins.safezone;

import com.google.gson.JsonObject;
import com.playmonumenta.plugins.point.AreaBounds;
import com.playmonumenta.plugins.point.Point;
import com.playmonumenta.plugins.safezone.SafeZoneManager.LocationType;

public class SafeZone extends AreaBounds {
	public String mName;
	public LocationType mType;

	public SafeZone(String name, LocationType type, Point lowerCorner, Point upperCorner) {
		super(lowerCorner, upperCorner);

		mName = name;
		mType = type;
	}

	public String getName() {
		return mName;
	}

	public LocationType getType() {
		return mType;
	}

	@Override
	public String toString() {
		return "{" + mName + ", " + mType.toString() + ", " +
		       mLowerCorner.toString() + ", " + mUpperCorner.toString() + "}";
	}

	public static SafeZone fromJsonObject(JsonObject object) throws Exception {
		return new SafeZone(object.get("name").getAsString(),
							LocationType.valueOf(object.get("type").getAsString()),
							Point.fromString(object.get("pos1").getAsString()),
							Point.fromString(object.get("pos2").getAsString()));
	}
}
