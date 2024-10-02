package com.playmonumenta.velocity.commands;

import com.playmonumenta.velocity.handlers.MonumentaReconnectHandler;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class Rejoin implements SimpleCommand {
	private final MonumentaReconnectHandler mMonumentaReconnectHandler;

	public Rejoin(MonumentaReconnectHandler reconnectHandler) {
		this.mMonumentaReconnectHandler = reconnectHandler;
	}

	@Override
	public void execute(final Invocation invocation) {
		CommandSource sender = invocation.source();
		// Get the arguments after the command alias
		if (sender instanceof Player player) {
			ServerConnection currentConnection = player.getCurrentServer().orElse(null);
			if (currentConnection == null) {
				return;
			}
			String serverName = currentConnection.getServerInfo().getName();
			if (!mMonumentaReconnectHandler.isExcluded(serverName)) {
				player.sendMessage(Component.text("Not on excluded server!", NamedTextColor.RED));
				return;
			}
			RegisteredServer server = mMonumentaReconnectHandler.getFallbackServer(player);
			if (server != null) {
				player.sendMessage(Component.text("Sending you to " + server.getServerInfo().getName(), NamedTextColor.GREEN));
				player.createConnectionRequest(server).fireAndForget();
			} else {
				player.disconnect(Component.text("Error finding server to rejoin! Target shard may be down or data may be corrupted!", NamedTextColor.RED));
			}
		}
	}

	// This method allows you to control who can execute the command.
	// If the executor does not have the required permission,
	// the execution of the command and the control of its autocompletion
	// will be sent directly to the server on which the sender is located
	@Override
	public boolean hasPermission(final Invocation invocation) {
		return true;
		// return invocation.source().hasPermission("monumenta.command.rejoin");
	}
}
