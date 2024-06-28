package com.playmonumenta.plugins.delves.abilities;

import com.playmonumenta.plugins.itemstats.enums.Tier;
import com.playmonumenta.plugins.itemstats.infusions.Shattered;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Fragile {

	public static final String DESCRIPTION = "Deaths cause extra shatter.";

	public static Component[] rankDescription(int level) {
		return new Component[]{
			Component.text("Upon death, players get +1 extra level of shatter.")
		};
	}

	public static void applyModifiers(Player player, int level) {
		if (level == 0) {
			return;
		}
		for (int i = 36; i <= 40; i++) {
			ItemStack item = player.getInventory().getContents()[i];
			if (item == null || ItemStatUtils.getTier(item) == Tier.NONE) {
				continue;
			}
			Shattered.shatter(item, 1);
		}
	}
}
