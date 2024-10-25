package com.playmonumenta.plugins.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import java.util.List;
import java.util.Objects;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class SendBlockCrackCommand {


	@SuppressWarnings("unchecked")
	public static void register() {
		final var perm = CommandPermission.fromString("monumenta.command.sendblockcrack");


		final var whereArgument = new LocationArgument("block");
		final var damage = new IntegerArgument("value [1-9]");
		final var audience = new EntitySelectorArgument.ManyPlayers("audience");
		new CommandAPICommand("sendblockcrack")
			.withPermission(perm)
			.withArguments(whereArgument, damage, audience)
			.executes((commandSender, commandArguments) -> {

				final Location where = Objects.requireNonNull(commandArguments.getByArgument(whereArgument));
				final float amount = Objects.requireNonNull(commandArguments.getByArgument(damage)).floatValue() / 9;
				for (final Player who : (List<Player>) Objects.requireNonNull(commandArguments.getUnchecked("audience"))) {
					who.sendBlockDamage(where.toLocation(who.getWorld()), amount);
				}
			})
			.register();
	}
}
