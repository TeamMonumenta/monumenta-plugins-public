package com.playmonumenta.plugins.guis.peb;

import com.playmonumenta.plugins.guis.lib.ReactiveValue;
import org.bukkit.Material;

final class SoundCategoriesPage extends PebPage {
	SoundCategoriesPage(PebGui gui) {
		super(
			gui,
			Material.PLAYER_HEAD,
			"Sound Options",
			"Use the menus and options below to customize your audio experience within Monumenta."
		);
	}

	private void put(Material material, String name, String desc, String prompt, String key, int row, int col) {
		entry(material, name, desc).invertedToggle(prompt, ReactiveValue.binaryScoreboard(mGui, key, false)).set(row, col);
	}

	@Override
	public void render() {
		super.render();
		put(Material.CHEST, "City Music", "Enable or disable music while in cities.", "City music: ", "MusicCity", 2, 1);
		put(Material.DIAMOND_SWORD, "Boss Music", "Enable or disable boss music.", "Boss music: ", "MusicBoss", 2, 4);
		put(Material.WOODEN_PICKAXE, "Dungeon Music", "Enable or disable dungeon music.", "Dungeon music: ", "MusicDungeon", 2, 7);
		put(Material.IRON_AXE, "Strike Music", "Enable or disable strike music.", "Strike music: ", "MusicStrike", 4, 1);
		put(Material.ITEM_FRAME, "Miscellaneous Music", "Enable or disable miscellaneous music.", "Miscellaneous music: ", "MusicMisc", 4, 7);
	}
}
