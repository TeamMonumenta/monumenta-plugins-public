package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.MessagingUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import java.util.Collection;
import java.util.List;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;

public class UpdatePlainName {
	public static final CommandPermission PERMISSION = CommandPermission.fromString("monumenta.updateplainname");

	@SuppressWarnings("unchecked")
	public static void register() {
		new CommandAPICommand("monumenta")
			.withPermission(PERMISSION)
			.withArguments(List.of(
				new MultiLiteralArgument("update_plain_name"),
				new MultiLiteralArgument("entity"),
				new EntitySelectorArgument.ManyEntities("targets")
			))
			.executes((CommandSender sender, Object[] args) -> {
				//noinspection unchecked
				Collection<Entity> entities = (Collection<Entity>) args[2];
				for (Entity entity : entities) {
					MessagingUtils.updatePlainName(entity);
				}
			})
			.register();
	}
}
