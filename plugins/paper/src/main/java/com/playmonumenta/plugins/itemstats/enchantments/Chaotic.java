package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.EnumSet;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.jetbrains.annotations.NotNull;

public class Chaotic implements Enchantment {

	@Override
	public @NotNull String getName() {
		return "Chaotic";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.CHAOTIC;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND);
	}

	@Override
	public double getPriorityAmount() {
		return 9;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		boolean isProjectile = (event.getType() == DamageType.PROJECTILE && event.getDamager() instanceof Trident) || event.getAbility() == ClassAbility.ALCHEMIST_POTION;
		if (isProjectile || event.getType() == DamageType.MELEE) {
			event.setFlatDamage(Math.max(0, event.getFlatDamage() + calculateChaoticDamage(isProjectile, player, value, enemy)));
		}
	}

	public static double calculateChaoticDamage(boolean isProjectile, Player player, double value, LivingEntity enemy) {
		if (value > 0) {
			int rand = FastUtils.RANDOM.nextInt(2 * (int) value + 1) - (int) value;
			if (isProjectile) {
				return rand;
			} else {
				new PartialParticle(Particle.DAMAGE_INDICATOR, enemy.getLocation().add(0, 1, 0), 1, 0.5, 0.5, 0.5, 0.001).spawnAsPlayerActive(player);
				return rand * player.getCooledAttackStrength(0);
			}
		}
		return 0;
	}
}
