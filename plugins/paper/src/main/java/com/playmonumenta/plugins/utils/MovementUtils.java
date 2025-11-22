package com.playmonumenta.plugins.utils;

import com.playmonumenta.plugins.bosses.BossManager;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

public class MovementUtils {
	public static void knockAway(Entity awayFromEntity, LivingEntity target, float speed) {
		knockAway(awayFromEntity.getLocation(), target, speed, 0.5f);
	}

	public static void knockAway(Entity awayFromEntity, LivingEntity target, float speed, float y) {
		knockAway(awayFromEntity.getLocation(), target, speed, y);
	}

	public static void knockAway(Entity awayFromEntity, LivingEntity target, float speed, boolean useKnockbackRes) {
		knockAway(awayFromEntity.getLocation(), target, speed, 0.5f, useKnockbackRes);
	}

	public static void knockAway(Entity awayFromEntity, LivingEntity target, float speed, float y, boolean useKnockbackRes) {
		knockAway(awayFromEntity.getLocation(), target, speed, y, useKnockbackRes);
	}

	public static void knockAway(Location loc, LivingEntity target, float speed) {
		knockAway(loc, target, speed, 0.5f);
	}

	public static void knockAway(Location loc, LivingEntity target, float speed, float y) {
		knockAway(loc, target, speed, y, true);
	}

	public static void knockAway(Location loc, LivingEntity target, float speed, boolean useKnockbackRes) {
		knockAway(loc, target, speed, 0.5f, useKnockbackRes);
	}

	//useKnockbackRes determines if max knockback resistance should be taken into account in the knock away
	//If true, knockback res is factored, if false, not used at all
	public static void knockAway(Location loc, LivingEntity target, float speed, float y, boolean useKnockbackRes) {
		knockAway(loc, target, speed, y, useKnockbackRes, false);
	}

	//ignoreYComponent is for cases like Knockback and Punch where we only want the horizontal component of the direction of launching.
	public static void knockAway(Location loc, LivingEntity target, float speed, float y, boolean useKnockbackRes, boolean ignoreYComponentOfLocation) {
		if (EntityUtils.isBoss(target)) {
			speed /= 2;
		}
		BossManager.getInstance().entityKnockedAway(target, speed);
		Vector dir = target.getLocation().subtract(loc.toVector()).toVector();
		if (ignoreYComponentOfLocation) {
			dir.setY(0);
		}

		if (dir.length() < 0.001) {
			dir = new Vector(0, 0, 0);
		} else {
			dir = dir.normalize().multiply(speed);
		}

		dir.setY(y);
		double mult = 1 - EntityUtils.getAttributeOrDefault(target, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 0);
		if (mult > 0 || !useKnockbackRes) {
			if (!useKnockbackRes) {
				mult = 1;
			}
			dir.multiply(mult);

			target.setVelocity(dir);
		}
	}

	/**
	 * Knocks an entity in a direction.
	 *
	 * @param dir      Vector to knock entity with speed of (entity will move at this speed, neglecting initial velocity)
	 * @param target   Entity that is being knocked back
	 * @param transfer Coefficient of how much initial velocity carries over
	 */
	public static void knockAwayDirection(Vector dir, LivingEntity target, float transfer) {
		knockAwayDirection(dir, target, transfer, true, true);
	}

	/**
	 * Knocks an entity in a direction.
	 *
	 * @param dir                 Vector to knock entity with speed of (entity will move at this speed, neglecting initial velocity)
	 * @param target              Entity that is being knocked back
	 * @param transfer            Coefficient of how much initial velocity carries over
	 * @param useKnockbackRes     Whether to use knockback resistance
	 * @param useBossKnockbackRes Whether to halve KB on bosses
	 */
	public static void knockAwayDirection(Vector dir, LivingEntity target, float transfer, boolean useKnockbackRes, boolean useBossKnockbackRes) {
		if (useBossKnockbackRes && EntityUtils.isBoss(target)) {
			dir.multiply(0.5);
		}
		BossManager.getInstance().entityKnockedAway(target, (float) dir.length());
		Vector initVel = target.getVelocity();

		if (dir.length() < 0.001) {
			dir = new Vector(0, 0, 0);
		}

		double mult = 1 - EntityUtils.getAttributeOrDefault(target, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 0);
		if (mult > 0 || !useKnockbackRes) {
			if (!useKnockbackRes) {
				mult = 1;
			}
			dir.multiply(mult);
			dir.add(initVel.multiply(transfer));
			target.setVelocity(dir);
		}
	}

