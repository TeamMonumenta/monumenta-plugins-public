package com.playmonumenta.plugins.commands;

import com.goncalomb.bukkit.nbteditor.nbt.attributes.ItemModifier;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.MonumentaRedisSyncIntegration;
import com.playmonumenta.plugins.itemstats.EffectType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.itemstats.enums.Location;
import com.playmonumenta.plugins.itemstats.enums.Masterwork;
import com.playmonumenta.plugins.itemstats.enums.Operation;
import com.playmonumenta.plugins.itemstats.enums.Region;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.itemstats.enums.Tier;
import com.playmonumenta.plugins.itemupdater.ItemUpdateHelper;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.NBTItem;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.nbtapi.iface.ReadableNBT;
import de.tr7zw.nbtapi.iface.ReadableNBTList;
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
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.arguments.TimeArgument;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ItemStatCommands {

	private static final Argument<String> locationArg = getLocationArgument();
	private static final IntegerArgument indexArg = new IntegerArgument("index", 0);

	public static void registerInfoCommand() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.editinfo");

		Region[] regionsRaw = Region.values();
		String[] regions = new String[regionsRaw.length];
		for (int i = 0; i < regions.length; i++) {
			regions[i] = regionsRaw[i].getName();
		}
		Argument<String> regionArg = new StringArgument("region").replaceSuggestions(ArgumentSuggestions.strings(regions));

		Tier[] tiersRaw = Tier.values();
		String[] tiers = new String[tiersRaw.length];
		for (int i = 0; i < tiers.length; i++) {
			tiers[i] = tiersRaw[i].getName();
		}
		Argument<String> tierArg = new StringArgument("tier").replaceSuggestions(ArgumentSuggestions.strings(tiers));

		Masterwork[] masterworkRaw = Masterwork.values();
		String[] ms = new String[masterworkRaw.length];
		for (int i = 0; i < ms.length; i++) {
			ms[i] = masterworkRaw[i].getName();
		}
		Argument<String> masterworkArg = new StringArgument("masterwork").replaceSuggestions(ArgumentSuggestions.strings(ms));

		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(regionArg);
		arguments.add(tierArg);
		arguments.add(locationArg);

		new CommandAPICommand("editinfo").withPermission(perms)
			.withArguments(arguments)
			.withOptionalArguments(masterworkArg)
			.executesPlayer((player, args) -> {
			ItemStack item = getHeldItemAndSendErrors(player);
			if (item == null) {
				return;
			}
			Region region = Region.getRegion(args.getByArgument(regionArg));
			Tier tier = Tier.getTier(args.getByArgument(tierArg));
			Location location = Location.getLocation(args.getByArgument(locationArg));
			Masterwork m = Masterwork.getMasterwork(args.getByArgument(masterworkArg));

			// For R3 items, set tier to match masterwork level
			if (region == Region.RING) {
				if (m != Masterwork.ERROR && m != Masterwork.NONE) {
					switch (Objects.requireNonNull(m)) {
						case ZERO:
						case I:
						case II:
						case III:
							tier = Tier.RARE;
							break;
						case IV:
						case V:
							tier = Tier.ARTIFACT;
							break;
						case VI:
							tier = Tier.EPIC;
							break;
						case VIIA:
						case VIIB:
						case VIIC:
							tier = Tier.LEGENDARY;
							break;
						default:
							break;
					}
				}
			}

			ItemStatUtils.editItemInfo(item, region, tier, m, location);

			ItemUpdateHelper.generateItemStats(item);
			Plugin.getInstance().mItemStatManager.updateStats(player);
		}).register();
	}

	public static void registerLoreCommand() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.editlore");

		GreedyStringArgument loreArg = new GreedyStringArgument("lore");
		GreedyStringArgument loreArgOptional = new GreedyStringArgument("lore");

		new CommandAPICommand("editlore").withPermission(perms)
			.withArguments(new LiteralArgument("add"))
			.withArguments(indexArg)
			.withOptionalArguments(loreArgOptional)
			.executesPlayer((player, args) -> {
			ItemStack item = getHeldItemAndSendErrors(player);
			if (item == null) {
				return;
			}
			int index = args.getByArgument(indexArg);
			String lore = args.getByArgument(loreArgOptional);
			Component comp = Component.empty();
			if (lore != null) {
				comp = MessagingUtils.fromMiniMessage(lore);
			}

			ItemUpdateHelper.regenerateStats(item);

			ItemStatUtils.addLore(item, index, comp);

			ItemUpdateHelper.generateItemStats(item);
		}).register();

		new CommandAPICommand("editlore").withPermission(perms)
			.withArguments(new LiteralArgument("del"))
			.withArguments(indexArg)
			.executesPlayer((player, args) -> {
			ItemStack item = getHeldItemAndSendErrors(player);
			if (item == null) {
				return;
			}
			int index = args.getByArgument(indexArg);

			ItemUpdateHelper.regenerateStats(item);

			ItemStatUtils.removeLore(item, index);

			ItemUpdateHelper.generateItemStats(item);
		}).register();

		new CommandAPICommand("editlore").withPermission(perms)
			.withArguments(new LiteralArgument("replace"))
			.withArguments(indexArg)
			.withArguments(loreArg)
			.executesPlayer((player, args) -> {
			ItemStack item = getHeldItemAndSendErrors(player);
			if (item == null) {
				return;
			}
			int index = args.getByArgument(indexArg);
			String lore = args.getByArgument(loreArg);

			ItemUpdateHelper.regenerateStats(item);

			ItemStatUtils.removeLore(item, index);
			ItemStatUtils.addLore(item, index, MessagingUtils.fromMiniMessage(lore));

			ItemUpdateHelper.generateItemStats(item);
		}).register();

		new CommandAPICommand("editlore").withPermission(perms)
			.withArguments(new LiteralArgument("list"))
			.executesPlayer((player, args) -> {
			ItemStack item = getHeldItemAndSendErrors(player);
			if (item == null) {
				return;
			}

			List<Component> lore = ItemStatUtils.getLore(item);
			for (int i = 0; i < lore.size(); i++) {
				Component line = lore.get(i);
				player.sendMessage(line.clickEvent(ClickEvent.suggestCommand("/editlore replace " + i + " " + MessagingUtils.toMiniMessage(line))));
			}
		}).register();

		new CommandAPICommand("editlore").withPermission(perms)
			.withArguments(new LiteralArgument("register"))
			.executesPlayer((player, args) -> {
			ItemStack item = getHeldItemAndSendErrors(player);
			if (item == null) {
				return;
			}

			List<Component> oldLore = item.lore();
			if (oldLore == null || oldLore.isEmpty()) {
				player.sendMessage(Component.text("Item has no lore!", NamedTextColor.RED));
				return;
			}

			ItemUpdateHelper.regenerateStats(item);

			int loreIdx = 0;
			for (Component c : oldLore) {
				ItemStatUtils.addLore(item, loreIdx, c);
				loreIdx++;
			}

			ItemUpdateHelper.generateItemStats(item);
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

		IntegerArgument powerArg = new IntegerArgument("amount", 0);

		new CommandAPICommand("editcharm")
			.withPermission(perms)
			.withSubcommand(
				new CommandAPICommand("add")
					.withArguments(indexArg)
					.withArguments(charmEffectArgument)
					.executesPlayer((player, args) -> {
						ItemStack item = getHeldItemAndSendErrors(player);
						if (item == null) {
							return;
						}
						int index = args.getByArgument(indexArg);
						String lore = args.getByArgument(charmEffectArgument);

						CharmManager.CharmParsedInfo parsedInfo = CharmManager.readCharmLine(lore);
						if (parsedInfo == null) {
							player.sendMessage(Component.text("Invalid charm effect! Make sure it starts with a number, optionally a percent sign, then a space, then an effect name.", NamedTextColor.RED));
							return;
						}
						if (!Plugin.getInstance().mCharmManager.mCharmEffectList.contains(parsedInfo.mEffect)) {
							player.sendMessage(Component.text("WARNING: Unknown effect '" + parsedInfo.mEffect + "'. The charm won't work without plugin changes!", NamedTextColor.YELLOW));
						}

						TextColor color = CharmManager.getCharmEffectColor(parsedInfo.mValue >= 0, parsedInfo.mEffect);

						Component text = Component.text(lore, color).decoration(TextDecoration.ITALIC, false);
						ItemStatUtils.addCharmEffect(item, index, text);

						ItemUpdateHelper.generateItemStats(item);
					}))

			.withSubcommand(
				new CommandAPICommand("del")
					.withArguments(indexArg)
					.executesPlayer((player, args) -> {
						ItemStack item = getHeldItemAndSendErrors(player);
						if (item == null) {
							return;
						}
						int index = args.getByArgument(indexArg);

						ItemStatUtils.removeCharmEffect(item, index);

						ItemUpdateHelper.generateItemStats(item);
					}))

			.withSubcommand(
				new CommandAPICommand("power")
					.withArguments(powerArg)
					.executesPlayer((player, args) -> {
						ItemStack item = getHeldItemAndSendErrors(player);
						if (item == null) {
							return;
						}
						int power = args.getByArgument(powerArg);

						if (power > 0) {
							ItemStatUtils.setCharmPower(item, power);
						} else {
							ItemStatUtils.removeCharmPower(item);
						}

						ItemUpdateHelper.generateItemStats(item);
					}))

			.withSubcommand(
				new CommandAPICommand("replace")
					.withArguments(
						indexArg,
						charmEffectArgument
					).executesPlayer((player, args) -> {
						ItemStack item = getHeldItemAndSendErrors(player);
						if (item == null) {
							return;
						}
						int index = args.getByArgument(indexArg);
						String lore = args.getByArgument(charmEffectArgument);

						CharmManager.CharmParsedInfo parsedInfo = CharmManager.readCharmLine(lore);
						if (parsedInfo == null) {
							player.sendMessage(Component.text("Invalid charm effect! Make sure it starts with a number, optionally a percent sign, then a space, then an effect name.", NamedTextColor.RED));
							return;
						}
						if (!Plugin.getInstance().mCharmManager.mCharmEffectList.contains(parsedInfo.mEffect)) {
							player.sendMessage(Component.text("WARNING: Unknown effect '" + parsedInfo.mEffect + "'. The charm won't work without plugin changes!", NamedTextColor.YELLOW));
						}

						ItemStatUtils.removeCharmEffect(item, index);
						TextColor color = CharmManager.getCharmEffectColor(parsedInfo.mValue >= 0, parsedInfo.mEffect);
						Component text = Component.text(lore, color).decoration(TextDecoration.ITALIC, false);
						ItemStatUtils.addCharmEffect(item, index, text);

						ItemUpdateHelper.generateItemStats(item);
					}))
			.register();
	}

	public static void registerFishCommand() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.editfish");

		IntegerArgument qualityArg = new IntegerArgument("amount", 0, 5);

		new CommandAPICommand("editfish")
			.withPermission(perms)
			.withSubcommand(
				new CommandAPICommand("quality")
					.withArguments(qualityArg)
					.executesPlayer((player, args) -> {
						ItemStack item = getHeldItemAndSendErrors(player);
						if (item == null) {
							return;
						}
						int quality = args.getByArgument(qualityArg);

						if (quality > 0) {
							ItemStatUtils.setFishQuality(item, quality);
						} else {
							ItemStatUtils.removeFishQuality(item);
						}

						ItemUpdateHelper.generateItemStats(item);
					})
			)
		.register();
	}

	public static void registerNameCommand() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.editname");

		BooleanArgument boldArg = new BooleanArgument("bold");
		BooleanArgument underlineArg = new BooleanArgument("underline");
		GreedyStringArgument nameArg = new GreedyStringArgument("name");

		new CommandAPICommand("editname").withPermission(perms)
			.withArguments(locationArg)
			.withOptionalArguments(boldArg)
			.withOptionalArguments(underlineArg)
			.withOptionalArguments(nameArg)
			.executesPlayer((player, args) -> {
			ItemStack item = getHeldItemAndSendErrors(player);
			if (item == null) {
				return;
			}
			Location location = Location.getLocation(args.getByArgument(locationArg));
			Boolean bold = args.getByArgument(boldArg);
			Boolean underline = args.getByArgument(underlineArg);
			String name = args.getByArgument(nameArg);

			ItemMeta itemMeta = item.getItemMeta();

			Component displayName;
			if (name != null) {
				displayName = Component.text(name).decoration(TextDecoration.ITALIC, false);
			} else {
				displayName = itemMeta.displayName();
				if (displayName == null) {
					return;
				}
			}

			displayName = displayName.color(location.getColor());
			if (bold != null) {
				displayName = displayName.decoration(TextDecoration.BOLD, bold);
			}
			if (underline != null) {
				displayName = displayName.decoration(TextDecoration.UNDERLINED, underline);
			}

			ItemStatUtils.setName(item, displayName);


			ItemUpdateHelper.generateItemStats(item);
		}).register();

		new CommandAPICommand("editname").withPermission(perms)
			.withArguments(new LiteralArgument("replace"))
			.withArguments(nameArg)
			.executesPlayer((player, args) -> {
			ItemStack item = getHeldItemAndSendErrors(player);
			if (item == null) {
				return;
			}

			String name = args.getByArgument(nameArg);

			ItemMeta itemMeta = item.getItemMeta();
			Component displayName = itemMeta.displayName();
			Component resultName = displayName != null ? Component.text(name, displayName.color()).decorations(displayName.decorations()) : Component.text(name, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false);
			ItemStatUtils.setName(item, resultName);


			ItemUpdateHelper.generateItemStats(item);
		}).register();

		new CommandAPICommand("editname").withPermission(perms)
		.withArguments(new LiteralArgument("minimessage"))
		.withArguments(nameArg)
		.executesPlayer((player, args) -> {
		ItemStack item = getHeldItemAndSendErrors(player);
		if (item == null) {
			return;
		}

		String name = args.getByArgument(nameArg);

		Component resultName = MessagingUtils.fromMiniMessage(name);
		ItemStatUtils.setName(item, resultName);

		ItemUpdateHelper.generateItemStats(item);
	}).register();
	}

	public static void registerConsumeCommand() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.editconsume");

		String[] effects = new String[EffectType.values().length];
		int i = 0;
		for (EffectType type : EffectType.values()) {
			effects[i++] = type.getType();
		}

		Argument<String> effectArg = new StringArgument("effect").includeSuggestions(ArgumentSuggestions.strings(info -> effects));
		TimeArgument durationArg = new TimeArgument("duration");
		DoubleArgument strengthArg = new DoubleArgument("strength", 0);

		new CommandAPICommand("editconsume").withPermission(perms)
			.withArguments(effectArg)
			.withArguments(durationArg)
			.withArguments(strengthArg)
			.executesPlayer((player, args) -> {
			ItemStack item = getHeldItemAndSendErrors(player);
			if (item == null) {
				return;
			}
			String effectString = args.getByArgument(effectArg);
			EffectType type = EffectType.fromType(effectString);
			if (type == null) {
				throw CommandAPI.failWithString("Invalid effect type " + effectString);
			}
			int duration = args.getByArgument(durationArg);
			double strength = args.getByArgument(strengthArg);

			ItemStatUtils.addConsumeEffect(item, type, strength, duration);
		}).register();

		new CommandAPICommand("editconsume").withPermission(perms)
			.withArguments(effectArg)
			.withArguments(new LiteralArgument("infinite"))
			.withArguments(strengthArg)
			.executesPlayer((player, args) -> {
				ItemStack item = getHeldItemAndSendErrors(player);
				if (item == null) {
					return;
				}
				String effectString = args.getByArgument(effectArg);
				EffectType type = EffectType.fromType(effectString);
				if (type == null) {
					throw CommandAPI.failWithString("Invalid effect type " + effectString);
				} else if (type.getPotionEffectType() == null) {
					throw CommandAPI.failWithString("Infinite duration can only be used with vanilla effects!");
				}
				double strength = args.getByArgument(strengthArg);

				ItemStatUtils.addConsumeEffect(item, type, strength, -1);
			}).register();

		new CommandAPICommand("editconsume").withPermission(perms)
			.withArguments(new LiteralArgument("del"))
			.withArguments(indexArg)
			.executesPlayer((player, args) -> {
			ItemStack item = getHeldItemAndSendErrors(player);
			if (item == null) {
				return;
			}

			int index = args.getByArgument(indexArg);

			ItemStatUtils.removeConsumeEffect(item, index);

			ItemUpdateHelper.generateItemStats(item);
		}).register();
	}

	public static void registerEnchCommand() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.editench");

		String[] enchantments = new String[EnchantmentType.values().length + InfusionType.values().length];
		int i = 0;

		for (EnchantmentType enchantment : EnchantmentType.values()) {
			if (enchantment != null && enchantment.getName() != null) {
				enchantments[i] = enchantment.getName().replace(" ", "");
				i++;
			}
		}

		for (InfusionType enchantment : InfusionType.values()) {
			if (enchantment != null && enchantment.getName() != null) {
				enchantments[i] = enchantment.getName().replace(" ", "");
				i++;
			}
		}

		Argument<String> enchantmentArgument = new StringArgument("enchantment").replaceSuggestions(ArgumentSuggestions.strings(info -> enchantments));
		IntegerArgument levelArg = new IntegerArgument("level", 0);
		StringArgument npcNameArg = new StringArgument("NPC Name");

		new CommandAPICommand("editench").withPermission(perms)
			.withArguments(enchantmentArgument)
			.withOptionalArguments(levelArg)
			.withOptionalArguments(npcNameArg)
			.executesPlayer((player, args) -> {
			ItemStack item = getHeldItemAndSendErrors(player);
			if (item == null) {
				return;
			}
			String enchantment = args.getByArgument(enchantmentArgument);
			int level = args.getByArgumentOrDefault(levelArg, 1);
			String npcName = args.getByArgument(npcNameArg);

			if (npcName == null || npcName.isEmpty()) {
				addEnchantmentOrInfusion(item, player, enchantment, level);
			} else {
				addNpcInfusion(item, player, enchantment, level, npcName);
			}
		}).register();

		EntitySelectorArgument.OnePlayer playerArg = new EntitySelectorArgument.OnePlayer("player");
		MultiLiteralArgument infuserTypeArg = new MultiLiteralArgument("infusertype", "player", "npc");
		StringArgument npcOrPlayerNameArg = new StringArgument("npc name/player name");

		new CommandAPICommand("editench").withPermission(perms)
			.withArguments(playerArg)
			.withArguments(enchantmentArgument)
			.withOptionalArguments(levelArg)
			.withOptionalArguments(infuserTypeArg)
			.withOptionalArguments(npcOrPlayerNameArg)
			.executes((sender, args) -> {
			Player player = args.getByArgument(playerArg);
			String enchantment = args.getByArgument(enchantmentArgument);
			int level = args.getByArgumentOrDefault(levelArg, 1);

			ItemStack item = getHeldItemAndSendErrors(player, false);

			String infuserType = args.getByArgument(infuserTypeArg);
			String name = args.getByArgument(npcOrPlayerNameArg);
			if (infuserType == null || name == null || name.isEmpty()) {
				addEnchantmentOrInfusion(item, player, enchantment, level);
			} else {
				if (infuserType.toLowerCase(Locale.ROOT).contains("player")) {
					@Nullable UUID uuid;
					try {
						// first attempt to parse it as a valid uuid
						uuid = UUID.fromString(name);
					} catch (IllegalArgumentException ex) {
						// if invalid, parse it as a player name
						uuid = MonumentaRedisSyncIntegration.cachedNameToUuid(name);
					}
					if (uuid == null) {
						throw CommandAPI.failWithString("Could not find a valid player from username in redis");
					}
					addUuidInfusion(item, player, enchantment, level, uuid);
				} else {
					addNpcInfusion(item, player, enchantment, level, name);
				}
			}
		}).register();
	}

	private static void addEnchantmentOrInfusion(@Nullable ItemStack item, Player player, String enchantment, int level) {
		if (item == null) {
			return;
		}

		EnchantmentType enchantmentType = EnchantmentType.getEnchantmentType(enchantment);
		if (enchantmentType != null) {
			if (level > 0) {
				ItemStatUtils.addEnchantment(item, enchantmentType, level);
			} else {
				ItemStatUtils.removeEnchantment(item, enchantmentType);
			}
		}

		InfusionType infusionType = InfusionType.getInfusionType(enchantment);
		if (infusionType != null) {
			if (level > 0) {
				ItemStatUtils.addInfusion(item, infusionType, level, player.getUniqueId(), false);
			} else {
				ItemStatUtils.removeInfusion(item, infusionType, false);
			}
		}

		ItemUpdateHelper.generateItemStats(item);
		Plugin.getInstance().mItemStatManager.updateStats(player);
	}

	private static void addUuidInfusion(@Nullable ItemStack item, Player player, String enchantment, int level, UUID uuid) {
		if (item == null) {
			return;
		}

		InfusionType infusionType = InfusionType.getInfusionType(enchantment);
		if (infusionType != null) {
			if (level > 0) {
				ItemStatUtils.addInfusion(item, infusionType, level, uuid, false);
			} else {
				ItemStatUtils.removeInfusion(item, infusionType, false);
			}
		}

		ItemUpdateHelper.generateItemStats(item);
		Plugin.getInstance().mItemStatManager.updateStats(player);
	}

	private static void addNpcInfusion(@Nullable ItemStack item, Player player, String enchantment, int level, String npcName) {
		if (item == null) {
			return;
		}

		InfusionType infusionType = InfusionType.getInfusionType(enchantment);
		if (infusionType != null) {
			if (level > 0) {
				ItemStatUtils.addInfusion(item, infusionType, level, npcName, false);
			} else {
				ItemStatUtils.removeInfusion(item, infusionType, false);
			}
		}

		ItemUpdateHelper.generateItemStats(item);
		Plugin.getInstance().mItemStatManager.updateStats(player);
	}

	public static void registerAttrCommand() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.editattr");

		String[] attributes = new String[AttributeType.values().length];
		int i = 0;

		for (AttributeType attribute : AttributeType.values()) {
			attributes[i] = attribute.getCodeName().replace(" ", "");
			i++;
		}

		Argument<String> attributeArg = new StringArgument("attribute").replaceSuggestions(ArgumentSuggestions.strings(info -> attributes));
		DoubleArgument amountArg = new DoubleArgument("amount");
		MultiLiteralArgument operationArg = new MultiLiteralArgument("operation", Operation.ADD.getName(), Operation.MULTIPLY.getName());
		MultiLiteralArgument slotArg = new MultiLiteralArgument("slot", Slot.MAINHAND.getName(), Slot.OFFHAND.getName(), Slot.HEAD.getName(), Slot.CHEST.getName(), Slot.LEGS.getName(), Slot.FEET.getName(), Slot.PROJECTILE.getName());

		new CommandAPICommand("editattr")
			.withPermission(perms)
			.withArguments(attributeArg)
			.withArguments(amountArg)
			.withArguments(operationArg)
			.withArguments(slotArg)
			.executesPlayer((player, args) -> {
			ItemStack item = getHeldItemAndSendErrors(player);
			if (item == null) {
				return;
			}
			String attribute = args.getByArgument(attributeArg);
			double amount = args.getByArgument(amountArg);
			String operationString = args.getByArgument(operationArg);
			Operation operation = Operation.getOperation(operationString);
			if (operation == null) {
				throw CommandAPI.failWithString("Invalid operation " + operationString);
			}
			String slotString = args.getByArgument(slotArg);
			Slot slot = Slot.getSlot(slotString);
			if (slot == null) {
				throw CommandAPI.failWithString("Invalid slot " + slotString);
			}

			if ((operationString.equals("add") && attribute.contains("Multiply")) || (operationString.equals("multiply") && attribute.contains("Add"))) {
				return;
			}

			if (operationString.equals("add") && attribute.contains("ProjectileSpeed")) {
				player.sendMessage("You are using the wrong type of Proj Speed, do multiply");
				return;
			}

			AttributeType type = AttributeType.getAttributeType(attribute);
			if (type != null) {
				if (amount != 0) {
					ItemStatUtils.addAttribute(item, type, amount, operation, slot);
				} else {
					ItemStatUtils.removeAttribute(item, type, operation, slot);
				}
			}

			ItemUpdateHelper.generateItemStats(item);
				Plugin.getInstance().mItemStatManager.updateStats(player);
		}).register();
	}

	public static void registerRemoveCommand() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.removestats");

		new CommandAPICommand("removestats").withPermission(perms).executesPlayer((player, args) -> {
			ItemStack item = getHeldItemAndSendErrors(player);
			if (item == null) {
				return;
			}

			// remove all enchantments and attributes even though we clear everything later because some use vanilla mechanics that persist
			for (EnchantmentType ench : EnchantmentType.values()) {
				ItemStatUtils.removeEnchantment(item, ench);
			}

			for (AttributeType attr : AttributeType.values()) {
				for (Operation op : Operation.values()) {
					for (Slot slot : Slot.values()) {
						ItemStatUtils.removeAttribute(item, attr, op, slot);
					}
				}
			}

			NBTItem nbt = new NBTItem(item);
			nbt.removeKey(ItemStatUtils.MONUMENTA_KEY);
			item.setItemMeta(nbt.getItem().getItemMeta());
			item.lore(null);

			ItemMeta meta = item.getItemMeta();
			for (Enchantment ench : Registry.ENCHANTMENT) {
				meta.removeEnchant(ench);
			}
			meta.removeItemFlags(ItemFlag.values());
			item.setItemMeta(meta);

			// Copied from nbti command to set the attributes to an empty list instead of removing them entirely
			ItemModifier.setItemStackModifiers(item, new ArrayList<>());
			ItemUpdateHelper.addDummyAttributeIfNeeded(item);

			ItemUtils.setPlainLore(item);

			Plugin.getInstance().mItemStatManager.updateStats(player);
		}).register();
	}

	public static void registerCopyCommand() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.copystats");

		new CommandAPICommand("copystats").withPermission(perms).executesPlayer((player, args) -> {
			ItemStack item = getHeldItemAndSendErrors(player);
			if (item == null) {
				return;
			}

			ItemStack offhand = player.getInventory().getItemInOffHand();
			if (offhand.getType() == Material.AIR) {
				player.sendMessage(Component.text("Must be holding an item in your offhand to copy from.", NamedTextColor.RED));
				return;
			}

			ReadableNBT offhandNBT = NBT.readNbt(offhand);
			ReadableNBT enchantments = ItemStatUtils.getEnchantments(offhandNBT);
			ReadableNBT infusions = ItemStatUtils.getInfusions(offhandNBT);
			ReadableNBTList<ReadWriteNBT> attributes = ItemStatUtils.getAttributes(offhandNBT);

			NBT.modify(item, nbt -> {
				ItemStatUtils.setEnchantments(nbt, enchantments);
				ItemStatUtils.setInfusions(nbt, infusions);
				ItemStatUtils.setAttributes(nbt, attributes);
			});

			ItemUpdateHelper.generateItemStats(item);
		}).register();
	}

	public static void registerColorCommand() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.color");

		new CommandAPICommand("color").withPermission(perms)
			.withArguments(locationArg)
			.executes((sender, args) -> {
			Location location = Location.getLocation(args.getByArgument(locationArg));
			Component message = Component.empty().append(location.getDisplay()).append(Component.text(" (" + location.getColor().asHexString() + ")")).hoverEvent(HoverEvent.showText(Component.text("Click to copy hex code to clipboard"))).clickEvent(ClickEvent.copyToClipboard(location.getColor().asHexString()));
			sender.sendMessage(message);
		}).register();

		new CommandAPICommand("color").withPermission(perms).withArguments(new LiteralArgument("list")).executes((sender, args) -> {
			Component message = Component.empty();
			for (Location location : Location.values()) {
				message = message.append(Component.text(location.getName(), location.getColor()).hoverEvent(HoverEvent.showText(Component.text("Click to copy hex code to clipboard"))).clickEvent(ClickEvent.copyToClipboard(location.getColor().asHexString())));
				message = message.append(Component.text(" "));
			}
			sender.sendMessage(message);
		}).register();
	}

	public static void registerDelveInfusionTypeCommand() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.delveinfusiontype");

		new CommandAPICommand("delveinfusiontype").withPermission(perms).withArguments(new LiteralArgument("get")).executesPlayer((player, args) -> {
			ItemStack item = getHeldItemAndSendErrors(player);
			if (item == null) {
				return;
			}
			DelveInfusionUtils.DelveInfusionMaterial type = DelveInfusionUtils.getDelveInfusionMaterial(item);
			player.sendMessage(type.mItemNamePlural);
		}).register();

		MultiLiteralArgument typeArg = new MultiLiteralArgument("type", Arrays.stream(DelveInfusionUtils.DelveInfusionMaterial.values()).map(m -> m.mLabel).toArray(String[]::new));

		new CommandAPICommand("delveinfusiontype").withPermission(perms)
			.withArguments(new LiteralArgument("set"), typeArg)
			.executesPlayer((player, args) -> {
			ItemStack item = getHeldItemAndSendErrors(player);
			if (item == null) {
				return;
			}
			String label = args.getByArgument(typeArg);
			for (DelveInfusionUtils.DelveInfusionMaterial m : DelveInfusionUtils.DelveInfusionMaterial.values()) {
				if (m.mLabel.equals(label)) {
					DelveInfusionUtils.setDelveInfusionMaterial(item, m);
					return;
				}
			}
			player.sendMessage(Component.text("Failed to find Delve Infusion Material " + label, NamedTextColor.RED));
		}).register();
	}

	private static @Nullable ItemStack getHeldItemAndSendErrors(Player player) {
		return getHeldItemAndSendErrors(player, true);
	}

	private static @Nullable ItemStack getHeldItemAndSendErrors(Player player, boolean requireCreative) {
		if (requireCreative && player.getGameMode() != GameMode.CREATIVE) {
			player.sendMessage(Component.text("Must be in creative mode to use this command!", NamedTextColor.RED));
			return null;
		}
		ItemStack item = player.getInventory().getItemInMainHand();
		if (item.getType() == Material.AIR) {
			player.sendMessage(Component.text("Must be holding an item!", NamedTextColor.RED));
			return null;
		}
		return item;
	}

	private static Argument<String> getLocationArgument() {
		Location[] locationsRaw = Location.values();
		String[] locations = new String[locationsRaw.length];
		for (int i = 0; i < locations.length; i++) {
			locations[i] = locationsRaw[i].getName();
		}

		return new StringArgument("location").replaceSuggestions(ArgumentSuggestions.strings(locations));
	}

}
