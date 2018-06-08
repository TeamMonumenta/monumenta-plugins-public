package pe.project.utils;

import java.util.LinkedList;
import java.util.List;

import org.bukkit.attribute.Attribute;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

public class PlayerUtils {
	public static boolean isCritical(Player player) {
		return player.getFallDistance() > 0.0F &&
        !player.isOnGround() &&
        !player.isInsideVehicle() &&
        !player.hasPotionEffect(PotionEffectType.BLINDNESS) &&
        player.getLocation().getBlock().getType() != Material.LADDER &&
        player.getLocation().getBlock().getType() != Material.VINE;
	}

	public static boolean hasLineOfSight(Player player, LivingEntity mob) {
		//	We want to check against two things since the players view isn't square,
		//	first we'll test against the X (Yaw).
		final double xDotMax = 0.65;

		Vector forwardX = player.getEyeLocation().getDirection().setY(0).normalize();
		Vector toMobX = mob.getEyeLocation().subtract(player.getEyeLocation()).toVector().setY(0).normalize();

		double xz = toMobX.dot(forwardX);
		boolean yawLos = xz > xDotMax;

		//	Then we'll test against the Y (Pitch)
		final double yDotMax = 0.70;
		final double yDotMin = -0.55;

		Vector forwardY = player.getEyeLocation().getDirection().normalize();
		Vector toMobY = mob.getEyeLocation().subtract(player.getEyeLocation()).toVector().normalize();

		double valY = toMobY.subtract(forwardY).getY();

		boolean pitchLos = valY < yDotMax && valY > yDotMin;

		return yawLos && pitchLos;
	}

	public static void awardStrike(Player player, String reason) {
		int strikes = ScoreboardUtils.getScoreboardValue(player, "Strikes");
		strikes++;
		ScoreboardUtils.setScoreboardValue(player, "Strikes", strikes);

		Location loc = player.getLocation();
		String OOBLoc = "[" + (int)loc.getX() + ", " + (int)loc.getY() + ", " + (int)loc.getZ() + "]";

		player.sendMessage(ChatColor.RED + "You've recieved a strike for "  + reason + " Location " + OOBLoc);
		player.sendMessage(ChatColor.YELLOW + "If you feel that this strike is unjustified feel free to send a message and screenshot of this to a moderator on the Discord.");

		player.teleport(player.getWorld().getSpawnLocation());
	}

	public static List<Player> getNearbyPlayers(Player player, double radius, boolean includeSourcePlayer) {
		List<Player> players = getNearbyPlayers(player.getLocation(), radius);
		if (!includeSourcePlayer) {
			players.removeIf(p -> (p == player));
		}
		return players;
	}

	public static List<Player> getNearbyPlayers(Location loc, double radius) {
		List<Player> players = new LinkedList<Player>();

		for (Player player : Bukkit.getOnlinePlayers()) {
			if (loc.distance(player.getLocation()) <= radius && player.getGameMode() != GameMode.SPECTATOR) {
				players.add(player);
			}
		}

		return players;
	}

	public static void healPlayer(Player player, double healAmount) {
		if (!player.isDead()) {
			double newHealth = Math.min(player.getHealth() + healAmount, player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
			player.setHealth(newHealth);
		}
	}
}
