package pe.project.point;

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
	
	public String getName() {
		return mName;
	}
	
	Point mLowerCorner;
	Point mUpperCorner;
	String mName;
}
