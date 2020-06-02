package com.playmonumenta.plugins.enums;

import com.playmonumenta.plugins.items.MonumentaItem;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.inventory.EquipmentSlot;

import java.util.ArrayList;

public enum ArmorMaterial {
	NONE(""),
	CLOTH(ChatColor.BLUE + "Cloth Armor"),
	LEATHER(ChatColor.AQUA + "Leather Armor"),
	BRONZE(ChatColor.DARK_AQUA + "Bronze Armor"),
	CHAIN(ChatColor.GREEN + "Chainmail Armor"),
	IRON(ChatColor.WHITE + "Iron Armor"),
	;

	private static MatchCondition[] calculateConditions(ArmorMaterial mat) {
		MatchCondition[] out = new MatchCondition[4];
		switch (mat) {
			case CLOTH:
				out[0] = new MatchCondition(Material.LEATHER_HELMET, 0.5, 0);
				out[1] = new MatchCondition(Material.LEATHER_CHESTPLATE, 2, 0);
				out[2] = new MatchCondition(Material.LEATHER_LEGGINGS, 1.0, 0);
				out[3] = new MatchCondition(Material.LEATHER_BOOTS, 0.5, 0);
				break;
			case LEATHER:
				out[0] = new MatchCondition(Material.LEATHER_HELMET, 1, 0);
				out[1] = new MatchCondition(Material.LEATHER_CHESTPLATE, 3, 0);
				out[2] = new MatchCondition(Material.LEATHER_LEGGINGS, 2, 0);
				out[3] = new MatchCondition(Material.LEATHER_BOOTS, 1, 0);
				break;
			case BRONZE:
				out[0] = new MatchCondition(Material.GOLDEN_HELMET, 1.5, 0.5);
				out[1] = new MatchCondition(Material.GOLDEN_CHESTPLATE, 4, 0.5);
				out[2] = new MatchCondition(Material.GOLDEN_LEGGINGS, 3, 0.5);
				out[3] = new MatchCondition(Material.GOLDEN_BOOTS, 1.5, 0.5);
				break;
			case CHAIN:
				out[0] = new MatchCondition(Material.CHAINMAIL_HELMET, 2, 1);
				out[1] = new MatchCondition(Material.CHAINMAIL_CHESTPLATE, 5, 1);
				out[2] = new MatchCondition(Material.CHAINMAIL_LEGGINGS, 4, 1);
				out[3] = new MatchCondition(Material.CHAINMAIL_BOOTS, 2, 1);
				break;
			case IRON:
				out[0] = new MatchCondition(Material.IRON_HELMET, 2, 1.5);
				out[1] = new MatchCondition(Material.IRON_CHESTPLATE, 5, 1.5);
				out[2] = new MatchCondition(Material.IRON_LEGGINGS, 4, 1.5);
				out[3] = new MatchCondition(Material.IRON_BOOTS, 2, 1.5);
				break;
			default:
				for (int i = 0; i < 4; i++) {
					out[i] = new MatchCondition(Material.AIR, 0, 0);
				}
		}
		return out;
	}

	String mReadableString;

	ArmorMaterial(String s) {
		this.mReadableString = s;
	}

	public String getReadableString() {
		return this.mReadableString;
	}

	public boolean matchesMonumentaItem(MonumentaItem i) {
		if (this == NONE) {
			return true;
		}
		MatchCondition[] conds = calculateConditions(this);
		Attribute armAtr = Attribute.GENERIC_ARMOR;
		Attribute touAtr = Attribute.GENERIC_ARMOR_TOUGHNESS;
		AttributeModifier.Operation op = AttributeModifier.Operation.ADD_NUMBER;
		EquipmentSlot[] slots = new EquipmentSlot[4];
		slots[0] = EquipmentSlot.HEAD;
		slots[1] = EquipmentSlot.CHEST;
		slots[2] = EquipmentSlot.LEGS;
		slots[3] = EquipmentSlot.FEET;
		for (int j = 0; j < 4; j++) {
			if (i.getMaterial() == conds[j].mMaterial && i.getAttribute(slots[j], armAtr, op) == conds[j].mArmor && i.getAttribute(slots[j], touAtr, op) == conds[j].mArmorToughness) {
				return true;
			}
		}
		return false;
	}

	public static String[] valuesAsStringArray() {
		ArrayList<String> out = new ArrayList<>();
		for (ArmorMaterial s : ArmorMaterial.values()) {
			out.add(s.toString().toLowerCase());
		}
		return out.toArray(new String[0]);
	}

	private static class MatchCondition {
		Material mMaterial;
		Double mArmor;
		Double mArmorToughness;

		MatchCondition(Material material, double armor, double armorToughness) {
			this.mArmor = armor;
			this.mArmorToughness = armorToughness;
			this.mMaterial = material;
		}
	}
}
