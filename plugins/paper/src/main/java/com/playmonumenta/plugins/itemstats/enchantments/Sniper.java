package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemStatUtils.Slot;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.EnumSet;

public class Sniper implements Enchantment {
	public static final int DISTANCE = 16;
	public static final int DAMAGE_PER_LEVEL = 2;

	@Override
	public String getName() {
		return "Sniper";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.SNIPER;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND);
	}

	@Override
	public double getPriorityAmount() {
		return 7;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double level, DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.PROJECTILE) {
			Location loca = player.getLocation();
			if (loca.distance(enemy.getLocation()) > DISTANCE) {
				event.setDamage(event.getDamage() + level * DAMAGE_PER_LEVEL);
				particles(enemy.getEyeLocation(), player);
			}
		}
	}

	public static void particles(Location loc, Player player) {
		World world = loc.getWorld();
		world.spawnParticle(Particle.CRIT, loc, 30, 0, 0, 0, 0.65);
		world.spawnParticle(Particle.CRIT_MAGIC, loc, 30, 0, 0, 0, 0.65);
		player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 0.6f, 0.5f);
	}

}
