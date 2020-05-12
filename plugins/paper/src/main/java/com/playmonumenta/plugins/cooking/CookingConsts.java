package com.playmonumenta.plugins.cooking;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

class CookingConsts {

	static final ItemStack BASE_TABLE_CONTENTS_BG = CookingUtils.makeBaseTableContentsBG();
	static final ItemStack BASE_TABLE_CONTENTS_BASE = CookingUtils.makeBaseTableContentsBase();
	static final ItemStack BASE_TABLE_CONTENTS_TOOL = CookingUtils.makeBaseTableContentsTool();
	static final ItemStack BASE_TABLE_CONTENTS_ING = CookingUtils.makeBaseTableContentsIngredient();
	static final ItemStack BASE_TABLE_CONTENTS_NYU = CookingUtils.makeBaseTableContentsNYU();
	static final ItemStack BASE_TABLE_CONTENTS_OUT = CookingUtils.makeBaseTableContentsOutput();
	static final ItemStack BASE_TABLE_CONTENTS_AIR = new ItemStack(Material.AIR);

	static final ItemStack[] BASE_TABLE_CONTENTS_EMPTY = CookingUtils.makeEmptyBaseTableContents();
	static final ItemStack[] BASE_TABLE_CONTENTS = CookingUtils.makeBaseTableContents();

	static final ItemStack[][] BASE_TABLE_CONTENTS_TIERED = CookingUtils.makeTieredBaseTableContents();

	static final String ERROR_ITEM_NAME = ChatColor.RED + "" + ChatColor.BOLD + "Error";

	static final String COOKING_ITEM_JSON_SEPARATOR = "§|§|§|";
	static final ChatColor POSITIVE_EFFECT_COLOR = ChatColor.BLUE;
	static final ChatColor NEGATIVE_EFFECT_COLOR = ChatColor.RED;
	static final ChatColor NEUTER_COLOR = ChatColor.GRAY;
}
