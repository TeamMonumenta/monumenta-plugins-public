package com.playmonumenta.plugins.itemstats.enums;

import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public enum ItemType {

	CHARM,
	HELMET,
	CHESTPLATE,
	LEGGINGS,
	BOOTS,
	SHIELD,
	ALCHEMIST,
	WAND,
	OFFHAND,
	PICKAXE,
	SCYTHE,
	CONSUMABLE,
	RANGED,
	SHOVEL,
	AXE,
	MAINHAND,
	MISC;

	public static ItemType of(ItemStack item) {
		if (ItemStatUtils.isCharm(item)) {
			return CHARM;
		} else if (ItemUtils.isArmorOrWearable(item)) {
			return switch (ItemUtils.getEquipmentSlot(item)) {
				case HEAD -> HELMET;
				case CHEST -> CHESTPLATE;
				case LEGS -> LEGGINGS;
				case FEET -> BOOTS;
				case HAND, OFF_HAND -> MISC;
			};
		} else if (item.getType() == Material.SHIELD) {
			return SHIELD;
		} else if (ItemUtils.isAlchemistItem(item)) {
			return ALCHEMIST;
		} else if (ItemUtils.isWand(item)) {
			return WAND;
		} else if (ItemStatUtils.hasAttributeInSlot(item, Slot.OFFHAND)) {
			return OFFHAND;
		} else if (ItemUtils.isPickaxe(item)) {
			return PICKAXE;
		} else if (ItemUtils.isHoe(item)) {
			return SCYTHE;
		} else if (ItemStatUtils.isConsumable(item) || item.getType().isEdible() || ItemUtils.isSomePotion(item)) {
			return CONSUMABLE;
		} else if (ItemUtils.isRanged(item.getType())) {
			return RANGED;
		} else if (ItemUtils.isShovel(item)) {
			return SHOVEL;
		} else if (ItemUtils.isAxe(item)) {
			return AXE;
		} else if (ItemStatUtils.hasAttributeInSlot(item, Slot.MAINHAND)) {
			return MAINHAND;
		} else {
			return MISC;
		}
	}
}
