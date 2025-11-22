package com.playmonumenta.plugins.guis.peb;

import com.playmonumenta.plugins.guis.lib.ReactiveValue;
import org.bukkit.Material;

final class SoundOverworldPlotsPage extends PebPage {
	SoundOverworldPlotsPage(PebGui gui) {
		super(
			gui,
			Material.COMPASS,
			"Overworld and Plots",
			"Use the options below to choose settings for certain categories of music."
		);
	}

	private void put(Material material, String title, String desc, String score, int row, int col, String... opts) {
		entry(
			material,
			title,
			desc
		).cycle(ReactiveValue.scoreboard(mGui, score, 0), opts).set(row, col);
	}

	private void put(Material material, String title, String desc, String score, int row, int col) {
		put(material, title, desc, score, row, col, "Official Theme", "Custom Theme", "No Theme");
	}

	@Override
	public void render() {
		super.render();

		put(
			Material.JUNGLE_SAPLING,
			"King’s Valley Music",
			"Choose what sounds to play in the King's Valley", "MusicOverworldValley",
			2, 1
		);

		put(
			Material.SAND,
			"Celsian Isles Music",
			"Choose what sounds to play in the Celsian Isles",
			"MusicOverworldIsles",
			2, 4
		);

		put(
			Material.RED_MUSHROOM_BLOCK,
			"Silvaria Music",
			"Choose what sounds to play in Silvaria",
			"MusicOverworldRing",
			2, 7
		);

		put(
			Material.AMETHYST_BLOCK,
			"Star Point Music",
			"Choose what sounds to play in Star Point",
			"MusicOverworldStarpoint",
			4, 1
		);

		put(
			Material.CRAFTING_TABLE,
			"Plots and Playerplots Music",
			"Enable or disable plots/playerplots music.",
			"MusicPlots",
			4, 4,
			"Official Theme",
			"Shuffled Themes",
			"Custom Theme",
			"No Theme"
		);

		put(
			Material.REDSTONE_LAMP,
			"Lobby Music",
			"Choose what to play in lobbies",
			"MusicLobby",
			4,
			7
		);
	}
}
