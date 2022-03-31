package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.MMLog;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import java.util.logging.Level;

public class ChangeLogLevel {
	public static void register() {
		new CommandAPICommand("monumenta")
			.withSubcommand(new CommandAPICommand("plugins")
				.withSubcommand(new CommandAPICommand("changeloglevel")
					.withPermission(CommandPermission.fromString("monumenta.command.changeloglevel"))
					.withSubcommand(new CommandAPICommand("INFO")
						.executes((sender, args) -> {
							MMLog.setLevel(Level.INFO);
						}))
					.withSubcommand(new CommandAPICommand("FINE")
						.executes((sender, args) -> {
							MMLog.setLevel(Level.FINE);
						}))
					.withSubcommand(new CommandAPICommand("FINER")
						.executes((sender, args) -> {
							MMLog.setLevel(Level.FINER);
						}))
					.withSubcommand(new CommandAPICommand("FINEST")
						.executes((sender, args) -> {
							MMLog.setLevel(Level.FINEST);
						}))
			)).register();
	}
}
