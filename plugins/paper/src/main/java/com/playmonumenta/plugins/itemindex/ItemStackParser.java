package com.playmonumenta.plugins.itemindex;

import com.google.common.collect.Multimap;
import com.playmonumenta.plugins.utils.ItemUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Banner;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;

import java.util.List;
import java.util.Map;

public class ItemStackParser {

	public ItemStackParser(ItemStack item) {
		this.mItemStack = item;
		this.mMonumentaItem = new MonumentaItem();
	}

	private ItemStack mItemStack;
	private MonumentaItem mMonumentaItem;

	public MonumentaItem parse() {
		if (this.mItemStack == null || this.mItemStack.getAmount() == 0 || this.mItemStack.getType() == Material.AIR) {
			return this.mMonumentaItem;
		}
		this.parseName();
		this.parseMaterial();
		this.parseRegionTier();
		this.parseArmorColor();
		this.parseAttributes();
		this.parseEnchants();
		this.parseLore();
		this.parseLocation();
		this.parseUnbreakable();
		this.parseDurability();
		this.parseOnConsume();
		this.parseCraftingMaterial();
		this.parseBannerPatterns();
		this.parseBook();
		this.parseQuestID();

		return this.mMonumentaItem;
	}

	private void parseQuestID() {
		List<String> loreLines = this.mItemStack.getItemMeta().getLore();
		if (loreLines == null) {
			return;
		}
		for (String s : loreLines) {
			if (s.equals(ChatColor.LIGHT_PURPLE + "* Quest Item *")) {
				this.mMonumentaItem.setQuestID("1");
				for (String s2 : loreLines) {
					if (s2.startsWith("#")) {
						this.mMonumentaItem.setQuestID(s2.substring(1));
					}
				}
				break;
			}
		}
	}

	private void parseBook() {
		ItemMeta meta = this.mItemStack.getItemMeta();
		if (meta instanceof BookMeta) {
			BookMeta bMeta = (BookMeta)meta;
			for (int i = 0; i < bMeta.getPageCount(); i++) {
				this.mMonumentaItem.setBookPageContent(i, bMeta.getPage(i));
			}
			this.mMonumentaItem.setBookAuthor(bMeta.getAuthor());
		}
	}

	private void parseBannerPatterns() {
		ItemMeta meta = this.mItemStack.getItemMeta();
		if (meta instanceof BannerMeta) {
			BannerMeta bMeta = (BannerMeta)meta;
				this.mMonumentaItem.setBannerPatterns(bMeta.getPatterns());
		} else if (this.mItemStack.getType() == Material.SHIELD) {
			BlockStateMeta bsMeta = (BlockStateMeta)meta;
			Banner banner = (Banner)bsMeta.getBlockState();
			this.mMonumentaItem.setBannerBaseColor(banner.getBaseColor());
			this.mMonumentaItem.setBannerPatterns(banner.getPatterns());
		}
	}

	private void parseCraftingMaterial() {
		List<String> loreLines = this.mItemStack.getItemMeta().getLore();
		if (loreLines == null) {
			return;
		}
		for (String s : loreLines) {
			for (CraftingMaterialKind k : CraftingMaterialKind.values()) {
				if (k != CraftingMaterialKind.NONE && k.getReadableString().equals(s)) {
					this.mMonumentaItem.setCraftingMaterialKind(k);
				}
			}
		}
	}

	private void parseOnConsume() {
		ItemMeta meta = this.mItemStack.getItemMeta();
		if (meta instanceof PotionMeta) {
			PotionMeta pMeta = (PotionMeta)meta;
			for (PotionEffect effect : pMeta.getCustomEffects()) {
				for (PassiveEffect p : PassiveEffect.values()) {
					if (!p.isCustom() && p.getBukkitEffect() == effect.getType()) {
						this.mMonumentaItem.setOnConsume(p, effect.getAmplifier(), effect.getDuration());
					}
				}
			}
		}
	}

	private void parseDurability() {
		ItemMeta meta = this.mItemStack.getItemMeta();
		if (meta instanceof Damageable) {
			Damageable dMeta = (Damageable)meta;
			this.mMonumentaItem.setDurability(dMeta.getDamage());
		}
	}

