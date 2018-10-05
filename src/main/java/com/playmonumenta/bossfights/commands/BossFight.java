package com.playmonumenta.bossfights.commands;

import com.playmonumenta.bossfights.BossManager;

import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument.EntitySelector;
import io.github.jorelali.commandapi.api.arguments.LocationArgument;
import io.github.jorelali.commandapi.api.arguments.StringArgument;
import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;

import java.util.LinkedHashMap;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.Location;

public class BossFight {
	public static void register(BossManager manager) {
		/* First one of these includes coordinate arguments */
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("entity", new EntitySelectorArgument(EntitySelector.ONE_ENTITY));
		arguments.put("boss_tag", new StringArgument());
		arguments.put("redstone_pos", new LocationArgument());

		CommandAPI.getInstance().register("bossfight",
		                                  new CommandPermission("bossfights.bossfight"),
		                                  arguments,
		                                  (sender, args) -> {
		                                      createBossStateful(manager, sender, (Entity)args[0],
		                                                         (String)args[1],
		                                                         (Location)args[2]);
		                                  }
		);

		/* Second one has just the boss name (stateless) */
		arguments = new LinkedHashMap<>();

		arguments.put("entity", new EntitySelectorArgument(EntitySelector.ONE_ENTITY));
		arguments.put("boss_tag", new StringArgument());

		CommandAPI.getInstance().register("bossfight",
		                                  new CommandPermission("bossfights.bossfight"),
		                                  arguments,
		                                  (sender, args) -> {
		                                      createBossStateless(manager, sender, (Entity)args[0],
		                                                          (String)args[1]);
		                                  }
		);
	}

	private static void createBossStateless(BossManager manager, CommandSender sender, Entity entity, String requestedTag) {
		if (entity instanceof LivingEntity && !(entity instanceof Player)) {
			manager.createBoss((LivingEntity)entity, requestedTag);
		} else {
			sender.sendMessage("This command must be on a LivingEntity!");
		}
	}

	private static void createBossStateful(BossManager manager, CommandSender sender, Entity entity, String requestedTag, Location endLoc) {
		if (entity instanceof LivingEntity && !(entity instanceof Player)) {
			manager.createBoss((LivingEntity)entity, requestedTag, endLoc);
		} else {
			sender.sendMessage("This command must be on a LivingEntity!");
		}
	}
}
