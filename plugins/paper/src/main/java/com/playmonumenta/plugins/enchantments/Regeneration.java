package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;
import com.playmonumenta.plugins.utils.PlayerUtils;

import org.bukkit.entity.Player;




// When there is Regeneration,
// this class also handles Mainhand Regeneration effects.
// When there is only Mainhand Regeneration,
// that class works through using this class.
// This is the "lead" class of the two.
public class Regeneration implements BaseEnchantment {
	// Level 1 heals 1 health total every 3s.
	// Divide by 4 because tick() is called 4 times per second
	private static final double TICK_HEALING_1 = 1d / 3 / 4;

	@Override
	public String getProperty() {
		return "Regeneration";
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		return EnumSet.of(ItemSlot.OFFHAND, ItemSlot.ARMOR);
	}

	@Override
	public void tick(
		Plugin plugin,
		Player player,
		int level
	) {
		doHeal(
			player,
			level + plugin.mTrackingManager.mPlayers.getPlayerCustomEnchantLevel(
				player,
				MainhandRegeneration.class
			)
		);
	}

	public static void doHeal(Player player, int level) {
		PlayerUtils.healPlayer(
			player,
			TICK_HEALING_1 * Math.sqrt(level)
		);
	}
}
