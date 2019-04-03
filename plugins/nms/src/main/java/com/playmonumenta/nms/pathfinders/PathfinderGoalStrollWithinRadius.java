package com.playmonumenta.nms.pathfinders;

import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;

import com.playmonumenta.bossfights.utils.Utils;

import net.minecraft.server.v1_13_R2.EntityCreature;
import net.minecraft.server.v1_13_R2.PathfinderGoal;

public class PathfinderGoalStrollWithinRadius extends PathfinderGoal {
	private double speed;

	private EntityCreature entity;

	private double radius;

	private Location loc;

	private int t = 0;

	public PathfinderGoalStrollWithinRadius(EntityCreature entity, double speed, double radius, Location loc) {
		this.entity = entity;
		this.speed = speed;
		this.radius = radius;
		this.loc = loc;
	}

	@Override
	public boolean a() {
		t--;
		return t <= 0;
	}

    @Override
    public boolean b() {
        return false;
    }

	@Override
	public void c() {
		if (Utils.getNearbyPlayers(loc, 10).size() > 0) {
			ThreadLocalRandom rand = ThreadLocalRandom.current();
			t = rand.nextInt(50, 70);
			entity.getNavigation().a(loc.getX() + rand.nextDouble(-radius, radius), loc.getY(), loc.getZ() + rand.nextDouble(-radius, radius), speed);
		}
	}
}
