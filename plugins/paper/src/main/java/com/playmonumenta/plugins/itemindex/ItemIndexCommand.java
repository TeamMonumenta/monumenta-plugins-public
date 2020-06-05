package com.playmonumenta.plugins.itemindex;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.CommandUtils;
import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.DoubleArgument;
import io.github.jorelali.commandapi.api.arguments.GreedyStringArgument;
import io.github.jorelali.commandapi.api.arguments.LiteralArgument;
import io.github.jorelali.commandapi.api.arguments.StringArgument;
import org.bukkit.Material;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import com.playmonumenta.plugins.enchantments.Enchantment;
import com.playmonumenta.plugins.utils.ItemUtils;
import io.github.jorelali.commandapi.api.arguments.DynamicSuggestedStringArgument;
import io.github.jorelali.commandapi.api.arguments.IntegerArgument;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashMap;

public class ItemIndexCommand {
	public static void register() {
		registerFixIndexItems();
		registerIndex();
		registerParseItem();
		registerChest();
		registerNew();
		registerCommit();
		registerRemove();
		registerEdit();
	}

	private static void registerFixIndexItems() {
		//mi fixIndexItems
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("fixIndexItems", new LiteralArgument("fixIndexItems"));
		CommandAPI.getInstance().register("mi",
			CommandPermission.fromString("monumenta.mi"),
			arguments,
			(sender, args) -> {
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
			}
		);
	}

	private static void registerParseItem() {
		//mi parseItem
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("parseItem", new LiteralArgument("parseItem"));
		CommandAPI.getInstance().register("mi",
			CommandPermission.fromString("monumenta.mi"),
			arguments,
			(sender, args) -> {
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
			}
		);
	}

