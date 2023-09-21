package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.PlayerArgument;
import java.util.HashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

public class BoatUtilsCommand {

	private static final HashMap<Player, BukkitTask> SCHEDULED_MOUNTS = new HashMap<>();
	public static final String ONE_PLAYER_BOAT_TAG = "OnePlayerBoat";

	public static void register() {
		new CommandAPICommand("boatutils")
			.withPermission("monumenta.command.boatutils")
			.withSubcommands(
				new CommandAPICommand("mount")
					.withArguments(
						new PlayerArgument("player")
					)
					.executes((executor, args) -> {
						Player player = (Player) args[0];

						mount(player);
					}),
				new CommandAPICommand("dismount")
					.withArguments(
						new PlayerArgument("player")
					)
					.executes((executor, args) -> {
						Player player = (Player) args[0];

						dismount(player);
					}),
				new CommandAPICommand("launch")
					.withArguments(
						new PlayerArgument("player"),
						new DoubleArgument("x velocity"),
						new DoubleArgument("y velocity"),
						new DoubleArgument("z velocity"),
						new IntegerArgument("ticks before remount")
					)
					.executes((executor, args) -> {
						Player player = (Player) args[0];
						Vector velocity = new Vector(
								(double) args[1],
								(double) args[2],
								(double) args[3]
						);
						int remountTicks = (int) args[4];

						launch(player, velocity, remountTicks);
					}),
				new CommandAPICommand("schedule_mount")
					.withArguments(
						new PlayerArgument("player"),
						new IntegerArgument("ticks")
					)
					.executes((executor, args) -> {
						Player player = (Player) args[0];
						int ticks = (int) args[1];

						scheduleMount(player, ticks);
					}),
				new CommandAPICommand("schedule_positioned_mount")
					.withArguments(
						new PlayerArgument("player"),
						new IntegerArgument("ticks"),
						new LocationArgument("location")
					)
					.executes((executor, args) -> {
						Player player = (Player) args[0];
						int ticks = (int) args[1];
						Location location = (Location) args[2];

						schedulePositionedMount(player, ticks, location);
					}),
				new CommandAPICommand("unschedule_mount")
					.withArguments(
						new PlayerArgument("player")
					)
					.executes((executor, args) -> {
						Player player = (Player) args[0];

						unscheduleMount(player);
					})
			)
			.register();
	}

	private static Boat.Type getPreferredBoat(Player player) {
		// OakBoat, BirchBoat, JungleBoat, AcaciaBoat, DarkOakBoat (with spruce being default)
		Boat.Type selectedType = Boat.Type.SPRUCE;

		outside: for (String tag : player.getScoreboardTags()) {
			switch (tag) {
				case "OakBoat" -> {
					selectedType = Boat.Type.OAK;
					break outside;
				}
				case "BirchBoat" -> {
					selectedType = Boat.Type.BIRCH;
					break outside;
				}
				case "AcaciaBoat" -> {
					selectedType = Boat.Type.ACACIA;
					break outside;
				}
				case "DarkOakBoat" -> {
					selectedType = Boat.Type.DARK_OAK;
					break outside;
				}
				case "JungleBoat" -> {
					selectedType = Boat.Type.JUNGLE;
					break outside;
				}
				case "MangroveBoat" -> {
					selectedType = Boat.Type.MANGROVE;
					break outside;
				}
				default -> {

				}
			}
		}

		return selectedType;
	}

	private static void mount(Player player) {
		Boat boat = (Boat) player.getWorld().spawnEntity(player.getLocation(), EntityType.BOAT);
		ArmorStand armorStand = (ArmorStand) player.getWorld().spawnEntity(boat.getLocation(), EntityType.ARMOR_STAND);
		armorStand.setVisible(false);
		armorStand.setMarker(true);
		armorStand.setSilent(true);
		armorStand.addScoreboardTag(ONE_PLAYER_BOAT_TAG);
		boat.setBoatType(getPreferredBoat(player));
		boat.addPassenger(armorStand);
		boat.addPassenger(player);
	}

	private static void dismount(Player player) {
		Entity vehicle = player.getVehicle();
		if (vehicle instanceof Boat) {
			vehicle.remove();
		}
	}

	private static void launch(Player player, Vector velocity, int remountTicks) {
		dismount(player);
		Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> player.setVelocity(velocity), 1);
		if (!SCHEDULED_MOUNTS.containsKey(player)) {
			BukkitTask task = Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
				mount(player);
				SCHEDULED_MOUNTS.remove(player);
			}, remountTicks);
			SCHEDULED_MOUNTS.put(player, task);
		}
	}

	private static void scheduleMount(Player player, int ticks) {
		if (!SCHEDULED_MOUNTS.containsKey(player)) {
			BukkitTask task = Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
				mount(player);
				SCHEDULED_MOUNTS.remove(player);
			}, ticks);
			SCHEDULED_MOUNTS.put(player, task);
		}
	}

	// Useful for mounting players at the start of boat races: it forces them to mount exactly
	// at the race's start (ScriptedQuests races allows players to move a bit during countdown)
	private static void schedulePositionedMount(Player player, int ticks, Location location) {
		if (!SCHEDULED_MOUNTS.containsKey(player)) {
			BukkitTask task = Bukkit.getScheduler().runTaskLater(Plugin.getInstance(), () -> {
				Location clonedLocation = location.clone().setDirection(player.getLocation().getDirection());
				player.teleport(clonedLocation);
				mount(player);
				SCHEDULED_MOUNTS.remove(player);
			}, ticks);
			SCHEDULED_MOUNTS.put(player, task);
		}
	}

	// Allows cancelling either of the scheduled mounts
	private static void unscheduleMount(Player player) {
		BukkitTask task = SCHEDULED_MOUNTS.remove(player);
		if (task != null) {
			task.cancel();
		}
	}
}
