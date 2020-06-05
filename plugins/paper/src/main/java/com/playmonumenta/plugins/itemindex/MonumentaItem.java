package com.playmonumenta.plugins.itemindex;

import com.goncalomb.bukkit.mylib.reflect.NBTUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.Enchantment;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import de.tr7zw.nbtapi.NBTItem;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import javax.annotation.Nullable;
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
	public MonumentaItem mEdits;
	private String mName;
	private Material mMaterial;
	private Region mRegion;
	private ItemTier mTier;
	private ItemLocation mLoc;
	private TreeMap<Integer, String> mLoreMap;
	private TreeMap<Enchantment, Integer> mEnchantMap;
	private int[] mColor;
	private String mLastEditedBy;
	private Long mLastEditedTimestamp;
	private TreeMap<EquipmentSlot, TreeMap<Attribute, TreeMap<AttributeModifier.Operation, Double>>> mAttributeDataMap;
	private ArmorMaterial mArmorMaterial;
	private ArmorMaterial mArmorMaterialOverride;

	public MonumentaItem() {
		this.mEdits = null;
		this.mName = null;
		this.mMaterial = null;
		this.mRegion = null;
		this.mTier = null;
		this.mLoc = null;
		this.mLoreMap = null;
		this.mEnchantMap = null;
		this.mColor = null;
		this.mLastEditedBy = null;
		this.mLastEditedTimestamp = null;
		this.mAttributeDataMap = null;
		this.mArmorMaterial = null;
		this.mArmorMaterialOverride = null;
	}

	public MonumentaItem clone() {
		MonumentaItem out = new MonumentaItem();
		out.mName = this.mName;
		out.mMaterial = this.mMaterial;
		out.mRegion = this.mRegion;
		out.mTier = this.mTier;
		out.mLoc = this.mLoc;
		if (this.mLoreMap != null) {
			out.mLoreMap = (TreeMap<Integer, String>)this.mLoreMap.clone();
		}
		if (this.mEnchantMap != null) {
			out.mEnchantMap = (TreeMap<Enchantment, Integer>)this.mEnchantMap.clone();
		}
		if (this.mColor != null) {
			out.mColor = this.mColor.clone();
		}
		out.mLastEditedBy = this.mLastEditedBy;
		out.mLastEditedTimestamp = this.mLastEditedTimestamp;
		out.mArmorMaterial = this.mArmorMaterial;
		out.mArmorMaterialOverride = this.mArmorMaterialOverride;
		if (this.mAttributeDataMap != null) {
			out.mAttributeDataMap = new TreeMap<>();
			for (Map.Entry<EquipmentSlot, TreeMap<Attribute, TreeMap<AttributeModifier.Operation, Double>>> entry1 : this.mAttributeDataMap.entrySet()) {
				EquipmentSlot slot = entry1.getKey();
				for (Map.Entry<Attribute, TreeMap<AttributeModifier.Operation, Double>> entry2 : entry1.getValue().entrySet()) {
					Attribute attribute = entry2.getKey();
					for (Map.Entry<AttributeModifier.Operation, Double> entry3: entry2.getValue().entrySet()) {
						AttributeModifier.Operation operation = entry3.getKey();
						Double amount = entry3.getValue();
						out.setAttribute(slot, attribute, operation, amount);
					}
				}
			}
		}
		if (this.mEdits != null) {
			out.mEdits = this.mEdits.clone();
		}
		return out;
	}

	public void mergeEdits() {
		MonumentaItem e = this.mEdits;
		if (e != null) {
			e.preCalc();
			if (e.mName != null) {
				this.setName(e.getNameRaw());
			}
			if (e.mMaterial != null) {
				this.setMaterial(e.getMaterial());
			}
			if (e.mRegion != null) {
				this.setRegion(e.getRegion());
			}
			if (e.mTier != null) {
				this.setTier(e.getTier());
			}
			if (e.mLoc != null) {
				this.setLocation(e.getLocation());
			}
			if (e.mLoreMap != null && e.mLoreMap.size() > 0) {
				for (int i = 0; i <= e.mLoreMap.lastKey(); i++) {
					if (e.mLoreMap.get(i) != null) {
						this.setLoreLine(i, e.getLore().get(i));
					}
				}
			}
			if (e.mEnchantMap != null) {
				for (Map.Entry<Enchantment, Integer> entry : e.getEnchantMap().entrySet()) {
					this.setEnchantLevel(entry.getKey(), entry.getValue());
				}
			}
			if (e.mColor != null) {
				this.setColor(e.getColor());
			}

			if (e.mArmorMaterialOverride != null) {
				this.setArmorMaterialOverride(e.getArmorMaterialOverride());
			}
			if (e.mAttributeDataMap != null) {
				for (Map.Entry<EquipmentSlot, TreeMap<Attribute, TreeMap<AttributeModifier.Operation, Double>>> entry1 : e.mAttributeDataMap.entrySet()) {
					EquipmentSlot slot = entry1.getKey();
					for (Map.Entry<Attribute, TreeMap<AttributeModifier.Operation, Double>> entry2 : entry1.getValue().entrySet()) {
						Attribute attribute = entry2.getKey();
						for (Map.Entry<AttributeModifier.Operation, Double> entry3: entry2.getValue().entrySet()) {
							AttributeModifier.Operation operation = entry3.getKey();
							Double amount = entry3.getValue();
							this.setAttribute(slot, attribute, operation, amount);
						}
					}
				}
			}
			if (e.mLastEditedBy != null) {
				this.setLastEditor(e.getLastEditor());
			}
			if (e.mLastEditedTimestamp != null) {
				this.mLastEditedTimestamp = e.getLastEditedTimestamp();
			}
			this.mEdits = null;
		}
	}

	private void preCalc() {
		// normal precalc
		this.mergeEdits();
	}


	public ItemStack toItemStack() {
		// init + material
		String jsonToStore = null;
		if (this.mEdits != null) {
			//differs from index entry. create a separate instance
			jsonToStore = new Gson().toJson(this.mEdits);
		}
		this.preCalc();
		ItemStack out = new ItemStack(this.getMaterial());
		ItemMeta meta = out.getItemMeta();
		ArrayList<String> loreLines = new ArrayList<>();

		// name
		meta.setDisplayName(this.getName() + "§r");

		// enchants
		if (this.mEnchantMap != null) {
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
		}

		// armor material
		if (this.getArmorMaterialOverride() != null) {
			if (this.getArmorMaterialOverride() != ArmorMaterial.NONE) {
				loreLines.add(this.getArmorMaterialOverride().getReadableString());
			}
		} else if (this.getArmorMaterial() != null && this.getArmorMaterial() != ArmorMaterial.NONE) {
			loreLines.add(this.getArmorMaterial().getReadableString());
		}

		// region + tier
		if (this.getRegion() != null && this.getTier() != null) {
			if (this.getRegion().getInt() >= 0 && this.getTier() != ItemTier.NONE) {
				loreLines.add(this.getRegion().getReadableString() + " : " + this.getTier().getReadableString());
			}
		}

		// location
		if (this.getLocation() != null && this.getLocation() != ItemLocation.NONE) {
			loreLines.add(this.getLocation().getReadableString());
		}

		// lore
		if (this.mLoreMap != null && this.mLoreMap.size() > 0) {
			for (int i = 0; i <= this.mLoreMap.lastKey(); i++) {
				String s = this.mLoreMap.getOrDefault(i, "");
				if (s.matches("^(&[k-o])?&[0-9a-e]")) {
					loreLines.add(s.replace('&', '§'));
				} else {
					loreLines.add(ChatColor.DARK_GRAY + s.replace('&', '§'));
				}
			}
		}

		// armor color
		if (this.getColor() != null) {
			System.out.println("colored");
			if (meta instanceof LeatherArmorMeta) {
				System.out.println("leatherArmorMeta");
				((LeatherArmorMeta)meta).setColor(Color.fromRGB(this.getColor()[0], this.getColor()[1], this.getColor()[2]));
			}
			if (this.isColorable()) {
				System.out.println("colorable");
			}
		}

		// attributes
		if (this.getAttributesMap() != null) {
			for (Map.Entry<EquipmentSlot, TreeMap<Attribute, TreeMap<AttributeModifier.Operation, Double>>> entry1 : this.mAttributeDataMap.entrySet()) {
				EquipmentSlot slot = entry1.getKey();
				loreLines.add("");
				loreLines.add(slot.getReadableString());
				for (Map.Entry<Attribute, TreeMap<AttributeModifier.Operation, Double>> entry2 : entry1.getValue().entrySet()) {
					Attribute attribute = entry2.getKey();
					for (Map.Entry<AttributeModifier.Operation, Double> entry3: entry2.getValue().entrySet()) {
						AttributeModifier.Operation operation = entry3.getKey();
						Double amount = entry3.getValue();
						loreLines.add(ItemUtils.buildAttributeLoreLine(slot, attribute, operation, amount));
						if (!attribute.isCustom()) {
							String attributeName = StringUtils.getAttributeName(attribute.getBukkitAttribute());
							meta.addAttributeModifier(attribute.getBukkitAttribute(), new AttributeModifier(UUID.randomUUID(), attributeName, amount, operation, slot.getBukkitSlot()));
						}
					}
				}
			}
		}

		// finish
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		meta.setLore(loreLines);
		out.setItemMeta(meta);
		if (jsonToStore != null) {
			// all this needs to be done at the end, even if the data is only available before converting to itemStack. dunno why. nbt magic
			NBTItem nbtOut = new NBTItem(out);
			nbtOut.setString("MonumentaItemEdits", jsonToStore);
			out = nbtOut.getItem();
		}
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
		out.append(this.toColoredRawEditInfoRaw());
		return out.toString();
	}


	public String toColoredRawEditInfoRaw() {
		StringBuilder out = new StringBuilder();
		String editable = ChatColor.GREEN + "" + ChatColor.BOLD;
		String readOnly = ChatColor.YELLOW + "" + ChatColor.BOLD;
		ChatColor reset = ChatColor.RESET;
		ChatColor unused = ChatColor.STRIKETHROUGH;
		ChatColor hidden = ChatColor.ITALIC;
		//out.append(String.format("\n", ));
		if (this.getNameRaw() != null) {
			out.append(String.format("%sName:%s %s (%s%s)\n", editable, reset, this.getNameRaw(), this.getName(), reset));
		}
		if (this.getMaterial() != null) {
			out.append(String.format("%sMaterial:%s %s (%s)\n", editable, reset, this.getMaterial(), this.getMaterial().getKey()));
		}
		if (this.getRegion() != null) {
			out.append(String.format("%s%sRegion:%s %s (%d : %s%s)\n", editable, this.getRegion() == Region.NONE || this.getTier() == ItemTier.NONE ? unused : "", reset, this.getRegion(), this.getRegion().getInt(), this.getRegion().getReadableString(), reset));
		}
		if (this.getTier() != null) {
			out.append(String.format("%s%sTier:%s %s (%s%s)\n", editable, this.getRegion() == Region.NONE || this.getTier() == ItemTier.NONE ? unused : "", reset, this.getTier(), this.getTier().getReadableString(), reset));
		}
		if (this.getLocation() != null) {
			out.append(String.format("%s%sLocation:%s %s (%s%s)\n", editable, this.getLocation() == ItemLocation.NONE ? unused : "",reset, this.getLocation(), this.getLocation().getReadableString(), reset));
		}
		if (this.getLore() != null) {
			out.append(String.format("%s%sLore:%s %s\n", editable, this.getLore().size() == 0 ? unused : "", reset, this.getLore().size() > 0 ? this.getLore().toString().replace(",", "\n              ").replace('{', '}').replace("}", "") : "None"));
		}
		if (this.getEnchantMap() != null) {
			out.append(String.format("%s%sEnchants:%s %s\n", editable, this.getEnchantMap().size() == 0 ? unused : "", reset, this.getEnchantMap().size() > 0 ? this.getEnchantMap().toString().replace(",", "\n              ").replace('{', '}').replace("}", "") : "None"));
		}
		if (this.getColor() != null) {
			out.append(String.format("%s%sColor:%s R:%d G:%d B:%d -> %s\n", editable, this.isColorable() ? "" : unused, reset, this.getColor()[0], this.getColor()[1], this.getColor()[2], String.format("#%02X%02X%02X", this.getColor()[0], this.getColor()[1], this.getColor()[2])));
		}
		if (this.getAttributesMap() != null) {
			out.append(String.format("%s%sAttributes:%s\n", editable, this.getAttributesMap().size() == 0 ? unused : "", reset));
			for (Map.Entry<EquipmentSlot, TreeMap<Attribute, TreeMap<AttributeModifier.Operation, Double>>> entry1 : this.mAttributeDataMap.entrySet()) {
				EquipmentSlot slot = entry1.getKey();
				for (Map.Entry<Attribute, TreeMap<AttributeModifier.Operation, Double>> entry2 : entry1.getValue().entrySet()) {
					Attribute attribute = entry2.getKey();
					for (Map.Entry<AttributeModifier.Operation, Double> entry3: entry2.getValue().entrySet()) {
						AttributeModifier.Operation operation = entry3.getKey();
						Double amount = entry3.getValue();
						out.append(String.format("%-8s %18s %17s %.3f\n", slot, attribute, operation, amount));
					}
				}
			}
		}
		if (this.getLastEditor() != null) {
			out.append(String.format("%s%sLastEditor:%s %s\n", readOnly, hidden, reset, this.getLastEditor()));
		}
		if (this.mLastEditedTimestamp != null) {
			out.append(String.format("%s%sLastEditionTime:%s %s (%s)\n", readOnly, hidden, reset, this.getLastEditTimeAsString(), this.getTimeSinceLastEditAsString()));
		}
		if (this.getArmorMaterial() != null) {
			out.append(String.format("%s%sArmorMaterial:%s %s (%s%s)\n", readOnly, this.getArmorMaterial() == ArmorMaterial.NONE || this.getArmorMaterialOverride() != null ? unused : "", reset, this.getArmorMaterial(), this.getArmorMaterial().getReadableString(), reset));
		}
		if (this.getArmorMaterialOverride() != null) {
			out.append(String.format("%sArmorMaterialOverride:%s %s (%s%s)\n", editable, reset, this.getArmorMaterialOverride(), this.getArmorMaterialOverride().getReadableString(), reset));
		}
		if (this.mEdits != null) {
			out.append("Edits differing from ItemIndex instance:\n");
			String[] split = this.mEdits.toColoredRawEditInfoRaw().split("\n");
			for (String s : split) {
				out.append(" + ").append(s).append("\n");
			}
		}
		return out.toString();
	}

	public void setDefaultValues() {
		this.mName = "&7New Item";
		this.mMaterial = Material.PAPER;
		this.mRegion = Region.MONUMENTA;
		this.mTier = ItemTier.DEV;
		this.mLastEditedBy = "System";
		this.mLastEditedTimestamp = LocalDateTime.now().toInstant(ZoneOffset.UTC).getEpochSecond();
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

	public MonumentaItem edit() {
		if (this.mEdits == null) {
			this.mEdits = new MonumentaItem();
		}
		return this.mEdits;
	}

	public String getName() {
		if (this.mName == null) {
			return "";
		}
		return this.mName.replace('&', '§');
	}

	public String getNameColorless() {
		return ChatColor.stripColor(this.getName());
	}

	@Nullable
	public String getNameRaw() {
		return this.mName;
	}

	@Nullable
	public Material getMaterial() {
		return this.mMaterial;
	}

	@Nullable
	public Region getRegion() {
		return this.mRegion;
	}

	@Nullable
	public ItemTier getTier() {
		return this.mTier;
	}

	@Nullable
	public ItemLocation getLocation() {
		return this.mLoc;
	}

	@Nullable
	public TreeMap<Integer, String> getLore() {
		return this.mLoreMap;
	}

	@Nullable
	public Map<Enchantment, Integer> getEnchantMap() {
		return this.mEnchantMap;
	}

	public int getEnchantLevel(Enchantment enchant) {
		if (this.getEnchantMap() == null) {
			return 0;
		}
		return this.getEnchantMap().getOrDefault(enchant, 0);
	}

	public boolean isColorable() {
		if (this.getMaterial() == null) {
			return false;
		}
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

	@Nullable
	public int[] getColor() {
		return this.mColor;
	}

	public long getLastEditedTimestamp() {
		if (this.mLastEditedTimestamp == null) {
			return 0;
		}
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

	@Nullable
	public String getLastEditor() {
		return this.mLastEditedBy;
	}

	@Nullable
	public TreeMap<EquipmentSlot, TreeMap<Attribute, TreeMap<AttributeModifier.Operation, Double>>> getAttributesMap() {
		return this.mAttributeDataMap;
	}

	public double getAttribute(EquipmentSlot slot, Attribute attribute, AttributeModifier.Operation operation) {
		if (this.getAttributesMap() == null) {
			return 0.0;
		}
		return this.getAttributesMap().getOrDefault(slot, new TreeMap<>()).getOrDefault(attribute, new TreeMap<>()).getOrDefault(operation, 0.0);
	}

	@Nullable
	public ArmorMaterial getArmorMaterial() {
		return this.mArmorMaterial;
	}

	@Nullable
	public ArmorMaterial getArmorMaterialOverride() {
		return this.mArmorMaterialOverride;
	}

	public void setEdits(MonumentaItem edits) {
		this.mEdits = edits;
	}

	public void setName(String name) {
		this.mName = name.replace('§', '&');
	}

	public void setMaterial(Material mMaterial) {
		this.mMaterial = mMaterial;
		this.computeArmorMaterial();
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

	public void setLore(TreeMap<Integer, String> lore) {
		this.mLoreMap = lore;
	}

	public void setLoreLine(int index, String str) {
		if (this.getLore() == null) {
			this.mLoreMap = new TreeMap<>();
		}
		this.mLoreMap.put(index, str);
	}

	public void setEnchantLevel(Enchantment enchant, int level) {
		if (this.getEnchantMap() == null) {
			this.mEnchantMap = new TreeMap<>();
		}
		this.mEnchantMap.put(enchant, level);
	}

	public void setEnchantMap(TreeMap<Enchantment, Integer> enchantMap) {
		this.mEnchantMap = enchantMap;
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
		if (this.getAttributesMap() == null) {
			this.mAttributeDataMap = new TreeMap<>();
		}
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
		if (attribute == Attribute.ARMOR || attribute == Attribute.TOUGHNESS) {
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

	public static MonumentaItem fromItemStack(ItemStack itemStack) {
		return Plugin.getInstance().mItemManager.getMMItemWithEdits(itemStack);
	}
}
