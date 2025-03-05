package com.playmonumenta.plugins.guis.peb;

import com.playmonumenta.plugins.Plugin;
import org.bukkit.Material;

final class ServerInfoPage extends PebPage {
	@SuppressWarnings("UnstableApiUsage")
	private static final String VERSION = Plugin.getInstance().getPluginMeta().getVersion();

	ServerInfoPage(PebGui gui) {
		super(gui, Material.DISPENSER, "Sound Categories", "Use the options below to choose settings for certain categories of music.");
	}

	@Override
	protected void render() {
		super.render();

		entry(
			Material.OAK_SIGN,
			"Version",
			"Monumenta <b><green>" + VERSION
		).set(2, 4);

		entry(
			Material.ENCHANTED_BOOK,
			"P.E.B. Introduction",
			"Click to hear the P.E.B. Introduction."
		).command("clickable peb_intro").set(3, 2);

		entry(
			Material.REDSTONE_TORCH,
			"Get a random tip!",
			"Click to get a random tip!"
		).command("clickable peb_tip").set(3, 6);
	}
}
