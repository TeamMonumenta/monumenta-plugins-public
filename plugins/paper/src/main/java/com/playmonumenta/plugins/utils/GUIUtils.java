package com.playmonumenta.plugins.utils;

import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.nbtapi.iface.ReadableNBT;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.Nullable;


public class GUIUtils {
	public static final String GUI_KEY = "Gui";
	public static final String PLACEHOLDER_KEY = "Placeholder";
	public static final String FILLER_KEY = "Filler";
	public static final String GUI_TEXTURES_OBJECTIVE = "GUITextures";
	public static final Material FILLER_MATERIAL = Material.LIGHT_GRAY_STAINED_GLASS_PANE;
	public static final ItemStack FILLER = createFiller();
	public static final Pattern RE_ALT_NEWLINES = Pattern.compile("<newline>|<br>");

	private static ItemStack createFiller() {
		return createFiller(FILLER_MATERIAL);
	}

	public static ItemStack createFiller(Material filler) {
		return createFiller(new ItemStack(filler));
	}

	public static ItemStack createFiller(ItemStack filler) {
		NBT.modify(filler, nbt -> {
			nbt.modifyMeta((unused, meta) -> meta.displayName(Component.empty()));
			setPlaceholder(nbt);
			setFiller(nbt);
		});
		return filler;
	}

