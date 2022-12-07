package com.playmonumenta.plugins.bosses.spells;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

public class SpellBaseSlam extends SpellBaseLeapAttack {
	/**
	 * @param plugin             Plugin
	 * @param jumpHeight         Height of the jump
	 * @param boss               Boss
	 * @param range              Range within which players may be targeted
	 * @param minRange           Minimum range for the attack to initiate
	 * @param runDistance        How far the mob runs before leaping
	 * @param cooldown           How often this spell can be cast
	 * @param velocityMultiplier Adjusts distance of the leap (multiplier of 1 usually lands around the target at a distance of 8+ blocks away)
	 * @param initiateAesthetic  Called when the attack initiates
	 * @param leapAesthetic      Called when the boss leaps
	 * @param leapingAesthetic   Called each tick at boss location during leap
	 * @param hitAction          Called when the boss intersects a player or lands
	 */
	public SpellBaseSlam(Plugin plugin, LivingEntity boss, double jumpHeight, int range, int minRange, int runDistance,
	                     int cooldown, double velocityMultiplier, AestheticAction initiateAesthetic,
	                     AestheticAction leapAesthetic, AestheticAction leapingAesthetic, HitAction hitAction) {
		super(plugin, boss, range, minRange, runDistance, cooldown,
			velocityMultiplier, initiateAesthetic, leapAesthetic,
			leapingAesthetic, hitAction,
			(velocity, bossLoc, targetLoc) -> {
				velocity.setY(jumpHeight);
				return velocity;
			},
			(unused, targetPlayer) -> {
				Vector towardsPlayer = targetPlayer.getLocation().subtract(boss.getLocation()).toVector().setY(0).normalize();
				Vector originalVelocity = boss.getVelocity();
				double scale = 0.5;
				Vector newVelocity = new Vector();
				newVelocity.setX((originalVelocity.getX() * 20 + towardsPlayer.getX() * scale) / 20);
				// Use the original mob's vertical velocity, so it doesn't somehow fall faster than gravity
				newVelocity.setY(originalVelocity.getY());
				newVelocity.setZ((originalVelocity.getZ() * 20 + towardsPlayer.getZ() * scale) / 20);
				boss.setVelocity(newVelocity);
			});
	}

	@Override
	public int castTicks() {
		return 20 * 2;
	}
}
