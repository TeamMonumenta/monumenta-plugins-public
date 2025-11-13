package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import java.util.EnumSet;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Earthseeker implements Enchantment {
	public static final double DAMAGE_BONUS_PER_LEVEL = 0.1;

	// NOTE: Enchantment is deprecated / unused as of 14th Oct 2025

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.EARTHSEEKER;
	}

	@Override
	public String getName() {
		return "Earthseeker";
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND, Slot.OFFHAND, Slot.HEAD, Slot.CHEST, Slot.LEGS, Slot.FEET, Slot.PROJECTILE);
	}

	@Override
	public double getPriorityAmount() {
		return 27;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double level, DamageEvent event, LivingEntity enemy) {
		if (enemy.getEyeLocation().getY() < player.getLocation().getY()) {
			event.updateGearDamageWithMultiplier(1 + DAMAGE_BONUS_PER_LEVEL * level);
		}
	}
}
