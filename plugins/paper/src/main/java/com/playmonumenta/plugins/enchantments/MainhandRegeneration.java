package com.playmonumenta.plugins.enchantments;

import java.util.EnumSet;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.EnchantmentManager.ItemSlot;

import org.bukkit.entity.Player;




// When there is Regeneration,
// that class also handles Mainhand Regeneration effects.
// When there is only Mainhand Regeneration,
// this class works through using that class.
// This is the "sub" class of the two.
public class MainhandRegeneration implements BaseEnchantment {
	@Override
	public String getProperty() {
		return "Mainhand Regeneration";
	}

	@Override
	public EnumSet<ItemSlot> getValidSlots() {
		return EnumSet.of(ItemSlot.MAINHAND);
	}

	@Override
	public void tick(
		Plugin plugin,
		Player player,
		int level
	) {
		int otherRegenerationLevel = plugin.mTrackingManager.mPlayers.getPlayerCustomEnchantLevel(
			player,
			Regeneration.class
		);
		// If Regeneration's level is 0 such that its tick() does not run to handle both
		// regenerations' healing, take over by doing it ourselves for Mainhand Regeneration
		if (otherRegenerationLevel == 0) {
			Regeneration.doHeal(player, level);
		}
	}
}
