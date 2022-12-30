package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemStatUtils.Slot;
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
		int rand = FastUtils.RANDOM.nextInt(2 * (int) value + 1) - (int) value;
		if (event.getType() == DamageType.MELEE) {
			new PartialParticle(Particle.DAMAGE_INDICATOR, enemy.getLocation().add(0, 1, 0), 1, 0.5, 0.5, 0.5, 0.001).spawnAsPlayerActive(player);
			event.setDamage(Math.max(0, event.getDamage() + rand * player.getCooledAttackStrength(0)));
		} else if (event.getType() == DamageType.PROJECTILE && event.getDamager() instanceof Trident) {
			event.setDamage(Math.max(0, event.getDamage() + rand));
		}
	}
}
