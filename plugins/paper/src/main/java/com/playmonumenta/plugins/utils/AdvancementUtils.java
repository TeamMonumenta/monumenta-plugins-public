package com.playmonumenta.plugins.utils;

import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;

public class AdvancementUtils {
	public static void grantAdvancement(Player player, String key) {
		NamespacedKey namespacedKey = NamespacedKey.fromString(key);
		if (namespacedKey != null) {
			grantAdvancement(player, namespacedKey);
		}
	}

	public static void grantAdvancement(Player player, NamespacedKey namespacedKey) {
		Advancement advancement = Bukkit.getAdvancement(namespacedKey);
		if (advancement == null) {
			return;
		}
		AdvancementProgress progress = player.getAdvancementProgress(advancement);
		for (String criteria : progress.getRemainingCriteria()) {
			progress.awardCriteria(criteria);
		}
	}

	public static void grantAdvancementCriteria(Player player, String key, String criteria) {
		NamespacedKey namespacedKey = NamespacedKey.fromString(key);
		if (namespacedKey != null) {
			grantAdvancementCriteria(player, namespacedKey, criteria);
		}
	}

	public static void grantAdvancementCriteria(Player player, NamespacedKey namespacedKey, String criteria) {
		Advancement advancement = Bukkit.getAdvancement(namespacedKey);
		if (advancement == null) {
			return;
		}
		AdvancementProgress progress = player.getAdvancementProgress(advancement);
		progress.awardCriteria(criteria);
	}
}
