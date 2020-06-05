package com.playmonumenta.plugins.itemindex;

import net.md_5.bungee.api.ChatColor;

import java.util.ArrayList;

public enum EquipmentSlot {

	MAIN_HAND(org.bukkit.inventory.EquipmentSlot.HAND, "When in main hand:"),
	OFF_HAND(org.bukkit.inventory.EquipmentSlot.OFF_HAND, "When in off hand:"),
	HEAD(org.bukkit.inventory.EquipmentSlot.HEAD, "When on head:"),
	CHEST(org.bukkit.inventory.EquipmentSlot.CHEST, "When on body:"),
	LEGS(org.bukkit.inventory.EquipmentSlot.LEGS, "When on legs:"),
	FEET(org.bukkit.inventory.EquipmentSlot.FEET, "When on feet:"),
	;

	private String mReadableString;
	private org.bukkit.inventory.EquipmentSlot mBukkitSlot;

	EquipmentSlot(org.bukkit.inventory.EquipmentSlot bukkitSlot, String s) {
		this.mReadableString = ChatColor.GRAY + s;
		this.mBukkitSlot = bukkitSlot;
	}

	String getReadableString() {
		return this.mReadableString;
	}

	org.bukkit.inventory.EquipmentSlot getBukkitSlot() {
		return this.mBukkitSlot;
	}

	public static String[] valuesAsStringArray() {
		ArrayList<String> out = new ArrayList<>();
		for (EquipmentSlot s : EquipmentSlot.values()) {
			out.add(s.toString().toLowerCase());
		}
		return out.toArray(new String[0]);
	}
}
