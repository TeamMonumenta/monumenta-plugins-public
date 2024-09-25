package com.playmonumenta.plugins.custominventories;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.depths.guis.DepthsAscensionGUI;
import com.playmonumenta.plugins.depths.guis.ZenithCharmPowerGUI;
import com.playmonumenta.plugins.guis.FishingDifficultyGui;
import com.playmonumenta.plugins.guis.IchorSelectionGUI;
import com.playmonumenta.plugins.guis.MusicGui;
import com.playmonumenta.plugins.guis.classselection.ClassSelectionGui;
import com.playmonumenta.plugins.infinitytower.guis.TowerGuiShowMobs;
import com.playmonumenta.plugins.listeners.IchorListener;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import static com.playmonumenta.plugins.custominventories.OrinCustomInventory.TeleporterPage;

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
				Player player = args.getUnchecked("player");
				new ExampleCustomInventory(player).openInventory(player, plugin);
			})
			.register();

		new CommandAPICommand("openteleportergui")
			.withPermission("monumenta.command.openteleportergui")
			.executesPlayer((player, args) -> {
				new OrinCustomInventory(player, TeleporterPage.SELF_DETERMINED).openInventory(player, plugin);
			})
			.register();
		new CommandAPICommand("openteleportergui")
			.withPermission("monumenta.command.openteleportergui")
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				Player player = args.getUnchecked("player");
				new OrinCustomInventory(player, TeleporterPage.SELF_DETERMINED).openInventory(player, plugin);
			})
			.register();

		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(new EntitySelectorArgument.OnePlayer("player"));
		arguments.add(new IntegerArgument("region #"));
		new CommandAPICommand("openinstancebot")
			.withPermission("monumenta.command.openinstancebot")
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = args.getUnchecked("player");
				for (TeleporterPage page : TeleporterPage.values()) {
					if (page.mPage == 10 + (int) args.get("region #")) {
						new OrinCustomInventory(player, page).openInventory(player, plugin);
						break;
					}
				}

				new OrinCustomInventory(player, TeleporterPage.SELF_DETERMINED).openInventory(player, plugin);
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
				Player player = args.getUnchecked("player");
				new PEBCustomInventory(player).openInventory(player, plugin);
			})
			.register();

		new CommandAPICommand("openzenithcharmpowergui")
			.withPermission("monumenta.command.openzenithcharmpowergui")
			.executesPlayer((player, args) -> {
				new ZenithCharmPowerGUI(player).openInventory(player, plugin);
			})
			.register();
		new CommandAPICommand("openzenithcharmpowergui")
			.withPermission("monumenta.command.openzenithcharmpowergui")
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				Player player = args.getUnchecked("player");
				new ZenithCharmPowerGUI(player).openInventory(player, plugin);
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
				Player player = args.getUnchecked("player");
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
				Player player = args.getUnchecked("player");
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
					new ParrotCustomInventory(player).open();
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
				Player player = args.getUnchecked("player");
				try {
					new ParrotCustomInventory(player).open();
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
				Player player = args.getUnchecked("player");
				new TowerGuiShowMobs(player).openInventory(player, plugin);
			})
			.register();
		new CommandAPICommand("openfishingdifficultygui")
			.withPermission("monumenta.command.openfishingdifficultygui")
			.executesPlayer((player, args) -> {
				new FishingDifficultyGui(player).open();
			})
			.register();
		new CommandAPICommand("openfishingdifficultygui")
			.withPermission("monumenta.command.openfishingdifficultygui")
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				Player player = args.getUnchecked("player");
				new FishingDifficultyGui(player).open();
			})
			.register();
		new CommandAPICommand("openichorinfusiongui")
			.withPermission("monumenta.command.openichorinfusiongui")
			.executesPlayer((player, args) -> {
				ItemStack mainhand = player.getInventory().getItemInMainHand();
				if (IchorListener.isIchor(mainhand)) {
					new IchorSelectionGUI(player).open();
				}
			})
			.register();
		new CommandAPICommand("openichorinfusiongui")
			.withPermission("monumenta.command.openichorinfusiongui")
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				Player player = args.getUnchecked("player");
				ItemStack mainhand = player.getInventory().getItemInMainHand();
				if (IchorListener.isIchor(mainhand)) {
					new IchorSelectionGUI(player).open();
				}
			})
			.register();
		new CommandAPICommand("openclassgui")
			.withPermission("monumenta.command.openclassgui")
			.executesPlayer((player, args) -> {
				new ClassSelectionGui(player, false).open();
			})
			.register();
		new CommandAPICommand("openclassgui")
			.withPermission("monumenta.command.openclassgui")
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				Player player = args.getUnchecked("player");
				Player viewer = player;
				if (sender instanceof Player playerSender) {
					viewer = playerSender;
				}
				new ClassSelectionGui(viewer, false).open();
			})
			.register();
		new CommandAPICommand("openclassdisplaygui")
			.withPermission("monumenta.command.openclassdisplaygui")
			.executesPlayer((player, args) -> {
				if (!AbilityUtils.getClass(player).equals("No Class")) {
					new ClassDisplayCustomInventory(player).open();
				}
			})
			.register();
		new CommandAPICommand("openclassdisplaygui")
			.withPermission("monumenta.command.openclassdisplaygui")
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				Player player = args.getUnchecked("player");
				if (!AbilityUtils.getClass(player).equals("No Class")) {
					new ClassDisplayCustomInventory(player).open();
				}
			})
			.register();
		new CommandAPICommand("playerdetails")
			.withPermission("monumenta.command.playerdetails")
			.executesPlayer((player, args) -> {
				new ClassSelectionGui(player, false).open();
			})
			.register();
		new CommandAPICommand("playerdetails")
			.withPermission("monumenta.command.playerdetails")
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				Player player = args.getUnchecked("player");
				Player viewer = player;
				if (sender instanceof Player playerSender) {
					viewer = playerSender;
				}
				new PlayerDisplayCustomInventory(viewer, player).openInventory(viewer, plugin);
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
				Player player = args.getUnchecked("player");
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
					Player player = args.getUnchecked("player");
					int region = args.getUnchecked("region #");
					int level = args.getUnchecked("level");
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
				Player player = args.getUnchecked("player");
				new EmojiCustomInventory(player).openInventory(player, plugin);
			})
			.register();
		new CommandAPICommand("emote")
			.withPermission("monumenta.command.emote.self")
			.executesPlayer((player, arg) -> {
				emote(player);
			})
			.register();
		new CommandAPICommand("emote")
			.withPermission("monumenta.command.emote.others")
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				Player player = args.getUnchecked("player");
				emote(player);
			})
			.register();

		new CommandAPICommand("opentrinketgui")
			.withPermission("monumenta.command.opentrinketgui")
			.executesPlayer((player, args) -> {
				new KnickKnackSackGui(player).open();
			})
			.register();
		new CommandAPICommand("opentrinketgui")
			.withPermission("monumenta.command.opentrinketgui")
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				Player player = args.getUnchecked("player");
				new KnickKnackSackGui(player).open();
			})
			.register();
		new CommandAPICommand("openascensiongui")
			.withPermission("monumenta.command.openascensiongui")
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				Player p = args.getUnchecked("player");
				new DepthsAscensionGUI(p).open();
			}).register();

		new CommandAPICommand("openmusicgui")
			.withPermission("monumenta.command.openmusicgui")
			.withArguments(
				new EntitySelectorArgument.OnePlayer("player"),
				new MultiLiteralArgument("page", Arrays.stream(MusicGui.MusicPage.values()).map(page -> page.mLabel).toArray(String[]::new))
			)
			.withOptionalArguments(
				new BooleanArgument("fromRecordPlayer"),
				new BooleanArgument("playToOthers")
			)
			.executes((sender, args) -> {
				Player p = args.getUnchecked("player");
				String label = args.getUnchecked("page");
				MusicGui.MusicPage musicPage = Arrays.stream(MusicGui.MusicPage.values()).filter(page -> page.mLabel.equals(label)).findAny().orElse(null);
				boolean fromRecordPlayer = args.getOrDefaultUnchecked("fromRecordPlayer", false);
				boolean playToOthers = args.getOrDefaultUnchecked("playToOthers", false);
				if (musicPage != null) {
					new MusicGui(p, musicPage, fromRecordPlayer, playToOthers).open();
				}
			}).register();

		new CommandAPICommand("openenchantexplanations")
			.withPermission("monumenta.command.openenchantexplanations.self")
			.executesPlayer((player, args) -> {
				new EnchantopediaGui(player).open();
			})
			.register();
		new CommandAPICommand("openenchantexplanationsfor")
			.withPermission("monumenta.command.openenchantexplanations.others")
			.withArguments(new EntitySelectorArgument.OnePlayer("player"))
			.executes((sender, args) -> {
				Player player = args.getUnchecked("player");
				new EnchantopediaGui(player).open();
			})
			.register();
	}

	private static void emote(Player player) {
		int defaultEmote = ScoreboardUtils.getScoreboardValue(player, EmojiCustomInventory.EMOJI_CHOICE_BOARD).orElse(0);
		ArrayList<EmojiCustomInventory.Emoji> list = new ArrayList<>(EmojiCustomInventory.EMOJI_LIST);
		list.removeIf(item -> item.mDefaultID != defaultEmote);
		if (list.isEmpty()) {
			player.sendMessage(Component.text("Select an emote in the emoji selection GUI as a default first!"));
			return;
		}
		if (list.get(0).mPatreon && !(ScoreboardUtils.getScoreboardValue(player, Constants.Objectives.PATREON_DOLLARS).orElse(0) >= Constants.PATREON_TIER_2)) {
			player.sendMessage(Component.text("You must be a T2+ Patron to use this emote!"));
			return;
		}
		EmojiCustomInventory.trySpawnEmoji(player, list.get(0).mEmojiName);
	}
}
