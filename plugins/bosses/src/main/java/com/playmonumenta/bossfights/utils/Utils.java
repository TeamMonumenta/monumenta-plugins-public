package com.playmonumenta.bossfights.utils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.playmonumenta.bossfights.spells.SpellBaseCharge;

public class Utils {
	public static class ArgumentException extends Exception {
		private static final long serialVersionUID = 1L;
		public ArgumentException(String message) {
			super(message);
		}
	}

	public static List<Player> playersInRange(Location loc, double range) {
		List<Player> out = new ArrayList<Player>();

		for (Player player : Bukkit.getServer().getOnlinePlayers()) {
			if (player.getLocation().distance(loc) < range &&
			    player.getGameMode() != GameMode.SPECTATOR &&
			    player.getHealth() > 0) {
				out.add(player);
			}
		}
		return out;
	}

	public static Location getLocation(Location origin, String sx, String sy, String sz) {
		Location out = new Location(origin.getWorld(), 0, 0, 0);
		if (sx.charAt(0) == '~') {
			if (sx.length() > 1) {
				sx = sx.substring(1);
			} else {
				sx = Integer.toString(0);
			}
			out.setX(origin.getX() + Double.parseDouble(sx));
		} else {
			out.setX(Double.parseDouble(sx));
		}
		if (sy.charAt(0) == '~') {
			if (sy.length() > 1) {
				sy = sy.substring(1);
			} else {
				sy = Integer.toString(0);
			}
			out.setY(origin.getY() + Double.parseDouble(sy));
		} else {
			out.setY(Double.parseDouble(sy));
		}
		if (sz.charAt(0) == '~') {
			if (sz.length() > 1) {
				sz = sz.substring(1);
			} else {
				sz = Integer.toString(0);
			}
			out.setZ(origin.getZ() + Double.parseDouble(sz));
		} else {
			out.setZ(Double.parseDouble(sz));
		}
		return out;
	}

	public static void assertArgCount(String[] arg, int expectedCount) throws ArgumentException {
		if (arg.length - 1 != expectedCount) {
			throw new ArgumentException("Expected " + Integer.toString(expectedCount) + " arguments, got " + Integer.toString(arg.length - 1));
		}
	}

	public static int parseInt(String arg, int min, int max) throws ArgumentException {
		int val;
		try {
			val = Integer.parseInt(arg);
		} catch (NumberFormatException e) {
			throw new ArgumentException("Unable to parse '" + arg + "' as int");
		}

		if (val < min || val > max) {
			throw new ArgumentException("Expected integer in range [" + Integer.toString(min) + "," + Integer.toString(max) + "], got " + arg);
		}
		return val;
	}

	public static Entity calleeEntity(CommandSender sender) throws ArgumentException {
		Entity launcher = null;
		if (sender instanceof Entity) {
			launcher = (Entity)sender;
		} else if (sender instanceof ProxiedCommandSender) {
			CommandSender callee = ((ProxiedCommandSender)sender).getCallee();
			if (callee instanceof Entity) {
				launcher = (Entity)callee;
			}
		}
		if (launcher == null) {
			throw new ArgumentException("Unable to determine target entity");
		}
		return launcher;
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

	/*
	 * TODO: This is really janky - it *probably* returns the correct entity... but it might not
	 */
	public static Entity summonEntityAt(Location loc, String id, String nbt) throws Exception {
		String cmd = "summon " + id + " " + loc.getX() + " " + loc.getY() + " " + loc.getZ() + " " + nbt;
		Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), cmd);

		List<Entity> entities = new ArrayList<Entity>(loc.getNearbyEntities(1f, 1f, 1f));
		if (entities.size() <= 0) {
			throw new Exception("Summoned mob but no mob appeared - " + cmd);
		}

