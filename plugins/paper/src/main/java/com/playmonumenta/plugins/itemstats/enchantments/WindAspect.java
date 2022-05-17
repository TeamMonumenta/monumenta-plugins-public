package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.PotionUtils;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class WindAspect implements Enchantment {

	@Override
	public @NotNull String getName() {
		return "Wind Aspect";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.WIND_ASPECT;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double level, DamageEvent event, LivingEntity enemy) {
		DamageEvent.DamageType type = event.getType();
		if ((type == DamageEvent.DamageType.MELEE && ItemStatUtils.isNotExclusivelyRanged(player.getInventory().getItemInMainHand())) || type == DamageEvent.DamageType.PROJECTILE) {
			PotionUtils.applyPotion(player, enemy, new PotionEffect(PotionEffectType.SLOW_FALLING, 20, 0));
			launch(enemy, level);
		}
	}

	private void launch(Entity e, double level) {
		World world = e.getWorld();
		Vector v = e.getVelocity();
		v.setY(1 + (level * 0.2)); // TODO: Check height here
		e.setVelocity(v);
		world.spawnParticle(Particle.END_ROD, e.getLocation(), 3, 0, 0.5, 0, 0.5);
		world.spawnParticle(Particle.BUBBLE_COLUMN_UP, e.getLocation(), 6, 0, 0, 0, 0.25);
		world.playSound(e.getLocation(), Sound.ENTITY_HORSE_BREATHE, 1.0f, 0.20f);
	}

}
