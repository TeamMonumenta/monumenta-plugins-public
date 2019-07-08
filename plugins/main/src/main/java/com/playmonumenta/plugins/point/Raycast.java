package com.playmonumenta.plugins.point;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Bed;
import org.bukkit.block.data.type.Snow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.utils.LocationUtils;

public class Raycast {

	private final Vector dir;

	public int iterations = 10;
	public double hitRange = 0.5;
	public double dirMultiplier = 1;
	public double distanceCheck = 1;

	public boolean targetPlayers = false;
	public boolean targetNonPlayers = true;
	public boolean throughBlocks = false;
	public boolean throughNonOccluding = false;
	public boolean noIterations = false;

	public Particle particle = null;

	public boolean precision = false;
	public int precisionTicks = 0;

	//Locations
	private final Location start;
	private Location end = null;

	public Raycast(Location p1, Location p2) {
		this.dir = LocationUtils.getDirectionTo(p2, p1);
		this.start = p1;
		this.end = p2;

		//If we're going from one point to another,
		//ignore using iteration by default.
		this.noIterations = true;
	}

	public Raycast(Location start, Vector dir, int iterations) {
		this.dir = dir;
		this.iterations = iterations;
		this.start = start;
	}

	/**
	 * Fires the raycast with the given properties
	 * @return All entities hit by the raycast
	 */
	public RaycastData shootRaycast() {
		int i = 0;
		double dist = 0;
		RaycastData data = new RaycastData();
		List<LivingEntity> entities = data.getEntities();
		if (end != null) {
			dist = start.distance(end);
		}
		while (true) {
			// safety for reaching the end of the ray as precision ends.
			if (!noIterations && !precision) {
				if (i >= iterations) {
					break;
				}
			}

			// spawn particles along the ray if desired.
			if (particle != null) {
				start.getWorld().spawnParticle(particle, start, 1, 0, 0, 0, 0.0001);
			}

			start.add(dir.clone().multiply(dirMultiplier));
			if (!data.getBlocks().contains(start.getBlock())) {
				data.getBlocks().add(start.getBlock());
			}

			if (!throughBlocks) {
				Block block = start.getBlock();

				// breakRay: determines if the ray should collide and end on this block.
				boolean breakRay = LocationUtils.collidesWithSolid(start, block);
				if (breakRay) {
					if (!throughNonOccluding) {
						break;
					} else if (start.getBlock().getType().isOccluding()) {
						break;
					}
				}

				// Much higher precision is needed when going through semi-solid blocks.
				// When the unprecise ray reaches a semisolid block without colliding, the ray will retrace
				// both the previous and the next two steps with pixel precision to verify the result.
				if (precision == false && (block.getType().isSolid()
				                           || block.getBlockData() instanceof Snow
				                           || block.getBlockData() instanceof Bed)) {
					start.subtract(dir.clone().multiply(dirMultiplier));
					precision = true;
					precisionTicks = 49;
					if (!noIterations) {
						if (iterations - i < 3) {
							precisionTicks = (iterations - i) * 16 + 1;
						}
						i = i + 3;
					}
					dirMultiplier = (1.0 / 16.0);
				}
			}

			for (Entity e : start.getWorld().getNearbyEntities(start, hitRange, hitRange, hitRange)) {
				if (e instanceof LivingEntity) {
					//  Make sure we should be targeting this entity.
					if ((targetPlayers && (e instanceof Player)) || (targetNonPlayers && (!(e instanceof Player)))) {
						if (!entities.contains(e)) {
							data.getEntities().add((LivingEntity)e);
						}
					}
				}
			}

			if (end != null) {
				if (start.distance(end) < distanceCheck) {
					break;
				}

				//This is to prevent an infinite loop should somehow
				//the raycast goes further past the end point.
				if (start.distance(end) > dist) {
					break;
				} else {
					dist = start.distance(end);
				}
			}

			if (!noIterations && !precision) {
				i++;
				if (i >= iterations) {
					break;
				}
			}
			if (precision) {
				precisionTicks--;
				if (precisionTicks <= 0) {
					dirMultiplier = 1.0;
					precision = false;
				}
			}
		}

		return data;
	}
}