	private void parseUnbreakable() {
		ItemMeta meta = this.mItemStack.getItemMeta();
		this.mMonumentaItem.setUnbreakable(meta.isUnbreakable());
	}

	private void parseName() {
		this.mMonumentaItem.setName(this.mItemStack.getItemMeta().getDisplayName());
	}

	private void parseMaterial() {
		this.mMonumentaItem.setMaterial(this.mItemStack.getType());
	}

	private void parseRegionTier() {
		ItemUtils.ItemRegion itemRegion = ItemUtils.getItemRegion(this.mItemStack);
		if (itemRegion != ItemUtils.ItemRegion.UNKNOWN) {
			Region r = Region.valueOf(itemRegion.name());
			this.mMonumentaItem.setRegion(r);
		}
		ItemUtils.ItemTier itemTier = ItemUtils.getItemTier(this.mItemStack);
		if (itemTier != ItemUtils.ItemTier.UNKNOWN) {
			ItemTier t = ItemTier.valueOf(itemTier.name());
			this.mMonumentaItem.setTier(t);
		}
	}

	private void parseAttributes() {
		Multimap<Attribute, AttributeModifier> attribs = this.mItemStack.getItemMeta().getAttributeModifiers();
		if (attribs == null) {
			return;
		}
		for (Map.Entry<Attribute, AttributeModifier> entry : attribs.entries()) {
			String eSName = entry.getValue().getSlot().toString();
			if (eSName.equals("HAND")) {
				eSName = "MAIN_HAND";
			}
			this.mMonumentaItem.setAttribute(EquipmentSlot.valueOf(eSName),
				com.playmonumenta.plugins.itemindex.Attribute.valueOf(entry.getKey().toString().replace("GENERIC_", "").replace("ARMOR_T", "T")),
				entry.getValue().getOperation(), entry.getValue().getAmount());
		}
	}

	private void parseArmorColor() {
		ItemMeta m = this.mItemStack.getItemMeta();
		if (m instanceof LeatherArmorMeta) {
			int rgb = ((LeatherArmorMeta) m).getColor().asRGB();
			int[] color = {rgb >> 16 & 255, rgb >> 8 & 255, rgb & 255};
			this.mMonumentaItem.setColor(color);
		}
	}

	private void parseEnchants() {
		// ArrayList<CustomEnchantment> customEnchants = new ArrayList<>();
		// for (CustomEnchantment ench : CustomEnchantment.values()) {
		// 	if (ench.isBukkitEnchantment()) {
		// 		this.mMonumentaItem.setEnchantLevel(ench, this.mItemStack.getEnchantmentLevel(ench.getBukkitEnchantment()));
		// 	} else {
		// 		customEnchants.add(ench);
		// 	}
		// }
		// for (String s : this.mItemStack.getItemMeta().getLore()) {
		// 	for (CustomEnchantment ench : customEnchants) {
		// 		if (s.startsWith(ench.getName())) {
		// 			int level = 1;
		// 			if (!ench.usesLevels()) {
		// 				String[] split = s.split(" ");
		// 				String roman = split[split.length - 1];
		// 				level = StringUtils.toArabic(roman);
		// 			}
		// 			this.mMonumentaItem.setEnchantLevel(ench, level);
		// 		}
		// 	}
		// }
	}

	private void parseLore() {
		int i = 0;
		main: for (String s : this.mItemStack.getItemMeta().getLore()) {
			if (s.startsWith(ChatColor.DARK_GRAY + "")) {
				for (Region r : Region.values()) {
					if (r != Region.NONE && s.startsWith(r.getReadableString())) {
						continue main;
					} else if (s.startsWith(ChatColor.DARK_GRAY + "* Magic Wand *")) {
						this.mMonumentaItem.setIsMagicWand(true);
						continue main;
					}
				}
				this.mMonumentaItem.setLoreLine(i, ChatColor.stripColor(s));
				i++;
			}
		}
	}

	private void parseLocation() {
		for (String s : this.mItemStack.getItemMeta().getLore()) {
			for (ItemLocation l : ItemLocation.values()) {
				if (l != ItemLocation.NONE && s.startsWith(l.getReadableString())) {
					this.mMonumentaItem.setLocation(l);
					return;
				}
			}
		}
	}
}
