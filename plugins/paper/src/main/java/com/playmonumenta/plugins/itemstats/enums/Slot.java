package com.playmonumenta.plugins.itemstats.enums;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.inventory.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

public enum Slot {
	MAINHAND(EquipmentSlot.HAND, "mainhand", "When in Main Hand:"),
	OFFHAND(EquipmentSlot.OFF_HAND, "offhand", "When in Off Hand:"),
	HEAD(EquipmentSlot.HEAD, "head", "When on Head:"),
	CHEST(EquipmentSlot.CHEST, "chest", "When on Chest:"),
	LEGS(EquipmentSlot.LEGS, "legs", "When on Legs:"),
	FEET(EquipmentSlot.FEET, "feet", "When on Feet:"),
	PROJECTILE(null, "projectile", "When Shot:");

	public static final String KEY = "Slot";

	final @Nullable EquipmentSlot mEquipmentSlot;
	final String mName;
	final Component mDisplay;

	Slot(@Nullable EquipmentSlot equipmentSlot, String name, String display) {
		mEquipmentSlot = equipmentSlot;
		mName = name;
		mDisplay = Component.text(display, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
	}

	public @Nullable EquipmentSlot getEquipmentSlot() {
		return mEquipmentSlot;
	}

	public String getName() {
		return mName;
	}

	public Component getDisplay() {
		return mDisplay;
	}

	public static @Nullable Slot getSlot(@Nullable String name) {
		if (name == null) {
			return null;
		}

		for (Slot slot : Slot.values()) {
			if (slot.getName().replace(" ", "").equals(name.replace(" ", ""))) {
				return slot;
			}
		}

		return null;
	}
}
