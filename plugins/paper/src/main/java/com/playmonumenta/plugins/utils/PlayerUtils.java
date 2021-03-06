package com.playmonumenta.plugins.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import com.destroystokyo.paper.MaterialSetTag;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.attributes.AttributeProjectileSpeed;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.potion.PotionManager.PotionID;



public class PlayerUtils {
	public static void callAbilityCastEvent(Player player, ClassAbility spell) {
		AbilityCastEvent event = new AbilityCastEvent(player, spell);
		Bukkit.getPluginManager().callEvent(event);
	}

	public static void awardStrike(Plugin plugin, Player player, String reason) {
		int strikes = ScoreboardUtils.getScoreboardValue(player, "Strikes");
		strikes++;
		ScoreboardUtils.setScoreboardValue(player, "Strikes", strikes);

		Location loc = player.getLocation();
		String oobLoc = "[" + (int)loc.getX() + ", " + (int)loc.getY() + ", " + (int)loc.getZ() + "]";

		player.sendMessage(ChatColor.RED + "WARNING: " + reason);
		player.sendMessage(ChatColor.RED + "Location: " + oobLoc);
		player.sendMessage(ChatColor.YELLOW + "This is an automated message generated by breaking a game rule.");
		player.sendMessage(ChatColor.YELLOW + "You have been teleported to spawn and given slowness for 5 minutes.");
		player.sendMessage(ChatColor.YELLOW + "There is no further punishment, but please do follow the rules.");

		plugin.mPotionManager.addPotion(player, PotionID.APPLIED_POTION,
		                                new PotionEffect(PotionEffectType.SLOW, 5 * 20 * 60, 3, false, true));

		player.teleport(player.getWorld().getSpawnLocation());
	}

	public static List<Player> playersInRange(Location loc, double range, boolean includeNonTargetable) {
		List<Player> players = new ArrayList<Player>();

		for (Player player : Bukkit.getServer().getOnlinePlayers()) {
			if (player.getLocation().distanceSquared(loc) < range * range && player.getGameMode() != GameMode.SPECTATOR && player.getHealth() > 0) {
				if (includeNonTargetable || !AbilityUtils.isStealthed(player)) {
					players.add(player);
				}
			}
		}

		return players;
	}

	public static List<Player> otherPlayersInRange(Player player, double radius, boolean includeNonTargetable) {
		List<Player> players = playersInRange(player.getLocation(), radius, includeNonTargetable);
		players.removeIf(p -> (p == player));
		return players;
	}

	public static void healPlayer(Player player, double healAmount) {
		if (!player.isDead()) {
			EntityRegainHealthEvent event = new EntityRegainHealthEvent(player, healAmount, EntityRegainHealthEvent.RegainReason.CUSTOM);
			Bukkit.getPluginManager().callEvent(event);
			if (!event.isCancelled()) {
				double newHealth = Math.min(player.getHealth() + event.getAmount(), player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
				player.setHealth(newHealth);
			}
		}
	}

	public static Location getRightSide(Location location, double distance) {
		float angle = location.getYaw() / 60;
		return location.clone().subtract(new Vector(FastUtils.cos(angle), 0, FastUtils.sin(angle)).normalize().multiply(distance));
	}

	/* Command should use @s for targeting selector */
	private static String getExecuteCommandOnNearbyPlayers(Location loc, int radius, String command) {
		String executeCmd = "execute as @a[x=" + (int)loc.getX() +
		                    ",y=" + (int)loc.getY() +
		                    ",z=" + (int)loc.getZ() +
		                    ",distance=.." + radius + "] at @s run ";
		return executeCmd + command;
	}

	public static void executeCommandOnNearbyPlayers(Location loc, int radius, String command) {
		Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(),
		                                   getExecuteCommandOnNearbyPlayers(loc, radius, command));
	}

	// How far back the player drew their bow,
	// vs what its max launch speed would be.
	// Launch velocity used to calculate is specifically for PLAYERS shooting BOWS!
	// Returns between 0 to 1, with 1 being full draw
	public static double calculateBowDraw(@NotNull AbstractArrow arrowlike) {
		double currentSpeed = arrowlike.getVelocity().length();
		double maxLaunchSpeed = Constants.PLAYER_BOW_INITIAL_SPEED * AttributeProjectileSpeed.getProjectileSpeedModifier(arrowlike);

		return Math.min(
			1,
			currentSpeed / maxLaunchSpeed
		);
	}

	/*
	 * Whether the player meets the conditions for a critical hit,
	 * emulating the vanilla check in full (no critting while sprinting).
	 */
	public static boolean isCriticalAttack(@NotNull Player player) {
		// NMS EntityHuman:
		// float f2 = this.getAttackCooldown(0.5F);
		// boolean flag = f2 > 0.9F;
		// boolean flag2 = flag && this.fallDistance > 0.0F && !this.onGround && !this.isClimbing() && !this.isInWater() && !this.hasEffect(MobEffects.BLINDNESS) && !this.isPassenger() && entity instanceof EntityLiving;
		// flag2 = flag2 && !this.isSprinting();
		return (
			isFallingAttack(player)
			&& !player.isSprinting()
		);
	}

