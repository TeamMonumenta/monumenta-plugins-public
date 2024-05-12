package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.MessagingUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import java.util.Collection;
import org.bukkit.entity.Entity;

public class UpdatePlainName {
	public static final CommandPermission PERMISSION = CommandPermission.fromString("monumenta.updateplainname");

	public static void register() {
		new CommandAPICommand("monumenta")
			.withPermission(PERMISSION)
			.withArguments(
				new LiteralArgument("update_plain_name"),
				new LiteralArgument("entity"),
				new EntitySelectorArgument.ManyEntities("targets")
			)
			.executes((sender, args) -> {
				Collection<Entity> entities = args.getUnchecked("targets");
				for (Entity entity : entities) {
					MessagingUtils.updatePlainName(entity);
				}
			})
			.register();
	}
}
