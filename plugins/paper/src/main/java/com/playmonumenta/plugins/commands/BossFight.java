package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.utils.MessagingUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class BossFight {
	static final String COMMAND = "bossfight";

	public static void register() {
		CommandPermission perms = CommandPermission.fromString("monumenta.bossfight");
		/* First one has just the boss name (stateless) */
		List<Argument> arguments = new ArrayList<>();

		arguments.add(new EntitySelectorArgument("entity", EntitySelector.ONE_ENTITY));
		arguments.add(new StringArgument("boss_tag").replaceSuggestions(
			(info) -> {
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
		arguments.add(new LocationArgument("redstone_pos"));
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
				BossManager.createBoss(sender, (LivingEntity)entity, requestedTag);
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
					BossManager.createBoss(sender, (LivingEntity)entity, requestedTag, endLoc);
				} catch (Exception ex) {
					MessagingUtils.sendStackTrace(sender, ex);
				}
		} else {
			sender.sendMessage(ChatColor.RED + "This command must be on a LivingEntity!");
		}
	}
}
