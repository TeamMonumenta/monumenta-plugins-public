package com.playmonumenta.plugins.items;

import com.google.common.collect.Multimap;
import com.playmonumenta.plugins.enums.Enchantment;
import com.playmonumenta.plugins.enums.ItemLocation;
import com.playmonumenta.plugins.enums.ItemTier;
import com.playmonumenta.plugins.enums.Region;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.ArrayList;
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

		this.mMonumentaItem.setEditable(true);
		return this.mMonumentaItem;
	}

	private void parseName() {
		this.mMonumentaItem.setName(this.mItemStack.getItemMeta().getDisplayName());
	}

	private void parseMaterial() {
		this.mMonumentaItem.setMaterial(this.mItemStack.getType());
	}

	private void parseRegionTier() {
		ItemUtils.ItemRegion itemRegion = ItemUtils.getItemRegion(this.mItemStack);
		if (itemRegion == ItemUtils.ItemRegion.UNKNOWN) {
			this.mMonumentaItem.setRegion(Region.NONE);
		}
		ItemUtils.ItemTier itemTier = ItemUtils.getItemTier(this.mItemStack);
		if (itemTier == ItemUtils.ItemTier.UNKNOWN) {
			this.mMonumentaItem.setTier(ItemTier.NONE);
		}
		Region r = Region.valueOf(itemRegion.name());
		ItemTier t = ItemTier.valueOf(itemTier.name());
		this.mMonumentaItem.setRegion(r);
		this.mMonumentaItem.setTier(t);
	}

	private void parseAttributes() {
		Multimap<Attribute, AttributeModifier> attribs = this.mItemStack.getItemMeta().getAttributeModifiers();
		if (attribs == null) {
			return;
		}
		for (Map.Entry<Attribute, AttributeModifier> entry : attribs.entries()) {
			this.mMonumentaItem.setAttribute(entry.getValue().getSlot(), entry.getKey(), entry.getValue().getOperation(), entry.getValue().getAmount());
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
		ArrayList<Enchantment> customEnchants = new ArrayList<>();
		for (Enchantment ench : Enchantment.values()) {
			if (ench.isBukkitEnchant()) {
				this.mMonumentaItem.setEnchantLevel(ench, this.mItemStack.getEnchantmentLevel(ench.getBukkitEnchantment()));
			} else {
				customEnchants.add(ench);
			}
		}
		for (String s : this.mItemStack.getItemMeta().getLore()) {
			for (Enchantment ench : customEnchants) {
				if (s.startsWith(ench.getReadableString())) {
					int level = 1;
					if (!ench.ignoresLevels()) {
						String[] split = s.split(" ");
						String roman = split[split.length - 1];
						level = StringUtils.toArabic(roman);
					}
					this.mMonumentaItem.setEnchantLevel(ench, level);
				}
			}
		}
	}

	private void parseLore() {
		int i = 0;
		main: for (String s : this.mItemStack.getItemMeta().getLore()) {
			if (s.startsWith(ChatColor.DARK_GRAY + "")) {
				for (Region r : Region.values()) {
					if (r != Region.NONE && s.startsWith(r.getReadableString())) {
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
