package pe.project.locations.zone;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import pe.project.locations.zone.SpawnEffect;
import pe.project.point.Point;

public class Zone {

	protected String mZoneName;
	protected int mShardID;
	protected Point mLowerCorner;
	protected Point mUpperCorner;
	protected List<SpawnEffect> mSEList = new ArrayList<SpawnEffect>();

	public Zone(String zoneName, int shardID, Point lowerCorner, Point upperCorner, List<SpawnEffect> effectList) {
		mZoneName = zoneName;
		mShardID = shardID;
		mLowerCorner = lowerCorner;
		mUpperCorner = upperCorner;
		mSEList = effectList;
	}

	public boolean withinZone(Location loc) {
		return withinZone(new Point(loc));
	}

	public boolean withinZone(Point point) {
		return	point.mX >= mLowerCorner.mX && point.mX <= mUpperCorner.mX &&
				point.mY >= mLowerCorner.mY && point.mY <= mUpperCorner.mY &&
				point.mZ >= mLowerCorner.mZ && point.mZ <= mUpperCorner.mZ;
	}

	public int getZoneShardID() {
		return mShardID;
	}

	public String getZoneName() {
		return mZoneName;
	}

	public List<SpawnEffect> getSpawnEffects() {
		return mSEList;
	}
}