	public static ItemStack createConfirm(@Nullable List<Component> lore) {
		ItemStack confirm = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
		ItemMeta meta = confirm.getItemMeta();
		meta.displayName(Component.text("Confirm", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
		if (lore != null) {
			meta.lore(lore);
		}
		confirm.setItemMeta(meta);
		ItemUtils.setPlainTag(confirm);
		NBT.modify(confirm, nbt -> {
			nbt.getOrCreateCompound("plain").getOrCreateCompound("display").setString("Name", "gui_checkmark");
		});
		return confirm;
	}

	public static ItemStack createCancel(@Nullable List<Component> lore) {
		return createCancel(Component.text("Cancel", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true), lore);
	}

	public static ItemStack createCancel(Component displayName, @Nullable List<Component> lore) {
		ItemStack cancel = new ItemStack(Material.ORANGE_STAINED_GLASS_PANE);
		ItemMeta meta = cancel.getItemMeta();
		meta.displayName(displayName);
		if (lore != null) {
			meta.lore(lore);
		}
		cancel.setItemMeta(meta);
		ItemUtils.setPlainTag(cancel);
		NBT.modify(cancel, nbt -> {
			nbt.getOrCreateCompound("plain").getOrCreateCompound("display").setString("Name", "gui_cancel");
		});
		return cancel;
	}

	public static ItemStack createExclamation(@Nullable List<Component> lore) {
		ItemStack exclamation = new ItemStack(Material.GOLD_INGOT);
		ItemMeta meta = exclamation.getItemMeta();
		meta.displayName(Component.text("!", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false).decoration(TextDecoration.BOLD, true));
		if (lore != null) {
			meta.lore(lore);
		}
		exclamation.setItemMeta(meta);
		ItemUtils.setPlainTag(exclamation);
		NBT.modify(exclamation, nbt -> {
			nbt.getOrCreateCompound("plain").getOrCreateCompound("display").setString("Name", "gui_exclamation_mark");
		});
		return exclamation;
	}

	public static void splitLoreLine(ItemStack item, String lore, TextColor color, boolean clean) {
		splitLoreLine(item, lore, color, 30, clean);
	}

	public static void splitLoreLine(ItemStack item, String lore, TextColor color, int maxLength, boolean clean) {
		ItemMeta meta = item.getItemMeta();
		splitLoreLine(meta, lore, color, maxLength, clean);
		item.setItemMeta(meta);
	}

	public static void splitLoreLine(ItemStack item, Component lore, int maxLength, boolean clean) {
		ItemMeta meta = item.getItemMeta();
		splitLoreLine(meta, lore, maxLength, clean);
		item.setItemMeta(meta);
	}

	public static void splitLoreLine(ItemMeta meta, String lore, TextColor color, int maxLength, boolean clean) {
		splitLoreLine(meta, Component.text(lore, color), maxLength, clean);
	}

	public static void splitLoreLine(ItemMeta meta, Component lore, int maxLength, boolean clean) {
		List<Component> prevLore = meta.lore();
		List<Component> lines = clean || prevLore == null ? new ArrayList<>() : prevLore;
		lines.addAll(splitLoreLine(lore, maxLength));
		meta.lore(lines);
	}

	public static List<Component> splitLoreLine(Component lore, int maxLength) {
		String mini = MessagingUtils.toMiniMessage(lore);
		if (mini.isEmpty()) {
			return new ArrayList<>();
		}
		mini = RE_ALT_NEWLINES.matcher(mini).replaceAll("\n");
		if (mini.length() <= maxLength && !mini.contains("\n")) {
			return List.of(fixLoreFormatting(lore));
		}
		String[] splitLine = mini.split(" |(?=\n)"); // split on spaces, and on the empty string just before a line break
		StringBuilder currentLine = new StringBuilder();
		List<String> finalMinis = new ArrayList<>();

		for (String word : splitLine) {
			int wordLength = MessagingUtils.plainLengthFromMini(word);
			boolean newline = wordLength > 0 && word.charAt(0) == '\n';
			if (newline || MessagingUtils.plainLengthFromMini(currentLine.toString()) + wordLength > maxLength) {
				if (!currentLine.isEmpty() && currentLine.charAt(currentLine.length() - 1) == ' ') {
					currentLine.setLength(currentLine.length() - 1);
				}
				String lineToAdd = currentLine.toString();
				finalMinis.add(lineToAdd);
				currentLine.setLength(0);

				// Add any unclosed minimessage formatting to the start of the next line, in the same order
				List<String> lingeringFormatting = findOpenFormatting(lineToAdd);
				Collections.reverse(lingeringFormatting);
				for (String format : lingeringFormatting) {
					currentLine.insert(0, format);
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

	private static List<String> findOpenFormatting(String mini) {
		String pattern = "<.+?>";
		Matcher matcher = Pattern.compile(pattern).matcher(mini);
		List<String> formats = new ArrayList<>();
		while (matcher.find()) {
			String match = matcher.group();
			if (!match.contains("/")) {
				formats.add(match);
			} else {
				String openMatch = match.replace("/", "");
				formats.removeIf(openMatch::equals);
			}
		}
		return formats;
	}

	private static Component fixLoreFormatting(Component c) {
		if (c.decoration(TextDecoration.ITALIC) == TextDecoration.State.NOT_SET) {
			c = c.decoration(TextDecoration.ITALIC, false);
		}
		c = c.colorIfAbsent(NamedTextColor.WHITE);
		return c;
	}

	public static ItemStack createBasicItem(Material mat, String name, TextColor nameColor) {
		return createBasicItem(mat, name, nameColor, false);
	}

	public static ItemStack createBasicItem(Material mat, String name, TextColor nameColor, boolean nameBold) {
		return createBasicItem(mat, name, nameColor, nameBold, "");
	}

	public static ItemStack createBasicItem(Material mat, int amount, String name, TextColor nameColor) {
		return createBasicItem(mat, amount, name, nameColor, false, "", NamedTextColor.GRAY, 30, true);
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
		return createBasicItem(mat, amount, name, desc.isEmpty() ? Component.empty() : Component.text("", loreColor).append(MessagingUtils.fromMiniMessage(desc)), maxLoreLength, setPlainTag);
	}

	public static ItemStack createBasicItem(Material mat, int amount, String name, TextColor nameColor, boolean nameBold, Component desc, int maxLoreLength, boolean setPlainTag) {
		return createBasicItem(mat, amount, formatName(name, nameColor, nameBold), desc, maxLoreLength, setPlainTag);
	}

	public static ItemStack createBasicItem(Material mat, int amount, Component name, Component desc, int maxLoreLength, boolean setPlainTag) {
		return createBasicItem(mat, amount, name, splitLoreLine(desc, maxLoreLength), setPlainTag);
	}

	public static ItemStack createBasicItem(Material mat, int amount, String name, TextColor nameColor, boolean nameBold, List<Component> desc, boolean setPlainTag) {
		return createBasicItem(mat, amount, formatName(name, nameColor, nameBold), desc, setPlainTag);
	}

	public static ItemStack createBasicItem(Material mat, Component name, List<String> desc, TextColor loreColor) {
		return createBasicItem(mat, 1, name, desc, loreColor);
	}

	public static ItemStack createBasicItem(Material mat, int amount, Component name, List<String> desc, TextColor loreColor) {
		return createBasicItem(mat, amount, name, desc, loreColor, true);
	}

	public static ItemStack createBasicItem(Material mat, int amount, Component name, List<String> desc, TextColor loreColor, boolean setPlainTag) {
		return createBasicItem(mat, amount, name, desc.stream().map(s -> (Component) Component.text(s, loreColor)).toList(), setPlainTag);
	}

	public static ItemStack createBasicItem(Material mat, int amount, Component name, List<Component> desc, boolean setPlainTag) {
		return createBasicItem(new ItemStack(mat, amount), amount, name, desc, setPlainTag);
	}

	public static ItemStack createBasicItem(Material mat, Component name) {
		return createBasicItem(mat, name, null);
	}

	public static ItemStack createBasicItem(Material mat, Component name, @Nullable String displayPlainTagValue) {
		return createBasicItem(mat, 1, name, new ArrayList<>(), true, displayPlainTagValue);
	}

	public static ItemStack createBasicItem(Material mat, int amount, Component name, List<Component> desc, boolean setPlainTag, @Nullable String displayPlainTagValue) {
		return createBasicItem(new ItemStack(mat, amount), amount, name, desc, setPlainTag, displayPlainTagValue);
	}

	public static ItemStack createBasicItem(ItemStack base, int amount, Component name, List<Component> desc, boolean setPlainTag) {
		return createBasicItem(base, amount, name, desc, setPlainTag, null);
	}

	public static ItemStack createBasicItem(ItemStack base, int amount, Component name, List<Component> desc, boolean setPlainTag, @Nullable String displayPlainTagValue) {
		ItemStack item = ItemUtils.clone(base);
		item.setAmount(amount);
		ItemMeta meta = item.getItemMeta();
		meta.displayName(name);
		meta.lore(desc.stream().map(GUIUtils::fixLoreFormatting).toList());
		meta.addItemFlags(ItemFlag.values());
		item.setItemMeta(meta);
		if (setPlainTag) {
			ItemUtils.setPlainTag(item);
		}
		setPlaceholder(item);
		if (displayPlainTagValue != null && !displayPlainTagValue.isEmpty()) {
			setDisplayPlainTag(item, displayPlainTagValue);
		}
		return item;
	}

	private static void setDisplayPlainTag(ItemStack icon, String value) {
		ItemUtils.setPlainTag(icon);
		NBT.modify(icon, nbt -> {
			nbt.getOrCreateCompound("plain").getOrCreateCompound("display").setString("Name", value);
		});
	}

	public static Component formatName(String name, TextColor nameColor, boolean nameBold) {
		return Component.text(name, nameColor)
			.decoration(TextDecoration.ITALIC, false)
			.decoration(TextDecoration.BOLD, nameBold);
	}

	public static void fillWithFiller(Inventory inventory) {
		fillWithFiller(inventory, false);
	}

	public static void fillWithFiller(Inventory inventory, boolean clearInv) {
		fillWithFiller(inventory, FILLER, clearInv);
	}

	public static void fillWithFiller(Inventory inventory, Material material) {
		fillWithFiller(inventory, material, false);
	}

	public static void fillWithFiller(Inventory inventory, Material material, boolean clearInv) {
		ItemStack filler = new ItemStack(material, 1);
		createFiller(filler);
		fillWithFiller(inventory, filler, clearInv);
	}

	public static void fillWithFiller(Inventory inventory, ItemStack filler, boolean clearInv) {
		for (int i = 0; i < inventory.getSize(); i++) {
			if (inventory.getItem(i) == null || clearInv) {
				inventory.setItem(i, filler);
			}
		}
	}

	/**
	 * Checks if the player has the required toggle for GUI textures enabled. Enabled when the score is 0.
	 */
	public static boolean getGuiTextureObjective(Player player) {
		return ScoreboardUtils.getScoreboardValue(player, GUI_TEXTURES_OBJECTIVE).orElse(0) == 0;
	}

	/**
	 * Sets a tag in the "GUI" tag on the item. This allows resource packs to style icons differently even if the name is the same
	 * without having to rely on lore that may change often.
	 * <p>
	 * Also returns the argument passed in, which is useful for nested function calls.
	 */
	public static ItemStack setGuiNbtTag(ItemStack item, String tagName, @Nullable String value, boolean active) {
		if (value == null) {
			return item;
		}
		if (active) {
			NBT.modify(item, nbt -> {
				nbt.getOrCreateCompound(GUI_KEY).setString(tagName, value);
			});
		}
		return item;
	}

	/**
	 * Creates a gui identifier item. This is for the rp to apply gui texture. Other than that it works like a filler.
	 */
	public static ItemStack createGuiIdentifierItem(String tag, boolean active) {
		ItemStack idItem = createFiller();
		setGuiNbtTag(idItem, "texture", tag, active);
		return idItem;
	}

	public static ItemStack createGuiIdentifierItem(ItemStack item, String tag, boolean active) {
		ItemStack idItem = ItemUtils.clone(item);
		setGuiNbtTag(idItem, "texture", tag, active);
		return idItem;
	}

	public static boolean isPlaceholder(final @Nullable ItemStack item) {
		if (ItemUtils.isNullOrAir(item)) {
			return false;
		}
		return NBT.get(item, nbt -> {
			ReadableNBT monumenta = nbt.getCompound(GUI_KEY);
			if (monumenta == null) {
				return false;
			}

			return monumenta.hasTag(PLACEHOLDER_KEY);
		});
	}

	public static void setPlaceholder(final @Nullable ItemStack item) {
		if (ItemUtils.isNullOrAir(item)) {
			return;
		}
		NBT.modify(item, nbt -> {
			setPlaceholder(nbt);
		});
	}

	public static void setPlaceholder(ReadWriteNBT nbt) {
		nbt.getOrCreateCompound(GUI_KEY).setBoolean(PLACEHOLDER_KEY, true);
	}

	public static void setFiller(ReadWriteNBT nbt) {
		nbt.getOrCreateCompound(GUI_KEY).setBoolean(FILLER_KEY, true);
		nbt.getOrCreateCompound("plain").getOrCreateCompound("display").setString("Name", "gui_blank");
	}

	public static void refreshOffhand(InventoryClickEvent event) {
		if (event.isCancelled()
			&& ClickType.SWAP_OFFHAND.equals(event.getClick())
			&& event.getWhoClicked() instanceof Player player) {
			PlayerInventory inventory = player.getInventory();
			ItemStack offhandItem = inventory.getItemInOffHand();
			inventory.setItemInOffHand(offhandItem);
		}
	}

	public static void setSkullOwner(ItemStack item, OfflinePlayer player) {
		if (item.getItemMeta() instanceof SkullMeta skullMeta) {
			skullMeta.setOwningPlayer(player);
			item.setItemMeta(skullMeta);
		}
	}
}