	public static void knockAwayRealistic(Location loc, LivingEntity target, float speed, float y, boolean useKnockbackRes) {
		if (speed == 0 && y == 0) {
			return;
		}
		if (EntityUtils.isBoss(target)) {
			speed /= 2;
		}
		BossManager.getInstance().entityKnockedAway(target, speed);
		Vector dir = target.getLocation().subtract(loc.toVector()).toVector();
		dir = dir.multiply(speed / Math.pow(Math.max(1, dir.length()), 2));
		dir.setY(Math.max(0.5, Math.min(2.5, dir.getY())));
		if (y != 0) {
			dir.setY(y);
		}
		double mult = useKnockbackRes ? 1 - EntityUtils.getAttributeOrDefault(target, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 0) : 1;
		if (mult > 0) {
			dir.multiply(mult);

			target.setVelocity(dir);
		}
	}

	public static void pullTowards(Entity towardsEntity, LivingEntity target, float speed) {
		pullTowards(towardsEntity.getLocation(), target, speed);
	}

	public static void pullTowards(Location location, LivingEntity target, float speed) {
		if (EntityUtils.isBoss(target)) {
			return;
		}
		BossManager.getInstance().entityKnockedAway(target, speed);
		Vector dir = target.getLocation().subtract(location.toVector()).toVector().multiply(-speed);
		if (dir.getY() < 0 && target.isOnGround()) {
			dir.setY(0.5f);
		}
		double mult = 1 - EntityUtils.getAttributeOrDefault(target, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 0);
		if (mult > 0) {
			dir.multiply(mult);

			target.setVelocity(dir);
		}
	}

	// Normalized Pull Velocity (Shouldn't increase based on distance)
	public static void pullTowardsNormalized(Location location, LivingEntity target, float speed) {
		pullTowardsNormalized(location, target, speed, true);
	}

	// Normalized Pull Velocity (Shouldn't increase based on distance)
	public static void pullTowardsNormalized(Location location, LivingEntity target, float speed, boolean useKnockbackRes) {
		if (EntityUtils.isBoss(target)) {
			return;
		}
		BossManager.getInstance().entityKnockedAway(target, speed);
		Vector dir = target.getLocation().subtract(location.toVector()).toVector().normalize().multiply(-speed);

		double mult = useKnockbackRes ? 1 - EntityUtils.getAttributeOrDefault(target, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 0) : 1;
		if (mult > 0) {
			dir.multiply(mult);

			target.setVelocity(dir);
		}
	}

	public static void pullTowardsByUnit(Entity towardsEntity, LivingEntity target, float speed) {
		if (EntityUtils.isBoss(target) || !target.getLocation().getWorld().equals(towardsEntity.getLocation().getWorld()) || target.getLocation().distance(towardsEntity.getLocation()) < 0.01) {
			// Don't pull if target is a boss, in a different world, or if they're already on top of each other
			return;
		}
		BossManager.getInstance().entityKnockedAway(target, speed);
		Vector dir = target.getLocation().subtract(towardsEntity.getLocation()).toVector();
		if (dir.length() < 0.001) {
			// Avoid dividing by 0
			return;
		}
		dir = dir.normalize().multiply(-speed);
		if (dir.getY() < 0 && target.isOnGround()) {
			dir.setY(0.5f);
		}
		double mult = 1 - EntityUtils.getAttributeOrDefault(target, Attribute.GENERIC_KNOCKBACK_RESISTANCE, 0);
		if (mult > 0) {
			dir.multiply(mult);
			target.setVelocity(dir);
		}
	}

	public static void pullTowardsStop(Entity towardsEntity, LivingEntity target) {
		if (EntityUtils.isBoss(target)) {
			return;
		}
		BossManager.getInstance().entityKnockedAway(target, 0);
		target.setVelocity(new Vector(0, 0, 0));
		Vector dir = towardsEntity.getLocation().subtract(target.getLocation()).toVector();
		dir.multiply(0.125f);
		dir.setY(0.5f);
		target.setVelocity(dir);
	}
}
