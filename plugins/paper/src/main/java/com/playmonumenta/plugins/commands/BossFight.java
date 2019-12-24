package com.playmonumenta.plugins.commands;

import java.util.LinkedHashMap;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.utils.MessagingUtils;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.DynamicSuggestedStringArgument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument.EntitySelector;
import io.github.jorelali.commandapi.api.arguments.LocationArgument;

public class BossFight {
	public static void register() {
		/* First one has just the boss name (stateless) */
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("entity", new EntitySelectorArgument(EntitySelector.ONE_ENTITY));
		arguments.put("boss_tag", new DynamicSuggestedStringArgument(() -> { return BossManager.getInstance().listBosses(); }));
		CommandAPI.getInstance().register("bossfight",
		                                  CommandPermission.fromString("monumenta.bossfight"),
		                                  arguments,
		                                  (sender, args) -> {
		                                      createBossStateless(sender, (Entity)args[0],
		                                                          (String)args[1]);
		                                  }
		);

		/* Second one of these includes coordinate arguments */
		arguments.put("redstone_pos", new LocationArgument());
		CommandAPI.getInstance().register("bossfight",
		                                  CommandPermission.fromString("monumenta.bossfight"),
		                                  arguments,
		                                  (sender, args) -> {
		                                      createBossStateful(sender, (Entity)args[0],
		                                                         (String)args[1],
		                                                         (Location)args[2]);
		                                  }
		);
	}

	private static void createBossStateless(CommandSender sender, Entity entity, String requestedTag) {
		if (entity instanceof LivingEntity && !(entity instanceof Player)) {
			try {
				BossManager.getInstance().createBoss(sender, (LivingEntity)entity, requestedTag);
			} catch (Exception ex) {
				MessagingUtils.sendStackTrace(sender, ex);
			}
		} else {
			sender.sendMessage(ChatColor.RED + "This command must be on a LivingEntity!");
		}
	}

	private static void createBossStateful(CommandSender sender, Entity entity, String requestedTag, Location endLoc) {
		if (entity instanceof LivingEntity && !(entity instanceof Player)) {
				try {
					BossManager.getInstance().createBoss(sender, (LivingEntity)entity, requestedTag, endLoc);
				} catch (Exception ex) {
					MessagingUtils.sendStackTrace(sender, ex);
				}
		} else {
			sender.sendMessage(ChatColor.RED + "This command must be on a LivingEntity!");
		}
	}
}
