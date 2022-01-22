package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.ItemStatUtils;

/*
 * NOTICE!
 * If this enchantment gets changed, make sure someone updates the Python item replacement code to match!
 * Constants and new enchantments included!
 * This most likely means @NickNackGus or @Combustible
 * If this does not happen, your changes will NOT persist across weekly updates!
 */
public class DeCluckifyHeldItem extends GenericCommand {
	public static void register() {
		registerPlayerCommand("decluckifyhelditem", "monumenta.command.decluckifyhelditem", (sender, player) -> {
			ItemStatUtils.removeEnchantment(player.getItemInHand(), ItemStatUtils.EnchantmentType.CLUCKING);
			ItemStatUtils.generateItemStats(player.getItemInHand());
			});

	}
}
