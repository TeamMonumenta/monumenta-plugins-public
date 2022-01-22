package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemStatUtils.Slot;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;

import java.util.EnumSet;

public class FireAspect implements Enchantment {
	private static final int FIRE_ASPECT_DURATION = 20 * 4;

	@Override
	public String getName() {
		return "Fire Aspect";
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND);
	}

	@Override
	public double getPriorityAmount() {
		return 10;
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.FIRE_ASPECT;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double level, DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE || event.getDamager() instanceof Trident) {
			EntityUtils.applyFire(plugin, (int) (FIRE_ASPECT_DURATION * level), enemy, player);
			player.getWorld().spawnParticle(Particle.FLAME, enemy.getLocation().add(0, 1, 0), 6, 0.5, 0.5, 0.5, 0.001);
		}
	}
}
