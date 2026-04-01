package com.playmonumenta.plugins.utils;

import com.playmonumenta.plugins.Constants;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class DescriptionUtils {
	/**
	 * Commonly used styles amongst all descriptions. Consistent color palette is important!
	 */
	public static final Style WHITE = Style.style(TextColor.color(0xF2F0EF));
	public static final Style LIGHT_GREY = Style.style(TextColor.color(0xC9C9C9));
	public static final Style GREY = Style.style(TextColor.color(0x999999));
	public static final Style DARK_GREY = Style.style(TextColor.color(0x404040));
	public static final Style BLACK = Style.style(TextColor.color(0x303030));
	public static final Style RED = Style.style(TextColor.color(0xFF4040));
	public static final Style GOLD = Style.style(TextColor.color(0xFFAA00));
	public static final Style GREEN = Style.style(TextColor.color(0x40FF40));

	public static final Style UNDERLINED = Style.style(TextDecoration.UNDERLINED);
	public static final Style DISABLED = DARK_GREY.decorate(TextDecoration.STRIKETHROUGH);
	public static final Style REGION_SCALED = Style.style(TextColor.color(0x99DDFF));

	public static final Style TRIGGER_LABEL = Style.style(TextColor.color(0xEDA73D));
	public static final Style TRIGGER_TEXT = Style.style(TextColor.color(0xFFDC73));
	public static final Style REQUIREMENT_LABEL = Style.style(TextColor.color(0x39B445));
	public static final Style REQUIREMENT_TEXT = Style.style(TextColor.color(0x7BD182));

	public static final Style ACTION_DENIED = Style.style(TextColor.color(0xB22222));
	public static final Style ACTION_SELECT = Style.style(TextColor.color(0xFFFF55));
	public static final Style ACTION_COMPLETED = Style.style(TextColor.color(0x8FFF40));

	public static final Style ALCHEMIST_ARROW = Style.style(TextColor.color(0x5D8514));
	public static final Style ALCHEMIST_LORE = Style.style(TextColor.color(0xBDDE7A), TextDecoration.ITALIC);
	public static final Style CLERIC_ARROW = Style.style(TextColor.color(0xA27B0F));
	public static final Style CLERIC_LORE = Style.style(TextColor.color(0xE6C58A), TextDecoration.ITALIC);
	public static final Style MAGE_ARROW = Style.style(TextColor.color(0x571A79));
	public static final Style MAGE_LORE = Style.style(TextColor.color(0xB790DE), TextDecoration.ITALIC);
	public static final Style ROGUE_ARROW = Style.style(TextColor.color(0x2C2642));
	public static final Style ROGUE_LORE = Style.style(TextColor.color(0xACA4B3), TextDecoration.ITALIC);
	public static final Style SCOUT_ARROW = Style.style(TextColor.color(0x246082));
	public static final Style SCOUT_LORE = Style.style(TextColor.color(0xA3D6F3), TextDecoration.ITALIC);
	public static final Style SHAMAN_ARROW = Style.style(TextColor.color(0x0A410E));
	public static final Style SHAMAN_LORE = Style.style(TextColor.color(0x8BD98B), TextDecoration.ITALIC);
	public static final Style WARLOCK_ARROW = Style.style(TextColor.color(0x7B1D4F));
	public static final Style WARLOCK_LORE = Style.style(TextColor.color(0xE6A1C3), TextDecoration.ITALIC);
	public static final Style WARRIOR_ARROW = Style.style(TextColor.color(0x76100B));
	public static final Style WARRIOR_LORE = Style.style(TextColor.color(0xE08787), TextDecoration.ITALIC);

	/**
	 * Gets the longest width of a line in this component, in Minecraft font pixels.
	 * Not related to string length; this represents how wide the text will appear on your screen.
	 * @param component the component to find the longest width of
	 * @return the width of the longest line in this component
	 */
	public static int getLongestWidth(Component component) {
		int longestWidth = 0;
		String string = PlainTextComponentSerializer.plainText().serialize(component);
		for (String line : string.split("\n ")) {
			longestWidth = Math.max(longestWidth, getPixelWidth(line));
		}
		return longestWidth;
	}

	/**
	 * Calculates the horizontal length a string will take up in Minecraft font pixels.
	 * Not related to string length; this represents how wide the text will appear on your screen.
	 * @param string the string to calculate the pixel width of
	 * @return the horizontal visual width of the string
	 */
	public static int getPixelWidth(String string) {
		return getPixelWidth(string, false);
	}

	/**
	 * Calculates the horizontal length a string will take up in Minecraft font pixels.
	 * Not related to string length; this represents how wide the text will appear on your screen.
	 * @param string the string to calculate the pixel width of
	 * @param bold whether the string is bolded or not (makes characters wider)
	 * @return the horizontal visual width of the string
	 */
	public static int getPixelWidth(String string, boolean bold) {
		// For the sake of determining line width, we need to replace keybinds ("key.swapOffhand") because they'll
		// make things inaccurate. We can't get the client's keybinds, so just use the default
		string = string.replaceAll(Constants.Keybind.ATTACK.asKeybind(), "Left Button")
			.replaceAll(Constants.Keybind.USE.asKeybind(), "Right Button")
			.replaceAll(Constants.Keybind.SWAP_OFFHAND.asKeybind(), "F")
			.replaceAll(Constants.Keybind.DROP.asKeybind(), "Q");

		int pixelWidth = 0;
		for (int i = 0; i < string.length(); i++) {
			char character = string.charAt(i);
			pixelWidth += switch (character) {
				case '\n' -> 0;
				case '.', ',', ':', '\'', 'i', '!' -> 2;
				case 'l' -> 3;
				case ' ', '(', ')', '[', ']', 't', 'I' -> 4;
				case 'f', 'k' -> 5;
				case '▶' -> 7;
				case '→', '–' -> 8;
				default -> 6;
			};

			if (bold) {
				pixelWidth++;
			}
		}
		return pixelWidth;
	}

	/**
	 * Prepends a string with spaces so that it is center aligned within a larger component.
	 * @param component the component in which to align the input in
	 * @param text the string to center align
	 * @param style the style to apply to the text
	 * @return the horizontal visual width of the string
	 */
	public static Component centeredComponent(Component component, String text, Style style) {
		return centeredComponent(component, text, style, false);
	}

	/**
	 * Prepends a string with spaces so that it is center aligned within a larger component.
	 * @param component the component in which to align the input in
	 * @param text the string to center align
	 * @param color the color to apply to the text
	 * @param bold whether the text is bolded or not
	 * @return the horizontal visual width of the string
	 */
	public static Component centeredComponent(Component component, String text, TextColor color, boolean bold) {
		return centeredComponent(component, text, Style.style(color), bold);
	}

	/**
	 * Prepends a string with spaces so that it is center aligned within a larger component.
	 * @param component the component in which to align the input in
	 * @param text the string to center align
	 * @param style the style to apply to the text
	 * @param bold whether the string is bolded or not (makes characters wider)
	 * @return the horizontal visual width of the string
	 */
	public static Component centeredComponent(Component component, String text, Style style, boolean bold) {
		if (text == null || text.isBlank()) {
			return Component.empty();
		}

		Component result = Component.text(text).style(style).decoration(TextDecoration.BOLD, bold).decoration(TextDecoration.ITALIC, false);

		int outerWidth = getLongestWidth(component);
		int innerWidth = getPixelWidth(text, bold);
		int pixelsToFill = (outerWidth - innerWidth) / 2;
		if (pixelsToFill > 0) {
			// yes, in minecraft, bolded numSpaces have more width than numSpaces, but this lets us fine-tune the width.
			int numBoldSpaces = 0;
			int numSpaces = 0;

			for (int i = pixelsToFill / 5; i >= 0; i--) {
				int remainder = pixelsToFill - i * 5;
				if (remainder > 0 && remainder % 4 == 0) {
					numBoldSpaces = i;
					numSpaces = remainder / 4;
					break;
				}
			}

			Component boldSpaces = Component.text(" ".repeat(numBoldSpaces)).decoration(TextDecoration.BOLD, true);
			Component spaces = Component.text(" ".repeat(numSpaces)).decoration(TextDecoration.BOLD, false);

			result = boldSpaces.append(spaces).append(result).append(spaces).append(boldSpaces);
		}

		return result;
	}

	public static Component actionLine(String text, Style style) {
		return Component.text("|| ", BLACK).append(Component.text(text, style));
	}
}
