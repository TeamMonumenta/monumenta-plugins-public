package com.playmonumenta.plugins.commands;

import java.util.LinkedHashMap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.playmonumenta.plugins.utils.ScoreboardUtils;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument.EntitySelector;
import io.github.jorelali.commandapi.api.arguments.FloatArgument;
import io.github.jorelali.commandapi.api.arguments.StringArgument;

public class TeleportByScore extends GenericCommand {
	public static void register() {
		String command = "teleportbyscore";
		CommandPermission perms = CommandPermission.fromString("monumenta.command.teleportbyscore");

		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("entity", new EntitySelectorArgument(EntitySelector.ONE_ENTITY));
		arguments.put("x objective", new StringArgument());
		arguments.put("y objective", new StringArgument());
		arguments.put("z objective", new StringArgument());
		CommandAPI.getInstance().register(command,
		                                  perms,
		                                  arguments,
		                                  (sender, args) -> {
											  teleport(sender, (Entity)args[0], (String)args[1], (String)args[2], (String)args[3], null, null, 1.0f);
		                                  }
		);
		arguments.put("yaw objective", new StringArgument());
		arguments.put("pitch objective", new StringArgument());
		CommandAPI.getInstance().register(command,
		                                  perms,
		                                  arguments,
		                                  (sender, args) -> {
											  teleport(sender, (Entity)args[0], (String)args[1], (String)args[2], (String)args[3], (String)args[4], (String)args[5], 1.0f);
		                                  }
		);
		arguments.put("scale", new FloatArgument(1));
		CommandAPI.getInstance().register(command,
		                                  perms,
		                                  arguments,
		                                  (sender, args) -> {
											  teleport(sender, (Entity)args[0], (String)args[1], (String)args[2], (String)args[3], (String)args[4], (String)args[5], (Float)args[6]);
		                                  }
		);
	}

	@Nullable
	private static Integer getValue(@Nonnull Entity entity, @Nullable String obj) {
		if (obj == null || obj.equals("~")) {
			return null;
		}

		return ScoreboardUtils.getScoreboardValue(entity.getName(), obj).orElse(null);
	}

	private static void teleport(@Nonnull CommandSender sender, @Nonnull Entity entity, @Nonnull String objX, @Nonnull String objY, @Nonnull String objZ, @Nullable String objYaw, @Nullable String objPitch, float scale) {
		Integer x = getValue(entity, objX);
		Integer y = getValue(entity, objY);
		Integer z = getValue(entity, objZ);
		Integer yawNullable = getValue(entity, objYaw);
		Integer pitchNullable = getValue(entity, objPitch);

		if (x == null) {
			error(sender, "Could not get value " + objX);
			return;
		} else if (y == null) {
			error(sender, "Could not get value " + objY);
			return;
		} else if (z == null) {
			error(sender, "Could not get value " + objZ);
			return;
		}

		float yaw;
		float pitch;
		if (yawNullable == null) {
			yaw = entity.getLocation().getYaw();
		} else {
			yaw = (float)yawNullable / scale;
		}
		if (pitchNullable == null) {
			pitch = entity.getLocation().getPitch();
		} else {
			pitch = (float)pitchNullable / scale;
		}

		Location loc = entity.getLocation();
		float offset = (scale == 1 ? 0.5f : 0.0f);
		loc.setX((float)x / scale + offset);
		loc.setY((float)y / scale + 0.1);
		loc.setZ((float)z / scale + offset);
		loc.setPitch((float)pitch);
		loc.setYaw((float)yaw);
		if (entity instanceof Player) {
			((Player)entity).setSwimming(false);
		}
		if (entity instanceof Mob) {
			((Mob)entity).setVelocity(new Vector(0, 0.1, 0));
		}
		entity.teleport(loc);
	}
}

