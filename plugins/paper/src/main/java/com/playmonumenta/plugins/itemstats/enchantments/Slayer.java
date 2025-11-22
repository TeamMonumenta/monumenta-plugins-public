package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.EnumSet;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;

public class Slayer implements Enchantment {
	private static final double DAMAGE_PER_LEVEL = 2;

	@Override
	public String getName() {
		return "Slayer";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.SLAYER;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND);
	}

	@Override
	public double getPriorityAmount() {
		return 5;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double level, DamageEvent event, LivingEntity enemy) {
		boolean isProjectile = event.getType() == DamageType.PROJECTILE && event.getDamager() instanceof Trident;
		if (isProjectile || event.getType() == DamageType.MELEE) {
			event.setFlatDamage(event.getFlatDamage() + calculateSlayerDamage(isProjectile, player, level, enemy));
		}
	}

	public static double calculateSlayerDamage(boolean isProjectile, Player player, double level, LivingEntity target) {
		if (EntityUtils.isBeast(target) && level > 0) {
			double damage = level * DAMAGE_PER_LEVEL;
			if (isProjectile) {
				return damage;
			} else {
				return damage * player.getCooledAttackStrength(0);
			}
		}
		return 0;
	}
}
