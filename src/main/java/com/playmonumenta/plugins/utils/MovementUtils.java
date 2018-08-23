package com.playmonumenta.plugins.utils;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

public class MovementUtils {
	public static void KnockAway(Entity awayFromEntity, LivingEntity target, float speed) {
		Vector dir = target.getLocation().subtract(awayFromEntity.getLocation().toVector()).toVector().multiply(speed);
		dir.setY(0.5f);

		target.setVelocity(dir);
	}

	public static void PullTowards(Entity awayFromEntity, LivingEntity target, float speed) {
		Vector dir = target.getLocation().subtract(awayFromEntity.getLocation().toVector()).toVector().multiply(-speed);
		if (dir.getY() < 0) {
			dir.setY(0.5f);
		}

		target.setVelocity(dir);
	}
}
