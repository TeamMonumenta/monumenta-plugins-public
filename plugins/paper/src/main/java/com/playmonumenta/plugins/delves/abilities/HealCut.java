package com.playmonumenta.plugins.delves.abilities;

import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;

public class HealCut {
	public static final String DESCRIPTION = "Healing is less effective";
	private static final int CUT_AMOUNT = 30;

	public static Component[] rankDescription(int level) {
		return new Component[]{
			Component.text("Saturation no longer heals and all other healing sources are reduced by " + ((level - 1) * CUT_AMOUNT) + "%.")
		};
	}

	public static void applyHealcut(EntityRegainHealthEvent event, int level) {
		if (level == 0) {
			return;
		}
		if (event.getEntity() instanceof Player player) {
			if (level > 1) {
				event.setAmount(event.getAmount() * (1 - CUT_AMOUNT / 100.0));
			}
			if (player.getSaturation() > 0) {
				player.setSaturation((float) Math.min(player.getSaturation(), 0.3));
			}
		}
	}
}
