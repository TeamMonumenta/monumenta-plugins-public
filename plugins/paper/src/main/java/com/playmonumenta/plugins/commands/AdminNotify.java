package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.integrations.MonumentaNetworkRelayIntegration;
import com.playmonumenta.scriptedquests.Plugin;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.TextArgument;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class AdminNotify {
	public static String COMMAND = "adminnotify";

	public static void register() {
		CommandPermission perms = CommandPermission.fromString("monumenta.checkinstances");
		List<Argument> arguments = new ArrayList<>();

		arguments.add(new TextArgument("message"));

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				String message = (String)args[0];
				Player player = null;
				if (sender instanceof Player) {
					player = (Player)sender;
				}
				String textWithReplacedText = PlaceholderAPI.setPlaceholders(player, message);

				Plugin.getInstance().getLogger().info("Sent admin notify message for: " + textWithReplacedText);

				MonumentaNetworkRelayIntegration.sendAdminMessage(textWithReplacedText);
			})
			.register();
	}
}
