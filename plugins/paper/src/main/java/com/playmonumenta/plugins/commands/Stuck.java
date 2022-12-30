package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.server.properties.ServerProperties;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class Stuck {
	public static String COMMAND = "stuck";

	public static void register() {
		CommandPermission perms = CommandPermission.fromString("monumenta.stuck");

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
					Player mTarget = player;
					Location mStartLoc = player.getLocation();

					@Override
					public void run() {
						if (mTarget == null || !mTarget.isOnline()) {
							this.cancel();
							return;
						}
						if (mTarget.getLocation().distance(mStartLoc) > 2) {
							mTarget.sendMessage(Component.text("You've moved too far from your original location! Please try again.", NamedTextColor.RED)
									.decoration(TextDecoration.ITALIC, false));
							this.cancel();
							return;
						}
						switch (mTime) {
						case 5:
						case 3:
						case 2:
						case 1:
							mTarget.sendMessage(Component.text("Teleporting in " + mTime + " seconds, please stand still!", NamedTextColor.RED)
									.decoration(TextDecoration.ITALIC, false));
							break;
							case 0:
								mTarget.teleport(player.getWorld().getSpawnLocation(), PlayerTeleportEvent.TeleportCause.COMMAND);
								this.cancel();
								return;
							default:
								// Wait
						}
						mTime--;
					}
				}.runTaskTimer(com.playmonumenta.plugins.Plugin.getInstance(), 20 * 1, 20 * 1);
			})
			.register();
	}
}
