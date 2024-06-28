package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.protocollib.PingListener;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class Ping {
	public static final String COMMAND = "ping";

	public static void register() {
		CommandPermission perms = CommandPermission.fromString("monumenta.ping");
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.executesPlayer((player, args) -> {
				PingListener.submitPingAction(
					player,
					(ping) -> player.sendMessage(Component.text("Your ping is " + ping + "ms.", NamedTextColor.GOLD)),
					Constants.TICKS_PER_SECOND * 15,
					false,
					() -> player.sendMessage(Component.text("Ping timed out after 15 seconds.", NamedTextColor.DARK_RED))
				);
			})
			.register();
	}
}
