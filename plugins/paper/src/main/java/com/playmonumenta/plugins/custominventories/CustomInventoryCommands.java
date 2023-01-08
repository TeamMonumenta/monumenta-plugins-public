package com.playmonumenta.plugins.custominventories;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.infinitytower.guis.TowerGuiShowMobs;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class CustomInventoryCommands {
	public static void register(Plugin plugin) {
		//Avoid unused arguments, make sure you have a permission tied to the GUI command,
		//and perform any checks that should reject the player from opening the GUI here.
		//Once in the constructor for the GUI, it's much more difficult to properly
		//reject the player.
		new CommandAPICommand("openexamplecustominvgui")
			.withPermission("monumenta.command.openexamplecustominvgui")
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				Player player = (Player)args[0];
				new ExampleCustomInventory(player).openInventory(player, plugin);
			})
			.register();

		new CommandAPICommand("openteleportergui")
			.withPermission("monumenta.command.openteleportergui")
			.executesPlayer((player, args) -> {
				new OrinCustomInventory(player, -1).openInventory(player, plugin);
			})
			.register();
		new CommandAPICommand("openteleportergui")
			.withPermission("monumenta.command.openteleportergui")
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				new OrinCustomInventory(player, -1).openInventory(player, plugin);
			})
			.register();

		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(new EntitySelectorArgument.OnePlayer("player"));
		arguments.add(new IntegerArgument("region #"));
		new CommandAPICommand("openinstancebot")
			.withPermission("monumenta.command.openinstancebot")
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = (Player) args[0];

				new OrinCustomInventory(player, (10 + (int) args[1])).openInventory(player, plugin);
			})
			.register();


		new CommandAPICommand("openpeb")
			.withPermission("monumenta.command.openpeb")
			.executesPlayer((player, args) -> {
				new PEBCustomInventory(player).openInventory(player, plugin);
			})
			.register();
		new CommandAPICommand("openpeb")
			.withPermission("monumenta.command.openpeb")
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				new PEBCustomInventory(player).openInventory(player, plugin);
			})
			.register();

		new CommandAPICommand("openinfusiongui")
			.withPermission("monumenta.command.openinfusiongui")
			.executesPlayer((player, args) -> {
				if (!player.hasPermission("monumenta.infusions")) {
					player.sendMessage(Component.text("Infusions are disabled, try again later.", NamedTextColor.RED).decoration(TextDecoration.BOLD, true));
					return;
				}
				Bukkit.getScheduler().runTaskLater(plugin, () ->
					new InfusionCustomInventory(player).openInventory(player, plugin), 1);
			})
			.register();
		new CommandAPICommand("openinfusiongui")
			.withPermission("monumenta.command.openinfusiongui")
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				if (!player.hasPermission("monumenta.infusions")) {
					player.sendMessage(Component.text("Infusions are disabled, try again later.", NamedTextColor.RED).decoration(TextDecoration.BOLD, true));
					return;
				}
				Bukkit.getScheduler().runTaskLater(plugin, () ->
					new InfusionCustomInventory(player).openInventory(player, plugin), 1);
			})
			.register();

		new CommandAPICommand("opendelveinfusiongui")
			.withPermission("monumenta.command.opendelveinfusiongui")
			.executesPlayer((player, args) -> {
				if (!player.hasPermission("monumenta.infusions")) {
					player.sendMessage(Component.text("Infusions are disabled, try again later.", NamedTextColor.RED).decoration(TextDecoration.BOLD, true));
					return;
				}
				Bukkit.getScheduler().runTaskLater(plugin, () ->
					new DelveInfusionCustomInventory(player).openInventory(player, plugin), 1);

			})
			.register();
		new CommandAPICommand("opendelveinfusiongui")
			.withPermission("monumenta.command.opendelveinfusiongui")
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				if (!player.hasPermission("monumenta.infusions")) {
					player.sendMessage(Component.text("Infusions are disabled, try again later.", NamedTextColor.RED).decoration(TextDecoration.BOLD, true));
					return;
				}
				Bukkit.getScheduler().runTaskLater(plugin, () ->
					new DelveInfusionCustomInventory(player).openInventory(player, plugin), 1);
			})
			.register();

		new CommandAPICommand("openparrotgui")
			.withPermission("monumenta.command.openparrotgui")
			.executesPlayer((player, args) -> {
				try {
					new ParrotCustomInventory(player).openInventory(player, plugin);
				} catch (Exception ex) {
					String msg = "Failed to open Parrot GUI: " + ex.getMessage();
					player.sendMessage(msg);
					ex.printStackTrace();
				}
			})
			.register();
		new CommandAPICommand("openparrotgui")
			.withPermission("monumenta.command.openparrotgui")
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				try {
					new ParrotCustomInventory(player).openInventory(player, plugin);
				} catch (Exception ex) {
					String msg = "Failed to open Parrot GUI: " + ex.getMessage();
					sender.sendMessage(msg);
					player.sendMessage(msg);
					ex.printStackTrace();
				}
			})
			.register();

		new CommandAPICommand("openblitzmobgui")
			.withPermission("monumenta.command.openblitzmobgui")
			.executesPlayer((player, args) -> {
				new TowerGuiShowMobs(player).openInventory(player, plugin);
			})
			.register();

		new CommandAPICommand("openblitzmobgui")
			.withPermission("monumenta.command.openblitzmobgui")
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				new TowerGuiShowMobs(player).openInventory(player, plugin);
			})
			.register();

		new CommandAPICommand("openclassgui")
			.withPermission("monumenta.command.openclassgui")
			.executesPlayer((player, args) -> {
				new ClassSelectionCustomInventory(player).openInventory(player, plugin);
			})
			.register();
		new CommandAPICommand("openclassgui")
			.withPermission("monumenta.command.openclassgui")
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				new ClassSelectionCustomInventory(player).openInventory(player, plugin);
			})
			.register();
		new CommandAPICommand("openmasterworkgui")
			.withPermission("monumenta.command.openmasterworkgui")
			.executesPlayer((player, args) -> {
				Bukkit.getScheduler().runTaskLater(plugin, () ->
					new MasterworkCustomInventory(player).openInventory(player, plugin), 1);

			})
			.register();
		new CommandAPICommand("openmasterworkgui")
			.withPermission("monumenta.command.openmasterworkgui")
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				Bukkit.getScheduler().runTaskLater(plugin, () ->
					new MasterworkCustomInventory(player).openInventory(player, plugin), 1);
			})
			.register();

		arguments = new ArrayList<>();
		arguments.add(new EntitySelectorArgument.OnePlayer("player"));
		arguments.add(new IntegerArgument("region #"));
		arguments.add(new IntegerArgument("level"));

		List<String> questScore = new ArrayList<>(Arrays.asList("DailyQuest", "Daily2Quest", "Daily3Quest"));
		List<String> rewardScore = new ArrayList<>(Arrays.asList("DailyReward", "Daily2Reward", "Daily3Reward"));

		new CommandAPICommand("openbountygui")
			.withPermission("monumenta.command.openbountygui")
			.withArguments(arguments)
			.executes((sender, args) -> {
				try {
					Player player = (Player) args[0];
					int region = (int) args[1];
					int level = (int) args[2];
					if (ScoreboardUtils.getScoreboardValue(player, questScore.get(region - 1)).orElse(0) == 0 &&
						ScoreboardUtils.getScoreboardValue(player, rewardScore.get(region - 1)).orElse(0) == 0) {
						new BountyGui(player, region, level).open();
					}
				} catch (Exception e) {
					MessagingUtils.sendStackTrace(Bukkit.getConsoleSender(), e);
				}
			})
			.register();

		new CommandAPICommand("emoji")
			.withPermission("monumenta.command.emoji")
			.executesPlayer((player, args) -> {
				new EmojiCustomInventory(player).openInventory(player, plugin);
			})
			.register();
		new CommandAPICommand("emoji")
			.withPermission("monumenta.command.emoji.others")
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				new EmojiCustomInventory(player).openInventory(player, plugin);
			})
			.register();
		new CommandAPICommand("emote")
			.withPermission("monumenta.command.emote")
			.executesPlayer((player, arg) -> {
				emote(player);
			})
			.register();
		new CommandAPICommand("emote")
			.withPermission("monumenta.command.emote.others")
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				Player player = (Player) args[0];
				emote(player);
			})
			.register();
	}

	private static void emote(Player player) {
		int defaultEmote = ScoreboardUtils.getScoreboardValue(player, EmojiCustomInventory.EMOJI_CHOICE_BOARD).orElse(0);
		ArrayList<EmojiCustomInventory.Emoji> list = new ArrayList<>(EmojiCustomInventory.EMOJI_LIST);
		list.removeIf(item -> item.mDefaultID != defaultEmote);
		if (list.isEmpty()) {
			player.sendMessage("Select an emote in the emoji selection GUI as a default first!");
			return;
		}
		if (list.get(0).mPatreon && !(ScoreboardUtils.getScoreboardValue(player, Constants.Objectives.PATREON_DOLLARS).orElse(0) >= Constants.PATREON_TIER_2)) {
			player.sendMessage("You must be a T2+ Patron to use this emote!");
			return;
		}
		EmojiCustomInventory.completeCommand(player, list.get(0).mLeftClick);
	}
}
