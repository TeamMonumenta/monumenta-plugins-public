package com.playmonumenta.plugins.delves;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.guis.Gui;
import com.playmonumenta.plugins.guis.GuiItem;
import com.playmonumenta.plugins.utils.GUIUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class DelvePresetSelectionGui extends Gui {
	private final String mDungeon;
	private final boolean mGuiTextures;

	public DelvePresetSelectionGui(Player player, String dungeon) {
		super(player, 27, Component.text("Choose a Delve Preset"));
		mDungeon = dungeon;
		mGuiTextures = GUIUtils.getGuiTextureObjective(player);
	}

	@Override
	public void setup() {
		ItemStack background = GUIUtils.createGuiIdentifierItem("gui_delve_2", mGuiTextures);
		setItem(0, 0, background);

		Map<Integer, List<DelvePreset>> levelMap = new HashMap<>();
		for (int i = 1; i <= 3; i++) {
			levelMap.put(i, new ArrayList<>());
		}
		for (DelvePreset preset : DelvePreset.values()) {
			if (preset.isDungeonChallengePreset()) {
				// Assumes all challenge presets are last, if this changes for some reason, can just be continue
				break;
			}
			List<DelvePreset> list = levelMap.get(preset.mLevel);
			if (list != null) {
				list.add(preset);
			}
		}

		levelMap.forEach((level, list) -> {
			int start = 4 - list.size() / 2;
			for (int i = 0; i < list.size(); i++) {
				setItem(level - 1, i + start, createItem(list.get(i)));
			}
		});
	}

	private GuiItem createItem(DelvePreset preset) {
		ItemStack item = GUIUtils.createBasicItem(preset.mDisplayItem, preset.mName, NamedTextColor.DARK_AQUA, false, "Delve preset of level " + preset.mLevel, NamedTextColor.WHITE);
		return new GuiItem(item).onClick((event) -> new DelveCustomInventory(mPlayer, mDungeon, true, preset).openInventory(mPlayer, Plugin.getInstance()));
	}
}
