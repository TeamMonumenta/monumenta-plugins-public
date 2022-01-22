package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemStatUtils.Slot;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.jetbrains.annotations.NotNull;

import java.util.EnumSet;

public class Bleeding implements Enchantment {

	private static final int DURATION = 20 * 5;
	private static final Particle.DustOptions COLOR = new Particle.DustOptions(Color.fromRGB(210, 44, 44), 1.0f);

	@Override
	public @NotNull String getName() {
		return "Bleeding";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.BLEEDING;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND);
	}

	@Override
	public double getPriorityAmount() {
		return 17;
	}

	@Override
	public void onDamage(@NotNull Plugin plugin, @NotNull Player player, double value, @NotNull DamageEvent event, @NotNull LivingEntity enemy) {
		double level = plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.BLEEDING);
		if (event.getType() == DamageType.MELEE) {
			EntityUtils.applyBleed(plugin, (int)(DURATION * player.getCooledAttackStrength(0)), (int) level, enemy);
			player.getWorld().spawnParticle(Particle.REDSTONE, enemy.getLocation().add(0, 1, 0), 8, 0.3, 0.6, 0.3, COLOR);
		} else if (event.getType() == DamageType.PROJECTILE && event.getDamager() instanceof Trident) {
			EntityUtils.applyBleed(plugin, (int)(DURATION), (int) level, enemy);
			player.getWorld().spawnParticle(Particle.REDSTONE, enemy.getLocation().add(0, 1, 0), 8, 0.3, 0.6, 0.3, COLOR);
		}
	}
}