		entities.sort((left, right) -> left.getLocation().distance(loc) >= right.getLocation().distance(loc) ? 1 : -1);
		return entities.get(0);
	}

	public static List<Player> getNearbyPlayers(Location loc, double radius) {
		List<Player> players = new ArrayList<Player>(Bukkit.getOnlinePlayers().size());

		for (Player player : Bukkit.getOnlinePlayers()) {
			if (loc.distance(player.getLocation()) <= radius) {
				players.add(player);
			}
		}

		return players;
	}

	/*
	 * Uses the charge mechanic to detect if a player has line of sight to a location (usually boss.getEyeLocation())
	 */
	public static boolean hasLineOfSight(Player player, LivingEntity target) {
		return SpellBaseCharge.doCharge(player, target, player.getEyeLocation(), Arrays.asList(player), null, null, null, null, false, false);
	}

	public static Vector getDirectionTo(Location to, Location from) {
		return getDirectionTo(to, from, true);
	}

	public static Vector getDirectionTo(Location to, Location from, boolean normalized) {
		Vector vFrom = from.toVector();
		Vector vTo = to.toVector().subtract(vFrom);
		if (normalized) {
			return vTo.normalize();
		} else {
			return vTo;
		}
	}

	public static void KnockAway(Location loc, LivingEntity target, float speed) {
		Vector dir = target.getLocation().subtract(loc.toVector()).toVector().normalize().multiply(1 + speed);
		dir.setY(0.5f);

		target.setVelocity(dir);
	}

	public static void KnockAway(Location loc, LivingEntity target, float speed, float y) {
		Vector dir = target.getLocation().subtract(loc.toVector()).toVector().normalize().multiply(1 + speed);
		dir.setY(y);

		target.setVelocity(dir);
	}

	public static ArrayList<Block> getNearbyBlocks(Block start, int radius) {
		ArrayList<Block> blocks = new ArrayList<Block>();
		for (double x = start.getLocation().getX() - radius; x <= start.getLocation().getX() + radius; x++) {
			for (double y = start.getLocation().getY() - radius; y <= start.getLocation().getY() + radius; y++) {
				for (double z = start.getLocation().getZ() - radius; z <= start.getLocation().getZ() + radius; z++) {
					Location loc = new Location(start.getWorld(), x, y, z);
					blocks.add(loc.getBlock());
				}
			}
		}
		return blocks;
	}

	/* Note:
	 * loc1 must be the location with a lesser x, y, and z coordinate than loc2.
	 * loc2 must be the location with a greater x, y, and z coordinate than loc1.
	 */

	public static List<Block> getEdge(Location loc1, Location loc2) {
		List<Block> blocks = new ArrayList<Block>();
		int x1 = loc1.getBlockX();
		int y1 = loc1.getBlockY();
		int z1 = loc1.getBlockZ();

		int x2 = loc2.getBlockX();
		int y2 = loc2.getBlockY();
		int z2 = loc2.getBlockZ();

		World world = loc1.getWorld();
		for (int xPoint = x1; xPoint <= x2; xPoint++) {
			Block currentBlock = world.getBlockAt(xPoint, y1, z1);
			blocks.add(currentBlock);
		}
		for (int xPoint = x1; xPoint <= x2; xPoint++) {
			Block currentBlock = world.getBlockAt(xPoint, y2, z2);
			blocks.add(currentBlock);
		}

		for (int yPoint = y1; yPoint <= y2; yPoint++) {
			Block currentBlock = world.getBlockAt(x1, yPoint, z1);
			blocks.add(currentBlock);
		}
		for (int yPoint = y1; yPoint <= y2; yPoint++) {
			Block currentBlock = world.getBlockAt(x2, yPoint, z2);
			blocks.add(currentBlock);
		}

		for (int zPoint = z1; zPoint <= z2; zPoint++) {
			Block currentBlock = world.getBlockAt(x1, y1, zPoint);
			blocks.add(currentBlock);
		}
		for (int zPoint = z1; zPoint <= z2; zPoint++) {
			Block currentBlock = world.getBlockAt(x2, y2, zPoint);
			blocks.add(currentBlock);
		}
		return blocks;
	}

	private static java.lang.reflect.Method cachedHandleMethod = null;
	private static java.lang.reflect.Method cachedGetAbsorpMethod = null;
	private static java.lang.reflect.Method cachedSetAbsorpMethod = null;
	public static float getAbsorp(Player player) {
		try {
			// Get player's absorp via reflection
			// Cache reflection results for performance
			if (cachedHandleMethod == null) {
				cachedHandleMethod = player.getClass().getMethod("getHandle");
			}
			Object handle = cachedHandleMethod.invoke(player);

			if (cachedGetAbsorpMethod == null) {
				cachedGetAbsorpMethod = handle.getClass().getMethod("getAbsorptionHearts");
			}
			if (cachedSetAbsorpMethod == null) {
				cachedSetAbsorpMethod = handle.getClass().getMethod("setAbsorptionHearts", float.class);
			}
			return (Float)cachedGetAbsorpMethod.invoke(handle);
		} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
			/* If error, return 0 rather than making caller handle the error */
			return 0;
		}
	}

	public static void setAbsorp(Player player, float value) {
		try {
			// Get player's absorp via reflection
			// Cache reflection results for performance
			if (cachedHandleMethod == null) {
				cachedHandleMethod = player.getClass().getMethod("getHandle");
			}
			Object handle = cachedHandleMethod.invoke(player);

			if (cachedSetAbsorpMethod == null) {
				cachedSetAbsorpMethod = handle.getClass().getMethod("setAbsorptionHearts", float.class);
			}
			cachedSetAbsorpMethod.invoke(handle, Math.max(0f, value));
		} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("rawtypes")
	public static Object getPrivateField(String fieldName, Class clazz, Object object) throws NoSuchFieldException, IllegalAccessException {
		Field field;
		Object o = null;

		field = clazz.getDeclaredField(fieldName);

		field.setAccessible(true);

		o = field.get(object);

		return o;
	}
}
