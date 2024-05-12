package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.PremiumVanishIntegration;
import com.playmonumenta.plugins.itemstats.gui.PlayerItemStatsGUI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public class PlayerItemStatsGUICommand {
	public static void register(Plugin plugin) {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.playerstats");

		new CommandAPICommand("playerstats").withPermission(perms).withAliases("ps")
			.executesPlayer((sender, args) -> {
				new PlayerItemStatsGUI(sender).openInventory(sender, plugin);
			}).register();

		new CommandAPICommand("playerstats").withPermission(perms).withAliases("ps")
			.withArguments(new EntitySelectorArgument.OnePlayer("other player"))
			.executesPlayer((sender, args) -> {
				Player otherPlayer = args.getUnchecked("other player");
				if (!PremiumVanishIntegration.canSee(sender, otherPlayer)) {
					sender.sendMessage(Component.text("No player was found", NamedTextColor.RED));
					return;
				}
				new PlayerItemStatsGUI(sender, otherPlayer, false).openInventory(sender, plugin);
			}).register();

		new CommandAPICommand("playerstats").withPermission(perms).withAliases("ps")
			.withArguments(new EntitySelectorArgument.OnePlayer("player1"), new EntitySelectorArgument.OnePlayer("player2"))
			.executesPlayer((sender, args) -> {
				Player player1 = args.getUnchecked("player1");
				Player player2 = args.getUnchecked("player2");
				if (!PremiumVanishIntegration.canSee(sender, player1) || !PremiumVanishIntegration.canSee(sender, player2)) {
					sender.sendMessage(Component.text("No player was found", NamedTextColor.RED));
					return;
				}
				new PlayerItemStatsGUI(player1, player2, false).openInventory(sender, plugin);
			}).register();

	}
}
