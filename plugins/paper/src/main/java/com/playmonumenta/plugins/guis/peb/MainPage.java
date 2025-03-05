package com.playmonumenta.plugins.guis.peb;

import org.bukkit.Material;

final class MainPage extends PebPage {
	MainPage(PebGui gui) {
		super(gui, Material.PLAYER_HEAD, "Main Menu", "Player settings");
	}

	@Override
	public void render() {
		super.render();

		entry(
			Material.DIRT,
			"Filtered Pickup and Disabled Drop",
			"Click to choose your pickup and disabled drop preferences."
		).switchTo(PebGui.PICKUP_AND_DISABLE_DROP_PAGE).set(2, 1);

		entry(
			Material.NETHER_STAR,
			"Particle Options",
			"Click to choose how many particles will be shown for different categories. Also available under " +
				"Gameplay/Combat options."
		).switchTo(PebGui.PARTIAL_PARTICLES_PAGE).set(2, 2);

		entry(
			Material.SPECTRAL_ARROW,
			"Glowing options",
			"Click to choose your preferences for the \"glowing\" effect. Also available under Gameplay/Combat " +
				"options."
		).switchTo(PebGui.GLOWING_PAGE).set(2, 3);

		entry(
			Material.JUKEBOX,
			"Music Options",
			"Click to choose your preferences across a wide variety of music"
		).switchTo(PebGui.SOUND_CONTROLS_PAGE).set(2, 5);

		entry(
			Material.ACACIA_BOAT,
			"Dailies",
			"Click to see what daily content you have and haven't done today."
		).command("clickable peb_dailies").set(2, 6);

		entry(
			Material.WHITE_WOOL,
			"Dungeon Instances",
			"Click to view what dungeon instances you have open, and how old they are."
		).command("clickable peb_dungeoninfo").set(2, 7);

		entry(
			Material.DIAMOND_SWORD,
			"Gameplay/Combat Options",
			"Particle options, skill-related toggles, and other toggle related to combat."
		).switchTo(PebGui.GAMEPLAY_OPTIONS_PAGE).set(4, 1);

		entry(
			Material.COMPARATOR,
			"Technical Options",
			"Dungeon auto-abandon, world name spoofing, GUI options, and other technical enhancements."
		).switchTo(PebGui.TECHNICAL_OPTIONS_PAGE).set(4, 2);

		entry(
			Material.SHIELD,
			"Trigger/Interactable Options",
			"Offhand Swap, Filtered Pickup, and more options to change or disable triggers."
		).switchTo(PebGui.INTERACTABLE_OPTIONS_PAGE).set(4, 3);

		entry(
			Material.PLAYER_HEAD,
			"Player Information",
			"Details about your Class, Dailies, and other player-focused options."
		).switchTo(PebGui.PLAYER_INFO_PAGE).set(4, 5);

		entry(
			Material.DISPENSER,
			"Server Information",
			"Information such as how to use the PEB and random tips."
		).switchTo(PebGui.SERVER_INFO_PAGE).set(4, 6);

		entry(
			Material.ENCHANTED_BOOK,
			"Book Skins",
			"Change the color of the cover on your P.E.B."
		).switchTo(PebGui.BOOK_SKINS_PAGE).set(4, 7);
	}
}
