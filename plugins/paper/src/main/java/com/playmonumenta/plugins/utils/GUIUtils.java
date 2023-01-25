package com.playmonumenta.plugins.utils;

import de.tr7zw.nbtapi.NBTItem;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;


public class GUIUtils {

	public static void splitLoreLine(ItemStack item, String lore, TextColor color, int maxLength, boolean clean) {
		ItemMeta meta = item.getItemMeta();
		splitLoreLine(meta, lore, color, maxLength, clean);
		item.setItemMeta(meta);
	}

	public static void splitLoreLine(ItemStack item, TextComponent lore, int maxLength, boolean clean) {
		ItemMeta meta = item.getItemMeta();
		splitLoreLine(meta, lore, maxLength, clean);
		item.setItemMeta(meta);
	}

	public static void splitLoreLine(ItemMeta meta, String lore, TextColor color, int maxLength, boolean clean) {
		splitLoreLine(meta, Component.text(lore, color), maxLength, clean);
	}

	public static void splitLoreLine(ItemMeta meta, TextComponent lore, int maxLength, boolean clean) {
		List<Component> prevLore = meta.lore();
		List<Component> lines = clean || prevLore == null ? new ArrayList<>() : prevLore;
		lines.addAll(splitLoreLine(lore, maxLength));
		meta.lore(lines);
	}

	private static List<Component> splitLoreLine(TextComponent lore, int maxLength) {
		String content = lore.content();
		if (lore.content().length() <= maxLength && !content.contains("\n")) {
			return List.of(fixLoreFormatting(lore));
		}
		String mini = MessagingUtils.toMiniMessage(lore);
		String[] splitLine = mini.split(" |(?=\n)"); // split on spaces, and on the empty string just before a line break
		StringBuilder currentLine = new StringBuilder();
		List<String> finalMinis = new ArrayList<>();

		for (String word : splitLine) {
			int wordLength = MessagingUtils.plainLengthFromMini(word);
			boolean newline = wordLength > 0 && word.charAt(0) == '\n';
			if (newline || MessagingUtils.plainLengthFromMini(currentLine.toString()) + wordLength > maxLength) {
				if (currentLine.length() > 0 && currentLine.charAt(currentLine.length() - 1) == ' ') {
					currentLine.setLength(currentLine.length() - 1);
				}
				String lastColor = findLastColor(currentLine.toString());
				finalMinis.add(currentLine.toString());
				currentLine.setLength(0);
				if (lastColor != null) {
					currentLine.append(lastColor);
				}
			}
			if (newline) {
				if (wordLength > 1) {
					currentLine.append(word.substring(1)).append(" ");
				}
			} else {
				currentLine.append(word).append(" ");
			}
		}
		if (!currentLine.toString().isEmpty()) {
			finalMinis.add(currentLine.toString());
		}

		return finalMinis.stream()
			.map(MessagingUtils::fromMiniMessage)
			.map(GUIUtils::fixLoreFormatting)
			.toList();
	}

	private static @Nullable String findLastColor(String mini) {
		String pattern = "<.+?>";
		Matcher matcher = Pattern.compile(pattern).matcher(mini);
		String match = null;
		while (matcher.find()) {
			match = matcher.group();
		}
		if (match == null || match.contains("/")) {
			return null;
		}
		return match;
	}

	private static Component fixLoreFormatting(Component c) {
		if (c.decoration(TextDecoration.ITALIC) == TextDecoration.State.NOT_SET) {
			c = c.decoration(TextDecoration.ITALIC, false);
		}
		c = c.colorIfAbsent(NamedTextColor.WHITE);
		return c;
	}

	public static ItemStack createBasicItem(Material mat, String name, TextColor nameColor, String desc) {
		return createBasicItem(mat, name, nameColor, false, desc);
	}

	public static ItemStack createBasicItem(Material mat, String name, TextColor nameColor, boolean nameBold, String desc) {
		return createBasicItem(mat, name, nameColor, nameBold, desc, NamedTextColor.GRAY);
	}

