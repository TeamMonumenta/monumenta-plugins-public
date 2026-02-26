package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import java.util.EnumSet;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;


public class Defiance implements Enchantment {
	public static double BASE_DAMAGE_PER_AILMENT = 0.05;

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.DEFIANCE;
	}

	@Override
	public String getName() {
		return "Defiance";
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND, Slot.OFFHAND, Slot.HEAD, Slot.CHEST, Slot.LEGS, Slot.FEET, Slot.PROJECTILE);
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double level, DamageEvent event, LivingEntity enemy) {
		double mult = 1;
		double damagePerAilment = BASE_DAMAGE_PER_AILMENT * level;
		for (PotionEffect p : player.getActivePotionEffects()) {
			PotionEffectType type = p.getType();
			if (type.getEffectCategory() == PotionEffectType.Category.HARMFUL) {
				mult += damagePerAilment;
			}
		}
		event.updateGearDamageWithMultiplier(mult);
	}
}
