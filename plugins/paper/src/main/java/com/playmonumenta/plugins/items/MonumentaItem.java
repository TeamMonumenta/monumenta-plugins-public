package com.playmonumenta.plugins.items;

import com.goncalomb.bukkit.mylib.reflect.NBTUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.enums.ArmorMaterial;
import com.playmonumenta.plugins.enums.Enchantment;
import com.playmonumenta.plugins.enums.ItemLocation;
import com.playmonumenta.plugins.enums.ItemTier;
import com.playmonumenta.plugins.enums.Region;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

// describes a monumenta item
public class MonumentaItem {
	private String mName;
	private Material mMaterial;
	private boolean mEditable;
	private Region mRegion;
	private ItemTier mTier;
	private ItemLocation mLoc;
	private ArrayList<String> mLore;
	private TreeMap<Enchantment, Integer> mEnchantMap;
	private int[] mColor;
	private String mLastEditedBy;
	private long mLastEditedTimestamp;
	private TreeMap<EquipmentSlot, TreeMap<Attribute, TreeMap<AttributeModifier.Operation, Double>>> mAttributeDataMap;
	private ArmorMaterial mArmorMaterial;
	private ArmorMaterial mArmorMaterialOverride;

	public MonumentaItem() {
		this.mName = "&7New Item";
		this.mMaterial = Material.STONE_SWORD;
		this.mEditable = false;
		this.mRegion = Region.MONUMENTA;
		this.mTier = ItemTier.DEV;
		this.mLoc = ItemLocation.NONE;
		this.mLore = new ArrayList<>();
		this.mEnchantMap = new TreeMap<>();
		this.mColor = new int[]{160, 101, 64};
		this.mLastEditedBy = "System";
		this.mLastEditedTimestamp = LocalDateTime.now().toInstant(ZoneOffset.UTC).getEpochSecond();
		this.mAttributeDataMap = new TreeMap<>();
		this.mArmorMaterial = ArmorMaterial.NONE;
		this.mArmorMaterialOverride = ArmorMaterial.NONE;
	}

	private void preCalc() {
	}

	public ItemStack toItemStack() {
		// init + material
		ItemStack out = new ItemStack(this.getMaterial());
		ItemMeta meta = out.getItemMeta();
		ArrayList<String> loreLines = new ArrayList<>();
		this.preCalc();

		// name
		meta.setDisplayName(this.getName() + "§r");

		// enchants
		for (Enchantment e : Enchantment.values()) {
			if (this.mEnchantMap.containsKey(e)) {
				Integer v = this.mEnchantMap.get(e);
				if (v > 0) {
					String toAdd = e.getReadableString();
					if (!e.ignoresLevels()) {
						toAdd += " " + StringUtils.toRoman(v);
					}
					loreLines.add(toAdd);
					if (e.isBukkitEnchant()) {
						meta.addEnchant(e.getBukkitEnchantment(), v, true);
					}
				}
			}
		}

		// armor material
		if (this.getArmorMaterialOverride() != ArmorMaterial.NONE) {
			loreLines.add(this.getArmorMaterialOverride().getReadableString());
		} else if (this.getArmorMaterial() != ArmorMaterial.NONE) {
			loreLines.add(this.getArmorMaterial().getReadableString());
		}

		// region + tier
		if (this.mRegion.getInt() >= 0 && this.mTier != ItemTier.NONE) {
			loreLines.add(this.getRegion().getReadableString() + " : " + this.getTier().getReadableString());
		}

		// location
		if (this.mLoc != ItemLocation.NONE) {
			loreLines.add(this.mLoc.getReadableString());
		}

		// lore
		for (String s : this.mLore) {
			if (s.matches("^(&[k-o])?&[0-9a-e]")) {
				loreLines.add(s.replace('&', '§'));
			} else {
				loreLines.add(ChatColor.DARK_GRAY + s.replace('&', '§'));
			}
		}

		// armor color
		if (new ItemStack(this.getMaterial()).getItemMeta() instanceof LeatherArmorMeta) {
			((LeatherArmorMeta)meta).setColor(Color.fromRGB(this.mColor[0], this.mColor[1], this.mColor[2]));
		}

		// attributes
		for (Map.Entry<EquipmentSlot, TreeMap<Attribute, TreeMap<AttributeModifier.Operation, Double>>> entry1 : this.mAttributeDataMap.entrySet()) {
			EquipmentSlot slot = entry1.getKey();
			for (Map.Entry<Attribute, TreeMap<AttributeModifier.Operation, Double>> entry2 : entry1.getValue().entrySet()) {
				Attribute attribute = entry2.getKey();
				String attributeName = StringUtils.getAttributeName(attribute);
				for (Map.Entry<AttributeModifier.Operation, Double> entry3: entry2.getValue().entrySet()) {
					AttributeModifier.Operation operation = entry3.getKey();
					Double amount = entry3.getValue();
					meta.addAttributeModifier(attribute, new AttributeModifier(UUID.randomUUID(), attributeName, amount, operation, slot));
				}
			}
		}

		// finish
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		if (this.isEditable()) {
			String first = "";
			if (loreLines.size() > 0) {
				first = loreLines.remove(0);
			}
			loreLines.add(0, StringUtils.convertToInvisibleLoreLine(this.toJson()) + "§¬§¬§¬" + first);
		}
		meta.setLore(loreLines);
		out.setItemMeta(meta);
		return out.ensureServerConversions();
	}

