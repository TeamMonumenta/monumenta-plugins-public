package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemStatUtils.Slot;
import java.util.EnumSet;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;

public class PointBlank implements Enchantment {
	public static final int DISTANCE = 8;
	public static final int DAMAGE_PER_LEVEL = 2;
	public static final String CHARM_DAMAGE = "Point Blank Damage";

	@Override
	public String getName() {
		return "Point Blank";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.POINT_BLANK;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND);
	}

	@Override
	public double getPriorityAmount() {
		return 8;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double level, DamageEvent event, LivingEntity target) {
		if (event.getType() == DamageType.PROJECTILE) {
			if (event.getDamager() instanceof AbstractArrow arrow && !(arrow instanceof Trident) && !arrow.isCritical()) {
				return;
			}

			event.setDamage(event.getDamage() + apply(player, target, level));
		}
	}

	public static double apply(Player player, LivingEntity target, double level) {
		if (level > 0 && player.getLocation().distance(target.getLocation()) < DISTANCE) {
			particles(target.getEyeLocation(), player);
			return CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, level * DAMAGE_PER_LEVEL);
		}
		return 0;
	}

	public static void particles(Location loc, Player player) {
		World world = loc.getWorld();
		world.spawnParticle(Particle.SMOKE_NORMAL, loc, 30, 0, 0, 0, 0.25);
		world.spawnParticle(Particle.CRIT_MAGIC, loc, 30, 0, 0, 0, 0.65);
		player.playSound(player.getLocation(), Sound.ENTITY_ARMOR_STAND_BREAK, 1.5f, 0.75f);
	}
}
