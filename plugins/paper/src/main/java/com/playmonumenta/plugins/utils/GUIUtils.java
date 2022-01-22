package com.playmonumenta.plugins.utils;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class GUIUtils {

	public static void splitLoreLine(ItemMeta meta, String lore, int maxLength, ChatColor defaultColor, boolean clean) {
		String[] splitLine = lore.split(" ");
		StringBuilder currentString = new StringBuilder(defaultColor + "");
		List<String> finalLines = (clean || meta.getLore() == null) ? new ArrayList<>() : meta.getLore();

		int currentLength = 0;
		for (String word : splitLine) {
			if (currentLength + word.length() > maxLength) {
				finalLines.add(currentString.toString());
				currentString.setLength(0);
				currentString.append(defaultColor + "");
				currentLength = 0;
			}
			currentString.append(word).append(" ");
			currentLength += word.length() + 1;
		}
		if (!currentString.toString().equals(defaultColor + "")) {
			finalLines.add(currentString.toString());
		}
		meta.setLore(finalLines);
	}

	public static void fillWithFiller(Inventory inventory, Material fillerMaterial) {
		ItemStack filler = new ItemStack(fillerMaterial, 1);
		ItemMeta meta = filler.getItemMeta();
		meta.setDisplayName(ChatColor.RESET + "");
		filler.setItemMeta(meta);
		for (int i = 0; i < inventory.getSize(); i++) {
			if (inventory.getItem(i) == null) {
				inventory.setItem(i, filler.clone());
			}
		}
	}

}
