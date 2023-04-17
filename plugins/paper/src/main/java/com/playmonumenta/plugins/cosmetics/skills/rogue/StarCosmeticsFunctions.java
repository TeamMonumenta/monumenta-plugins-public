package com.playmonumenta.plugins.cosmetics.skills.rogue;

import com.playmonumenta.plugins.utils.FastUtils;
import java.util.ArrayList;
import org.bukkit.util.Vector;

public class StarCosmeticsFunctions {

	public static Vector[] generateStarVertices(int vertices, double starSize, double starHeaviness, boolean rotateRandomly, boolean grounded) {
		Vector[] starPoints = new Vector[2 * vertices];
		double randomRotation = rotateRandomly ? FastUtils.randomDoubleInRange(0, 2 * Math.PI / vertices) : 0;

		for (int i = 0; i < vertices; i++) {
			double angle = randomRotation + 2 * i * Math.PI / vertices;
			if (grounded) {
				starPoints[2 * i] = new Vector(FastUtils.sin(angle), 0, FastUtils.cos(angle)).multiply(starSize);
				starPoints[2 * i + 1] = new Vector(FastUtils.sin(angle + Math.PI / vertices), 0, FastUtils.cos(angle + Math.PI / vertices)).multiply(starHeaviness * starSize);
			} else {
				starPoints[2 * i] = new Vector(FastUtils.sin(angle), FastUtils.cos(angle), 0).multiply(starSize);
				starPoints[2 * i + 1] = new Vector(FastUtils.sin(angle + Math.PI / vertices), FastUtils.cos(angle + Math.PI / vertices), 0).multiply(starHeaviness * starSize);
			}
		}
		return starPoints;
	}

	public static ArrayList<Vector> interpolatePolygon(Vector[] polygonVertices, int interpolationCount) {
		ArrayList<Vector> starFull = new ArrayList<>();
		float step = 1.0f / (float) (interpolationCount + 1);
		for (int starPointIterator = 0; starPointIterator < polygonVertices.length; starPointIterator++) {
			starFull.add(polygonVertices[starPointIterator]);
			for (int i = 1; i <= interpolationCount; i++) {
				float t = step * i;
				starFull.add(
					polygonVertices[starPointIterator == polygonVertices.length - 1 ? 0 : starPointIterator + 1].clone()
						.subtract(polygonVertices[starPointIterator].clone())
						.multiply(t)
						.add(polygonVertices[starPointIterator].clone())
				);
			}
		}
		return starFull;
	}
}
