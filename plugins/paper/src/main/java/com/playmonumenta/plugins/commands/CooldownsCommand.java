package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.PlayerArgument;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public class CooldownsCommand {

	private static final String COMMAND = "cooldowns";
	private static final String PERMISSION = "monumenta.command.cooldowns";


	public static void register(Plugin mPlugin) {

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION)
			.withSubcommand(
				new CommandAPICommand("getall")
					.withArguments(new PlayerArgument("who"))
					.executes((sender, args) -> {
						final Player who = (Player) Optional.ofNullable(args.getUnchecked(0)).orElseThrow();
						sender.sendMessage(Component.text("(!) Cooldowns of player: " + who.getName()));
						Plugin.getInstance().mTimers.getCooldowns(who.getUniqueId()).forEach((k, v) -> sender.sendMessage(Component.text(k.getName(), NamedTextColor.GOLD).append(Component.text(": " + v, NamedTextColor.BLUE))));
					})
			)
			.withSubcommand(new CommandAPICommand("clear"))
			.withArguments(new PlayerArgument("who"))
			.executes((sender, args) -> {
				final Player who = (Player) Optional.ofNullable(args.getUnchecked(0)).orElseThrow();
				mPlugin.mTimers.removeAllCooldowns(who);
				sender.sendMessage(Component.text("Cleared cooldowns of " + who.getName(), NamedTextColor.GREEN));
			}).register();


	}
}
