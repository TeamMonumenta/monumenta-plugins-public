package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.CommandUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.FunctionArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.RotationArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.wrappers.FunctionWrapper;
import dev.jorel.commandapi.wrappers.Rotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentSkipListSet;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

// Designed to be 1:1 with the vanilla teleport command, but loads chunks async before teleporting.
public class TeleportAsync extends GenericCommand {
	private static final String COMMAND = "teleportasync";
	private static ConcurrentSkipListSet<UUID> entitiesTeleportingAsync = new ConcurrentSkipListSet<>();

	@SuppressWarnings("unchecked")
	public static void register() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.teleportasync");

		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(new FunctionArgument("function"));
		arguments.add(new EntitySelectorArgument.OneEntity("destination"));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				return teleport(sender, (FunctionWrapper[])args[0], (Entity)args[1]);
			})
			.register();

		arguments.clear();
		arguments.add(new FunctionArgument("function"));
		arguments.add(new LocationArgument("location"));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				return teleport(sender, (FunctionWrapper[])args[0], (Location)args[1]);
			})
			.register();

		arguments.clear();
		arguments.add(new FunctionArgument("function"));
		arguments.add(new EntitySelectorArgument.ManyEntities("targets"));
		arguments.add(new EntitySelectorArgument.OneEntity("destination"));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Entity dst = (Entity)args[2];
				return teleport(sender, (FunctionWrapper[])args[0], (Collection<Entity>)args[1], dst.getLocation(), getEntityRotation(dst));
			})
			.register();

		arguments.clear();
		arguments.add(new FunctionArgument("function"));
		arguments.add(new EntitySelectorArgument.ManyEntities("targets"));
		arguments.add(new LocationArgument("location"));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				return teleport(sender, (FunctionWrapper[])args[0], (Collection<Entity>)args[1], (Location)args[2], (Rotation)null);
			})
			.register();

		arguments.clear();
		arguments.add(new FunctionArgument("function"));
		arguments.add(new EntitySelectorArgument.ManyEntities("targets"));
		arguments.add(new LocationArgument("location"));
		arguments.add(new MultiLiteralArgument("facing"));
		arguments.add(new MultiLiteralArgument("entity"));
		arguments.add(new EntitySelectorArgument.OneEntity("facingEntity"));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				return teleportFacing(sender, (FunctionWrapper[])args[0], (Collection<Entity>)args[1], (Location)args[2], (Entity)args[5]);
			})
			.register();
		// TODO facing anchors not currently supported by CommandAPI, no support here.

		arguments.clear();
		arguments.add(new FunctionArgument("function"));
		arguments.add(new EntitySelectorArgument.ManyEntities("targets"));
		arguments.add(new LocationArgument("location"));
		arguments.add(new MultiLiteralArgument("facing"));
		arguments.add(new LocationArgument("facingLocation"));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				return teleportFacing(sender, (FunctionWrapper[])args[0], (Collection<Entity>)args[1], (Location)args[2], (Location)args[4]);
			})
			.register();

		arguments.clear();
		arguments.add(new FunctionArgument("function"));
		arguments.add(new EntitySelectorArgument.ManyEntities("targets"));
		arguments.add(new LocationArgument("location"));
		arguments.add(new RotationArgument("Rotation"));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				return teleport(sender, (FunctionWrapper[])args[0], (Collection<Entity>)args[1], (Location)args[2], (Rotation)args[3]);
			})
			.register();
	}

	// Teleport (possibly proxied) sender to an entity, copying its rotation
	private static int teleport(CommandSender sender, FunctionWrapper[] functions, Entity dst) throws WrapperCommandSyntaxException {
		CommandSender srcSender = CommandUtils.getCallee(sender);

		if (!(srcSender instanceof Entity)) {
			throw CommandAPI.failWithString("An entity is required to run this command here");
		}

		Entity src = (Entity) srcSender;
		return teleport(sender, functions, src, dst.getLocation(), getEntityRotation(dst));
	}

	// Teleport (possibly proxied) sender to a location
	private static int teleport(CommandSender sender, FunctionWrapper[] functions, Location dst) throws WrapperCommandSyntaxException {
		CommandSender srcSender = CommandUtils.getCallee(sender);

		if (!(srcSender instanceof Entity)) {
			throw CommandAPI.failWithString("An entity is required to run this command here");
		}

		Entity src = (Entity) srcSender;
		return teleport(sender, functions, src, dst, null);
	}

	private static int teleport(CommandSender sender, FunctionWrapper[] functions, Collection<Entity> entities, Location dst, @Nullable Rotation rot) {
		int teleported = 0;
		for (Entity src : entities) {
			teleported += teleport(sender, functions, src, dst, rot);
		}
		return teleported;
	}

	public static int teleport(CommandSender sender, FunctionWrapper[] functions, Entity entity, Location dst, @Nullable Rotation rot) {
		if (entitiesTeleportingAsync.contains(entity.getUniqueId())) {
			sender.sendMessage(entity.getName() + " is already scheduled to teleport, honoring previous request instead.");
			return 0;
		}

		if (entity instanceof Player) {
			((Player) entity).setSwimming(false);
		}
		if (entity instanceof Mob) {
			((Mob) entity).setVelocity(new Vector(0, 0.1, 0));
		}

		Location srcLocation = entity.getLocation();
		Location adjustedDst = dst.clone();
		if (rot == null) {
			adjustedDst.setPitch(srcLocation.getPitch());
			adjustedDst.setYaw(srcLocation.getYaw());
		} else {
			adjustedDst.setPitch(rot.getPitch());
			adjustedDst.setYaw(rot.getYaw());
		}

		entitiesTeleportingAsync.add(entity.getUniqueId());
		CompletableFuture<Boolean> completableFuture = entity.teleportAsync(adjustedDst, TeleportCause.COMMAND);

		completableFuture.thenApply(resultBool -> {
			entitiesTeleportingAsync.remove(entity.getUniqueId());

			if (resultBool) {
				sender.sendMessage("Teleported " + entity.getName());
			} else {
				sender.sendMessage("Didn't teleport " + entity.getName() + "? Got False from CompletableFuture<Boolean>.");
			}

			for (FunctionWrapper func : functions) {
				func.run();
			}

			return resultBool;
		});
		completableFuture.exceptionally(ex -> {
			entitiesTeleportingAsync.remove(entity.getUniqueId());
			sender.sendMessage("An exception occurred teleporting " + entity.getName() + ": " + ex.getMessage());
			return false;
		});

		sender.sendMessage("Teleporting " + entity.getName() + " to " + Double.toString(adjustedDst.getX()) + " " + Double.toString(adjustedDst.getY()) + " " + Double.toString(adjustedDst.getZ()) + ", yaw/pitch " + Float.toString(adjustedDst.getYaw()) + " " + Float.toString(adjustedDst.getPitch()));
		return 1;
	}

	private static int teleportFacing(CommandSender sender, FunctionWrapper[] functions, Collection<Entity> entities, Location dst, Entity facingEntity) {
		Rotation rot;
		if (entities.contains(facingEntity)) {
			rot = new Rotation(0.0f, -90.0f);
		} else {
			rot = getFacingRotation(dst, facingEntity.getLocation());
		}

		int teleported = 0;
		for (Entity src : entities) {
			teleported += teleport(sender, functions, src, dst, rot);
		}
		return teleported;
	}

	private static int teleportFacing(CommandSender sender, FunctionWrapper[] functions, Collection<Entity> entities, Location dst, Location facing) {
		Rotation rot = getFacingRotation(dst, facing);

		int teleported = 0;
		for (Entity src : entities) {
			teleported += teleport(sender, functions, src, dst, rot);
		}
		return teleported;
	}

	private static Rotation getEntityRotation(Entity entity) {
		Location loc = entity.getLocation();
		return getLocationRotation(loc);
	}

	public static Rotation getLocationRotation(Location loc) {
		return new Rotation(loc.getPitch(), loc.getYaw());
	}

	private static Rotation getFacingRotation(Location dst, Location facing) {
		Vector dstVec = dst.toVector();
		Vector facingVec = facing.toVector();
		facingVec.subtract(dstVec);

		Location workLoc = dst.clone();
		workLoc.setDirection(facingVec);

		return new Rotation(workLoc.getPitch(), workLoc.getYaw());
	}
}
