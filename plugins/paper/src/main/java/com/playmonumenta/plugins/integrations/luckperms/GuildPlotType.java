package com.playmonumenta.plugins.integrations.luckperms;

import com.playmonumenta.plugins.utils.GUIUtils;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public enum GuildPlotType {
	LAKE_ISLAND(
		0,
		"Lake Island",
		"A plot featuring an island in the middle of a lake.",
		Material.CHERRY_LEAVES,
		TextColor.color(0xff, 0x7f, 0x7f)
	),
	GRASSY_FLATLAND(
		1,
		"Grassy Flatland",
		"A flat grassy plot ready for building.",
		Material.GRASS_BLOCK,
		NamedTextColor.GREEN
	),
	VOID(
		2,
		"Void",
		"An empty plot, the perfect blank slate for the skyblock feel. Don't worry, there are barrier blocks underneath you.",
		Material.BLACK_CONCRETE,
		NamedTextColor.GRAY
	);

	public final int mScore;
	public final String mName;
	public final String mDescription;
	public final Material mIconMaterial;
	public final TextColor mNameColor;

	GuildPlotType(int score, String name, String description, Material iconMaterial, TextColor nameColor) {
		mScore = score;
		mName = name;
		mDescription = description;
		mIconMaterial = iconMaterial;
		mNameColor = nameColor;
	}

	public static GuildPlotType byScore(int score) {
		for (GuildPlotType type : values()) {
			if (type.mScore == score) {
				return type;
			}
		}
		return LAKE_ISLAND;
	}

	public ItemStack getIcon() {
		return GUIUtils.createBasicItem(mIconMaterial, mName, mNameColor, mDescription);
	}
}
