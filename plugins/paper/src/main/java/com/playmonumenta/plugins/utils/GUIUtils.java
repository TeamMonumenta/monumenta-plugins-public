package com.playmonumenta.plugins.utils;

import de.tr7zw.nbtapi.NBTItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
		String content = lore.content();
		if (lore.content().length() <= maxLength && !content.contains("\n")) {
			meta.lore(List.of(lore));
			return;
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

		List<Component> finalLines = new ArrayList<>();
		if (!clean && meta.hasLore()) {
			finalLines.addAll(Objects.requireNonNull(meta.lore()));
		}
		finalLines.addAll(finalMinis.stream()
			.map(MessagingUtils::fromMiniMessage)
			.map(c -> {
				if (c.decoration(TextDecoration.ITALIC) == TextDecoration.State.NOT_SET) {
					return c.decoration(TextDecoration.ITALIC, false);
				}
				return c;
			})
			.map(c -> c.colorIfAbsent(NamedTextColor.WHITE))
			.toList());
		meta.lore(finalLines);
	}

	private static @Nullable String findLastColor(String mini) {
		String pattern = "<.+?>";
		Matcher matcher = Pattern.compile(pattern).matcher(mini);
		String match = null;
		while (matcher.find()) {
			MMLog.info(matcher.group());
			match = matcher.group();
		}
		if (match == null || match.contains("/")) {
			return null;
		}
		return match;
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
		Component nameComponent = Component.text(name, nameColor)
			.decoration(TextDecoration.ITALIC, false)
			.decoration(TextDecoration.BOLD, nameBold);
		return createBasicItem(mat, amount, nameComponent, desc, loreColor, maxLoreLength, setPlainTag);
	}

	public static ItemStack createBasicItem(Material mat, int amount, Component name, String desc, TextColor loreColor, int maxLoreLength, boolean setPlainTag) {
		return createBasicItem(mat, amount, name, Component.text(desc, loreColor), maxLoreLength, setPlainTag);
	}

	public static ItemStack createBasicItem(Material mat, int amount, String name, TextColor nameColor, boolean nameBold, TextComponent desc, int maxLoreLength, boolean setPlainTag) {
		Component nameComponent = Component.text(name, nameColor)
			.decoration(TextDecoration.ITALIC, false)
			.decoration(TextDecoration.BOLD, nameBold);
		return createBasicItem(mat, amount, nameComponent, desc, maxLoreLength, setPlainTag);
	}

	public static ItemStack createBasicItem(Material mat, int amount, Component name, TextComponent desc, int maxLoreLength, boolean setPlainTag) {
		ItemStack item = new ItemStack(mat, amount);
		ItemMeta meta = item.getItemMeta();
		meta.displayName(name);
		splitLoreLine(meta, desc, maxLoreLength, true);
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
		item.setItemMeta(meta);
		if (setPlainTag) {
			ItemUtils.setPlainTag(item);
		}
		return item;
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
