package com.playmonumenta.plugins.utils;

import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

public class MovementUtils {
	public static void KnockAway(Entity awayFromEntity, LivingEntity target, float speed) {
		KnockAway(awayFromEntity.getLocation(), target, speed, 0.5f);
	}

	public static void KnockAway(Entity awayFromEntity, LivingEntity target, float speed, float y) {
		KnockAway(awayFromEntity.getLocation(), target, speed, y);
	}

	public static void KnockAway(Location loc, LivingEntity target, float speed) {
		KnockAway(loc, target, speed, 0.5f);
	}

	public static void KnockAway(Location loc, LivingEntity target, float speed, float y) {
		if (EntityUtils.isBoss(target)) {
			speed /= 2;
		}
		Vector dir = target.getLocation().subtract(loc.toVector()).toVector().multiply(speed);
		dir.setY(y);
		double mult = 1 - target.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).getValue();
		if (mult > 0) {
			dir.multiply(mult);

			target.setVelocity(dir);
		}
	}

	public static void knockAwayConstant(Location loc, LivingEntity target, float speed, float y) {
		if (EntityUtils.isBoss(target)) {
			speed /= 2;
		}
		Vector dir = target.getLocation().subtract(loc.toVector()).toVector().normalize().multiply(speed);
		dir.setY(y);

		double mult = 1 - target.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).getValue();
		if (mult > 0) {
			dir.multiply(mult);

			target.setVelocity(dir);
		}
	}

	public static void knockAwayRealistic(Location loc, LivingEntity target, float speed, float y) {
		if (EntityUtils.isBoss(target)) {
			speed /= 2;
		}
		Vector dir = target.getLocation().subtract(loc.toVector()).toVector();
		dir.setY(0.5 * dir.getY());
		dir = dir.multiply(speed / Math.pow(Math.max(1, dir.length()), 2));
		dir.setY(Math.max(0.5, Math.min(1, dir.getY())));
		double mult = 1 - target.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).getValue();
		if (mult > 0) {
			dir.multiply(mult);

			target.setVelocity(dir);
		}
	}

	public static void PullTowards(Entity awayFromEntity, LivingEntity target, float speed) {
		if (EntityUtils.isBoss(target)) {
			return;
		}
		Vector dir = target.getLocation().subtract(awayFromEntity.getLocation().toVector()).toVector().multiply(-speed);
		if (dir.getY() < 0) {
			dir.setY(0.5f);
		}
		double mult = 1 - target.getAttribute(Attribute.GENERIC_KNOCKBACK_RESISTANCE).getValue();
		if (mult > 0) {
			dir.multiply(mult);

			target.setVelocity(dir);
		}
	}
}
