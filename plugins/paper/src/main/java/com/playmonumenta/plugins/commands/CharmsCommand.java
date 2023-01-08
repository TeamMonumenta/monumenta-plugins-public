package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmsGUI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class CharmsCommand extends GenericCommand {

	public static void register(Plugin plugin) {

		CommandPermission perms = CommandPermission.fromString("monumenta.command.charm");
		CommandPermission guiPerms = CommandPermission.fromString("monumenta.command.charm.gui");

		//ADD COMMAND

		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(new LiteralArgument("add"));

		arguments.add(new EntitySelectorArgument.OnePlayer("player"));

		new CommandAPICommand("charm")
		.withPermission(perms)
		.withArguments(arguments)
		.executes((sender, args) -> {
			Player player = (Player) args[0];

			if (CharmManager.getInstance().addCharm(player, player.getInventory().getItemInMainHand())) {
				player.sendMessage("Charm added successfully");
			} else {
				player.sendMessage("Charm failed to add");
			}
		})
		.register();

		//REMOVE COMMAND

		arguments.clear();
		arguments.add(new LiteralArgument("remove"));
		arguments.add(new EntitySelectorArgument.OnePlayer("player"));

		new CommandAPICommand("charm")
		.withPermission(perms)
		.withArguments(arguments)
		.executes((sender, args) -> {
			Player player = (Player) args[0];

			if (CharmManager.getInstance().removeCharm(player, player.getInventory().getItemInMainHand())) {
				player.sendMessage("Charm removed successfully");
			} else {
				player.sendMessage("No charm was removed");
			}
		}).register();

		arguments.clear();
		arguments.add(new LiteralArgument("remove"));
		arguments.add(new EntitySelectorArgument.OnePlayer("player"));
		arguments.add(new IntegerArgument("slot"));

		new CommandAPICommand("charm")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = (Player) args[0];

				if (CharmManager.getInstance().removeCharmBySlot(player, (int) args[1])) {
					player.sendMessage("Charm removed successfully");
				} else {
					player.sendMessage("No charm was removed");
				}
			}).register();

		//EFFECT SUMMARY COMMAND

		arguments.clear();
		arguments.add(new LiteralArgument("effectsummary"));
		arguments.add(new EntitySelectorArgument.OnePlayer("player"));

		new CommandAPICommand("charm")
		.withPermission(perms)
		.withArguments(arguments)
		.executes((sender, args) -> {
			Player player = (Player) args[0];
			List<Component> effects = CharmManager.getInstance().getSummaryOfAllAttributesAsComponents(player);
			for (Component c : effects) {
				player.sendMessage(c);
			}
		}).register();

		//CHARM SUMMARY COMMAND

		arguments.clear();
		arguments.add(new LiteralArgument("charmsummary"));
		arguments.add(new EntitySelectorArgument.OnePlayer("player"));

		new CommandAPICommand("charm")
		.withPermission(perms)
		.withArguments(arguments)
		.executes((sender, args) -> {
			Player player = (Player) args[0];
			player.sendMessage(CharmManager.getInstance().getSummaryOfCharmNames(player));
		}).register();

		//CHARM CLEAR COMMAND

		arguments.clear();
		arguments.add(new LiteralArgument("clear"));
		arguments.add(new EntitySelectorArgument.OnePlayer("player"));

		new CommandAPICommand("charm")
		.withPermission(perms)
		.withArguments(arguments)
		.executes((sender, args) -> {
			Player player = (Player) args[0];
			CharmManager.getInstance().clearCharms(player);
		}).register();

		//CHARM GUI COMMAND
		//Usable by all players

		arguments.clear();
		arguments.add(new LiteralArgument("gui"));

		new CommandAPICommand("charm")
			.withPermission(guiPerms)
			.withArguments(arguments)
			.executesPlayer((player, args) -> {
				new CharmsGUI(player).openInventory(player, plugin);
			}).register();

		arguments.add(new EntitySelectorArgument.OnePlayer("player"));

		new CommandAPICommand("charm")
		.withPermission(guiPerms)
		.withArguments(arguments)
		.executes((sender, args) -> {
			Player player = (Player) args[0];
			Player viewer = player;
			if (sender instanceof Player s) {
				viewer = s;
			}
			new CharmsGUI(viewer, player).openInventory(viewer, plugin);
		}).register();

		//These are identical, maybe just a bit easier to use

		arguments.clear();

		new CommandAPICommand("viewcharms")
			.withAliases("vc")
			.withPermission(guiPerms)
			.withArguments(arguments)
			.executesPlayer((player, args) -> {
				new CharmsGUI(player).openInventory(player, plugin);
			}).register();

		arguments.add(new EntitySelectorArgument.OnePlayer("player"));

		new CommandAPICommand("viewcharms")
			.withAliases("vc")
			.withPermission(guiPerms)
			.withArguments(arguments)
			.executesPlayer((sender, args) -> {
				Player player = (Player) args[0];
				new CharmsGUI(sender, player).openInventory(sender, plugin);
			}).register();

		// CHARM SEARCH COMMAND

		arguments.clear();
		arguments.add(new LiteralArgument("search"));
		arguments.add(new GreedyStringArgument("key"));

		new CommandAPICommand("charm")
			.withPermission(perms)
			.withArguments(arguments)
			.executesPlayer((player, args) -> {
				String key = (String) args[0];
				List<String> results = new ArrayList<>();
				for (String effect : CharmManager.getInstance().mCharmEffectList) {
					if (effect.contains(key)) {
						results.add(effect);
					}
				}
				if (results.isEmpty()) {
					player.sendMessage(ChatColor.RED + "No effects found containing " + ChatColor.DARK_RED + key);
				} else {
					player.sendMessage("Found " + results.size() + " effects containing " + ChatColor.GREEN + key);
					results.forEach(effect -> player.sendMessage(Component.text(effect, TextColor.fromHexString("#4AC2E5"))));
				}
			}).register();

	}
}