	/*
	 * Whether the player meets the conditions for a critical hit,
	 * emulating the vanilla check,
	 * except this does not check whether they are sprinting.
	 * This is used because MM has historically had a non-exact crit check that
	 * allowed crit-triggered abilities to trigger off non-crit melee damage
	 * while sprinting.
	 */
	public static boolean isFallingAttack(@NotNull Player player) {
		return (
			player.getCooledAttackStrength(0.5f) > 0.9
			&& player.getFallDistance() > 0
			&& isAirborne(player)
			&& !player.isInWater()
			&& !player.hasPotionEffect(PotionEffectType.BLINDNESS)
			&& !player.isInsideVehicle()
			//TODO pass in the Entity in question to check if LivingEntity
		);
	}

	// Whether player is considered to be in the air
	public static boolean isAirborne(Player player) {
		if (!player.isOnGround()) {
			Material playerFeetMaterial = player.getLocation().getBlock().getType();
			// Accounts for vines, ladders, nether vines, scaffolding etc
			if (!MaterialSetTag.CLIMBABLE.isTagged(playerFeetMaterial)) {
				return true;
			}
		}

		return false;
	}

	/*
	 * Whether the player meets the conditions for a sweep attack,
	 * emulating the vanilla check, except the sword or proximity requirements.
	 * This also does not require them to be on the ground.
	 */
	public static boolean isNonFallingAttack(
		@NotNull Player player,
		@NotNull Entity enemy
	) {
		return (
			player.getCooledAttackStrength(0.5f) > 0.9
			&& !isCriticalAttack(player)
			&& !player.isSprinting()
			// Last check on horizontal "speed" requires an internal vanilla
			// collision adjustment Vec3D,
			// it is not simply player.getVelocity() (that is used elsewhere)
		);
	}

	/*
	 * Whether the player meets the conditions for a sweep attack,
	 * emulating the vanilla check, except the sword or proximity requirements.
	 */
	public static boolean isSweepingAttack(
		@NotNull Player player,
		@NotNull Entity enemy
	) {
		// NMS Entity:
		// this.z = this.A;
		// this.A = (float)((double)this.A + (double)MathHelper.sqrt(c(vec3d1)) * 0.6D);
		// public static double c(Vec3D vec3d) {
		//     return vec3d.x * vec3d.x + vec3d.z * vec3d.z;
		// }
		//
		// NMS EntityHuman:
		// if (this.isSprinting() && flag) {
		//     flag1 = true;
		// }
		// double d0 = (double)(this.A - this.z);
		// if (flag && !flag2 && !flag1 && this.onGround && d0 < (double)this.dN()) {
		return (
			isNonFallingAttack(player, enemy)
			&& player.isOnGround()
		);
	}

	public static boolean checkPlayer(@NotNull Player player) {
		return player.isValid() && !GameMode.SPECTATOR.equals(player.getGameMode());
	}

	/*
	 * Returns players within a bounding box of the specified dimensions.
	 *
	 * Does not include dead players or spectators
	 */
	public static @NotNull Collection<@NotNull Player> playersInBox(
		@NotNull Location boxCenter,
		double totalWidth,
		double totalHeight
	) {
		return boxCenter.getNearbyPlayers(
			totalWidth / 2,
			totalHeight / 2,
			PlayerUtils::checkPlayer
		);
	}

	/*
	 * Returns players within a cube of the specified dimensions.
	 *
	 * Does not include dead players or spectators
	 */
	public static @NotNull Collection<@NotNull Player> playersInCube(
		@NotNull Location cubeCenter,
		double sideLength
	) {
		return playersInBox(cubeCenter, sideLength, sideLength);
	}

	/*
	 * Returns players within a sphere of the specified dimensions.
	 *
	 * Measures based on feet location.
	 * Does not include dead players or spectators
	 */
	public static @NotNull Collection<@NotNull Player> playersInSphere(
		@NotNull Location sphereCenter,
		double radius
	) {
		@NotNull Collection<@NotNull Player> spherePlayers = playersInCube(sphereCenter, radius * 2);
		double radiusSquared = radius * radius;
		spherePlayers.removeIf((@NotNull Player player) -> {
			if (sphereCenter.distanceSquared(player.getLocation()) > radiusSquared) {
				return true;
			} else {
				return false;
			}
		});

		return spherePlayers;
	}

	/*
	 * Returns players within an upright cylinder of the specified dimensions.
	 *
	 * Does not include dead players or spectators
	 */
	public static @NotNull Collection<@NotNull Player> playersInCylinder(
		@NotNull Location cylinderCenter,
		double radius,
		double totalHeight
	) {
		@NotNull Collection<@NotNull Player> cylinderPlayers = playersInBox(cylinderCenter, radius * 2, totalHeight);
		double centerY = cylinderCenter.getY();
		cylinderPlayers.removeIf((@NotNull Player player) -> {
			@NotNull Location flattenedLocation = player.getLocation();
			flattenedLocation.setY(centerY);
			if (cylinderCenter.distanceSquared(flattenedLocation) > radius * radius) {
				return true;
			} else {
				return false;
			}
		});

		return cylinderPlayers;
	}
}