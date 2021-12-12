package com.playmonumenta.plugins.commands;

import org.bukkit.Location;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.server.properties.ServerProperties;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

public class Stuck {
	public static String COMMAND = "stuck";
	private static Plugin mPlugin;

	public static void register(Plugin plugin) {
		CommandPermission perms = CommandPermission.fromString("monumenta.stuck");
		mPlugin = plugin;


		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.executesPlayer((player, args) -> {
				if (!ServerProperties.getShardName().equals("plots") && !ServerProperties.getShardName().equals("dev1")) {
					player.sendMessage(Component.text("Stuck is not available on this shard, contact a moderator to be unstuck.", NamedTextColor.RED)
							.decoration(TextDecoration.ITALIC, false));
					return;
				}
				player.sendMessage(Component.text("Teleporting in 10 seconds, please stand still!", NamedTextColor.RED)
						.decoration(TextDecoration.ITALIC, false));
				new BukkitRunnable() {
					int mTime = 10;
					Player target = player;
					Location startLoc = player.getLocation();
					@Override
					public void run() {
						if (target == null || !target.isOnline()) {
							this.cancel();
							return;
						}
						if (target.getLocation().distance(startLoc) > 2) {
							target.sendMessage(Component.text("You've moved too far from your original location! Please try again.", NamedTextColor.RED)
									.decoration(TextDecoration.ITALIC, false));
							this.cancel();
							return;
						}
						switch (mTime) {
						case 5:
						case 3:
						case 2:
						case 1:
							target.sendMessage(Component.text("Teleporting in " + mTime + " seconds, please stand still!", NamedTextColor.RED)
									.decoration(TextDecoration.ITALIC, false));
							break;
						case 0:
							target.teleport(player.getWorld().getSpawnLocation());
							this.cancel();
							return;
						}
						mTime--;
					}
				}.runTaskTimer(mPlugin, 20 * 1, 20 * 1);
			})
			.register();
	}
}
