package com.playmonumenta.plugins.itemindex;

import java.util.ArrayList;
import java.util.LinkedHashMap;

import javax.annotation.Nullable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.Enchantment;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.ItemUtils;

import org.bukkit.Material;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;

public class ItemIndexCommand {
	private static final String COMMAND = "mi";

	public static void register() {
		registerFixIndexItems();
		registerIndex();
		registerParseItem();
		registerChest();
		registerNew();
		registerCommit();
		registerRemove();
		registerReload();
		registerEdit();
	}

	private static void registerFixIndexItems() {
		//mi fixIndexItems
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("fixIndexItems", new LiteralArgument("fixIndexItems"));
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player p = CommandUtils.getPlayerFromSender(sender);
				if (p == null) {
					return;
				}
				MonumentaItem[] items = Plugin.getInstance().mItemManager.getItemArray();
				for (int i = 0; i < items.length; i++) {
					p.sendActionBar("fixing index items...   " + (i + 1) + " / " + items.length);
					MonumentaItem item = items[i];
					ItemStack itemStack = item.toItemStack();
					item = MonumentaItem.fromItemStack(itemStack);
					Plugin.getInstance().mItemManager.addToManager(item);
				}
				p.sendMessage("Items fixed ;3");
			})
			.register();
	}

	private static void registerParseItem() {
		//mi parseItem
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("parseItem", new LiteralArgument("parseItem"));
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player p = CommandUtils.getPlayerFromSender(sender);
				if (p == null) {
					return;
				}
				ItemStack mainhand = p.getInventory().getItemInMainHand();
				if (mainhand.getAmount() == 0 || mainhand.getType() == Material.AIR) {
					return;
				}
				MonumentaItem item = new MonumentaItem();
				item.setDefaultValues();
				item.setEdits((new ItemStackParser(mainhand).parse()));
				ItemStack out = item.toItemStack();
				p.getInventory().addItem(out);
				p.sendMessage("the converted item has been added to your inventory. make sure the parsed data is correct, and do /mi commit when everything is done as wanted");
			})
			.register();
	}

	private static void registerChest() {
		//mi chest <action>
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("chest", new LiteralArgument("chest"));
		arguments.put("action", new StringArgument().overrideSuggestions(new String[]{"parseItem", "commit"}));
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player p = CommandUtils.getPlayerFromSender(sender);
				if (p == null) {
					return;
				}
				Block b = p.getTargetBlock(5);
				if (b != null && (b.getType() == Material.CHEST || b.getType() == Material.TRAPPED_CHEST)) {
					Inventory inv = ((Chest) b.getState()).getBlockInventory();
					int changes = 0;
					if (args[0].equals("parseItem")) {
						for (int i = 0; i < inv.getSize(); i++) {
							ItemStack inputStack = inv.getItem(i);
							if (inputStack == null || inputStack.getType() == Material.AIR) {
								continue;
							}
							changes++;
							MonumentaItem item = new MonumentaItem();
							item.setDefaultValues();
							item.setEdits(new ItemStackParser(inputStack).parse());
							inv.setItem(i, item.toItemStack());
						}
						p.sendMessage("parsed and edited " + changes + "items. make sure they are correct, then commit them");
						return;
					} else if (args[0].equals("commit")) {
						for (int i = 0; i < inv.getSize(); i++) {
							ItemStack inputStack = inv.getItem(i);
							if (inputStack == null || inputStack.getType() == Material.AIR) {
								continue;
							}
							MonumentaItem item = Plugin.getInstance().mItemManager.getMMItemWithEdits(inputStack);
							if (item == null) {
								continue;
							}
							item.preCalc();
							Plugin.getInstance().mItemManager.addToManager(item);
							changes++;
						}
						p.sendMessage("commited " + changes + "items.");
						return;
					}
				}
				p.sendMessage("You must be looking at a chest to do this. careful: the chest contents will be overwritten");
			})
			.register();
	}

	private static void registerNew() {
		//mi new
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("new", new LiteralArgument("new"));
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player p = CommandUtils.getPlayerFromSender(sender);
				if (p == null) {
					sender.sendMessage("command must be launched from a player");
					return;
				}
				MonumentaItem i = new MonumentaItem();
				i.setDefaultValues();
				updateItemInHand(i, p);
				p.sendMessage("you've been given a new item, ready for edits. when you want to add it to the Index, do /mi commit");
			})
			.register();
	}

	private static void registerCommit() {
		//mi commit
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("commit", new LiteralArgument("commit"));
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player p = commandSecurities(sender);
				if (p == null) {
					return;
				}
				MonumentaItem item = Plugin.getInstance().mItemManager.getMMItemWithEdits(p.getInventory().getItemInMainHand());
				if (item == null) {
					return;
				}
				item.preCalc();
				Plugin.getInstance().mItemManager.addToManager(item);
				updateItemInHandNoTimestamps(item, p);
				p.sendMessage("Index entry for " + item.getMaterial().toString().toLowerCase() + ":" + item.getNameColorless() +
					" has been successfully edited");
			})
			.register();
	}

	private static void registerIndex() {
		//mi index
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("index", new LiteralArgument("index"));
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(arguments)
			.executes((sender, args) -> {
				if (!(sender instanceof Player)) {
					return;
				}
				Plugin.getInstance().mIndexInventoryManager.openIndex((Player) sender);
			})
			.register();
	}

	private static void registerRemove() {
		//mi enableEdit
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("remove", new LiteralArgument("remove"));
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player p = commandSecurities(sender);
				if (p == null) {
					return;
				}
				MonumentaItem item = Plugin.getInstance().mItemManager.getMMItemWithEdits(p.getInventory().getItemInMainHand());
				if (item == null) {
					p.sendMessage("no item has been found in the index that matches the current mainHand item");
					return;
				}
				Plugin.getInstance().mItemManager.remove(item);
				p.sendMessage("item " + item.getMaterial().toString().toLowerCase() + ":" + item.getNameColorless() +
					"has been successfully removed from index.");
			})
			.register();
	}

	private static void registerReload() {
		//mi enableEdit
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("reload", new LiteralArgument("reload"));
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player p = commandSecurities(sender, false);
				if (p == null) {
					return;
				}
				Plugin.getInstance().mItemManager = new ItemManager();
				p.sendMessage("monumenta index reloaded ( " + Plugin.getInstance().mItemManager.getItemArray().length + "items )");
			})
			.register();
	}

	private static void registerEdit() {
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("edit", new LiteralArgument("edit"));
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player p = commandSecurities(sender);
				if (p == null) {
					return;
				}
				MonumentaItem item = itemSecurities(p);
				if (item == null) {
					return;
				}
				p.sendMessage(item.toColoredRawEditInfo());
				updateItemInHandNoTimestamps(item, p);
			})
			.register();
		registerEditName();
		registerEditMaterial();
		registerEditRegion();
		registerEditTier();
		registerEditLocation();
		registerEditLore();
		registerEditEnchant();
		registerEditColor();
		registerEditAttribute();
		registerEditArmorMaterial();
		registerEditIsMagicWand();
		registerEditUnbreakable();
		registerEditDurability();
		registerEditOnConsume();
		registerEditBannerMeta();
		registerEditCraftingMaterialKind();
		registerEditBook();
		registerEditQuestID();
	}

	private static void registerEditName() {
		//mi edit name <value>
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("edit", new LiteralArgument("edit"));
		arguments.put("name", new LiteralArgument("name"));
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player p = commandSecurities(sender);
				if (p == null) {
					return;
				}
				MonumentaItem item = itemSecurities(p);
				if (item == null) {
					return;
				}
				item.edit().setName("");
				item.edit().setOldName(item.getNameRaw());
				updateItemInHand(item, p);
			})
			.register();
		arguments.put("value", new GreedyStringArgument());
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player p = commandSecurities(sender);
				if (p == null) {
					return;
				}
				MonumentaItem item = itemSecurities(p);
				if (item == null) {
					return;
				}
				item.edit().setName((String)args[0]);
				item.edit().setOldName(item.getNameRaw());
				updateItemInHand(item, p);
			})
			.register();
	}

	private static void registerEditMaterial() {
		//mi edit material <value>
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("edit", new LiteralArgument("edit"));
		arguments.put("material", new LiteralArgument("material"));
		arguments.put("value", new StringArgument().overrideSuggestions(ItemUtils.getBukkitMaterialStringArray()));
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player p = commandSecurities(sender);
				if (p == null) {
					return;
				}
				MonumentaItem item = itemSecurities(p);
				if (item == null) {
					return;
				}
				Material mat = Material.valueOf(((String)args[0]).toUpperCase());
				item.edit().setMaterial(mat);
				item.edit().setOldMaterial(item.getMaterial());
				updateItemInHand(item, p);
			})
			.register();
	}

	private static void registerEditRegion() {
		//mi edit material <value>
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("edit", new LiteralArgument("edit"));
		arguments.put("region", new LiteralArgument("region"));
		arguments.put("value", new StringArgument().overrideSuggestions(Region.valuesLowerCase()));
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player p = commandSecurities(sender);
				if (p == null) {
					return;
				}
				MonumentaItem item = itemSecurities(p);
				if (item == null) {
					return;
				}
				Region r = Region.valueOf(((String)args[0]).toUpperCase());
				item.edit().setRegion(r);
				updateItemInHand(item, p);
			})
			.register();
	}

	private static void registerEditTier() {
		//mi edit material <value>
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("edit", new LiteralArgument("edit"));
		arguments.put("tier", new LiteralArgument("tier"));
		arguments.put("value", new StringArgument().overrideSuggestions(ItemTier.valuesLowerCase()));
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player p = commandSecurities(sender);
				if (p == null) {
					return;
				}
				MonumentaItem item = itemSecurities(p);
				if (item == null) {
					return;
				}
				ItemTier tier = ItemTier.valueOf(((String)args[0]).toUpperCase());
				item.edit().setTier(tier);
				updateItemInHand(item, p);
			})
			.register();
	}

	private static void registerEditLocation() {
		//mi edit material <value>
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("edit", new LiteralArgument("edit"));
		arguments.put("location", new LiteralArgument("location"));
		arguments.put("value", new StringArgument().overrideSuggestions(ItemTier.valuesLowerCase()));
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player p = commandSecurities(sender);
				if (p == null) {
					return;
				}
				MonumentaItem item = itemSecurities(p);
				if (item == null) {
					return;
				}
				ItemLocation loc = ItemLocation.valueOf(((String)args[0]).toUpperCase());
				item.edit().setLocation(loc);
				updateItemInHand(item, p);
			})
			.register();
	}

	private static void registerEditLore() {
		//mi edit name <value>
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("edit", new LiteralArgument("edit"));
		arguments.put("lore", new LiteralArgument("lore"));
		arguments.put("index", new IntegerArgument(1));
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player p = commandSecurities(sender);
				if (p == null) {
					return;
				}
				MonumentaItem item = itemSecurities(p);
				if (item == null) {
					return;
				}
				item.edit().setLoreLine((Integer)args[0] - 1, null);
				updateItemInHand(item, p);
			})
			.register();
		arguments.put("value", new GreedyStringArgument());
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player p = commandSecurities(sender);
				if (p == null) {
					return;
				}
				MonumentaItem item = itemSecurities(p);
				if (item == null) {
					return;
				}
				item.edit().setLoreLine((Integer)args[0] - 1, (String)args[1]);
				updateItemInHand(item, p);
			})
			.register();
	}

	private static void registerEditEnchant() {
		//mi edit enchant <enchant_id> <level>
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("edit", new LiteralArgument("edit"));
		arguments.put("enchant", new LiteralArgument("enchant"));
		arguments.put("id", new StringArgument().overrideSuggestions(Enchantment.valuesLowerCase()));
		arguments.put("value", new IntegerArgument(0));
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player p = commandSecurities(sender);
				if (p == null) {
					return;
				}
				MonumentaItem item = itemSecurities(p);
				if (item == null) {
					return;
				}
				Enchantment ench = Enchantment.valueOf(((String)args[0]).toUpperCase());
				item.edit().setEnchantLevel(ench, (int)args[1]);
				updateItemInHand(item, p);
			})
			.register();
	}

	private static void registerEditColor() {
		//mi edit color <value>
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("edit", new LiteralArgument("edit"));
		arguments.put("color", new LiteralArgument("color"));
		arguments.put("value", new StringArgument());
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player p = commandSecurities(sender);
				if (p == null) {
					return;
				}
				MonumentaItem item = itemSecurities(p);
				if (item == null) {
					return;
				}
				int rgb = Integer.parseInt((String)args[0], 16);
				int[] color = {rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255};
				item.edit().setColor(color);
				updateItemInHand(item, p);
			})
			.register();
	}

	private static String[] getAttributeOperations() {
		ArrayList<String> out = new ArrayList<>();
		for (AttributeModifier.Operation a : AttributeModifier.Operation.values()) {
			out.add(a.toString().toLowerCase());
			out.add(String.valueOf(a.ordinal()));
		}
		return out.toArray(new String[0]);
	}

	private static void registerEditAttribute() {
		//mi edit attribute <attribute_name> <operation> <amount> <slot>
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("edit", new LiteralArgument("edit"));
		arguments.put("attribute", new LiteralArgument("attribute"));
		arguments.put("attribute_name", new StringArgument().overrideSuggestions(Attribute.valuesAsStringArray()));
		arguments.put("Operation", new StringArgument().overrideSuggestions(getAttributeOperations()));
		arguments.put("amount", new DoubleArgument());
		arguments.put("slot", new StringArgument().overrideSuggestions(EquipmentSlot.valuesAsStringArray()));
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player p = commandSecurities(sender);
				if (p == null) {
					return;
				}
				MonumentaItem item = itemSecurities(p);
				if (item == null) {
					return;
				}
				Attribute attrib = Attribute.valueOf(((String)args[0]).toUpperCase());
				AttributeModifier.Operation op = AttributeModifier.Operation.ADD_NUMBER;
				if (args[1].equals("1") || args[1].equals(AttributeModifier.Operation.ADD_SCALAR.toString().toLowerCase())) {
					op = AttributeModifier.Operation.ADD_SCALAR;
				} else if (args[1].equals("2") || args[1].equals(AttributeModifier.Operation.MULTIPLY_SCALAR_1.toString().toLowerCase())) {
					op = AttributeModifier.Operation.MULTIPLY_SCALAR_1;
				}
				Double amount = (Double)args[2];
				EquipmentSlot slot = EquipmentSlot.valueOf(((String)args[3]).toUpperCase());
				item.edit().setAttribute(slot, attrib, op, amount);
				updateItemInHand(item, p);
			})
			.register();
	}

	private static void registerEditArmorMaterial() {
		//mi edit armorMaterial <value>
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("edit", new LiteralArgument("edit"));
		arguments.put("armorMaterial", new LiteralArgument("armorMaterial"));
		arguments.put("value", new StringArgument().overrideSuggestions(ArmorMaterial.valuesAsStringArray()));
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player p = commandSecurities(sender);
				if (p == null) {
					return;
				}
				MonumentaItem item = itemSecurities(p);
				if (item == null) {
					return;
				}
				ArmorMaterial mat = ArmorMaterial.valueOf(((String)args[0]).toUpperCase());
				item.edit().setArmorMaterialOverride(mat);
				updateItemInHand(item, p);
			})
			.register();
	}

	private static void registerEditIsMagicWand() {
		//mi edit isMagicWand <value>
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("edit", new LiteralArgument("edit"));
		arguments.put("isMagicWand", new LiteralArgument("isMagicWand"));
		arguments.put("value", new BooleanArgument());
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player p = commandSecurities(sender);
				if (p == null) {
					return;
				}
				MonumentaItem item = itemSecurities(p);
				if (item == null) {
					return;
				}
				item.edit().setIsMagicWand((Boolean)args[0]);
				updateItemInHand(item, p);
			})
			.register();
	}

	private static void registerEditUnbreakable() {
		//mi edit unbreakable <value>
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("edit", new LiteralArgument("edit"));
		arguments.put("unbreakable", new LiteralArgument("unbreakable"));
		arguments.put("value", new BooleanArgument());
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player p = commandSecurities(sender);
				if (p == null) {
					return;
				}
				MonumentaItem item = itemSecurities(p);
				if (item == null) {
					return;
				}
				item.edit().setUnbreakable((Boolean)args[0]);
				updateItemInHand(item, p);
			})
			.register();
	}

	private static void registerEditDurability() {
		//mi edit durability <value>
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("edit", new LiteralArgument("edit"));
		arguments.put("durability", new LiteralArgument("durability"));
		arguments.put("value", new IntegerArgument());
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player p = commandSecurities(sender);
				if (p == null) {
					return;
				}
				MonumentaItem item = itemSecurities(p);
				if (item == null) {
					return;
				}
				item.edit().setDurability((Integer)args[0]);
				updateItemInHand(item, p);
			})
			.register();
	}

	private static void registerEditOnConsume() {
		//mi edit onConsume <effect> <potency> <duration>
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("edit", new LiteralArgument("edit"));
		arguments.put("onConsume", new LiteralArgument("onConsume"));
		arguments.put("effect", new StringArgument().overrideSuggestions(PassiveEffect.valuesAsStringArray()));
		arguments.put("potency", new IntegerArgument());
		arguments.put("duration", new IntegerArgument());
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player p = commandSecurities(sender);
				if (p == null) {
					return;
				}
				MonumentaItem item = itemSecurities(p);
				if (item == null) {
					return;
				}
				PassiveEffect effect = PassiveEffect.valueOf(((String)args[0]).toUpperCase());
				Integer potency = (Integer)args[1];
				Integer duration = (Integer)args[2];
				item.edit().setOnConsume(effect, potency, duration);
				updateItemInHand(item, p);
			})
			.register();
	}

	private static void registerEditBannerMeta() {
		//mi edit applyBannerMeta
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("edit", new LiteralArgument("edit"));
		arguments.put("bannerMeta", new LiteralArgument("applyBannerMeta"));
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player p = commandSecurities(sender);
				if (p == null) {
					return;
				}
				MonumentaItem item = itemSecurities(p);
				if (item == null) {
					return;
				}
				ItemStack offHandItem = p.getInventory().getItemInOffHand();
				if (!(offHandItem.getItemMeta() instanceof BannerMeta)) {
					p.sendMessage("To use that command, a banner item must be placed in your offhand");
					return;
				}
				item.edit().setBanner(offHandItem);
				updateItemInHand(item, p);
			})
			.register();
	}

	private static void registerEditBook() {
		// mi edit book applyPage <index>
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("edit", new LiteralArgument("edit"));
		arguments.put("book", new LiteralArgument("book"));
		arguments.put("action", new LiteralArgument("applyPage"));
		arguments.put("index", new IntegerArgument(1));
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player p = commandSecurities(sender);
				if (p == null) {
					return;
				}
				MonumentaItem item = itemSecurities(p);
				if (item == null) {
					return;
				}
				Integer index = (Integer)args[0] - 1;
				ItemStack offHandItem = p.getInventory().getItemInOffHand();
				MonumentaItem ohMMItem = Plugin.getInstance().mItemManager.getMMItemWithEdits(offHandItem);
				if (ohMMItem == null) {
					p.sendMessage("To use that command, you must have a MonumentaItem containing lore data in your offhand");
					return;
				}
				ohMMItem.preCalc();
				if (ohMMItem.getLore() == null || ohMMItem.getLore().size() == 0) {
					p.sendMessage("To use that command, you must have a MonumentaItem containing lore data in your offhand");
					return;
				}
				ArrayList<String> loreLines = new ArrayList<>();
				for (int i = 0; i <= ohMMItem.getLore().lastKey(); i++) {
					loreLines.add(ohMMItem.getLore().getOrDefault(i, ""));
				}
				item.edit().setBookPageContent(index, String.join("\n", loreLines.toArray(new String[0])));
				updateItemInHand(item, p);
			})
			.register();

		// mi edit book extractPage <index>
		arguments.put("action", new LiteralArgument("extractPage"));
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player p = commandSecurities(sender);
				if (p == null) {
					return;
				}
				MonumentaItem item = itemSecurities(p);
				if (item == null) {
					return;
				}
				Integer index = (Integer)args[0] - 1;
				MonumentaItem out = new MonumentaItem();
				out.setDefaultValues();
				out.edit().setName(item.getName() + " - Page " + (index + 1));
				String page = item.getBookPage(index - 1);
				int i = 0;
				if (page == null) {
					page = "";
				}
				for (String s : page.split("\n")) {
					out.edit().setLoreLine(i, s);
					i++;
				}
				p.getInventory().setItemInOffHand(out.toItemStack());
				updateItemInHand(item, p);
			})
			.register();

		// mi edit book author <value>
		arguments.put("action", new LiteralArgument("author"));
		arguments.remove("index");
		arguments.put("value", new GreedyStringArgument());
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player p = commandSecurities(sender);
				if (p == null) {
					return;
				}
				MonumentaItem item = itemSecurities(p);
				if (item == null) {
					return;
				}
				item.edit().setBookAuthor((String)args[0]);
				updateItemInHand(item, p);
			})
			.register();
	}

	private static void registerEditCraftingMaterialKind() {
		//mi edit CraftingMaterialKind <value>
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("edit", new LiteralArgument("edit"));
		arguments.put("craftingMaterialKind", new LiteralArgument("craftingMaterialKind"));
		arguments.put("value", new StringArgument().overrideSuggestions(CraftingMaterialKind.valuesAsStringArray()));
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player p = commandSecurities(sender);
				if (p == null) {
					return;
				}
				MonumentaItem item = itemSecurities(p);
				if (item == null) {
					return;
				}
				CraftingMaterialKind k = CraftingMaterialKind.valueOf(((String)args[0]).toUpperCase());
				item.edit().setCraftingMaterialKind(k);
				updateItemInHand(item, p);
			})
			.register();
	}

	private static void registerEditQuestID() {
		//mi edit questID <value>
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("edit", new LiteralArgument("edit"));
		arguments.put("QuestID", new LiteralArgument("QuestID"));
		arguments.put("value", new StringArgument());
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(arguments)
			.executes((sender, args) -> {
				Player p = commandSecurities(sender);
				if (p == null) {
					return;
				}
				MonumentaItem item = itemSecurities(p);
				if (item == null) {
					return;
				}
				item.edit().setQuestID((String)args[0]);
				updateItemInHand(item, p);
			})
			.register();
	}

	@Nullable
	private static Player commandSecurities(CommandSender sender) {
		return commandSecurities(sender, true);
	}

	@Nullable
	private static Player commandSecurities(CommandSender sender, boolean requiresItem) {
		Player p = CommandUtils.getPlayerFromSender(sender);
		if (p == null) {
			sender.sendMessage("Command must be launched from a Player");
			return null;
		}
		ItemStack item = p.getInventory().getItemInMainHand();
		if (requiresItem && (item.getType() == Material.AIR || item.getAmount() == 0)) {
			p.sendMessage("You must hold an item in hand to use /mi edit");
			return null;
		}
		return p;
	}

	private static MonumentaItem itemSecurities(Player p) {
		MonumentaItem out = Plugin.getInstance().mItemManager.getMMItemWithEdits(p.getInventory().getItemInMainHand());
		return out;
	}

	private static void updateItemInHand(MonumentaItem item, Player p) {
		item.edit().updateLastEditedTimestamp();
		item.edit().setLastEditor(p.getDisplayName());
		updateItemInHandNoTimestamps(item, p);
	}

	private static void updateItemInHandNoTimestamps(MonumentaItem item, Player p) {
		// set mainhand item
		int amount = p.getInventory().getItemInMainHand().getAmount();
		ItemStack out = item.toItemStack();
		out.setAmount(amount == 0 ? 1 : amount);
		p.getInventory().setItemInMainHand(out);
		p.sendMessage("item successfully edited");
	}
}
