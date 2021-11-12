package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.listeners.AuditListener;

import org.bukkit.entity.Player;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;
import dev.jorel.commandapi.arguments.TextArgument;

public class AuditLogCommand {
	public static void register() {
		new CommandAPICommand("auditlog")
			.withPermission(CommandPermission.fromString("monumenta.command.auditlog"))
			.withArguments(new TextArgument("message"))
			.executes((sender, args) -> {
				AuditListener.log(((String)args[0]));
			})
			.register();
		new CommandAPICommand("auditlogplayer")
			.withPermission(CommandPermission.fromString("monumenta.command.auditlog"))
			.withArguments(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER))
			.withArguments(new TextArgument("message"))
			.executes((sender, args) -> {
				AuditListener.log(((String)args[1]).replaceAll("@S", ((Player)args[0]).getName()));
			})
			.register();
		new CommandAPICommand("auditlogsevere")
			.withPermission(CommandPermission.fromString("monumenta.command.auditlog"))
			.withArguments(new TextArgument("message"))
			.executes((sender, args) -> {
				AuditListener.logSevere(((String)args[0]));
			})
			.register();
		new CommandAPICommand("auditlogsevereplayer")
			.withPermission(CommandPermission.fromString("monumenta.command.auditlog"))
			.withArguments(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER))
			.withArguments(new TextArgument("message"))
			.executes((sender, args) -> {
				AuditListener.logSevere(((String)args[1]).replaceAll("@S", ((Player)args[0]).getName()));
			})
			.register();
	}
}
