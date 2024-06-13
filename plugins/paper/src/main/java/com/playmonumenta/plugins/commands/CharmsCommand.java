package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.depths.DepthsUtils;
import com.playmonumenta.plugins.depths.charmfactory.CharmFactory;
import com.playmonumenta.plugins.integrations.PremiumVanishIntegration;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmsGUI;
import com.playmonumenta.plugins.utils.PotionUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.LocationType;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class CharmsCommand extends GenericCommand {

	public static void register() {

		CommandPermission perms = CommandPermission.fromString("monumenta.command.charm");
		CommandPermission charmFactoryPerms = CommandPermission.fromString("monumenta.command.charmfactory");
		CommandPermission guiPerms = CommandPermission.fromString("monumenta.command.charm.gui");

		//ADD COMMAND

		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(new LiteralArgument("add"));

		arguments.add(new EntitySelectorArgument.OnePlayer("player"));

		new CommandAPICommand("charm")
		.withPermission(perms)
		.withArguments(arguments)
		.executes((sender, args) -> {
			Player player = args.getUnchecked("player");

			if (CharmManager.getInstance().addCharm(player, player.getInventory().getItemInMainHand(), CharmManager.CharmType.NORMAL)) {
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
			Player player = args.getUnchecked("player");

			if (CharmManager.getInstance().removeCharm(player, player.getInventory().getItemInMainHand(), CharmManager.CharmType.NORMAL)) {
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
				Player player = args.getUnchecked("player");

				if (CharmManager.getInstance().removeCharmBySlot(player, args.getUnchecked("slot"), CharmManager.CharmType.NORMAL)) {
					player.sendMessage("Charm removed successfully");
				} else {
					player.sendMessage("No charm was removed");
				}
			}).register();

		// CLEAR COMMAND

		arguments.clear();
		arguments.add(new LiteralArgument("clear"));
		arguments.add(new EntitySelectorArgument.OnePlayer("player"));

		new CommandAPICommand("charm")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = args.getUnchecked("player");
				for (CharmManager.CharmType charmType : CharmManager.CharmType.values()) {
					CharmManager.getInstance().clearCharms(player, charmType);
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
			Player player = args.getUnchecked("player");
			List<Component> effects = CharmManager.getInstance().getSummaryOfAllAttributesAsComponents(player, CharmManager.CharmType.NORMAL);
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
			Player player = args.getUnchecked("player");
			player.sendMessage(CharmManager.getInstance().getSummaryOfCharmNames(player, CharmManager.CharmType.NORMAL));
		}).register();

		// DEPTHS CHARM MOG COMMAND

		arguments.clear();
		arguments.add(new LiteralArgument("zenithfactory"));
		arguments.add(new IntegerArgument("rarity"));
		arguments.add(new LocationArgument("loc", LocationType.BLOCK_POSITION));

		new CommandAPICommand("charm")
			.withPermission(charmFactoryPerms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Location loc = args.getUnchecked("loc");
				int rarity = args.getUnchecked("rarity");
				int power = new Random().nextInt(5) + 1;
				ItemStack charm = CharmFactory.generateCharm(rarity, power, 0, null, null, null, null, null);
				Item lootOnGround = loc.getWorld().dropItem(loc, charm);
				lootOnGround.setGlowing(true);
				PotionUtils.applyColoredGlowing("ZenithCharmgen", lootOnGround, DepthsUtils.getRarityNamedTextColor(rarity), 10000);

			}).register();

		//CHARM GUI COMMAND
		//Usable by all players

		arguments.clear();
		arguments.add(new LiteralArgument("gui"));

		new CommandAPICommand("charm")
			.withPermission(guiPerms)
			.withArguments(arguments)
			.executesPlayer((player, args) -> {
				if (checkZone(player)) {
					return;
				}
				new CharmsGUI(player).open();
			}).register();

		arguments.add(new EntitySelectorArgument.OnePlayer("player"));

		new CommandAPICommand("charm")
			.withPermission(guiPerms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player player = args.getUnchecked("player");
				Player viewer = player;
				if (sender instanceof Player s) {
					viewer = s;
				}
				if (checkZone(viewer)) {
					return;
				}
				new CharmsGUI(viewer, player).open();
			}).register();

		// charm edit GUI command - only for moderators

		new CommandAPICommand("charm")
			.withPermission(perms)
			.withArguments(new LiteralArgument("gui-edit"),
				new EntitySelectorArgument.OnePlayer("player"))
			.executesPlayer((sender, args) -> {
				Player player = args.getUnchecked("player");
				new CharmsGUI(sender, player, true).open();
			}).register();

		//These are identical, maybe just a bit easier to use

		arguments.clear();

		new CommandAPICommand("viewcharms")
			.withAliases("vc")
			.withPermission(guiPerms)
			.withArguments(arguments)
			.executesPlayer((player, args) -> {
				if (checkZone(player)) {
					return;
				}
				new CharmsGUI(player).open();
			}).register();

		arguments.add(new EntitySelectorArgument.OnePlayer("player"));

		new CommandAPICommand("viewcharms")
			.withAliases("vc")
			.withPermission(guiPerms)
			.withArguments(arguments)
			.executesPlayer((sender, args) -> {
				if (checkZone(sender)) {
					return;
				}
				Player player = args.getUnchecked("player");
				if (!PremiumVanishIntegration.canSee(sender, player)) {
					sender.sendMessage(Component.text("No player was found", NamedTextColor.RED));
					return;
				}
				new CharmsGUI(sender, player).open();
			}).register();

		arguments.clear();

		new CommandAPICommand("viewzenithcharms")
			.withAliases("vzc")
			.withPermission(guiPerms)
			.withArguments(arguments)
			.executesPlayer((player, args) -> {
				if (checkZone(player)) {
					return;
				}
				new CharmsGUI(player, CharmManager.CharmType.ZENITH).open();
			}).register();

		arguments.add(new EntitySelectorArgument.OnePlayer("player"));

		new CommandAPICommand("viewzenithcharms")
			.withAliases("vzc")
			.withPermission(guiPerms)
			.withArguments(arguments)
			.executesPlayer((sender, args) -> {
				if (checkZone(sender)) {
					return;
				}
				Player player = args.getUnchecked("player");
				if (!PremiumVanishIntegration.canSee(sender, player)) {
					sender.sendMessage(Component.text("No player was found", NamedTextColor.RED));
					return;
				}
				new CharmsGUI(sender, player, CharmManager.CharmType.ZENITH).open();
			}).register();

		// CHARM SEARCH COMMAND

		arguments.clear();
		arguments.add(new LiteralArgument("search"));
		arguments.add(new GreedyStringArgument("key"));

		new CommandAPICommand("charm")
			.withPermission(perms)
			.withArguments(arguments)
			.executesPlayer((player, args) -> {
				String key = args.getUnchecked("key");
				List<String> results = new ArrayList<>();
				for (String effect : CharmManager.getInstance().mCharmEffectList) {
					if (effect.contains(key)) {
						results.add(effect);
					}
				}
				if (results.isEmpty()) {
					player.sendMessage(Component.text("No effects found containing ", NamedTextColor.RED).append(Component.text(key, NamedTextColor.DARK_RED)));
				} else {
					player.sendMessage(Component.text("Found " + results.size() + " effects containing ").append(Component.text(key, NamedTextColor.GREEN)));
					results.forEach(effect -> player.sendMessage(Component.text(effect, TextColor.fromHexString("#4AC2E5"))));
				}
			}).register();

	}

	private static boolean checkZone(Player player) {
		if (ZoneUtils.hasZoneProperty(player, ZoneUtils.ZoneProperty.NO_CHARMS)) {
			player.sendMessage(Component.text("Charms cannot be accessed here!", NamedTextColor.RED));
			return true;
		}
		return false;
	}
}
