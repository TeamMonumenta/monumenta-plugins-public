package pe.project.point;

public class AreaBounds {
	public AreaBounds(Point lowerCorner, Point upperCorner) {
		mLowerCorner = lowerCorner;
		mUpperCorner = upperCorner;
	}
	
	public boolean within(Point point) {
		return	point.mX >= mLowerCorner.mX && point.mX <= mUpperCorner.mX &&
				point.mY >= mLowerCorner.mY && point.mY <= mUpperCorner.mY &&
				point.mZ >= mLowerCorner.mZ && point.mZ <= mUpperCorner.mZ;
	}
	
	Point mLowerCorner;
	Point mUpperCorner;
}
