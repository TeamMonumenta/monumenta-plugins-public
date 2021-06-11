package com.playmonumenta.plugins.itemindex;

import com.goncalomb.bukkit.mylib.reflect.NBTUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.Constants.Materials;
import com.playmonumenta.plugins.enchantments.CustomEnchantment;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import de.tr7zw.nbtapi.NBTItem;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Banner;
import org.bukkit.block.banner.Pattern;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;

import javax.annotation.Nullable;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

// describes a monumenta item
public class MonumentaItem {
	private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
	private static final Gson GSON_PRETTY = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

	private MonumentaItem mEdits;
	private String mName;
	private Material mMaterial;
	private Region mRegion;
	private ItemTier mTier;
	private ItemLocation mLoc;
	private TreeMap<Integer, String> mLoreMap;
	private TreeMap<CustomEnchantment, Integer> mEnchantMap;
	private int[] mColor;
	private String mLastEditedBy;
	private Long mLastEditedTimestamp;
	private TreeMap<EquipmentSlot, TreeMap<Attribute, TreeMap<AttributeModifier.Operation, Double>>> mAttributeDataMap;
	private ArmorMaterial mArmorMaterial;
	private ArmorMaterial mArmorMaterialOverride;
	private Boolean mIsMagicWand;
	private Boolean mUnbreakable;
	private Integer mBaseDurability;
	private String mOldName;
	private Material mOldMaterial;
	private TreeMap<PassiveEffect, TreeMap<Integer, Integer>> mOnConsumeMap;
	private ArrayList<Pattern> mBannerPatterns;
	private DyeColor mBannerBaseColor;
	private CraftingMaterialKind mCraftingMaterialKind;
	private TreeMap<Integer, String> mBookTextContentMap;
	private String mBookAuthor;
	private String mQuestID;

	public MonumentaItem() {
	}

