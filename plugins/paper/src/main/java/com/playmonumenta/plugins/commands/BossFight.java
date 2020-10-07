package com.playmonumenta.plugins.commands;

import java.util.LinkedHashMap;

import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.utils.MessagingUtils;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.StringArgument;

public class BossFight {
	static final String COMMAND = "bossfight";

	public static void register() {
		CommandPermission perms = CommandPermission.fromString("monumenta.bossfight");
		/* First one has just the boss name (stateless) */
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();

		arguments.put("entity", new EntitySelectorArgument(EntitySelector.ONE_ENTITY));
		arguments.put("boss_tag", new StringArgument().overrideSuggestions(
			(sender) -> {
				return BossManager.getInstance().listBosses();
			}
		));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				createBossStateless(sender, (Entity)args[0], (String)args[1]);
			})
			.register();

		/* Second one of these includes coordinate arguments */
		arguments.put("redstone_pos", new LocationArgument());
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				createBossStateful(sender, (Entity)args[0], (String)args[1], (Location)args[2]);
			})
			.register();
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
