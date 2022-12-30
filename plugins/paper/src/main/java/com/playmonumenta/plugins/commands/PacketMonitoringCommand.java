package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.protocollib.ProtocolLibIntegration;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;

public class PacketMonitoringCommand {

	public static void register(Plugin plugin) {

		ProtocolLibIntegration protocolLibIntegration = plugin.mProtocolLibIntegration;
		if (protocolLibIntegration == null) {
			return;
		}

		new CommandAPICommand("packetmonitoring")
			.withPermission("monumenta.command.packetmonitoring")
			.withArguments(new MultiLiteralArgument("off", "simple", "full"))
			.executes((sender, args) -> {
				String reporting = (String) args[0];
				protocolLibIntegration.enablePacketMonitor(sender, !reporting.equals("off"), reporting.equals("full"));
			})
			.register();

	}

}
