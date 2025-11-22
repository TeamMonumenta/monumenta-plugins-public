package com.playmonumenta.plugins.guis.peb;

import com.playmonumenta.plugins.custominventories.ClassDisplayCustomInventory;
import org.bukkit.Material;

final class PlayerInfoPage extends PebPage {
	PlayerInfoPage(PebGui gui) {
		super(gui, Material.PLAYER_HEAD, "Player Information", "Player information");
	}

	@Override
	protected void render() {
		super.render();

		entry(
			Material.STONE_SWORD,
			"Class",
			"Click to view your class and skills."
		).onMouseClick(() -> {
			mGui.close();
			new ClassDisplayCustomInventory(getPlayer()).open();
		}).set(2, 2);

		entry(
			Material.ACACIA_BOAT,
			"Dailies",
			"Click to see what daily content you have and haven't done today."
		).command("clickable peb_dailies").set(2, 4);

		entry(
			Material.WHITE_WOOL,
			"Dungeon Instances",
			"Click to view what dungeon instances you have open, and how old they are."
		).command("clickable peb_dungeoninfo").set(2, 6);

		entry(
			Material.GLOWSTONE_DUST,
			"Patron",
			"Click to view patron information. Use <b>/help donate</b> to learn about donating."
		).command("clickable peb_patroninfo").set(4, 2);

		entry(
			Material.KNOWLEDGE_BOOK,
			"Item Stats",
			"Click to view your current item stats and compare items."
		).command("playerstats").set(4, 6);
	}
}
