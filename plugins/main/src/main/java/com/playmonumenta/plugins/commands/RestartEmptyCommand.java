package com.playmonumenta.plugins.commands;

import java.util.LinkedHashMap;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;

public class RestartEmptyCommand {
	public static BukkitRunnable task = null;

	public static void register(Plugin plugin) {
		CommandAPI.getInstance().register("restart-empty",
		                                  CommandPermission.fromString("monumenta.command.restart-empty"),
		                                  new LinkedHashMap<>(),
		                                  (sender, args) -> {
											  run(plugin, sender);
		                                  }
		);
	}

	private static void run(Plugin plugin, CommandSender sender) {
		if (task != null && !task.isCancelled()) {
			task.cancel();
			task = null;
			sender.sendMessage("Pending restart cancelled");
			plugin.getLogger().info("restart-empty: Pending restart cancelled");
		} else {
			task = new BukkitRunnable() {
				@Override
				public void run() {
					if (Bukkit.getOnlinePlayers().size() == 0) {
						this.cancel();
						task = null;
						plugin.getLogger().info("restart-empty: Restarting server now that it is empty");
						Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), "stop");
					}
				}
			};
			task.runTaskTimer(plugin, 0, 40);
			sender.sendMessage("The server will restart the next time it is empty");
			plugin.getLogger().info("restart-empty: The server will restart the next time it is empty");
		}
	}
}
