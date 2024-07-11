package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.guis.LoadoutManagerGui;
import com.playmonumenta.plugins.managers.LoadoutManager;
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
						Player player = args.getUnchecked("player");
						Plugin.getInstance().mLoadoutManager.openOwnGui(player);
					})
			)
			.withSubcommand(
				new CommandAPICommand("gui_other")
					.withArguments(new EntitySelectorArgument.OnePlayer("other player"))
					.executesPlayer((caller, args) -> {
						Player target = args.getUnchecked("player");
						new LoadoutManagerGui(caller, target).open();
					})
			)
			.withSubcommand(
				new CommandAPICommand("quickswap")
					.withArguments(new EntitySelectorArgument.OnePlayer("player"))
					.executes((sender, args) -> {
						Player player = args.getUnchecked("player");
						Plugin.getInstance().mLoadoutManager.quickSwap(player);
					})
			)
			.withSubcommand(
				new CommandAPICommand("modify_slots")
					.withArguments(new EntitySelectorArgument.OnePlayer("player"))
					.withArguments(new IntegerArgument("delta"))
					.executes((sender, args) -> {
						Player player = args.getUnchecked("player");
						int delta = args.getUnchecked("delta");

						LoadoutManager.LoadoutData data = Plugin.getInstance().mLoadoutManager.getData(player);
						data.mMaxLoadouts += delta;

						sender.sendMessage(Component.text("Max loadouts for " + player.getName() + " is now " + data.mMaxLoadouts, NamedTextColor.YELLOW));
					})
			)
			.register();

	}

}
