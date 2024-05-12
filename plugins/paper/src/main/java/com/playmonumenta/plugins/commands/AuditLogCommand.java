package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.listeners.AuditListener;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import org.bukkit.entity.Player;

public class AuditLogCommand {
	public static void register() {
		new CommandAPICommand("auditlog")
			.withPermission(CommandPermission.fromString("monumenta.command.auditlog"))
			.withArguments(new TextArgument("message"))
			.executes((sender, args) -> {
				AuditListener.log(args.getUnchecked("message"));
			})
			.register();
		new CommandAPICommand("auditlogplayer")
			.withPermission(CommandPermission.fromString("monumenta.command.auditlog"))
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.withArguments(new TextArgument("message"))
			.executes((sender, args) -> {
				AuditListener.log(((String) args.getUnchecked("message")).replaceAll("@S", ((Player) args.getUnchecked("player")).getName()));
			})
			.register();
		new CommandAPICommand("auditlogsevere")
			.withPermission(CommandPermission.fromString("monumenta.command.auditlog"))
			.withArguments(new TextArgument("message"))
			.executes((sender, args) -> {
				AuditListener.logSevere(args.getUnchecked("message"));
			})
			.register();
		new CommandAPICommand("auditlogsevereplayer")
			.withPermission(CommandPermission.fromString("monumenta.command.auditlog"))
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.withArguments(new TextArgument("message"))
			.executes((sender, args) -> {
				AuditListener.logSevere(((String) args.getUnchecked("message")).replaceAll("@S", ((Player) args.getUnchecked("player")).getName()));
			})
			.register();
	}
}
