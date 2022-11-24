package com.playmonumenta.plugins.itemstats.gui;

import com.google.common.collect.ImmutableList;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.EquipmentSlot;

enum PSGUIEquipment {
	MAINHAND(46, 52, 0, "Main Hand", EquipmentSlot.HAND, ItemStatUtils.Slot.MAINHAND),
	OFFHAND(19, 25, 40, "Off Hand", EquipmentSlot.OFF_HAND, ItemStatUtils.Slot.OFFHAND),
	HEAD(18, 26, 39, "Head", EquipmentSlot.HEAD, ItemStatUtils.Slot.HEAD),
	CHEST(27, 35, 38, "Chest", EquipmentSlot.CHEST, ItemStatUtils.Slot.CHEST),
	LEGS(36, 44, 37, "Legs", EquipmentSlot.LEGS, ItemStatUtils.Slot.LEGS),
	FEET(45, 53, 36, "Feet", EquipmentSlot.FEET, ItemStatUtils.Slot.FEET);

	final int mLeftSlot;
	final int mRightSlot;
	final int mPlayerInventorySlot;
	private final String mName;
	final EquipmentSlot mEquipmentSlot;
	final ItemStatUtils.Slot mSlot;
	private final ImmutableList<Component> mLore = ImmutableList.of(
		Component.text("Click here, then click an item to compare builds.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false),
		Component.text("Right click to restore the initial item.", NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));

	PSGUIEquipment(int leftSlot, int rightSlot, int playerInventorySlot, String name, EquipmentSlot equipmentSlot, ItemStatUtils.Slot slot) {
		mLeftSlot = leftSlot;
		mRightSlot = rightSlot;
		mPlayerInventorySlot = playerInventorySlot;
		mName = name;
		mEquipmentSlot = equipmentSlot;
		mSlot = slot;
	}

	public Material getIcon() {
		return Material.ITEM_FRAME;
	}

	public Component getDisplay(boolean selected) {
		return Component.text(String.format("%s Slot%s", mName, selected ? " (Selected)" : ""), selected ? NamedTextColor.GREEN : NamedTextColor.GRAY).decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false);
	}

	public ImmutableList<Component> getLore() {
		return mLore;
	}
}
