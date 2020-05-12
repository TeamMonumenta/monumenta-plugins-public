package com.playmonumenta.plugins.cooking;

import com.google.gson.Gson;
import com.playmonumenta.plugins.utils.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.Objective;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CookingUtils {

	// actual utils

	static int getTableTierFromPlayer(Player player) {
		Objective objective = Bukkit.getScoreboardManager().getMainScoreboard().getObjective("CookTier");
		int score = 0;
		if (player != null && objective != null) {
			score = objective.getScore(player.getName()).getScore();
		}
		// make sure score stays between boundaries to prevent Index exceptions
		if (score > 8) {
			score = 8;
			objective.getScore(player.getName()).setScore(score);
		} else if (score < 0) {
			score = 0;
			objective.getScore(player.getName()).setScore(score);
		}
		return score;
	}

	public static CookingItemObject cookingItemObjectFromJson(String json) {
		return new Gson().fromJson(json, CookingItemObject.class);
	}

	public static String extractItemDataFromFirstLoreLine(ItemStack item) {
		if (item == null) {
			return null;
		}
		List<String> strList = item.getItemMeta().getLore();
		if (strList == null) {
			return null;
		}

		String line = strList.get(0);
		line = line.split("§\\|§\\|§\\|")[0];
		return StringUtils.convertToVisibleLoreLine(line);
	}

	static String[] getCookingItemTypeStringArray() {
		ArrayList<String> strList = new ArrayList<>();
		for (CookingItemType t : CookingItemType.values()) {
			strList.add(t.toString());
		}
		return strList.toArray(new String[0]);
	}

	public static ArrayList<String> compileEffectLines(Map<CookingEffectsEnum, Integer> effects) {
		ArrayList<String> out = new ArrayList<>();

		for (CookingEffectsEnum e : CookingEffectsEnum.values()) {
			if (e.getKind() == CookingEffectsEnum.Kind.DURATION) {
				// ignore Duration kind, except the following:
				continue;
			} else if (e.getKind() == CookingEffectsEnum.Kind.POTENCY) {
				Integer potency = effects.getOrDefault(e, 0);
				if (potency <= 0) {
					continue;
				}
				CookingEffectsEnum de = CookingEffectsEnum.valueOf(e.toString().replace("POTENCY", "DURATION"));
				Integer duration = effects.getOrDefault(de, 0);
				String eStr = e.getReadableStr();
				if (duration < 0) {
					// invert sign color
					eStr = eStr.replace(ChatColor.DARK_RED + "", ChatColor.BLACK + "")
						.replace(ChatColor.DARK_BLUE + "", ChatColor.DARK_RED + "")
						.replace(ChatColor.BLACK + "", ChatColor.DARK_BLUE + "");
				}
				out.add(String.format(eStr + de.getReadableStr(), StringUtils.toRoman(potency), StringUtils.intToMinuteAndSeconds(duration)));
			} else {
				Integer value = effects.getOrDefault(e, 0);
				ChatColor signColor = CookingConsts.POSITIVE_EFFECT_COLOR;
				if (value == 0) {
					continue;
				} else if (value < 0) {
					signColor = CookingConsts.NEGATIVE_EFFECT_COLOR;
				}
				out.add(String.format(signColor + e.getReadableStr(), value));
			}
		}

		return out;
	}

	static Material cookedMaterialVersionOf(Material mat) {
		switch (mat) {
			case BEEF:
				return Material.COOKED_BEEF;
			case CHICKEN:
				return Material.COOKED_CHICKEN;
			default:
				return mat;
		}
	}

	// cooking table builder

	static ItemStack[][] makeTieredBaseTableContents() {
		ItemStack[][] out = new ItemStack[9][];
		out[0] = CookingConsts.BASE_TABLE_CONTENTS;
		for (int i = 1; i < 9; i++) {
			ItemStack[] tmp = out[i - 1].clone();
			for (int j = 29; j < 42; j++) {
				if (tmp[j].equals(CookingConsts.BASE_TABLE_CONTENTS_NYU)) {
					tmp[j] = CookingConsts.BASE_TABLE_CONTENTS_AIR;
					break;
				}
			}
			out[i] = tmp;
		}
		return out;
	}

	static ItemStack[] makeBaseTableContents() {
		ItemStack[] out = CookingConsts.BASE_TABLE_CONTENTS_EMPTY.clone();
		out[10] = CookingConsts.BASE_TABLE_CONTENTS_BASE;
		out[11] = CookingConsts.BASE_TABLE_CONTENTS_AIR;
		out[13] = CookingConsts.BASE_TABLE_CONTENTS_TOOL;
		out[14] = CookingConsts.BASE_TABLE_CONTENTS_AIR;
		out[25] = CookingConsts.BASE_TABLE_CONTENTS_OUT;
		out[28] = CookingConsts.BASE_TABLE_CONTENTS_ING;
		out[29] = CookingConsts.BASE_TABLE_CONTENTS_NYU;
		out[30] = CookingConsts.BASE_TABLE_CONTENTS_NYU;
		out[31] = CookingConsts.BASE_TABLE_CONTENTS_NYU;
		out[32] = CookingConsts.BASE_TABLE_CONTENTS_NYU;
		out[37] = CookingConsts.BASE_TABLE_CONTENTS_ING;
		out[38] = CookingConsts.BASE_TABLE_CONTENTS_NYU;
		out[39] = CookingConsts.BASE_TABLE_CONTENTS_NYU;
		out[40] = CookingConsts.BASE_TABLE_CONTENTS_NYU;
		out[41] = CookingConsts.BASE_TABLE_CONTENTS_NYU;
		return out;
	}


	static ItemStack[] makeEmptyBaseTableContents() {
		ItemStack[] out = new ItemStack[54];
		for (int i = 0; i < 54; i++) {
			out[i] = CookingConsts.BASE_TABLE_CONTENTS_BG;
		}
		return out;
	}

	static ItemStack makeBaseTableContentsBG() {
		ItemStack out = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
		ItemMeta outM = out.getItemMeta();
		outM.setDisplayName(" ");
		out.setItemMeta(outM);
		return out;
	}

	static ItemStack makeBaseTableContentsBase() {
		ItemStack out = new ItemStack(Material.FURNACE);
		ItemMeta outM = out.getItemMeta();
		outM.setDisplayName("Insert Base Ingredient here");
		out.setItemMeta(outM);
		return out;
	}

	static ItemStack makeBaseTableContentsTool() {
		ItemStack out = new ItemStack(Material.IRON_SWORD);
		ItemMeta outM = out.getItemMeta();
		outM.setDisplayName("Insert Cooking Tool Here");
		out.setItemMeta(outM);
		return out;
	}

	static ItemStack makeBaseTableContentsIngredient() {
		ItemStack out = new ItemStack(Material.BROWN_MUSHROOM);
		ItemMeta outM = out.getItemMeta();
		outM.setDisplayName("Insert Ingredients Here");
		out.setItemMeta(outM);
		return out;
	}

	static ItemStack makeBaseTableContentsNYU() {
		ItemStack out = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
		ItemMeta outM = out.getItemMeta();
		outM.setDisplayName("Ingredient Slot not yet unlocked");
		out.setItemMeta(outM);
		return out;
	}

	static ItemStack makeBaseTableContentsOutput() {
		ItemStack out = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
		ItemMeta outM = out.getItemMeta();
		outM.setDisplayName("Output will be here");
		out.setItemMeta(outM);
		return out;
	}
}
