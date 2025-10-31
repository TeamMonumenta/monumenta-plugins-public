package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.DateUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

public class UpTimeCommand {
	public static final LocalDateTime mStartTime = DateUtils.trueUtcDateTime();
	public static final int mStartTick = Bukkit.getCurrentTick();

	public static void register() {
		new CommandAPICommand("uptime")
			.withPermission(CommandPermission.fromString("monumenta.command.uptime"))
			.executes((sender, args) -> {
				return run(sender);
			})
			.register();
	}

	private static int run(CommandSender sender) {
		LocalDateTime now = DateUtils.trueUtcDateTime();
		long upTimeSeconds = mStartTime.until(now, ChronoUnit.SECONDS);
		sender.sendMessage(Component.text(
			"This shard has been up for "
				+ StringUtils.longToHoursMinuteAndSeconds(upTimeSeconds),
			NamedTextColor.AQUA
		));

		return Bukkit.getCurrentTick() - mStartTick;
	}
}
