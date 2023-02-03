package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.EffectType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import de.tr7zw.nbtapi.NBTItem;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemStatCommands {

	public static void registerInfoCommand() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.editinfo");

		ItemStatUtils.Region[] regionsRaw = ItemStatUtils.Region.values();
		String[] regions = new String[regionsRaw.length];
		for (int i = 0; i < regions.length; i++) {
			regions[i] = regionsRaw[i].getName();
		}

		ItemStatUtils.Tier[] tiersRaw = ItemStatUtils.Tier.values();
		String[] tiers = new String[tiersRaw.length];
		for (int i = 0; i < tiers.length; i++) {
			tiers[i] = tiersRaw[i].getName();
		}

		ItemStatUtils.Masterwork[] masterworkRaw = ItemStatUtils.Masterwork.values();
		String[] ms = new String[masterworkRaw.length];
		for (int i = 0; i < ms.length; i++) {
			ms[i] = masterworkRaw[i].getName();
		}

		ItemStatUtils.Location[] locationsRaw = ItemStatUtils.Location.values();
		String[] locations = new String[locationsRaw.length];
		for (int i = 0; i < locations.length; i++) {
			locations[i] = locationsRaw[i].getName();
		}

		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(new StringArgument("region").replaceSuggestions(ArgumentSuggestions.strings(regions)));
		arguments.add(new StringArgument("tier").replaceSuggestions(ArgumentSuggestions.strings(tiers)));
		arguments.add(new StringArgument("location").replaceSuggestions(ArgumentSuggestions.strings(locations)));
		arguments.add(new StringArgument("masterwork").replaceSuggestions(ArgumentSuggestions.strings(ms)));

		new CommandAPICommand("editinfo").withPermission(perms).withArguments(arguments).executesPlayer((player, args) -> {
			if (player.getGameMode() != GameMode.CREATIVE) {
				player.sendMessage(ChatColor.RED + "Must be in creative mode to use this command!");
				return;
			}
			ItemStatUtils.Region region = ItemStatUtils.Region.getRegion((String) args[0]);
			ItemStatUtils.Tier tier = ItemStatUtils.Tier.getTier((String) args[1]);
			ItemStatUtils.Location location = ItemStatUtils.Location.getLocation((String) args[2]);
			ItemStatUtils.Masterwork m = ItemStatUtils.Masterwork.getMasterwork((String) args[3]);
			ItemStack item = player.getInventory().getItemInMainHand();
			if (item.getType() == Material.AIR) {
				player.sendMessage(ChatColor.RED + "Must be holding an item!");
				return;
			}

			// For R3 items, set tier to match masterwork level
			if (region == ItemStatUtils.Region.RING) {
				if (m != ItemStatUtils.Masterwork.ERROR && m != ItemStatUtils.Masterwork.NONE) {
					switch (Objects.requireNonNull(m)) {
						case ZERO:
						case I:
						case II:
						case III:
							tier = ItemStatUtils.Tier.RARE;
							break;
						case IV:
						case V:
							tier = ItemStatUtils.Tier.ARTIFACT;
							break;
						case VI:
							tier = ItemStatUtils.Tier.EPIC;
							break;
						case VIIA:
						case VIIB:
						case VIIC:
							tier = ItemStatUtils.Tier.LEGENDARY;
							break;
						default:
							break;
					}
				}
			}

			ItemStatUtils.editItemInfo(item, region, tier, m, location);

			ItemStatUtils.generateItemStats(item);
			ItemStatManager.PlayerItemStats playerItemStats = Plugin.getInstance().mItemStatManager.getPlayerItemStats(player);
			if (playerItemStats != null) {
				playerItemStats.updateStats(player, true, true);
			}
		}).register();
	}

	public static void registerLoreCommand() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.editlore");

		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(new MultiLiteralArgument("add"));
		arguments.add(new IntegerArgument("index", 0));

		new CommandAPICommand("editlore").withPermission(perms).withArguments(arguments).executesPlayer((player, args) -> {
			if (player.getGameMode() != GameMode.CREATIVE) {
				player.sendMessage(ChatColor.RED + "Must be in creative mode to use this command!");
				return;
			}
			Integer index = (Integer) args[1];
			ItemStack item = player.getInventory().getItemInMainHand();
			if (item.getType() == Material.AIR) {
				player.sendMessage(ChatColor.RED + "Must be holding an item!");
				return;
			}

			ItemStatUtils.addLore(item, index, Component.empty());

			ItemStatUtils.generateItemStats(item);
		}).register();

		arguments.add(new GreedyStringArgument("lore"));

		new CommandAPICommand("editlore").withPermission(perms).withArguments(arguments).executesPlayer((player, args) -> {
			if (player.getGameMode() != GameMode.CREATIVE) {
				player.sendMessage(ChatColor.RED + "Must be in creative mode to use this command!");
				return;
			}
			Integer index = (Integer) args[1];
			String lore = (String) args[2];
			ItemStack item = player.getInventory().getItemInMainHand();
			if (item.getType() == Material.AIR) {
				player.sendMessage(ChatColor.RED + "Must be holding an item!");
				return;
			}

			ItemStatUtils.addLore(item, index, MessagingUtils.fromMiniMessage(lore));

			ItemStatUtils.generateItemStats(item);
		}).register();

		arguments.clear();
		arguments.add(new MultiLiteralArgument("del"));
		arguments.add(new IntegerArgument("index", 0));

		new CommandAPICommand("editlore").withPermission(perms).withArguments(arguments).executesPlayer((player, args) -> {
			if (player.getGameMode() != GameMode.CREATIVE) {
				player.sendMessage(ChatColor.RED + "Must be in creative mode to use this command!");
				return;
			}
			Integer index = (Integer) args[1];
			ItemStack item = player.getInventory().getItemInMainHand();
			if (item.getType() == Material.AIR) {
				player.sendMessage(ChatColor.RED + "Must be holding an item!");
				return;
			}

			ItemStatUtils.removeLore(item, index);

			ItemStatUtils.generateItemStats(item);
		}).register();

		arguments.clear();
		arguments.add(new MultiLiteralArgument("replace"));
		arguments.add(new IntegerArgument("index", 0));
		arguments.add(new GreedyStringArgument("lore"));
		new CommandAPICommand("editlore").withPermission(perms).withArguments(arguments).executesPlayer((player, args) -> {
			if (player.getGameMode() != GameMode.CREATIVE) {
				player.sendMessage(ChatColor.RED + "Must be in creative mode to use this command!");
				return;
			}
			Integer index = (Integer) args[1];
			String lore = (String) args[2];
			ItemStack item = player.getInventory().getItemInMainHand();
			if (item.getType() == Material.AIR) {
				player.sendMessage(ChatColor.RED + "Must be holding an item!");
				return;
			}

			ItemStatUtils.removeLore(item, index);
			ItemStatUtils.addLore(item, index, MessagingUtils.fromMiniMessage(lore));

			ItemStatUtils.generateItemStats(item);
		}).register();

		arguments.clear();
		arguments.add(new MultiLiteralArgument("list"));

		new CommandAPICommand("editlore").withPermission(perms).withArguments(arguments).executesPlayer((player, args) -> {
			if (player.getGameMode() != GameMode.CREATIVE) {
				player.sendMessage(ChatColor.RED + "Must be in creative mode to use this command!");
				return;
			}

			ItemStack item = player.getInventory().getItemInMainHand();
			if (item.getType() == Material.AIR) {
				player.sendMessage(ChatColor.RED + "Must be holding an item!");
				return;
			}

			List<Component> lore = ItemStatUtils.getLore(item);
			for (int i = 0; i < lore.size(); i++) {
				Component line = lore.get(i);
				player.sendMessage(line.clickEvent(ClickEvent.suggestCommand("/editlore replace " + i + " " + MessagingUtils.toMiniMessage(line))));
			}
		}).register();

		arguments.clear();
		arguments.add(new MultiLiteralArgument("register"));

		new CommandAPICommand("editlore").withPermission(perms).withArguments(arguments).executesPlayer((player, args) -> {
			if (player.getGameMode() != GameMode.CREATIVE) {
				player.sendMessage(ChatColor.RED + "Must be in creative mode to use this command!");
				return;
			}
			ItemStack item = player.getInventory().getItemInMainHand();
			if (item.getType() == Material.AIR) {
				player.sendMessage(ChatColor.RED + "Must be holding an item!");
				return;
			}

			List<Component> oldLore = item.lore();
			if (oldLore == null || oldLore.isEmpty()) {
				player.sendMessage(ChatColor.RED + "Item has no lore!");
				return;
			}

			int loreIdx = 0;
			for (Component c : oldLore) {
				ItemStatUtils.addLore(item, loreIdx, c);
				loreIdx++;
			}

			ItemStatUtils.generateItemStats(item);
		}).register();
	}

	public static void registerCharmCommand() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.editcharm");

		Argument<String> charmEffectArgument =
			new GreedyStringArgument("effect")
				.replaceSuggestions(ArgumentSuggestions.stringCollection(info -> {
					String start = info.currentArg();
					String[] split = start.split(" ", 2);
					if (split.length == 1) {
						return Collections.emptyList();
					}
					return Plugin.getInstance().mCharmManager.mCharmEffectList.stream()
						       .filter(e -> e.startsWith(split[1]))
						       .map(e -> split[0] + " " + e)
						       .toList();
				}));

		new CommandAPICommand("editcharm")
			.withPermission(perms)
			.withSubcommand(
				new CommandAPICommand("add")
					.withArguments(new IntegerArgument("index", 0))
					.executesPlayer((player, args) -> {
						if (player.getGameMode() != GameMode.CREATIVE) {
							player.sendMessage(ChatColor.RED + "Must be in creative mode to use this command!");
							return;
						}
						Integer index = (Integer) args[0];
						ItemStack item = player.getInventory().getItemInMainHand();
						if (item.getType() == Material.AIR) {
							player.sendMessage(ChatColor.RED + "Must be holding an item!");
							return;
						}

						ItemStatUtils.addCharmEffect(item, index, Component.empty());

						ItemStatUtils.generateItemStats(item);
					}))
			.withSubcommand(
				new CommandAPICommand("add")
					.withArguments(
						new IntegerArgument("index", 0),
						charmEffectArgument)
					.executesPlayer((player, args) -> {
						if (player.getGameMode() != GameMode.CREATIVE) {
							player.sendMessage(ChatColor.RED + "Must be in creative mode to use this command!");
							return;
						}
						Integer index = (Integer) args[0];
						String lore = (String) args[1];
						ItemStack item = player.getInventory().getItemInMainHand();
						if (item.getType() == Material.AIR) {
							player.sendMessage(ChatColor.RED + "Must be holding an item!");
							return;
						}

						CharmManager.CharmParsedInfo parsedInfo = CharmManager.readCharmLine(lore);
						if (parsedInfo == null) {
							player.sendMessage(Component.text("Invalid charm effect! Make sure it starts with a number, optionally a percent sign, then a space, then an effect name.", NamedTextColor.RED));
							return;
						}
						if (!Plugin.getInstance().mCharmManager.mCharmEffectList.contains(parsedInfo.mEffect)) {
							player.sendMessage(Component.text("WARNING: Unknown effect '" + parsedInfo.mEffect + "'. The charm won't work without plugin changes!", NamedTextColor.YELLOW));
						}

						String hexColor = CharmManager.getCharmEffectColor(lore.charAt(0) == '+', lore);

						Component text = Component.text(lore, TextColor.fromHexString(hexColor)).decoration(TextDecoration.ITALIC, false);
						ItemStatUtils.addCharmEffect(item, index, text);

						ItemStatUtils.generateItemStats(item);
					}))

			.withSubcommand(
				new CommandAPICommand("del")
					.withArguments(new IntegerArgument("index", 0))
					.executesPlayer((player, args) -> {
						if (player.getGameMode() != GameMode.CREATIVE) {
							player.sendMessage(ChatColor.RED + "Must be in creative mode to use this command!");
							return;
						}
						Integer index = (Integer) args[0];
						ItemStack item = player.getInventory().getItemInMainHand();
						if (item.getType() == Material.AIR) {
							player.sendMessage(ChatColor.RED + "Must be holding an item!");
							return;
						}

						ItemStatUtils.removeCharmEffect(item, index);

						ItemStatUtils.generateItemStats(item);
					}))

			.withSubcommand(
				new CommandAPICommand("power")
					.withArguments(new IntegerArgument("amount", 0))
					.executesPlayer((player, args) -> {
						if (player.getGameMode() != GameMode.CREATIVE) {
							player.sendMessage(ChatColor.RED + "Must be in creative mode to use this command!");
							return;
						}
						Integer power = (Integer) args[0];
						ItemStack item = player.getInventory().getItemInMainHand();
						if (item.getType() == Material.AIR) {
							player.sendMessage(ChatColor.RED + "Must be holding an item!");
							return;
						}

						if (power > 0) {
							ItemStatUtils.setCharmPower(item, power);
						} else {
							ItemStatUtils.removeCharmPower(item);
						}

						ItemStatUtils.generateItemStats(item);
					}))

			.withSubcommand(
				new CommandAPICommand("replace")
					.withArguments(
						new IntegerArgument("index", 0),
						charmEffectArgument
					).executesPlayer((player, args) -> {
						if (player.getGameMode() != GameMode.CREATIVE) {
							player.sendMessage(ChatColor.RED + "Must be in creative mode to use this command!");
							return;
						}
						Integer index = (Integer) args[0];
						String lore = (String) args[1];
						ItemStack item = player.getInventory().getItemInMainHand();
						if (item.getType() == Material.AIR) {
							player.sendMessage(ChatColor.RED + "Must be holding an item!");
							return;
						}

						CharmManager.CharmParsedInfo parsedInfo = CharmManager.readCharmLine(lore);
						if (parsedInfo == null) {
							player.sendMessage(Component.text("Invalid charm effect! Make sure it starts with a number, optionally a percent sign, then a space, then an effect name.", NamedTextColor.RED));
							return;
						}
						if (!Plugin.getInstance().mCharmManager.mCharmEffectList.contains(parsedInfo.mEffect)) {
							player.sendMessage(Component.text("WARNING: Unknown effect '" + parsedInfo.mEffect + "'. The charm won't work without plugin changes!", NamedTextColor.YELLOW));
						}

						ItemStatUtils.removeCharmEffect(item, index);
						if (lore.charAt(0) == '+') {
							Component text = Component.text(lore, TextColor.fromHexString("#4AC2E5")).decoration(TextDecoration.ITALIC, false);
							ItemStatUtils.addCharmEffect(item, index, text);
						} else if (lore.charAt(0) == '-') {
							Component text = Component.text(lore, TextColor.fromHexString("#D02E28")).decoration(TextDecoration.ITALIC, false);
							ItemStatUtils.addCharmEffect(item, index, text);
						} else {
							Component text = Component.text(lore, TextColor.fromHexString("#C8A2C8")).decoration(TextDecoration.ITALIC, false);
							ItemStatUtils.addCharmEffect(item, index, text);
						}

						ItemStatUtils.generateItemStats(item);
					}))
			.register();
	}

	public static void registerNameCommand() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.editname");

		ItemStatUtils.Location[] locationsRaw = ItemStatUtils.Location.values();
		String[] locations = new String[locationsRaw.length];
		for (int i = 0; i < locations.length; i++) {
			locations[i] = locationsRaw[i].getName();
		}

		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(new StringArgument("location").replaceSuggestions(ArgumentSuggestions.strings(info -> locations)));
		arguments.add(new BooleanArgument("bold"));
		arguments.add(new BooleanArgument("underline"));
		arguments.add(new GreedyStringArgument("name"));

		new CommandAPICommand("editname").withPermission(perms).withArguments(arguments).executesPlayer((player, args) -> {
			if (player.getGameMode() != GameMode.CREATIVE) {
				player.sendMessage(ChatColor.RED + "Must be in creative mode to use this command!");
				return;
			}
			ItemStatUtils.Location location = ItemStatUtils.Location.getLocation((String) args[0]);
			Boolean bold = (Boolean) args[1];
			Boolean underline = (Boolean) args[2];
			String name = (String) args[3];
			ItemStack item = player.getInventory().getItemInMainHand();
			if (item.getType() == Material.AIR) {
				player.sendMessage(ChatColor.RED + "Must be holding an item!");
				return;
			}

			ItemMeta itemMeta = item.getItemMeta();
			itemMeta.displayName(Component.text(name, TextColor.fromHexString(location.getDisplay().color().asHexString())).decoration(TextDecoration.BOLD, bold).decoration(TextDecoration.UNDERLINED, underline).decoration(TextDecoration.ITALIC, false));
			item.setItemMeta(itemMeta);
			ItemUtils.setPlainName(item, name);

		}).register();
	}

	public static void registerConsumeCommand() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.editconsume");

		String[] effects = new String[EffectType.values().length];
		int i = 0;
		for (EffectType type : EffectType.values()) {
			effects[i++] = type.getType();
		}

		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(new StringArgument("enchantment").includeSuggestions(ArgumentSuggestions.strings(info -> effects)));
		arguments.add(new IntegerArgument("duration", 0));
		arguments.add(new DoubleArgument("strength", 0));

		new CommandAPICommand("editconsume").withPermission(perms).withArguments(arguments).executesPlayer((player, args) -> {
			if (player.getGameMode() != GameMode.CREATIVE) {
				player.sendMessage(ChatColor.RED + "Must be in creative mode to use this command!");
				return;
			}
			EffectType type = EffectType.fromType((String) args[0]);
			if (type == null) {
				throw CommandAPI.failWithString("Invalid effect type " + args[0]);
			}
			int duration = (int) args[1];
			double strength = (double) args[2];
			ItemStack item = player.getInventory().getItemInMainHand();
			if (item.getType() == Material.AIR) {
				player.sendMessage(ChatColor.RED + "Must be holding an item!");
				return;
			}

			ItemStatUtils.addConsumeEffect(item, type, strength, duration, null);
		}).register();

		arguments.clear();
		arguments.add(new MultiLiteralArgument("del"));
		arguments.add(new IntegerArgument("index", 0));

		new CommandAPICommand("editconsume").withPermission(perms).withArguments(arguments).executesPlayer((player, args) -> {
			if (player.getGameMode() != GameMode.CREATIVE) {
				player.sendMessage(ChatColor.RED + "Must be in creative mode to use this command!");
				return;
			}
			Integer index = (Integer) args[1];
			ItemStack item = player.getInventory().getItemInMainHand();
			if (item.getType() == Material.AIR) {
				player.sendMessage(ChatColor.RED + "Must be holding an item!");
				return;
			}

			ItemStatUtils.removeConsumeEffect(item, index);

			ItemStatUtils.generateItemStats(item);
		}).register();
	}

	public static void registerEnchCommand() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.editench");

		String[] enchantments = new String[ItemStatUtils.EnchantmentType.values().length + ItemStatUtils.InfusionType.values().length];
		int i = 0;

		for (ItemStatUtils.EnchantmentType enchantment : ItemStatUtils.EnchantmentType.values()) {
			if (enchantment != null && enchantment.getName() != null) {
				enchantments[i] = enchantment.getName().replace(" ", "");
				i++;
			}
		}

		for (ItemStatUtils.InfusionType enchantment : ItemStatUtils.InfusionType.values()) {
			if (enchantment != null && enchantment.getName() != null) {
				enchantments[i] = enchantment.getName().replace(" ", "");
				i++;
			}
		}

		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(new StringArgument("enchantment").replaceSuggestions(ArgumentSuggestions.strings(info -> enchantments)));
		arguments.add(new IntegerArgument("level", 0));

		new CommandAPICommand("editench").withPermission(perms).withArguments(arguments).executesPlayer((player, args) -> {
			if (player.getGameMode() != GameMode.CREATIVE) {
				player.sendMessage(ChatColor.RED + "Must be in creative mode to use this command!");
				return;
			}
			String enchantment = (String) args[0];
			Integer level = (Integer) args[1];
			ItemStack item = player.getInventory().getItemInMainHand();
			if (item.getType() == Material.AIR) {
				player.sendMessage(ChatColor.RED + "Must be holding an item!");
				return;
			}

			addEnchantmentOrInfusion(item, player, enchantment, level);
		}).register();

		List<Argument<?>> argumentsOther = new ArrayList<>();
		argumentsOther.add(new EntitySelectorArgument.OnePlayer("player"));
		argumentsOther.add(new StringArgument("enchantment").replaceSuggestions(ArgumentSuggestions.strings(info -> enchantments)));
		argumentsOther.add(new IntegerArgument("level", 0));

		new CommandAPICommand("editench").withPermission(perms).withArguments(argumentsOther).executes((sender, args) -> {
			Player player = (Player) args[0];
			String enchantment = (String) args[1];
			Integer level = (Integer) args[2];
			ItemStack item = player.getInventory().getItemInMainHand();
			if (item.getType() == Material.AIR) {
				player.sendMessage(ChatColor.RED + "Must be holding an item!");
				return;
			}

			addEnchantmentOrInfusion(item, player, enchantment, level);
		}).register();
	}

	private static void addEnchantmentOrInfusion(ItemStack item, Player player, String enchantment, int level) {
		ItemStatUtils.EnchantmentType type1 = ItemStatUtils.EnchantmentType.getEnchantmentType(enchantment);
		if (type1 != null) {
			if (level > 0) {
				ItemStatUtils.addEnchantment(item, type1, level);
			} else {
				ItemStatUtils.removeEnchantment(item, type1);
			}
		}

		ItemStatUtils.InfusionType type2 = ItemStatUtils.InfusionType.getInfusionType(enchantment);
		if (type2 != null) {
			if (level > 0) {
				ItemStatUtils.addInfusion(item, type2, level, player.getUniqueId(), false);
			} else {
				ItemStatUtils.removeInfusion(item, type2, false);
			}
		}

		ItemStatUtils.generateItemStats(item);
		ItemStatManager.PlayerItemStats playerItemStats = Plugin.getInstance().mItemStatManager.getPlayerItemStats(player);
		if (playerItemStats != null) {
			playerItemStats.updateStats(player, true, true);
		}
	}

	public static void registerAttrCommand() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.editattr");

		String[] attributes = new String[ItemStatUtils.AttributeType.values().length];
		int i = 0;

		for (ItemStatUtils.AttributeType attribute : ItemStatUtils.AttributeType.values()) {
			attributes[i] = attribute.getCodeName().replace(" ", "");
			i++;
		}

		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(new StringArgument("attribute").replaceSuggestions(ArgumentSuggestions.strings(info -> attributes)));
		arguments.add(new DoubleArgument("amount"));
		arguments.add(new MultiLiteralArgument(ItemStatUtils.Operation.ADD.getName(), ItemStatUtils.Operation.MULTIPLY.getName()));
		arguments.add(new MultiLiteralArgument(ItemStatUtils.Slot.MAINHAND.getName(), ItemStatUtils.Slot.OFFHAND.getName(), ItemStatUtils.Slot.HEAD.getName(), ItemStatUtils.Slot.CHEST.getName(), ItemStatUtils.Slot.LEGS.getName(), ItemStatUtils.Slot.FEET.getName(), ItemStatUtils.Slot.PROJECTILE.getName()));

		new CommandAPICommand("editattr").withPermission(perms).withArguments(arguments).executesPlayer((player, args) -> {
			if (player.getGameMode() != GameMode.CREATIVE) {
				player.sendMessage(ChatColor.RED + "Must be in creative mode to use this command!");
				return;
			}
			String attribute = (String) args[0];
			Double amount = (Double) args[1];
			ItemStatUtils.Operation operation = ItemStatUtils.Operation.getOperation((String) args[2]);
			if (operation == null) {
				throw CommandAPI.failWithString("Invalid operation " + args[2]);
			}
			ItemStatUtils.Slot slot = ItemStatUtils.Slot.getSlot((String) args[3]);
			if (slot == null) {
				throw CommandAPI.failWithString("Invalid slot " + args[3]);
			}
			ItemStack item = player.getInventory().getItemInMainHand();
			if (item.getType() == Material.AIR) {
				player.sendMessage(ChatColor.RED + "Must be holding an item!");
				return;
			}

			if ((args[3] == "add" && attribute.contains("Multiply")) || (args[3] == "multiply" && attribute.contains("Add"))) {
				return;
			}

			if (args[3] == "add" && attribute.contains("ProjectileSpeed")) {
				player.sendMessage("You are using the wrong type of Proj Speed, do multiply");
				return;
			}

			ItemStatUtils.AttributeType type1 = ItemStatUtils.AttributeType.getAttributeType(attribute);
			if (type1 != null) {
				if (amount != 0) {
					ItemStatUtils.addAttribute(item, type1, amount, operation, slot);
				} else {
					ItemStatUtils.removeAttribute(item, type1, operation, slot);
				}
			}

			ItemStatUtils.generateItemStats(item);
			ItemStatManager.PlayerItemStats playerItemStats = Plugin.getInstance().mItemStatManager.getPlayerItemStats(player);
			if (playerItemStats != null) {
				playerItemStats.updateStats(player, true, true);
			}
		}).register();
	}

	public static void registerRemoveCommand() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.removestats");

		new CommandAPICommand("removestats").withPermission(perms).executesPlayer((player, args) -> {
			if (player.getGameMode() != GameMode.CREATIVE) {
				player.sendMessage(ChatColor.RED + "Must be in creative mode to use this command!");
				return;
			}
			ItemStack item = player.getInventory().getItemInMainHand();
			if (item.getType() == Material.AIR) {
				player.sendMessage(ChatColor.RED + "Must be holding an item!");
				return;
			}

			// remove all enchantments and attributes even though we clear everything later because some use vanilla mechanics that persist
			for (ItemStatUtils.EnchantmentType ench : ItemStatUtils.EnchantmentType.values()) {
				ItemStatUtils.removeEnchantment(item, ench);
			}

			for (ItemStatUtils.AttributeType attr : ItemStatUtils.AttributeType.values()) {
				for (ItemStatUtils.Operation op : ItemStatUtils.Operation.values()) {
					for (ItemStatUtils.Slot slot : ItemStatUtils.Slot.values()) {
						ItemStatUtils.removeAttribute(item, attr, op, slot);
					}
				}
			}

			NBTItem nbt = new NBTItem(item);
			nbt.removeKey(ItemStatUtils.MONUMENTA_KEY);
			item.setItemMeta(nbt.getItem().getItemMeta());
			item.lore(Collections.emptyList());

			ItemStatManager.PlayerItemStats playerItemStats = Plugin.getInstance().mItemStatManager.getPlayerItemStats(player);
			if (playerItemStats != null) {
				playerItemStats.updateStats(player, true, true);
			}
		}).register();
	}

}