	public static ItemStack createBasicItem(Material mat, String name, TextColor nameColor, boolean nameBold, String desc, TextColor loreColor) {
		return createBasicItem(mat, name, nameColor, nameBold, desc, loreColor, 30);
	}

	public static ItemStack createBasicItem(Material mat, String name, TextColor nameColor, boolean nameBold, String desc, TextColor loreColor, int maxLoreLength) {
		return createBasicItem(mat, name, nameColor, nameBold, desc, loreColor, maxLoreLength, true);
	}

	public static ItemStack createBasicItem(Material mat, String name, TextColor nameColor, boolean nameBold, String desc, TextColor loreColor, int maxLoreLength, boolean setPlainTag) {
		return createBasicItem(mat, 1, name, nameColor, nameBold, desc, loreColor, maxLoreLength, setPlainTag);
	}

	public static ItemStack createBasicItem(Material mat, int amount, String name, TextColor nameColor, boolean nameBold, String desc, TextColor loreColor, int maxLoreLength, boolean setPlainTag) {
		return createBasicItem(mat, amount, formatName(name, nameColor, nameBold), desc, loreColor, maxLoreLength, setPlainTag);
	}

	public static ItemStack createBasicItem(Material mat, int amount, Component name, String desc, TextColor loreColor, int maxLoreLength, boolean setPlainTag) {
		return createBasicItem(mat, amount, name, Component.text(desc, loreColor), maxLoreLength, setPlainTag);
	}

	public static ItemStack createBasicItem(Material mat, int amount, String name, TextColor nameColor, boolean nameBold, TextComponent desc, int maxLoreLength, boolean setPlainTag) {
		return createBasicItem(mat, amount, formatName(name, nameColor, nameBold), desc, maxLoreLength, setPlainTag);
	}

	public static ItemStack createBasicItem(Material mat, int amount, Component name, TextComponent desc, int maxLoreLength, boolean setPlainTag) {
		return createBasicItem(mat, amount, name, splitLoreLine(desc, maxLoreLength), setPlainTag);
	}

	public static ItemStack createBasicItem(Material mat, int amount, String name, TextColor nameColor, boolean nameBold, List<Component> desc, boolean setPlainTag) {
		return createBasicItem(mat, amount, formatName(name, nameColor, nameBold), desc, setPlainTag);
	}

	public static ItemStack createBasicItem(Material mat, int amount, Component name, List<Component> desc, boolean setPlainTag) {
		ItemStack item = new ItemStack(mat, amount);
		ItemMeta meta = item.getItemMeta();
		meta.displayName(name);
		meta.lore(desc.stream().map(GUIUtils::fixLoreFormatting).toList());
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
		item.setItemMeta(meta);
		if (setPlainTag) {
			ItemUtils.setPlainTag(item);
		}
		return item;
	}

	private static Component formatName(String name, TextColor nameColor, boolean nameBold) {
		return Component.text(name, nameColor)
			       .decoration(TextDecoration.ITALIC, false)
			       .decoration(TextDecoration.BOLD, nameBold);
	}

	public static void fillWithFiller(Inventory inventory, Material fillerMaterial) {
		fillWithFiller(inventory, fillerMaterial, false);
	}

	public static void fillWithFiller(Inventory inventory, Material fillerMaterial, boolean clearInv) {
		ItemStack filler = new ItemStack(fillerMaterial, 1);
		ItemMeta meta = filler.getItemMeta();
		meta.displayName(Component.empty());
		filler.setItemMeta(meta);
		for (int i = 0; i < inventory.getSize(); i++) {
			if (inventory.getItem(i) == null || clearInv) {
				inventory.setItem(i, filler.clone());
			}
		}
	}

	/**
	 * Sets a tag in the "GUI" tag on the item. This allows resource packs to style icons differently even if the name is the same
	 * without having to rely on lore that may change often.
	 */
	public static void setGuiNbtTag(ItemStack item, String tagName, String value) {
		new NBTItem(item, true)
			.addCompound("GUI")
			.setString(tagName, value);
	}

}
