package com.playmonumenta.plugins.commands;


import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.utils.DateUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.PlayerArgument;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Command to handle reporting of players:
 * - /report to report the player
 *
 * @author Tristian
 */
public class ReportCommand {


	private static final String COMMAND = "report";

	private static final String PERMISSION_SEND = "monumenta.commands.report.send";
	private static final String PERMISSION_SEE = "monumenta.commands.report.see";
	private static final String PERMISSION_IMMUNE = "monumenta.commands.report.immune";
	private static final Map<UUID, Map<UUID, Long>> dailyReports = new ConcurrentHashMap<>();


	public static void register() {

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION_SEND)
			.withArguments(new PlayerArgument("who"))
			.withArguments(new GreedyStringArgument("why"))
			.executesPlayer((sender, args) -> {
				final var who = Objects.requireNonNull(args.getByClass("who", Player.class));
				final var why = Objects.requireNonNull(args.getByClass("why", String.class));

				if (who.hasPermission(PERMISSION_IMMUNE)) {
					sender.sendMessage(Component.text("(!) You cannot report this player.", NamedTextColor.RED));
					return;
				}

				if (who.getUniqueId().equals(sender.getUniqueId())) {
					sender.sendMessage(Component.text("(!) You cannot report yourself.", NamedTextColor.RED));
					return;
				}

				final long currentDay = DateUtils.getDaysSinceEpoch();
				final UUID reporterUuid = sender.getUniqueId();
				final UUID reportedUuid = who.getUniqueId();

				final Map<UUID, Long> playersReports = dailyReports.get(reporterUuid);
				if (playersReports != null) {
					final Long lastDay = playersReports.get(reportedUuid);
					if (lastDay != null && lastDay == currentDay) {
						sender.sendMessage(Component.text("You've already reported this player today!", NamedTextColor.RED));
						return;
					}
				}

				for (var p : Bukkit.getServer().getOnlinePlayers()) {
					if (!p.hasPermission(PERMISSION_SEE)) {
						continue;
					}
					p.sendMessage(Component.text("[REPORT] ", NamedTextColor.DARK_RED).append(sender.name().color(NamedTextColor.GRAY)).append(Component.text(" reported ", NamedTextColor.GRAY)).append(who.name()).append(Component.text(":", NamedTextColor.GRAY))
						.appendNewline()
						.append(Component.text("Reason: ", NamedTextColor.DARK_RED).append(Component.text(why, NamedTextColor.GRAY))));
				}
				AuditListener.logReport(sender.getName() + " reported " + who.getName() + ": " + why);
				dailyReports.computeIfAbsent(reporterUuid, k -> new ConcurrentHashMap<>()).put(reportedUuid, currentDay);

			}).register();

	}
}
