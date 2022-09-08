package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.player.activity.ActivityManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ViewActivity extends GenericCommand {

	public static void register() {
		registerPlayerCommand("viewactivity", "monumenta.command.viewactivity",
			ViewActivity::run);
	}

	private static void run(CommandSender sender, Player player) {
		Component text = Component.text("Player Activities:", NamedTextColor.WHITE).decoration(TextDecoration.BOLD, true).append(Component.newline());
		for (Player worldPlayer : player.getWorld().getPlayers()) {
			int activity = ActivityManager.getManager().mActivity.getOrDefault(worldPlayer.getUniqueId(), 0);
			text = text.append(Component.newline()).append(Component.text(String.format("%s's Activity: %s", worldPlayer.getName(), activity), activity > 0 ? NamedTextColor.GREEN : NamedTextColor.RED));
		}
		sender.sendMessage(text);
	}
}
