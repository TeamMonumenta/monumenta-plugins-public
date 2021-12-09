package com.playmonumenta.plugins.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.playmonumenta.plugins.Plugin;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.LiteralArgument;

public class RestartEmptyCommand {
	public static @Nullable BukkitRunnable TASK = null;

	public static void register(Plugin plugin) {
		new CommandAPICommand("restart-empty")
			.withPermission(CommandPermission.fromString("monumenta.command.restart-empty"))
			.executes((sender, args) -> {
				run(plugin, sender, false);
			})
			.register();

		new CommandAPICommand("restart-empty")
			.withPermission(CommandPermission.fromString("monumenta.command.restart-empty"))
			.withArguments(new LiteralArgument("cancel"))
			.executes((sender, args) -> {
				run(plugin, sender, true);
			})
			.register();
	}

	private static void run(Plugin plugin, CommandSender sender, boolean cancel) {
		if (TASK != null && !TASK.isCancelled()) {
			if (cancel) {
				TASK.cancel();
				TASK = null;
				sender.sendMessage("Pending restart cancelled");
				plugin.getLogger().info("restart-empty: Pending restart cancelled");
			} else {
				sender.sendMessage("Server is already pending restart");
			}
		} else if (cancel) {
				sender.sendMessage("Nothing changed, server was not pending restart");
		} else {
			TASK = new BukkitRunnable() {
				@Override
				public void run() {
					if (Bukkit.getOnlinePlayers().size() == 0) {
						this.cancel();
						TASK = null;
						plugin.getLogger().info("restart-empty: Restarting server now that it is empty");
						Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "stop");
					}
				}
			};
			TASK.runTaskTimer(plugin, 0, 40);
			sender.sendMessage("The server will restart the next time it is empty");
			plugin.getLogger().info("restart-empty: The server will restart the next time it is empty");
		}
	}
}
