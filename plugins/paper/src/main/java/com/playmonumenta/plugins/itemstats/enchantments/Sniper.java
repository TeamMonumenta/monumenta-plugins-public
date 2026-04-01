package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.EnumSet;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Sniper implements Enchantment {
	public static final int DISTANCE = 12;
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
		return EnumSet.of(Slot.MAINHAND, Slot.PROJECTILE);
	}

	@Override
	public double getPriorityAmount() {
		return 7;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double level, DamageEvent event, LivingEntity target) {
		if (event.getType() == DamageType.PROJECTILE && !event.isCancelled()) {
			double bowDraw = 1.0;
			if (event.getDamager() instanceof AbstractArrow arrow) {
				bowDraw = PlayerUtils.calculateBowDraw(arrow);
			}

			event.setFlatDamage(event.getFlatDamage() + bowDraw * apply(player, target, level));
		}
	}

	public static double apply(Player player, LivingEntity target, double level) {
		return apply(player, target.getEyeLocation(), level);
	}

	public static double apply(Player player, Location target, double level) {
		if (level > 0) {
			float distanceScaling = Math.min((float) player.getEyeLocation().distance(target) / DISTANCE, 1);
			particles(target, player, distanceScaling);
			return (level * DAMAGE_PER_LEVEL * distanceScaling);
		}
		return 0;
	}

	public static void particles(Location loc, Player player, float distanceScaling) {
		new PartialParticle(Particle.CRIT, loc, (int) (30 * distanceScaling), 0, 0, 0, 0.65).spawnAsPlayerActive(player);
		new PartialParticle(Particle.CRIT_MAGIC, loc, (int) (30 * distanceScaling), 0, 0, 0, 0.65).spawnAsPlayerActive(player);
		if (distanceScaling > 0.4) {
			player.playSound(player.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, SoundCategory.PLAYERS, distanceScaling * 0.6f, 0.5f);
		}
	}
}