	@SuppressWarnings("unchecked")
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
			out.mEnchantMap = (TreeMap<CustomEnchantment, Integer>)this.mEnchantMap.clone();
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
		out.mIsMagicWand = this.mIsMagicWand;
		out.mUnbreakable = this.mUnbreakable;
		out.mBaseDurability = this.mBaseDurability;
		out.mOldName = this.mOldName;
		out.mOldMaterial = this.mOldMaterial;
		if (this.mOnConsumeMap != null) {
			for (Map.Entry<PassiveEffect, TreeMap<Integer, Integer>> potionEntry : this.mOnConsumeMap.entrySet()) {
				PassiveEffect effect = potionEntry.getKey();
				for (Map.Entry<Integer, Integer> valueEntry : potionEntry.getValue().entrySet()) {
					Integer potency = valueEntry.getKey();
					Integer duration = valueEntry.getValue();
					out.setOnConsume(effect, potency, duration);
				}
			}
		}
		out.mBannerBaseColor = this.mBannerBaseColor;
		if (this.mBannerPatterns != null) {
			out.mBannerPatterns = new ArrayList<>();
			out.mBannerPatterns.addAll(this.mBannerPatterns);
		}
		out.mCraftingMaterialKind = this.mCraftingMaterialKind;
		if (this.mEdits != null) {
			out.mEdits = this.mEdits.clone();
		}
		if (this.mBookTextContentMap != null) {
			out.mBookTextContentMap = new TreeMap<>();
			for (Map.Entry<Integer, String> entry : this.mBookTextContentMap.entrySet()) {
				out.mBookTextContentMap.put(entry.getKey(), entry.getValue());
			}
		}
		out.mBookAuthor = this.mBookAuthor;
		out.mQuestID = this.mQuestID;
		return out;
	}

	public void mergeEdits() {
		MonumentaItem e = this.mEdits;
		if (e != null) {
			e.mergeEdits();
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
				for (Map.Entry<CustomEnchantment, Integer> entry : e.getEnchantMap().entrySet()) {
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
			if (e.mIsMagicWand != null) {
				this.mIsMagicWand = e.mIsMagicWand;
			}
			if (e.mUnbreakable != null) {
				this.mUnbreakable = e.mUnbreakable;
			}
			if (e.mBaseDurability != null) {
				this.mBaseDurability = e.mBaseDurability;
			}
			if (e.mOnConsumeMap != null) {
				for (Map.Entry<PassiveEffect, TreeMap<Integer, Integer>> potionEntry : e.mOnConsumeMap.entrySet()) {
					PassiveEffect effect = potionEntry.getKey();
					for (Map.Entry<Integer, Integer> valueEntry : potionEntry.getValue().entrySet()) {
						Integer potency = valueEntry.getKey();
						Integer duration = valueEntry.getValue();
						this.setOnConsume(effect, potency, duration);
					}
				}
			}
			if (e.mBannerBaseColor != null) {
				this.mBannerBaseColor = e.mBannerBaseColor;
			}
			if (e.mBannerPatterns != null) {
				this.mBannerPatterns = e.mBannerPatterns;
			}
			if (e.mCraftingMaterialKind != null) {
				this.mCraftingMaterialKind = e.mCraftingMaterialKind;
			}
			if (e.mBookTextContentMap != null && e.mBookTextContentMap.size() > 0) {
				if (this.mBookTextContentMap == null) {
					this.mBookTextContentMap = new TreeMap<>();
				}
				for (int i = 0; i <= e.mBookTextContentMap.lastKey(); i++) {
					if (e.mBookTextContentMap.get(i) != null) {
						this.mBookTextContentMap.put(i, e.mBookTextContentMap.get(i));
					}
				}
			}
			if (e.mBookAuthor != null) {
				this.mBookAuthor = e.mBookAuthor;
			}
			if (e.mQuestID != null) {
				this.mQuestID = e.mQuestID;
			}
			this.mEdits = e.mEdits;
		}
	}

	public ItemStack toItemStack() {
		// init + material
		String jsonToStore = null;
		if (this.mEdits != null) {
			//differs from index entry. create a separate instance
			jsonToStore = GSON.toJson(this.mEdits);
		}
		this.preCalc();
		ItemStack out = new ItemStack(this.getMaterial());
		ItemMeta meta = out.getItemMeta();
		ArrayList<String> loreLines = new ArrayList<>();

		// banner patterns
		if (this.mBannerPatterns != null && this.mBannerPatterns.size() > 0) {
			if (meta instanceof BannerMeta) {
				BannerMeta bMeta = (BannerMeta)meta;
				for (Pattern p : this.mBannerPatterns) {
					bMeta.addPattern(p);
				}
			} else if (this.getMaterial() == Material.SHIELD) {
				BlockStateMeta bsMeta = (BlockStateMeta)meta;
				Banner banner = (Banner)bsMeta.getBlockState();
				banner.setBaseColor(this.mBannerBaseColor);
				banner.setPatterns(this.mBannerPatterns);
				banner.update();
				bsMeta.setBlockState(banner);
			}
		}

		// name
		meta.setDisplayName(this.getName() + ChatColor.RESET);

		// book things
		if (meta instanceof BookMeta) {
			BookMeta bMeta = (BookMeta)meta;
			if (this.mBookTextContentMap != null && this.mBookTextContentMap.size() > 0) {
				Integer last = this.mBookTextContentMap.lastKey();
				for (int i = 0; i <= last; i++) {
					String s = this.mBookTextContentMap.getOrDefault(i, " ");
					bMeta.addPage(s.replace('&', '§'));
				}
			}
			bMeta.setTitle("no u");
			bMeta.setAuthor("System");
			if (this.mBookAuthor != null) {
				bMeta.setAuthor(this.mBookAuthor);
				loreLines.add(ChatColor.GRAY + "by " + this.mBookAuthor);
			}
			loreLines.add(ChatColor.GRAY + "Original");
		}

		// on consume effects
		if (this.mOnConsumeMap != null && meta instanceof PotionMeta) {
			for (Map.Entry<PassiveEffect, TreeMap<Integer, Integer>> potionEntry : this.mOnConsumeMap.entrySet()) {
				PassiveEffect effect = potionEntry.getKey();
				for (Map.Entry<Integer, Integer> valueEntry : potionEntry.getValue().entrySet()) {
					Integer potency = valueEntry.getKey();
					Integer duration = valueEntry.getValue();
					if (potency > 0 && duration != 0) {
						if (!effect.isCustom()) {
							PotionMeta pMeta = (PotionMeta)meta;
							pMeta.addCustomEffect(new PotionEffect(effect.getBukkitEffect(), duration, potency - 1), false);
						}
						loreLines.add(String.format("%s%s%s%s", effect.isNegative() ^ duration < 0 ? ChatColor.RED : ChatColor.BLUE,
							effect.getReadableStr(), potency > 1 ? " " + StringUtils.toRoman(potency) : "",
							duration > 20 ? String.format(" (%s)", StringUtils.intToMinuteAndSeconds(duration / 20)) : ""));
					}
				}
			}
		}

		// enchants
		// if (this.mEnchantMap != null) {
		// 	for (CustomEnchantment e : CustomEnchantment.values()) {
		// 		if (this.mEnchantMap.containsKey(e)) {
		// 			Integer v = this.mEnchantMap.get(e);
		// 			if (v > 0) {
		// 				String toAdd = e.getName();
		// 				if (!e.usesLevels()) {
		// 					toAdd += " " + StringUtils.toRoman(v);
		// 				}
		// 				loreLines.add(toAdd);
		// 				if (e.isBukkitEnchantment()) {
		// 					meta.addEnchant(e.getBukkitEnchantment(), v, true);
		// 				}
		// 			}
		// 		}
		// 	}
		// }

		// magic wand
		if (this.mIsMagicWand != null && this.mIsMagicWand) {
			loreLines.add(ChatColor.DARK_GRAY + "* Magic Wand *");
		}

		// crafting material
		if (this.mCraftingMaterialKind != null && this.mCraftingMaterialKind != null) {
			loreLines.add(this.mCraftingMaterialKind.getReadableString());
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
			if (meta instanceof LeatherArmorMeta) {
				((LeatherArmorMeta)meta).setColor(Color.fromRGB(this.getColor()[0], this.getColor()[1], this.getColor()[2]));
			}
			if (this.isColorable()) {
				// TODO
			}
		}

		// questID

		if (this.mQuestID != null) {
			loreLines.add(ChatColor.LIGHT_PURPLE + "* Quest Item *");
			if (!this.mQuestID.equals("1")) {
				loreLines.add("#" + this.mQuestID);
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

		// unbreakable
		if (this.mUnbreakable != null) {
			meta.setUnbreakable(this.mUnbreakable);
		}

		// durability
		if (this.mBaseDurability != null) {
			if (meta instanceof Damageable) {
				((Damageable) meta).setDamage(this.mBaseDurability);
			}
		}

		// finish
		meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
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
			out.append(String.format("%sName:%s %s (%s%s)\n",
				editable,
				reset,
				this.getNameRaw(),
				this.getName(),
				reset));
		}
		if (this.getMaterial() != null) {
			out.append(String.format("%sMaterial:%s %s (%s)\n",
				editable,
				reset,
				this.getMaterial(),
				this.getMaterial().getKey()));
		}
		if (this.getRegion() != null) {
			out.append(String.format("%s%sRegion:%s %s (%d : %s%s)\n",
				editable,
				this.getRegion() == Region.NONE || this.getTier() == ItemTier.NONE ? unused : "",
				reset,
				this.getRegion(),
				this.getRegion().getInt(),
				this.getRegion().getReadableString(),
				reset));
		}
		if (this.getTier() != null) {
			out.append(String.format("%s%sTier:%s %s (%s%s)\n",
				editable,
				this.getRegion() == Region.NONE || this.getTier() == ItemTier.NONE ? unused : "",
				reset,
				this.getTier(),
				this.getTier().getReadableString(),
				reset));
		}
		if (this.getLocation() != null) {
			out.append(String.format("%s%sLocation:%s %s (%s%s)\n",
				editable,
				this.getLocation() == ItemLocation.NONE ? unused : "",
				reset,
				this.getLocation(),
				this.getLocation().getReadableString(),
				reset));
		}
		if (this.getLore() != null) {
			out.append(String.format("%s%sLore:%s %s\n",
				editable, this.getLore().size() == 0 ? unused : "",
				reset, this.getLore().size() > 0 ? this.getLore().toString().replace(",", "\n              ").replace('{', '}').replace("}", "") : "None"));
		}
		if (this.getEnchantMap() != null) {
			out.append(String.format("%s%sEnchants:%s %s\n",
				editable,
				this.getEnchantMap().size() == 0 ? unused : "",
				reset,
				this.getEnchantMap().size() > 0 ? this.getEnchantMap().toString().replace(",", "\n              ").replace('{', '}').replace("}", "") : "None"));
		}
		if (this.getColor() != null) {
			out.append(String.format("%s%sColor:%s R:%d G:%d B:%d -> %s\n",
				editable,
				this.isColorable() ? "" : unused,
				reset,
				this.getColor()[0],
				this.getColor()[1],
				this.getColor()[2],
				String.format("#%02X%02X%02X",
					this.getColor()[0],
					this.getColor()[1],
					this.getColor()[2])));
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
			out.append(String.format("%s%sLastEditor:%s %s\n",
				readOnly,
				hidden,
				reset,
				this.getLastEditor()));
		}
		if (this.mLastEditedTimestamp != null) {
			out.append(String.format("%s%sLastEditionTime:%s %s (%s)\n",
				readOnly,
				hidden,
				reset,
				this.getLastEditTimeAsString(),
				this.getTimeSinceLastEditAsString()));
		}
		if (this.getArmorMaterial() != null) {
			out.append(String.format("%s%sArmorMaterial:%s %s (%s%s)\n",
				readOnly,
				this.getArmorMaterial() == ArmorMaterial.NONE || this.getArmorMaterialOverride() != null ? unused : "",
				reset,
				this.getArmorMaterial(),
				this.getArmorMaterial().getReadableString(),
				reset));
		}
		if (this.getArmorMaterialOverride() != null) {
			out.append(String.format("%sArmorMaterialOverride:%s %s (%s%s)\n", editable, reset, this.getArmorMaterialOverride(), this.getArmorMaterialOverride().getReadableString(), reset));
		}
		if (this.getIsMagicWand() != null) {
			out.append(String.format("%sIsMagicWand:%s %s\n", editable, reset, this.isMagicWand()));
		}
		if (this.getUnbreakable() != null) {
			out.append(String.format("%sUnbreakable:%s %s\n", editable, reset, this.getUnbreakable()));
		}
		if (this.getDurability() != null) {
			out.append(String.format("%s%sDurability:%s %s\n", editable, this.getUnbreakable() != null && this.getUnbreakable() ? unused : "", reset, this.getUnbreakable()));
		}
		if (this.getOldName() != null) {
			out.append(String.format("%sOld Name:%s %s\n", readOnly, reset, this.getOldName()));
		}
		if (this.getOldMaterial() != null) {
			out.append(String.format("%sOld Material:%s %s\n", readOnly, reset, this.getOldMaterial()));
		}
		if (this.mOnConsumeMap != null) {
			out.append(String.format("%s%sOnConsume Effects:%s\n", editable, this.mOnConsumeMap.size() == 0 ? unused : "", reset));
			for (Map.Entry<PassiveEffect, TreeMap<Integer, Integer>> potionEntry : this.mOnConsumeMap.entrySet()) {
				PassiveEffect effect = potionEntry.getKey();
				for (Map.Entry<Integer, Integer> valueEntry : potionEntry.getValue().entrySet()) {
					Integer potency = valueEntry.getKey();
					Integer duration = valueEntry.getValue();
					out.append(String.format("%s   %s   %s\n", effect, potency, duration));
				}
			}
		}
		if (this.mBannerBaseColor != null) {
			out.append(String.format("%s%sBanner Base Color:%s %s\n", editable, this.mMaterial == Material.SHIELD ? unused : "", reset, this.mBannerBaseColor));
		}
		if (this.mBannerPatterns != null) {
			out.append(String.format("%s%sBanner Patterns:%s %s\n", editable, this.mMaterial == Material.SHIELD || (this.mMaterial != null && new ItemStack(this.mMaterial).getItemMeta() instanceof BannerMeta) ? unused : "", reset, this.mBannerPatterns.toString()));
		}
		if (this.mBookTextContentMap != null) {
			out.append(String.format("%s%sBook Contents:%s %s\n",
				editable, this.getMaterial() == Material.WRITABLE_BOOK || this.getMaterial() == Material.WRITTEN_BOOK ? "" : unused,
				reset,
				this.mBookTextContentMap.size() > 0 ? this.mBookTextContentMap.toString().replace(",", "\n              ").replace('{', '}').replace("}", "") : "None"));
		}
		if (this.mBookAuthor != null) {
			out.append(String.format("%s%sBook Author:%s %s\n",
				editable,
				this.getMaterial() == Material.WRITABLE_BOOK || this.getMaterial() == Material.WRITTEN_BOOK ? "" : unused,
				reset,
				this.getBookAuthor()));
		}
		if (this.mQuestID != null) {
			out.append(String.format("%sQuestID:%s %s\n",
				editable,
				reset,
				this.mQuestID));
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

	//getters

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
	public Map<CustomEnchantment, Integer> getEnchantMap() {
		return this.mEnchantMap;
	}

	public int getEnchantLevel(CustomEnchantment enchant) {
		if (this.getEnchantMap() == null) {
			return 0;
		}
		return this.getEnchantMap().getOrDefault(enchant, 0);
	}

	public boolean isColorable() {
		boolean result = false;
		if (this.getMaterial() != null) {
			switch (this.getMaterial()) {
				case LEATHER_BOOTS:
				case LEATHER_LEGGINGS:
				case LEATHER_CHESTPLATE:
				case LEATHER_HELMET:
					result = true;
					break;
				default:
					break;
			}
		}
		return result;
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

	@Nullable
	public Boolean getIsMagicWand() {
		return this.mIsMagicWand;
	}

	@Nullable
	public Boolean isMagicWand() {
		return mIsMagicWand;
	}

	@Nullable
	public Boolean getUnbreakable() {
		return this.mUnbreakable;
	}

	@Nullable
	public Integer getDurability() {
		return this.mBaseDurability;
	}

	@Nullable
	public String getOldName() {
		return this.mOldName;
	}

	@Nullable
	public Material getOldMaterial() {
		return this.mOldMaterial;
	}

	@Nullable
	public TreeMap<PassiveEffect, TreeMap<Integer, Integer>> getOnConsumeMap() {
		return this.mOnConsumeMap;
	}

	@Nullable
	public ArrayList<Pattern> getBannerPatterns() {
		return this.mBannerPatterns;
	}

	@Nullable
	public CraftingMaterialKind getCraftingMaterialKind() {
		return this.mCraftingMaterialKind;
	}

	@Nullable
	public TreeMap<Integer, String> getBookTextContentMap() {
		return this.mBookTextContentMap;
	}

	@Nullable
	public String getBookPage(Integer i) {
		String out = null;
		if (this.mBookTextContentMap != null) {
			out = this.mBookTextContentMap.get(i);
		}
		return out;
	}

	@Nullable
	public String getBookAuthor() {
		return mBookAuthor;
	}

	public String getQuestID() {
		return this.mQuestID;
	}

	// setters

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

	public void setEnchantLevel(CustomEnchantment enchant, int level) {
		if (this.getEnchantMap() == null) {
			this.mEnchantMap = new TreeMap<>();
		}
		this.mEnchantMap.put(enchant, level);
	}

	public void setEnchantMap(TreeMap<CustomEnchantment, Integer> enchantMap) {
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
		if (!Materials.WEARABLE.contains(this.mMaterial)) {
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

	public void setIsMagicWand(Boolean isMagicWand) {
		this.mIsMagicWand = isMagicWand;
	}

	public void setUnbreakable(Boolean unbreakable) {
		this.mUnbreakable = unbreakable;
	}

	public void setDurability(Integer i) {
		this.mBaseDurability = i;
	}

	public void setOldName(String oldName) {
		this.mOldName = oldName;
	}

	public void setOldMaterial(Material oldMaterial) {
		this.mOldMaterial = oldMaterial;
	}

	public void setOnConsume(PassiveEffect effect, Integer potency, Integer duration) {
		if (this.mOnConsumeMap == null) {
			this.mOnConsumeMap = new TreeMap<>();
		}
		TreeMap<Integer, Integer> valueMap = this.mOnConsumeMap.getOrDefault(effect, new TreeMap<>());
		valueMap.put(potency, duration);
		this.mOnConsumeMap.put(effect, valueMap);
	}

	public void setBannerPatterns(List<Pattern> patternList) {
		ArrayList<Pattern> in = new ArrayList<>(patternList);
		this.mBannerPatterns = in;
	}

	public void setBannerBaseColor(DyeColor bannerBaseColor) {
		this.mBannerBaseColor = bannerBaseColor;
	}

	public void setBanner(ItemStack banner) {
		if (banner.getItemMeta() instanceof BannerMeta) {
			BannerMeta bMeta = (BannerMeta)banner.getItemMeta();
			this.setBannerPatterns(bMeta.getPatterns());
			DyeColor c = DyeColor.valueOf(banner.getType().toString().replace("_BANNER", ""));
			this.setBannerBaseColor(c);
		}
	}

	public void setCraftingMaterialKind(CraftingMaterialKind kind) {
		this.mCraftingMaterialKind = kind;
	}

	public void setBookPageContent(int page, String content) {
		if (this.mBookTextContentMap == null) {
			this.mBookTextContentMap = new TreeMap<>();
		}
		this.mBookTextContentMap.put(page, content);
	}

	public void setBookAuthor(String mBookAuthor) {
		this.mBookAuthor = mBookAuthor;
	}

	public void setQuestID(String mQuestID) {
		this.mQuestID = mQuestID;
	}

	// utils

	public static MonumentaItem fromItemStack(ItemStack itemStack) {
		return Plugin.getInstance().mItemManager.getMMItemWithEdits(itemStack);
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
		return GSON.toJsonTree(this);
	}

	public String toPrettyJson() {
		return GSON_PRETTY.toJson(this);
	}

	public String toJson() {
		return GSON.toJson(this);
	}

	public String toLootTablePrettyJson() {
		return GSON_PRETTY.toJson(this.toLootTableJsonElement());
	}

	public void setDefaultValues() {
		this.mName = "&7New Item";
		this.mMaterial = Material.PAPER;
		this.mRegion = Region.MONUMENTA;
		this.mTier = ItemTier.DEV;
	}

	void preCalc() {
		this.mergeEdits();

		// remove useless values
		// onConsume
		if (this.mOnConsumeMap != null) {
			TreeMap<PassiveEffect, TreeMap<Integer, Integer>> newMap = new TreeMap<>();
			all : for (Map.Entry<PassiveEffect, TreeMap<Integer, Integer>> potionEntry : this.mOnConsumeMap.entrySet()) {
				PassiveEffect effect = potionEntry.getKey();
				TreeMap<Integer, Integer> newSubMap = new TreeMap<>();
				for (Map.Entry<Integer, Integer> valueEntry : potionEntry.getValue().entrySet()) {
					Integer potency = valueEntry.getKey();
					if (potency == 0) {
						continue all;
					}
					Integer duration = valueEntry.getValue();
					if (duration != 0) {
						newSubMap.put(potency, duration);
					}
				}
				if (newSubMap.size() > 0) {
					newMap.put(effect, newSubMap);
				}
			}
			if (newMap.size() == 0) {
				newMap = null;
			}
			this.mOnConsumeMap = newMap;
		}
		// QuestID
		if (this.mQuestID != null && this.mQuestID.equals("0")) {
			this.mQuestID = null;
		}
		// Lore
		if (this.mLoreMap != null && this.mLoreMap.size() > 0) {
			ArrayList<Integer> toRemove = new ArrayList<>();
			for (Map.Entry<Integer, String> entry : this.mLoreMap.entrySet()) {
				if (entry.getValue().equals(".")) {
					toRemove.add(entry.getKey());
				}
			}
			for (Integer i : toRemove) {
				this.mLoreMap.remove(i);
			}
			if (this.mLoreMap.size() == 0) {
				this.mLoreMap = null;
			}
		}

	}
}
