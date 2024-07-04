package com.playmonumenta.plugins.commands;


import com.playmonumenta.plugins.listeners.AuditListener;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.PlayerArgument;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/** Command to handle reporting of players:
 *  - /report to report the player
 * @author Tristian
 */
public class ReportCommand {


	private static final String COMMAND = "report";

	private static final String PERMISSION_SEND = "monumenta.commands.report.send";
	private static final String PERMISSION_SEE = "monumenta.commands.report.see";
	private static final String PERMISSION_IMMUNE = "monumenta.commands.report.immune";



	public static void register() {

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION_SEND)
			.withArguments(new PlayerArgument("who"))
			.withArguments(new GreedyStringArgument("why"))
			.executes((sender, args) -> {
				final var who = Objects.requireNonNull(args.getByClass("who", Player.class));
				final var why = Objects.requireNonNull(args.getByClass("why", String.class));

				if (who.hasPermission(PERMISSION_IMMUNE)) {
					sender.sendMessage(Component.text("(!) You cannot report this player.", NamedTextColor.RED));
					return;
				}
				if (who.getName().equals(sender.getName())) {
					sender.sendMessage(Component.text("(!) You cannot report yourself.", NamedTextColor.RED));
					return;
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

			}).register();

	}
}
