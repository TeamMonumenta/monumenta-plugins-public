package com.playmonumenta.plugins.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.FloatArgument;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Entity;

public class RotateCommand {
	public static final String COMMAND = "setrotation";
	public static final String PERMISSION = "monumenta.command.setrotation";

	public static void register() {
		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.executes((sender, args) -> {
				sender.sendMessage(Component.text("Usage:", NamedTextColor.GOLD));
				sender.sendMessage(Component.text("/" + COMMAND + ": ", NamedTextColor.GOLD).append(Component.text("Shows this help text.", NamedTextColor.WHITE)));
				sender.sendMessage(Component.text("/" + COMMAND + " <entity> [yaw] [pitch]", NamedTextColor.GOLD));
				sender.sendMessage(Component.text("Sets the rotation of the targeted entity.", NamedTextColor.WHITE));
			})
			.register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withArguments(
				new EntitySelectorArgument.ManyEntities("entities")
			)
			.withOptionalArguments(
				new FloatArgument("yaw"),
				new FloatArgument("pitch")
			)
			.executes((sender, args) -> {
				List<Entity> entityList = args.getUnchecked("entities");
				if (entityList == null || entityList.isEmpty()) {
					return;
				}

				for (Entity entity : entityList) {
					float yaw = Objects.requireNonNullElseGet(args.getUnchecked("yaw"), entity::getYaw);
					float pitch = Objects.requireNonNullElseGet(args.getUnchecked("pitch"), entity::getPitch);
					entity.setRotation(yaw, pitch);
				}

			}).register();
	}
}