	private static void registerChest() {
		//mi chest <action>
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("chest", new LiteralArgument("chest"));
		arguments.put("action", new DynamicSuggestedStringArgument(() -> new String[]{"parseItem", "commit"}));
		CommandAPI.getInstance().register("mi",
			CommandPermission.fromString("monumenta.mi"),
			arguments,
			(sender, args) -> {
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
							MonumentaItem mMItem = new ItemStackParser(inputStack).parse();
							inv.setItem(i, mMItem.toItemStack());
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
							item.mergeEdits();
							Plugin.getInstance().mItemManager.addToManager(item);
							changes++;
						}
						p.sendMessage("commited " + changes + "items.");
						return;
					}
				}
				p.sendMessage("You must be looking at a chest to do this. careful: the chest contents will be overwritten");
			}
		);
	}

	private static void registerNew() {
		//mi new
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("new", new LiteralArgument("new"));
		CommandAPI.getInstance().register("mi",
			CommandPermission.fromString("monumenta.mi"),
			arguments,
			(sender, args) -> {
				Player p = CommandUtils.getPlayerFromSender(sender);
				if (p == null) {
					sender.sendMessage("command must be launched from a player");
					return;
				}
				MonumentaItem i = new MonumentaItem();
				i.setDefaultValues();
				updateItemInHand(i, p);
				p.sendMessage("you've been given a new item, ready for edits. when you want to add it to the Index, do /mi commit");
			}
		);
	}

	private static void registerCommit() {
		//mi commit
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("commit", new LiteralArgument("commit"));
		CommandAPI.getInstance().register("mi",
			CommandPermission.fromString("monumenta.mi"),
			arguments,
			(sender, args) -> {
				Player p = commandSecurities(sender);
				if (p == null) {
					return;
				}
				MonumentaItem item = Plugin.getInstance().mItemManager.getMMItemWithEdits(p.getInventory().getItemInMainHand());
				if (item == null) {
					return;
				}
				item.mergeEdits();
				Plugin.getInstance().mItemManager.addToManager(item);
				updateItemInHandNoTimestamps(item, p);
				p.sendMessage("Index entry for " + item.getMaterial().toString().toLowerCase() + ":" + item.getNameColorless() +
					" has been successfully edited");
			}
		);
	}

	private static void registerIndex() {
		//mi index
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("index", new LiteralArgument("index"));
		CommandAPI.getInstance().register("mi",
			CommandPermission.fromString("monumenta.mi"),
			arguments,
			(sender, args) -> {
				if (!(sender instanceof Player)) {
					return;
				}
				Plugin.getInstance().mIndexInventoryManager.openIndex((Player) sender);
			}
		);
	}

	private static void registerRemove() {
		//mi enableEdit
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("remove", new LiteralArgument("remove"));
		CommandAPI.getInstance().register("mi",
			CommandPermission.fromString("monumenta.mi"),
			arguments,
			(sender, args) -> {
				Player p = commandSecurities(sender);
				if (p == null) {
					return;
				}
				MonumentaItem item = Plugin.getInstance().mItemManager.getIndexMMItem(p.getInventory().getItemInMainHand());
				if (item == null) {
					p.sendMessage("no item has been found in the index that matches the current mainHand item");
					return;
				}
				Plugin.getInstance().mItemManager.remove(item);
				p.sendMessage("item " + item.getMaterial().toString().toLowerCase() + ":" + item.getNameColorless() +
					"has been successfully removed from index.");
			}
		);
	}

	private static void registerEdit() {
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("edit", new LiteralArgument("edit"));
		CommandAPI.getInstance().register("mi",
			CommandPermission.fromString("monumenta.mi"),
			arguments,
			(sender, args) -> {
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
			}
		);
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
	}

	private static void registerEditName() {
		//mi edit name <value>
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("edit", new LiteralArgument("edit"));
		arguments.put("name", new LiteralArgument("name"));
		CommandAPI.getInstance().register("mi",
			CommandPermission.fromString("monumenta.mi"),
			arguments,
			(sender, args) -> {
				Player p = commandSecurities(sender);
				if (p == null) {
					return;
				}
				MonumentaItem item = itemSecurities(p);
				if (item == null) {
					return;
				}
				item.edit().setName("");
				updateItemInHand(item, p);
			}
		);
		arguments.put("value", new GreedyStringArgument());
		CommandAPI.getInstance().register("mi",
			CommandPermission.fromString("monumenta.mi"),
			arguments,
			(sender, args) -> {
				Player p = commandSecurities(sender);
				if (p == null) {
					return;
				}
				MonumentaItem item = itemSecurities(p);
				if (item == null) {
					return;
				}
				item.edit().setName((String)args[0]);
				updateItemInHand(item, p);
			}
		);
	}

	private static void registerEditMaterial() {
		//mi edit material <value>
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("edit", new LiteralArgument("edit"));
		arguments.put("material", new LiteralArgument("material"));
		arguments.put("value", new DynamicSuggestedStringArgument(ItemUtils::getBukkitMaterialStringArray));
		CommandAPI.getInstance().register("mi",
			CommandPermission.fromString("monumenta.mi"),
			arguments,
			(sender, args) -> {
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
				updateItemInHand(item, p);
			}
		);
	}

	private static void registerEditRegion() {
		//mi edit material <value>
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("edit", new LiteralArgument("edit"));
		arguments.put("region", new LiteralArgument("region"));
		arguments.put("value", new DynamicSuggestedStringArgument(Region::valuesLowerCase));
		CommandAPI.getInstance().register("mi",
			CommandPermission.fromString("monumenta.mi"),
			arguments,
			(sender, args) -> {
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
			}
		);
	}

	private static void registerEditTier() {
		//mi edit material <value>
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("edit", new LiteralArgument("edit"));
		arguments.put("tier", new LiteralArgument("tier"));
		arguments.put("value", new DynamicSuggestedStringArgument(ItemTier::valuesLowerCase));
		CommandAPI.getInstance().register("mi",
			CommandPermission.fromString("monumenta.mi"),
			arguments,
			(sender, args) -> {
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
			}
		);
	}

	private static void registerEditLocation() {
		//mi edit material <value>
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("edit", new LiteralArgument("edit"));
		arguments.put("location", new LiteralArgument("location"));
		arguments.put("value", new DynamicSuggestedStringArgument(ItemLocation::valuesLowerCase));
		CommandAPI.getInstance().register("mi",
			CommandPermission.fromString("monumenta.mi"),
			arguments,
			(sender, args) -> {
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
			}
		);
	}

	private static void registerEditLore() {
		//mi edit name <value>
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("edit", new LiteralArgument("edit"));
		arguments.put("lore", new LiteralArgument("lore"));
		arguments.put("index", new IntegerArgument(1));
		CommandAPI.getInstance().register("mi",
			CommandPermission.fromString("monumenta.mi"),
			arguments,
			(sender, args) -> {
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
			}
		);
		arguments.put("value", new GreedyStringArgument());
		CommandAPI.getInstance().register("mi",
			CommandPermission.fromString("monumenta.mi"),
			arguments,
			(sender, args) -> {
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
			}
		);
	}

	private static void registerEditEnchant() {
		//mi edit enchant <enchant_id> <level>
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("edit", new LiteralArgument("edit"));
		arguments.put("enchant", new LiteralArgument("enchant"));
		arguments.put("id", new DynamicSuggestedStringArgument(Enchantment::valuesLowerCase));
		arguments.put("value", new IntegerArgument(0));
		CommandAPI.getInstance().register("mi",
			CommandPermission.fromString("monumenta.mi"),
			arguments,
			(sender, args) -> {
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
			}
		);
	}

	private static void registerEditColor() {
		//mi edit color <value>
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("edit", new LiteralArgument("edit"));
		arguments.put("color", new LiteralArgument("color"));
		arguments.put("value", new StringArgument());
		CommandAPI.getInstance().register("mi",
			CommandPermission.fromString("monumenta.mi"),
			arguments,
			(sender, args) -> {
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
			}
		);
	}

	private static void registerEditAttribute() {
		//mi edit attribute <attribute_name> <operation> <amount> <slot>
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("edit", new LiteralArgument("edit"));
		arguments.put("attribute", new LiteralArgument("attribute"));
		arguments.put("attribute_name", new DynamicSuggestedStringArgument(Attribute::valuesAsStringArray));
		arguments.put("Operation", new DynamicSuggestedStringArgument(() -> {
			ArrayList<String> out = new ArrayList<>();
			for (AttributeModifier.Operation a : AttributeModifier.Operation.values()) {
				out.add(a.toString().toLowerCase());
				out.add(String.valueOf(a.ordinal()));
			}
			return out.toArray(new String[0]);
		}));
		arguments.put("amount", new DoubleArgument());
		arguments.put("slot", new DynamicSuggestedStringArgument(EquipmentSlot::valuesAsStringArray));
		CommandAPI.getInstance().register("mi",
			CommandPermission.fromString("monumenta.mi"),
			arguments,
			(sender, args) -> {
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
			}
		);
	}

	private static void registerEditArmorMaterial() {
		//mi edit armorMaterial <value>
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put("edit", new LiteralArgument("edit"));
		arguments.put("armorMaterial", new LiteralArgument("armorMaterial"));
		arguments.put("value", new DynamicSuggestedStringArgument(ArmorMaterial::valuesAsStringArray));
		CommandAPI.getInstance().register("mi",
			CommandPermission.fromString("monumenta.mi"),
			arguments,
			(sender, args) -> {
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
			}
		);
	}

	@Nullable
	private static Player commandSecurities(CommandSender sender) {
		Player p = CommandUtils.getPlayerFromSender(sender);
		if (p == null) {
			sender.sendMessage("Command must be launched from a Player");
			return null;
		}
		ItemStack item = p.getInventory().getItemInMainHand();
		if (item.getType() == Material.AIR || item.getAmount() == 0) {
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
