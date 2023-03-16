package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.guis.LoadoutManagerGui;
import com.playmonumenta.plugins.managers.LoadoutManager;
import com.playmonumenta.plugins.overrides.YellowTesseractOverride;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public class LoadoutManagerCommand {

	public static void register() {

		new CommandAPICommand("loadoutmanager")
			.withPermission("monumenta.command.loadoutmanager")
			.withSubcommand(
				new CommandAPICommand("gui")
					.withArguments(new EntitySelectorArgument.OnePlayer("player"))
					.executes((sender, args) -> {
						Player player = (Player) args[0];
						if (!player.hasPermission("monumenta.feature.loadoutmanager")) {
							player.sendMessage(LoadoutManager.LOADOUT_MANAGER_NAME + " is currently disabled.");
							return;
						}
						if (ZoneUtils.hasZoneProperty(player, ZoneUtils.ZoneProperty.RESIST_5)) {
							YellowTesseractOverride.setCooldown(player, 0);
						}
						if (YellowTesseractOverride.getCooldown(player) > 0) {
							player.sendMessage(Component.text("Warning: Swapping skills is on cooldown! You will be silenced if you perform any changes to your class or abilities.", NamedTextColor.YELLOW));
						}
						new LoadoutManagerGui(player).open();
					})
			)
			.withSubcommand(
				new CommandAPICommand("gui_other")
					.withArguments(new EntitySelectorArgument.OnePlayer("other player"))
					.executes((sender, args) -> {
						Player caller = CommandUtils.getPlayerFromSender(sender);
						Player target = (Player) args[0];
						new LoadoutManagerGui(caller, target).open();
					})
			)
			.withSubcommand(
				new CommandAPICommand("quickswap")
					.withArguments(new EntitySelectorArgument.OnePlayer("player"))
					.executes((sender, args) -> {
						Player player = (Player) args[0];
						if (!player.hasPermission("monumenta.feature.loadoutmanager")) {
							player.sendMessage(LoadoutManager.LOADOUT_MANAGER_NAME + " is currently disabled.");
							return;
						}
						Plugin.getInstance().mLoadoutManager.quickSwap(player);
					})
			)
			.withSubcommand(
				new CommandAPICommand("modify_slots")
					.withArguments(new EntitySelectorArgument.OnePlayer("player"))
					.withArguments(new IntegerArgument("delta"))
					.executes((sender, args) -> {
						Player player = (Player) args[0];
						int delta = (int) args[1];

						LoadoutManager.LoadoutData data = Plugin.getInstance().mLoadoutManager.getData(player);
						data.mMaxLoadouts += delta;

						sender.sendMessage(Component.text("Max loadouts for " + player.getName() + " is now " + data.mMaxLoadouts, NamedTextColor.YELLOW));
					})
			)
			.register();

	}

}
