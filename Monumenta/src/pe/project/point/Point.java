package pe.project.point;

import org.bukkit.Location;

public class Point {
	public Point(double x, double y, double z) {
		mX = x;
		mY = y;
		mZ = z;
	}
	
	public Point(Location loc) {
		mX = loc.getX();
		mY = loc.getY();
		mZ = loc.getZ();
	}
	
	public double mX;
	public double mY;
	public double mZ;
}
