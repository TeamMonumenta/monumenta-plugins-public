package com.playmonumenta.plugins.guis.peb;

import org.bukkit.Material;

final class BookSkinsPage extends PebPage {
	public enum Skin {
		ENCHANTED_BOOK(Material.ENCHANTED_BOOK, "Enchanted Book", "enchantedbook"),
		REGAL(Material.YELLOW_CONCRETE, "Regal", "regal"),
		CRIMSON_KING(Material.RED_TERRACOTTA, "Crimson King", "ck"),
		ROSE(Material.RED_CONCRETE, "Rose", "rose"),
		WHITE(Material.WHITE_WOOL, "White", "white"),
		ORANGE(Material.ORANGE_WOOL, "Orange", "orange"),
		MAGENTA(Material.MAGENTA_WOOL, "Magenta", "magenta"),
		LIGHT_BLUE(Material.LIGHT_BLUE_WOOL, "Light Blue", "lightblue"),
		YELLOW(Material.YELLOW_WOOL, "Yellow", "yellow"),
		LIME(Material.LIME_WOOL, "Lime", "lime"),
		PINK(Material.PINK_WOOL, "Pink", "pink"),
		GRAY(Material.GRAY_WOOL, "Gray", "gray"),
		LIGHT_GRAY(Material.LIGHT_GRAY_WOOL, "Light Gray", "lightgray"),
		CYAN(Material.CYAN_WOOL, "Cyan", "cyan"),
		PURPLE(Material.PURPLE_WOOL, "Purple", "purple"),
		BLUE(Material.BLUE_WOOL, "Blue", "blue"),
		BROWN(Material.BROWN_WOOL, "Brown", "brown"),
		GREEN(Material.GREEN_WOOL, "Green", "green"),
		RED(Material.RED_WOOL, "Red", "red"),
		BLACK(Material.BLACK_WOOL, "Black", "black");

		private final Material mMaterial;
		private final String mName;
		private final String mId;

		Skin(Material material, String name, String id) {
			this.mMaterial = material;
			this.mName = name;
			this.mId = id;
		}
	}

	BookSkinsPage(PebGui gui) {
		super(gui, Material.ENCHANTED_BOOK, "Book Skins", "Configure PEB skins");
	}

	@Override
	protected void render() {
		super.render();

		for (int i = 0; i < Skin.values().length; i++) {
			Skin value = Skin.values()[i];
			entry(value.mMaterial, value.mName, "Set PEB skin to " + value.mName)
				.command("clickable peb_skin_" + value.mId)
				.set(i / 7 + 2, i % 7 + 1);
		}
	}
}
