package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.StringUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

import java.time.Instant;

public class UpTimeCommand {
	public static final Instant startTime = Instant.now();

	public static void register() {
		new CommandAPICommand("uptime")
		.withPermission(CommandPermission.fromString("monumenta.command.uptime"))
		.executes((sender, args) -> {
			run(sender);
		})
		.register();
	}

	private static void run(CommandSender sender) {
		long upTimeSeconds = Instant.now().getEpochSecond() - startTime.getEpochSecond();
		sender.sendMessage(Component.text("This shard has been up for "
		                                  + StringUtils.longToHoursMinuteAndSeconds(upTimeSeconds), NamedTextColor.AQUA));
	}
}
