package pe.project.point;

import org.bukkit.Location;

public class AreaBounds {
	public AreaBounds(String name, Point lowerCorner, Point upperCorner) {
		mName = name;
		mLowerCorner = lowerCorner;
		mUpperCorner = upperCorner;
	}
	
	public boolean within(Point point) {
		return	point.mX >= mLowerCorner.mX && point.mX <= mUpperCorner.mX &&
				point.mY >= mLowerCorner.mY && point.mY <= mUpperCorner.mY &&
				point.mZ >= mLowerCorner.mZ && point.mZ <= mUpperCorner.mZ;
	}
	
	public boolean within(Location loc) {
		return	loc.getX() >= mLowerCorner.mX && loc.getX() <= mUpperCorner.mX &&
				loc.getY() >= mLowerCorner.mY && loc.getY() <= mUpperCorner.mY &&
				loc.getZ() >= mLowerCorner.mZ && loc.getZ() <= mUpperCorner.mZ;
	}
	
	public String getName() {
		return mName;
	}
	
	Point mLowerCorner;
	Point mUpperCorner;
	String mName;
}
