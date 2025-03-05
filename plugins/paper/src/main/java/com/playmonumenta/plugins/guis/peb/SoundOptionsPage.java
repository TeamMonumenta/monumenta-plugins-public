package com.playmonumenta.plugins.guis.peb;

import com.playmonumenta.plugins.guis.lib.ReactiveValue;
import org.bukkit.Material;

final class SoundOptionsPage extends PebPage {
	SoundOptionsPage(PebGui gui) {
		super(
			gui,
			Material.JUKEBOX,
			"Sound Options",
			"Use the menus and options below to customize your audio experience within Monumenta."
		);
	}

	private void delay(Material material, String name, String score, int row) {
		entry(material, name, "").cycle(ReactiveValue.scoreboard(mGui, score, 0),
			"There will be 10 minutes between the start of each play of music.",
			"There will be no delay between loops of the official theme. There will be 4 minutes between the start of each play of a custom theme."
		).set(row, 6);
	}

	@Override
	public void render() {
		super.render();

		entry(
			Material.CHEST,
			"Sound Categories",
			"Control the settings related to categories such as boss music, strike music, and city music."
		).switchTo(PebGui.SOUND_CATEGORIES_PAGE).set(2, 2);

		entry(
			Material.COMPASS,
			"Overworld and Plots",
			"Toggle between original, custom, and disabled music for the overworlds and plots."
		).switchTo(PebGui.SOUND_OVERWORLD_PLOTS_PAGE).set(3, 2);

		entry(
			Material.ENDER_PEARL,
			"Teleporter Cutoffs",
			"Click to toggle if teleporters will stop the currently playing song."
		).toggle(
			"Stop current song: ",
			ReactiveValue.binaryScoreboard(mGui, "MusicStopResetTpToggle", true)
		).set(2, 4);

		delay(Material.BRICKS, "Overworld Music Delay", "MusicOverworldDelay", 2);
		delay(Material.WOODEN_PICKAXE, "Dungeon Music Delay", "MusicDungeonDelay", 3);
		delay(Material.CRAFTING_TABLE, "Plots Music Delay", "MusicPlotsDelay", 4);
	}
}
