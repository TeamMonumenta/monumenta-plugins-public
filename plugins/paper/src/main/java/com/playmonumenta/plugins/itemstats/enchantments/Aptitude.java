package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import java.util.EnumSet;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class Aptitude implements Enchantment {
	public static final double COOLDOWN_REDUCTION_PER_LEVEL = 0.05;

	@Override
	public @NotNull String getName() {
		return "Aptitude";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.APTITUDE;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND, Slot.OFFHAND, Slot.HEAD, Slot.CHEST, Slot.LEGS, Slot.FEET, Slot.PROJECTILE);
	}

	@Override
	public void onAbilityCast(Plugin plugin, Player player, double value, AbilityCastEvent event) {
		event.setCooldown((int) (event.getCooldown() * getCooldownPercentage(value)));
	}

	public static double getCooldownPercentage(double level) {
		return Math.pow(1 - COOLDOWN_REDUCTION_PER_LEVEL, level);
	}
}
