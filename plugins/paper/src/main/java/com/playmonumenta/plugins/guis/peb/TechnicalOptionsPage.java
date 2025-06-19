package com.playmonumenta.plugins.guis.peb;

import com.playmonumenta.plugins.commands.VirtualFirmament;
import com.playmonumenta.plugins.commands.WorldNameCommand;
import com.playmonumenta.plugins.guis.lib.ReactiveValue;
import com.playmonumenta.plugins.itemstats.infusions.Shattered;
import com.playmonumenta.plugins.protocollib.EntityEquipmentReplacer;
import com.playmonumenta.plugins.protocollib.RecipeBookGUIOpener;
import com.playmonumenta.plugins.protocollib.SpawnerEntityReplacer;
import com.playmonumenta.plugins.utils.GUIUtils;
import org.bukkit.Material;

final class TechnicalOptionsPage extends PebPage {
	TechnicalOptionsPage(PebGui gui) {
		super(gui, Material.COMPARATOR, "Technical Options", "Technical options");
	}

	@Override
	protected void render() {
		super.render();

		entry(
			Material.LEATHER_HELMET,
			"Display other players' gear",
			"Toggle whether you see the gear on other players. Improves client performance with the resource pack"
		).toggle(
			"Player gear: ",
			ReactiveValue.binaryScoreboard(mGui, EntityEquipmentReplacer.DISPLAY_GEAR_SCORE, true)
		).set(2, 2);

		entry(
			Material.SPAWNER,
			"Spawner Equipment",
			"Toggle whether mob equipment is displayed in spawners (significantly decreases FPS in many areas)"
		).toggle(
			"Spawner equipment: ",
			SpawnerEntityReplacer.TAG
		).set(2, 3);

		entry(
			Material.DEEPSLATE_EMERALD_ORE,
			"Trading GUI Options",
			"Choose your NPC trading preferences."
		).switchTo(PebGui.TRADE_GUI_PAGE).set(2, 4);

		entry(
			Material.COMPASS,
			"Compass Particles",
			"Toggle the trail of guiding particles when following the quest compass."
		).invertedToggle("Compass particles: ", "noCompassParticles").set(2, 5);

		entry(
			Material.GLOWSTONE,
			"Show name on patron buff announcement",
			"Toggle whether your IGN is in the announcement when they activate <gold>Patreon</gold> buffs."
		).invertedToggle("Show name: ", "patreon_shrine_privacy").set(2, 6);

		entry(
			Material.DAMAGED_ANVIL,
			"Shattered and Region Scaling Messages",
			"Toggle actionbar messages when you have equipment that is shattered or debuffed based on region scaling."
		).invertedToggle("Warnings: ", Shattered.MESSAGE_DISABLE_TAG).set(3, 2);

		entry(
			Material.BIRCH_SIGN,
			"Show Overworld POI Titles",
			"Toggle seeing titles appear upon entering certain Overworld Points of Interest."
		).invertedToggle("POI titles: ", ReactiveValue.binaryScoreboard(mGui, "POITitles", false)).set(3, 3);
		entry(
			Material.DAYLIGHT_DETECTOR,
			"Auto-Abandon Completed Dungeons",
			"Click to disable or enable automatically abandoning completed dungeon instances a week after your last visit."
		).invertedToggle("Auto-abandon: ", ReactiveValue.tag(mGui, "NoAutoDungeonAbandon")).set(3, 4);

		entry(
			Material.KNOWLEDGE_BOOK,
			"Recipe Book Opening Player Details",
			"Click here to disable or enable clicking on the recipe book opening the Player Details GUI"
		).toggle("Mode: ", RecipeBookGUIOpener.DISABLE_TAG, "<white>vanilla", "<white>player details GUI").set(3, 5);

		entry(
			Material.PRISMARINE,
			"Virtual Firmament",
			"Toggle Virtual Firmament, which visually turns your Firmament into a stack of blocks for faster placement."
		).toggle("Mode: ", VirtualFirmament.TAG).set(3, 6);

		entry(
			Material.CARTOGRAPHY_TABLE,
			"Spoof World Names",
			"Click to enable or disable spoofing of shard-specific world names. This is helpful for world map mods to be able to detect worlds better."
		).toggle("Mode: ", WorldNameCommand.TAG).set(4, 2);

		entry(
			Material.BEEHIVE,
			"Resource Pack GUI Textures",
			"Click to enable or disable the Monumenta Resource Pack applying background textures to various GUIs. " +
				"(Requires v5.0.0 or higher to work when enabled.)"
		).invertedToggle(
			"Custom textures: ",
			ReactiveValue.binaryScoreboard(mGui, GUIUtils.GUI_TEXTURES_OBJECTIVE, false)
		).set(4, 3);

		entry(
			Material.FLOWER_BANNER_PATTERN,
			"Simplified Tab List",
			"Click to enable or disable the simplified tab list, removing custom effects from the tab list."
				+ " Recommended if using an up-to-date version of the Unofficial Monumenta Mod (1.9.8+)."
		).toggle(
			"Simple tab list: ",
			ReactiveValue.permission(mGui, "monumenta.tablist.simplified")
		).set(4, 4);

		entry(
			Material.FLOWER_BANNER_PATTERN,
			"Swap Friends and Guild Sections in Tab List",
			"Click to swap the positions of the Friends and Guild sections in the tab list."
		).toggle(
			"Swap Friends and Guild: ",
			ReactiveValue.togglePermission(mGui, "monumenta.tablist.swapfriendsandguild")
		).set(4, 5);
	}
}
