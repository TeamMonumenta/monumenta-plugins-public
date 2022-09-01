package com.playmonumenta.plugins.utils;

import org.bukkit.entity.Player;

public class ExperienceUtils {

	public static final int LEVEL_30 = 1395;
	public static final int LEVEL_40 = 2920;
	public static final int LEVEL_50 = 5345;
	public static final int LEVEL_60 = 8670;
	public static final int LEVEL_70 = 12895;
	public static final int LEVEL_80 = 18020;

	public static int getTotalExperience(int level) {
		int xp = 0;

		if (level >= 0 && level <= 15) {
			xp = (int) Math.round(Math.pow(level, 2) + 6 * level);
		} else if (level > 15 && level <= 30) {
			xp = (int) Math.round(2.5 * Math.pow(level, 2) - 40.5 * level + 360);
		} else if (level > 30) {
			xp = (int) Math.round(4.5 * Math.pow(level, 2) - 162.5 * level + 2220);
		}
		return xp;
	}

	public static int getTotalExperience(Player player) {
		return Math.round(player.getExp() * player.getExpToLevel()) + getTotalExperience(player.getLevel());
	}

	public static int getLevel(int totalXp) {
		float a = 0;
		float b = 0;
		float c = -totalXp;

		if (totalXp > getTotalExperience(0) && totalXp <= getTotalExperience(15)) {
			a = 1;
			b = 6;
		} else if (totalXp > getTotalExperience(15) && totalXp <= getTotalExperience(30)) {
			a = 2.5f;
			b = -40.5f;
			c += 360;
		} else if (totalXp > getTotalExperience(30)) {
			a = 4.5f;
			b = -162.5f;
			c += 2220;
		}
		return (int) Math.floor((-b + Math.sqrt(Math.pow(b, 2) - (4 * a * c))) / (2 * a));
	}

	public static void setTotalExperience(Player player, int amount) {
		int level = getLevel(amount);
		int xp = amount - getTotalExperience(level);
		player.setLevel(level);
		player.setExp(0);
		player.giveExp(xp);
	}

	public static void addTotalExperience(Player player, int amount) {
		setTotalExperience(player, amount + getTotalExperience(player));
	}
}