	public String toLootTablePrettyJson() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String json = gson.toJson(this.toLootTableJsonElement());
		return json;
	}

	public String toColoredRawEditInfo() {
		StringBuilder out = new StringBuilder();
		String editable = ChatColor.GREEN + "" + ChatColor.BOLD;
		String readOnly = ChatColor.YELLOW + "" + ChatColor.BOLD;
		ChatColor reset = ChatColor.RESET;
		ChatColor unused = ChatColor.STRIKETHROUGH;
		ChatColor hidden = ChatColor.ITALIC;
		out.append(String.format("%sGreen: %sEditable Field\n", editable, reset));
		out.append(String.format("%sYellow: %sRead-Only Field\n", readOnly, reset));
		out.append(String.format("%sStrikeThrough:%s field is unused because of related values\n", unused, reset));
		out.append(String.format("%sItalic:%s the field's value is Hidden from players\n\n", hidden, reset));
		out.append("Raw Field values for held item:\n");
		//out.append(String.format("\n", ));
		out.append(String.format("%s%sEditable:%s %s\n", readOnly, hidden, reset, this.isEditable() ? "YES" : ChatColor.RED + "NO. This is not meant to happen. ggwp x9 report ray"));
		out.append(String.format("%sName:%s %s (%s%s)\n", editable, reset, this.getNameRaw(), this.getName(), reset));
		out.append(String.format("%sMaterial:%s %s (%s)\n", editable, reset, this.getMaterial(), this.getMaterial().getKey()));
		out.append(String.format("%s%sRegion:%s %s (%d : %s%s)\n", editable, this.getRegion() == Region.NONE || this.getTier() == ItemTier.NONE ? unused : "", reset, this.getRegion(), this.getRegion().getInt(), this.getRegion().getReadableString(), reset));
		out.append(String.format("%s%sTier:%s %s (%s%s)\n", editable, this.getRegion() == Region.NONE || this.getTier() == ItemTier.NONE ? unused : "", reset, this.getTier(), this.getTier().getReadableString(), reset));
		out.append(String.format("%s%sLocation:%s %s (%s%s)\n", editable, this.getLocation() == ItemLocation.NONE ? unused : "",reset, this.getLocation(), this.getLocation().getReadableString(), reset));
		out.append(String.format("%s%sLore:%s %s\n", editable, this.getLore().size() == 0 ? unused : "", reset, (this.getLore().size() > 0 ? "\"" + this.getLore().get(0) + "\"" : ChatColor.GRAY + "None")));
		for (int i = 1; i < this.getLore().size(); i++) {
			out.append(String.format("         \"%s\"\n", this.getLore().get(i)));
		}
		out.append(String.format("%s%sEnchants:%s %s\n", editable, this.getEnchantMap().size() == 0 ? unused : "", reset, this.getEnchantMap().size() > 0 ? this.getEnchantMap().toString().replace(",", "\n              ").replace('{', '}').replace("}", "") : "None"));
		out.append(String.format("%s%sColor:%s R:%d G:%d B:%d -> %s\n", editable, this.isColorable() ? "" : unused, reset, this.getColor()[0], this.getColor()[1], this.getColor()[2], String.format("#%02X%02X%02X", this.getColor()[0], this.getColor()[1], this.getColor()[2])));
		out.append(String.format("%s%sAttributes:%s\n", editable, this.getAttributesMap().size() == 0 ? unused : "", reset));
		for (Map.Entry<EquipmentSlot, TreeMap<Attribute, TreeMap<AttributeModifier.Operation, Double>>> entry1 : this.mAttributeDataMap.entrySet()) {
			EquipmentSlot slot = entry1.getKey();
			for (Map.Entry<Attribute, TreeMap<AttributeModifier.Operation, Double>> entry2 : entry1.getValue().entrySet()) {
				Attribute attribute = entry2.getKey();
				String attributeName = StringUtils.getAttributeName(attribute);
				for (Map.Entry<AttributeModifier.Operation, Double> entry3: entry2.getValue().entrySet()) {
					AttributeModifier.Operation operation = entry3.getKey();
					Double amount = entry3.getValue();
					out.append(String.format("%8s %28s %17s %.3f\n", slot, attributeName, operation, amount));
				}
			}
		}
		out.append(String.format("%s%sLastEditor:%s %s\n", readOnly, hidden, reset, this.getLastEditor()));
		out.append(String.format("%s%sLastEditionTime:%s %s (%s)\n", readOnly, hidden, reset, this.getLastEditTimeAsString(), this.getTimeSinceLastEditAsString()));

		return out.toString();


	}

	public JsonElement toLootTableJsonElement() {
		ItemStack stack = this.toItemStack();
		JsonObject function = new JsonObject();
		function.addProperty("function", "set_nbt");
		function.addProperty("tag", NBTUtils.getItemStackTag(stack).toString());
		JsonArray functions = new JsonArray();
		functions.add(function);
		JsonObject entry = new JsonObject();
		entry.addProperty("type", "item");
		entry.addProperty("weight", 1);
		entry.addProperty("name", stack.getType().getKey().toString());
		entry.add("functions", functions);
		JsonArray entries = new JsonArray();
		entries.add(entry);
		JsonObject pool = new JsonObject();
		pool.addProperty("rolls", 1);
		pool.add("entries", entries);
		JsonArray pools = new JsonArray();
		pools.add(pool);
		JsonObject root = new JsonObject();
		root.add("pools", pools);
		root.add("monumentaItem", this.toJsonElement());
		return root;
	}

	public JsonElement toJsonElement() {
		return new Gson().toJsonTree(this);
	}

	public String toPrettyJson() {
		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		return gson.toJson(this);
	}

	public String toJson() {
		return new Gson().toJson(this);
	}

	public String getName() {
		return this.mName.replace('&', '§');
	}

	public String getNameColorless() {
		return ChatColor.stripColor(this.getName());
	}

	public String getNameRaw() {
		return this.mName;
	}

	public Material getMaterial() {
		return this.mMaterial;
	}

	public Region getRegion() {
		return this.mRegion;
	}

	public ItemTier getTier() {
		return this.mTier;
	}

	public ItemLocation getLocation() {
		return this.mLoc;
	}

	public ArrayList<String> getLore() {
		return this.mLore;
	}

	public Map<Enchantment, Integer> getEnchantMap() {
		return this.mEnchantMap;
	}

	public int getEnchantLevel(Enchantment enchant) {
		return this.mEnchantMap.getOrDefault(enchant, 0);
	}

	public boolean isEditable() {
		return this.mEditable;
	}

	public boolean isColorable() {
		switch (this.getMaterial()) {
			case LEATHER_BOOTS:
			case LEATHER_LEGGINGS:
			case LEATHER_CHESTPLATE:
			case LEATHER_HELMET:
				return true;
			default:
				return false;
		}
	}

	public int[] getColor() {
		return this.mColor;
	}

	public long getLastEditedTimestamp() {
		return this.mLastEditedTimestamp;
	}

	public String getLastEditTimeAsString() {
		return new SimpleDateFormat("E, dd MMM yyyy HH:mm:ss z").format(new Date(this.getLastEditedTimestamp() * 1000));
	}

	public String getTimeSinceLastEditAsString() {
		long edit = this.getLastEditedTimestamp();
		long now = LocalDateTime.now().toInstant(ZoneOffset.UTC).getEpochSecond();
		long seconds = now - edit;
		long minutes = seconds / 60;
		seconds = seconds % 60;
		long hours = minutes / 60;
		minutes = minutes % 60;
		long days = hours / 24;
		hours = hours % 24;
		if (days > 1) {
			return days + " day(s) ago";
		} else if (hours > 0) {
			return hours + " hour(s) ago";
		} else if (minutes > 0) {
			return minutes + " minute(s) ago";
		} else {
			return seconds + " second(s) ago";
		}
	}

	public String getLastEditor() {
		return this.mLastEditedBy;
	}

	public TreeMap<EquipmentSlot, TreeMap<Attribute, TreeMap<AttributeModifier.Operation, Double>>> getAttributesMap() {
		return this.mAttributeDataMap;
	}

	public double getAttribute(EquipmentSlot slot, Attribute attribute, AttributeModifier.Operation operation) {
		return this.mAttributeDataMap.getOrDefault(slot, new TreeMap<>()).getOrDefault(attribute, new TreeMap<>()).getOrDefault(operation, 0.0);
	}

	public ArmorMaterial getArmorMaterial() {
		return this.mArmorMaterial;
	}

	public ArmorMaterial getArmorMaterialOverride() {
		return this.mArmorMaterialOverride;
	}

	public void setName(String name) {
		this.mName = name.replace('§', '&');
	}

	public void setMaterial(Material mMaterial) {
		this.mMaterial = mMaterial;
		this.computeArmorMaterial();
	}

	public void setEditable(boolean editable) {
		this.mEditable = editable;
	}

	public void setRegion(Region region) {
		this.mRegion = region;
	}

	public void setTier(ItemTier tier) {
		this.mTier = tier;
	}

	public void setLocation(ItemLocation loc) {
		this.mLoc = loc;
	}

	public void setLore(ArrayList<String> loreLines) {
		this.mLore = loreLines;
	}

	public void setLoreLine(int index, String str) {
		if (str != null) {
			if (this.mLore.size() <= index) {
				this.mLore.add(str);
			} else {
				this.mLore.set(index, str);
			}
		} else {
			this.mLore.remove(index);
		}
	}

	public void setEnchantLevel(Enchantment enchant, int level) {
		if (level == 0) {
			this.mEnchantMap.remove(enchant);
		} else {
			this.mEnchantMap.put(enchant, level);
		}
	}

	public void setColor(int[] color) {
		this.mColor = color;
	}

	public void updateLastEditedTimestamp() {
		this.mLastEditedTimestamp = LocalDateTime.now().toInstant(ZoneOffset.UTC).getEpochSecond();
	}

	public void setLastEditor(String name) {
		this.mLastEditedBy = name;
	}

	public void setAttribute(EquipmentSlot slot, Attribute attribute, AttributeModifier.Operation op, Double amount) {
		if (amount != 0) {
			// set mode
			TreeMap<Attribute, TreeMap<AttributeModifier.Operation, Double>> trunk = this.mAttributeDataMap.getOrDefault(slot, new TreeMap<>());
			TreeMap<AttributeModifier.Operation, Double> leave = trunk.getOrDefault(attribute, new TreeMap<>());
			leave.put(op, amount);
			trunk.put(attribute, leave);
			this.mAttributeDataMap.put(slot, trunk);
		} else {
			// delete mode
			TreeMap<Attribute, TreeMap<AttributeModifier.Operation, Double>> trunk = this.mAttributeDataMap.getOrDefault(slot, new TreeMap<>());
			TreeMap<AttributeModifier.Operation, Double> leave = trunk.getOrDefault(attribute, new TreeMap<>());
			leave.remove(op);
			if (leave.size() == 0) {
				trunk.remove(attribute);
				if (trunk.size() == 0) {
					this.mAttributeDataMap.remove(slot);
				}
			}
		}
		if (attribute == Attribute.GENERIC_ARMOR || attribute == Attribute.GENERIC_ARMOR_TOUGHNESS) {
			this.computeArmorMaterial();
		}
	}

	public void computeArmorMaterial() {
		if (!ItemUtils.wearable.contains(this.mMaterial)) {
			this.mArmorMaterial = ArmorMaterial.NONE;
			return;
		}
		for (ArmorMaterial m : ArmorMaterial.values()) {
			if (m == ArmorMaterial.NONE) {
				continue;
			}
			if (m.matchesMonumentaItem(this)) {
				this.mArmorMaterial = m;
				return;
			}
		}
		this.mArmorMaterial = ArmorMaterial.NONE;
	}

	public void setArmorMaterialOverride(ArmorMaterial armorMaterialOverride) {
		this.mArmorMaterialOverride = armorMaterialOverride;
	}
}
