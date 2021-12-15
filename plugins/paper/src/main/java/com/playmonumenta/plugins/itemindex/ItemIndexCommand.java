package com.playmonumenta.plugins.itemindex;

import java.util.ArrayList;
import java.util.TreeMap;

import org.bukkit.Material;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.checkerframework.checker.nullness.qual.Nullable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.plugins.utils.ItemUtils;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.DoubleArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;

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
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(new MultiLiteralArgument("fixIndexItems"))
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
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(new MultiLiteralArgument("parseItem"))
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
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(new MultiLiteralArgument("chest"))
			.withArguments(new StringArgument("action").overrideSuggestions(new String[]{"parseItem", "commit"}))
			.executes((sender, args) -> {
				Player p = CommandUtils.getPlayerFromSender(sender);
				if (p == null) {
					return;
				}
				Block b = p.getTargetBlock(5);
				if (b != null && (b.getType() == Material.CHEST || b.getType() == Material.TRAPPED_CHEST)) {
					Inventory inv = ((Chest) b.getState()).getBlockInventory();
					int changes = 0;
					if (args[1].equals("parseItem")) {
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
					} else if (args[1].equals("commit")) {
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
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(new MultiLiteralArgument("new"))
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
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(new MultiLiteralArgument("commit"))
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
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(new MultiLiteralArgument("index"))
			.executes((sender, args) -> {
				if (!(sender instanceof Player)) {
					return;
				}
				Plugin.getInstance().mIndexInventoryManager.openIndex((Player) sender);
			})
			.register();
	}

	private static void registerRemove() {
		//mi remove
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(new MultiLiteralArgument("remove"))
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
		//mi reload
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(new MultiLiteralArgument("reload"))
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
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(new MultiLiteralArgument("edit"))
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
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(new MultiLiteralArgument("edit"))
			.withArguments(new MultiLiteralArgument("name"))
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
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(new MultiLiteralArgument("edit"))
			.withArguments(new MultiLiteralArgument("name"))
			.withArguments(new GreedyStringArgument("value"))
			.executes((sender, args) -> {
				Player p = commandSecurities(sender);
				if (p == null) {
					return;
				}
				MonumentaItem item = itemSecurities(p);
				if (item == null) {
					return;
				}
				item.edit().setName((String)args[2]);
				item.edit().setOldName(item.getNameRaw());
				updateItemInHand(item, p);
			})
			.register();
	}

	private static void registerEditMaterial() {
		//mi edit material <value>
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(new MultiLiteralArgument("edit"))
			.withArguments(new MultiLiteralArgument("material"))
			.withArguments(new StringArgument("value").overrideSuggestions(ItemUtils.getBukkitMaterialStringArray()))
			.executes((sender, args) -> {
				Player p = commandSecurities(sender);
				if (p == null) {
					return;
				}
				MonumentaItem item = itemSecurities(p);
				if (item == null) {
					return;
				}
				Material mat = Material.valueOf(((String)args[2]).toUpperCase());
				item.edit().setMaterial(mat);
				item.edit().setOldMaterial(item.getMaterial());
				updateItemInHand(item, p);
			})
			.register();
	}

	private static void registerEditRegion() {
		//mi edit material <value>
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(new MultiLiteralArgument("edit"))
			.withArguments(new MultiLiteralArgument("region"))
			.withArguments(new StringArgument("value").overrideSuggestions(Region.valuesLowerCase()))
			.executes((sender, args) -> {
				Player p = commandSecurities(sender);
				if (p == null) {
					return;
				}
				MonumentaItem item = itemSecurities(p);
				if (item == null) {
					return;
				}
				Region r = Region.valueOf(((String)args[2]).toUpperCase());
				item.edit().setRegion(r);
				updateItemInHand(item, p);
			})
			.register();
	}

	private static void registerEditTier() {
		//mi edit material <value>
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(new MultiLiteralArgument("edit"))
			.withArguments(new MultiLiteralArgument("tier"))
			.withArguments(new StringArgument("value").overrideSuggestions(ItemTier.valuesLowerCase()))
			.executes((sender, args) -> {
				Player p = commandSecurities(sender);
				if (p == null) {
					return;
				}
				MonumentaItem item = itemSecurities(p);
				if (item == null) {
					return;
				}
				ItemTier tier = ItemTier.valueOf(((String)args[2]).toUpperCase());
				item.edit().setTier(tier);
				updateItemInHand(item, p);
			})
			.register();
	}

	private static void registerEditLocation() {
		//mi edit material <value>
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(new MultiLiteralArgument("edit"))
			.withArguments(new MultiLiteralArgument("location"))
			.withArguments(new StringArgument("value").overrideSuggestions(ItemTier.valuesLowerCase()))
			.executes((sender, args) -> {
				Player p = commandSecurities(sender);
				if (p == null) {
					return;
				}
				MonumentaItem item = itemSecurities(p);
				if (item == null) {
					return;
				}
				ItemLocation loc = ItemLocation.valueOf(((String)args[2]).toUpperCase());
				item.edit().setLocation(loc);
				updateItemInHand(item, p);
			})
			.register();
	}

	private static void registerEditLore() {
		//mi edit lore <index> <value>
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(new MultiLiteralArgument("edit"))
			.withArguments(new MultiLiteralArgument("lore"))
			.withArguments(new IntegerArgument("index", 1))
			.executes((sender, args) -> {
				Player p = commandSecurities(sender);
				if (p == null) {
					return;
				}
				MonumentaItem item = itemSecurities(p);
				if (item == null) {
					return;
				}
				item.edit().setLoreLine((Integer)args[2] - 1, null);
				updateItemInHand(item, p);
			})
			.register();
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(new MultiLiteralArgument("edit"))
			.withArguments(new MultiLiteralArgument("lore"))
			.withArguments(new IntegerArgument("index", 1))
			.withArguments(new GreedyStringArgument("value"))
			.executes((sender, args) -> {
				Player p = commandSecurities(sender);
				if (p == null) {
					return;
				}
				MonumentaItem item = itemSecurities(p);
				if (item == null) {
					return;
				}
				item.edit().setLoreLine((Integer)args[2] - 1, (String)args[3]);
				updateItemInHand(item, p);
			})
			.register();
	}

	private static void registerEditEnchant() {
		//mi edit enchant <enchant_id> <level>
		// new CommandAPICommand(COMMAND)
		// 	.withPermission(CommandPermission.fromString("monumenta.mi"))
		// 	.withArguments(new MultiLiteralArgument("edit"))
		// 	.withArguments(new MultiLiteralArgument("enchant"))
		// 	.withArguments(new StringArgument("id").overrideSuggestions(CustomEnchantment.valuesLowerCase()))
		// 	.withArguments(new IntegerArgument("value", 0))
		// 	.executes((sender, args) -> {
		// 		Player p = commandSecurities(sender);
		// 		if (p == null) {
		// 			return;
		// 		}
		// 		MonumentaItem item = itemSecurities(p);
		// 		if (item == null) {
		// 			return;
		// 		}
		// 		CustomEnchantment ench = CustomEnchantment.valueOf(((String)args[2]).toUpperCase());
		// 		item.edit().setEnchantLevel(ench, (int)args[3]);
		// 		updateItemInHand(item, p);
		// 	})
		// 	.register();
	}

	private static void registerEditColor() {
		//mi edit color <value>
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(new MultiLiteralArgument("edit"))
			.withArguments(new MultiLiteralArgument("color"))
			.withArguments(new StringArgument("value"))
			.executes((sender, args) -> {
				Player p = commandSecurities(sender);
				if (p == null) {
					return;
				}
				MonumentaItem item = itemSecurities(p);
				if (item == null) {
					return;
				}
				int rgb = Integer.parseInt((String)args[2], 16);
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
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(new MultiLiteralArgument("edit"))
			.withArguments(new MultiLiteralArgument("attribute"))
			.withArguments(new StringArgument("attribute_name").overrideSuggestions(Attribute.valuesAsStringArray()))
			.withArguments(new StringArgument("Operation").overrideSuggestions(getAttributeOperations()))
			.withArguments(new DoubleArgument("amount"))
			.withArguments(new StringArgument("slot").overrideSuggestions(EquipmentSlot.valuesAsStringArray()))
			.executes((sender, args) -> {
				Player p = commandSecurities(sender);
				if (p == null) {
					return;
				}
				MonumentaItem item = itemSecurities(p);
				if (item == null) {
					return;
				}
				Attribute attrib = Attribute.valueOf(((String)args[2]).toUpperCase());
				AttributeModifier.Operation op = AttributeModifier.Operation.ADD_NUMBER;
				if (args[3].equals("1") || args[3].equals(AttributeModifier.Operation.ADD_SCALAR.toString().toLowerCase())) {
					op = AttributeModifier.Operation.ADD_SCALAR;
				} else if (args[3].equals("2") || args[3].equals(AttributeModifier.Operation.MULTIPLY_SCALAR_1.toString().toLowerCase())) {
					op = AttributeModifier.Operation.MULTIPLY_SCALAR_1;
				}
				Double amount = (Double)args[4];
				EquipmentSlot slot = EquipmentSlot.valueOf(((String)args[5]).toUpperCase());
				item.edit().setAttribute(slot, attrib, op, amount);
				updateItemInHand(item, p);
			})
			.register();
	}

	private static void registerEditArmorMaterial() {
		//mi edit armorMaterial <value>
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(new MultiLiteralArgument("edit"))
			.withArguments(new MultiLiteralArgument("armorMaterial"))
			.withArguments(new StringArgument("value").overrideSuggestions(ArmorMaterial.valuesAsStringArray()))
			.executes((sender, args) -> {
				Player p = commandSecurities(sender);
				if (p == null) {
					return;
				}
				MonumentaItem item = itemSecurities(p);
				if (item == null) {
					return;
				}
				ArmorMaterial mat = ArmorMaterial.valueOf(((String)args[2]).toUpperCase());
				item.edit().setArmorMaterialOverride(mat);
				updateItemInHand(item, p);
			})
			.register();
	}

	private static void registerEditIsMagicWand() {
		//mi edit isMagicWand <value>
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(new MultiLiteralArgument("edit"))
			.withArguments(new MultiLiteralArgument("isMagicWand"))
			.withArguments(new BooleanArgument("value"))
			.executes((sender, args) -> {
				Player p = commandSecurities(sender);
				if (p == null) {
					return;
				}
				MonumentaItem item = itemSecurities(p);
				if (item == null) {
					return;
				}
				item.edit().setIsMagicWand((Boolean)args[2]);
				updateItemInHand(item, p);
			})
			.register();
	}

	private static void registerEditUnbreakable() {
		//mi edit unbreakable <value>
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(new MultiLiteralArgument("edit"))
			.withArguments(new MultiLiteralArgument("unbreakable"))
			.withArguments(new BooleanArgument("value"))
			.executes((sender, args) -> {
				Player p = commandSecurities(sender);
				if (p == null) {
					return;
				}
				MonumentaItem item = itemSecurities(p);
				if (item == null) {
					return;
				}
				item.edit().setUnbreakable((Boolean)args[2]);
				updateItemInHand(item, p);
			})
			.register();
	}

	private static void registerEditDurability() {
		//mi edit durability <value>
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(new MultiLiteralArgument("edit"))
			.withArguments(new MultiLiteralArgument("durability"))
			.withArguments(new IntegerArgument("value"))
			.executes((sender, args) -> {
				Player p = commandSecurities(sender);
				if (p == null) {
					return;
				}
				MonumentaItem item = itemSecurities(p);
				if (item == null) {
					return;
				}
				item.edit().setDurability((Integer)args[2]);
				updateItemInHand(item, p);
			})
			.register();
	}

	private static void registerEditOnConsume() {
		//mi edit onConsume <effect> <potency> <duration>
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(new MultiLiteralArgument("edit"))
			.withArguments(new MultiLiteralArgument("onConsume"))
			.withArguments(new StringArgument("effect").overrideSuggestions(PassiveEffect.valuesAsStringArray()))
			.withArguments(new IntegerArgument("potency"))
			.withArguments(new IntegerArgument("duration"))
			.executes((sender, args) -> {
				Player p = commandSecurities(sender);
				if (p == null) {
					return;
				}
				MonumentaItem item = itemSecurities(p);
				if (item == null) {
					return;
				}
				PassiveEffect effect = PassiveEffect.valueOf(((String)args[2]).toUpperCase());
				Integer potency = (Integer)args[3];
				Integer duration = (Integer)args[4];
				item.edit().setOnConsume(effect, potency, duration);
				updateItemInHand(item, p);
			})
			.register();
	}

	private static void registerEditBannerMeta() {
		//mi edit applyBannerMeta
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(new MultiLiteralArgument("edit"))
			.withArguments(new MultiLiteralArgument("applyBannerMeta"))
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
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(new MultiLiteralArgument("edit"))
			.withArguments(new MultiLiteralArgument("book"))
			.withArguments(new MultiLiteralArgument("applyPage"))
			.withArguments(new IntegerArgument("index", 1))
			.executes((sender, args) -> {
				Player p = commandSecurities(sender);
				if (p == null) {
					return;
				}
				MonumentaItem item = itemSecurities(p);
				if (item == null) {
					return;
				}
				Integer index = (Integer) args[3] - 1;
				ItemStack offHandItem = p.getInventory().getItemInOffHand();
				MonumentaItem ohMMItem = Plugin.getInstance().mItemManager.getMMItemWithEdits(offHandItem);
				if (ohMMItem == null) {
					p.sendMessage("To use that command, you must have a MonumentaItem containing lore data in your offhand");
					return;
				}
				ohMMItem.preCalc();
				TreeMap<Integer, String> lore = ohMMItem.getLore();
				if (lore == null || lore.size() == 0) {
					p.sendMessage("To use that command, you must have a MonumentaItem containing lore data in your offhand");
					return;
				}
				ArrayList<String> loreLines = new ArrayList<>();
				for (int i = 0; i <= lore.lastKey(); i++) {
					loreLines.add(lore.getOrDefault(i, ""));
				}
				item.edit().setBookPageContent(index, String.join("\n", loreLines.toArray(new String[0])));
				updateItemInHand(item, p);
			})
			.register();

		// mi edit book extractPage <index>
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(new MultiLiteralArgument("edit"))
			.withArguments(new MultiLiteralArgument("book"))
			.withArguments(new MultiLiteralArgument("applyPage"))
			.withArguments(new IntegerArgument("index", 1))
			.withArguments(new MultiLiteralArgument("extractPage"))
			.executes((sender, args) -> {
				Player p = commandSecurities(sender);
				if (p == null) {
					return;
				}
				MonumentaItem item = itemSecurities(p);
				if (item == null) {
					return;
				}
				Integer index = (Integer)args[3] - 1;
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
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(new MultiLiteralArgument("edit"))
			.withArguments(new MultiLiteralArgument("book"))
			.withArguments(new MultiLiteralArgument("author"))
			.withArguments(new GreedyStringArgument("value"))
			.executes((sender, args) -> {
				Player p = commandSecurities(sender);
				if (p == null) {
					return;
				}
				MonumentaItem item = itemSecurities(p);
				if (item == null) {
					return;
				}
				item.edit().setBookAuthor((String)args[3]);
				updateItemInHand(item, p);
			})
			.register();
	}

	private static void registerEditCraftingMaterialKind() {
		//mi edit CraftingMaterialKind <value>
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(new MultiLiteralArgument("edit"))
			.withArguments(new MultiLiteralArgument("craftingMaterialKind"))
			.withArguments(new StringArgument("value").overrideSuggestions(CraftingMaterialKind.valuesAsStringArray()))
			.executes((sender, args) -> {
				Player p = commandSecurities(sender);
				if (p == null) {
					return;
				}
				MonumentaItem item = itemSecurities(p);
				if (item == null) {
					return;
				}
				CraftingMaterialKind k = CraftingMaterialKind.valueOf(((String)args[2]).toUpperCase());
				item.edit().setCraftingMaterialKind(k);
				updateItemInHand(item, p);
			})
			.register();
	}

	private static void registerEditQuestID() {
		//mi edit questID <value>
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString("monumenta.mi"))
			.withArguments(new MultiLiteralArgument("edit"))
			.withArguments(new MultiLiteralArgument("QuestID"))
			.withArguments(new StringArgument("value"))
			.executes((sender, args) -> {
				Player p = commandSecurities(sender);
				if (p == null) {
					return;
				}
				MonumentaItem item = itemSecurities(p);
				if (item == null) {
					return;
				}
				item.edit().setQuestID((String)args[2]);
				updateItemInHand(item, p);
			})
			.register();
	}

	private static @Nullable Player commandSecurities(CommandSender sender) throws WrapperCommandSyntaxException {
		return commandSecurities(sender, true);
	}

	private static @Nullable Player commandSecurities(CommandSender sender, boolean requiresItem) throws WrapperCommandSyntaxException {
		Player p = CommandUtils.getPlayerFromSender(sender);
		ItemStack item = p.getInventory().getItemInMainHand();
		if (requiresItem && (item.getType() == Material.AIR || item.getAmount() == 0)) {
			p.sendMessage("You must hold an item in hand to use /mi edit");
			return null;
		}
		return p;
	}

	private static @Nullable MonumentaItem itemSecurities(Player p) {
		return Plugin.getInstance().mItemManager.getMMItemWithEdits(p.getInventory().getItemInMainHand());
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
