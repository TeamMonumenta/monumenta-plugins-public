package com.playmonumenta.plugins.safezone;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.point.AreaBounds;
import com.playmonumenta.plugins.point.Point;
import com.playmonumenta.plugins.safezone.SafeZoneManager.LocationType;

public class SafeZone extends AreaBounds {
	private String mName;
	private LocationType mType;
	private boolean mEquipmentDamage;

	public SafeZone(String name, LocationType type, Point lowerCorner, Point upperCorner, boolean equipmentDamage) {
		super(lowerCorner, upperCorner);

		mName = name;
		mType = type;
		mEquipmentDamage = equipmentDamage;
	}

	public String getName() {
		return mName;
	}

	public LocationType getType() {
		return mType;
	}

	public boolean getEquipmentDamage() {
		return mEquipmentDamage;
	}

	@Override
	public String toString() {
		return "{" + mName + ", " + mType.toString() + ", " +
		       mLowerCorner.toString() + ", " + mUpperCorner.toString() + "}";
	}

	public static SafeZone fromJsonObject(JsonObject object) throws Exception {
		LocationType type = LocationType.valueOf(object.get("type").getAsString());
		JsonElement element = object.get("equipmentDamage");
		boolean equipmentDamage = !(type.equals(LocationType.Capital) || type.equals(LocationType.SafeZone));
		if (element != null) {
			equipmentDamage = object.get("equipmentDamage").getAsBoolean();
		}
		return new SafeZone(object.get("name").getAsString(),
							type,
							Point.fromString(object.get("pos1").getAsString()),
							Point.fromString(object.get("pos2").getAsString()),
							equipmentDamage);
	}
}
